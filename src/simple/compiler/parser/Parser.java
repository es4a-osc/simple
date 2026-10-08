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

package simple.compiler.parser;

import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.expressions.AdditionExpression;
import simple.compiler.expressions.AliasExpression;
import simple.compiler.expressions.AndExpression;
import simple.compiler.expressions.AssignmentExpression;
import simple.compiler.expressions.CallExpression;
import simple.compiler.expressions.ConcatenationExpression;
import simple.compiler.expressions.ConstantBooleanExpression;
import simple.compiler.expressions.ConstantNumberExpression;
import simple.compiler.expressions.ConstantStringExpression;
import simple.compiler.expressions.DivisionExpression;
import simple.compiler.expressions.EqualExpression;
import simple.compiler.expressions.ExponentiationExpression;
import simple.compiler.expressions.Expression;
import simple.compiler.expressions.GreaterExpression;
import simple.compiler.expressions.GreaterOrEqualExpression;
import simple.compiler.expressions.IdentifierExpression;
import simple.compiler.expressions.IdentityExpression;
import simple.compiler.expressions.IntegerDivisionExpression;
import simple.compiler.expressions.IsExpression;
import simple.compiler.expressions.IsNotExpression;
import simple.compiler.expressions.LessExpression;
import simple.compiler.expressions.LessOrEqualExpression;
import simple.compiler.expressions.LikeExpression;
import simple.compiler.expressions.MeExpression;
import simple.compiler.expressions.ModuloExpression;
import simple.compiler.expressions.MultiplicationExpression;
import simple.compiler.expressions.NegationExpression;
import simple.compiler.expressions.NewExpression;
import simple.compiler.expressions.NotEqualExpression;
import simple.compiler.expressions.NotExpression;
import simple.compiler.expressions.NothingExpression;
import simple.compiler.expressions.OrExpression;
import simple.compiler.expressions.QualifiedIdentifierExpression;
import simple.compiler.expressions.ShiftLeftExpression;
import simple.compiler.expressions.ShiftRightExpression;
import simple.compiler.expressions.SimpleIdentifierExpression;
import simple.compiler.expressions.SubtractionExpression;
import simple.compiler.expressions.TypeOfExpression;
import simple.compiler.expressions.XorExpression;
import simple.compiler.expressions.synthetic.NewComponentExpression;
import simple.compiler.scanner.Scanner;
import simple.compiler.scanner.TokenKind;
import simple.compiler.scopes.Scope;
import simple.compiler.statements.DoUntilStatement;
import simple.compiler.statements.DoWhileStatement;
import simple.compiler.statements.ExitStatement;
import simple.compiler.statements.ExpressionStatement;
import simple.compiler.statements.ForEachStatement;
import simple.compiler.statements.ForNextStatement;
import simple.compiler.statements.IfStatement;
import simple.compiler.statements.OnErrorStatement;
import simple.compiler.statements.RaiseEventStatement;
import simple.compiler.statements.SelectStatement;
import simple.compiler.statements.Statement;
import simple.compiler.statements.StatementBlock;
import simple.compiler.statements.WhileStatement;
import simple.compiler.statements.OnErrorStatement.OnErrorCaseStatement;
import simple.compiler.statements.SelectStatement.SelectCaseStatement;
import simple.compiler.statements.synthetic.LocalVariableDefinitionStatement;
import simple.compiler.statements.synthetic.MarkerStatement;
import simple.compiler.statements.synthetic.RaiseInitializeEventStatement;
import simple.compiler.statements.synthetic.RegisterEventHandlersStatement;
import simple.compiler.symbols.ConstantDataMemberSymbol;
import simple.compiler.symbols.DataMemberSymbol;
import simple.compiler.symbols.EventHandlerSymbol;
import simple.compiler.symbols.EventSymbol;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.InstanceDataMemberSymbol;
import simple.compiler.symbols.InstanceFunctionSymbol;
import simple.compiler.symbols.LocalVariableSymbol;
import simple.compiler.symbols.NamespaceSymbol;
import simple.compiler.symbols.ObjectDataMemberSymbol;
import simple.compiler.symbols.ObjectFunctionSymbol;
import simple.compiler.symbols.ObjectSymbol;
import simple.compiler.symbols.PropertySymbol;
import simple.compiler.types.ArrayType;
import simple.compiler.types.BooleanType;
import simple.compiler.types.ByteType;
import simple.compiler.types.DateType;
import simple.compiler.types.DoubleType;
import simple.compiler.types.IntegerType;
import simple.compiler.types.LongType;
import simple.compiler.types.ObjectType;
import simple.compiler.types.ShortType;
import simple.compiler.types.SingleType;
import simple.compiler.types.StringType;
import simple.compiler.types.Type;
import simple.compiler.types.UnresolvedType;
import simple.compiler.types.VariantType;
import simple.compiler.types.synthetic.ErrorType;
import simple.compiler.util.Signatures;
import simple.util.Preconditions;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 此类解析Simple的源文件并生成语句和表达式树。
 *
 * <p>有关Simple语言及其语法的详细信息，请参阅
 * <a href="http://code.google.com/google-simple">Simple语言定义</a>
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class Parser {

	/*
	 * 每当遇到语法错误时将被抛出。
	 */
	@SuppressWarnings("serial")
	private static final class SyntaxError extends RuntimeException {
	}

	/*
	 * 解析器遇到不可恢复的错误时将被抛出（例如损坏的源文件属性部分）。
	 */
	@SuppressWarnings("serial")
	private static final class FatalError extends RuntimeException {
	}

	// 表达式解析器的优先级
	private static final int PRIO_ASSIGN = 0;
	private static final int PRIO_LOGICALORBITOPERATION_CONJUNCTION = 1;
	private static final int PRIO_LOGICALORBITOPERATION_DISJUNCTION = 2;
	private static final int PRIO_LOGICALORBITOPERATION_NEGATION = 3;
	private static final int PRIO_COMPARISON = 4;
	private static final int PRIO_SHIFT = 5;
	private static final int PRIO_CONCATENATION = 6;
	private static final int PRIO_ADDITION = 7;
	private static final int PRIO_MODULO = 8;
	private static final int PRIO_INTEGERDIVISION = 9;
	private static final int PRIO_MULTIPLICATION = 10;
	private static final int PRIO_NEGATION = 11;
	private static final int PRIO_EXPONENTIATION = 12;
	private static final int PRIO_DOT = 13;

	// 当前编译的编译器实例
	private final Compiler compiler;

	// 正在分析的源文件的扫描仪
	private final Scanner scanner;

	// 正在分析的源的命名空间符号
	private final NamespaceSymbol currentNamespace;

	// 正在分析的源文件的对象符号
	private final ObjectSymbol currentObjectSymbol;

	// 当前正在解析的函数符号（过程、属性获取器/设置器、事件处理程序）
	private FunctionSymbol currentFunction;

	// 当前活动作用域
	private Scope currentScope;

	// 当前语句列表（仅在解析函数、过程、属性和事件处理程序时设置）
	private StatementBlock currentStatementList;

	// 在当前源文件中没有特定位置的错误的特殊源位置
	private final long fileOnlySourcePosition;

	// 指示分析器当前是否正在处理源文件的属性部分
	private boolean parsingPropertiesSection;

	// 组件列表（仅由窗口源文件定义）
	private List<DataMemberSymbol> components;

	/**
	 * 创建新的解析器。
	 *
	 * @param compiler  当前编译的编译器实例
	 * @param scanner  要分析的源文件的扫描仪
	 * @param qualifiedClassName  源文件的限定类名
	 */
	public Parser(Compiler compiler, Scanner scanner, String qualifiedClassName) {
		this.compiler = compiler;
		this.scanner = scanner;

		fileOnlySourcePosition = scanner.getSourceFileOnlyPosition();

		// Simple中没有默认包
		// simple.samples.test.Test -> simple.samples.test
		String packageName = Signatures.getPackageName(qualifiedClassName);
		if (packageName.equals("")) {
			// 没取到单元的包名，报错！
			compiler.error(fileOnlySourcePosition, Error.errNoPackage);
			currentNamespace = compiler.getGlobalNamespaceSymbol();
		} else {
			// 修复将包名转为内部包名 xhwsd@qq.com 2021-7-1
			currentNamespace = NamespaceSymbol.getNamespaceSymbol(compiler, packageName.replace('.', '/'));
		}

		// 分配由源文件定义的对象
		currentObjectSymbol = new ObjectSymbol(fileOnlySourcePosition,
				Signatures.getClassName(qualifiedClassName), currentNamespace, null);
		currentObjectSymbol.markAsCompiled();

		Scope scope = currentNamespace.getScope();
		String className = currentObjectSymbol.getName();
		if (scope.lookupShallow(className) != null) {
			compiler.error(fileOnlySourcePosition, Error.errSymbolRedefinition, className);
		} else {
			scope.enterSymbol(currentObjectSymbol);
		}

		compiler.addObject(currentObjectSymbol);
		currentScope = currentObjectSymbol.getScope();

		components = new ArrayList<DataMemberSymbol>();
	}

	/*
	 * 遇到语法错误后重新同步：因为Simple是一种基于行的语言，所以我们只需跳过任何标记，直到找到语句结尾或文件结尾。
	 */
	private void resyncAfterSyntaxError() {
		for (;;) {
			TokenKind token = scanner.nextToken();
			if (token == TokenKind.TOK_EOS || token == TokenKind.TOK_EOF) {
				skipEndOfStatements();
				break;
			}
		}
	}

	/*
	 * 接受给定的令牌，否则报告错误。
	 */
	private void accept(TokenKind token) {
		if (scanner.getToken() != token) {
			if (!parsingPropertiesSection) {
				compiler.error(scanner.getTokenStartPosition(), Error.errExpected, token.toString(),
						scanner.getToken().toString());
			}
			throw new SyntaxError();
		}
	}

	/*
	 * 接受给定的令牌并跳过它。
	 */
	private TokenKind acceptAndSkip(TokenKind token) {
		accept(token);
		return scanner.nextToken();
	}

	/*
	 * 接受当前语句的结尾（行末或':'）并跳过它或报告错误。
	 */
	private void acceptAndSkipEndOfStatement() {
		TokenKind token = scanner.getToken();
		if (token != TokenKind.TOK_EOS && token != TokenKind.TOK_EOF) {
			if (!parsingPropertiesSection) {
				compiler.error(scanner.getTokenStartPosition(), Error.errExpected, "结束语句",
						scanner.getToken().toString());
			}
			throw new SyntaxError();
		}

		skipEndOfStatements();
	}

	/*
	 * 尽可能多地跳过可以找到的连续结束语。
	 */
	private void skipEndOfStatements() {
		while (scanner.nextToken() == TokenKind.TOK_EOS) {
			// 跳过语句标记的任何其他结尾
		}
	}

	/*
	 * 解析可能的限定对象类型。
	 */
	private ObjectType parseObjectType() {
		accept(TokenKind.TOK_IDENTIFIER);
		return new UnresolvedType(parseQualifiedIdentifier());
	}

	/*
	 * 解析类型名称（没有数组指定符）。
	 */
	private Type parseNonArrayType() {
		switch (scanner.getToken()) {
			default:
				compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected,
						scanner.getToken().toString());
				throw new SyntaxError();

			case TOK_IDENTIFIER:
				return new UnresolvedType(parseQualifiedIdentifier());

			case TOK_BOOLEAN:
				scanner.nextToken(); // 跳过 '逻辑型'
				return BooleanType.booleanType;

			case TOK_BYTE:
				scanner.nextToken(); // 跳过 '字节型'
				return ByteType.byteType;

			case TOK_SHORT:
				scanner.nextToken(); // 跳过 '短整数型'
				return ShortType.shortType;

			case TOK_INTEGER:
				scanner.nextToken(); // 跳过 '整数型'
				return IntegerType.integerType;

			case TOK_LONG:
				scanner.nextToken(); // 跳过 '长整数型'
				return LongType.longType;

			case TOK_SINGLE:
				scanner.nextToken(); // 跳过 '单精度小数型'
				return SingleType.singleType;

			case TOK_DOUBLE:
				scanner.nextToken(); // 跳过 '双精度小数型'
				return DoubleType.doubleType;

			case TOK_OBJECT:
				scanner.nextToken(); // 跳过 '对象'
				return ObjectType.objectType;

			case TOK_STRING:
				scanner.nextToken(); // 跳过 '文本型'
				return StringType.stringType;

			case TOK_DATE:
				scanner.nextToken(); // 跳过 '日期时间型'
				return DateType.dateType;

			case TOK_VARIANT:
				scanner.nextToken(); // 跳过 '变体型'
				return VariantType.variantType;
		}
	}

	/*
	 * 解析类型名称。
	 */
	private Type parseType() {
		Type type = parseNonArrayType();

		List<Expression> dimensions = null;
		if (scanner.getToken() == TokenKind.TOK_OPENPARENTHESIS) {
			// 数组类型
			long position = scanner.getTokenStartPosition();
			dimensions = parseArrayDimensions();
			type = new ArrayType(type, dimensions.size());

			// 数组类型必须是动态大小
			if (dimensions.get(0) != null) {
				compiler.error(position, Error.errDimensionInDynamicArray);
			}
		}

		return type;
	}

	/*
	 * 解析函数调用的实际参数列表。
	 */
	private List<Expression> parseActualArgumentList() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_OPENPARENTHESIS);

		List<Expression> argList = new ArrayList<Expression>();
		if (scanner.nextToken() == TokenKind.TOK_CLOSEPARENTHESIS) {
			// 空参数列表
			scanner.nextToken();
		} else {
			for (;;) {
				argList.add(parseExpression());

				if (scanner.getToken() == TokenKind.TOK_CLOSEPARENTHESIS) {
					scanner.nextToken();  // 跳过 ')'
					break;
				}

				acceptAndSkip(TokenKind.TOK_COMMA);
			}
		}
		return argList;
	}

	/*
	 * 解析数组标注表达式。
	 * 它应该由逗号分隔的表达式列表或逗号列表（可能没有逗号）组成，但不能混合使用。
	 */
	private List<Expression> parseArrayDimensions() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_OPENPARENTHESIS);

		List<Expression> dimensions = new ArrayList<Expression>();
		switch (scanner.nextToken()) {
			default:
				// 一个或多个指定维度
				for (;;) {
					dimensions.add(parseExpression());
					if (scanner.getToken() == TokenKind.TOK_CLOSEPARENTHESIS) {
						scanner.nextToken();  // 跳过 ')'
						break;
					}
					acceptAndSkip(TokenKind.TOK_COMMA);
				}
				break;

			case TOK_COMMA:
				// 多个未指定的维度
				do {
					dimensions.add(null);
				} while (scanner.nextToken() == TokenKind.TOK_COMMA);
				dimensions.add(null);
				acceptAndSkip(TokenKind.TOK_CLOSEPARENTHESIS);
				break;

			case TOK_CLOSEPARENTHESIS:
				// 单个未指定的维度
				dimensions.add(null);
				scanner.nextToken();  // 跳过 ')'
				break;
		}

		return dimensions;
	}

	/*
	 * 解析限定标识符。
	 */
	private IdentifierExpression parseQualifiedIdentifier() {
		// 检验当前令牌是否是标识符
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_IDENTIFIER);

		// 始终从单个标识符开始
		IdentifierExpression expr = new SimpleIdentifierExpression(scanner.getTokenStartPosition(),
				currentScope, scanner.getTokenValueIdentifier());

		// 如果后跟“.”，则为限定标识符
		if (scanner.nextToken() == TokenKind.TOK_DOT) {
			do {
				scanner.nextToken(); // 跳过 '.'
				accept(TokenKind.TOK_IDENTIFIER);
				expr = new QualifiedIdentifierExpression(scanner.getTokenStartPosition(), expr,
						scanner.getTokenValueIdentifier());
			} while (scanner.nextToken() == TokenKind.TOK_DOT);
		}

		return expr;
	}

	/*
	 * 解析 '创建' 表达式。
	 */
	private Expression parseNewExpression() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_NEW);

		long exprStartPosition = scanner.getTokenStartPosition();
		scanner.nextToken();  // 跳过 '创建'

		Type type = parseNonArrayType();

		List<Expression> dimensions = null;
		switch (scanner.getToken()) {
			default:
				return new NewExpression(exprStartPosition, type, null);

			case TOK_OPENPARENTHESIS:
				// 数组类型
				dimensions = parseArrayDimensions();
				return new NewExpression(exprStartPosition, new ArrayType(type, dimensions.size()),
						dimensions);

			case TOK_ON:
				// 容器上的组件
				scanner.nextToken();  // 跳过 '位于'
				return new NewComponentExpression(parseExpression(), type);
		}
	}

	/*
	 * 跳过第一个标识运算符，并在下面出现任何其他标识运算符时报告错误。
	 */
	private void skipIdentityOperator() {
		TokenKind tokenKind = scanner.nextToken();  // 跳过 '-' 或 '+'

		while (tokenKind == TokenKind.TOK_MINUS || tokenKind == TokenKind.TOK_PLUS) {
			compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected, tokenKind.toString());
			tokenKind = scanner.nextToken();
		}
	}

	/*
	 * 根据部分表达式的优先级解析它们。
	 * 如果解析器遇到优先级高于当前运算符的运算符，则它将完成对子表达式的解析。
	 */
	private Expression parseExpression(int priority) {
		Expression expr;

		// 首先解析表达式的左侧
		TokenKind tokenKind = scanner.getToken();
		long exprStartPosition = scanner.getTokenStartPosition();

		switch (tokenKind) {
			default:
				compiler.error(exprStartPosition, Error.errUnexpected, tokenKind.toString());
				throw new SyntaxError();

			case TOK_BOOLEANCONSTANT: // 逻辑 常数
				expr = new ConstantBooleanExpression(exprStartPosition, scanner.getTokenValueBoolean());
				scanner.nextToken();
				break;

			case TOK_IDENTIFIER: // 标识符
				// 解析限定标识符
				expr = parseQualifiedIdentifier();
				// 子表达式
				if (scanner.getToken() == TokenKind.TOK_OPENPARENTHESIS) {
					expr = new CallExpression(scanner.getTokenStartPosition(), expr,
							parseActualArgumentList());
				}
				break;

			case TOK_ME: // 本对象
				expr = new MeExpression(exprStartPosition, currentObjectSymbol);
				scanner.nextToken();
				// TODO: what about Me.foo.bar?
				break;

			case TOK_MINUS: // 减
				skipIdentityOperator();
				expr = new NegationExpression(exprStartPosition, parseExpression(PRIO_NEGATION));
				break;

			case TOK_NEW: // 创建
				expr = parseNewExpression();
				break;

			case TOK_NOT: // 取反
				scanner.nextToken(); // 跳过 '取反'
				expr = new NotExpression(exprStartPosition,
						parseExpression(PRIO_LOGICALORBITOPERATION_NEGATION));
				break;

			case TOK_NOTHING: // 空
				scanner.nextToken(); // 跳过 '空'
				expr =  new NothingExpression(exprStartPosition);
				break;

			case TOK_NUMERICCONSTANT: // 数值 常数
				expr = new ConstantNumberExpression(exprStartPosition, scanner.getTokenValueNumber());
				scanner.nextToken();
				break;

			case TOK_OPENPARENTHESIS: // 左括号
				scanner.nextToken(); // 跳过 '('
				expr = parseExpression();
				acceptAndSkip(TokenKind.TOK_CLOSEPARENTHESIS);
				break;

			case TOK_PLUS: // 加
				skipIdentityOperator();
				expr = new IdentityExpression(exprStartPosition, parseExpression(PRIO_NEGATION));
				break;

			case TOK_STRINGCONSTANT: // 文本 常数
				expr = new ConstantStringExpression(exprStartPosition, scanner.getTokenValueString());
				scanner.nextToken();
				break;

			case TOK_TYPEOF: // 类型检验
				scanner.nextToken(); // 跳过 '类型检验'
				expr = parseExpression(PRIO_COMPARISON);
				exprStartPosition = scanner.getTokenStartPosition();
				acceptAndSkip(TokenKind.TOK_IS);
				expr = new TypeOfExpression(exprStartPosition, expr, parseType());
				break;
		}

		// 然后重复解析右侧的子表达式，只要它们的优先级比当前表达式的优先级低（或者相同，具体取决于操作符）。
		for (;;) {
			exprStartPosition = scanner.getTokenStartPosition();
			switch (scanner.getToken()) {
				default:
					return expr;

				case TOK_AND:
					if (priority >= PRIO_LOGICALORBITOPERATION_CONJUNCTION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '且'
					expr = new AndExpression(exprStartPosition, expr,
							parseExpression(PRIO_LOGICALORBITOPERATION_CONJUNCTION));
					break;

				case TOK_AMPERSAND:
					if (priority >= PRIO_CONCATENATION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '&'
					expr = new ConcatenationExpression(exprStartPosition, expr,
							parseExpression(PRIO_CONCATENATION));
					break;

				case TOK_DIVIDE:
					if (priority >= PRIO_MULTIPLICATION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '/'
					expr = new DivisionExpression(exprStartPosition, expr,
							parseExpression(PRIO_MULTIPLICATION));
					break;

				case TOK_DOT:
					scanner.nextToken(); // 跳过 '.'
					accept(TokenKind.TOK_IDENTIFIER);
					expr = new QualifiedIdentifierExpression(scanner.getTokenStartPosition(), expr,
							scanner.getTokenValueIdentifier());
					scanner.nextToken();
					break;

				case TOK_EQUAL:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '='
					expr = new EqualExpression(exprStartPosition, expr, parseExpression(PRIO_COMPARISON));
					break;

				case TOK_EXP:
					if (priority > PRIO_EXPONENTIATION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '^'
					expr = new ExponentiationExpression(exprStartPosition, expr,
							parseExpression(PRIO_EXPONENTIATION));
					break;

				case TOK_INTEGERDIVIDE:
					if (priority >= PRIO_INTEGERDIVISION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '\'
					expr = new IntegerDivisionExpression(exprStartPosition, expr,
							parseExpression(PRIO_INTEGERDIVISION));
					break;

				case TOK_IS:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '是'
					expr = new IsExpression(exprStartPosition, expr, parseExpression(PRIO_COMPARISON));
					break;

				case TOK_ISNOT:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '非'
					expr = new IsNotExpression(exprStartPosition, expr, parseExpression(PRIO_COMPARISON));
					break;

				case TOK_GREATER:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '>'
					expr = new GreaterExpression(exprStartPosition, expr, parseExpression(PRIO_COMPARISON));
					break;

				case TOK_GREATEREQUAL:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '>='
					expr = new GreaterOrEqualExpression(exprStartPosition, expr,
							parseExpression(PRIO_COMPARISON));
					break;

				case TOK_LESS:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '<'
					expr = new LessExpression(exprStartPosition, expr, parseExpression(PRIO_COMPARISON));
					break;

				case TOK_LESSEQUAL:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '<='
					expr = new LessOrEqualExpression(exprStartPosition, expr,
							parseExpression(PRIO_COMPARISON));
					break;

				case TOK_LIKE:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '匹配'
					expr = new LikeExpression(exprStartPosition, expr, parseExpression(PRIO_COMPARISON));
					break;

				case TOK_MINUS:
					if (priority >= PRIO_ADDITION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '-'
					expr = new SubtractionExpression(exprStartPosition, expr, parseExpression(PRIO_ADDITION));
					break;

				case TOK_MOD:
					if (priority >= PRIO_MODULO) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '%'
					expr = new ModuloExpression(exprStartPosition, expr, parseExpression(PRIO_MODULO));
					break;

				case TOK_NOTEQUAL:
					if (priority >= PRIO_COMPARISON) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '<>'
					expr = new NotEqualExpression(exprStartPosition, expr, parseExpression(PRIO_COMPARISON));
					break;

				case TOK_OR:
					if (priority >= PRIO_LOGICALORBITOPERATION_DISJUNCTION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '或'
					expr = new OrExpression(exprStartPosition, expr,
							parseExpression(PRIO_LOGICALORBITOPERATION_DISJUNCTION));
					break;

				case TOK_PLUS:
					if (priority >= PRIO_ADDITION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '+'
					expr = new AdditionExpression(exprStartPosition, expr, parseExpression(PRIO_ADDITION));
					break;

				case TOK_SHIFTLEFT:
					if (priority >= PRIO_SHIFT) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '<<'
					expr = new ShiftLeftExpression(exprStartPosition, expr, parseExpression(PRIO_SHIFT));
					break;

				case TOK_SHIFTRIGHT:
					if (priority >= PRIO_SHIFT) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '>>'
					expr = new ShiftRightExpression(exprStartPosition, expr, parseExpression(PRIO_SHIFT));
					break;

				case TOK_TIMES:
					if (priority >= PRIO_MULTIPLICATION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '*'
					expr = new MultiplicationExpression(exprStartPosition, expr,
							parseExpression(PRIO_MULTIPLICATION));
					break;

				case TOK_XOR:
					if (priority >= PRIO_LOGICALORBITOPERATION_DISJUNCTION) {
						return expr;
					}

					scanner.nextToken(); // 跳过 '异或'
					expr = new XorExpression(exprStartPosition, expr,
							parseExpression(PRIO_LOGICALORBITOPERATION_DISJUNCTION));
					break;
			}
		}
	}

	/**
	 * 解析（顶层）表达式。
	 */
	private Expression parseExpression() {
		return parseExpression(PRIO_ASSIGN);
	}

	/**
	 * 解析 执行-循环 语句。注意有两种格式：执行-判断循环 和 执行-直到。
	 */
	private Statement parseDoLoopStatement() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_DO);

		scanner.nextToken();  // 跳过 '执行'

		acceptAndSkipEndOfStatement();

		// 解析循环体
		StatementBlock savedStatementList = currentStatementList;
		StatementBlock loopStatements = new StatementBlock(currentScope);
		try {
			currentStatementList = loopStatements;
			currentScope = loopStatements.getScope();
			parseStatementList(TokenKind.TOK_DO);
		} finally {
			currentStatementList = savedStatementList;
			currentScope = currentStatementList.getScope();
		}

		// 解析循环条件
		TokenKind loopToken = scanner.getToken();
		long stmtStartPosition = scanner.getTokenStartPosition();
		scanner.nextToken();
		Expression condition = parseExpression();
		acceptAndSkipEndOfStatement();

		switch (loopToken) {
			default:
				// Should not happen! It must be 'While' or 'Until' because
				// otherwise parseStatementList(TokenKind.TOK_DO) wouldn't have returned
				// here - otherwise it would have thrown an exception.
				assert false;
				return null;

			case TOK_UNTIL:
				return new DoUntilStatement(stmtStartPosition, condition, loopStatements);

			case TOK_WHILE:
				return new DoWhileStatement(stmtStartPosition, condition, loopStatements);
		}
	}

	/*
	 * Parses an Exit statement. The acceptEndOfStatement parameter must be set to false for
	 * sub-statements of the single line version of the If-Else-Statement.
	 */
	private Statement parseExitStatement(boolean acceptEndOfStatement) {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_EXIT);

		long stmtStartPosition = scanner.getTokenStartPosition();
		TokenKind token = scanner.nextToken();
		switch (token) {
			default:
				token = TokenKind.TOK_NONE;
				break;

			case TOK_DO:
			case TOK_EVENT:
			case TOK_FOR:
			case TOK_FUNCTION:
			case TOK_PROPERTY:
			case TOK_SUB:
			case TOK_WHILE:
				// These tokens are allowed to follow an Exit statement
				scanner.nextToken();
				break;
		}

		if (acceptEndOfStatement) {
			acceptAndSkipEndOfStatement();
		}

		return new ExitStatement(stmtStartPosition, token);
	}

	/*
	 * Parses an expression statement. The acceptEndOfStatement parameter must be set to false for
	 * sub-statements of the single line version of the If-Else-Statement.
	 */
	private Statement parseExpressionStatement(boolean acceptEndOfStatement) {
		// Parsing an assignment expression is kind of tricky because of the overloaded use of '=' for
		// assignment as well as comparison. We solve this by just parsing a right-hand-side
		// expression (PRIO_DOT) and if it is followed by '=' we assume it is an assignment expression.
		long stmtStartPosition = scanner.getTokenStartPosition();
		Expression expr = parseExpression(PRIO_DOT);
		if (scanner.getToken() == TokenKind.TOK_EQUAL) {
			// This is an assignment expression
			long exprStartPosition = scanner.getTokenStartPosition();
			scanner.nextToken(); // Skip '='
			expr = new AssignmentExpression(exprStartPosition, expr, parseExpression());
		}

		if (acceptEndOfStatement) {
			acceptAndSkipEndOfStatement();
		}

		return new ExpressionStatement(stmtStartPosition, expr);
	}

	/*
	 * Parses a For-statement. Note that there are two variations: For-Next-statements and For-Each-
	 * statements.
	 */
	private Statement parseForStatement() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_FOR);

		long stmtStartPosition = scanner.getTokenStartPosition();
		Statement stmt;

		if (scanner.nextToken() == TokenKind.TOK_EACH) {
			// Parse For...Each statement
			scanner.nextToken();  // Skip 'Each'

			accept(TokenKind.TOK_IDENTIFIER);
			String loopVarName = scanner.getTokenValueIdentifier();
			Expression loopVarExpr = new SimpleIdentifierExpression(scanner.getTokenStartPosition(),
					currentScope, loopVarName);

			// Parse initialization expression
			scanner.nextToken();  // Skip identifier
			acceptAndSkip(TokenKind.TOK_IN);
			Expression initExpr = parseExpression();
			acceptAndSkipEndOfStatement();

			// Parse the loop body
			StatementBlock savedStatementList = currentStatementList;
			StatementBlock loopStatements = new StatementBlock(currentScope);
			try {
				currentStatementList = loopStatements;
				currentScope = loopStatements.getScope();
				parseStatementList(TokenKind.TOK_FOR);
			} finally {
				currentStatementList = savedStatementList;
				currentScope = currentStatementList.getScope();
			}

			// If there is the optional identifier after the Next keyword then it must match the name of
			// the loop variable.
			if (scanner.getToken() == TokenKind.TOK_IDENTIFIER) {
				if (!scanner.getTokenValueIdentifier().equals(loopVarName)) {
					compiler.error(scanner.getTokenStartPosition(), Error.errForNextIdentifierMismatch,
							scanner.getTokenValueIdentifier(), loopVarName);
				}
				scanner.nextToken();  // Skip identifier
			}

			// Return the For statement.
			stmt = new ForEachStatement(stmtStartPosition, loopVarExpr, initExpr, loopStatements);
		} else {
			// Parse For...Next statement
			accept(TokenKind.TOK_IDENTIFIER);
			String loopVarName = scanner.getTokenValueIdentifier();
			Expression loopVarExpr = new SimpleIdentifierExpression(scanner.getTokenStartPosition(),
					currentScope, loopVarName);

			// Parse initialization expression
			scanner.nextToken();  // Skip identifier
			acceptAndSkip(TokenKind.TOK_EQUAL);
			Expression initExpr = parseExpression();

			// Parse end condition
			acceptAndSkip(TokenKind.TOK_TO);
			Expression endExpr = parseExpression();

			// Parse optional step expression
			Expression stepExpr;
			if (scanner.getToken() == TokenKind.TOK_STEP) {
				scanner.nextToken();  // Skip 'Step'
				stepExpr = parseExpression();
			} else {
				stepExpr = new ConstantNumberExpression(0, BigDecimal.ONE);
			}

			acceptAndSkipEndOfStatement();

			// Parse the loop body
			StatementBlock savedStatementList = currentStatementList;
			StatementBlock loopStatements = new StatementBlock(currentScope);
			try {
				currentStatementList = loopStatements;
				currentScope = loopStatements.getScope();
				parseStatementList(TokenKind.TOK_FOR);
			} finally {
				currentStatementList = savedStatementList;
				currentScope = currentStatementList.getScope();
			}

			// If there is the optional identifier after the Next keyword then it must match the name of
			// the loop variable.
			if (scanner.getToken() == TokenKind.TOK_IDENTIFIER) {
				if (!scanner.getTokenValueIdentifier().equals(loopVarName)) {
					compiler.error(scanner.getTokenStartPosition(), Error.errForNextIdentifierMismatch,
							scanner.getTokenValueIdentifier(), loopVarName);
				}
				scanner.nextToken();  // Skip identifier
			}

			// Return the For statement.
			stmt = new ForNextStatement(stmtStartPosition, loopVarExpr, initExpr, endExpr, stepExpr,
					loopStatements);
		}

		acceptAndSkipEndOfStatement();

		return stmt;
	}

	/*
	 * Parses an If-statement. There are plenty of different variations, but the most tricky ones
	 * are the single-line versions. They will require us to allow statements without the customary
	 * end-of-statement delimiters. Also the statements allowed for the then- and else-parts are
	 * limited to expression and exit statements.
	 */
	private Statement parseIfStatement() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_IF ||
				scanner.getToken() == TokenKind.TOK_ELSEIF);

		long stmtStartPosition = scanner.getTokenStartPosition();

		scanner.nextToken();
		Expression condition = parseExpression();

		acceptAndSkip(TokenKind.TOK_THEN);

		StatementBlock thenStatements = new StatementBlock(currentScope);
		StatementBlock elseStatements = null;

		TokenKind token = scanner.getToken();
		if (token != TokenKind.TOK_EOS && token != TokenKind.TOK_EOF) {
			// Single line form of If statement
			thenStatements.add(parseSameLineStatement());

			if (scanner.getToken() == TokenKind.TOK_ELSE) {
				scanner.nextToken();  // Skip 'Else'
				elseStatements = new StatementBlock(currentScope);
				elseStatements.add(parseSameLineStatement());
			}

			acceptAndSkipEndOfStatement();
		} else {
			// Complex form of If statement
			acceptAndSkipEndOfStatement();

			StatementBlock savedStatementList = currentStatementList;

			try {
				// Parse statements in Then block
				currentStatementList = thenStatements;
				currentScope = thenStatements.getScope();
				parseStatementList(TokenKind.TOK_IF);

				if (scanner.getToken() == TokenKind.TOK_ELSEIF) {
					// Parse ElseIf block statements
					currentStatementList =
					elseStatements = new StatementBlock(currentScope);
					currentScope = elseStatements.getScope();
					elseStatements.add(parseIfStatement());
				} else if (scanner.getToken() == TokenKind.TOK_ELSE) {
					// Parse Else block statements
					scanner.nextToken();
					acceptAndSkipEndOfStatement();

					currentStatementList =
					elseStatements = new StatementBlock(currentScope);
					currentScope = elseStatements.getScope();
					parseStatementList(TokenKind.TOK_IF);
				}
			} finally {
				currentStatementList = savedStatementList;
				currentScope = currentStatementList.getScope();
			}
		}

		return new IfStatement(stmtStartPosition, condition, thenStatements, elseStatements);
	}

	/*
	 * Parses an On Error statement. Note that there can be only one On Error statement per
	 * function, procedure, event handler or property getter/setter.
	 */
	private void parseOnErrorStatement() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_ON);

		long stmtStartPosition = scanner.getTokenStartPosition();

		scanner.nextToken();
		acceptAndSkip(TokenKind.TOK_ERROR);
		acceptAndSkipEndOfStatement();

		StatementBlock savedStatementList = currentStatementList;

		OnErrorStatement onErrorStmt = new OnErrorStatement(stmtStartPosition);

		try {
			for (;;) {
				TokenKind tokenKind = scanner.getToken();
				switch (tokenKind) {
					default:
						compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected,
								tokenKind.toString());
						throw new SyntaxError();

					case TOK_END:
						scanner.nextToken(); // Skip 'End'
						acceptAndSkip(TokenKind.TOK_ERROR);
						acceptAndSkipEndOfStatement();
						break;

					case TOK_CASE:
						StatementBlock statements = new StatementBlock(currentScope);
						OnErrorCaseStatement caseStatement = onErrorStmt.newCaseStatement(
								scanner.getTokenStartPosition(), statements);

						if (scanner.nextToken() == TokenKind.TOK_ELSE) {
							scanner.nextToken();  // Skip 'Else'
						} else {
							for (;;) {
								caseStatement.addTypeExpression(parseExpression());

								if (scanner.getToken() != TokenKind.TOK_COMMA) {
									break;
								}

								scanner.nextToken(); // Skip ','
							}
						}

						acceptAndSkipEndOfStatement();

						currentStatementList = statements;
						currentScope = statements.getScope();
						parseStatementList(TokenKind.TOK_SELECT);
						continue;
				}
				break;
			}
		} finally {
			currentStatementList = savedStatementList;
			currentScope = currentStatementList.getScope();
		}

		if (!currentFunction.setOnErrorStatement(onErrorStmt)) {
			compiler.error(onErrorStmt.getPosition(), Error.errMultipleOnErrorStatements);
		}
	}

	/*
	 * Parses a RaiseEvent statement. The acceptEndOfStatement parameter must be set to false for
	 * sub-statements of the single line version of the If-Else-Statement.
	 */
	private Statement parseRaiseEventStatement(boolean acceptEndOfStatement) {
		long stmtStartPosition = scanner.getTokenStartPosition();

		scanner.nextToken();  // Skip 'RaiseEvent'

		accept(TokenKind.TOK_IDENTIFIER);
		String eventName = scanner.getTokenValueIdentifier();
		scanner.nextToken();  // Skip '<identifier>'

		accept(TokenKind.TOK_OPENPARENTHESIS);
		List<Expression> actualArgList = parseActualArgumentList();

		if (acceptEndOfStatement) {
			acceptAndSkipEndOfStatement();
		}

		return new RaiseEventStatement(stmtStartPosition, eventName, actualArgList);
	}

	/*
	 * Parses a Select statement.
	 */
	private Statement parseSelectStatement() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_SELECT);

		long stmtStartPosition = scanner.getTokenStartPosition();

		scanner.nextToken();
		Expression selector = parseExpression();

		acceptAndSkipEndOfStatement();

		StatementBlock savedStatementList = currentStatementList;

		SelectStatement selectStmt = new SelectStatement(stmtStartPosition, selector);

		try {
			for (;;) {
				TokenKind tokenKind = scanner.getToken();
				switch (tokenKind) {
					default:
						compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected,
								tokenKind.toString());
						throw new SyntaxError();

					case TOK_END:
						scanner.nextToken(); // Skip 'End'
						acceptAndSkip(TokenKind.TOK_SELECT);
						acceptAndSkipEndOfStatement();
						break;

					case TOK_CASE:
						StatementBlock statements = new StatementBlock(currentScope);
						SelectCaseStatement caseStatement = selectStmt.newCaseStatement(
								scanner.getTokenStartPosition(), statements);

						if (scanner.nextToken() == TokenKind.TOK_ELSE) {
							scanner.nextToken();  // Skip 'Else'
						} else {
							for (;;) {
								switch (scanner.getToken()) {
									default:
										Expression expression = parseExpression();
										if (scanner.getToken() == TokenKind.TOK_TO) {
											scanner.nextToken();  // Skip 'To'
											caseStatement.addRangeExpression(expression, parseExpression());
										} else {
											caseStatement.addEqualExpression(expression);
										}
										break;

									case TOK_IS:
										tokenKind = scanner.nextToken();
										scanner.nextToken();  // Skip comparison token
										switch (tokenKind) {
											default:
												compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected,
														tokenKind.toString());
												throw new SyntaxError();

											case TOK_EQUAL:
												caseStatement.addEqualExpression(parseExpression());
												break;

											case TOK_NOTEQUAL:
												caseStatement.addNotEqualExpression(parseExpression());
												break;

											case TOK_LESS:
												caseStatement.addLessExpression(parseExpression());
												break;

											case TOK_LESSEQUAL:
												caseStatement.addLessOrEqualExpression(parseExpression());
												break;

											case TOK_GREATER:
												caseStatement.addGreaterExpression(parseExpression());
												break;

											case TOK_GREATEREQUAL:
												caseStatement.addGreaterOrEqualExpression(parseExpression());
												break;
										}
										break;
								}

								if (scanner.getToken() != TokenKind.TOK_COMMA) {
									break;
								}

								scanner.nextToken();  // Skip ','
							}
						}

						acceptAndSkipEndOfStatement();

						currentStatementList = statements;
						currentScope = statements.getScope();
						parseStatementList(TokenKind.TOK_SELECT);
						continue;
				}
				break;
			}
		} finally {
			currentStatementList = savedStatementList;
			currentScope = currentStatementList.getScope();
		}

		return selectStmt;
	}

	/*
	 * Parses a While statement.
	 */
	private Statement parseWhileStatement() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_WHILE);

		long stmtStartPosition = scanner.getTokenStartPosition();

		scanner.nextToken();
		Expression condition = parseExpression();

		acceptAndSkipEndOfStatement();

		StatementBlock savedStatementList = currentStatementList;
		StatementBlock loopStatements = new StatementBlock(currentScope);
		try {
			currentStatementList = loopStatements;
			currentScope = loopStatements.getScope();
			parseStatementList(TokenKind.TOK_WHILE);
		} finally {
			currentStatementList = savedStatementList;
			currentScope = currentStatementList.getScope();
		}

		return new WhileStatement(stmtStartPosition, condition, loopStatements);
	}

	/*
	 * 解析局部变量声明。
	 */
	private void parseLocalVariableDeclaration() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_DIM);

		scanner.nextToken();  // 跳过 '变量'

		for (;;) {
			// 解析标识符和类型声明。
			long symStartPosition = scanner.getTokenStartPosition();
			accept(TokenKind.TOK_IDENTIFIER);
			String name = scanner.getTokenValueIdentifier();
			scanner.nextToken();  // 跳过 '<标识符>'
			acceptAndSkip(TokenKind.TOK_AS);
			Type type = parseNonArrayType();

			List<Expression> dimensions = null;
			if (scanner.getToken() == TokenKind.TOK_OPENPARENTHESIS) {
				// 数组类型
				dimensions = parseArrayDimensions();
				type = new ArrayType(type, dimensions.size());
			}

			// 声明局部变量。
			LocalVariableSymbol local =
					currentFunction.addLocalVariable(symStartPosition, name, type, currentScope);
			if (local == null) {
				compiler.error(symStartPosition, Error.errSymbolRedefinition, name);
			} else {
				MarkerStatement marker = new LocalVariableDefinitionStatement(local);
				currentStatementList.add(marker);
				local.setBeginScope(marker);
				local.setStaticArrayDimensions(dimensions);
			}

			// 除非有逗号，否则我们就完成了。
			if (scanner.getToken() != TokenKind.TOK_COMMA) {
				break;
			}

			scanner.nextToken(); // 跳过 ','
		}

		acceptAndSkipEndOfStatement();
	}

	/*
	 * Parses a block (or list) of statements.
	 */
	private void parseStatementList(TokenKind tokenAfterEnd) {
		for (;;) {
			// Keep parsing until we hit a token that could indicate an end to the statement list
			switch (scanner.getToken()) {
				default:
					break;

				case TOK_END:
					if (tokenAfterEnd != TokenKind.TOK_SELECT && tokenAfterEnd != TokenKind.TOK_ERROR) {
						scanner.nextToken(); // Skip 'End'
						acceptAndSkip(tokenAfterEnd);
						acceptAndSkipEndOfStatement();
					}
					return;

				case TOK_NEXT:
					if (tokenAfterEnd == TokenKind.TOK_FOR) {
						scanner.nextToken(); // Skip 'Next'
						return;
					}
					break;

				case TOK_UNTIL:
				case TOK_WHILE:
					if (tokenAfterEnd == TokenKind.TOK_DO) {
						return;
					}
					break;

				case TOK_CASE:
				case TOK_EOF:
					return;
			}

			try {
				Statement stmt = parseStatement();
				if (stmt == null) {
					// Possibly encountered an 'Else' or 'ElseIf'
					TokenKind tokenKind = scanner.getToken();
					switch (tokenKind) {
						default:
							break;

						case TOK_ELSE:
						case TOK_ELSEIF:
							if (tokenAfterEnd == TokenKind.TOK_IF) {
								return;
							}
							compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected,
									tokenKind.toString());
							throw new SyntaxError();
					}
				} else {
					currentStatementList.add(stmt);
				}
			} catch (SyntaxError se) {
				resyncAfterSyntaxError();
			}
		}
	}

	/*
	 * Parses a statement on the same line as the enclosing statement. The only case where this is
	 * actually happening is the single line If-statement. This method does not accept
	 * end-of-statement tokens at the end of the statement.
	 */
	private Statement parseSameLineStatement() {
		TokenKind tokenKind = scanner.getToken();
		switch (tokenKind) {
			default:
				compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected, tokenKind.toString());
				throw new SyntaxError();

			case TOK_IDENTIFIER:
			case TOK_ME:
				return parseExpressionStatement(false);

			case TOK_EXIT:
				return parseExitStatement(false);

			case TOK_RAISEEVENT:
				return parseRaiseEventStatement(false);
		}
	}

	/*
	 * Parses a single statement (will also absorb local variable declarations). Note that this
	 * method will return null only if it encounters an 'Else' or 'ElseIf' token or if wants to
	 * defer error reporting to its caller.
	 */
	private Statement parseStatement() {
		TokenKind tokenKind = scanner.getToken();
		switch (tokenKind) {
			default:
				compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected, tokenKind.toString());
				throw new SyntaxError();

			case TOK_DIM:
				parseLocalVariableDeclaration();
				return null;

			case TOK_DO:
				return parseDoLoopStatement();

			case TOK_ELSE:
			case TOK_ELSEIF:
				return null;

			case TOK_EXIT:
				return parseExitStatement(true);

			case TOK_FOR:
				return parseForStatement();

			case TOK_IDENTIFIER:
			case TOK_ME:
				return parseExpressionStatement(true);

			case TOK_IF:
				return parseIfStatement();

			case TOK_ON:
				parseOnErrorStatement();
				return null;

			case TOK_RAISEEVENT:
				return parseRaiseEventStatement(true);

			case TOK_SELECT:
				return parseSelectStatement();

			case TOK_WHILE:
				return parseWhileStatement();
		}
	}

	/*
	 * Parses an alias declaration (only allowed at file-level).
	 */
	private void parseAliasDeclaration() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_ALIAS);

		scanner.nextToken();  // Skip 'Alias'
		accept(TokenKind.TOK_IDENTIFIER);
		String alias = scanner.getTokenValueIdentifier();
		scanner.nextToken();  // Skip '<identifier>'

		long exprStartPosition = scanner.getTokenStartPosition();
		acceptAndSkip(TokenKind.TOK_EQUAL);

		currentObjectSymbol.addAlias(new AliasExpression(exprStartPosition, alias,
				parseQualifiedIdentifier()));

		acceptAndSkipEndOfStatement();
	}

	/*
	 * 解析常量声明。
	 */
	private void parseConstantDeclaration() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_CONST);

		// xhwsd@qq.com 2021-5-27 修复无法连续定义常量（常量 标识符 为 类型 = 常量值, 标识符 为 类型 =  常量值）
		scanner.nextToken();  // 跳过 '常量'

		for (;;) {
			// 解析常量声明
			long symStartPosition = scanner.getTokenStartPosition();
			// 检验当前令牌是否是标识符
			accept(TokenKind.TOK_IDENTIFIER);
			// 取常量名
			String name = scanner.getTokenValueIdentifier();
			// 跳过常量名
			scanner.nextToken();  // 跳过 '<标识符>'
			// 接受并跳过 '为'
			acceptAndSkip(TokenKind.TOK_AS);
			// 取常量类型
			Type type = parseNonArrayType();
			// 接受并跳过 '='
			acceptAndSkip(TokenKind.TOK_EQUAL);
			// 表达式
			Expression constExpr = parseExpression();
			// 向当前类添加新常量字段成员
			ConstantDataMemberSymbol constDataMember = new ConstantDataMemberSymbol(symStartPosition,
					currentObjectSymbol, name, type, constExpr);
			if (!currentObjectSymbol.addDataMember(constDataMember)) {
				compiler.error(symStartPosition, Error.errSymbolRedefinition, name);
			}

			//  除非有逗号，否则我们就完成了。
			if (scanner.getToken() != TokenKind.TOK_COMMA) {
				break;
			}

			scanner.nextToken(); // 跳过 ','
		}

		acceptAndSkipEndOfStatement();
	}

	/*
	 * Parses a field (data member) declaration for either a static (object) or an instance field.
	 */
	private void parseFieldDeclaration(boolean isStatic) {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_DIM);

		scanner.nextToken();  // Skip 'Dim'

		for (;;) {
			// Parse field member declaration
			long symStartPosition = scanner.getTokenStartPosition();
			accept(TokenKind.TOK_IDENTIFIER);
			String name = scanner.getTokenValueIdentifier();
			scanner.nextToken();  // Skip '<identifier>'
			acceptAndSkip(TokenKind.TOK_AS);
			Type type = parseNonArrayType();

			List<Expression> dimensions = null;
			if (scanner.getToken() == TokenKind.TOK_OPENPARENTHESIS) {
				// Array type
				dimensions = parseArrayDimensions();
				type = new ArrayType(type, dimensions.size());
			}

			// Add a new field member to the current class
			DataMemberSymbol field = isStatic ?
					new ObjectDataMemberSymbol(symStartPosition, currentObjectSymbol, name, type) :
					new InstanceDataMemberSymbol(symStartPosition, currentObjectSymbol, name, type);
			if (!currentObjectSymbol.addDataMember(field)) {
				compiler.error(symStartPosition, Error.errSymbolRedefinition, name);
			}
			field.setStaticArrayDimensions(dimensions);

			// Unless there is a comma, we are done.
			if (scanner.getToken() != TokenKind.TOK_COMMA) {
				break;
			}

			scanner.nextToken(); // Skip ','
		}

		acceptAndSkipEndOfStatement();
	}

	/*
	 * Parses the formal argument list of a function, procedure, property or event handler.
	 */
	private void parseFormalArgumentList(FunctionSymbol functionSymbol) {
		for (;;) {
			boolean isReferenceParameter = false;

			switch (scanner.getToken()) {
				default:
					break;

				case TOK_BYVAL:
					// Nothing to do - this is the default
					scanner.nextToken(); // Skip 'ByVal'
					break;

				case TOK_BYREF:
					isReferenceParameter = true;
					scanner.nextToken(); // Skip 'ByRef'
					break;
			}

			accept(TokenKind.TOK_IDENTIFIER);
			long symStartPosition = scanner.getTokenStartPosition();
			String name = scanner.getTokenValueIdentifier();

			scanner.nextToken();
			acceptAndSkip(TokenKind.TOK_AS);

			if (functionSymbol.addParameter(symStartPosition, name, parseType(),
					isReferenceParameter) == null) {
				compiler.error(symStartPosition, Error.errSymbolRedefinition, name);
			}

			if (scanner.getToken() == TokenKind.TOK_CLOSEPARENTHESIS) {
				scanner.nextToken(); // Skip ')'
				break;
			}

			acceptAndSkip(TokenKind.TOK_COMMA);
		}
	}

	/*
	 * Parses the body of a function, procedure, property or event handler.
	 */
	private void parseFunctionBody(FunctionSymbol function, TokenKind afterEndToken) {
		currentStatementList = function.getFunctionStatements();
		currentFunction = function;

		Scope outerScope = currentScope;
		currentScope = currentStatementList.getScope();

		try {
			parseStatementList(afterEndToken);
		} finally {
			currentScope = outerScope;
			currentStatementList = null;
			currentFunction = null;
		}
	}

	/*
	 * Parses an event handler declaration.
	 */
	private void parseEventHandlerDeclaration() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_EVENT);

		// There are two situations to handle here:
		//
		// 1) An event declaration:
		//
		//    Event <identifier>(<formalArgList>)
		//    End Event
		//
		//    When we get to code generation time, this will simply be an empty virtual method
		//    with an @SimpleEvent attribute
		//
		// 2) An event handler definition:
		//
		//    Event <identifier>.<identifier>(<formalArgList>)
		//      <statements>
		//    End Events
		//
		//    The first identifier can either be a type name (in which case the type name must match
		//    the name of the current object) or a data member name. The second identifier must be the
		//    name of an event.

		FunctionSymbol eventSymbol;
		boolean isEvent = false;
		try {
			scanner.nextToken();  // Skip 'Event'
			accept(TokenKind.TOK_IDENTIFIER);
			long firstIdentifierStartPosition = scanner.getTokenStartPosition();
			String firstIdentifier = scanner.getTokenValueIdentifier();

			if (scanner.nextToken() == TokenKind.TOK_DOT) { // Skip <identifier>
				// Parse an event handler definition
				scanner.nextToken();  // Skip '.'

				accept(TokenKind.TOK_IDENTIFIER);
				String secondIdentifier = scanner.getTokenValueIdentifier();
				scanner.nextToken();  // Skip <identifier>

				// Define EventHandler
				EventHandlerSymbol eventHandler = new EventHandlerSymbol(firstIdentifierStartPosition,
						currentObjectSymbol, firstIdentifier, secondIdentifier);
				if (!currentObjectSymbol.addEventHandler(eventHandler)) {
					compiler.error(firstIdentifierStartPosition, Error.errSymbolRedefinition,
							firstIdentifier + '.' + secondIdentifier);
				}

				eventSymbol = eventHandler;

			} else {
				// Parse an event declaration
				EventSymbol event = new EventSymbol(firstIdentifierStartPosition, currentObjectSymbol,
						firstIdentifier);
				if (!currentObjectSymbol.addEvent(event)) {
					compiler.error(firstIdentifierStartPosition, Error.errSymbolRedefinition,
							firstIdentifier);
				}

				event.setIsCompiled();
				isEvent = true;

				eventSymbol = event;
			}

			// Both event handler and event share the following code: parse formal arguments
			acceptAndSkip(TokenKind.TOK_OPENPARENTHESIS);
			if (scanner.getToken() == TokenKind.TOK_CLOSEPARENTHESIS) {
				// Empty formal argument list
				scanner.nextToken(); // Skip ')'
			} else {
				parseFormalArgumentList(eventSymbol);
			}
			acceptAndSkipEndOfStatement();

		} catch (SyntaxError se) {
			resyncAfterSyntaxError();
			// Allocate a dummy event handler so that we can continue parsing
			eventSymbol = new EventHandlerSymbol(Scanner.NO_POSITION, currentObjectSymbol, "", "");
		}

		if (isEvent) {
			// Events must not have a body
			acceptAndSkip(TokenKind.TOK_END);
			acceptAndSkip(TokenKind.TOK_EVENT);
			acceptAndSkipEndOfStatement();

		} else {
			parseFunctionBody(eventSymbol, TokenKind.TOK_EVENT);
		}
	}

	/*
	 * Parses a function declaration (for either a static (object) or instance function).
	 */
	private void parseFunctionDeclaration(boolean isStatic) {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_FUNCTION);

		FunctionSymbol function = null;
		try {
			// Parse name part of function declaration
			scanner.nextToken();  // Skip 'Function'
			accept(TokenKind.TOK_IDENTIFIER);
			long symStartPosition = scanner.getTokenStartPosition();
			String functionName = scanner.getTokenValueIdentifier();

			// 先创建函数，完整解析参数列表后再登记；重载身份由参数个数决定。
			function = isStatic ?
					new ObjectFunctionSymbol(symStartPosition, currentObjectSymbol, functionName) :
					new InstanceFunctionSymbol(symStartPosition, currentObjectSymbol, functionName);
			function.setIsCompiled();

			scanner.nextToken();  // Skip <identifier>

			// Parse formal arguments
			acceptAndSkip(TokenKind.TOK_OPENPARENTHESIS);
			if (scanner.getToken() == TokenKind.TOK_CLOSEPARENTHESIS) {
				// Empty formal argument list
				scanner.nextToken(); // Skip ')'
			} else {
				parseFormalArgumentList(function);
			}

			acceptAndSkip(TokenKind.TOK_AS);
			function.setResultType(parseType());

			if (!currentObjectSymbol.addFunction(function)) {
				compiler.error(symStartPosition, Error.errSymbolRedefinition, functionName);
			}

			acceptAndSkipEndOfStatement();

		} catch (SyntaxError se) {
			resyncAfterSyntaxError();
			// Allocate a dummy function symbol so that we can continue parsing
			if (function ==  null) {
				function = isStatic ?
						new ObjectFunctionSymbol(Scanner.NO_POSITION, currentObjectSymbol, "") :
						new InstanceFunctionSymbol(Scanner.NO_POSITION, currentObjectSymbol, "");
				function.setIsCompiled();
				currentObjectSymbol.addFunction(function);
			}
			function.setResultType(ErrorType.errorType);
		}

		// Parse function body (non-interface source files only)
		if (currentObjectSymbol.isInterface()) {
			function.setIsAbstract();
		} else {
			parseFunctionBody(function, TokenKind.TOK_FUNCTION);
		}
	}

	/*
	 * Parses a property declaration.
	 */
	private void parsePropertyDeclaration() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_PROPERTY);

		String propertyName;
		Type propertyType;
		long symStartPosition;
		try {
			// Parse name and type of property
			scanner.nextToken();  // Skip 'Property'
			accept(TokenKind.TOK_IDENTIFIER);
			symStartPosition = scanner.getTokenStartPosition();
			propertyName = scanner.getTokenValueIdentifier();
			scanner.nextToken();  // Skip <identifier>

			acceptAndSkip(TokenKind.TOK_AS);

			propertyType = parseType();

			acceptAndSkipEndOfStatement();
		} catch (SyntaxError se) {
			resyncAfterSyntaxError();
			propertyName = "";
			propertyType = VariantType.variantType;
			symStartPosition = Scanner.NO_POSITION;
		}

		FunctionSymbol getter = null;
		FunctionSymbol setter = null;

		// Parse getter and setter definitions
		if (scanner.getToken() == TokenKind.TOK_GET) {
			getter = new InstanceFunctionSymbol(scanner.getTokenStartPosition(), currentObjectSymbol,
					propertyName);
			getter.setIsCompiled();
			getter.setResultType(propertyType);
			getter.setIsProperty();

			scanner.nextToken();  // Skip 'Get'
			acceptAndSkipEndOfStatement();

			parseFunctionBody(getter, TokenKind.TOK_GET);
		}

		if (scanner.getToken() == TokenKind.TOK_SET) {
			// Using the property name for its setter argument is not a problem because properties
			// are not supposed to invoked recursively and don't have a result.
			setter = new InstanceFunctionSymbol(scanner.getTokenStartPosition(), currentObjectSymbol,
					propertyName);
			setter.setIsCompiled();
			setter.addParameter(symStartPosition, propertyName, propertyType, false);
			setter.setIsProperty();

			scanner.nextToken();  // Skip 'Set'
			acceptAndSkipEndOfStatement();

			parseFunctionBody(setter, TokenKind.TOK_SET);
		}

		acceptAndSkip(TokenKind.TOK_END);
		acceptAndSkip(TokenKind.TOK_PROPERTY);

		if (getter == null && setter == null) {
			compiler.error(symStartPosition, Error.errPropertyWithoutGetterOrSetter, propertyName);
		}

		// Define property symbol
		PropertySymbol property = new PropertySymbol(symStartPosition, currentObjectSymbol,
				propertyName, propertyType);
		if (!currentObjectSymbol.addProperty(property, getter, setter)) {
			compiler.error(symStartPosition, Error.errSymbolRedefinition, propertyName);
		}

		acceptAndSkipEndOfStatement();
	}

	/*
	 * Parses a procedure declaration (for either a static (object) or instance procedure).
	 */
	private void parseProcedureDeclaration(boolean isStatic) {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_SUB);

		FunctionSymbol procedure;
		try {
			// Parse name part of procedure declaration
			scanner.nextToken();  // Skip 'Sub'
			accept(TokenKind.TOK_IDENTIFIER);
			long symStartPosition = scanner.getTokenStartPosition();
			String procedureName = scanner.getTokenValueIdentifier();
			scanner.nextToken();  // Skip <identifier>

			// 先创建过程，完整解析参数列表后再登记；同名声明由参数个数区分。
			procedure = isStatic ?
					new ObjectFunctionSymbol(symStartPosition, currentObjectSymbol, procedureName) :
					new InstanceFunctionSymbol(symStartPosition, currentObjectSymbol, procedureName);
			procedure.setIsCompiled();

			// Parse formal arguments
			acceptAndSkip(TokenKind.TOK_OPENPARENTHESIS);
			if (scanner.getToken() == TokenKind.TOK_CLOSEPARENTHESIS) {
				// Empty formal argument list
				scanner.nextToken(); // Skip ')'
			} else {
				parseFormalArgumentList(procedure);
			}
			if (!currentObjectSymbol.addFunction(procedure)) {
				compiler.error(symStartPosition, Error.errSymbolRedefinition, procedureName);
			}
			acceptAndSkipEndOfStatement();
		} catch (SyntaxError se) {
			resyncAfterSyntaxError();
			// Allocate a procedure symbol so that we can continue parsing
			procedure = isStatic ?
					new ObjectFunctionSymbol(Scanner.NO_POSITION, currentObjectSymbol, "") :
					new InstanceFunctionSymbol(Scanner.NO_POSITION, currentObjectSymbol, "");
			procedure.setIsCompiled();
		}

		// Parse function body (non-interface source files only)
		if (currentObjectSymbol.isInterface()) {
			procedure.setIsAbstract();
		} else {
			parseFunctionBody(procedure, TokenKind.TOK_SUB);
		}
	}

	/*
	 * Parses a declaration starting with 'Static'.
	 */
	private void parseStaticDeclaration() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_STATIC);

		TokenKind tokenKind = scanner.nextToken();
		switch (tokenKind) {
			default:
				compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected, tokenKind.toString());
				throw new SyntaxError();

			case TOK_DIM:
				parseFieldDeclaration(true);
				break;

			case TOK_FUNCTION:
				parseFunctionDeclaration(true);
				break;

			case TOK_SUB:
				parseProcedureDeclaration(true);
				break;
		}
	}

	/*
	 * Parses the body of a $Define-statement which contains properties and nested $Define-
	 * statements.
	 */
	private void parse$DefineStatementBody(Expression componentExpr) {
		Statement stmt;
		for (;;) {
			TokenKind tokenKind = scanner.getToken();
			switch (tokenKind) {
				default:
					compiler.error(fileOnlySourcePosition, Error.errCorruptedSourceFileProperties);
					throw new FatalError();

				case TOK_$DEFINE:
					// $Define statements can be nested for nested components.
					parse$DefineStatement(componentExpr);
					continue;

				case TOK_IDENTIFIER:
					Expression propertyExpr = new QualifiedIdentifierExpression(
							scanner.getTokenStartPosition(), componentExpr, scanner.getTokenValueIdentifier());
					// If followed by a '.' it is a compound property
					while (scanner.nextToken() == TokenKind.TOK_DOT) {    // Skip <identifier>
						scanner.nextToken(); // Skip '.'
						accept(TokenKind.TOK_IDENTIFIER);
						propertyExpr = new QualifiedIdentifierExpression(scanner.getTokenStartPosition(),
								propertyExpr, scanner.getTokenValueIdentifier());
					}

					long exprStartPosition = scanner.getTokenStartPosition();
					acceptAndSkip(TokenKind.TOK_EQUAL);
					stmt = new ExpressionStatement(fileOnlySourcePosition,
							new AssignmentExpression(exprStartPosition, propertyExpr, parseExpression()));
					break;

				case TOK_EOS:
					scanner.nextToken(); // Skip <endofstatement>
					continue;

				case TOK_$END:
					scanner.nextToken(); // Skip '$End'
					acceptAndSkip(TokenKind.TOK_$DEFINE);
					acceptAndSkipEndOfStatement();
					return;
			}

			currentStatementList.add(stmt);
		}
	}

	/*
	 * Parses a $Define-statement in the properties section of a Simple form source file. The
	 * topLevel parameter is true if the component is part of the form container or false if it
	 * is nested within another component container.
	 */
	private void parse$DefineStatement(Expression container) {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_$DEFINE);

		scanner.nextToken();
		accept(TokenKind.TOK_IDENTIFIER);
		String componentName = scanner.getTokenValueIdentifier();

		scanner.nextToken();  // Skip component name
		acceptAndSkip(TokenKind.TOK_$AS);

		accept(TokenKind.TOK_IDENTIFIER);
		ObjectType componentType = parseObjectType();

		// Create field for component
		Expression componentExpr = new SimpleIdentifierExpression(fileOnlySourcePosition, currentScope,
				componentName);

		Expression newExpr;
		DataMemberSymbol component;
		if (container == null) {
			component = new ObjectDataMemberSymbol(fileOnlySourcePosition, currentObjectSymbol,
					componentName, componentType);
			newExpr = new MeExpression(fileOnlySourcePosition, currentObjectSymbol);
		} else {
			component = new InstanceDataMemberSymbol(fileOnlySourcePosition, currentObjectSymbol,
					componentName, componentType);
			newExpr = new NewComponentExpression(container, componentType);
		}

		components.add(component);
		if (!currentObjectSymbol.addDataMember(component)) {
			compiler.error(fileOnlySourcePosition, Error.errCorruptedSourceFileProperties);
			throw new FatalError();
		}

		Expression expr = new AssignmentExpression(fileOnlySourcePosition, componentExpr, newExpr);
		Statement stmt = new ExpressionStatement(fileOnlySourcePosition, expr);
		currentStatementList.add(stmt);

		acceptAndSkipEndOfStatement();

		parse$DefineStatementBody(componentExpr);
	}

	/*
	 * Parses a $Define-statement for defining a component instance in a Simple form source file.
	 */
	private void parseTopLevel$DefineStatement(FunctionSymbol define) {
		currentStatementList = define.getFunctionStatements();
		currentFunction = define;

		Scope outerScope = currentScope;
		currentScope = currentStatementList.getScope();

		try {
			parse$DefineStatement(null);

			// Register event handlers
			currentStatementList.add(new RegisterEventHandlersStatement(currentObjectSymbol));

			// Invoke initializer events for any components
			for (DataMemberSymbol component : components) {
				currentStatementList.add(new RaiseInitializeEventStatement(component));
			}
		} finally {
			currentScope = outerScope;
			currentStatementList = null;
			currentFunction = null;
		}
	}

	/*
	 * Parses the properties section of a Simple form source file.
	 */
	private void parseFormPropertiesSection() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_$FORM);

		scanner.nextToken();  // Skip '$Form'
		acceptAndSkipEndOfStatement();

		// Now we know the correct superclass of the current class
		currentObjectSymbol.setBaseObject((ObjectType) ObjectSymbol.getObjectSymbol(compiler,
				Compiler.RUNTIME_ROOT_INTERNAL + "/components/窗口").getType());

		// Always need to create a $define() method which will instantiate any components on the form
		InstanceFunctionSymbol function = new InstanceFunctionSymbol(Scanner.NO_POSITION,
				currentObjectSymbol, "$define");
		function.setIsCompiled();
		function.setIsGenerated();
		currentObjectSymbol.addFunction(function);
		parseTopLevel$DefineStatement(function);
	}

	/*
	 * 解析一个Simple对象源文件的属性部分。
	 */
	private void parseObjectPropertiesSection() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_$OBJECT);

		scanner.nextToken();  // 提供 '$对象'
		acceptAndSkipEndOfStatement();

		// 解析属性，可以是 基础对象 或 实现接口
		for (;;) {
			if (scanner.getToken() != TokenKind.TOK_IDENTIFIER) {
				// 不是标识符。让我们假设我们已经完成了
				break;
			}

			String identifier = scanner.getTokenValueIdentifier();
			scanner.nextToken();  // 跳过 <标识符>

			if (identifier.equals("基础对象")) {
				// 格式：基础对象 = 完全限定类名
				acceptAndSkip(TokenKind.TOK_EQUAL);
				// 基础对象必须是完全限定的名称，将在全局命名空间范围内查找（使用当前类范围导致各种问题）
				Scope savedCurrentScope = currentScope;
				currentScope = compiler.getGlobalNamespaceSymbol().getScope();
				try {
					ObjectType baseObject = parseObjectType();
					currentObjectSymbol.setBaseObject(baseObject);
				} finally {
					currentScope = savedCurrentScope;
				}
			} else if (identifier.equals("实现接口")) {
				// 格式：实现接口 = 完全限定类名 [, 完全限定类名]
				acceptAndSkip(TokenKind.TOK_EQUAL);
				for (;;) {
					currentObjectSymbol.addInterface(parseObjectType());
					if (scanner.getToken() != TokenKind.TOK_COMMA) {
						break;
					}
					scanner.nextToken();  // 跳过 ','
				}
			} else {
				// 这将是一个错误。让我们返回并让调用者报告一个错误。
				break;
			}

			acceptAndSkipEndOfStatement();
		}
	}

	/*
	 * 解析Simple接口源文件的属性部分。
	 */
	private void parseInterfacePropertiesSection() {
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_$INTERFACE);

		scanner.nextToken();  // 跳过 '$接口'
		acceptAndSkipEndOfStatement();

		currentObjectSymbol.markAsInterface();

		// 无属性
	}

	/**
	 * 解析服务属性部分
	 */
	private void parseServicePropertiesSection() {
		/*
		关联代码：
		{@link simple.compiler.scanner.TokenKind#TOK_$SERVICE TOK_$SERVICE}
		{@link simple.compiler.scanner.Keywords#Keywords() Keywords}
		{@link simple.compiler.parser.Parser#parse() parse}
		{@link simple.runtime.collections.服务 服务}
		*/
		Preconditions.checkState(scanner.getToken() == TokenKind.TOK_$SERVICE); // 检验当前标记是否为 '服务'
		scanner.nextToken(); // 跳过 '$服务' 到下个标记
		acceptAndSkipEndOfStatement(); // 跳过 换行

		// 现在我们知道了当前类的正确超类
		// 设置单元对象继承类为 {@link simple.runtime.collections.服务}
		currentObjectSymbol.setBaseObject((ObjectType) ObjectSymbol.getObjectSymbol(compiler,
				Compiler.RUNTIME_ROOT_INTERNAL + "/collections/服务").getType());
	}

	/**
	 * 解析源文件。
	 */
	public void parse() {
		// Every Simple source file contains with a properties section. This is normally not visible to
		// the user in the source file (this obviously requires support from the editor - if you use vi,
		// emacs or any other low-level editor, you will be able to see this section as well).
		try {
			parsingPropertiesSection = true;
			scanner.scanPropertiesSection();
			skipEndOfStatements();

			// Design-time section is enclosed by $Properties ... $End $Properties
			try {
				acceptAndSkip(TokenKind.TOK_$PROPERTIES);
				acceptAndSkipEndOfStatement();

				// First statement in the properties section must be the $Source statement which determines
				// the rest of the content in the section
				acceptAndSkip(TokenKind.TOK_$SOURCE);
				switch (scanner.getToken()) {
					default:
						compiler.error(fileOnlySourcePosition, Error.errCorruptedSourceFileProperties);
						throw new FatalError();

					case TOK_$FORM:
						parseFormPropertiesSection();
						break;

					case TOK_$OBJECT:
						parseObjectPropertiesSection();
						break;

					case TOK_$INTERFACE:
						parseInterfacePropertiesSection();
						break;

					case TOK_$SERVICE:
						parseServicePropertiesSection();
						break;
				}

				// End of properties section
				acceptAndSkip(TokenKind.TOK_$END);
				acceptAndSkip(TokenKind.TOK_$PROPERTIES);
				skipEndOfStatements();
				accept(TokenKind.TOK_EOF);
			} catch (SyntaxError se) {
				compiler.error(fileOnlySourcePosition, Error.errCorruptedSourceFileProperties);
				throw new FatalError();
			}

			// Now start parsing the language section of the Simple source file.
			parsingPropertiesSection = false;
			scanner.scanSourceSection();
			skipEndOfStatements();

			// Interfaces only allow a subset of top-level keywords
			if (currentObjectSymbol.isInterface()) {
				// Parse an interface source file
				for (;;) {
					try {
						TokenKind tokenKind = scanner.getToken();
						switch (tokenKind) {
							default:
								compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected,
										tokenKind.toString());
								throw new SyntaxError();

							case TOK_ALIAS:
								parseAliasDeclaration();
								break;

							case TOK_CONST:
								parseConstantDeclaration();
								break;

							case TOK_EOF:
								return;

							case TOK_FUNCTION:
								parseFunctionDeclaration(false);
								acceptAndSkip(TokenKind.TOK_END);
								acceptAndSkip(TokenKind.TOK_FUNCTION);
								acceptAndSkipEndOfStatement();
								break;

							case TOK_SUB:
								parseProcedureDeclaration(false);
								acceptAndSkip(TokenKind.TOK_END);
								acceptAndSkip(TokenKind.TOK_SUB);
								acceptAndSkipEndOfStatement();
								break;
						}
					} catch (SyntaxError se) {
						resyncAfterSyntaxError();
					}
				}
			} else {
				// Parse a regular object or form source file
				for (;;) {
					try {
						TokenKind tokenKind = scanner.getToken();
						switch (tokenKind) {
							default:
								compiler.error(scanner.getTokenStartPosition(), Error.errUnexpected,
										tokenKind.toString());
								throw new SyntaxError();

							case TOK_ALIAS:
								parseAliasDeclaration();
								break;

							case TOK_CONST:
								parseConstantDeclaration();
								break;

							case TOK_DIM:
								parseFieldDeclaration(false);
								break;

							case TOK_EOF:
								return;

							case TOK_EVENT:
								parseEventHandlerDeclaration();
								break;

							case TOK_FUNCTION:
								parseFunctionDeclaration(false);
								break;

							case TOK_PROPERTY:
								parsePropertyDeclaration();
								break;

							case TOK_STATIC:
								parseStaticDeclaration();
								break;

							case TOK_SUB:
								parseProcedureDeclaration(false);
								break;
						}
					} catch (SyntaxError se) {
						resyncAfterSyntaxError();
					}
				}
			}
		} catch (FatalError e) {
			// Error was already reported. Just abort parsing.
		}
	}
}
