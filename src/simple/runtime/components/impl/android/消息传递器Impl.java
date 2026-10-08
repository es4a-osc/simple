package simple.runtime.components.impl.android;

import simple.runtime.components.消息传递器;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.events.EventDispatcher;
import simple.runtime.variants.Variant;

import android.os.Handler;
import android.os.Message;

/**
 * 消息传递器组件的 Android 实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 消息传递器Impl extends 组件Impl implements 消息传递器 {

	private Handler handler;

	/**
	 * 创建新的组件。
	 *
	 * @param container 容纳组件的容器（对于不可见的组件必须是窗口，不可为 {@code null}）
	 */
	public 消息传递器Impl(组件容器 container) {
		super(container);
		
		handler = new Handler() {
			@Override
			public void handleMessage(Message msg) {
				收到消息(msg.what, (Variant) msg.obj);
			};
		};
	}

	@Override
	public void 收到消息(int what, Variant value) {
		EventDispatcher.dispatchEvent(this, "收到消息", what, value);
	}

	@Override
	public boolean 发送消息(int what, Variant value) {
		Message msg = Message.obtain();
		msg.what = what;
		msg.obj = value;
		return handler.sendMessage(msg);
	}

	@Override
	public boolean 发送消息(int what) {
		return 发送消息(what, null);
	}
}
