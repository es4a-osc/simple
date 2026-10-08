package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;

/**
 * 音频播放器组件。
 *
 * @author 树先生 xhwds@qq.com
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
								// 允许程序访问网络连接，可能产生GPRS流量
								"android.permission.INTERNET," +
								// 允许程序挂载、反挂载外部文件系统
								"android.permission.WRITE_EXTERNAL_STORAGE," +
								// 允许程序写入外部存储,如SD卡上写文件
								"android.permission.MOUNT_UNMOUNT_FILESYSTEMS")
public interface 音频播放器 extends 组件 {

	// 错误类型
	@SimpleDataElement
	static final int 错误_未知 = 1; // MediaPlayer.MEDIA_ERROR_UNKNOWN
	@SimpleDataElement
	static final int 错误_服务器 = 100; // MediaPlayer.MEDIA_ERROR_SERVER_DIED
	@SimpleDataElement
	static final int 错误_读写 = -1004; // MediaPlayer.MEDIA_ERROR_IO API17
	@SimpleDataElement
	static final int 错误_流无效 = -1007; // MediaPlayer.MEDIA_ERROR_MALFORMED
	@SimpleDataElement
	static final int 错误_不支持 = -1010; // MediaPlayer.MEDIA_ERROR_UNSUPPORTED API17
	@SimpleDataElement
	static final int 错误_超时 = -110; // MediaPlayer.MEDIA_ERROR_TIMED_OUT API17

	// 信息或警告类型
	@SimpleDataElement
	static final int 信息_未知 = 1; // MediaPlayer.MEDIA_INFO_UNKNOWN
	@SimpleDataElement
	static final int 信息_缓冲开始 = 701; // MediaPlayer.MEDIA_INFO_BUFFERING_START
	@SimpleDataElement
	static final int 信息_缓冲结束 = 702; // MediaPlayer.MEDIA_INFO_BUFFERING_END
	@SimpleDataElement
	static final int 信息_错误交叉 = 800; // MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING
	@SimpleDataElement
	static final int 信息_无法跳转 = 801; // MediaPlayer.MEDIA_INFO_NOT_SEEKABLE
	@SimpleDataElement
	static final int 信息_元数据更新 = 802; // MediaPlayer.MEDIA_INFO_METADATA_UPDATE

	/**
	 * 当播放器触发错误时，触发该事件。
	 *
	 * @param what 可选值请参考{@link 音频播放器#错误_未知}等
	 * @param extra
	 */
	@SimpleEvent
	void 错误通知(int what, int extra);

	/**
	 * 当播放器触发信息或警告时，触发该事件。
	 *
	 * @param what 可选值请参考{@link 音频播放器#信息_未知}等
	 * @param extra
	 */
	@SimpleEvent
	void 信息通知(int what, int extra);

	/**
	 * 被调用以更新缓冲通过渐进式HTTP下载接收的媒体流的状态。
	 * 接收的缓冲百分比表示已缓冲或播放了多少内容。
	 * 例如，当已播放一半内容时，缓冲更新为80%表示要播放的下一个30%的内容已被缓冲。
	 *
	 * @param percent 到目前为止已缓冲或播放的内容的百分比（0-100）
	 */
	@SimpleEvent
	void 缓冲更新(int percent);

	/**
	 * 当播放器数据源已就绪，触发该事件。
	 */
	@SimpleEvent
	void 播放就绪();

	/**
	 * 当设置播放{@code 位置}跳转完毕后，触发该事件。
	 */
	@SimpleEvent
	void 位置改变();

	/**
	 * 当播放到结尾时，触发该事件。
	 */
	@SimpleEvent
	void 播放结束();

	/**
	 * 获取文件的播放时长(毫秒), 如果没有可用的时长, 就会返回 -1
	 *
	 * @return 播放时长(毫秒)
	 */
	@SimpleProperty
	int 时长();

	/**
	 * 获取当前播放器播放的位置
	 *
	 * @return 当前播放位置，单位毫秒
	 */
	@SimpleProperty
	int 位置();

	/**
	 * 设置当前播放器播放的位置
	 * 该方法是异步的，会触发{@code 位置改变}事件。
	 *
	 * @param value 当前播放位置，单位毫秒
	 */
	@SimpleProperty
	void 位置(int value);

	/**
	 * 获取播放器当前是否循环播放
	 *
	 * @return 否循环播放
	 */
	@SimpleProperty
	boolean 循环();

	/**
	 * 设置播放器是否循环播放
	 *
	 * @param looping 否循环播放
	 */
	@SimpleProperty
	void 循环(boolean looping);

	/**
	 * 设置播放数据源。
	 *
	 * @param mFile 可以是文件路径、URL地址、知产路径。
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	@SimpleFunction
	boolean 打开(String mFile);

	/**
	 * 播放器收到事件{@code 播放继续}后开始播放，或{@code 暂停}后恢复播放。
	 *
	 * @return 成功返回{@code true}，失败返回{@code false}
	 */
	@SimpleFunction
	boolean 播放();

	/**
	 * 暂停当前播放
	 *
	 * @return 成功返回{@code true}，失败返回{@code false}
	 */
	@SimpleFunction
	boolean 暂停();

	/**
	 * 停止播放和相关下载（如果可以）。
	 *
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	@SimpleFunction
	boolean 停止();

	/**
	 * 判断当前播放器正在播放。
	 *
	 * @return 成功返回{@code true}，失败返回{@code false}。
	 */
	@SimpleFunction
	boolean 是否正在播放();

	/**
	 * 设置音量。（范围0.0~1.0之间）
	 *
	 * @param Right 左边
	 * @param Left 右边
	 */
	@SimpleFunction
	void 置音量(float Right, float Left);
}
