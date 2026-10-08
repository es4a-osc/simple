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

package simple.compiler.symbols;

import simple.classfiles.ClassFile;
import simple.classfiles.Method;
import simple.classfiles.MethodTooBigException;
import simple.classfiles.Method.Label;
import simple.compiler.Compiler;
import simple.compiler.Error;
import simple.compiler.RuntimeLoader;
import simple.compiler.scanner.Scanner;
import simple.compiler.scanner.TokenKind;
import simple.compiler.scopes.LocalScope;
import simple.compiler.scopes.Scope;
import simple.compiler.statements.OnErrorStatement;
import simple.compiler.statements.StatementBlock;
import simple.compiler.statements.synthetic.LocalVariableDefinitionStatement;
import simple.compiler.statements.synthetic.MarkerStatement;
import simple.compiler.types.FunctionType;
import simple.compiler.types.Type;
import simple.util.Preconditions;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

/**
 * 所有函数符号的超类。
 *
 * @author Herbert Czymontek
 */
public abstract class FunctionSymbol extends Symbol implements SymbolWithType {

	/*
	 * This is a local helper class used to store information that available for compiled functions
	 * only. Separating this information results in smaller function symbols for functions defined
	 * from loaded class files.
	 */
	private class CompilationInformation {
		// Number of variables used by function
		private short varCount;

		// List of formal parameters
		private final List<LocalVariableSymbol> params;

		// List of local variables
		private final List<LocalVariableSymbol> locals;

		// Function scope
		private final LocalScope scope;

		// 'Me' variable - can be null for static functions
		private LocalVariableSymbol me;

		// Local variable for function results (will be null for procedures)
		private LocalVariableSymbol returnVariable;

		// Statements in function body (can be null for interface function definitions)
		private StatementBlock statements;

		// Optional On Error statement (one per function)
		private OnErrorStatement onErrorStatement;

		// Begin of function scope marker statement
		private final MarkerStatement beginFunctionMarker;

		// Label to branch to for function Exit-statement
		private Method.Label exitLabel;

		// Indicates whether the function is a property getter or setter (properties need different
		// attributes generated)
		private boolean isProperty;

		// Indicates whether the function is compiler generated
		private boolean isGenerated;

		/*
		 * Creates a new compilation information record (as the name suggests, for compiled functions
		 * only).
		 */
		private CompilationInformation() {
			params = new ArrayList<LocalVariableSymbol>();
			locals = new ArrayList<LocalVariableSymbol>();
			statements = new StatementBlock(definingObject.getScope());
			exitLabel = Method.newLabel();
			scope = (LocalScope) statements.getScope();

			// Will mark the start-of-scope for all parameters
			beginFunctionMarker = new MarkerStatement();
			statements.add(beginFunctionMarker);

			// Add a 'Me' variable for instance functions
			if (hasMeArgument()) {
				// Even though we call this variable 'Me', the Java debugger gets confused if it is not
				// called this. Specifically, JDI will report that there is a "this" and also a variable
				// named "Me.
				me = addVariable(getPosition(), "this", definingObject.getType(), false);
				me.setBeginScope(beginFunctionMarker);

				// Even though it will never be looked up via its scope, 'Me' needs to be entered because
				// the scope is responsible for setting variable scope information for debug information
				// generation.
				scope.enterSymbol(me);
			}
		}

		/*
		 * 添加函数参数。
		 */
		private LocalVariableSymbol addParameter(long position, String parName, Type parType,
				boolean isRef) {
			LocalVariableSymbol param = addVariable(position, parName, parType, isRef);
			param.setBeginScope(beginFunctionMarker);
			referenceParameters.set(params.size(), isRef);
			params.add(param);

			if (scope.lookupShallow(parName) != null) {
				return null;
			}

			scope.enterSymbol(param);
			return param;
		}

		/*
		 * 添加新的局部变量。
		 */
		private LocalVariableSymbol addLocalVariable(long position, String varName, Type varType,
				Scope varScope) {
			LocalVariableSymbol local = addVariable(position, varName, varType, false);
			locals.add(local);

			if (varScope.lookupShallow(varName) != null) {
				return null;
			}

			varScope.enterSymbol(local);
			return local;
		}

		/*
		 * 添加新变量（参数或局部变量）。
		 * 该方法只应从编译信息中调用。
		 */
		private LocalVariableSymbol addVariable(long position, String varName, Type varType,
				boolean isRef) {
			short varIndex = varCount;
			varCount += (varType.isWideType() && !isRef) ? 2 : 1;
			return new LocalVariableSymbol(position, varName, varType, varIndex, isRef);
		}

