/*
 * Copyright 2009 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package simple.compiler.scanner;

/**
 * Simple的语言标记（有关Simple语言的更多信息，请参阅Simple语言指南）。
 *
 * @author Herbert Czymontek
 */
public enum TokenKind {

	// 未初始化的令牌 - 扫描器的任何客户端都不应该看到这种情况。
	TOK_NONE(""),

	/**
	 * {@code <标识符>} - {@code <identifier>}
	 *
	 * <p>标识符令牌，如用户定义的变量名等。
	 */
	TOK_IDENTIFIER("<标识符>"),

	// 关键字令牌。

	/**
	 * {@code 别名} - {@code Alias}
	 */
	TOK_ALIAS("别名"),

	/**
	 * {@code 且} - {@code And}
	 */
	TOK_AND("且"),

	/**
	 * {@code 为} - {@code As}
	 */
	TOK_AS("为"),

	/**
	 * {@code 逻辑型} - {@code Boolean}
	 */
	TOK_BOOLEAN("逻辑型"),

	/**
	 * {@code 传址} - {@code ByRef}
	 */
	TOK_BYREF("传址"),

		/**
	 * {@code 字节型} - {@code Byte}
	 */
	TOK_BYTE("字节型"),

	/**
	 * {@code 传值} - {@code ByVal}
	 */
	TOK_BYVAL("传值"),

	/**
	 * {@code 分支} - {@code Case}
	 */
	TOK_CASE("分支"),

		/**
	 * {@code 常量} - {@code Const}
	 */
	TOK_CONST("常量"),

	/**
	 * {@code 日期时间型} - {@code Date}
	 */
	TOK_DATE("日期时间型"),

	/**
	 * {@code 变量} - {@code Dim}
	 */
	TOK_DIM("变量"),

		/**
	 * {@code 执行} - {@code Do}
	 */
	TOK_DO("执行"),

	/**
	 * {@code 双精度小数型} - {@code Double}
	 */
	TOK_DOUBLE("双精度小数型"),

	/**
	 * {@code 每个} - {@code Each}
	 */
	TOK_EACH("每个"),

	/**
	 * {@code 否则} - {@code Else}
	 */
	TOK_ELSE("否则"),

	/**
	 * {@code 否则如果} - {@code ElseIf}
	 */
	TOK_ELSEIF("否则如果"),

	/**
	 * {@code 结束} - {@code End}
	 */
	TOK_END("结束"),

	/**
	 * {@code 错误} - {@code Error}
	 */
	TOK_ERROR("错误"),

	/**
	 * {@code 事件} - {@code Event}
	 */
	TOK_EVENT("事件"),

	/**
	 * {@code 退出} - {@code Exit}
	 */
	TOK_EXIT("退出"),

	/**
	 * {@code 循环} - {@code For}
	 */
	TOK_FOR("循环"),

	/**
	 * {@code 函数} - {@code Function}
	 */
	TOK_FUNCTION("函数"),

	/**
	 * {@code 获取} - {@code Get}
	 */
	TOK_GET("获取"),

	/**
	 * {@code 如果} - {@code If}
	 */
	TOK_IF("如果"),

	/**
	 * {@code 从} - {@code In}
	 */
	TOK_IN("从"),

	/**
	 * {@code 整数型} - {@code Integer}
	 */
	TOK_INTEGER("整数型"),

	/**
	 * {@code 是} - {@code Is}
	 *
	 * <p>比较左操作数是否等于右操作数。注意，操作数必须是{@code 对象}或者{@code 数组}类型。
	 */
	TOK_IS("是"),

	/**
	 * {@code 非} - {@code IsNot}
	 *
	 * <p>比较左操作数不等于右操作数。
	 */
	TOK_ISNOT("非"),

	/**
	 * {@code 匹配} - {@code Like}
	 */
	TOK_LIKE("匹配"),

	/**
	 * {@code 长整数型} - {@code Long}
	 */
	TOK_LONG("长整数型"),

	/**
	 * {@code 本对象} - {@code Me}
	 */
	TOK_ME("本对象"),

	/**
	 * {@code %} - 求模运算符
	 */
	TOK_MOD("%"),

	/**
	 * {@code 下个} - {@code Next}
	 */
	TOK_NEXT("下个"),

	/**
	 * {@code 创建} - {@code New}
	 */
	TOK_NEW("创建"),

	/**
	 * {@code 取反} - {@code Not}
	 */
	TOK_NOT("取反"),

	/**
	 * {@code 空} - {@code Nothing}
	 */
	TOK_NOTHING("空"),

	/**
	 * {@code 对象} - {@code Object}
	 */
	TOK_OBJECT("对象"),

	/**
	 * {@code 位于} - {@code On}
	 *
	 * <p>{@code 创建 ... 位于 ...}
	 * <p>{@code 位于 错误 ... 结束 错误}
	 */
	TOK_ON("位于"),

