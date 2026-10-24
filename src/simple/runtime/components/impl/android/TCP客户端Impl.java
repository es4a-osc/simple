package simple.runtime.components.impl.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import simple.runtime.android.MainActivity;
import simple.runtime.components.TCP客户端;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.TcpTransport;
import simple.runtime.events.EventDispatcher;

/**
 * 将 TCP 客户端传输结果交付到主线程，协调宿主销毁与连接释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class TCP客户端Impl extends 组件Impl implements TCP客户端, MainActivity.OnDestroyListener {

	private final MainActivity activity;
	private final Handler handler;
	private final TcpTransport.Client transport;
	private boolean attached;
	private volatile boolean destroyed;

	/**
	 * 创建传输组件并延后登记宿主监听，避免修改正在分发的监听集合。
	 *
	 * @param container 所属组件容器
	 */
	public TCP客户端Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		handler = new Handler(Looper.getMainLooper());
		transport = new TcpTransport.Client(new Executor() {
			@Override
			public void execute(Runnable action) {
				handler.post(action);
			}
		}, new TcpTransport.ClientListener() {
			@Override
			public void connected(boolean result) {
				连接完毕(result);
			}

			@Override
			public void received(byte[] data) {
				收到数据(data);
			}

			@Override
			public void disconnected() {
				连接断开();
			}

			@Override
			public void sent(int length) {
				发送完毕(length);
			}

			@Override
			public void failed(String operation, String message) {
				操作失败(operation, message);
			}
		});
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (!destroyed) {
					if (activity.isFinishing() || activity.isDestroyed()) {
						销毁();
						return;
					}
					activity.addOnDestroyListener(TCP客户端Impl.this);
					attached = true;
				}
			}
		});
	}

	/* 组件事件 */

	@Override
	public void 连接完毕(boolean result) {
		EventDispatcher.dispatchEvent(this, "连接完毕", result);
	}

	@Override
	public void 收到数据(byte[] data) {
		EventDispatcher.dispatchEvent(this, "收到数据", data);
	}

	@Override
	public void 连接断开() {
		EventDispatcher.dispatchEvent(this, "连接断开");
	}

	@Override
	public void 发送完毕(int length) {
		EventDispatcher.dispatchEvent(this, "发送完毕", length);
	}

	@Override
	public void 操作失败(String operation, String message) {
		EventDispatcher.dispatchEvent(this, "操作失败", operation, message);
	}

	/* 组件属性 */

	@Override
	public boolean 正在连接() {
		return transport.isConnecting();
	}

	@Override
	public String 错误信息() {
		return transport.getLastError();
	}

	/* 组件方法 */

	@Override
	public boolean 置接收数据包大小(int size) {
		return transport.setBufferSize(size);
	}

	@Override
	public boolean 连接服务器(String address, int port, int timeout) {
		return transport.connect(address, port, timeout);
	}

	@Override
	public boolean 取连接状态() {
		return transport.isConnected();
	}

	@Override
	public boolean 发送数据(byte[] data) {
		return transport.send(data);
	}

	@Override
	public void 断开连接() {
		transport.disconnect();
	}

	@Override
	public void 销毁() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			transport.destroy();
			handler.post(new Runnable() {
				@Override
				public void run() {
					TCP客户端Impl.this.销毁();
				}
			});
			return;
		}
		if (destroyed) {
			return;
		}
		destroyed = true;
		transport.destroy();
		handler.post(new Runnable() {
			@Override
			public void run() {
				if (attached) {
					activity.removeOnDestroyListener(TCP客户端Impl.this);
					attached = false;
				}
			}
		});
		super.销毁();
	}

	@Override
	public void onDestroy() {
		销毁();
	}
}
