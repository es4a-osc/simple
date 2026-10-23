package simple.runtime.components;

import simple.runtime.annotations.ManifestNodes;
import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 调用系统语音识别界面和文字朗读引擎的非可见组件。
 *
 * <p>所有调用和事件均在主线程进行。识别需要可处理请求的系统应用，朗读需要可用的引擎及系统默认语言的数据。
 * 识别与朗读是两项独立能力；本组件不直接录音，不申请麦克风权限。宿主销毁时自动释放，销毁后不再派发事件。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@ManifestNodes(rootXml =
	"<queries>" +
		// Android 11 包可见性：只查询实际使用的识别界面与朗读服务。
		"<intent>" +
			"<action android:name=\"android.speech.action.RECOGNIZE_SPEECH\" />" +
		"</intent>" +
		"<intent>" +
			"<action android:name=\"android.intent.action.TTS_SERVICE\" />" +
		"</intent>" +
	"</queries>"
)
public interface 语音处理器 extends 组件 {

	/* 组件事件 */

	/**
	 * 识别成功时触发，返回首项识别文本，不拼接候选结果。
	 *
	 * @param result 识别文本
	 */
	@SimpleEvent
	void 语音识别完毕(String result);

	/**
	 * 用户取消系统识别界面时触发，当前识别已经结束。
	 */
	@SimpleEvent
	void 语音识别取消();

	/**
	 * 朗读引擎和默认语言就绪时触发；重复初始化已就绪的引擎不会再次触发。
	 *
	 * <p>初始化期间暂存的最后一次朗读请求会先提交，再通知就绪；事件不表示文字已经朗读完毕。
	 */
	@SimpleEvent
	void 语音朗读就绪();

	/**
	 * 已受理的异步操作失败时触发。
	 *
	 * @param operation 语音识别、初始化语音朗读或语音朗读
	 * @param message 失败原因；不包含系统未报告的实际发声结果
	 */
	@SimpleEvent
	void 操作失败(String operation, String message);

	/* 组件属性 */

	/**
	 * 判断是否正在等待系统识别界面返回。
	 *
	 * @return 存在尚未返回的识别请求时为真
	 */
	@SimpleProperty
	boolean 正在识别();

	/**
	 * 判断朗读引擎和系统默认语言是否已经准备好。
	 *
	 * @return 可以提交朗读请求时为真，不表示正在发声
	 */
	@SimpleProperty
	boolean 朗读就绪();

	/**
	 * 返回最近一次请求被拒绝或异步操作失败的原因。
	 *
	 * @return 错误说明；请求正常受理后为空文本，能力查询不修改此值
	 */
	@SimpleProperty
	String 错误信息();

	/* 组件方法 */

	/**
	 * 检查系统是否有可处理语音识别请求的应用，不检查文字朗读引擎。
	 *
	 * @return 组件未销毁且存在识别应用时为真，不保证识别服务或网络当前可用
	 */
	@SimpleFunction
	boolean 是否支持语音识别();

	/**
	 * 在应用前台打开系统语音识别界面，同一实例同时进行一次识别。
	 *
	 * <p>结果通过语音识别完毕、语音识别取消或操作失败通知；未受理时不触发事件。
	 *
	 * @return 请求已发起时为真；重复请求、宿主不在前台、组件销毁或无法打开时为假，原因见错误信息
	 */
	@SimpleFunction
	boolean 语音识别();

	/**
	 * 异步初始化朗读引擎并选择系统默认语言，可以重复调用。
	 *
	 * <p>正在初始化或已经就绪时不重复创建引擎。新引擎通过语音朗读就绪或操作失败通知结果。
	 *
	 * @return 已开始初始化、正在初始化或已经就绪时为真；未受理时为假，原因见错误信息
	 */
	@SimpleFunction
	boolean 初始化语音朗读();

	/**
	 * 使用系统默认语言朗读文本，新请求替换尚未完成的朗读。
	 *
	 * <p>尚未就绪时自动初始化，只保留最后一次请求；初始化失败会通知操作失败。
	 * 返回真仅表示请求暂存或已交给引擎，不表示已经发声或朗读完成，实际输出由引擎、语言数据和音量决定。
	 *
	 * @param text 非空朗读文本，不能超过系统引擎允许的输入长度
	 * @return 请求已受理时为真；未受理时为假，原因见错误信息，不触发事件
	 */
	@SimpleFunction
	boolean 语音朗读(String text);

	/**
	 * 清除暂存文本，停止并释放朗读引擎；后续朗读可以重新初始化。
	 *
	 * <p>不取消正在进行的系统语音识别；重复释放没有副作用。
	 */
	@SimpleFunction
	void 释放语音朗读();
}
