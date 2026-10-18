package simple.runtime.collections;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.Charset;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.XML错误;

/**
 * 按节点主动遍历 XML 的普通对象，读取属性与内容不会推进解析位置。
 *
 * <p>每个对象独立保存解析状态；仅处理传入内存的数据，不读取文件或网络。
 * 启用命名空间，不提供 DTD 验证、XPath 或原始排版无损往返。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class XML解析器 {

	/**
	 * 文档开始节点类型。
	 */
	@SimpleDataElement
	public static final int 节点_文档开始 = XmlPullParser.START_DOCUMENT;

	/**
	 * 文档结束节点类型。
	 */
	@SimpleDataElement
	public static final int 节点_文档结束 = XmlPullParser.END_DOCUMENT;

	/**
	 * 开始节点类型。
	 */
	@SimpleDataElement
	public static final int 节点_开始 = XmlPullParser.START_TAG;

	/**
	 * 结束节点类型。
	 */
	@SimpleDataElement
	public static final int 节点_结束 = XmlPullParser.END_TAG;

	/**
	 * 文本节点类型。
	 */
	@SimpleDataElement
	public static final int 节点_文本 = XmlPullParser.TEXT;

	private XmlPullParser parser;
	private String lastError = "";

	/**
	 * 创建尚未设置 XML 输入的解析器。
	 */
	public XML解析器() {
	}

	/* 解析与节点读取 */

	/**
	 * 从文本开始解析并停在文档开始位置；文本已经解码，不按 XML 声明再次转换编码。
	 *
	 * @param text 完整 XML 文本，不得为空引用。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 解析文本(String text) {
		重置();
		if (text == null) {
			throw failure("XML 文本不得为空引用", null);
		}

		try {
			XmlPullParser current = createParser();
			current.setInput(new StringReader(text));
			parser = current;
		} catch (XmlPullParserException error) {
			throw failure("开始解析失败", error);
		}
	}

	/**
	 * 按 BOM 或 XML 声明自动识别字节编码，并停在文档开始位置。
	 *
	 * @param data XML 原始字节，不得为空引用。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 解析字节集(byte[] data) {
		解析字节集(data, "");
	}

	/**
	 * 从字节集开始解析，可显式指定字符集；重新开始会替换原解析状态。
	 *
	 * @param data XML 原始字节，不得为空引用。
	 * @param encoding 指定字符集；空文本表示自动识别。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public void 解析字节集(byte[] data, String encoding) {
		重置();
		if (data == null || encoding == null) {
			throw failure("XML 数据和编码不得为空引用", null);
		}

		try {
			String charset = encoding.length() == 0 ? null : Charset.forName(encoding).name();
			XmlPullParser current = createParser();
			current.setInput(new ByteArrayInputStream(data), charset);
			parser = current;
		} catch (XmlPullParserException | IllegalArgumentException error) {
			throw failure("开始解析失败", error);
		}
	}

	/**
	 * 推进到下一个节点，返回节点类型；文档结束后重复调用仍返回文档结束。
	 *
	 * 跳过注释和处理指令，CDATA 与实体解码后的文本按文本节点读取。
	 *
	 * @return 节点_文档开始、节点_文档结束、节点_开始、节点_结束或节点_文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public int 读取下一个() {
		XmlPullParser current = requireParser();
		try {
			return current.getEventType() == 节点_文档结束 ? 节点_文档结束 : current.next();
		} catch (XmlPullParserException | IOException error) {
			String position = "第 " + current.getLineNumber() + " 行，第 " + current.getColumnNumber() + " 列";
			parser = null;
			throw failure("读取 XML 失败（" + position + "）", error);
		}
	}

	/**
	 * 读取当前位置的节点类型，不推进解析器。
	 *
	 * @return 当前节点类型，开始解析后首先为节点_文档开始。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public int 取节点类型() {
		try {
			return requireParser().getEventType();
		} catch (XmlPullParserException error) {
			throw failure("读取节点类型失败", error);
		}
	}

	/**
	 * 读取元素的本地名称，不含命名空间前缀；其他位置返回空文本。
	 *
	 * @return 当前元素本地名称，非元素位置为空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取节点名称() {
		return text(requireParser().getName());
	}

	/**
	 * 读取当前文本节点的内容，不移动位置，也不汇总子节点文本。
	 *
	 * @return 文本节点的解码内容；非文本位置为空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取节点内容() {
		return 取节点类型() == 节点_文本 ? text(requireParser().getText()) : "";
	}

	/**
	 * 读取当前元素的命名空间前缀，不移动位置。
	 *
	 * @return 当前前缀，没有前缀或非元素位置为空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取前缀() {
		return text(requireParser().getPrefix());
	}

	/**
	 * 读取当前元素的命名空间 URI，不移动位置。
	 *
	 * @return 命名空间 URI，没有命名空间或非元素位置为空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取命名空间() {
		return text(requireParser().getNamespace());
	}

	/**
	 * 读取当前节点深度，根元素深度为 1。
	 *
	 * @return 当前深度，文档开始及结束时为 0。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public int 取节点深度() {
		return requireParser().getDepth();
	}

	/**
	 * 读取当前解析位置的行号。
	 *
	 * @return 从 1 开始的行号，系统无法提供时为 -1。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public int 取行号() {
		return requireParser().getLineNumber();
	}

	/**
	 * 读取当前解析位置的列号。
	 *
	 * @return 从 1 开始的列号，系统无法提供时为 -1。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public int 取列号() {
		return requireParser().getColumnNumber();
	}

	/**
	 * 读取开始节点的普通属性数，不包含命名空间声明。
	 *
	 * @return 开始节点的属性数；其他位置为 0。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public int 取属性个数() {
		return Math.max(0, requireParser().getAttributeCount());
	}

	/**
	 * 读取指定属性的本地名称。
	 *
	 * @param index 属性索引，从 0 开始，必须小于取属性个数。
	 * @return 对应属性值，没有前缀或命名空间时返回空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取属性名称(int index) {
		XmlPullParser current = requireAttribute(index);
		return text(current.getAttributeName(index));
	}

	/**
	 * 读取指定属性的解码内容。
	 *
	 * @param index 属性索引，从 0 开始，必须小于取属性个数。
	 * @return 对应属性值，没有前缀或命名空间时返回空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取属性内容(int index) {
		XmlPullParser current = requireAttribute(index);
		return text(current.getAttributeValue(index));
	}

	/**
	 * 读取指定属性的命名空间前缀。
	 *
	 * @param index 属性索引，从 0 开始，必须小于取属性个数。
	 * @return 对应属性值，没有前缀或命名空间时返回空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取属性前缀(int index) {
		XmlPullParser current = requireAttribute(index);
		return text(current.getAttributePrefix(index));
	}

	/**
	 * 读取指定属性的命名空间 URI。
	 *
	 * @param index 属性索引，从 0 开始，必须小于取属性个数。
	 * @return 对应属性值，没有前缀或命名空间时返回空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取属性命名空间(int index) {
		XmlPullParser current = requireAttribute(index);
		return text(current.getAttributeNamespace(index));
	}

	/**
	 * 按名称读取无命名空间的属性，不移动位置。
	 *
	 * @param name 无命名空间的属性本地名称。
	 * @return 属性内容；不存在时为空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取指定属性内容(String name) {
		return 取指定属性内容("", name);
	}

	/**
	 * 按命名空间 URI 和本地名称读取属性，不以显示前缀作为身份。
	 *
	 * @param namespace 属性命名空间 URI，无命名空间使用空文本。
	 * @param name 属性本地名称，不含前缀。
	 * @return 属性内容；不存在时为空文本。
	 * @throws XML错误 输入、状态或 XML 数据无效。
	 */
	@SimpleFunction
	public String 取指定属性内容(String namespace, String name) {
		XmlPullParser current = requireParser();
		if (取节点类型() != 节点_开始 || namespace == null || name == null || name.length() == 0) {
			throw failure("请在开始节点提供属性命名空间和本地名称", null);
		}

		return text(current.getAttributeValue(namespace, name));
	}

	/**
	 * 读取最近一次本对象抛出的 XML 错误说明，开始解析或重置时清空。
	 *
	 * @return 错误说明，没有错误时为空文本。
	 */
	@SimpleFunction
	public String 取错误信息() {
		return lastError;
	}

	/**
	 * 清除解析位置和错误信息，之后可开始解析另一份数据。
	 */
	@SimpleFunction
	public void 重置() {
		parser = null;
		lastError = "";
	}

	/* 状态与错误处理 */

	/**
	 * 创建启用命名空间的系统解析器，不从外部解析 DTD。
	 *
	 * @return 尚未设置输入的新解析器。
	 * @throws XmlPullParserException 系统无法创建或配置解析器。
	 */
	private XmlPullParser createParser() throws XmlPullParserException {
		XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
		factory.setNamespaceAware(true);
		return factory.newPullParser();
	}

	/**
	 * 验证已经开始解析且未因格式错误失效。
	 *
	 * @return 当前解析器。
	 */
	private XmlPullParser requireParser() {
		if (parser == null) {
			throw failure("请先设置有效的 XML 输入", null);
		}
		return parser;
	}

	/**
	 * 校验属性索引，其他读取函数不改变解析位置。
	 *
	 * @param index 从 0 开始的属性索引。
	 * @return 当前解析器。
	 */
	private XmlPullParser requireAttribute(int index) {
		XmlPullParser current = requireParser();
		if (index < 0 || index >= current.getAttributeCount()) {
			throw failure("属性索引超出范围：" + index, null);
		}

		return current;
	}

	/**
	 * 将系统的空值转换为 Simple 可直接使用的空文本。
	 *
	 * @param value 可能为空的节点字段。
	 * @return 非空引用文本。
	 */
	private static String text(String value) {
		return value == null ? "" : value;
	}

	/**
	 * 保存错误原因并保留底层异常，交给 Simple 错误分支处理。
	 *
	 * @param message 本次操作说明。
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
