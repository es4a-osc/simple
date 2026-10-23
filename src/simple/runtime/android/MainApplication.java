package simple.runtime.android;

import android.app.Application;
import android.content.Context;

/**
 * 由应用清单注册、Android 系统创建的运行库 Application，提供当前进程的应用上下文。
 *
 * <p>应用上下文不依赖主 Activity 的创建与销毁，不用于需要窗口主题或窗口令牌的界面操作。
 * 多进程应用中，每个进程持有自己的实例。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class MainApplication extends Application {

	// 基础上下文附加后才发布实例，供主线程和后台线程读取。
	private static volatile MainApplication INSTANCE;

	/**
	 * 供 Android 创建应用实例；构造期间尚未附加基础上下文，不发布当前对象。
	 */
	public MainApplication() {
		super();
	}

	/**
	 * 附加系统提供的基础上下文后，发布当前进程的应用实例。
	 *
	 * <p>发布时机早于 onCreate()，允许先创建的内容提供器取得已附加上下文的应用实例。
	 *
	 * @param base 系统提供的基础上下文
	 */
	@Override
	protected void attachBaseContext(Context base) {
		super.attachBaseContext(base);
		INSTANCE = this;
	}

	/**
	 * 返回当前进程已附加基础上下文的应用实例。
	 *
	 * @return 应用上下文；系统尚未完成基础上下文附加时返回空
	 */
	public static MainApplication getContext() {
		return INSTANCE;
	}
}
