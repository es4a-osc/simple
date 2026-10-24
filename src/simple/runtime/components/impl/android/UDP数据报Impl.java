package simple.runtime.components.impl.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import simple.runtime.android.MainActivity;
import simple.runtime.components.UDP数据报;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.UdpTransport;
import simple.runtime.events.EventDispatcher;

/**
 * 将 UDP 收发结果交付到主线程，协调组件及宿主销毁时的资源释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class UDP数据报Impl extends 组件Impl implements UDP数据报, MainActivity.OnDestroyListener {

	private final MainActivity activity;
	private final Handler handler;
	private final UdpTransport transport;
	private boolean attached;
	private volatile boolean destroyed;

	/**
	 * 创建传输组件并延后登记宿主监听，避免修改正在分发的监听集合。
	 *
	 * @param container 所属组件容器
	 */
	public UDP数据报Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		handler = new Handler(Looper.getMainLooper());
		transport = new UdpTransport(new Executor() {
			@Override
			public void execute(Runnable action) {
				handler.post(action);
			}
		}, new UdpTransport.Listener() {
			@Override
			public void started(boolean result, int port) {
				启动完毕(result, port);
			}

			@Override
			public void received(byte[] data, String address, int port) {
				收到数据(data, address, port);
			}

			@Override
			public void sent(int length, String address, int port) {
				发送完毕(length, address, port);
			}

			@Override
			public void stopped() {
				监听停止();
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
					activity.addOnDestroyListener(UDP数据报Impl.this);
					attached = true;
				}
			}
		});
	}

	/* 组件事件 */

	@Override
	public void 启动完毕(boolean result, int port) {
		EventDispatcher.dispatchEvent(this, "启动完毕", result, port);
	}

	@Override
	public void 收到数据(byte[] data, String address, int port) {
		EventDispatcher.dispatchEvent(this, "收到数据", data, address, port);
	}

	@Override
	public void 发送完毕(int length, String address, int port) {
		EventDispatcher.dispatchEvent(this, "发送完毕", length, address, port);
	}

	@Override
	public void 监听停止() {
		EventDispatcher.dispatchEvent(this, "监听停止");
	}

	@Override
	public void 操作失败(String operation, String message) {
		EventDispatcher.dispatchEvent(this, "操作失败", operation, message);
	}

	/* 组件属性 */

	@Override
	public boolean 正在监听() {
		return transport.isListening();
	}

	@Override
	public int 监听端口() {
		return transport.getLocalPort();
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
	public boolean 开始监听(int port) {
		return transport.start(port);
	}

	@Override
	public boolean 发送数据(byte[] data, String address, int port) {
		return transport.send(data, address, port);
	}

	@Override
	public void 停止监听() {
		transport.stop();
	}

	@Override
	public void 销毁() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			transport.destroy();
			handler.post(new Runnable() {
				@Override
				public void run() {
					UDP数据报Impl.this.销毁();
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
					activity.removeOnDestroyListener(UDP数据报Impl.this);
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
