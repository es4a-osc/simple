package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.annotations.UsesPermissions;
import simple.runtime.collections.集合;

/**
 * 管理系统通讯录中的联系人和分组，支持同步及异步读取。
 *
 * <p>联系人每项为文本数组：{@code [原始联系人编号、姓名、电话号码、聚合联系人编号]}，多个号码分行返回。
 * 使用前需取得通讯录权限；修改和删除使用原始联系人编号。</p>
 *
 * @author xhwsd@qq.com 2026-10-16
 */
@SimpleComponent
@SimpleObject
@UsesPermissions(permissionNames =
	// 允许读取系统通讯录中的联系人和分组。
	"android.permission.READ_CONTACTS," +
	// 允许新增、修改或删除系统通讯录中的联系人和分组。
	"android.permission.WRITE_CONTACTS"
)
public interface 通讯录 extends 组件 {

	/**
	 * 获取最近一次完成操作的错误信息。
	 *
	 * @return 错误信息；空文本表示成功，异步结果优先以事件参数为准。
	 */
	@SimpleProperty
	String 最近错误();

	/**
	 * 查询是否正在异步读取；同时只接受一个读取任务。
	 *
	 * @return 正在异步读取返回真，否则返回假。
	 */
	@SimpleProperty
	boolean 读取中();

	/**
	 * 异步读取结束后在主线程通知；失败时结果为空集合，错误信息非空。
	 *
	 * @param contacts 每项为四列文本数组，列顺序见组件说明。
	 * @param requestMark 调用时传入的请求标记。
	 * @param error 空文本表示成功。
	 */
	@SimpleEvent
	void 取联系人完毕(集合 contacts, String requestMark, String error);

	/**
	 * 同步读取当前可访问的原始联系人及号码；大通讯录应使用异步读取。
	 *
	 * @return 联系人集合，每项为四列文本数组，列顺序见组件说明。
	 */
	@SimpleFunction
	集合 取联系人();

	/**
	 * 后台读取联系人，请求标记为空文本；沿用重复请求和销毁后的拒绝规则。
	 *
	 * @return 接受请求返回真，不表示读取成功。
	 */
	@SimpleFunction
	boolean 异步取联系人();

	/**
	 * 后台读取联系人；重复请求或组件已销毁时拒绝，不触发完成事件。
	 *
	 * @param requestMark 随完成事件返回的请求标记。
	 * @return 接受请求返回真，不表示读取成功。
	 */
	@SimpleFunction
	boolean 异步取联系人(String requestMark);

	/**
	 * 以一次批量事务创建无指定账号的本地原始联系人、姓名和号码。
	 *
	 * @param name 联系人姓名，不得为空。
	 * @param phone 电话号码，不得为空。
	 * @return 新原始联系人编号；失败为空文本。不会更新同名联系人。
	 */
	@SimpleFunction
	String 添加联系人(String name, String phone);

	/**
	 * 删除一个原始联系人及其数据；系统可能向所属账号同步这次删除。
	 *
	 * @param rawContactId 本组件返回的原始联系人编号，不能传姓名或聚合联系人编号。
	 * @return 实际删除一条原始联系人返回真；不存在或失败返回假。
	 */
	@SimpleFunction
	boolean 删除联系人(String rawContactId);

	/**
	 * 读取当前可访问的联系人分组。
	 *
	 * @return 分组集合，每项为文本数组：{@code [分组编号、名称、账号名称、账号类型、数据集]}。
	 */
	@SimpleFunction
	集合 取联系人分组();

	/**
	 * 读取指定分组中的联系人。
	 *
	 * @param groupId 分组编号。
	 * @return 该分组中的联系人，四列结构与取联系人相同。
	 */
	@SimpleFunction
	集合 取分组联系人(String groupId);

	/**
	 * 创建指定账号的分组；本地分组的账号名称和类型均传空文本，设备可能不支持。
	 *
	 * @param name 分组名称，不得为空。
	 * @param accountName 已存在账号的名称。
	 * @param accountType 已存在账号的类型，必须与账号名称同时为空或同时非空。
	 * @return 分组编号；失败为空文本。不会创建或注册系统账号。
	 */
	@SimpleFunction
	String 新建联系人分组(String name, String accountName, String accountType);

	/**
	 * 将指定原始联系人加入分组。
	 *
	 * @param groupId 分组编号。
	 * @param rawContactId 同账号、同数据集的原始联系人编号。
	 * @return 添加成功或已属于该分组返回真；失败返回假。
	 */
	@SimpleFunction
	boolean 添加分组联系人(String groupId, String rawContactId);

	/**
	 * 仅移除分组关系，保留联系人。
	 *
	 * @param groupId 分组编号。
	 * @param rawContactId 原始联系人编号。
	 * @return 实际移除关系返回真，不存在或失败返回假。
	 */
	@SimpleFunction
	boolean 移除分组联系人(String groupId, String rawContactId);

	/**
	 * 仅删除分组，保留联系人。
	 *
	 * @param groupId 分组编号。
	 * @return 实际删除分组返回真，不存在或失败返回假。
	 */
	@SimpleFunction
	boolean 删除联系人分组(String groupId);
}
