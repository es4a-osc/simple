package simple.runtime.collections;

import simple.runtime.日志输出;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

import android.app.IntentService;
import android.content.Intent;

/**
 * Simple的服务对象，注意该对象将由系统实例化。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public class 服务 extends IntentService {
	/*
	android.app.IntentService:
	https://developer.android.google.cn/reference/android/app/IntentService.html

	关联代码：
	simple.compiler.scanner.TokenKind#TOK_$SERVICE
	simple.compiler.scanner.Keywords#Keywords()
	simple.compiler.parser.Parser#parseServicePropertiesSection()
	simple.compiler.parser.Parser#parse()

	IntentService是一个用于按需处理异步请求的Service基类， 调用方通过 startService(Intent) 启动服务，
	IntentService为每一个Intent开启一个单独的工作线程，并且在任务完成时自动终止服务。

	这种“工作队列处理器”模式通常用于某个程序主线程之外的后台任务。IntentService类简化了这种机制。
	要使用这种工作队列模式，只使用继承IntentService并实现onHandleIntent(Intent)方法。
	IntentService会接受Intents，启动工作线程，并在合适的时候终止服务。

	startService的生命周期： onCreate -> onStartCommand -> onDestroy

	1.service 一直在后台运行，不会跟随Activity一起销毁
	2.多次调用sartService，只执行一次OnCreate 但是每次都会执行onStartCommand
	3.可以在广播中startService方式启动服务
	*/

	/**
	 * 取当前服务单元的类名
	 *
	 * @return 成功返回类名，失败返回{@code null}
	 */
	private static String getClassName() {
		StackTraceElement[] ste = Thread.currentThread().getStackTrace();
		return ste.length > 4 ? ste[4].getClassName() : "IntentService";
	}

	/*
	 * 注意该构造方法必须公开
	 */
	public 服务() {
		super(getClassName());
		
		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/服务] 构造对象；服务名称=" +  getClassName());
	}

	/**
	 * 服务被创建时执行该方法，继承单元请覆写该{@code 过程}。
	 */
	@SimpleFunction
	public void 服务创建() {

	}

	/**
	 * 收到意图将执行该方法，继承单元请覆写该{@code 过程}。
	 *
	 * @param intent 意图实例。
	 */
	@SimpleFunction
	public void 服务处理(意图 intent) {
		日志输出.输出警告(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/服务.服务处理] 服务单元未覆写该过程；单元类名=" + getClass().getName());
	}

	/**
	 * 服务将被销毁时执行该方法，继承单元请覆写该{@code 过程}。
	 */
	@SimpleFunction
	public void 服务销毁() {
	}

	/**
	 * 创建；首次创建服务时由系统调用。
	 */
	@Override
	public void onCreate() {
		super.onCreate();

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/服务.onCreate] 服务被创建");
		服务创建();
	}

	/**
	 * 处理意图；在工作线程上调用此方法并请求处理。
	 */
	@Override
	protected void onHandleIntent(Intent intent) {
		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/服务.onHandleIntent] 处理意图");

		if (intent != null) {
			服务处理(new 意图(intent));
		}
	}

	/**
	 * 销毁；由系统调用以通知服务它已不再使用并正在被删除。
	 */
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "[" + hashCode() + "/服务.onDestroy] 服务被销毁");
		服务销毁();
	}
}
