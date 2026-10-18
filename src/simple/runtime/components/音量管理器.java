package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.UsesPermissions;

/**
 * 读取、设置系统音频流音量并检测有线耳机连接状态的不可视组件。
 *
 * <p>音量是设备定义的整数档位，不是百分比，也不同于单个播放器的声道音量。
 * 声明的 MODIFY_AUDIO_SETTINGS 是普通权限，不需要运行时授权弹窗；
 * 触发勿扰模式变化仍可能需要用户单独授予通知策略访问权限。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
@SimpleComponent
@UsesPermissions(
	permissionNames =
		// 允许应用修改系统音频设置；普通权限，无需运行时请求。
		"android.permission.MODIFY_AUDIO_SETTINGS"
)
public interface 音量管理器 extends 组件 {

	/* 音量类型常量 */

	/**
	 * 系统提示音，类型值为 1。
	 */
	@SimpleDataElement
	int 音量_系统 = 1;

	/**
	 * 通话音量，类型值为 2。
	 */
	@SimpleDataElement
	int 音量_通话 = 2;

	/**
	 * 来电铃声音量，类型值为 3。
	 */
	@SimpleDataElement
	int 音量_铃声 = 3;

	/**
	 * 音乐、视频等媒体音量，类型值为 4。
	 */
	@SimpleDataElement
	int 音量_媒体 = 4;

	/**
	 * 闹钟音量，类型值为 5。
	 */
	@SimpleDataElement
	int 音量_闹钟 = 5;

	/* 组件方法 */

	/**
	 * 读取指定类型的当前系统音量档位。
	 *
	 * @param type 音量类型，使用本组件的音量常量；其它值沿用系统提示音类型。
	 * @return 当前音量档位，不是百分比；此查询不需要危险权限的运行时授权。
	 */
	@SimpleFunction
	int 取音量(int type);

	/**
	 * 读取指定类型在当前设备上的最大音量档位。
	 *
	 * @param type 音量类型，使用本组件的音量常量；其它值沿用系统提示音类型。
	 * @return 最大音量档位，各类型及设备的范围可能不同。
	 */
	@SimpleFunction
	int 取最大音量(int type);

	/**
	 * 设置指定类型的系统音量，不显示系统音量面板。
	 *
	 * <p>设置会影响使用该音频流的其它应用。固定音量设备或系统策略可能忽略设置，
	 * 调用后应读取当前音量确认结果。系统拒绝设置时抛出可由 Simple 错误处理接住的运行错误，
	 * 不自动申请通知策略访问权限；铃声和勿扰行为仍由系统策略决定。</p>
	 *
	 * @param type 音量类型，使用本组件的音量常量；其它值沿用系统提示音类型。
	 * @param volume 目标整数档位，应在 0 至该类型最大音量之间；实际限制由系统决定。
	 */
	@SimpleFunction
	void 置音量(int type, int volume);

	/**
	 * 判断是否连接了系统识别的有线耳机。
	 *
	 * <p>Android 6.0 及以上枚举输出设备，检测有线耳麦、有线耳机和明确标识的 USB 耳机，
	 * 不把一般 USB 音频设备或蓝牙耳机当作有线耳机。旧系统使用有线耳机查询接口。
	 * 连接状态不代表声音当前一定从该设备输出，不需要录音或蓝牙运行时权限。</p>
	 *
	 * @return 检测到有线耳机返回真，否则返回假。
	 */
	@SimpleFunction
	boolean 有线耳机是否连接();
}
