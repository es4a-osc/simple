package simple.runtime.components;

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.errors.文件读写错误;

/**
 * 调用系统文档选择界面的非可视组件，不申请广泛存储访问权限。
 *
 * <p>返回 content URI 而非物理路径；通过本组件读写，或复制到私有目录供路径接口使用。
 * 文件功能要求 Android 4.4，目录功能要求 Android 5.0；访问范围和读写能力由提供方授权决定。
 * 同步读写可能阻塞，不在界面线程处理大文件或慢速云文档。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 文件选择器 extends 组件 {

	/* 组件事件 */

	/**
	 * 用户选择文件成功时在主线程触发，URI 不是物理文件路径。
	 *
	 * @param uris 选中文件的 content URI 数组，按返回顺序去重。
	 */
	@SimpleEvent
	void 文件选择完毕(String[] uris);

	/**
	 * 用户确定保存位置并创建空文件后触发。
	 *
	 * @param uri 系统创建的新文件 URI，尚未写入业务数据。
	 */
	@SimpleEvent
	void 文件创建完毕(String uri);

	/**
	 * 用户确认目录授权后触发，后续仅可访问授权目录及其子项。
	 *
	 * @param uri 用户授权的目录树 URI。
	 */
	@SimpleEvent
	void 目录选择完毕(String uri);

	/**
	 * 用户取消文件、创建或目录选择时触发，不作为失败处理。
	 */
	@SimpleEvent
	void 取消选择();

	/**
	 * 无法发起选择或返回结果无效时在主线程触发。
	 *
	 * @param message 不支持的系统、参数、启动或结果错误说明。
	 */
	@SimpleEvent
	void 选择失败(String message);

	/* 组件属性 */

	/**
	 * 当前是否有尚未返回的系统选择请求。
	 *
	 * @return 有未结束请求返回真。
	 */
	@SimpleProperty
	boolean 正在选择();

	/**
	 * 最近一次失败的说明；成功开始选择或完成同步操作时清空。
	 *
	 * @return 最近错误说明，无错误时为空文本。
	 */
	@SimpleProperty
	String 错误信息();

	/* 组件方法 */

	/**
	 * 选择单个任意类型文件，要求 Android 4.4 或更高版本。
	 */
	@SimpleFunction
	void 选择文件();

	/**
	 * 按 MIME 类型选择单个文件。
	 *
	 * @param mimeType MIME 类型，例如 text/plain、image/*，也可选择任意类型。
	 */
	@SimpleFunction
	void 选择文件(String mimeType);

	/**
	 * 选择一个或多个同类文件，结果通过文件选择完毕返回。
	 *
	 * @param mimeType 允许的 MIME 类型。
	 * @param multiple 是否允许返回多个文件。
	 */
	@SimpleFunction
	void 选择文件(String mimeType, boolean multiple);

	/**
	 * 打开系统文件选择器，可提示初始位置，不承诺系统一定采用。
	 *
	 * @param mimeType 允许的 MIME 类型。
	 * @param multiple 是否允许多选，系统提供方可能只返回一个文件。
	 * @param initialUri 文档或目录 content URI；空文本不指定，Android 8.0 起仅作为提示。
	 */
	@SimpleFunction
	void 选择文件(String mimeType, boolean multiple, String initialUri);

	/**
	 * 让用户选择保存位置并创建文件，默认 MIME 类型为 application/octet-stream。
	 *
	 * @param name 建议文件名；同名文件由系统处理，不承诺覆盖。
	 */
	@SimpleFunction
	void 创建文件(String name);

	/**
	 * 创建指定类型的文件，返回 URI 后再写入数据。
	 *
	 * @param name 建议文件名。
	 * @param mimeType 准确的 MIME 类型，例如 application/xml。
	 */
	@SimpleFunction
	void 创建文件(String name, String mimeType);

	/**
	 * 打开系统保存界面；取消时不会由组件额外写入数据。
	 *
	 * @param name 不含路径分隔符的建议文件名。
	 * @param mimeType 不能包含通配符的 MIME 类型。
	 * @param initialUri 空文本或 content URI，Android 8.0 起作为初始位置提示。
	 */
	@SimpleFunction
	void 创建文件(String name, String mimeType, String initialUri);

	/**
	 * 让用户授权一个目录及其子项，要求 Android 5.0 或更高版本。
	 */
	@SimpleFunction
	void 选择目录();

	/**
	 * 打开系统目录授权界面，系统禁止访问的目录仍不能选择。
	 *
	 * @param initialUri 空文本或目录 content URI，系统可忽略此提示。
	 */
	@SimpleFunction
	void 选择目录(String initialUri);

	/**
	 * 查询提供方给出的显示名称，目录树 URI 会转换为对应文档 URI。
	 *
	 * @param uri 文件或目录的 content URI。
	 * @return 提供方的显示名称；未提供时为空文本。
	 * @throws 文件读写错误 URI、授权或查询操作失败。
	 */
	@SimpleFunction
	String 取名称(String uri);

	/**
	 * 查询提供方报告的字节大小，云文件等可能无法提供大小。
	 *
	 * @param uri 文件 content URI。
	 * @return 字节大小；未知时为 -1，不代表空文件。
	 * @throws 文件读写错误 URI、授权或查询操作失败。
	 */
	@SimpleFunction
	long 取文件大小(String uri);

	/**
	 * 查询 MIME 类型，目录类型为 vnd.android.document/directory。
	 *
	 * @param uri 文件或目录 content URI。
	 * @return MIME 类型；提供方未提供时为空文本。
	 * @throws 文件读写错误 URI、授权或查询操作失败。
	 */
	@SimpleFunction
	String 取文件类型(String uri);

	/**
	 * 将文件完整读入内存；大文件优先复制到文件，云文件读取可能阻塞。
	 *
	 * @param uri 已经获得读权限的文件 content URI。
	 * @return 文件的完整字节内容。
	 * @throws 文件读写错误 URI、授权或读取失败。
	 */
	@SimpleFunction
	byte[] 读入字节集(String uri);

	/**
	 * 以覆盖截断模式写入文件，失败可能已经写入部分内容，不保证跨提供方原子提交。
	 *
	 * @param uri 已经获得写权限的文件 content URI。
	 * @param data 要写入的字节集，不得为空引用。
	 * @throws 文件读写错误 URI、授权、数据或写入失败。
	 */
	@SimpleFunction
	void 写出字节集(String uri, byte[] data);

	/**
	 * 将文档流式复制到本地文件并返回绝对路径，供 EXCEL 等路径接口直接使用；目标目录须已存在。
	 *
	 * @param uri 已经获得读权限的文件 content URI。
	 * @param targetPath 相对路径以应用私有 files 目录为基准，也可传入有权限的绝对路径。
	 * @return 完成复制后的本地绝对路径。
	 * @throws 文件读写错误 URI、授权或本地文件操作失败。
	 */
	@SimpleFunction
	String 复制到文件(String uri, String targetPath);

	/**
	 * 保留 URI 的读权限；用户移动、删除文档或撤销授权后仍可能失效。
	 *
	 * @param uri 系统选择返回的可持久授权 URI。
	 * @return 成功保留返回真，否则返回假并设置错误信息。
	 */
	@SimpleFunction
	boolean 保留访问授权(String uri);

	/**
	 * 保留读权限或读写权限，不会扩大系统选择时获得的授权范围。
	 *
	 * @param uri 系统选择返回的可持久授权 URI。
	 * @param writable 是否同时保留写权限，必须已经获得相应授权。
	 * @return 成功返回真；提供方不支持或权限不足返回假。
	 */
	@SimpleFunction
	boolean 保留访问授权(String uri, boolean writable);

	/**
	 * 释放该 URI 自身的持久读写授权，不删除文件或目录。
	 *
	 * @param uri 先前保留授权的原始 URI。
	 * @return 成功释放返回真，没有对应记录或失败返回假。
	 */
	@SimpleFunction
	boolean 释放访问授权(String uri);

	/**
	 * 列出已授权目录的直接子项 URI，不递归、不保证提供方返回顺序，要求 Android 5.0。
	 *
	 * @param directoryUri 已授权目录树或其目录文档 URI。
	 * @return 直接子项的 content URI 数组，空目录返回空数组。
	 * @throws 文件读写错误 目录、授权或查询失败。
	 */
	@SimpleFunction
	String[] 取子项(String directoryUri);

	/**
	 * 在已授权目录内创建子文件，不额外弹出选择界面，要求 Android 5.0。
	 *
	 * @param directoryUri 已获得写权限的目录树或其目录文档 URI。
	 * @param name 建议显示名称。
	 * @param mimeType 不能包含通配符的准确 MIME 类型。
	 * @return 新文件 URI；同名文件是否重命名由提供方决定。
	 * @throws 文件读写错误 目录、授权或创建操作失败。
	 */
	@SimpleFunction
	String 创建子文件(String directoryUri, String name, String mimeType);
}
