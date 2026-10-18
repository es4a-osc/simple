package simple.runtime.components.impl.android;

import android.app.PendingIntent;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;

import simple.runtime.android.MainActivity;
import simple.runtime.components.NFC读写器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * NFC 前台标签读写实现，负责页面生命周期和标签连接的释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class NFC读写器Impl extends 组件Impl implements NFC读写器,
		MainActivity.OnResumeListener, MainActivity.OnPauseListener, MainActivity.OnNewIntentListener {

	private static final AtomicInteger NEXT_REQUEST = new AtomicInteger();
	private final int pendingRequest = NEXT_REQUEST.getAndIncrement();
	private static final Charset UTF_8 = Charset.forName("UTF-8");

	// 编译平台为 API 26；Android 12 起 NFC 前台分发需要可变 PendingIntent。
	private static final int FLAG_MUTABLE = 0x02000000;
	private final MainActivity activity;
	private NfcAdapter adapter;
	private PendingIntent pendingIntent;
	private boolean initialized;
	private boolean dispatchEnabled;
	private boolean firstResume = true;
	private boolean destroyed;
	private boolean readMode;
	private String writeText;

	public NFC读写器Impl(组件容器 container) {
		super(container);

		activity = MainActivity.getContext();
	}

	/* 组件事件 */

	@Override
	public void 读取到信息(String text) {
		EventDispatcher.dispatchEvent(this, "读取到信息", text);
	}

	@Override
	public void 读取到卡号(String cardId) {
		EventDispatcher.dispatchEvent(this, "读取到卡号", cardId);
	}

	/* 组件方法 */

	@Override
	public int 初始化NFC() {
		if (destroyed || Build.VERSION.SDK_INT < 10) {
			return 1;
		}

		adapter = NfcAdapter.getDefaultAdapter(activity);
		if (adapter == null) {
			return 1;
		}

		if (!adapter.isEnabled()) {
			return 2;
		}

		// 生命周期监听仅注册一次，重复初始化只检查当前设备状态。
		if (!initialized) {
			int flags = PendingIntent.FLAG_UPDATE_CURRENT;
			if (Build.VERSION.SDK_INT >= 31) {
				flags |= FLAG_MUTABLE;
			}

			pendingIntent = PendingIntent.getActivity(activity, pendingRequest,
					new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), flags);

			activity.addOnResumeListener(this);
			activity.addOnPauseListener(this);
			activity.addOnNewIntentListener(this);
			initialized = true;
		}

		if (activity.hasWindowFocus()) {
			onResume();
		}

		return 3;
	}

	@Override
	public void 置读取模式() {
		readMode = true;
		writeText = null;
	}

	@Override
	public void 置写入模式(String text) {
		readMode = false;
		writeText = text == null ? "" : text;
	}

	@Override
	public void 销毁() {
		if (!destroyed) {
			onPause();
			destroyed = true;
			if (initialized) {
				activity.removeOnResumeListener(this);
				activity.removeOnPauseListener(this);
				activity.removeOnNewIntentListener(this);
			}

			if (pendingIntent != null) {
				pendingIntent.cancel();
			}
		}

		super.销毁();
	}

	/* 内部接口与平台回调 */

	@Override
	public void onResume() {
		if (!initialized || destroyed || !adapter.isEnabled()) {
			return;
		}

		if (!dispatchEnabled) {
			// 不限制技术类型，统一处理 NDEF、TECH 与 TAG 三种发现入口。
			adapter.enableForegroundDispatch(activity, pendingIntent, null, null);
			dispatchEnabled = true;
		}

		if (firstResume) {
			firstResume = false;
			onNewIntent(activity.getIntent());
		}
	}

	@Override
	public void onPause() {
		if (dispatchEnabled) {
			adapter.disableForegroundDispatch(activity);
			dispatchEnabled = false;
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		if (!initialized || destroyed || intent == null) {
			return;
		}

		String action = intent.getAction();
		if (!NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)
				&& !NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
				&& !NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {
			return;
		}

		Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
		if (tag == null) {
			return;
		}

		byte[] id = tag.getId();
		if (id != null && id.length > 0) {
			// 保留原卡号约定：小端字节序转无符号十进制，避免长卡号溢出。
			byte[] reversed = new byte[id.length];

			for (int i = 0; i < id.length; i++) {
				reversed[i] = id[id.length - 1 - i];
			}

			读取到卡号(new BigInteger(1, reversed).toString());
		}

		// 卡号与内容分别报告；读写模式互斥，单次标签发现不同时执行读和写。
		if (readMode) {
			读取到信息(processIntent(intent));
		} else if (writeText != null) {
			if (writeText.isEmpty()) {
				读取到信息("请输入您要写入标签的信息");
			} else {
				NdefRecord record = new NdefRecord(NdefRecord.TNF_MIME_MEDIA,
						"text/plain".getBytes(UTF_8), new byte[0], writeText.getBytes(UTF_8));
				writeTag(new NdefMessage(new NdefRecord[] { record }), tag);
			}
		}
	}

	/* 内部辅助方法 */

	/**
	 * 读取 NFC 意图中首条 NDEF 消息的首个记录并解码文本。
	 *
	 * @param intent 系统意图
	 * @return 解码后的文本；没有可读取记录或载荷不完整时为空文本。
	 */
	private String processIntent(Intent intent) {
		Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		if (messages == null || messages.length == 0 || !(messages[0] instanceof NdefMessage)) {
			return "";
		}

		NdefRecord[] records = ((NdefMessage) messages[0]).getRecords();
		if (records.length == 0) {
			return "";
		}

		NdefRecord record = records[0];
		byte[] payload = record.getPayload();

		if (record.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(record.getType(), NdefRecord.RTD_TEXT)) {
			if (payload.length == 0) {
				return "";
			}

			int languageLength = payload[0] & 0x3f;
			int start = languageLength + 1;

			if (start > payload.length) {
				return "";
			}

			Charset encoding = (payload[0] & 0x80) == 0 ? UTF_8 : Charset.forName("UTF-16");
			return new String(payload, start, payload.length - start, encoding);
		}

		// 兼容旧组件直接写入 MIME 记录的裸文本。
		return new String(payload, UTF_8);
	}

	/**
	 * 将 NDEF 消息写入指定标签。
	 *
	 * @param message 消息文本
	 * @param tag 附加标记或 NFC 标签
	 */
	private void writeTag(NdefMessage message, Tag tag) {
		// 已格式化标签先校验写权限和容量；所有退出路径都关闭标签连接。
		Ndef ndef = Ndef.get(tag);
		if (ndef != null) {
			try {
				ndef.connect();
				if (!ndef.isWritable()) {
					读取到信息("标签不允许写入");
				} else if (ndef.getMaxSize() < message.toByteArray().length) {
					读取到信息("写入信息超出容量");
				} else {
					ndef.writeNdefMessage(message);
					读取到信息("写入信息成功");
				}
			} catch (IOException | android.nfc.FormatException error) {
				Log.e("NFC", "写入标签失败", error);
				读取到信息("写入信息失败");
			} finally {
				try {
					ndef.close();
				} catch (IOException error) {
					Log.w("NFC", "关闭标签连接失败", error);
				}
			}

			return;
		}

		// 尚无 NDEF 格式的标签仅在设备支持格式化时尝试写入。
		NdefFormatable format = NdefFormatable.get(tag);
		if (format == null) {
			读取到信息("标签不支持NDEF");
			return;
		}

		try {
			format.connect();
			format.format(message);
			读取到信息("格式化标签并且写入信息");
		} catch (IOException | android.nfc.FormatException error) {
			Log.e("NFC", "格式化标签失败", error);
			读取到信息("格式化标签失败");
		} finally {
			try {
				format.close();
			} catch (IOException error) {
				Log.w("NFC", "关闭标签连接失败", error);
			}
		}
	}
}
