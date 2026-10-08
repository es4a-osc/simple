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

package simple.compiler;

/**
 * 此类提供错误消息，供编译器报告。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public final class Error extends Message {

	/**
	 * 错误消息模板。
	 */
	public static final String errArrayDimensionExpected;
	public static final String errArrayOrCollectionNeededInForEach;
	public static final String errAssignmentOrCallExprExpected;
	public static final String errCannotConvertType;
	public static final String errCannotInvokeEvent;
	public static final String errCaseElseNotLast;
	public static final String errConstantPoolOverflow;
	public static final String errConstantValueExpected;
	public static final String errCorruptedSourceFileProperties;
	public static final String errDataMemberExpected;
	public static final String errDimensionInDynamicArray;
	public static final String errExpected;
	public static final String errForNextIdentifierMismatch;
	public static final String errFunctionOrArrayTypeNeeded;
	public static final String errFunctionTooBig;
	public static final String errIdentifierDoesNotResolveToSymbol;
	public static final String errIdentifierNotFound;
	public static final String errIllegalCharacter;
	public static final String errInstanceMemberWithoutMe;
	public static final String errMalformedDecimalConstant;
	public static final String errMalformedFloatConstant;
	public static final String errMalformedHexConstant;
	public static final String errMalformedUnicodeEscapeSequence;
	public static final String errMultipleOnErrorStatements;
	public static final String errNoComponentImplementation;
	public static final String errNoMatchingFunctionOverload;
	public static final String errNoMatchForExit;
	public static final String errNoPackage;
	public static final String errObjectOrArrayTypeNeeded;
	public static final String errObjectTypeNeeded;
	public static final String errOperandNotAssignable;
	public static final String errPropertyWithoutGetterOrSetter;
	public static final String errRaiseEventFromObjectFunction;
	public static final String errReadError;
	public static final String errRuntimeErrorTypeNeeded;
	public static final String errScalarIntegerTypeNeededForOperand;
	public static final String errScalarTypeNeededForOperand;
	public static final String errScalarTypeNeededInForNext;
	public static final String errScalarTypeNeededInSelectCase;
	public static final String errSymbolRedefinition;
	public static final String errTooFewArguments;
	public static final String errTooFewIndices;
	public static final String errTooManyArguments;
	public static final String errTooManyIndices;
	public static final String errUndefinedSymbol;
	public static final String errUnexpected;
	public static final String errUnterminatedString;
	public static final String errValueExpected;
	public static final String errWriteError;

	static {
		errArrayDimensionExpected = Message.localize("errArrayDimensionExpected",
				"需要数组维度");
		errArrayOrCollectionNeededInForEach = Message.localize("errArrayOrCollectionNeededInForEach",
				"在 循环-每个 语句中迭代需要数组或集合");
		errAssignmentOrCallExprExpected = Message.localize("errAssignmentOrCallExprExpected",
				"需要赋值或调用表达式");
		errCannotConvertType = Message.localize("errCannotConvertType",
				"无法将 %1 类型转换为 %2 类型");
		errCannotInvokeEvent = Message.localize("errCannotInvokeEvent",
				"无法直接调用事件 - 请使用 触发事件 语句");
		errCaseElseNotLast = Message.localize("errCaseElseNotLast",
				"分支-否则 语句应该为最后一个分支");
		errConstantPoolOverflow = Message.localize("errConstantPoolOverflow",
				"为 %1 生成类文件时出现常量池溢出");
		errConstantValueExpected = Message.localize("errConstantValueExpected",
				"需要常量值");
		errCorruptedSourceFileProperties = Message.localize("errCorruptedSourceFileProperties",
				"源文件的属性部分已损坏");
		errDataMemberExpected = Message.localize("errDataMemberExpected",
				"%1 不是数据成员，但应该是");
		errDimensionInDynamicArray = Message.localize("errDimensionInDynamicArray",
				"动态数组不可定义维度");
		errExpected = Message.localize("errExpected",
				"需要 '%1' 但找到 '%2'");
		errForNextIdentifierMismatch = Message.localize("errForNextIdentifierMismatch",
				"下个 语句中的标识符 %1 与 循环 语句中的标识符 %2 不匹配");
		errFunctionOrArrayTypeNeeded = Message.localize("errFunctionOrArrayTypeNeeded",
				"需要函数或数组类型");
		errFunctionTooBig = Message.localize("errFunctionTooBig",
				"类 %1 中的函数 %2 过大");
		errIdentifierDoesNotResolveToSymbol = Message.localize("errIdentifierDoesNotResolveToSymbol",
				"限定标识符 %1 无法解析为符号");
		errIdentifierNotFound = Message.localize("errIdentifierNotFound",
				"未找到标识符 %1");
		errIllegalCharacter = Message.localize("errIllegalCharacter",
				"非法源字符");
		errInstanceMemberWithoutMe = Message.localize("errInstanceMemberWithoutMe",
				"在非实例成员中使用实例成员 '%1'");
		errMalformedDecimalConstant = Message.localize("errMalformedecimalConstant",
				"格式错误的十进制常数");
		errMalformedFloatConstant = Message.localize("errMalformedFloatConstant",
				"格式错误的双精度小数常数");
		errMalformedHexConstant = Message.localize("errMalformedHexConstant",
				"格式错误的十六进制常数");
		errMalformedUnicodeEscapeSequence = Message.localize("errMalformedUnicodeEscapeSequence",
				"格式错误的unicode转义序列");
		errMultipleOnErrorStatements = Message.localize("errMultipleOnErrorStatements",
				"重复定义 位于-错误 语句");  // TODO: 报告其他语句的位置
		errNoComponentImplementation = Message.localize("errNoComponentImplementation",
				"未找到 %1 的组件实现");
		errNoMatchingFunctionOverload = Message.localize("errNoMatchingFunctionOverload",
				"找不到函数或过程 %1 的 %2 参数重载");
		errNoMatchForExit = Message.localize("errNoMatchForExit",
				"未找到 退出 %1 的匹配项");
		errNoPackage = Message.localize("errNoPackage",
				"Simple的源文件需要成为包的一部分");
		errObjectTypeNeeded = Message.localize("errObjectTypeNeeded",
				"需要对象类型，但找到 %1");
		errObjectOrArrayTypeNeeded = Message.localize("errObjectOrArrayTypeNeeded",
				"需要对象或数组类型");
		errOperandNotAssignable = Message.localize("errOperandNotAssignable",
				"左操作数需要可赋值");
		errPropertyWithoutGetterOrSetter = Message.localize("errPropertyWithoutGetterOrSetter",
				"属性 %1 需要定义属性获取器或设置器或两者都定义");
		errRaiseEventFromObjectFunction = Message.localize("errRaiseEventFromObjectFunction",
				"触发事件 语句只能在实例函数或过程中使用");
		errReadError = Message.localize("errReadError",
				"读取文件 %1 时发生 I/O 错误");
		errRuntimeErrorTypeNeeded = Message.localize("errRuntimeErrorTypeNeeded",
				"需要运行错误类型");
		errScalarIntegerTypeNeededForOperand = Message.localize("errScalarIntegerTypeNeededForOperand",
				"操作数需要逻辑或数值类型（字节型、短整数型、整数型或长整数型）");
		errScalarTypeNeededForOperand = Message.localize("errScalarTypeNeededForOperand",
				"操作数需要标量类型");
		errScalarTypeNeededInForNext = Message.localize("errScalarTypeNeededInForNext",
				"循环-下个 语句中循环变量需要标量类型");
		errScalarTypeNeededInSelectCase = Message.localize("errScalarTypeNeededInSelectCase",
				"分支 语句中需要标量类型");
		errSymbolRedefinition = Message.localize("errSymbolRedefinition",
				"重复定义 %1");  // TODO: 同时报告原始定义的位置
		errTooFewArguments = Message.localize("errTooFewArguments",
				"调用参数过少");
		errTooFewIndices = Message.localize("errTooFewIndices",
				"调用索引过少");
		errTooManyArguments = Message.localize("errTooManyArguments",
				"调用参数过多");
		errTooManyIndices = Message.localize("errTooManyIndices",
				"调用索引过多");
		errUndefinedSymbol = Message.localize("errUndefinedSymbol",
				"无法解析标识符 %1");
		errUnexpected = Message.localize("errUnexpected",
				"发现多余的 '%1'");
		errUnterminatedString = Message.localize("errUnterminatedString",
				"没有闭合的文本常数");
		errValueExpected = Message.localize("errValueExpected",
				"需要值");
		errWriteError = Message.localize("errWriteError",
				"写入文件 %1 时发生 I/O 错误");
	}

	/**
	 * 错误；创建一个新的错误消息。
	 *
	 * @param position  源位置
	 * @param message  错误消息模板
	 * @param params  错误消息的参数
	 */
	public Error(long position, String message, String... params) {
		super(position, message, params);
	}

	@Override
	protected String messageKind() {
		return "错误";
	}
}
