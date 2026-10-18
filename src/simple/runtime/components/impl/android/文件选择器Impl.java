package simple.runtime.components.impl.android;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.UriPermission;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.DocumentsContract;
import android.provider.OpenableColumns;
import android.util.AtomicFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import simple.runtime.android.MainActivity;
import simple.runtime.components.文件选择器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.errors.文件读写错误;
import simple.runtime.events.EventDispatcher;

/**
 * 系统文档选择、URI 访问与持久授权适配，不将内容 URI 伪装成文件路径。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 文件选择器Impl extends 组件Impl implements 文件选择器, MainActivity.OnActivityResultListener {

	// 与蓝牙、相机和语音组件已有请求区间分开，每个实例占三个请求号。
	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger(0x7000);
	private final int openRequest = NEXT_REQUEST.getAndAdd(3);
	private final int createRequest = openRequest + 1;
	private final int treeRequest = openRequest + 2;
	private final MainActivity activity;
	private final ContentResolver resolver;
	private final Handler handler;
	private final MainActivity.OnDestroyListener destroyListener;

	// 选择状态只在主线程修改；同步 URI 操作可在工作线程执行。
	private volatile int pendingRequest;
	private boolean pendingMultiple;
	private volatile boolean destroyed;
	private volatile String lastError = "";

	/**
	 * 创建组件并登记系统选择结果及 Activity 销毁监听。
	 *
	 * @param container 容纳本非可视组件的窗口。
	 */
	public 文件选择器Impl(组件容器 container) {
		super(container);
		if (treeRequest > 0x7fff) {
			throw new IllegalStateException("文件选择器请求标识已耗尽");
		}

		activity = MainActivity.getContext();
		resolver = activity.getContentResolver();
		handler = new Handler(activity.getMainLooper());
		destroyListener = new MainActivity.OnDestroyListener() {

			@Override
			public void onDestroy() {
				dispose(false);
			}
		};
		activity.addOnActivityResultListener(this);
		activity.addOnDestroyListener(destroyListener);
	}

	/* 组件事件 */

	/**
	 * 用户选择文件成功时在主线程触发，URI 不是物理文件路径。
	 *
	 * @param uris 选中文件的 content URI 数组，按返回顺序去重。
	 */
	@Override
	public void 文件选择完毕(String[] uris) {
		EventDispatcher.dispatchEvent(this, "文件选择完毕", (Object) uris);
	}

	/**
	 * 用户确定保存位置并创建空文件后触发。
	 *
	 * @param uri 系统创建的新文件 URI，尚未写入业务数据。
	 */
	@Override
	public void 文件创建完毕(String uri) {
		EventDispatcher.dispatchEvent(this, "文件创建完毕", uri);
	}

	/**
	 * 用户确认目录授权后触发，后续仅可访问授权目录及其子项。
	 *
	 * @param uri 用户授权的目录树 URI。
	 */
	@Override
	public void 目录选择完毕(String uri) {
		EventDispatcher.dispatchEvent(this, "目录选择完毕", uri);
	}

	/**
	 * 用户取消文件、创建或目录选择时触发，不作为失败处理。
	 */
	@Override
	public void 取消选择() {
		EventDispatcher.dispatchEvent(this, "取消选择");
	}

	/**
	 * 无法发起选择或返回结果无效时在主线程触发。
	 *
	 * @param message 不支持的系统、参数、启动或结果错误说明。
	 */
	@Override
	public void 选择失败(String message) {
		EventDispatcher.dispatchEvent(this, "选择失败", message);
	}

	/* 组件属性 */

	/**
	 * 当前是否有尚未返回的系统选择请求。
	 *
	 * @return 有未结束请求返回真。
	 */
	@Override
	public boolean 正在选择() {
		return !destroyed && pendingRequest != 0;
	}

	/**
	 * 最近一次失败的说明；成功开始选择或完成同步操作时清空。
	 *
	 * @return 最近错误说明。
	 */
	@Override
	public String 错误信息() {
		return lastError;
	}

	/* 组件方法 */

	/**
	 * 选择单个任意类型文件，要求 Android 4.4 或更高版本。
	 */
	@Override
	public void 选择文件() {
		选择文件("*/*");
	}

	/**
	 * 按 MIME 类型选择单个文件。
	 *
	 * @param mimeType MIME 类型，例如 text/plain、image/*，也可选择任意类型。
	 */
	@Override
	public void 选择文件(String mimeType) {
		选择文件(mimeType, false);
	}

	/**
	 * 选择一个或多个同类文件，结果通过文件选择完毕返回。
	 *
	 * @param mimeType 允许的 MIME 类型。
	 * @param multiple 是否允许返回多个文件。
	 */
	@Override
	public void 选择文件(String mimeType, boolean multiple) {
		选择文件(mimeType, multiple, "");
	}

	/**
	 * 打开系统文件选择器，可提示初始位置，不承诺系统一定采用。
	 *
	 * @param mimeType 允许的 MIME 类型。
	 * @param multiple 是否允许多选，系统提供方可能只返回一个文件。
	 * @param initialUri 文档或目录 content URI；空文本不指定，Android 8.0 起仅作为提示。
	 */
	@Override
	public void 选择文件(String mimeType, boolean multiple, String initialUri) {
		if (!canOpen(19)) {
			return;
		}
		try {
			Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType(validateMimeType(mimeType));
			intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, multiple);
			configure(intent, initialUri);
			launch(intent, openRequest, multiple);
		} catch (RuntimeException error) {
			notifyFailure("打开文件选择器失败：" + error.getMessage());
		}
	}

	/**
	 * 让用户选择保存位置并创建文件，默认 MIME 类型为 application/octet-stream。
	 *
	 * @param name 建议文件名；同名文件由系统处理，不承诺覆盖。
	 */
	@Override
	public void 创建文件(String name) {
		创建文件(name, "application/octet-stream");
	}

	/**
	 * 创建指定类型的文件，返回 URI 后再写入数据。
	 *
	 * @param name 建议文件名。
	 * @param mimeType 准确的 MIME 类型，例如 application/xml。
	 */
	@Override
	public void 创建文件(String name, String mimeType) {
		创建文件(name, mimeType, "");
	}

	/**
	 * 打开系统保存界面；取消时不会由组件额外写入数据。
	 *
	 * @param name 不含路径分隔符的建议文件名。
	 * @param mimeType 不能包含通配符的 MIME 类型。
	 * @param initialUri 空文本或 content URI，Android 8.0 起作为初始位置提示。
	 */
	@Override
	public void 创建文件(String name, String mimeType, String initialUri) {
		if (!canOpen(19)) {
			return;
		}
		try {
			if (name == null || name.trim().length() == 0 || name.contains("/") || name.contains("\\")) {
				throw new IllegalArgumentException("请提供不含路径的文件名");
			}

			String type = validateMimeType(mimeType);
			if (type.contains("*")) {
				throw new IllegalArgumentException("创建文件必须指定准确的 MIME 类型");
			}

			Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
			intent.addCategory(Intent.CATEGORY_OPENABLE);
			intent.setType(type);
			intent.putExtra(Intent.EXTRA_TITLE, name);
			configure(intent, initialUri);
			launch(intent, createRequest, false);
		} catch (RuntimeException error) {
			notifyFailure("创建文件失败：" + error.getMessage());
		}
	}

	/**
	 * 让用户授权一个目录及其子项，要求 Android 5.0 或更高版本。
	 */
	@Override
	public void 选择目录() {
		选择目录("");
	}

	/**
	 * 打开系统目录授权界面，系统禁止访问的目录仍不能选择。
	 *
	 * @param initialUri 空文本或目录 content URI，系统可忽略此提示。
	 */
	@Override
	public void 选择目录(String initialUri) {
		if (!canOpen(21)) {
			return;
		}
		try {
			Intent intent = new Intent("android.intent.action.OPEN_DOCUMENT_TREE");
			configure(intent, initialUri);
			launch(intent, treeRequest, false);
		} catch (RuntimeException error) {
			notifyFailure("选择目录失败：" + error.getMessage());
		}
	}

	/**
	 * 查询提供方给出的显示名称，目录树 URI 会转换为对应文档 URI。
	 *
	 * @param uri 文件或目录的 content URI。
	 * @return 提供方的显示名称；未提供时为空文本。
	 * @throws 文件读写错误 URI、授权或查询操作失败。
	 */
	@Override
	public String 取名称(String uri) {
		try {
			Uri document = documentUri(checkedUri(uri));

			try (Cursor cursor = resolver.query(document, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
				if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
					lastError = "";
					return cursor.getString(0);
				}
			}
			lastError = "";
			return "";
		} catch (RuntimeException error) {
			throw operationError("查询名称失败", error);
		}
	}

	/**
	 * 查询提供方报告的字节大小，云文件等可能无法提供大小。
	 *
	 * @param uri 文件 content URI。
	 * @return 字节大小；未知时为 -1，不代表空文件。
	 * @throws 文件读写错误 URI、授权或查询操作失败。
	 */
	@Override
	public long 取文件大小(String uri) {
		try {
			Uri document = documentUri(checkedUri(uri));

			try (Cursor cursor = resolver.query(document, new String[]{OpenableColumns.SIZE}, null, null, null)) {
				if (cursor != null && cursor.moveToFirst() && !cursor.isNull(0)) {
					lastError = "";
					return cursor.getLong(0);
				}
			}
			lastError = "";
			return -1;
		} catch (RuntimeException error) {
			throw operationError("查询文件大小失败", error);
		}
	}

	/**
	 * 查询 MIME 类型，目录类型为 vnd.android.document/directory。
	 *
	 * @param uri 文件或目录 content URI。
	 * @return MIME 类型；提供方未提供时为空文本。
	 * @throws 文件读写错误 URI、授权或查询操作失败。
	 */
	@Override
	public String 取文件类型(String uri) {
		try {
			String type = resolver.getType(documentUri(checkedUri(uri)));
			lastError = "";
			return type == null ? "" : type;
		} catch (RuntimeException error) {
			throw operationError("查询文件类型失败", error);
		}
	}

	/**
	 * 将文件完整读入内存；大文件优先复制到文件，云文件读取可能阻塞。
	 *
	 * @param uri 已经获得读权限的文件 content URI。
	 * @return 文件的完整字节内容。
	 * @throws 文件读写错误 URI、授权或读取失败。
	 */
	@Override
	public byte[] 读入字节集(String uri) {
		try (InputStream input = resolver.openInputStream(checkedUri(uri));
			ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			if (input == null) {
				throw new IOException("提供方未返回输入流");
			}

			copy(input, output);
			lastError = "";
			return output.toByteArray();
		} catch (IOException | RuntimeException error) {
			throw operationError("读取文档失败", error);
		}
	}

	/**
	 * 以覆盖截断模式写入文件，失败可能已经写入部分内容，不保证跨提供方原子提交。
	 *
	 * @param uri 已经获得写权限的文件 content URI。
	 * @param data 要写入的字节集，不得为空引用。
	 * @throws 文件读写错误 URI、授权、数据或写入失败。
	 */
	@Override
	public void 写出字节集(String uri, byte[] data) {
		try {
			Uri target = checkedUri(uri);
			if (data == null) {
				throw new IllegalArgumentException("写入数据不得为空引用");
			}

			try (OutputStream output = resolver.openOutputStream(target, "wt")) {
				if (output == null) {
					throw new IOException("提供方未返回输出流");
				}
				output.write(data);
			}
			lastError = "";
		} catch (IOException | RuntimeException error) {
			throw operationError("写入文档失败", error);
		}
	}

	/**
	 * 将文档流式复制到本地文件并返回绝对路径，供 EXCEL 等路径接口直接使用；目标目录须已存在。
	 *
	 * @param uri 已经获得读权限的文件 content URI。
	 * @param targetPath 相对路径以应用私有 files 目录为基准，也可传入有权限的绝对路径。
	 * @return 完成复制后的本地绝对路径。
	 * @throws 文件读写错误 URI、授权或本地文件操作失败。
	 */
	@Override
	public String 复制到文件(String uri, String targetPath) {
		AtomicFile target = null;
		FileOutputStream output = null;

		try {
			Uri source = checkedUri(uri);
			if (targetPath == null || targetPath.length() == 0) {
				throw new IllegalArgumentException("目标路径不得为空");
			}

			File destination = new File(targetPath);
			if (!destination.isAbsolute()) {
				File base = activity.getFilesDir().getCanonicalFile();
				destination = new File(base, targetPath).getCanonicalFile();
				if (!destination.getPath().startsWith(base.getPath() + File.separator)) {
					throw new IllegalArgumentException("相对目标路径不能越出应用私有目录");
				}
			}

			try (InputStream input = resolver.openInputStream(source)) {
				if (input == null) {
					throw new IOException("提供方未返回输入流");
				}

				target = new AtomicFile(destination);
				output = target.startWrite();
				copy(input, output);
				target.finishWrite(output);
				output = null;
			}
			lastError = "";
			return destination.getCanonicalPath();
		} catch (IOException | RuntimeException error) {
			if (target != null && output != null) {
				target.failWrite(output);
			}
			throw operationError("复制文档失败", error);
		}
	}

	/**
	 * 保留 URI 的读权限；用户移动、删除文档或撤销授权后仍可能失效。
	 *
	 * @param uri 系统选择返回的可持久授权 URI。
	 * @return 成功保留返回真，否则返回假并设置错误信息。
	 */
	@Override
	public boolean 保留访问授权(String uri) {
		return 保留访问授权(uri, false);
	}

	/**
	 * 保留读权限或读写权限，不会扩大系统选择时获得的授权范围。
	 *
	 * @param uri 系统选择返回的可持久授权 URI。
	 * @param writable 是否同时保留写权限，必须已经获得相应授权。
	 * @return 成功返回真；提供方不支持或权限不足返回假。
	 */
	@Override
	public boolean 保留访问授权(String uri, boolean writable) {
		try {
			Uri target = checkedUri(uri);
			Api19.persist(resolver, target, writable);
			lastError = "";
			return true;
		} catch (RuntimeException error) {
			lastError = "保留访问授权失败：" + error.getMessage();
			return false;
		}
	}

	/**
	 * 释放该 URI 自身的持久读写授权，不删除文件或目录。
	 *
	 * @param uri 先前保留授权的原始 URI。
	 * @return 成功释放返回真，没有对应记录或失败返回假。
	 */
	@Override
	public boolean 释放访问授权(String uri) {
		try {
			boolean released = Api19.release(resolver, checkedUri(uri));
			lastError = "";
			return released;
		} catch (RuntimeException error) {
			lastError = "释放访问授权失败：" + error.getMessage();
			return false;
		}
	}

	/**
	 * 列出已授权目录的直接子项 URI，不递归、不保证提供方返回顺序，要求 Android 5.0。
	 *
	 * @param directoryUri 已授权目录树或其目录文档 URI。
	 * @return 直接子项的 content URI 数组，空目录返回空数组。
	 * @throws 文件读写错误 目录、授权或查询失败。
	 */
	@Override
	public String[] 取子项(String directoryUri) {
		try {
			Uri target = checkedTreeUri(directoryUri);
			String[] children = Api21.children(resolver, target);
			lastError = "";
			return children;
		} catch (RuntimeException error) {
			throw operationError("列出目录失败", error);
		}
	}

	/**
	 * 在已授权目录内创建子文件，不额外弹出选择界面，要求 Android 5.0。
	 *
	 * @param directoryUri 已获得写权限的目录树或其目录文档 URI。
	 * @param name 建议显示名称。
	 * @param mimeType 不能包含通配符的准确 MIME 类型。
	 * @return 新文件 URI；同名文件是否重命名由提供方决定。
	 * @throws 文件读写错误 目录、授权或创建操作失败。
	 */
	@Override
	public String 创建子文件(String directoryUri, String name, String mimeType) {
		try {
			Uri parent = documentUri(checkedTreeUri(directoryUri));
			String type = validateMimeType(mimeType);
			if (name == null || name.trim().length() == 0 || name.contains("/") || name.contains("\\") || type.contains("*")) {
				throw new IllegalArgumentException("请提供有效文件名和准确 MIME 类型");
			}

			Uri result = Api21.create(resolver, parent, type, name);
			if (result == null) {
				throw new IllegalStateException("提供方未创建文件");
			}

			lastError = "";
			return result.toString();
		} catch (IOException | RuntimeException error) {
			throw operationError("创建子文件失败", error);
		}
	}

	/* 生命周期与系统结果 */

	/**
	 * 注销组件事件，延后移除监听器以避免修改 Activity 正在遍历的集合。
	 */
	@Override
	public void 销毁() {
		dispose(true);
	}

	/**
	 * 仅消费本实例正在等待的请求；结果派发延后到 Activity 遍历结束之后。
	 *
	 * @param requestCode 系统返回的请求号。
	 * @param resultCode Activity 结果状态。
	 * @param data 可能为空的返回意图。
	 */
	@Override
	public void onActivityResult(final int requestCode, final int resultCode, final Intent data) {
		if (destroyed || pendingRequest == 0 || requestCode != pendingRequest) {
			return;
		}

		final boolean multiple = pendingMultiple;
		pendingRequest = 0;
		pendingMultiple = false;

		handler.post(new Runnable() {

			@Override
			public void run() {
				if (destroyed) {
					return;
				}
				if (resultCode == Activity.RESULT_CANCELED) {
					lastError = "";
					取消选择();
					return;
				}
				if (resultCode != Activity.RESULT_OK || data == null) {
					notifyFailure("系统没有返回有效的文档选择结果");
					return;
				}

				String[] uris;

				try {
					uris = Api19.results(data, requestCode == openRequest && multiple);
					if (uris.length == 0) {
						throw new IllegalArgumentException("未返回文档 URI");
					}

					for (String uri : uris) {
						checkedUri(uri);
					}
				} catch (RuntimeException error) {
					notifyFailure("处理选择结果失败：" + error.getMessage());
					return;
				}

				// 文档结果校验完成后再派发业务回调，不把用户代码异常归为选择失败。
				lastError = "";
				if (requestCode == openRequest) {
					文件选择完毕(uris);
				} else if (requestCode == createRequest) {
					文件创建完毕(uris[0]);
				} else {
					目录选择完毕(uris[0]);
				}
			}
		});
	}

	/**
	 * 销毁后拒绝结果，不自动撤销用户主动保留的持久授权。
	 *
	 * @param removeListeners 是否主动移除 Activity 监听器。
	 */
	private void dispose(boolean removeListeners) {
		if (destroyed) {
			return;
		}

		destroyed = true;
		pendingRequest = 0;
		handler.removeCallbacksAndMessages(null);
		super.销毁();

		if (removeListeners) {
			handler.post(new Runnable() {

				@Override
				public void run() {
					activity.removeOnActivityResultListener(文件选择器Impl.this);
					activity.removeOnDestroyListener(destroyListener);
				}
			});
		}
	}

	/* 参数、流与状态校验 */

	/**
	 * 校验选择只能由主线程发起，且同一实例同时只保留一个请求。
	 *
	 * @param minimum 最低支持的系统 API。
	 * @return 可以发起请求返回真。
	 */
	private boolean canOpen(int minimum) {
		if (destroyed) {
			return false;
		}
		if (Build.VERSION.SDK_INT < minimum) {
			notifyFailure("当前系统不支持此选择功能，需要 Android API " + minimum + " 或更高版本");
			return false;
		}
		if (Looper.myLooper() != activity.getMainLooper()) {
			notifyFailure("请在主线程打开系统选择界面");
			return false;
		}
		if (pendingRequest != 0) {
			notifyFailure("当前文件选择器仍在等待系统返回结果");
			return false;
		}

		return true;
	}

	/**
	 * 添加 URI 授权请求与可选初始位置，不将字符串解释为本地路径。
	 *
	 * @param intent 待启动的系统意图。
	 * @param initialUri 空文本或初始内容 URI。
	 */
	private void configure(Intent intent, String initialUri) {
		if (initialUri == null) {
			throw new IllegalArgumentException("初始位置不得为空引用");
		}

		intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
			| Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
		if (initialUri.length() > 0) {
			Uri initial = checkedUri(initialUri);
			if (Build.VERSION.SDK_INT >= 26) {
				intent.putExtra("android.provider.extra.INITIAL_URI", initial);
			}
		}
	}

	/**
	 * 在启动前登记请求，失败时清除本次等待状态。
	 *
	 * @param intent 已配置的系统意图。
	 * @param requestCode 本实例专用的请求号。
	 * @param multiple 是否允许多个结果。
	 */
	private void launch(Intent intent, int requestCode, boolean multiple) {
		lastError = "";
		pendingRequest = requestCode;
		pendingMultiple = multiple;
		try {
			activity.startActivityForResult(intent, requestCode);
		} catch (RuntimeException error) {
			pendingRequest = 0;
			pendingMultiple = false;
			throw error;
		}
	}

	/**
	 * 将同步启动错误与后台调用错误统一在主线程派发。
	 *
	 * @param message 错误说明。
	 */
	private void notifyFailure(final String message) {
		lastError = message;
		if (Looper.myLooper() == activity.getMainLooper()) {
			if (!destroyed) {
				选择失败(message);
			}
		} else {
			handler.post(new Runnable() {

				@Override
				public void run() {
					if (!destroyed) {
						选择失败(message);
					}
				}
			});
		}
	}

	/**
	 * 校验 MIME 结构并保留系统选择所支持的通配类型。
	 *
	 * @param mimeType MIME 类型。
	 * @return 小写、无首尾空白的 MIME 类型。
	 */
	private static String validateMimeType(String mimeType) {
		if (mimeType == null) {
			throw new IllegalArgumentException("MIME 类型不得为空引用");
		}

		String value = mimeType.trim().toLowerCase(Locale.US);
		if (!value.matches("(?:[a-z0-9!#$&^_.+\\-]+|\\*)/(?:[a-z0-9!#$&^_.+\\-]+|\\*)")
			|| (value.startsWith("*/") && !"*/*".equals(value))) {
			throw new IllegalArgumentException("MIME 类型无效：" + mimeType);
		}

		return value;
	}

	/**
	 * 验证内容 URI 与当前组件状态，文件路径和 file URI 不作为授权文档处理。
	 *
	 * @param value 内容 URI 文本。
	 * @return 已校验的 URI。
	 */
	private Uri checkedUri(String value) {
		if (destroyed) {
			throw new IllegalStateException("文件选择器已销毁");
		}
		if (Build.VERSION.SDK_INT < 19) {
			throw new IllegalStateException("文档访问需要 Android 4.4 或更高版本");
		}
		if (value == null || value.length() == 0) {
			throw new IllegalArgumentException("文档 URI 不得为空");
		}

		Uri uri = Uri.parse(value);
		if (!"content".equals(uri.getScheme()) || (uri.getAuthority() == null || uri.getAuthority().length() == 0)) {
			throw new IllegalArgumentException("请使用系统返回的 content URI");
		}

		return uri;
	}

	/**
	 * 验证目录操作持有目录树 URI，防止把普通文件当作目录树使用。
	 *
	 * @param value 目录树或其子文档 URI。
	 * @return 原始授权树中的 URI。
	 */
	private Uri checkedTreeUri(String value) {
		Uri uri = checkedUri(value);
		if (Build.VERSION.SDK_INT < 21 || uri.getPathSegments().size() < 2
			|| !"tree".equals(uri.getPathSegments().get(0))) {
			throw new IllegalArgumentException("此操作需要 Android 5.0 及目录选择返回的授权树 URI");
		}

		return uri;
	}

	/**
	 * 将目录树根转换为可查询或创建子文件的文档 URI，普通文档 URI 保持原样。
	 *
	 * @param uri 内容 URI。
	 * @return 对应文档 URI。
	 */
	private Uri documentUri(Uri uri) {
		return Build.VERSION.SDK_INT >= 21 ? Api21.document(uri) : uri;
	}

	/**
	 * 按固定缓冲区复制，不把整个文件读入中间数组。
	 *
	 * @param input 输入流，由调用者关闭。
	 * @param output 输出流，由调用者关闭。
	 * @throws IOException 流读取或写入失败。
	 */
	private static void copy(InputStream input, OutputStream output) throws IOException {
		byte[] buffer = new byte[8192];
		int count;
		while ((count = input.read(buffer)) != -1) {
			if (count > 0) {
				output.write(buffer, 0, count);
			}
		}
	}

	/**
	 * 将文档或提供方错误转换为 Simple 可捕获的文件读写错误。
	 *
	 * @param message 操作说明。
	 * @param cause 原始异常。
	 * @return 可捕获的错误，错误信息属性同步保存说明。
	 */
	private 文件读写错误 operationError(String message, Throwable cause) {
		lastError = message + "：" + cause.getMessage();
		文件读写错误 error = new 文件读写错误(lastError);
		error.initCause(cause);
		return error;
	}

	/* 系统版本边界 */

	/**
	 * 隔离 Android 4.4 的文档结果和持久授权 API。
	 */
	@TargetApi(19)
	private static final class Api19 {

		private Api19() {
		}

		/**
		 * 按返回顺序去重；单选请求只接受第一个结果。
		 *
		 * @param data 系统返回意图。
		 * @param multiple 是否接受多个结果。
		 * @return 去重后的 URI 文本数组。
		 */
		static String[] results(Intent data, boolean multiple) {
			Set<String> uris = new LinkedHashSet<String>();
			ClipData clips = data.getClipData();
			if (clips != null) {
				for (int index = 0; index < clips.getItemCount(); index++) {
					Uri uri = clips.getItemAt(index).getUri();
					if (uri != null) {
						uris.add(uri.toString());
						if (!multiple) {
							break;
						}
					}
				}
			}
			if (uris.isEmpty() && data.getData() != null) {
				uris.add(data.getData().toString());
			}
			return uris.toArray(new String[uris.size()]);
		}

		/**
		 * 仅保留已获授权的读权限或读写权限，系统负责拒绝超范围请求。
		 *
		 * @param resolver 内容访问入口。
		 * @param uri 授权 URI。
		 * @param writable 是否同时保留写权限。
		 */
		static void persist(ContentResolver resolver, Uri uri, boolean writable) {
			int flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
				| (writable ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
			resolver.takePersistableUriPermission(uri, flags);
		}

		/**
		 * 根据实际保存的权限标志释放对应 URI，不撤销其它记录。
		 *
		 * @param resolver 内容访问入口。
		 * @param uri 原始持久授权 URI。
		 * @return 找到并成功释放返回真。
		 */
		static boolean release(ContentResolver resolver, Uri uri) {
			for (UriPermission permission : resolver.getPersistedUriPermissions()) {
				if (uri.equals(permission.getUri())) {
					int flags = (permission.isReadPermission() ? Intent.FLAG_GRANT_READ_URI_PERMISSION : 0)
						| (permission.isWritePermission() ? Intent.FLAG_GRANT_WRITE_URI_PERMISSION : 0);
					resolver.releasePersistableUriPermission(uri, flags);
					return true;
				}
			}
			return false;
		}
	}

	/**
	 * 隔离 Android 5.0 的目录树访问与子文件创建 API。
	 */
	@TargetApi(21)
	private static final class Api21 {

		private Api21() {
		}

		/**
		 * 将只有树根的 URI 转换为根文档，子文档 URI 保持原样。
		 *
		 * @param uri 树根或文档 URI。
		 * @return 可查询的文档 URI。
		 */
		static Uri document(Uri uri) {
			List<String> segments = uri.getPathSegments();
			if (segments.size() == 2 && "tree".equals(segments.get(0))) {
				return DocumentsContract.buildDocumentUriUsingTree(uri, DocumentsContract.getTreeDocumentId(uri));
			}

			return uri;
		}

		/**
		 * 列出授权目录的直接子项，只返回 URI，不推断物理文件路径。
		 *
		 * @param resolver 内容访问入口。
		 * @param uri 树根或目录文档 URI。
		 * @return 子文档 URI 数组。
		 */
		static String[] children(ContentResolver resolver, Uri uri) {
			Uri directory = document(uri);
			String id = DocumentsContract.getDocumentId(directory);
			Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(uri, id);
			List<String> result = new ArrayList<String>();

			try (Cursor cursor = resolver.query(children, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID},
				null, null, null)) {
				if (cursor == null) {
					throw new IllegalStateException("提供方未返回目录列表");
				}
				while (cursor.moveToNext()) {
					result.add(DocumentsContract.buildDocumentUriUsingTree(uri, cursor.getString(0)).toString());
				}
			}

			return result.toArray(new String[result.size()]);
		}

		/**
		 * 在授权目录内创建指定 MIME 类型的文件。
		 *
		 * @param resolver 内容访问入口。
		 * @param parent 父目录文档 URI。
		 * @param type 准确 MIME 类型。
		 * @param name 建议文件名。
		 * @return 新文件 URI，提供方拒绝时可能为空。
		 * @throws IOException 文档提供方创建失败。
		 */
		static Uri create(ContentResolver resolver, Uri parent, String type, String name) throws IOException {
			return DocumentsContract.createDocument(resolver, parent, type, name);
		}
	}
}
