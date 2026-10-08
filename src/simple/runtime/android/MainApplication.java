package simple.runtime.android;

// Application是和应用关联的，为应用上下文做准备！
import android.app.Application;

/**
 * 应用清单将指点该应用类
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class MainApplication extends Application {

	// 应用上下文
	private static MainApplication INSTANCE;

	public MainApplication() {
		INSTANCE = this;
	}

	@Override
	public void onCreate()  {
		super.onCreate();
	}

	/* 公开方法 */

	/**
	 * 返回当前主活动的上下文实例。
	 *
	 * @return 主活动的上下文实例
	 */
	public static MainApplication getContext() {
		return INSTANCE;
	}
}
