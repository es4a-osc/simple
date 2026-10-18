package simple.runtime.collections;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlSerializer;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.XML错误;

/**
 * 按节点生成 XML 的普通对象，实际输出字节与文档声明使用相同编码。
 *
 * <p>每个对象独立保存文档；文本和属性由系统转义，不接收原始 XML 片段。
 * 完成文档后可导出文本或字节集，文件与网络访问由调用者处理。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class XML生成器 {

	private static final String XML_NAMESPACE = "http://www.w3.org/XML/1998/namespace";
	private static final String XMLNS_NAMESPACE = "http://www.w3.org/2000/xmlns/";

	private final Set<String> attributes = new HashSet<String>();
	private XmlSerializer serializer;
	private ByteArrayOutputStream output;
	private Charset charset;
	private boolean rootStarted;
	private boolean finished;
	private boolean acceptingAttributes;
	private String lastError = "";

	/**
	 * 创建尚未开始文档的生成器，不提前分配输出缓冲区。
	 */
	public XML生成器() {
	}

	/* 文档、命名空间与节点写入 */

	/**
	 * 以 UTF-8 开始新的 XML 文档，替换此前生成状态。
	 *
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 开始文档() {
		开始文档("UTF-8");
	}

	/**
	 * 使用指定编码开始新文档，XML 声明与实际输出字节使用同一编码。
	 *
	 * @param encoding 实际输出字节采用的字符集，例如 UTF-8、UTF-16 或 GB18030。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 开始文档(String encoding) {
		重置();
		try {
			if (encoding == null || encoding.length() == 0) {
				throw new IllegalArgumentException("编码不得为空");
			}
			charset = Charset.forName(encoding);
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			XmlSerializer current = XmlPullParserFactory.newInstance().newSerializer();
			current.setOutput(bytes, charset.name());
			current.startDocument(charset.name(), null);
			output = bytes;
			serializer = current;
		} catch (XmlPullParserException | IOException | IllegalArgumentException error) {
			throw failure("开始 XML 文档失败", error);
		}
	}

	/**
	 * 在目标开始节点之前设置命名空间前缀；预定义 xml 前缀只能绑定其标准 URI。
	 *
	 * @param prefix 命名空间前缀，空文本表示默认命名空间。
	 * @param namespace 命名空间 URI；空 URI 只允许使用空前缀。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 设置命名空间(String prefix, String namespace) {
		XmlSerializer current = requireDocument();
		if (prefix == null || (prefix.length() > 0 && !isName(prefix))) {
			throw failure("命名空间前缀无效", null);
		}
		checkText(prefix, "命名空间前缀");
		checkText(namespace, "命名空间");
		if ("xmlns".equals(prefix) || XMLNS_NAMESPACE.equals(namespace)
			|| ("xml".equals(prefix) != XML_NAMESPACE.equals(namespace))
			|| (prefix.length() > 0 && namespace.length() == 0)) {
			throw failure("命名空间绑定无效", null);
		}
		try {
			current.setPrefix(prefix, namespace);
			acceptingAttributes = false;
		} catch (IOException | IllegalArgumentException | IllegalStateException error) {
			throw abort("设置命名空间失败", error);
		}
	}

	/**
	 * 开始无命名空间节点，必须显式结束后才能结束文档。
	 *
	 * @param name 不含前缀的元素本地名称。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 开始节点(String name) {
		开始节点("", name);
	}

	/**
	 * 开始指定命名空间的节点；可先设置前缀，未指定时由系统生成前缀。
	 *
	 * @param namespace 元素命名空间 URI，无命名空间使用空文本。
	 * @param name 元素本地名称，不含前缀。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 开始节点(String namespace, String name) {
		XmlSerializer current = requireDocument();
		checkName(name);
		checkText(namespace, "命名空间");
		if (XMLNS_NAMESPACE.equals(namespace)) {
			throw failure("元素不能使用 xmlns 命名空间", null);
		}
		if (rootStarted && current.getDepth() == 0) {
			throw failure("XML 文档只能包含一个根节点", null);
		}
		try {
			current.startTag(namespace, name);
			rootStarted = true;
			acceptingAttributes = true;
			attributes.clear();
		} catch (IOException | IllegalArgumentException | IllegalStateException error) {
			throw abort("开始节点失败", error);
		}
	}

	/**
	 * 为刚开始的节点写入无命名空间属性。
	 *
	 * @param name 无命名空间的属性名称。
	 * @param value 属性文本，XML 特殊字符由系统转义。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 设置属性(String name, String value) {
		设置属性("", name, value);
	}

	/**
	 * 为刚开始的节点写属性，同一节点不允许重复的命名空间和名称组合。
	 *
	 * @param namespace 属性命名空间 URI，无命名空间使用空文本。
	 * @param name 属性本地名称，不含前缀。
	 * @param value 属性文本，XML 特殊字符由系统转义。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 设置属性(String namespace, String name, String value) {
		XmlSerializer current = requireDocument();
		if (!acceptingAttributes) {
			throw failure("属性必须紧跟在开始节点之后设置", null);
		}
		checkName(name);
		checkText(namespace, "命名空间");
		checkText(value, "属性内容");
		if (XMLNS_NAMESPACE.equals(namespace) || (namespace.length() == 0 && "xmlns".equals(name))) {
			throw failure("请使用设置命名空间声明前缀", null);
		}
		String key = namespace + '\0' + name;
		if (attributes.contains(key)) {
			throw failure("当前节点已存在该属性：" + name, null);
		}
		try {
			current.attribute(namespace, name, value);
			attributes.add(key);
		} catch (IOException | IllegalArgumentException | IllegalStateException error) {
			throw abort("设置属性失败", error);
		}
	}

	/**
	 * 向当前元素追加文本，自动转义 XML 特殊字符；可与子元素交替形成混合内容。
	 *
	 * @param text 当前元素中的文本，不是 XML 片段。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 设置内容(String text) {
		XmlSerializer current = requireDocument();
		if (current.getDepth() == 0) {
			throw failure("文本必须写入根节点内部", null);
		}
		checkText(text, "节点内容");
		try {
			current.text(text);
			acceptingAttributes = false;
		} catch (IOException | IllegalArgumentException | IllegalStateException error) {
			throw abort("设置节点内容失败", error);
		}
	}

	/**
	 * 结束当前无命名空间节点，必须与最近未结束节点对应。
	 *
	 * @param name 当前无命名空间元素的本地名称。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 结束节点(String name) {
		结束节点("", name);
	}

	/**
	 * 按命名空间和本地名称结束当前节点，名称不匹配时拒绝修改。
	 *
	 * @param namespace 当前元素的命名空间 URI。
	 * @param name 当前元素的本地名称，不含前缀。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 结束节点(String namespace, String name) {
		XmlSerializer current = requireDocument();
		if (namespace == null || name == null || current.getDepth() == 0
			|| !name.equals(current.getName()) || !namespace.equals(current.getNamespace())) {
			throw failure("结束节点必须与当前节点的命名空间及名称一致", null);
		}
		try {
			current.endTag(namespace, name);
			acceptingAttributes = false;
		} catch (IOException | IllegalArgumentException | IllegalStateException error) {
			throw abort("结束节点失败", error);
		}
	}

	/**
	 * 结束文档并完成编码输出；必须已有根节点且全部节点均已显式结束。
	 *
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 结束文档() {
		XmlSerializer current = requireDocument();
		if (!rootStarted || current.getDepth() != 0) {
			throw failure("结束文档前必须创建并闭合唯一根节点", null);
		}
		try {
			current.endDocument();
			finished = true;
		} catch (IOException | IllegalArgumentException | IllegalStateException error) {
			throw abort("结束文档失败", error);
		}
	}

	/**
	 * 将已完成文档按生成时的编码解码为文本；保存该文本时仍需选择相同文件编码。
	 *
	 * @return 含 XML 声明的完整文本；实际编码字节请使用导出字节集。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 导出文本() {
		requireFinished();
		return new String(output.toByteArray(), charset);
	}

	/**
	 * 导出已完成文档的独立字节副本，实际字节编码与 XML 声明保持一致。
	 *
	 * @return 可直接交给文件写入或网络发送的 XML 字节集。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public byte[] 导出字节集() {
		requireFinished();
		return output.toByteArray();
	}

	/**
	 * 读取最近一次本对象抛出的 XML 错误说明，开始文档或重置时清空。
	 *
	 * @return 错误说明，没有错误时为空文本。
	 */
	@SimpleFunction
	public String 取错误信息() {
		return lastError;
	}

	/**
	 * 清除生成文档及错误信息，释放对输出数据的引用。
	 */
	@SimpleFunction
	public void 重置() {
		serializer = null;
		output = null;
		charset = null;
		rootStarted = false;
		finished = false;
		acceptingAttributes = false;
		attributes.clear();
		lastError = "";
	}

	/* 状态、名称与编码校验 */

	/**
	 * 校验文档仍可写入。
	 *
	 * @return 当前序列化器。
	 */
	private XmlSerializer requireDocument() {
		if (serializer == null || finished) {
			throw failure("请先开始尚未结束的 XML 文档", null);
		}
		return serializer;
	}

	/**
	 * 仅允许导出已经成功完成的文档。
	 */
	private void requireFinished() {
		if (!finished || output == null) {
			throw failure("请先成功结束 XML 文档", null);
		}
	}

	/**
	 * 验证元素或属性使用 XML 1.0 本地名称，前缀通过命名空间接口设置。
	 *
	 * @param name 不含冒号的本地名称。
	 */
	private void checkName(String name) {
		if (!isName(name)) {
			throw failure("XML 本地名称无效：" + name, null);
		}
		checkText(name, "XML 名称");
	}

	/**
	 * 按 XML 1.0 第五版的名称字符范围校验 NCName，排除命名空间分隔冒号。
	 *
	 * @param name 待校验名称。
	 * @return 名称有效返回真。
	 */
	private static boolean isName(String name) {
		if (name == null || name.length() == 0 || !isNameStart(name.codePointAt(0))) {
			return false;
		}
		for (int offset = Character.charCount(name.codePointAt(0)); offset < name.length();) {
			int value = name.codePointAt(offset);
			if (!isNameStart(value) && value != '-' && value != '.' && value != 0xB7
				&& !(value >= '0' && value <= '9') && !(value >= 0x300 && value <= 0x36F)
				&& !(value >= 0x203F && value <= 0x2040)) {
				return false;
			}
			offset += Character.charCount(value);
		}
		return true;
	}

	/**
	 * 判断 XML 本地名称的首字符，不允许前缀分隔冒号。
	 *
	 * @param value Unicode 码点。
	 * @return 可以作为本地名称首字符返回真。
	 */
	private static boolean isNameStart(int value) {
		return value == '_' || (value >= 'A' && value <= 'Z') || (value >= 'a' && value <= 'z')
			|| (value >= 0xC0 && value <= 0xD6) || (value >= 0xD8 && value <= 0xF6)
			|| (value >= 0xF8 && value <= 0x2FF) || (value >= 0x370 && value <= 0x37D)
			|| (value >= 0x37F && value <= 0x1FFF) || (value >= 0x200C && value <= 0x200D)
			|| (value >= 0x2070 && value <= 0x218F) || (value >= 0x2C00 && value <= 0x2FEF)
			|| (value >= 0x3001 && value <= 0xD7FF) || (value >= 0xF900 && value <= 0xFDCF)
			|| (value >= 0xFDF0 && value <= 0xFFFD) || (value >= 0x10000 && value <= 0xEFFFF);
	}

	/**
	 * 拒绝非法 XML 字符与目标编码不能表示的文本，避免静默替换成问号。
	 *
	 * @param value 要写入的文本。
	 * @param label 字段说明。
	 */
	private void checkText(String value, String label) {
		if (value == null) {
			throw failure(label + "不得为空引用", null);
		}
		for (int offset = 0; offset < value.length();) {
			int point = value.codePointAt(offset);
			if (point != 9 && point != 10 && point != 13 && !(point >= 0x20 && point <= 0xD7FF)
				&& !(point >= 0xE000 && point <= 0xFFFD) && !(point >= 0x10000 && point <= 0x10FFFF)) {
				throw failure(label + "含有非法 XML 字符", null);
			}
			offset += Character.charCount(point);
		}
		if (!charset.newEncoder().canEncode(value)) {
			throw failure(label + "不能使用 " + charset.name() + " 编码", null);
		}
	}

	/**
	 * 系统写入失败时使文档失效，禁止导出部分结果。
	 *
	 * @param message 操作说明。
	 * @param cause 系统异常。
	 * @return 可捕获的 XML 错误。
	 */
	private XML错误 abort(String message, Throwable cause) {
		serializer = null;
		output = null;
		finished = false;
		return failure(message, cause);
	}

	/**
	 * 保存错误原因并保留底层异常。
	 *
	 * @param message 操作说明。
	 * @param cause 可为空的系统异常。
	 * @return 可捕获的 XML 错误。
	 */
	private XML错误 failure(String message, Throwable cause) {
		lastError = message + (cause == null ? "" : "：" + cause.getMessage());
		XML错误 error = new XML错误(lastError);
		if (cause != null) {
			error.initCause(cause);
		}
		return error;
	}
}