	/**
	 * {@code 或} - {@code Or}
	 */
	TOK_OR("或"),

	/**
	 * {@code 属性} - {@code Property}
	 */
	TOK_PROPERTY("属性"),

	/**
	 * {@code 触发事件} - {@code RaiseEvent}
	 */
	TOK_RAISEEVENT("触发事件"),

	/**
	 * {@code 判断} - {@code Select}
	 */
	TOK_SELECT("判断"),

	/**
	 * {@code 设置} - {@code Set}
	 */
	TOK_SET("设置"),

	/**
	 * {@code 短整数型} - {@code Short}
	 */
	TOK_SHORT("短整数型"),

	/**
	 * {@code 单精度小数型} - {@code Single}
	 */
	TOK_SINGLE("单精度小数型"),

	/**
	 * {@code 静态} - {@code Static}
	 *
	 * <p>{@code 静态 变量 <变量名> 为 <变量类型>}
	 * <p>{@code 静态 函数 <函数名>()<返回类型>}
	 * <p>{@code 静态 过程 <过程名>()}
	 */
	TOK_STATIC("静态"),

	/**
	 * {@code 步进} - {@code Step}
	 *
	 * <p>{@code 循环 <循环变量名> = <起始值> 至 <终止值> 步进 <步长>}
	 */
	TOK_STEP("步进"),

	/**
	 * {@code 文本型} - {@code String}
	 */
	TOK_STRING("文本型"),

	/**
	 * {@code 过程} - {@code Sub}
	 */
	TOK_SUB("过程"),

	/**
	 * {@code 则} - {@code Then}
	 */
	TOK_THEN("则"),

	/**
	 * {@code 至} - {@code To}
	 */
	TOK_TO("至"),

	/**
	 * {@code 类型检验} - {@code TypeOf}
	 */
	TOK_TYPEOF("类型检验"),

	/**
	 * {@code 直到} - {@code Until}
	 */
	TOK_UNTIL("直到"),

	/**
	 * {@code 变体型} - {@code Variant}
	 */
	TOK_VARIANT("变体型"),

	/**
	 * {@code 判断循环} - {@code While}
	 */
	TOK_WHILE("判断循环"),

	/**
	 * {@code 异或} - {@code Xor}
	 */
	TOK_XOR("异或"),

	// 常数字面(立即数)令牌。

	/**
	 * {@code <逻辑 常数>} - {@code <boolean constant>}
	 */
	TOK_BOOLEANCONSTANT("<逻辑 常数>"),

	/**
	 * {@code <空 常数>} - {@code <null constant>}
	 */
	TOK_NULLCONSTANT("<空 常数>"),

	/**
	 * {@code <数值 常数>} - {@code <numeric constant>}
	 */
	TOK_NUMERICCONSTANT("<数字 常数>"),

	/**
	 * {@code <文本 常数>}
	 *
	 * <p>原英文名：{@code <string constant>}
	 */
	TOK_STRINGCONSTANT("<文本 常数>"),

	// 运算符令牌。

	/**
	 * {@code (}
	 */
	TOK_OPENPARENTHESIS("("),

	/**
	 * {@code )}
	 */
	TOK_CLOSEPARENTHESIS(")"),

	/**
	 * {@code .}
	 */
	TOK_DOT("."),

	/**
	 * {@code ,}
	 */
	TOK_COMMA(","),

	/**
	 * {@code <}
	 */
	TOK_LESS("<"),

	/**
	 * {@code >}
	 */
	TOK_GREATER(">"),

	/**
	 * {@code <=}
	 */
	TOK_LESSEQUAL("<="),

		/**
	 * {@code >=}
	 */
	TOK_GREATEREQUAL(">="),

	/**
	 * {@code =}
	 */
	TOK_EQUAL("="),

	/**
	 * {@code ^}
	 */
	TOK_EXP("^"),

	/**
	 * {@code <>}
	 */
	TOK_NOTEQUAL("<>"),

	/**
	 * {@code +}
	 */
	TOK_PLUS("+"),

	/**
	 * {@code -}
	 */
	TOK_MINUS("-"),

	/**
	 * {@code *}
	 */
	TOK_TIMES("*"),

	/**
	 * {@code /}
	 */
	TOK_DIVIDE("/"),

	/**
	 * {@code \}
	 */
	TOK_INTEGERDIVIDE("\\"),

	/**
	 * {@code <<}
	 */
	TOK_SHIFTLEFT("<<"),

	/**
	 * {@code >>}
	 */
	TOK_SHIFTRIGHT(">>"),

	/**
	 * {@code &}
	 *
	 * <p>和号，用与连接字符串
	 */
	TOK_AMPERSAND("&"),

	// 特殊的语句结束和文件结束令牌
	//（注意语句结束与行结束是不同的 - 有连续的行('_' 后面跟 '/n' 等价)和显式的语句结束标记 ':'）。
	// 语句连续使用"_"，行结束使用':'

