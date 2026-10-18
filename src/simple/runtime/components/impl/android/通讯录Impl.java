package simple.runtime.components.impl.android;

import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.RawContacts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import simple.runtime.android.MainActivity;
import simple.runtime.collections.集合;
import simple.runtime.components.组件容器;
import simple.runtime.components.通讯录;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;
import simple.runtime.variants.ArrayVariant;

/**
 * 基于系统 Contacts Provider 实现联系人和分组的查询、维护。
 *
 * <p>使用原始联系人编号区分记录，复合写入通过提供程序的批量事务完成。</p>
 *
 * <p>异步查询串行执行，结果在主线程派发；销毁时取消查询并移除待派发回调。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 通讯录Impl extends 组件Impl implements 通讯录 {

	private final MainActivity activity;
	private final ContentResolver resolver;

	// 查询串行执行，结果与事件由主线程派发。
	private final Handler handler = new Handler(Looper.getMainLooper());
	private final ExecutorService reader = Executors.newSingleThreadExecutor();
	private final Object lifecycle = new Object();
	private final MainActivity.OnDestroyListener destroyListener;

	// 以下生命周期状态和待取消查询均由 lifecycle 锁保护。
	private boolean destroyed;
	private boolean reading;
	private QueryCancellation pendingQuery;

	// 同步操作与异步回调均可更新，属性读取保持可见。
	private volatile String lastError = "";

	/**
	 * 创建通讯录组件并登记 Activity 销毁回调。
	 *
	 * @param container 容纳此不可视组件的窗口，不得为空。
	 */
	public 通讯录Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		resolver = activity.getContentResolver();
		destroyListener = new MainActivity.OnDestroyListener() {
			@Override
			public void onDestroy() {
				// Activity 正在遍历监听器列表，此处不能修改该列表。
				release(false);
			}
		};
		activity.addOnDestroyListener(destroyListener);
	}

	/* 组件事件 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void 取联系人完毕(集合 contacts, String requestMark, String error) {
		EventDispatcher.dispatchEvent(this, "取联系人完毕", contacts, requestMark, error);
	}

	/* 组件属性 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String 最近错误() {
		return lastError;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 读取中() {
		synchronized (lifecycle) {
			return reading;
		}
	}

	/* 组件方法 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public 集合 取联系人() {
		return perform(new Operation<集合>() {
			@Override
			public 集合 run() {
				return readContacts(null, null);
			}
		}, new 集合());
	}

	/**
	 * 后台读取联系人，请求标记为空文本；沿用重复请求和销毁后的拒绝规则。
	 *
	 * @return 接受请求返回真，不表示读取成功。
	 */
	@Override
	public boolean 异步取联系人() {
		return 异步取联系人("");
	}

	/**
	 * {@inheritDoc}
	 *
	 * <p>后台串行查询，结果在主线程派发；销毁后丢弃未派发的结果。
	 */
	@Override
	public boolean 异步取联系人(final String requestMark) {
		synchronized (lifecycle) {
			if (destroyed || reading) {
				lastError = destroyed ? "组件已销毁" : "已有联系人读取任务尚未完成";
				return false;
			}

			// 持锁登记任务，避免重复读取及销毁与提交任务交错。
			reading = true;
			lastError = "";
			final QueryCancellation signal = Build.VERSION.SDK_INT >= 16 ? new CancellableQuery() : null;
			pendingQuery = signal;
			reader.execute(new Runnable() {
				@Override
				public void run() {
					集合 result = new 集合();
					String error = "";

					try {
						result = readContacts(null, signal);
					} catch (Exception failure) {
						error = errorText(failure);
					}

					// 工作线程不再修改结果，主线程回调负责完成状态与事件派发。
					final 集合 completed = result;
					final String message = error;

					synchronized (lifecycle) {
						if (destroyed) {
							return;
						}

						handler.post(new Runnable() {
							@Override
							public void run() {
								synchronized (lifecycle) {
									if (destroyed) {
										return;
									}

									reading = false;
									pendingQuery = null;
									lastError = message;
								}

								取联系人完毕(completed, requestMark == null ? "" : requestMark, message);
							}
						});
					}
				}
			});
			return true;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String 添加联系人(final String name, final String phone) {
		return perform(new Operation<String>() {
			@Override
			public String run() throws Exception {
				requireText(name, "联系人姓名");
				requireText(phone, "电话号码");

				ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
				operations.add(
					ContentProviderOperation.newInsert(RawContacts.CONTENT_URI)
					.withValue(RawContacts.ACCOUNT_NAME, null)
					.withValue(RawContacts.ACCOUNT_TYPE, null)
					.build());
				operations.add(
					ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, 0)
					.withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
					.withValue(StructuredName.DISPLAY_NAME, name)
					.build());
				operations.add(
					ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValueBackReference(Data.RAW_CONTACT_ID, 0)
					.withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
					.withValue(Phone.NUMBER, phone)
					.withValue(Phone.TYPE, Phone.TYPE_MOBILE)
					.build());
				ContentProviderResult[] results = resolver.applyBatch(ContactsContract.AUTHORITY, operations);
				return Long.toString(ContentUris.parseId(results[0].uri));
			}
		}, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 删除联系人(final String rawContactId) {
		return perform(new Operation<Boolean>() {
			@Override
			public Boolean run() {
				long id = positiveId(rawContactId);
				ensureRawContact(id);
				return requireChanged(resolver.delete(ContentUris.withAppendedId(RawContacts.CONTENT_URI, id), null, null));
			}
		}, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public 集合 取联系人分组() {
		return perform(new Operation<集合>() {
			@Override
			public 集合 run() {
				集合 result = new 集合();
				{
					// 只读取未删除的分组；游标在转换完成或异常时都必须关闭。
					Cursor cursor = query(
						Groups.CONTENT_URI,
						new String[] {
							Groups._ID,
							Groups.TITLE,
							Groups.ACCOUNT_NAME,
							Groups.ACCOUNT_TYPE,
							Groups.DATA_SET
						},
						Groups.DELETED + "=0",
						null,
						Groups.TITLE + " ASC," + Groups._ID + " ASC",
						null);
					try {
						while (cursor.moveToNext()) {
							addRow(
								result,
								new String[] {
									text(cursor, 0),
									text(cursor, 1),
									text(cursor, 2),
									text(cursor, 3),
									text(cursor, 4)
								});
						}
					} finally {
						cursor.close();
					}
				}

				return result;
			}
		}, new 集合());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public 集合 取分组联系人(final String groupId) {
		return perform(new Operation<集合>() {
			@Override
			public 集合 run() {
				long id = positiveId(groupId);
				account(Groups.CONTENT_URI, id);
				return readContacts(Long.toString(id), null);
			}
		}, new 集合());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String 新建联系人分组(final String name, final String accountName, final String accountType) {
		return perform(new Operation<String>() {
			@Override
			public String run() {
				requireText(name, "分组名称");
				String owner = nullIfEmpty(accountName);
				String type = nullIfEmpty(accountType);

				if ((owner == null) != (type == null)) {
					throw new IllegalArgumentException("账号名称和类型必须同时填写或同时为空");
				}

				ContentValues values = new ContentValues();
				values.put(Groups.TITLE, name);
				values.put(Groups.ACCOUNT_NAME, owner);
				values.put(Groups.ACCOUNT_TYPE, type);
				values.put(Groups.GROUP_VISIBLE, 1);
				Uri uri = resolver.insert(Groups.CONTENT_URI, values);
				if (uri == null) {
					throw new IllegalStateException("系统未返回新分组编号");
				}

				return Long.toString(ContentUris.parseId(uri));
			}
		}, "");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 添加分组联系人(final String groupId, final String rawContactId) {
		return perform(new Operation<Boolean>() {
			@Override
			public Boolean run() throws Exception {
				long group = positiveId(groupId);
				long contact = positiveId(rawContactId);
				String[] owner = account(Groups.CONTENT_URI, group);
				String[] person = account(RawContacts.CONTENT_URI, contact);

				for (int i = 0; i < owner.length; i++) {
					if (!owner[i].equals(person[i])) {
						throw new IllegalArgumentException("分组和原始联系人必须属于同一账号和数据集");
					}
				}

				ensureWritable(Groups.CONTENT_URI, group, Groups.GROUP_IS_READ_ONLY);
				ensureRawContact(contact);
				{
					Cursor cursor = query(
						Data.CONTENT_URI,
						new String[] {Data._ID},
						membershipSelection(),
						membershipArgs(group, contact),
						null,
						null);
					try {
						if (cursor.moveToFirst()) {
							return true;
						}
					} finally {
						cursor.close();
					}
				}

				// 再次断言账号及数据集，防止查询与插入之间其它应用修改记录归属。
				ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
				operations.add(accountAssertion(Groups.CONTENT_URI, group, owner));
				operations.add(accountAssertion(RawContacts.CONTENT_URI, contact, person));
				operations.add(
					ContentProviderOperation.newInsert(Data.CONTENT_URI)
					.withValue(Data.MIMETYPE, GroupMembership.CONTENT_ITEM_TYPE)
					.withValue(Data.RAW_CONTACT_ID, contact)
					.withValue(GroupMembership.GROUP_ROW_ID, group)
					.build());
				resolver.applyBatch(ContactsContract.AUTHORITY, operations);
				return true;
			}
		}, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 移除分组联系人(final String groupId, final String rawContactId) {
		return perform(new Operation<Boolean>() {
			@Override
			public Boolean run() {
				long group = positiveId(groupId);
				long contact = positiveId(rawContactId);

				ensureWritable(Groups.CONTENT_URI, group, Groups.GROUP_IS_READ_ONLY);
				ensureRawContact(contact);
				return requireChanged(resolver.delete(Data.CONTENT_URI, membershipSelection(), membershipArgs(group, contact)));
			}
		}, false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean 删除联系人分组(final String groupId) {
		return perform(new Operation<Boolean>() {
			@Override
			public Boolean run() {
				long id = positiveId(groupId);
				ensureWritable(Groups.CONTENT_URI, id, Groups.GROUP_IS_READ_ONLY);
				return requireChanged(resolver.delete(ContentUris.withAppendedId(Groups.CONTENT_URI, id), null, null));
			}
		}, false);
	}

	/* 生命周期 */

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void 销毁() {
		release(true);
	}

	/**
	 * 取消查询、移除待派发回调并释放执行器；重复调用不再执行清理。
	 *
	 * @param removeListener 是否主动移除 Activity 监听器；遍历监听器期间传假。
	 */
	private void release(boolean removeListener) {
		synchronized (lifecycle) {
			if (destroyed) {
				return;
			}

			destroyed = true;
			reading = false;
			if (pendingQuery != null) {
				pendingQuery.cancel();
			}

			pendingQuery = null;
			handler.removeCallbacksAndMessages(null);
			reader.shutdownNow();
		}

		if (removeListener) {
			activity.removeOnDestroyListener(destroyListener);
		}

		super.销毁();
	}

	/**
	 * 分批读取原始联系人和号码，按原始联系人编号合并结果。
	 *
	 * <p>分组读取先收集成员编号；每个号码输出一行，无号码的联系人保留一行空号码。
	 *
	 * @param groupId 分组编号；传 {@code null} 时读取全部可访问联系人。
	 * @param signal 查询取消控制器；传 {@code null} 时不使用提供程序取消信号。
	 * @return 按姓名和原始联系人编号排列的四列联系人集合。
	 */
	private 集合 readContacts(String groupId, QueryCancellation signal) {
		Set<String> members = null;
		if (groupId != null) {
			members = new HashSet<String>();
			{
				Cursor cursor = query(
					Data.CONTENT_URI,
					new String[] {Data.RAW_CONTACT_ID},
					Data.MIMETYPE + "=? AND " + GroupMembership.GROUP_ROW_ID + "=?",
					new String[] {GroupMembership.CONTENT_ITEM_TYPE, groupId},
					null,
					signal);
				try {
					while (cursor.moveToNext()) {
						members.add(text(cursor, 0));
					}
				} finally {
					cursor.close();
				}
			}
		}

		// 保留查询顺序，并以原始联系人编号区分不同账号中的记录。
		Map<String, String[]> people = new LinkedHashMap<String, String[]>();
		{
			Cursor cursor = query(
				RawContacts.CONTENT_URI,
				new String[] {
					RawContacts._ID,
					RawContacts.DISPLAY_NAME_PRIMARY,
					RawContacts.CONTACT_ID
				},
				RawContacts.DELETED + "=0",
				null,
				RawContacts.DISPLAY_NAME_PRIMARY + " ASC," + RawContacts._ID + " ASC",
				signal);
			try {
				while (cursor.moveToNext()) {
					checkCanceled(signal);
					String id = text(cursor, 0);
					if (members == null || members.contains(id)) {
						people.put(id, new String[] {id, text(cursor, 1), "", text(cursor, 2)});
					}
				}
			} finally {
				cursor.close();
			}
		}

		// 一次读取号码，避免为每个联系人分别查询提供程序。
		Map<String, ArrayList<String>> phones = new LinkedHashMap<String, ArrayList<String>>();
		{
			Cursor cursor = query(
				Data.CONTENT_URI,
				new String[] {Data.RAW_CONTACT_ID, Phone.NUMBER},
				Data.MIMETYPE + "=?",
				new String[] {Phone.CONTENT_ITEM_TYPE},
				Data._ID + " ASC",
				signal);
			try {
				while (cursor.moveToNext()) {
					checkCanceled(signal);
					String id = text(cursor, 0);
					if (!people.containsKey(id)) {
						continue;
					}

					ArrayList<String> numbers = phones.get(id);
					if (numbers == null) {
						numbers = new ArrayList<String>();
						phones.put(id, numbers);
					}

					numbers.add(text(cursor, 1));
				}
			} finally {
				cursor.close();
			}
		}

		// 一个号码输出一行，没有号码的联系人仍保留一行。
		集合 result = new 集合();

		for (Map.Entry<String, String[]> person : people.entrySet()) {
			ArrayList<String> numbers = phones.get(person.getKey());
			String[] row = person.getValue();

			if (numbers == null) {
				addRow(result, row);
			} else {
				for (String number : numbers) {
					addRow(result, new String[] {row[0], row[1], number, row[3]});
				}
			}
		}

		return result;
	}

	/**
	 * 执行系统查询，并将空游标视为查询失败。
	 *
	 * @param uri 查询目标。
	 * @param columns 返回的列。
	 * @param selection 筛选条件，可为 {@code null}。
	 * @param args 筛选参数，可为 {@code null}。
	 * @param order 排序规则，可为 {@code null}。
	 * @param signal 查询取消控制器，可为 {@code null}。
	 * @return 查询游标，调用者必须在 {@code finally} 中关闭。
	 * @throws IllegalStateException 提供程序未返回游标。
	 */
	private Cursor query(Uri uri, String[] columns, String selection, String[] args, String order, QueryCancellation signal) {
		Cursor cursor = signal == null
			? resolver.query(uri, columns, selection, args, order)
			: signal.query(resolver, uri, columns, selection, args, order);
		if (cursor == null) {
			throw new IllegalStateException("系统通讯录查询未返回结果");
		}

		return cursor;
	}

	/**
	 * 核实目标记录存在，并读取其账号名称、账号类型和数据集。
	 *
	 * @param uri 原始联系人或分组的内容 URI。
	 * @param id 目标记录编号。
	 * @return 三列账号归属信息；数据库空值统一为空文本。
	 * @throws IllegalArgumentException 目标不存在或已经删除。
	 */
	private String[] account(Uri uri, long id) {
		{
			Cursor cursor = query(
				ContentUris.withAppendedId(uri, id),
				new String[] {
					RawContacts.ACCOUNT_NAME,
					RawContacts.ACCOUNT_TYPE,
					RawContacts.DATA_SET
				},
				RawContacts.DELETED + "=0",
				null,
				null,
				null);
			try {
				if (!cursor.moveToFirst()) {
					throw new IllegalArgumentException("联系人或分组不存在");
				}

				return new String[] {text(cursor, 0), text(cursor, 1), text(cursor, 2)};
			} finally {
				cursor.close();
			}
		}
	}

	/**
	 * 构造批量事务中的归属断言，避免查询后记录被删除或迁移到其它账号。
	 *
	 * @param uri 原始联系人或分组的内容 URI。
	 * @param id 目标记录编号。
	 * @param owner 查询得到的账号名称、账号类型和数据集。
	 * @return 要求记录仍存在且归属不变的断言操作。
	 */
	private ContentProviderOperation accountAssertion(Uri uri, long id, String[] owner) {
		String[] columns = {
			RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE, RawContacts.DATA_SET
		};
		ContentProviderOperation.Builder assertion = ContentProviderOperation.newAssertQuery(ContentUris.withAppendedId(uri, id))
			.withSelection(RawContacts.DELETED + "=0", null)
			.withExpectedCount(1);
		for (int i = 0; i < columns.length; i++) {
			assertion.withValue(columns[i], nullIfEmpty(owner[i]));
		}

		return assertion.build();
	}

	/**
	 * 核实原始联系人存在，并拒绝包含只读数据行的联系人。
	 *
	 * <p>部分系统不支持 {@code RawContacts.RAW_CONTACT_IS_READ_ONLY} 查询列， 因此检查关联数据行，最终仍以提供程序的写入结果为准。
	 *
	 * @param id 原始联系人编号。
	 * @throws IllegalArgumentException 联系人不存在或已经删除。
	 * @throws IllegalStateException 联系人包含只读数据。
	 */
	private void ensureRawContact(long id) {
		account(RawContacts.CONTENT_URI, id);
		Cursor cursor = query(
			Data.CONTENT_URI,
			new String[] {Data._ID},
			Data.RAW_CONTACT_ID + "=? AND " + Data.IS_READ_ONLY + "<>0",
			new String[] {Long.toString(id)},
			null,
			null);
		try {
			if (cursor.moveToFirst()) {
				throw new IllegalStateException("联系人包含只读数据");
			}
		} finally {
			cursor.close();
		}
	}

	/**
	 * 核实目标存在且指定只读标志为零。
	 *
	 * @param uri 目标内容 URI。
	 * @param id 目标记录编号。
	 * @param column 只读标志列名。
	 * @throws IllegalArgumentException 目标不存在或已经删除。
	 * @throws IllegalStateException 目标为只读。
	 */
	private void ensureWritable(Uri uri, long id, String column) {
		{
			Cursor cursor = query(
				ContentUris.withAppendedId(uri, id),
				new String[] {column},
				RawContacts.DELETED + "=0",
				null,
				null,
				null);
			try {
				if (!cursor.moveToFirst()) {
					throw new IllegalArgumentException("联系人或分组不存在");
				}

				if (cursor.getInt(0) != 0) {
					throw new IllegalStateException("该联系人或分组为只读");
				}
			} finally {
				cursor.close();
			}
		}
	}

	/**
	 * 检查提供程序取消信号和工作线程中断状态。
	 *
	 * @param signal 查询取消控制器，可为 {@code null}。
	 * @throws IllegalStateException 工作线程已被中断。
	 */
	private static void checkCanceled(QueryCancellation signal) {
		if (signal != null) {
			signal.throwIfCanceled();
		}

		if (Thread.currentThread().isInterrupted()) {
			throw new IllegalStateException("联系人读取已取消");
		}
	}

	/**
	 * 隔离平台查询取消能力，使低版本系统不直接加载取消信号实现。
	 */
	private interface QueryCancellation {
		/**
		 * 执行可取消的系统查询。
		 *
		 * @param resolver 系统内容解析器。
		 * @param uri 查询目标。
		 * @param columns 返回的列。
		 * @param selection 筛选条件，可为 {@code null}。
		 * @param args 筛选参数，可为 {@code null}。
		 * @param order 排序规则，可为 {@code null}。
		 * @return 查询游标；提供程序失败时可能返回 {@code null}。
		 */
		Cursor query(ContentResolver resolver, Uri uri, String[] columns, String selection, String[] args, String order);

		/**
		 * 取消当前查询。
		 */
		void cancel();

		/**
		 * 检查当前查询是否已经取消；已取消时由平台抛出取消异常。
		 */
		void throwIfCanceled();
	}

	/**
	 * 使用 API 16 的取消信号执行查询。
	 *
	 * <p>API 14/15 使用五参数查询，通过线程中断和生命周期状态抑制后续回调。
	 */
	private static final class CancellableQuery implements QueryCancellation {
		private final CancellationSignal signal = new CancellationSignal();

		/**
		 * {@inheritDoc}
		 */
		@Override
		public Cursor query(ContentResolver resolver, Uri uri, String[] columns, String selection, String[] args, String order) {
			return resolver.query(uri, columns, selection, args, order, signal);
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void cancel() {
			signal.cancel();
		}

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void throwIfCanceled() {
			signal.throwIfCanceled();
		}
	}

	/**
	 * 由统一错误处理入口执行的同步操作。
	 *
	 * @param <T> 操作结果类型。
	 */
	private interface Operation<T> {
		/**
		 * 执行本次同步操作。
		 *
		 * @return 操作结果。
		 * @throws Exception 参数校验、系统查询或写入失败。
		 */
		T run() throws Exception;
	}

	/**
	 * 执行同步操作，并统一维护最近错误和失败返回值。
	 *
	 * @param <T> 操作结果类型。
	 * @param operation 要执行的操作。
	 * @param fallback 组件已销毁或操作失败时返回的默认值。
	 * @return 操作结果；失败时返回默认值。
	 */
	private <T> T perform(Operation<T> operation, T fallback) {
		try {
			synchronized (lifecycle) {
				if (destroyed) {
					throw new IllegalStateException("组件已销毁");
				}
			}

			T result = operation.run();
			lastError = "";
			return result;
		} catch (Exception failure) {
			lastError = errorText(failure);
			return fallback;
		}
	}

	/**
	 * 将异常转换为对外显示的错误文本，权限异常使用统一说明。
	 *
	 * @param failure 本次操作捕获的异常。
	 * @return 错误信息。
	 */
	private static String errorText(Exception failure) {
		if (failure instanceof SecurityException) {
			return "未获得通讯录访问权限或系统拒绝访问";
		}

		return failure.getClass().getSimpleName()
			+ ": "
			+ (failure.getMessage() == null ? "操作失败" : failure.getMessage());
	}

	/**
	 * 将编号文本解析为正整数。
	 *
	 * @param value 编号文本。
	 * @return 解析得到的正整数编号。
	 * @throws IllegalArgumentException 文本无法解析为长整数或编号不大于零。
	 */
	private static long positiveId(String value) {
		long id = Long.parseLong(value == null ? "" : value);
		if (id <= 0) {
			throw new IllegalArgumentException("编号必须为正整数文本");
		}

		return id;
	}

	/**
	 * 检查必填文本，空引用或仅包含空白的文本均不接受。
	 *
	 * @param value 待检查的文本。
	 * @param name 用于错误提示的参数名称。
	 * @throws IllegalArgumentException 文本为空。
	 */
	private static void requireText(String value, String name) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(name + "不得为空");
		}
	}

	/**
	 * 检查删除或修改操作确实影响了目标记录。
	 *
	 * @param count 提供程序返回的受影响记录数。
	 * @return 影响记录数大于零时返回真。
	 * @throws IllegalArgumentException 未影响任何记录。
	 */
	private static boolean requireChanged(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("目标不存在或未发生修改");
		}

		return true;
	}

	/**
	 * 读取文本列，将数据库空值统一为空文本。
	 *
	 * @param cursor 当前已定位到记录的游标。
	 * @param index 列索引。
	 * @return 当前列的文本。
	 */
	private static String text(Cursor cursor, int index) {
		return cursor.isNull(index) ? "" : cursor.getString(index);
	}

	/**
	 * 将空引用或空文本转换为数据库空值，不裁剪非空文本。
	 *
	 * @param value 待转换的文本。
	 * @return 空值返回 {@code null}，否则返回原文本。
	 */
	private static String nullIfEmpty(String value) {
		return value == null || value.length() == 0 ? null : value;
	}

	/**
	 * 将一行文本数组包装为 Simple 数组并加入结果集合。
	 *
	 * @param result 目标集合。
	 * @param row 一行文本数据。
	 */
	private static void addRow(集合 result, String[] row) {
		result.加入(ArrayVariant.getArrayVariant(row));
	}

	/**
	 * 构造精确匹配联系人分组关系的筛选条件。
	 *
	 * @return 依次匹配数据类型、原始联系人编号和分组编号的条件。
	 */
	private static String membershipSelection() {
		return Data.MIMETYPE
			+ "=? AND "
			+ Data.RAW_CONTACT_ID
			+ "=? AND "
			+ GroupMembership.GROUP_ROW_ID
			+ "=?";
	}

	/**
	 * 按分组关系筛选条件的占位顺序创建参数。
	 *
	 * @param group 分组编号。
	 * @param contact 原始联系人编号。
	 * @return 数据类型、原始联系人编号和分组编号。
	 */
	private static String[] membershipArgs(long group, long contact) {
		return new String[] {
			GroupMembership.CONTENT_ITEM_TYPE, Long.toString(contact), Long.toString(group)
		};
	}
}
