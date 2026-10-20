package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 使用设备已录入的指纹进行本机验证，功能要求 Android 6.0（API 23）及以上。
 *
 * <p>所有调用及事件均在主线程进行。每实例同时进行一次验证；未匹配和提示事件不结束验证。
 * 应用进入后台时取消，组件或宿主销毁后不再派发事件。应用负责显示操作提示，不读取指纹数据。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	"android.permission.USE_FINGERPRINT"
)
public interface 指纹验证 extends 组件 {

	/* 常量 */

	/**
	 * 系统接口、权限、硬件、锁屏和录入条件均已满足；不代表当前没有临时锁定。
	 */
	@SimpleDataElement
	int 状态_可用 = 0;

	/**
	 * 系统低于 Android 6.0（API 23）。
	 */
	@SimpleDataElement
	int 状态_系统不支持 = 1;

	/**
	 * 设备没有可供应用使用的指纹硬件。
	 */
	@SimpleDataElement
	int 状态_无指纹硬件 = 2;

	/**
	 * 设备尚未设置安全锁屏。
	 */
	@SimpleDataElement
	int 状态_未设置锁屏 = 3;

	/**
	 * 用户尚未录入指纹。
	 */
	@SimpleDataElement
	int 状态_未录入指纹 = 4;

	/**
	 * 应用没有 USE_FINGERPRINT 权限。
	 */
	@SimpleDataElement
	int 状态_权限不足 = 5;

	/**
	 * 系统服务暂不可用或检查失败。
	 */
	@SimpleDataElement
	int 状态_暂不可用 = 6;

	/**
	 * 组件已经销毁，不能重新使用。
	 */
	@SimpleDataElement
	int 状态_已销毁 = 7;

	/**
	 * 系统因多次未匹配而临时锁定；等待系统允许后重新开始。
	 */
	@SimpleDataElement
	int 错误_临时锁定 = 7;

	/**
	 * 系统要求先用设备锁屏凭据解锁，再重新开始。
	 */
	@SimpleDataElement
	int 错误_永久锁定 = 9;

	/* 事件 */

	/**
	 * 指纹验证成功，当前验证已经结束。
	 */
	@SimpleEvent
	void 验证成功();

	/**
	 * 本次指纹未匹配，验证仍在进行，可提示用户继续尝试。
	 */
	@SimpleEvent
	void 验证未通过();

	/**
	 * 系统报告可恢复的采集问题，验证仍在进行。
	 *
	 * @param code 系统提示代码。
	 * @param message 系统提示文本。
	 */
	@SimpleEvent
	void 验证提示(int code, String message);

	/**
	 * 主动取消、应用暂停或系统取消后触发一次，当前验证已经结束。
	 *
	 * <p>空闲时取消不触发事件；组件或宿主销毁不触发此事件。
	 */
	@SimpleEvent
	void 验证取消();

	/**
	 * 已启动的验证因系统错误结束。
	 *
	 * @param code 系统错误代码；临时及永久锁定分别为 7、9。
	 * @param message 系统错误文本。
	 */
	@SimpleEvent
	void 验证错误(int code, String message);

	/* 属性 */

	/**
	 * 返回是否有尚未结束的验证。
	 *
	 * @return 正在等待系统结果时返回真。
	 */
	@SimpleProperty
	boolean 正在验证();

	/**
	 * 返回最近一次操作未受理或系统验证错误的原因。
	 *
	 * @return 错误说明；成功受理操作后为空文本，状态查询不修改此值。
	 */
	@SimpleProperty
	String 错误信息();

	/* 方法 */

	/**
	 * 检查系统、权限、硬件、锁屏及指纹录入条件。
	 *
	 * <p>此检查不扫描指纹，也不能预先确定系统临时锁定或传感器占用状态。
	 *
	 * @return 状态常量之一；API 23 以下返回状态_系统不支持。
	 */
	@SimpleFunction
	int 取可用状态();

	/**
	 * 开始一次异步指纹验证，提示界面由调用者提供。
	 *
	 * <p>正在验证、应用暂停、组件销毁或条件不满足时不受理；读取错误信息取得原因。
	 * 未受理时不派发结果事件。已受理请求的事件在此方法返回后派发，结束后可再次开始。
	 *
	 * @return 成功受理返回真，未受理返回假。
	 */
	@SimpleFunction
	boolean 开始验证();

	/**
	 * 取消当前验证；空闲时无操作，取消后可重新开始。
	 *
	 * <p>取消先释放当前请求，再通知验证取消事件；旧请求的迟到回调不影响新请求。
	 */
	@SimpleFunction
	void 取消验证();

	/**
	 * 打开系统安全设置，供用户设置锁屏和录入指纹。
	 *
	 * <p>设置项目及名称由设备提供；不会自动录入指纹。离开应用会取消正在进行的验证。
	 *
	 * @return 已打开设置返回真；系统低于 API 23、组件销毁或无法打开时返回假，原因见错误信息。
	 */
	@SimpleFunction
	boolean 打开设置();
}
