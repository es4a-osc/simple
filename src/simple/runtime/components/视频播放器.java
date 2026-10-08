package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 视频播放器组件。
 *
 * @author 小刀(siyu1840)
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 视频播放器 extends 可视组件 {

	@SimpleEvent
	void 播放完毕();

	@SimpleFunction
	void 播放视频(String paramString);

	@SimpleFunction
	void 暂停播放();

	@SimpleFunction
	void 继续播放();

	@SimpleFunction
	void 停止播放();

	@SimpleFunction
	int 取视频时长();

	@SimpleFunction
	int 取播放位置();

	@SimpleFunction
	void 置播放位置(int paramInt);

	@SimpleFunction
	boolean 取播放状态();

	@SimpleFunction
	int 取缓冲进度();

	@SimpleFunction
	void 置控制条状态(boolean paramBoolean);
}