		/*
		 * 解析形式参数列表。
		 */
		private void resolveParameters(Compiler compiler) {
			for (LocalVariableSymbol param : params) {
				param.resolve(compiler, FunctionSymbol.this);
				formalArgumentTypes.add(param.getType());
			}
		}

		/*
		 * 解析函数体（包括局部变量）。
		 */
		private void resolveBody(Compiler compiler) {
			if (statements != null) {
				// 首先解析局部变量
				for (LocalVariableSymbol local: locals) {
					local.resolve(compiler, FunctionSymbol.this);
				}

				// 解析实际函数体
				statements.resolve(compiler, FunctionSymbol.this, null);

				if (onErrorStatement != null) {
					onErrorStatement.resolve(compiler, FunctionSymbol.this);
				}
			}
		}
	}

	// 对象定义功能
	private ObjectSymbol definingObject;

	// 形式参数类型列表
	private final List<Type> formalArgumentTypes;

	// 表示参考（引用）参数。
	private final BitSet referenceParameters;

	// 函数的结果类型（对于过程为空）
	private Type resultType;

	// 有关已编译函数的函数体的附加信息
	private CompilationInformation compilationInformation;

	// 函数类型
	private Type type;

	// 仅由同一名称在作用域中的首个函数保存；按参数个数定位各重载。
	private List<FunctionSymbol> overloads;

	/**
	 * 创建新的函数标识符。
	 *
	 * @param position  源代码的标识符起始位置
	 * @param definingObject  定义对象
	 * @param name  函数名称
	 */
	public FunctionSymbol(long position, ObjectSymbol definingObject, String name) {
		super(position, name);

		this.definingObject = definingObject;

		formalArgumentTypes = new ArrayList<Type>();
		referenceParameters = new BitSet();
	}

	/**
	 * 返回函数或过程的形式参数个数。
	 *
	 * <p>源码函数在类型解析前从已解析的参数声明中取值；运行库函数直接从
	 * Java方法转换后的参数类型列表中取值。因此声明登记阶段即可按参数个数判重。
	 *
	 * @return 形式参数个数
	 */
	public int getParameterCount() {
		return compilationInformation != null ?
				compilationInformation.params.size() : formalArgumentTypes.size();
	}

	/**
	 * 将同名、不同参数个数的函数或过程加入当前重载组。
	 *
	 * @param function 要加入的函数或过程
	 * @return 参数个数未被占用时返回{@code true}；参数个数重复时返回{@code false}
	 */
	public boolean addOverload(FunctionSymbol function) {
		if (overloads == null) {
			overloads = new ArrayList<FunctionSymbol>();
			overloads.add(this);
		}

		int parameterCount = function.getParameterCount();
		for (FunctionSymbol overload : overloads) {
			if (overload == function) {
				return true;
			}
			if (overload.getParameterCount() == parameterCount) {
				return false;
			}
		}

		overloads.add(function);
		return true;
	}

	/**
	 * 在当前对象声明的同名函数中按参数个数查找重载。
	 *
	 * @param parameterCount 调用实参数量
	 * @return 匹配的函数或过程；没有匹配项时返回{@code null}
	 */
	public FunctionSymbol getOverload(int parameterCount) {
		if (overloads == null) {
			return getParameterCount() == parameterCount ? this : null;
		}

		for (FunctionSymbol overload : overloads) {
			if (overload.getParameterCount() == parameterCount) {
				return overload;
			}
		}
		return null;
	}

	/**
	 * 将函数标记为已编译函数。
	 */
	public void setIsCompiled() {
		compilationInformation = new CompilationInformation();
	}

	/**
	 * 将函数标记为抽象（接口函数）。
	 */
	public void setIsAbstract() {
		compilationInformation.statements = null;
	}

	/**
	 * 将函数标记为属性获取器或设置器。
	 */
	public void setIsProperty() {
		// This will cause generation of the @SimpleProperty annotation rather than @SimpleFunction.
		compilationInformation.isProperty = true;
	}

	/**
	 * 将函数标记为编译器生成的函数。
	 */
	public void setIsGenerated() {
		Preconditions.checkNotNull(compilationInformation);
		compilationInformation.isGenerated = true;
	}

	/**
	 * 设置已编译函数的“位于 错误”语句。
	 *
	 * @param onErrorStatement  On-Error statement
	 * @return  {@code false} if there is another On-Error statement in this
	 *          function already, {@code true} otherwise
	 */
	public boolean setOnErrorStatement(OnErrorStatement onErrorStatement) {
		Preconditions.checkNotNull(compilationInformation);

		if (compilationInformation.onErrorStatement != null) {
			return false;
		}

		compilationInformation.onErrorStatement = onErrorStatement;
		return true;
	}

