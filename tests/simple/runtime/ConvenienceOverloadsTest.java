package simple.runtime;

import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.TestCase;

import simple.runtime.collections.正则表达式;
import simple.runtime.collections.线程;

/**
 * 验证便捷重载的边界、默认匹配规则、时区及等待语义。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ConvenienceOverloadsTest extends TestCase {

	/**
	 * 省略截取长度时保留全部后缀，并继续拒绝越界起点。
	 */
	public void testTextSuffixBounds() {
		assertEquals("中文", 文本操作.取文本中间("A中文", 1));
		assertEquals("", 文本操作.取文本中间("abc", 3));
		assertEquals("", 文本操作.取文本中间("", 0));
		invalid(() -> 文本操作.取文本中间("abc", -1));
		invalid(() -> 文本操作.取文本中间("abc", 4));
	}

	/**
	 * 字节后缀是独立副本，不因省略长度而返回原数组。
	 */
	public void testByteSuffixCopiesData() {
		byte[] source = new byte[]{1, 2, 3};
		byte[] suffix = 字节集操作.取字节集中间(source, 1);
		assertTrue(Arrays.equals(new byte[]{2, 3}, suffix));
		suffix[0] = 9;
		assertEquals(2, source[1]);
		assertEquals(0, 字节集操作.取字节集中间(source, source.length).length);
		assertEquals(0, 字节集操作.取字节集中间(new byte[0], 0).length);
		invalid(() -> 字节集操作.取字节集中间(source, -1));
		invalid(() -> 字节集操作.取字节集中间(source, 4));
	}

	/**
	 * 默认过滤保留包含项，维持去重顺序，完整接口仍可排除匹配项。
	 */
	public void testFilterIncludesByDefault() {
		String[] source = new String[]{"beta", "alpha", "beta", "gamma"};
		assertTrue(Arrays.equals(new String[]{"beta", "alpha", "gamma"}, 数组操作.过滤数组文本(source, "a")));
		assertTrue(Arrays.equals(new String[]{"beta"}, 数组操作.过滤数组文本(source, "et")));
		assertEquals(0, 数组操作.过滤数组文本(source, "absent").length);
		assertTrue(Arrays.equals(new String[]{"alpha", "gamma"}, 数组操作.过滤数组文本(source, "et", false)));
		assertEquals(4, source.length);
	}

	/**
	 * 默认正则区分大小写，显式匹配标志不受影响。
	 */
	public void testDefaultRegexFlags() {
		正则表达式 expression = new 正则表达式();
		expression.编译("a([0-9]+)");
		expression.匹配("a42");
		assertTrue(expression.匹配下一个());
		assertEquals("42", expression.取子匹配文本(1));
		expression.匹配("A42");
		assertFalse(expression.匹配下一个());
		expression.编译("a([0-9]+)", 正则表达式.匹配_不区分大小写);
		expression.匹配("A42");
		assertTrue(expression.匹配下一个());
	}

	/**
	 * 默认格式使用对象自身时区，不能偷偷切换到设备默认时区。
	 */
	public void testDateFormatKeepsObjectTimeZone() {
		Calendar value = Calendar.getInstance(TimeZone.getTimeZone("Asia/Shanghai"));
		value.setTimeInMillis(0);
		assertEquals("1970-01-01 08:00:00", 日期时间操作.格式化时间(value));
		value.setTimeZone(TimeZone.getTimeZone("UTC"));
		assertEquals("1970-01-01 00:00:00", 日期时间操作.格式化时间(value));
		assertEquals("1970", 日期时间操作.格式化时间(value, "yyyy"));
	}

	/**
	 * 无参数参与等待线程真实结束，而非只检查线程已启动。
	 */
	public void testJoinWithoutTimeout() {
		final AtomicBoolean completed = new AtomicBoolean();
		线程 worker = new 线程() {
			@Override
			public void 线程运行() {
				completed.set(true);
			}
		};

		assertTrue(worker.启动());
		assertTrue(worker.参与());
		assertTrue(completed.get());
		assertEquals(Thread.State.TERMINATED, worker.getThread().getState());
	}

	/**
	 * 确认原完整接口的参数校验仍由便捷入口保留。
	 *
	 * @param action 应拒绝无效参数的操作。
	 */
	private static void invalid(Runnable action) {
		try {
			action.run();
			fail("无效参数未被拒绝");
		} catch (IllegalArgumentException expected) {
			// 保留原有参数错误约定。
		}
	}
}
