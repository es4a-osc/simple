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

package simple.compiler.statements;

import simple.classfiles.Method;
import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.expressions.Expression;
import simple.compiler.scanner.TokenKind;
import simple.compiler.symbols.FunctionSymbol;
import simple.compiler.symbols.LocalVariableSymbol;
import simple.compiler.types.ArrayType;
import simple.compiler.types.IntegerType;
import simple.compiler.types.Type;
import simple.compiler.types.VariantType;

/**
 * 此类实现了迭代集合或数组的“循环-每个-声明”。
 *
 * @author Herbert Czymontek
 */
public final class ForEachStatement extends IterativeLoopStatement {

	// 集合的运行时库支持的内部名称
	private static final String COLLECTION_INTERNALNAME =
			Compiler.RUNTIME_ROOT_INTERNAL + "/collections/集合";

	// 包含语句的辅助方法的运行时库包。
	private static final String STATEMENT_HELPERS_INTERNAL_NAME =
			Compiler.RUNTIME_ROOT_INTERNAL + "/helpers/StmtHelpers";

	// 用于保存已计算初始化、当前索引和计数表达式的结果的临时变量
	private LocalVariableSymbol initExprTemp;
	private LocalVariableSymbol indexTemp;
	private LocalVariableSymbol countTemp;

	/**
	 * 创建一个新的“循环-每个-声明”。
	 *
	 * @param position  源代码语句的起始位置
	 * @param loopVarExpr  要迭代的变量
	 * @param initExpr  循环变量的初始化表达式，欲要遍历的目标
	 * @param loopStatements  循环体中的语句
	 */
	public ForEachStatement(long position, Expression loopVarExpr, Expression initExpr,
			StatementBlock loopStatements) {
		super(position, loopVarExpr, initExpr, loopStatements);
	}

	@Override
	public void resolve(Compiler compiler, FunctionSymbol currentFunction) {
		super.resolve(compiler, currentFunction);

		// 检验欲要遍历目标类型
		Type initType = initExpr.getType();
		if (!initType.isArrayType() && !initType.isVariantType() &&
				!initType.internalName().equals(COLLECTION_INTERNALNAME)) {
			compiler.error(loopVarExpr.getPosition(), Error.errArrayOrCollectionNeededInForEach);
		}

		initExprTemp = currentFunction.addTempVariable(initType);
		indexTemp = currentFunction.addTempVariable(IntegerType.integerType);
		countTemp = currentFunction.addTempVariable(IntegerType.integerType);
	}

	@Override
	protected TokenKind getLoopStartToken() {
		return TokenKind.TOK_FOR;
	}

	@Override
	public void generate(Method m) {
		Type initType = initExpr.getType();

		// Generate the initialization expression into a temp
		generateLineNumberInformation(m);
		initExpr.generate(m);
		m.generateInstrDup();
		initType.generateStoreLocal(m, initExprTemp);

		if (initType.isVariantType()) {
			m.generateInstrInvokestatic(STATEMENT_HELPERS_INTERNAL_NAME, "forEachCount",
					"(L" + VariantType.VARIANT_INTERNAL_NAME + ";)I");
		} else if (initType.isArrayType()) {
			m.generateInstrArraylength();
		} else {
			m.generateInstrInvokevirtual(COLLECTION_INTERNALNAME, "计数", "()I");
		}
		IntegerType.integerType.generateStoreLocal(m, countTemp);

		// Initialize index variable
		// TODO: generate better code
		m.generateInstrLdc(0);
		IntegerType.integerType.generateStoreLocal(m, indexTemp);

		// Generate the loop body.
		Method.Label testLabel = Method.newLabel();
		m.generateInstrGoto(testLabel);

		Method.Label loopLabel = Method.newLabel();
		m.setLabel(loopLabel);

		loopVarExpr.generatePrepareWrite(m);
		initType.generateLoadLocal(m, initExprTemp);
		IntegerType.integerType.generateLoadLocal(m, indexTemp);
		Type loopVarType = loopVarExpr.getType();
		if (initType.isArrayType()) {
			Type elementType = ((ArrayType) initType).getElementType();
			elementType.generateLoadArray(m);
			if (!loopVarType.equals(elementType)) {
				elementType.generateConversion(m, loopVarType);
			}
		} else {
			if (initType.isVariantType()) {
				m.generateInstrInvokestatic(STATEMENT_HELPERS_INTERNAL_NAME, "forEachItem",
						"(L" + VariantType.VARIANT_INTERNAL_NAME + ";I)" +
								"L" + VariantType.VARIANT_INTERNAL_NAME + ";");
			} else {
				m.generateInstrInvokevirtual(COLLECTION_INTERNALNAME, "项目",
					"(I)L" + VariantType.VARIANT_INTERNAL_NAME + ';');
			}
			if (!loopVarType.equals(VariantType.variantType)) {
				VariantType.variantType.generateConversion(m, loopVarType);
			}
		}

		loopVarExpr.generateWrite(m);

		loopStatements.generate(m);

		// Generate step
		generateLineNumberInformation(m);
		m.generateInstrIinc(indexTemp.getVarIndex(), (byte)1);

		// Generate test
		m.setLabel(testLabel);
		IntegerType.integerType.generateLoadLocal(m, indexTemp);
		IntegerType.integerType.generateLoadLocal(m, countTemp);
		IntegerType.integerType.generateBranchIfCmpLess(m, loopLabel);

		m.setLabel(exitLabel);
	}

	@Override
	public String toString() {
		return "循环 每个" + loopVarExpr.toString() + " 从 " + initExpr.toString();  // COV_NF_LINE
	}
}