	/**
	 * 设置函数结果类型。
	 *
	 * @param type  函数结果类型
	 */
	public void setResultType(Type type) {
		resultType = type;
		if (compilationInformation != null) {
			// If this is a compiled function we also need to allocate a local variable of the same
			// name as the function to hold the function result
			LocalVariableSymbol returnVariable = addLocalVariable(getPosition(), getName(), type,
					getScope());
			MarkerStatement marker = new LocalVariableDefinitionStatement(returnVariable);
			compilationInformation.statements.add(marker);
			returnVariable.setBeginScope(marker);
			compilationInformation.returnVariable = returnVariable;
		}
	}

	/**
	 * Change the defining object for this function. This is needed when moving an
	 * event handler into a synthetic inner class.
	 *
	 * @param object  new defining object
	 */
	public void changeDefiningObject(ObjectSymbol object) {
		definingObject = object;
	}

	/**
	 * Indicates whether this function is a compiled function (as opposed to
	 * loaded from a library).
	 *
	 * @return  {@code true} for compiled functions, {@code false} otherwise
	 */
	public boolean isCompiled() {
		return compilationInformation != null;
	}

	/**
	 * Indicates whether the functions is an instance or object function.
	 *
	 * @return  {@code true} for instance functions, {@code false} otherwise
	 */
	public boolean hasMeArgument() {
		return false;
	}

	/**
	 * Returns the function result type.
	 *
	 * @return  function result type or {@code null} for procedures
	 */
	public Type getResultType() {
		return resultType;
	}

	/**
	 * Adds a formal parameter.
	 *
	 * @param position  source code start position of symbol
	 * @param parName  parameter name (can be {@code null} for loaded functions)
	 * @param parType  parameter type
	 * @param isRef  indicates reference parameter
	 * @return  parameter symbol or {@code null} for redefiniton errors
	 *          (note: will always be {code null} for loaded functions which does
	 *          not indicate an error)
	 */
	public LocalVariableSymbol addParameter(long position, String parName, Type parType,
			boolean isRef) {
		if (compilationInformation != null) {
			return compilationInformation.addParameter(position, parName, parType, isRef);
		} else {
			referenceParameters.set(formalArgumentTypes.size(), isRef);
			formalArgumentTypes.add(parType);
			return null;
		}
	}

	/**
	 * Adds a local variable. May not be called for loaded functions!
	 *
	 * @param position  source code start position of symbol
	 * @param varName  local variable name
	 * @param varType  local variable type
	 * @param scope  scope to enter local variable into
	 * @return  local variable symbol
	 */
	public LocalVariableSymbol addLocalVariable(long position, String varName, Type varType,
			Scope scope) {
		Preconditions.checkNotNull(compilationInformation);

		return compilationInformation.addLocalVariable(position, varName, varType, scope);
	}

	/**
	 * Adds a temporary local variable. Should be used to allocated additional
	 * variables needed for code generation. Must be called during the resolution
	 * phase.
	 *
	 * @param varType  type of temporary
	 * @return  temporary variable symbol
	 */
	public LocalVariableSymbol addTempVariable(Type varType) {
		return compilationInformation.addVariable(Scanner.NO_POSITION, "<temp>", varType, false);
	}

	/**
	 * Returns the object defining the data member.
	 *
	 * @return  defining object
	 */
	public ObjectSymbol getDefiningObject() {
		return definingObject;
	}

	/**
	 * 返回函数作用域。
	 *
	 * @return  函数作用域
	 */
	public Scope getScope() {
		return compilationInformation.scope;
	}

	/**
	 * 返回函数主体中的语句。
	 *
	 * @return  function body statement block (can be {@code null} for abstract
	 *          functions)
	 */
	public StatementBlock getFunctionStatements() {
		return compilationInformation.statements;
	}

	@Override
	public void resolve(Compiler compiler, FunctionSymbol currentFunction) {
		if (type == null) {
			// For compiled functions resolve formal parameters (will resolve types needed to resolve
			// function type)
			if (compilationInformation != null) {
				compilationInformation.resolveParameters(compiler);
			}

			// 创建和解析函数类型
			type = new FunctionType(hasMeArgument(), formalArgumentTypes, referenceParameters,
					resultType);
			type.resolve(compiler);
		}
	}

	/**
	 * Resolves the body of compiled functions.
	 * 解析了编译后的函数体。
	 *
	 * @param compiler  当前的编译器实例
	 */
	public void resolveBody(Compiler compiler) {
		if (compilationInformation != null) {
			compilationInformation.resolveBody(compiler);
		}
	}

