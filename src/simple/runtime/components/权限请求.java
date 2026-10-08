package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 权限请求。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 权限请求 extends 组件 {

	/**
	 * 请求权限结果通知。
	 *
	 * @param permissions 权限名称。
	 * @param grantResults 授予结果。
	 * @param requestMark 请求标记。
	 */
	@SimpleEvent
	void 请求结果(String[] permissions, int[] grantResults, String requestMark);

	/**
	 * 取当前应用所有权限列表。
	 *
	 * @return 成功返回所有权限列表，否则返回{@code null}。
	 */
	@SimpleFunction
	String[] 取所有权限();

	/**
	 * 取所有未授权权限列表。
	 *
	 * @return 成功返回所有未授权权限列表，否则返回{@code null}。
	 */
	@SimpleFunction
	String[] 取所需权限();

	/**
	 * 检验当前应用请求权限是否授权。
	 *
	 * <p>注意API等级小于21始终返回{@code true}
	 *
	 * @param permission 请求权限名。
	 * @return 已授权返回{@code true}，未授权返回{@code false}。
	 */
	@SimpleFunction
	boolean 是否授权(String permission);

	/**
	 * 请求使用权限，请求结果将通过请求权限结果事件通知。
	 *
	 * <p>谷歌不推荐一次请求大量权限，会使用户感到无所适。
	 *
	 * @param permissions 权限列表名，注意仅可为清单中声明的权限。
	 * @param requestMark 请求标记，可为{@code null}。
	 */
	@SimpleFunction
	void 请求权限(String[] permissions, String requestMark);

	/**
	 * 检验是否应该显示权限理由。
	 *
	 * @param permission 请求权限名。
	 * @return 未屏蔽返回{@code true}，已屏蔽返回{@code false}。
	 */
	@SimpleFunction
	boolean 需要理由(String permission);
}
