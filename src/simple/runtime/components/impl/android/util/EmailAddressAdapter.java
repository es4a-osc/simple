/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.components.impl.android.util;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.CancellationSignal;
import android.os.OperationCanceledException;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.text.util.Rfc822Token;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 在过滤线程查询本机联系人邮箱，仅将有限的不可变候选快照交付界面。
 *
 * <p>不持有查询游标；文本、焦点或生命周期改变时取消查询并丢弃过期结果。
 *
 * @author Sharon Perl
 * @author 树先生 xhwsd@qq.com
 */
public final class EmailAddressAdapter extends BaseAdapter implements Filterable {

	private static final int MAX_RESULTS = 50;
	private static final String[] PROJECTION = {Email.DISPLAY_NAME_PRIMARY, Email.ADDRESS};
	private final Object lock = new Object();
	private final Context context;
	private final Filter filter;
	private FailureListener failureListener;
	private List<Item> items = Collections.emptyList();
	private CancellationSignal cancellation;
	private long revision;
	private String constraint = "";
	private boolean active;
	private boolean closed;
	private volatile String lastError = "";

	/**
	 * 在主线程接收有效查询的失败原因。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public interface FailureListener {

		/**
		 * 通知权限或联系人提供者错误；无匹配结果不视为失败。
		 *
		 * @param message 错误原因
		 */
		void failed(String message);
	}

	/**
	 * 保存一条完整候选，不依赖已经关闭的联系人游标。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class Item {

		public final String name;
		public final String address;

		/**
		 * 创建候选快照。
		 *
		 * @param name 联系人显示名
		 * @param address 邮箱地址
		 */
		private Item(String name, String address) {
			this.name = name.equals(address) ? "" : name;
			this.address = address;
		}

		@Override
		public String toString() {
			if (name.isEmpty()) {
				return address;
			}
			return new Rfc822Token(name, address, null).toString();
		}
	}

	/**
	 * 将查询结果和所属输入版本一起送回主线程。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	private static final class Result {

		final long revision;
		final List<Item> items;
		final String error;

		/**
		 * 创建一次查询结果。
		 *
		 * @param revision 输入版本
		 * @param items 有限候选快照
		 * @param error 失败原因，成功或取消时为空
		 */
		Result(long revision, List<Item> items, String error) {
			this.revision = revision;
			this.items = items;
			this.error = error;
		}
	}

	/**
	 * 创建只持有应用上下文的适配器；所有界面方法由主线程调用。
	 *
	 * @param context 宿主上下文
	 * @param failureListener 有效查询失败时的通知
	 */
	public EmailAddressAdapter(Context context, FailureListener failureListener) {
		this.context = context.getApplicationContext();
		this.failureListener = failureListener;
		filter = new Filter() {
			@Override
			protected FilterResults performFiltering(CharSequence text) {
				Result result = query(text == null ? "" : text.toString().trim());
				FilterResults filtered = new FilterResults();
				filtered.values = result;
				filtered.count = result.items.size();
				return filtered;
			}

			@Override
			protected void publishResults(CharSequence text, FilterResults filtered) {
				Result result = (Result) filtered.values;
				synchronized (lock) {
					if (closed || !active || result.revision != revision) {
						return;
					}
				}
				items = result.items;
				lastError = result.error;
				notifyDataSetChanged();
				if (!result.error.isEmpty() && EmailAddressAdapter.this.failureListener != null) {
					EmailAddressAdapter.this.failureListener.failed(result.error);
				}
			}

			@Override
			public CharSequence convertResultToString(Object value) {
				return value instanceof Item ? value.toString() : "";
			}
		};
	}

	/**
	 * 在文本变化时先更新输入版本，避免旧查询覆盖新输入；取消不持有内部锁。
	 *
	 * @param text 当前输入
	 * @param active 当前是否允许建议查询
	 * @param completing 是否正在完成候选选择，此时保留条目供点击事件读取
	 */
	public void prepare(CharSequence text, boolean active, boolean completing) {
		CancellationSignal pending;
		synchronized (lock) {
			constraint = text == null ? "" : text.toString().trim();
			this.active = active && !closed;
			revision++;
			pending = cancellation;
			cancellation = null;
		}
		if (pending != null) {
			pending.cancel();
		}
		if (!completing) {
			items = Collections.emptyList();
			// 同步失效通知会关闭弹层，让输入框误判为已完成选择而跳过本次文本过滤。
			notifyDataSetChanged();
		}
	}

	/**
	 * 永久取消查询并释放失败监听；晚到结果不再交付。
	 */
	public void close() {
		synchronized (lock) {
			closed = true;
		}
		prepare("", false, false);
		failureListener = null;
	}

	/**
	 * 读取最近一次有效查询失败原因。
	 *
	 * @return 无错误时为空文本
	 */
	public String getLastError() {
		return lastError;
	}

	@Override
	public int getCount() {
		return items.size();
	}

	@Override
	public Item getItem(int position) {
		return position >= 0 && position < items.size() ? items.get(position) : null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		TextView view = convertView instanceof TextView ? (TextView) convertView
				: (TextView) LayoutInflater.from(parent.getContext()).inflate(
						android.R.layout.simple_dropdown_item_1line, parent, false);
		Item item = getItem(position);
		view.setText(item == null ? "" : item.toString());
		return view;
	}

	@Override
	public Filter getFilter() {
		return filter;
	}

	/**
	 * 查询当前输入对应的本机邮箱，权限拒绝或提供者失败时返回可观察错误。
	 *
	 * @param text 已去首尾空白的输入
	 * @return 携带输入版本的有限结果
	 */
	private Result query(String text) {
		long expected;
		CancellationSignal signal = new CancellationSignal();
		synchronized (lock) {
			expected = revision;
			if (closed || !active || text.isEmpty() || !text.equals(constraint)) {
				return new Result(-1, Collections.emptyList(), "");
			}
			cancellation = signal;
		}
		try {
			if (context.checkSelfPermission(Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
				return new Result(expected, Collections.emptyList(), "未授予读取联系人权限，可继续手动输入邮箱");
			}
			// 现行联系人查询和取消接口均已覆盖最低支持版本，不再依赖旧 Contacts 或自动重查游标。
			Uri uri = Email.CONTENT_FILTER_URI.buildUpon().appendPath(text)
					.appendQueryParameter(ContactsContract.LIMIT_PARAM_KEY, String.valueOf(MAX_RESULTS)).build();
			List<Item> found = new ArrayList<>();
			Set<String> addresses = new HashSet<>();
			String sort = Email.DISPLAY_NAME_PRIMARY + " COLLATE LOCALIZED ASC, " + Email.ADDRESS + " ASC";
			try (Cursor cursor = context.getContentResolver().query(uri, PROJECTION, null, null, sort, signal)) {
				if (cursor == null) {
					return new Result(expected, Collections.emptyList(), "联系人查询未返回结果");
				}
				while (found.size() < MAX_RESULTS && cursor.moveToNext()) {
					signal.throwIfCanceled();
					String address = cursor.getString(1);
					if (address == null || address.trim().isEmpty()) {
						continue;
					}
					address = address.trim();
					if (addresses.add(address)) {
						String name = cursor.getString(0);
						found.add(new Item(name == null ? "" : name.trim(), address));
					}
				}
			}
			return new Result(expected, found, "");
		} catch (OperationCanceledException cancelled) {
			return new Result(-1, Collections.emptyList(), "");
		} catch (RuntimeException failure) {
			return new Result(expected, Collections.emptyList(),
					"联系人查询失败：" + failure.getClass().getSimpleName() + "：" + String.valueOf(failure.getMessage()));
		} finally {
			synchronized (lock) {
				if (cancellation == signal) {
					cancellation = null;
				}
			}
		}
	}
}
