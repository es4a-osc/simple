package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 预加载短音效并支持多路播放的非可视组件。
 *
 * <p>载入返回音效编号，播放返回播放编号；暂停、继续和停止使用播放编号。
 * 资产与应用私有文件无需额外权限，其他绝对路径仍受系统文件访问限制。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 音效播放器 extends 组件 {

	/* 组件事件 */

	/**
	 * 音效异步载入成功时在主线程触发，随后可以播放。
	 *
	 * @param soundId 载入音效返回的音效编号。
	 */
	@SimpleEvent
	void 载入完毕(int soundId);

	/**
	 * 音效异步解码失败时在主线程触发，不再触发载入完毕。
	 *
	 * @param soundId 本次载入的音效编号。
	 * @param status 系统返回的非零失败状态，不代表音效编号。
	 */
	@SimpleEvent
	void 载入失败(int soundId, int status);

	/* 组件方法 */

	/**
	 * 创建播放器并设置最大并发播放数，重新初始化会释放旧音效及播放流。
	 *
	 * @param maxStreams 最大并发播放数，必须大于 0；超出时由系统按优先级及先后顺序取舍。
	 * @throws IllegalArgumentException 最大并发播放数小于 1。
	 * @throws IllegalStateException 组件已销毁。
	 */
	@SimpleFunction
	void 初始化播放器(int maxStreams);

	/**
	 * 异步载入 APK 资产或可访问的绝对文件路径，须等待载入完毕事件后再播放。
	 *
	 * <p>不接收网络地址或 content URI。同步打开失败返回 -1；取得正编号后，
	 * 解码失败由载入失败事件通知。适合短音效，不用于播放长音乐。</p>
	 *
	 * @param source 资产文件名或以斜杠开头的绝对文件路径。
	 * @return 正音效编号；同步打开失败或系统拒绝载入时为 -1。
	 * @throws IllegalStateException 未初始化播放器或组件已销毁。
	 */
	@SimpleFunction
	int 载入音效(String source);

	/**
	 * 播放已经载入成功的音效一次。
	 *
	 * @param soundId 音效编号，必须来自当前播放器。
	 * @return 本次播放的正播放编号；未就绪、编号无效或系统拒绝播放时为 0。
	 * @throws IllegalStateException 未初始化播放器或组件已销毁。
	 */
	@SimpleFunction
	int 播放音效(int soundId);

	/**
	 * 播放已经载入成功的音效，允许同一音效产生多个独立播放编号。
	 *
	 * @param soundId 音效编号，必须来自当前播放器。
	 * @param loopCount 额外重复次数；0 播放一次，-1 持续循环，正数表示额外重复该次数。
	 * @return 本次播放的正播放编号；未就绪、编号无效或系统拒绝播放时为 0。
	 * @throws IllegalArgumentException 循环次数小于 -1。
	 * @throws IllegalStateException 未初始化播放器或组件已销毁。
	 */
	@SimpleFunction
	int 播放音效(int soundId, int loopCount);

	/**
	 * 暂停指定播放流；已经结束或无效的播放编号由系统忽略。
	 *
	 * @param streamId 播放音效返回的播放编号，不是载入音效返回的音效编号。
	 * @throws IllegalStateException 未初始化播放器或组件已销毁。
	 */
	@SimpleFunction
	void 暂停播放(int streamId);

	/**
	 * 继续指定的已暂停播放流，不会重新创建播放流。
	 *
	 * @param streamId 播放音效返回的播放编号；已经结束或无效时由系统忽略。
	 * @throws IllegalStateException 未初始化播放器或组件已销毁。
	 */
	@SimpleFunction
	void 继续播放(int streamId);

	/**
	 * 停止指定播放流；停止后需要再次播放音效以取得新的播放编号。
	 *
	 * @param streamId 播放音效返回的播放编号；已经结束或无效时由系统忽略。
	 * @throws IllegalStateException 未初始化播放器或组件已销毁。
	 */
	@SimpleFunction
	void 停止播放(int streamId);

	/**
	 * 卸载当前播放器中的音效，并丢弃该音效尚未派发的载入回调。
	 *
	 * @param soundId 载入音效返回的音效编号。
	 * @return 系统确认卸载返回真，编号无效或卸载失败返回假。
	 * @throws IllegalStateException 未初始化播放器或组件已销毁。
	 */
	@SimpleFunction
	boolean 卸载音效(int soundId);

	/**
	 * 释放全部音效、播放流及待派发的回调；可重复调用，随后可重新初始化。
	 *
	 * <p>旧音效编号及播放编号不再可用；销毁组件后则不能重新初始化。</p>
	 */
	@SimpleFunction
	void 释放播放器();
}
