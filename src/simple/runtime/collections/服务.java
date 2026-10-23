package simple.runtime.collections;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Process;
import simple.runtime.日志输出;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 由 Android 系统实例化的 Simple 服务单元基类，依次处理启动意图，队列完成后自动停止。
 *
 * <p>服务创建和服务销毁在主线程执行，服务处理在同一个工作线程串行执行。
 * 不提供绑定服务、任务持久化或进程保活；不能通过创建普通对象替代系统启动服务。
 * API 26 及以上受普通服务的后台启动与执行限制，本类不会自动升级为前台通知服务。
 * 服务与主 Activity 默认位于同一进程，显式调用终止应用会结束同一进程。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public class 服务 extends Service {

	private HandlerThread worker;
	private Handler requests;
	private volatile boolean destroyed;

	/**
	 * 保留公开无参数构造方法，供系统创建编译器生成的服务子类；此时尚不能使用服务上下文。
	 */
	public 服务() {
		super();
	}

	/**
	 * 服务实例创建完成后在主线程调用，服务单元可覆写此过程进行初始化。
	 */
	@SimpleFunction
	public void 服务创建() {
	}

	/**
	 * 在工作线程按启动顺序处理一个意图，不直接操作界面；服务单元应覆写此过程。
	 *
	 * <p>过程正常返回后继续处理后续任务，全部处理完毕后自动停止服务。
	 * 外部停止或系统销毁会取消尚未开始的任务，并向工作线程发送中断信号；
	 * 正在执行的操作需要自行响应中断，不保证在服务销毁前已经退出。
	 *
	 * @param intent 本次非空启动意图
	 */
	@SimpleFunction
	public void 服务处理(意图 intent) {
		日志输出.warning("服务单元未覆写服务处理；单元类名=%s", getClass().getName());
	}

	/**
	 * 服务销毁时在主线程调用，服务单元可覆写此过程释放资源。
	 *
	 * <p>未开始的任务已取消；正在执行的处理可能仍在响应中断。
	 * 进程直接被系统终止时不保证调用此过程。
	 */
	@SimpleFunction
	public void 服务销毁() {
	}

	/**
	 * 创建串行工作线程，再通知服务单元；不依赖主 Activity 已经创建。
	 */
	@Override
	public void onCreate() {
		super.onCreate();
		destroyed = false;
		try {
			// 原 IntentService 在 API 30 弃用；Service 与显式 HandlerThread 覆盖全部支持版本。
			worker = new HandlerThread("SimpleService-" + getClass().getName(), Process.THREAD_PRIORITY_BACKGROUND);
			worker.start();
			requests = new Handler(worker.getLooper());

			日志输出.info("服务被创建；单元类名=%s", getClass().getName());
			服务创建();
		} catch (RuntimeException | Error failure) {
			destroyed = true;
			closeWorker();
			throw failure;
		}
	}

	/**
	 * 将系统启动请求交给同一工作队列，不在主线程执行服务处理。
	 *
	 * @param intent 启动意图；空意图只完成对应启动请求，不调用服务处理
	 * @param flags 系统启动标志
	 * @param startId 本次启动编号，用于避免旧任务完成时停止新任务
	 * @return 非粘性启动模式；进程被终止后不要求系统自动重建或重放任务
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (destroyed || requests == null || !requests.post(() -> handleRequest(intent, startId))) {
			日志输出.warning("服务工作队列不可用；启动编号=%d", startId);
			stopSelfResult(startId);
		}
		return START_NOT_STICKY;
	}

	/**
	 * 本类仅处理启动意图，不提供绑定接口。
	 *
	 * @param intent 绑定请求
	 * @return 始终为空
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	/**
	 * 取消未开始的任务并中断工作线程，再通知服务单元；不阻塞主线程等待用户任务。
	 */
	@Override
	public void onDestroy() {
		destroyed = true;
		closeWorker();
		try {
			日志输出.info("服务被销毁；单元类名=%s", getClass().getName());
			服务销毁();
		} finally {
			super.onDestroy();
		}
	}

	/**
	 * 处理一次请求；完成旧启动编号时不能停止后续已经到达的请求。
	 *
	 * @param intent 系统启动意图，可为空
	 * @param startId 系统启动编号
	 */
	private void handleRequest(Intent intent, int startId) {
		if (destroyed) {
			return;
		}
		try {
			if (intent != null) {
				服务处理(new 意图(intent));
			}
		} finally {
			if (!destroyed) {
				// 是否停止取决于系统最新启动编号，不能使用无编号 stopSelf() 提前丢弃队列。
				stopSelfResult(startId);
			}
		}
	}

	/**
	 * 丢弃排队请求，退出消息循环并通知正在执行的任务中断；不强制终止用户代码。
	 */
	private void closeWorker() {
		if (requests != null) {
			requests.removeCallbacksAndMessages(null);
			requests = null;
		}
		if (worker != null) {
			worker.quit();
			worker.interrupt();
			worker = null;
		}
	}
}
