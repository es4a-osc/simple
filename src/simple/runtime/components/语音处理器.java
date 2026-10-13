package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 调用系统语音识别与朗读引擎的非可见组件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
public interface 语音处理器 extends 组件 {

	/* 组件事件 */

	@SimpleEvent
	void 语音识别完毕(String result);

	/* 组件方法 */

	/**
	 * 保留原名称，实际检查系统是否有可接收语音识别请求的应用。
	 */
	@SimpleFunction
	boolean 是否已安装语音引擎();

	@SimpleFunction
	void 语音识别();

	@SimpleFunction
	void 初始化语音朗读();

	/**
	 * 使用系统默认语言朗读；初始化期间保留最后一次请求，就绪后开始朗读。
	 */
	@SimpleFunction
	void 语音朗读(String text);

	/**
	 * 停止并释放朗读引擎；后续朗读可重新初始化。
	 */
	@SimpleFunction
	void 释放语音朗读();
}
