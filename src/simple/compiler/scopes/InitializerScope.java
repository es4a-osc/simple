package simple.compiler.scopes;

import simple.compiler.scanner.Scanner;
import simple.compiler.symbols.LocalVariableSymbol;
import simple.compiler.symbols.Symbol;

/**
 * 局部变量初值的名称查找边界；不改变普通语句的既有作用域规则。
 *
 * <p>解析器完成整个函数后才解析表达式，因此父作用域可能已经登记了后续变量。
 * 初值不能读取自身或尚未声明的局部变量，否则可能读取未赋值的 JVM 局部槽。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class InitializerScope extends Scope {

	private final int declarationOffset;

	/**
	 * 创建局部变量初值的名称查找边界，以声明位置限制可见的局部变量。
	 *
	 * @param outerScope  变量声明所在的外层作用域
	 * @param declarationPosition  当前变量声明的源码位置
	 */
	public InitializerScope(Scope outerScope, long declarationPosition) {
		super(outerScope);
		declarationOffset = Scanner.getOffset(declarationPosition);
	}

	@Override
	public Symbol lookupDeep(String identifier) {
		Symbol symbol = lookupShallow(identifier);
		if (symbol != null) {
			return symbol;
		}
		symbol = outerScope.lookupDeep(identifier);
		if (symbol instanceof LocalVariableSymbol
				&& Scanner.getOffset(symbol.getPosition()) >= declarationOffset) {
			return null;
		}
		return symbol;
	}
}