	/**
	 * 返回函数退出语句的退出标签。
	 *
	 * @return  退出标签
	 */
	public Method.Label getExitLabel() {
		return compilationInformation.exitLabel;
	}

	/**
	 * 在函数的退出语句后返回要预期的令牌。
	 *
	 * @return  退出令牌
	 */
	public TokenKind getExitToken() {
		if (compilationInformation.isProperty) {
			return TokenKind.TOK_PROPERTY;
		} else if (resultType != null) {
			return TokenKind.TOK_FUNCTION;
		} else {
			return TokenKind.TOK_SUB;
		}
	}

	@Override
	public void generateRead(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	@Override
	public void generateWrite(Method m) {
		Compiler.internalError();  // COV_NF_LINE
	}

	/**
	 * 为函数生成代码。
	 *
	 * @param compiler  当前编译器实例
	 * @param cf  类文件来生成代码
	 */
	public void generate(Compiler compiler, ClassFile cf) {
		// 默认情况下，所有Simple函数都是公共函数
		short access = Method.ACC_PUBLIC;

		// 对象函数是静态的
		if (!hasMeArgument()) {
			access |= Method.ACC_STATIC;
		}

		// 接口函数是抽象的
		if (compilationInformation.statements == null) {
			access |= Method.ACC_ABSTRACT;
		}

		Method m = cf.newMethod(access, getName(), type.signature());
		// 将函数/属性标记为Simple函数/属性
		if (!compilationInformation.isGenerated) {
			m.getRuntimeVisibleAnnotationsAttribute().newAnnotation(
					'L' + RuntimeLoader.ANNOTATION_INTERNAL +
					(compilationInformation.isProperty ? "/SimpleProperty;" : "/SimpleFunction;"));
		}

		// 如果有一个函数体，那么生成字节码
		if (compilationInformation.statements != null) {
			try {
				m.startCodeGeneration();

				// If there is an On Error statement then we must explicitly initialize all local
				// variables to appease the verifier (even if they later re-initialized).
				if (compilationInformation.onErrorStatement != null) {
					for (LocalVariableSymbol local : compilationInformation.locals) {
						local.generateInitializer(m);
					}
				}

				// Need to copy reference parameters into their dereferenced temporary local variables
				for (LocalVariableSymbol param :compilationInformation.params) {
					// TODO: generate better code - only do this for used parameters
					param.readReferenceParameter(m);
				}

				Label startLabel = Method.newLabel();
				m.setLabel(startLabel);

				compilationInformation.statements.generate(m);

				m.setLabel(compilationInformation.exitLabel);

				// Need to write back local temps of reference parameters
				for (LocalVariableSymbol param :compilationInformation.params) {
					// TODO: generate better code - only do this for used parameters
					param.writeBackReferenceParameter(m);
				}

				if (compilationInformation.returnVariable == null) {
					m.generateInstrReturn();
				} else {
					compilationInformation.returnVariable.generateRead(m);
					compilationInformation.returnVariable.getType().generateReturn(m);
				}

				// 生成错误语句（如果有）
				if (compilationInformation.onErrorStatement != null) {

					Label handlerLabel = Method.newLabel();
					m.setLabel(handlerLabel);

					m.generateExceptionHandlerInfo(startLabel, compilationInformation.exitLabel, handlerLabel,
							"java/lang/Throwable");

					compilationInformation.onErrorStatement.generate(m);
				}

				// 生成局部变量调试信息
				if (compilationInformation.me != null) {
					generateLocalVarDebugInfo(m, compilationInformation.me);
				}
				for (LocalVariableSymbol param: compilationInformation.params) {
					generateLocalVarDebugInfo(m, param);
				}
				for (LocalVariableSymbol local: compilationInformation.locals) {
					generateLocalVarDebugInfo(m, local);
				}

				m.finishCodeGeneration();
			} catch (MethodTooBigException e) {
				compiler.error(Scanner.NO_POSITION, Error.errFunctionTooBig, getName(),
						definingObject.getType().internalName().replace('/', '.'));
			}
		}
	}

	/*
	 * 为局部变量生成调试信息。
	 */
	private void generateLocalVarDebugInfo(Method m, LocalVariableSymbol local) {
		m.generateLocalVariableInformation(local.getStartScopeLabel(), local.getEndScopeLabel(),
				local.getName(), local.getType().signature(), local.getActualVarIndex());
	}

	// SymbolWithType 实现

	public final Type getType() {
		return type;
	}

	public final void setType(Type type) {
		this.type = type;
	}
}
