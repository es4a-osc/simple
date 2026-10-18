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

	/* 组件事件 */

	/**
	 * 视频播放结束时触发。
	 */
	@SimpleEvent
	void 播放完毕();

	/* 组件方法 */

	/**
	 * 设置视频来源并开始播放。
	 *
	 * @param paramString 视频来源路径或地址。
	 */
	@SimpleFunction
	void 播放视频(String paramString);

	/**
	 * 暂停当前视频播放。
	 */
	@SimpleFunction
	void 暂停播放();

	/**
	 * 继续当前视频播放。
	 */
	@SimpleFunction
	void 继续播放();

	/**
	 * 停止当前视频并释放播放资源。
	 */
	@SimpleFunction
	void 停止播放();

	/**
	 * 读取当前视频总时长，单位为毫秒。
	 *
	 * @return 视频总时长，单位为毫秒；不可用时沿用系统返回值。
	 */
	@SimpleFunction
	int 取视频时长();

	/**
	 * 读取当前播放位置，单位为毫秒。
	 *
	 * @return 当前播放位置，单位为毫秒。
	 */
	@SimpleFunction
	int 取播放位置();

	/**
	 * 跳转到指定的毫秒位置。
	 *
	 * @param paramInt 目标播放位置，单位为毫秒。
	 */
	@SimpleFunction
	void 置播放位置(int paramInt);

	/**
	 * 判断当前视频是否正在播放。
	 *
	 * @return 正在播放时为真，否则为假。
	 */
	@SimpleFunction
	boolean 取播放状态();

	/**
	 * 读取已缓冲内容的百分比。
	 *
	 * @return 已缓冲内容的百分比。
	 */
	@SimpleFunction
	int 取缓冲进度();

	/**
	 * 设置视频播放控制条是否显示。
	 *
	 * @param paramBoolean 是否显示播放控制条。
	 */
	@SimpleFunction
	void 置控制条状态(boolean paramBoolean);
}
