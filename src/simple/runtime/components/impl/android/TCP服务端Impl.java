package simple.runtime.components.impl.android;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

import simple.runtime.android.MainActivity;
import simple.runtime.components.TCP服务端;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.TcpTransport;
import simple.runtime.events.EventDispatcher;

/**
 * 将 TCP 服务端传输结果交付到主线程，协调宿主销毁与连接释放。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class TCP服务端Impl extends 组件Impl implements TCP服务端, MainActivity.OnDestroyListener {

	private final MainActivity activity;
	private final Handler handler;
	private final TcpTransport.Server transport;
	private boolean attached;
	private volatile boolean destroyed;

	/**
	 * 创建传输组件并延后登记宿主监听，避免修改正在分发的监听集合。
	 *
	 * @param container 所属组件容器
	 */
	public TCP服务端Impl(组件容器 container) {
		super(container);
		activity = MainActivity.getContext();
		handler = new Handler(Looper.getMainLooper());
		transport = new TcpTransport.Server(new Executor() {
			@Override
			public void execute(Runnable action) {
				handler.post(action);
			}
		}, new TcpTransport.ServerListener() {
			@Override
			public void started(boolean result, int port) {
				启动完毕(result, port);
			}

			@Override
			public void stopped() {
				服务器关闭();
			}

			@Override
			public void entered(String address, int port) {
				客户进入(address, port);
			}

			@Override
			public void received(byte[] data, String address, int port) {
				收到数据(data, address, port);
			}

			@Override
			public void left(String address, int port) {
				客户离开(address, port);
			}

			@Override
			public void sent(int length, String address, int port) {
				发送完毕(length, address, port);
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
					activity.addOnDestroyListener(TCP服务端Impl.this);
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
	public void 客户进入(String address, int port) {
		EventDispatcher.dispatchEvent(this, "客户进入", address, port);
	}

	@Override
	public void 收到数据(byte[] data, String address, int port) {
		EventDispatcher.dispatchEvent(this, "收到数据", data, address, port);
	}

	@Override
	public void 客户离开(String address, int port) {
		EventDispatcher.dispatchEvent(this, "客户离开", address, port);
	}

	@Override
	public void 服务器关闭() {
		EventDispatcher.dispatchEvent(this, "服务器关闭");
	}

	@Override
	public void 发送完毕(int length, String address, int port) {
		EventDispatcher.dispatchEvent(this, "发送完毕", length, address, port);
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
		return transport.getPort();
	}

	@Override
	public int 客户数量() {
		return transport.getClientCount();
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
	public boolean 启动服务器(int port) {
		return transport.start(port);
	}

	@Override
	public boolean 发送数据(byte[] data, String address, int port) {
		return transport.send(data, address, port);
	}

	@Override
	public void 关闭服务器() {
		transport.stop();
	}

	@Override
	public void 销毁() {
		if (Looper.myLooper() != Looper.getMainLooper()) {
			transport.destroy();
			handler.post(new Runnable() {
				@Override
				public void run() {
					TCP服务端Impl.this.销毁();
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
					activity.removeOnDestroyListener(TCP服务端Impl.this);
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
