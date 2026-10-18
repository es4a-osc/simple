package simple.runtime;

import java.util.Arrays;

import junit.framework.TestCase;

import simple.runtime.collections.正则表达式;

/**
 * 验证批量正则查找的匹配标志、零长度边界及已有匹配状态隔离。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class RegexTest extends TestCase {

	/**
	 * 不依赖逐项匹配初始化，保留编译标志并按顺序返回完整匹配而非捕获组。
	 */
	public void testAllMatchesUsesCompiledFlags() {
		正则表达式 expression = new 正则表达式();
		expression.编译("^([a-z]+)=([0-9]+)$",
			正则表达式.匹配_不区分大小写 | 正则表达式.匹配_多行模式);

		assertEquals(Arrays.asList("APPLE=12", "pear=7"),
			Arrays.asList(expression.取全部匹配("APPLE=12\n忽略\npear=7")));
		assertEquals(0, expression.取全部匹配("未找到").length);
		assertEquals(0, expression.取全部匹配("").length);
	}

	/**
	 * 空表达式及零长度匹配按查找规则推进，不丢弃合法空文本或进入死循环。
	 */
	public void testZeroLengthMatches() {
		正则表达式 expression = new 正则表达式();
		expression.编译("(?=a)");
		assertEquals(Arrays.asList("", ""), Arrays.asList(expression.取全部匹配("aa")));

		expression.编译("");
		assertEquals(Arrays.asList(""), Arrays.asList(expression.取全部匹配("")));
		assertEquals(Arrays.asList("", "", ""), Arrays.asList(expression.取全部匹配("ab")));
	}

	/**
	 * 另一个输入的批量查找不覆盖原输入、捕获组或已推进及已耗尽的游标。
	 */
	public void testExistingMatcherRemainsUnchanged() {
		正则表达式 expression = new 正则表达式();
		expression.编译("([0-9]+)");
		expression.匹配("a1 b22 c333");
		assertTrue(expression.匹配下一个());

		assertEquals(Arrays.asList("9", "88"), Arrays.asList(expression.取全部匹配("x9 y88")));
		assertEquals("1", expression.取匹配文本());
		assertEquals("1", expression.取子匹配文本(1));
		assertEquals(1, expression.取开始位置());
		assertEquals(2, expression.取结束位置());
		assertTrue(expression.匹配下一个());
		assertEquals("22", expression.取匹配文本());
		assertTrue(expression.匹配下一个());
		assertEquals("333", expression.取匹配文本());
		assertFalse(expression.匹配下一个());

		assertEquals(0, expression.取全部匹配("无数字").length);
		assertFalse(expression.匹配下一个());
		assertEquals("a# b# c#", expression.替换("#"));
	}

	/**
	 * 延续未编译与空引用的拒绝约定，失败调用也不破坏已有匹配结果。
	 */
	public void testInvalidStateDoesNotAlterMatcher() {
		正则表达式 expression = new 正则表达式();
		try {
			expression.取全部匹配("abc");
			fail("未编译时应拒绝查找");
		} catch (NullPointerException expected) {
			assertEquals("未编译表达式", expected.getMessage());
		}

		expression.编译("a");
		expression.匹配("a");
		assertTrue(expression.匹配下一个());
		try {
			expression.取全部匹配(null);
			fail("空引用不能作为待查找文本");
		} catch (NullPointerException expected) {
			assertEquals("a", expression.取匹配文本());
		}
	}
}
