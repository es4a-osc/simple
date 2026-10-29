/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.runtime.android;

import simple.runtime.ApplicationFunctions;
import simple.runtime.文件操作;
import simple.runtime.应用操作;
import simple.runtime.日志输出;
import simple.runtime.collections.意图;
import simple.runtime.components.窗口;
import simple.runtime.components.组件;
import simple.runtime.components.impl.android.窗口Impl;
import simple.runtime.parameters.BooleanReferenceParameter;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.Variant;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.DisplayMetrics;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowMetrics;
import android.graphics.Insets;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.lang.reflect.InvocationTargetException;

/**
 * 组件基础活动和UI应用程序。
 *
 * <p>这是任何Android活动的根容器，也是Simple/AndroidUI应用程序的超类。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class MainActivity extends Activity implements ApplicationFunctions {
	/**
	 * 重启监听器
	 *
	 * <p>用于将 Activity onRestart() 方法分发给感兴趣的组件。
	 */
	public interface OnRestartListener {
		/**
		 * 响应活动重新启动通知。
		 */
		public void onRestart();
	}

	/**
	 * 恢复监听器
	 *
	 * <p>用于将 Activity OnResume() 方法分发给感兴趣的组件。
	 */
	public interface OnResumeListener {
		/**
		 * 响应活动恢复到前台的通知。
		 */
		public void onResume();
	}

	/**
	 * 暂停监听器
	 *
	 * <p>用于将 Activity onPause() 方法分配给感兴趣的组件。
	 */
	public interface OnPauseListener {
		/**
		 * 响应活动暂停通知。
		 */
		public void onPause();
	}

	/**
	 * 停止监听器
	 *
	 * <p>用于将 Activity onStop() 方法分配给感兴趣的组件。
	 */
	public interface OnStopListener {
		/**
		 * 响应活动停止通知。
		 */
		public void onStop();
	}

	/**
	 * 销毁监听器
	 *
	 * <p>用于将 Activity onDestroy() 方法分发给感兴趣的组件。
	 */
	public interface OnDestroyListener {
		/**
		 * 响应宿主销毁通知，处理本对象的资源清理。
		 */
		public void onDestroy();
	}

	/**
	 * 保存实例状态监听器
	 *
	 * <p>用于将 Activity onSaveInstanceState() 方法分发给感兴趣的组件。
	 */
	public interface OnSaveInstanceStateListener {
		/**
		 * 保存活动重建需要恢复的状态。
		 *
		 * @param paramBundle 需要保存的活动状态
		 */
		public void onSaveInstanceState(Bundle paramBundle);
	}

	/**
	 * 恢复实例状态监听器
	 *
	 * <p>用于将 Activity onRestoreInstanceState() 方法分发给感兴趣的组件。
	 */
	public interface OnRestoreInstanceStateListener {
		/**
		 * 恢复活动先前保存的状态。
		 *
		 * @param savedInstanceState 此前保存的活动状态
		 */
		public void onRestoreInstanceState(Bundle savedInstanceState);
	}

	/**
	 * 配置更改监听器
	 *
	 * <p>用于将 Activity onConfigurationChanged() 方法分发给感兴趣的组件。
	 */
	public interface OnConfigurationChangedListener {
		/**
		 * 处理屏幕方向等配置变化。
		 *
		 * @param newConfig 变化后的系统配置
		 */
		public void onConfigurationChanged(Configuration newConfig);
	}

	/**
	 * 新意图监听器
	 *
	 * <p>用于将 Activity onNewIntent() 方法分发给感兴趣的组件。
	 */
	public interface OnNewIntentListener {
		/**
		 * 处理宿主收到的新启动意图。
		 *
		 * @param intent 系统意图
		 */
		public void onNewIntent(Intent intent);
	}

	/**
	 * 活动结果监听器
	 *
	 * <p>用于将 Activity onActivityResult() 方法分发给感兴趣的组件。
	 */
	public interface OnActivityResultListener {
		/**
		 * 处理由本组件发起的活动返回结果。
		 *
		 * @param requestCode 发起操作时使用的请求编号
		 * @param resultCode 系统返回的结果编号
		 * @param data 本次传入的数据
		 */
		public void onActivityResult(int requestCode, int resultCode, Intent data);
	}

	/**
	 * 请求权限结果监听器
	 *
	 * <p>用于将 Activity onRequestPermissionsResult() 方法分发给感兴趣的组件。
	 */
	public interface OnRequestPermissionsResultListener {
		/**
		 * 处理系统返回的权限请求结果。
		 *
		 * @param requestCode 发起操作时使用的请求编号
		 * @param permissions 本次请求的权限名称
		 * @param grantResults 与权限名称逐项对应的授权结果
		 */
		public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults);
	}

	// 活动上下文
	private static MainActivity INSTANCE;

	// 生命周期监听按本轮快照分发；回调内增删不会改变本轮顺序，新监听器从下一轮生效。
	// 活动重启监听器
	private final List<OnRestartListener> onRestartListeners;
	// 活动恢复监听器
	private final List<OnResumeListener> onResumeListeners;
	// 活动暂停监听器
	private final List<OnPauseListener> onPauseListeners;
	// 活动停止监听器
	private final List<OnStopListener> onStopListeners;
	// 活动销毁监听器
	private final List<OnDestroyListener> onDestroyListeners;
	// 活动保存实例状态监听器
	private final List<OnSaveInstanceStateListener> onSaveInstanceStateListeners;
	// 活动恢复实例状态监听器
	private final List<OnRestoreInstanceStateListener> onRestoreInstanceStateListeners;
	// 配置更改监听器
	private final List<OnConfigurationChangedListener> onConfigurationChangedListeners;
	// 活动新意图监听器
	private final List<OnNewIntentListener> onNewIntentListeners;
	// 活动结果监听器
	private final List<OnActivityResultListener> onActivityResultListeners;
	// 活动请求权限结果监听器
	private final List<OnRequestPermissionsResultListener> onRequestPermissionsResultListeners;

	// 包含菜单项标题的列表
	private final List<String> menuItems;

	// 触摸手势探测器
	private GestureDetector gestureDetector;

	// 应用程序的根视图
	private ViewGroup rootView;

	// 应用程序的内容视图（根视图的单独子级）
	private View contentView;

	// 当前活跃窗口
	private 窗口Impl activeForm;

	// 当前是否在前
	private boolean inForeground;

	// 只有显式调用终止应用才结束进程，普通活动销毁不连带结束服务。
	private boolean terminationRequested;

	/**
	 * 返回当前主活动的上下文实例。
	 *
	 * @return 活动上下文
	 */
	public static MainActivity getContext() {
		return INSTANCE;
	}

	/**
	 * 创建新应用程序。
	 */
	public MainActivity() {
		INSTANCE = this;

		// 初始化变量
		onRestartListeners = new CopyOnWriteArrayList<OnRestartListener>();
		onResumeListeners = new CopyOnWriteArrayList<OnResumeListener>();
		onPauseListeners = new CopyOnWriteArrayList<OnPauseListener>();
		onStopListeners = new CopyOnWriteArrayList<OnStopListener>();
		onDestroyListeners = new CopyOnWriteArrayList<OnDestroyListener>();
		onSaveInstanceStateListeners = new CopyOnWriteArrayList<OnSaveInstanceStateListener>();
		onRestoreInstanceStateListeners = new CopyOnWriteArrayList<OnRestoreInstanceStateListener>();
		onConfigurationChangedListeners = new CopyOnWriteArrayList<OnConfigurationChangedListener>();
		onNewIntentListeners = new CopyOnWriteArrayList<OnNewIntentListener>();
		onActivityResultListeners = new CopyOnWriteArrayList<OnActivityResultListener>();
		onRequestPermissionsResultListeners = new CopyOnWriteArrayList<OnRequestPermissionsResultListener>();

		menuItems = new ArrayList<String>();
	}

	/**
	 * 创建活动、运行库适配器和主窗口，再分发首次启动意图。
	 *
	 * @param icicle 系统保存的活动状态；首次创建时为空
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		gestureDetector = new GestureDetector(this, new SimpleOnGestureListener() {
			@Override
			public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
				// 子视图可能先消费按下事件，窗口只收到后续触摸片段。
				// 缺少起止坐标时不推断方向，继续交由后续处理。
				if (e1 == null || e2 == null) {
					return false;
				}

				int direction;
				int deltaX = (int) (e1.getRawX() - e2.getRawX());
				int deltaY = (int) (e1.getRawY() - e2.getRawY());

				if (Math.abs(deltaX) > Math.abs(deltaY)) {
					// 水平移动
					direction = deltaX > 0 ? 组件.触摸_左滑 : 组件.触摸_右滑;
				} else {
					// 垂直移动
					direction = deltaY > 0 ? 组件.触摸_上滑 : 组件.触摸_下滑;
				}

				if (activeForm != null) {
					activeForm.触摸手势(direction);
				}
				return true;
			}

			@Override
			public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
				int direction;
				if (Math.abs(distanceX) > Math.abs(distanceY)) {
					// 水平移动
					direction = distanceX > 0 ? 组件.触摸_左移 : 组件.触摸_右移;
				} else {
					// 垂直移动
					direction = distanceY > 0 ? 组件.触摸_上移 : 组件.触摸_下移;
				}

				if (activeForm != null) {
					activeForm.触摸手势(direction);
				}
				return true;
			}

			@Override
			public boolean onSingleTapConfirmed(MotionEvent e) {
				if (activeForm != null) {
					activeForm.触摸手势(组件.触摸_单击);
				}
				return true;
			}

			@Override
			public boolean onDoubleTap(MotionEvent e) {
				if (activeForm != null) {
					activeForm.触摸手势(组件.触摸_双击);
				}
				return true;
			}
		});

		// 现有同步网络函数仍依赖此策略；迁移调用方为异步之前保留，不把放宽策略当作异步处理。
		StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		StrictMode.setThreadPolicy(policy);

		// 初始化运行时组件
		应用操作.initialize(this);
		日志输出.initialize(new LogImpl(this));
		文件操作.initialize(getFilesDir());

		// 我们需要利用根视图，以便我们可以删除实际的窗口布局容器，并在可滚动视图内重新添加。否则无法删除内容视图。
		rootView = new android.widget.FrameLayout(this);
		setContentView(rootView, new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		));

		// 获取主窗口信息并切换到它
		try {
			Bundle metaData = getPackageManager().getActivityInfo(getComponentName(), PackageManager.GET_META_DATA).metaData;
			String mainFormName = metaData == null ? null : metaData.getString("simple.runtime.android.MainForm");
			if (mainFormName == null || mainFormName.isEmpty()) {
				日志输出.error("应用清单缺少主窗口类名");
				finish();
				return;
			}
			日志输出.输出信息(日志输出.MODULE_NAME_RTL, "主窗口类: " + mainFormName);
			switchForm((窗口Impl) getClassLoader().loadClass(mainFormName).getDeclaredConstructor().newInstance());
		} catch (NoSuchMethodException e) {
			日志输出.error("主窗口缺少无参构造方法");
			e.printStackTrace();
			finish();
			return;
		} catch (InvocationTargetException e) {
			日志输出.error("主窗口构造方法执行失败");
			e.getCause().printStackTrace();
			finish();
			return;
		} catch (ClassNotFoundException e) {
			日志输出.error("未找到主窗口类");
			e.printStackTrace();
			finish();
			return;
		} catch (NameNotFoundException e) {
			日志输出.error("未找到主活动的清单信息");
			e.printStackTrace();
			finish();
			return;
		} catch (InstantiationException | IllegalAccessException | SecurityException | ClassCastException e) {
			日志输出.error("无法创建主窗口");
			e.printStackTrace();
			finish();
			return;
		}

		// 程序启动事件
		if (activeForm != null) {
			// 返回启动此活动的意图。
			Intent intent = getIntent();
			日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onCreate 程序被启动；行为=" + intent.getAction());

			activeForm.程序被启动(new 意图(intent));
		}
	}

	/**
	 * 重启
	 *
	 * <p>Activity从后台重新回到前台时被调用。
	 */
	@Override
	protected void onRestart() {
		super.onRestart();

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onRestart 程序被重启");

		// 分发事件
		for (OnRestartListener onRestartListener : onRestartListeners) {
			onRestartListener.onRestart();
		}
	}

	/**
	 * 恢复
	 *
	 * <p>Activity创建或者从被覆盖、后台重新回到前台时被调用。
	 */
	@Override
	protected void onResume() {
		super.onResume();
		inForeground = true;

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onResume 程序被恢复");

		// 分发事件
		for (OnResumeListener onResumeListener : onResumeListeners) {
			onResumeListener.onResume();
		}
	}

	/**
	 * 暂停
	 *
	 * <p>Activity被覆盖到下面或者锁屏时被调用。
	 */
	@Override
	protected void onPause() {
		super.onPause();
		inForeground = false;

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onPause 程序被暂停");

		// 分发事件
		for (OnPauseListener onPauseListener : onPauseListeners) {
			onPauseListener.onPause();
		}
		// 有可能在执行完onPause或onStop后，系统资源紧张将Activity杀死，所以有必要在此保存持久数据
	}

	/**
	 * 停止
	 *
	 * <p>退出当前Activity或者跳转到新Activity时被调用
	 */
	@Override
	protected void onStop() {
		super.onStop();

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onStop 程序被停止");

		// 分发事件
		for (OnStopListener onStopListener : onStopListeners) {
			onStopListener.onStop();
		}
	}

	/**
	 * 分发活动销毁并释放宿主引用；只有显式终止应用才结束当前进程。
	 *
	 * <p>系统为配置变化重建活动时也会调用，不将其等同于用户终止应用。
	 */
	@Override
	protected void onDestroy() {
		inForeground = false;
		try {
			日志输出.info("MainActivity.onDestroy 程序被销毁");
			for (OnDestroyListener listener : onDestroyListeners) {
				listener.onDestroy();
			}
		} finally {
			// 移除全部宿主监听与视图引用，不依赖结束进程回收当前活动。
			onRestartListeners.clear();
			onResumeListeners.clear();
			onPauseListeners.clear();
			onStopListeners.clear();
			onDestroyListeners.clear();
			onSaveInstanceStateListeners.clear();
			onRestoreInstanceStateListeners.clear();
			onConfigurationChangedListeners.clear();
			onNewIntentListeners.clear();
			onActivityResultListeners.clear();
			onRequestPermissionsResultListeners.clear();
			if (rootView != null) {
				rootView.removeAllViews();
			}
			activeForm = null;
			contentView = null;
			rootView = null;
			gestureDetector = null;
			if (INSTANCE == this) {
				INSTANCE = null;
				应用操作.initialize(null);
			}
			super.onDestroy();

			// 终止() 保留结束整个应用进程的约定；返回键、finish() 和配置重建不走此分支。
			if (terminationRequested) {
				android.os.Process.killProcess(android.os.Process.myPid());
			}
		}
	}

	/**
	 * 保存供后续活动重建使用的临时界面状态；不替代持久数据保存。
	 *
	 * <p>Android 9 及以上且目标 API 至少为 28 时，在 onStop() 之后调用；其它情况不依赖固定顺序。
	 * 系统直接结束进程时不保证有机会调用。
	 *
	 * @param outState 需要写入的状态
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onSaveInstanceState 保存实例状态");

		// 分发事件
		for (OnSaveInstanceStateListener onSaveInstanceStateListener : onSaveInstanceStateListeners) {
			onSaveInstanceStateListener.onSaveInstanceState(outState);
		}
	}

	/**
	 * 恢复实例状态
	 *
	 * <p>Activity被系统杀死后再重建时被调用。
	 *
	 * <p>例如：
	 * 屏幕方向改变时，Activity被销毁再重建；
	 * 当前Activity处于后台，系统资源紧张将其杀死，用户又启动该Activity。
	 * 这两种情况下onRestoreInstanceState都会被调用，在onStart之后。
	 *
	 * @param savedInstanceState 此前保存的活动状态
	 */
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onRestoreInstanceState 恢复实例状态");

		// 分发事件
		for (OnRestoreInstanceStateListener onRestoreInstanceStateListener : onRestoreInstanceStateListeners) {
			onRestoreInstanceStateListener.onRestoreInstanceState(savedInstanceState);
		}
	}

	/**
	 * 配置更改
	 *
	 * <p>在活动运行时设备配置更改时由系统调用。
	 *
	 * <p>如屏幕方向方式改变等
	 *
	 * @param newConfig 变化后的系统配置
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onConfigurationChanged 配置已更改");

		// 分发事件
		for (OnConfigurationChangedListener onConfigurationChangedListener : onConfigurationChangedListeners) {
			onConfigurationChangedListener.onConfigurationChanged(newConfig);
		}
	}

	/**
	 * 新意图
	 *
	 * <p>如果应用已创建，再去启动将收到该事件
	 *
	 * @param intent 启动时传递给程序的意图
	 */
	@Override
	protected void onNewIntent(Intent intent) {
		super.onNewIntent(intent);
		setIntent(intent);

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onNewIntent 程序被启动；行为=" + intent.getAction());

		// 分发事件
		for (OnNewIntentListener onNewIntentListener : onNewIntentListeners) {
			onNewIntentListener.onNewIntent(intent);
		}
	}

	/**
	 * 活动结果
	 *
	 * <p>当您启动的活动退出时调用，
	 * 为您提供您启动它的请求代码、
	 * 返回的结果代码以及来自它的任何其他数据。
	 *
	 * @param requestCode 发起操作时使用的请求编号
	 * @param resultCode 系统返回的结果编号
	 * @param data 本次传入的数据
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onActivityResult 收到返回结果");

		// 分发事件
		for (OnActivityResultListener onActivityResultListener : onActivityResultListeners) {
			onActivityResultListener.onActivityResult(requestCode, resultCode, data);
		}
	}

	/**
	 * 请求权限结果
	 *
	 * <p>请求权限的结果回调。
	 *
	 * 对于{@code requestPermissions(String[], int)}每个调用都会调用此方法。
	 *
	 * @param requestCode 发起操作时使用的请求编号
	 * @param permissions 本次请求的权限名称
	 * @param grantResults 与权限名称逐项对应的授权结果
	 */
	@Override
	@TargetApi(Build.VERSION_CODES.M) // 使用API23检查
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		日志输出.输出信息(日志输出.MODULE_NAME_RTL, "MainActivity.onRequestPermissionsResult 收到请求权限结果");

		// 分发事件
		for (OnRequestPermissionsResultListener onRequestPermissionsResultListener : onRequestPermissionsResultListeners) {
			onRequestPermissionsResultListener.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	/**
	 * 按下某键
	 *
	 * <p>当按下某个键且该键未被活动内部的任何视图处理时调用。
	 *
	 * @param keycode 键码
	 * @param event 事件的说明。
	 * @return {@code true}已处理不再传递该事件，{@code false}表示未处理将继续传递该事件
	 */
	@Override
	public boolean onKeyDown(int keycode, KeyEvent event) {
		// 创建逻辑型参考参数，缺省为假。
		BooleanReferenceParameter shield = new BooleanReferenceParameter(false);
		if (activeForm != null) {
			activeForm.按下某键(keycode, shield);
		}

		// 保留由 Simple 窗口事件自行控制切换和退出的约定。
		// 此处不调用父类，避免返回键在切换窗口后继续触发系统默认退出。
		return shield.get();
	}

	/**
	 * 触摸事件
	 *
	 * <p>用户触摸窗口触发该事件。
	 *
	 * @param event 触摸事件
	 * @return {@code true}已处理不再传递该事件，{@code false}表示未处理将继续传递该事件
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		return (gestureDetector != null && gestureDetector.onTouchEvent(event)) || super.onTouchEvent(event);
	}

	/**
	 * 创建选项菜单
	 *
	 * <p>初始化活动的标准选项菜单的内容。
	 *
	 * @param menu 放置项目的选项菜单。
	 * @return {@code true}已处理不再传递该事件，{@code false}表示未处理将继续传递该事件
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		for (String caption : menuItems) {
			menu.add(caption);
		}
		return !menuItems.isEmpty();
	}

	/**
	 * 菜单项已选择
	 *
	 * <p>每当选择选项菜单中的项时，都会调用此挂钩。
	 *
	 * @param item 选项菜单项目
	 * @return {@code true}已处理不再传递该事件，{@code false}表示未处理将继续传递该事件
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (activeForm != null) {
			activeForm.选中菜单(item.getTitle().toString());
		}
		return true;
	}

	/* 公开方法 */

	/**
	 * 将给定视图设置为应用程序的根视图的内容
	 *
	 * @param view 新的根视图内容
	 */
	public void setContent(View view) {
		if (contentView != null) {
			rootView.removeView(contentView);
		}

		// 修复APP在高版本系统切换窗口崩溃（java.lang.IllegalStateException）xhwsd@qq.com 2021-5-30
		// 解决指定的子级已具有父级
		ViewGroup group = (ViewGroup) view.getParent();
		if (group != null) {
			group.removeView(view);
		}

		contentView = view;
		rootView.addView(view, new ViewGroup.LayoutParams(
			ViewGroup.LayoutParams.MATCH_PARENT,
			ViewGroup.LayoutParams.MATCH_PARENT
		));
	}

	/**
	 * 检查给定窗口是否为活动（当前）窗口。
	 *
	 * @param form 窗口以检查它是否处于活跃状态
	 * @return {@code true}如果给定的窗口处于活跃状态，否则为{@code false}
	 */
	public boolean isActiveForm(窗口Impl form) {
		return form == activeForm;
	}

	/**
	 * 将给定的监听器添加到 onRestar 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnRestartListener(OnRestartListener listener) {
		onRestartListeners.add(listener);
	}

	/**
	 * 从 onRestar 监听器列表中移除给定的监听器
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnRestartListener(OnRestartListener listener) {
		onRestartListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onResume 监听器。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnResumeListener(OnResumeListener listener) {
		onResumeListeners.add(listener);
	}

	/**
	 * 从 onResume 监听器中删除给定的监听器。
	 *
	 * @param listener 要添加的侦听器
	 */
	public void removeOnResumeListener(OnResumeListener listener) {
		onResumeListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onPause 监听器。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnPauseListener(OnPauseListener listener) {
		onPauseListeners.add(listener);
	}

	/**
	 * 从 onPause 监听器中删除给定的监听器。
	 *
	 * @param listener 要添加的侦听器
	 */
	public void removeOnPauseListener(OnPauseListener listener) {
		onPauseListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onStop 监听器。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnStopListener(OnStopListener listener) {
		onStopListeners.add(listener);
	}

	/**
	 * 从 onStop 监听器中删除给定的监听器。
	 *
	 * @param listener 要添加的侦听器
	 */
	public void removeOnStopListener(OnStopListener listener) {
		onStopListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onDestroy 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnDestroyListener(OnDestroyListener listener) {
		onDestroyListeners.add(listener);
	}

	/**
	 * 从 onDestroy 监听器列表中移除给定的监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnDestroyListener(OnDestroyListener listener) {
		onDestroyListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onSaveInstanceState 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnSaveInstanceStateListener(OnSaveInstanceStateListener listener) {
		onSaveInstanceStateListeners.add(listener);
	}

	/**
	 * 从 onSaveInstanceState 监听器列表中移除给定的监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnSaveInstanceStateListener(OnSaveInstanceStateListener listener) {
		onSaveInstanceStateListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onRestoreInstanceState 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnRestoreInstanceStateListener(OnRestoreInstanceStateListener listener) {
		onRestoreInstanceStateListeners.add(listener);
	}

	/**
	 * 从 onRestoreInstanceState 监听器列表中移除给定的监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnRestoreInstanceStateListener(OnRestoreInstanceStateListener listener) {
		onRestoreInstanceStateListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onConfigurationChanged 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnConfigurationChangedListener(OnConfigurationChangedListener listener) {
		onConfigurationChangedListeners.add(listener);
	}

	/**
	 * 从 onConfigurationChanged 监听器列表中移除给定的监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnConfigurationChangedListener(OnConfigurationChangedListener listener) {
		onConfigurationChangedListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onNewIntent 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnNewIntentListener(OnNewIntentListener listener) {
		onNewIntentListeners.add(listener);
	}

	/**
	 * 从 onNewIntent 监听器列表中移除给定的监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnNewIntentListener(OnNewIntentListener listener) {
		onNewIntentListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onActivityResult 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnActivityResultListener(OnActivityResultListener listener) {
		onActivityResultListeners.add(listener);
	}

	/**
	 * 从 onActivityResult 监听器列表中移除给定的监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnActivityResultListener(OnActivityResultListener listener) {
		onActivityResultListeners.remove(listener);
	}

	/**
	 * 将给定的监听器添加到 onRequestPermissionsResult 监听器列表。
	 *
	 * @param listener 要添加的监听器
	 */
	public void addOnRequestPermissionsResultListener(OnRequestPermissionsResultListener listener) {
		onRequestPermissionsResultListeners.add(listener);
	}

	/**
	 * 从 onRequestPermissionsResult 监听器列表中移除给定的监听器。
	 *
	 * @param listener 要移除的监听器
	 */
	public void removeOnRequestPermissionsResultListener(OnRequestPermissionsResultListener listener) {
		onRequestPermissionsResultListeners.remove(listener);
	}

	/* ApplicationFunctions 实现 */

	@Override
	public void terminate() {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				terminationRequested = true;
				finishAndRemoveTask();
			}
		});
	}

	@Override
	public void addMenuItem(String caption) {
		menuItems.add(caption);
	}

	@Override
	public void switchForm(窗口 form) {
		if (activeForm != null) {
			// 触发即将切换事件
			activeForm.即将切换();

			// 失去焦点
			View view = getCurrentFocus();
			if (view != null) {
				view.clearFocus();
			}
		}

		// 设置根视图
		窗口Impl formImpl = (窗口Impl) form;
		setContent(formImpl.getView());

		// 先更新活动窗口，再应用其标题和背景，避免属性设置器忽略本次切换。
		activeForm = formImpl;
		form.标题(form.标题());
		form.背景图片(form.背景图片());

		// 触发切换完毕事件
		activeForm.切换完毕();
	}

	@Override
	public Variant getPreference(String name) {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		return StringVariant.getStringVariant(preferences.getString(name, ""));
	}

	@Override
	public void storePreference(String name, Variant value) {
		SharedPreferences preferences = getPreferences(MODE_PRIVATE);
		SharedPreferences.Editor editor = preferences.edit();
		editor.putString(name, value.getString());
		// 终止应用会主动结束进程，保持同步写盘，不能把尚未持久化的数据当作已经保存。
		editor.commit();
	}

	@Override
	public String getAppName() {
		try {
			CharSequence label = getPackageManager().getApplicationLabel(getApplicationInfo());
			return label == null ? "" : label.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	public String getVersionName() {
		try {
			String name = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
			return name == null ? "" : name;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getVersionCode() {
		try {
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
				// 新接口返回高低位合并值，转为 int 只保留 versionCode 对应的低 32 位。
				return (int) packageInfo.getLongVersionCode();
			} else {
				// 旧系统没有新接口，沿用旧字段并在本方法内抑制弃用警告。
				return packageInfo.versionCode;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public boolean serviceStart(意图 intent) {
		if (intent == null) {
			日志输出.warning("启动服务失败：意图为空");
			return false;
		}
		try {
			// 返回值只表示系统受理请求；后台限制与权限拒绝按公开约定返回失败。
			return startService(intent.getIntent()) != null;
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			日志输出.error("启动服务失败：%s", e.toString());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public boolean serviceStop(意图 intent) {
		if (intent == null) {
			日志输出.warning("停止服务失败：意图为空");
			return false;
		}
		try {
			return stopService(intent.getIntent());
		} catch (IllegalStateException | IllegalArgumentException | SecurityException e) {
			日志输出.error("停止服务失败：%s", e.toString());
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public void broadcastSend(意图 intent) {
		sendBroadcast(intent.getIntent());
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getDisplayWidth() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// API 30 起通过窗口指标获取当前应用窗口范围。
			WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
			// 按旧接口的尺寸口径，扣除左右导航栏和刘海安全区域的占用。
			Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
				WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout()
			);
			return metrics.getBounds().width() - insets.left - insets.right;
		} else {
			// 左右若有系统导航栏或刘海，不包含其占用区域。
			DisplayMetrics displaymetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			return displaymetrics.widthPixels;
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public int getDisplayHeight() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// API 30 起通过窗口指标获取当前应用窗口范围。
			WindowMetrics metrics = getWindowManager().getCurrentWindowMetrics();
			// 按旧接口的尺寸口径，扣除上下导航栏和刘海安全区域的占用。
			Insets insets = metrics.getWindowInsets().getInsetsIgnoringVisibility(
				WindowInsets.Type.navigationBars() | WindowInsets.Type.displayCutout()
			);
			return metrics.getBounds().height() - insets.top - insets.bottom;
		} else {
			// 含系统状态栏和应用标题栏，不含上下系统导航栏及刘海占用区域。
			DisplayMetrics displaymetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			return displaymetrics.heightPixels;
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public double getDisplayDensity() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
			// 从当前 Activity 配置读取逻辑密度，并按 160 dpi 基准换算为比例。
			int densityDpi = getResources().getConfiguration().densityDpi;
			// 按原 density 字段的 float 精度计算，再自动扩宽为 double。
			return densityDpi * (1.0f / DisplayMetrics.DENSITY_DEFAULT);
		} else {
			DisplayMetrics displaymetrics = new DisplayMetrics();
			getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
			return displaymetrics.density;
		}
	}

	@Override
	public int getStatusBarHeight() {
		try {
			// 沿用原 Activity 的资源配置，通过资源名称替代对内部 R 类的反射。
			Resources resources = getResources();
			int resourceId = resources.getIdentifier(
				"status_bar_height", "dimen", "android"
			);
			if (resourceId == 0) {
				return 0;
			}

			// 读取系统配置的状态栏像素高度，不受状态栏显示或隐藏影响。
			return resources.getDimensionPixelSize(resourceId);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	@Override
	public int getContentWidth() {
		// 左右若有系统导航栏，不包含其占用区域。
		return getWindow().findViewById(Window.ID_ANDROID_CONTENT).getWidth();
	}

	@Override
	public int getContentHeight() {
		// 不含系统状态栏、应用标题栏和底部系统导航栏。
		return getWindow().findViewById(Window.ID_ANDROID_CONTENT).getHeight();
	}

	@Override
	public boolean isPortraitOrientation() {
		return getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT;
	}

	@Override
	public boolean isInForeground() {
		return inForeground;
	}

	@Override
	public void returnDesktop() {
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.addCategory(Intent.CATEGORY_HOME);
		startActivity(intent);
	}

	@Override
	public void returnApp() {
		Intent intent = new Intent(this, MainActivity.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
		startActivity(intent);
	}
}
