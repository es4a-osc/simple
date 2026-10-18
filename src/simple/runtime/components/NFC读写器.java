package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * NFC 标签读写组件。初始化NFC 返回 1（不支持）、2（未开启）或 3（就绪）。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(
	permissionNames =
		// 允许应用程序通过 NFC 进行读写操作。
		"android.permission.NFC"
)
public interface NFC读写器 extends 组件 {

	/* 组件事件 */

	/**
	 * 读取到 NFC 标签内容时触发。
	 *
	 * @param text 待处理文本
	 */
	@SimpleEvent
	void 读取到信息(String text);

	/**
	 * 卡号为标签标识按小端字节序转换的无符号十进制文本。
	 *
	 * @param cardId SIM 卡标识
	 */
	@SimpleEvent
	void 读取到卡号(String cardId);

	/* 组件方法 */

	/**
	 * 初始化 NFC：1 不支持，2 未开启，3 就绪；可重复调用。
	 *
	 * @return 初始化 NFC：1 不支持，2 未开启，3 就绪；可重复调用
	 */
	@SimpleFunction
	int 初始化NFC();

	/**
	 * 切换为读取模式，读取首条 NDEF 记录。
	 */
	@SimpleFunction
	void 置读取模式();

	/**
	 * 切换为写入模式，以 UTF-8 文本保存内容。
	 *
	 * @param text 待处理文本
	 */
	@SimpleFunction
	void 置写入模式(String text);
}
