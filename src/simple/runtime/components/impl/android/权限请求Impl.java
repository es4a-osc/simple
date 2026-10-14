package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;
import simple.runtime.components.权限请求;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;

import android.annotation.TargetApi;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

import java.util.ArrayList;

/**
 * 权限请求的实现。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class 权限请求Impl extends 组件Impl implements 权限请求 {

	// 请求权限标记
	private ArrayList<String> requestMarks;

	/**
	 * 创建新的组件。
	 *
	 * @param container 容纳组件的容器（对于不可见的组件必须是窗口，不可为 {@code null}）
	 */
	public 权限请求Impl(组件容器 container) {
		super(container);

		// 初始权限标记
		requestMarks = new ArrayList<String>();

		// 请求权限结果监听器
		MainActivity.getContext().addOnRequestPermissionsResultListener(new MainActivity.OnRequestPermissionsResultListener() {
			@Override
			public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
				// 仅触发当前窗口申请权限的事件
				if (requestCode == 权限请求Impl.this.hashCode() && !requestMarks.isEmpty()) {
					请求结果(permissions, grantResults, requestMarks.remove(0));
				}
			}
		});
	}

	/* 组件事件 */

	@Override
	public void 请求结果(String[] permissions, int[] grantResults, String requestMark) {
		EventDispatcher.dispatchEvent(this, "请求结果", permissions, grantResults, requestMark);
	}

	/* 组件方法 */

	@Override
    public boolean 需要请求() {
		//安卓6.0的版本号是23，只有当安卓版本号为23以上时才需要动态申请权限
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}

	@Override
	public String[] 取所有权限() {
		try {
			MainActivity context = MainActivity.getContext();
			return context.getPackageManager().getPackageInfo(
					context.getPackageName(),
					PackageManager.GET_PERMISSIONS
				).requestedPermissions;
		} catch (NameNotFoundException e) {
			return null;
		}
	}

	@Override
	@TargetApi(Build.VERSION_CODES.M) // 使用API23检查
	public String[] 取所需权限() {
		if (需要请求()) {
			// 取应用使用权限
			String[] permissions = 取所有权限();
			if (permissions == null) {
				return null;
			} else if (permissions.length > 0) {
				// 检验应用使用权限是否授权
				ArrayList<String> list = new ArrayList<String>();
				for (String permission : permissions) {
					if (是否授权(permission) == false) {
						list.add(permission);
					}
				}
				return list.toArray(new String[0]);
			}
		}
		return new String[0];
	}

	@Override
	@TargetApi(Build.VERSION_CODES.M) // 使用API23检查
	public boolean 是否授权(String permission) {
		if (需要请求()) {
			// 添加于API23：ContextWrapper.checkSelfPermission(String)
			return MainActivity.getContext().checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
		}
		return true;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.M) // 使用API23检查
	public boolean 请求权限(String requestMark) {
		if (需要请求()) {
			String[] permissions = 取所需权限();
			if (permissions != null && permissions.length > 0) {
				requestMarks.add(requestMark);
				// 添加于API23：Activity.requestPermissions(String[], int)
				MainActivity.getContext().requestPermissions(permissions, hashCode());
				return true;
			}
		}
		return false;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.M) // 使用API23检查
	public boolean 请求权限(String[] permissions, String requestMark) {
		if (需要请求()) {
			requestMarks.add(requestMark);
			// 添加于API23：Activity.requestPermissions(String[], int)
			MainActivity.getContext().requestPermissions(permissions, hashCode());
			return true;
		}
		return false;
	}

	@Override
	@TargetApi(Build.VERSION_CODES.M) // 使用API23检查
	public boolean 需要理由(String permission) {
		if (需要请求()) {
			// 添加于API23：Activity.shouldShowRequestPermissionRationale(String)
			return MainActivity.getContext().shouldShowRequestPermissionRationale(permission);
		}
		return false;
	}

	@Override
	public void 打开设置() {
    	Intent intent = new Intent();
    	intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
    	Uri uri = Uri.fromParts("package", MainActivity.getContext().getPackageName(), null);
    	intent.setData(uri);
    	MainActivity.getContext().startActivityForResult(intent, 123456789);
	}	
}