	/**
	 * {@code 语句结束标识符} - {@code end-of-statement}
	 */
	TOK_EOS("语句结束标识符"),

	/**
	 * {@code 文件结束标识符} - {@code end-of-file}
	 */
	TOK_EOF("文件结束标识符"),

	// 以下并不是真正的关键字，但是为它们定义标记是非常有用的……

	/**
	 * {@code 真} - {@code True}
	 *
	 * <p>关联代码：
	 * {@link simple.runtime.helpers.ConvHelpers#boolean2string(boolean b) boolean2string}
	 * {@link simple.runtime.helpers.ConvHelpers#string2boolean(boolean b) string2boolean}
	 */
	TOK_TRUE("真"),

	/**
	 * {@code 假} - {@code False}
	 *
	 * <p>关联代码：
	 * {@link simple.runtime.helpers.ConvHelpers#boolean2string(boolean b) boolean2string}
	 * {@link simple.runtime.helpers.ConvHelpers#string2boolean(String s) string2boolean}
	 */
	TOK_FALSE("假"),

	// 属性部分令牌（这些令牌只能在源文件的属性部分中找到，
	// 而这个部分通常对程序员来说是不可见的 - 除非他们使用任何不支持Simple语言的低级外部文本编辑器）。

	/**
	 * {@code $为} - {@code $As}
	 */
	TOK_$AS("$为"),

	/**
	 * {@code $定义} - {@code $Define}
	 */
	TOK_$DEFINE("$定义"),

	/**
	 * {@code $结束} - {@code $End}
	 */
	TOK_$END("$结束"),

	/**
	 * {@code $窗口} - {@code $Form}
	 */
	TOK_$FORM("$窗口"),

	/**
	 * {@code $接口} - {@code $Interface}
	 */
	TOK_$INTERFACE("$接口"),

	/**
	 * {@code $对象} - {@code $Object}
	 */
	TOK_$OBJECT("$对象"),

	/**
	 * {@code $属性} - {@code $Properties}
	 *
	 * <p>关联代码：
	 * {@link simple.compiler.scanner.Scanner#findSectionStarts(String source) findSectionStarts}
	 */
	TOK_$PROPERTIES("$属性"),

	/**
	 * {@code $资源} - 原英文名：{@code $Source}
	 */
	TOK_$SOURCE("$资源"),

	/**
	 * {@code $服务} - 后续扩展实现
	 *
	 * <p>Simple的Android服务单元
	 * <p>关联代码：
	 * {@link simple.compiler.scanner.Keywords#Keywords() Keywords}
	 * {@link simple.compiler.parser.Parser#parseServicePropertiesSection() parseServicePropertiesSection}
	 * {@link simple.compiler.parser.Parser#parse() parse}
	 * {@link msimple.runtime.collections.服务 服务}
	 */
	TOK_$SERVICE("$服务"),

	/**
	Load -> 加载
	对象事件：该类型对象首次加载到内存时触发；该方法是静态方法，仅可引用静态成员
	关联代码：
	{@link simple.compiler.symbols.EventHandlerSymbol#hasMeArgument() hasMeArgument}
	{@link simple.compiler.symbols.EventHandlerSymbol#resolve(simple.compiler.Compiler, simple.compiler.symbols.FunctionSymbol) resolve}
	*/

	/**
	Initialize -> 初始化
	对象事件：初始化（创建）该对象时触发
	关联代码：
	{@link simple.compiler.symbols.EventHandlerSymbol#resolve(Compiler compiler, FunctionSymbol currentFunction) resolve}
	{@link simple.compiler.statements.synthetic.RaiseInitializeEventStatement#generate(simple.classfiles.Method) RaiseInitializeEventStatement}
	{@link simple.runtime.components.Component#Initialize() Initialize}
	{@link simple.runtime.components.impl.ComponentImpl#Initialize() Initialize}
	{@link simple.runtime.components.impl.android.LocationSensorImpl#Initialize() Initialize}
	*/

	/**
	BaseObject -> 基础对象
	对象单元属性：设置基础对象
	关联代码：
	{@link simple.compiler.parser.Parser#parseObjectPropertiesSection() parseObjectPropertiesSection}
	*/

	/**
	ImplementsInterface -> 实现接口
	对象单元属性：设置实现接口
	关联代码：
	{@link simple.compiler.parser.Parser#parseObjectPropertiesSection() parseObjectPropertiesSection}
	*/

	/**
	 * 对于语法高亮显示程序，有一个发送注释标记的扫描模式是很有用的。
	 */
	TOK_COMMENT("'");

	// 令牌字符串值
	private final String string;

	/*
	 * 创建令牌；创建一个新令牌。
	 */
	TokenKind(String string) {
		this.string = string;
	}

	@Override
	public String toString() {
		return string;
	}
}

