package simple.runtime;

import junit.framework.TestCase;

import simple.runtime.errors.断言失败;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.IntegerVariant;

/**
 * 验证断言操作的成功、失败及数值逻辑转换，不依赖编译器用例。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class AssertionsTest extends TestCase {

	/**
	 * 满足真、假条件的输入正常结束。
	 */
	public void testAcceptedBooleanConditions() {
		断言操作.断言真(BooleanVariant.getBooleanVariant(true));
		断言操作.断言假(BooleanVariant.getBooleanVariant(false));
	}

	/**
	 * 真断言不接受假值，必须产生约定的运行时错误。
	 */
	public void testTrueAssertionRejectsFalse() {
		try {
			断言操作.断言真(BooleanVariant.getBooleanVariant(false));
			fail("假值不能通过真断言");
		} catch (断言失败 expected) {
			// 只接受运行库的断言错误，不捕获 JUnit 自身的失败。
		}
	}

	/**
	 * 假断言不接受真值。
	 */
	public void testFalseAssertionRejectsTrue() {
		try {
			断言操作.断言假(BooleanVariant.getBooleanVariant(true));
			fail("真值不能通过假断言");
		} catch (断言失败 expected) {
			// 失败类型属于公开的 Simple 运行时错误。
		}
	}

	/**
	 * 整数变体沿用零为假、非零为真的转换约定。
	 */
	public void testNumericConditions() {
		断言操作.断言假(IntegerVariant.getIntegerVariant(0));
		断言操作.断言真(IntegerVariant.getIntegerVariant(1));
		断言操作.断言真(IntegerVariant.getIntegerVariant(-1));
	}
}
