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

package simple.runtime;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 实现各种与日期和时间相关的运行时功能。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public final class 日期时间操作 {

	/* 日期/时间间隔类型 */

	/**
	 * 日期运算使用的年字段。
	 */
	@SimpleDataElement
	public static final int 日期_年 = Calendar.YEAR;

	/**
	 * 日期运算使用的月字段。
	 */
	@SimpleDataElement
	public static final int 日期_月 = Calendar.MONTH;

	/**
	 * 日期运算使用的天字段。
	 */
	@SimpleDataElement
	public static final int 日期_天 = Calendar.DATE;

	/**
	 * 日期运算使用的星期字段。
	 */
	@SimpleDataElement
	public static final int 日期_星期 = Calendar.WEEK_OF_YEAR;

	/**
	 * 日期运算使用的时字段。
	 */
	@SimpleDataElement
	public static final int 日期_时 = Calendar.HOUR_OF_DAY;

	/**
	 * 日期运算使用的分字段。
	 */
	@SimpleDataElement
	public static final int 日期_分 = Calendar.MINUTE;

	/**
	 * 日期运算使用的秒字段。
	 */
	@SimpleDataElement
	public static final int 日期_秒 = Calendar.SECOND;

	/* 一星期中第几天 */

	/**
	 * Java 日期星期常量：星期一。
	 */
	@SimpleDataElement
	public static final int 星期_星期一 = Calendar.MONDAY;

	/**
	 * Java 日期星期常量：星期二。
	 */
	@SimpleDataElement
	public static final int 星期_星期二 = Calendar.TUESDAY;

	/**
	 * Java 日期星期常量：星期三。
	 */
	@SimpleDataElement
	public static final int 星期_星期三 = Calendar.WEDNESDAY;

	/**
	 * Java 日期星期常量：星期四。
	 */
	@SimpleDataElement
	public static final int 星期_星期四 = Calendar.THURSDAY;

	/**
	 * Java 日期星期常量：星期五。
	 */
	@SimpleDataElement
	public static final int 星期_星期五 = Calendar.FRIDAY;

	/**
	 * Java 日期星期常量：星期六。
	 */
	@SimpleDataElement
	public static final int 星期_星期六 = Calendar.SATURDAY;

	/**
	 * Java 日期星期常量：星期天。
	 */
	@SimpleDataElement
	public static final int 星期_星期天 = Calendar.SUNDAY;

	/* 月份：沿用 Calendar 的 0–11 编号，一月为 0。 */

	/**
	 * Java 日期月份常量：一月，月份编号从 0 开始。对应 {@code Calendar.JANUARY}。
	 */
	@SimpleDataElement
	public static final int 月份_一月 = Calendar.JANUARY;

	/**
	 * Java 日期月份常量：二月，月份编号从 0 开始。对应 {@code Calendar.FEBRUARY}。
	 */
	@SimpleDataElement
	public static final int 月份_二月 = Calendar.FEBRUARY;

	/**
	 * Java 日期月份常量：三月，月份编号从 0 开始。对应 {@code Calendar.MARCH}。
	 */
	@SimpleDataElement
	public static final int 月份_三月 = Calendar.MARCH;

	/**
	 * Java 日期月份常量：四月，月份编号从 0 开始。对应 {@code Calendar.APRIL}。
	 */
	@SimpleDataElement
	public static final int 月份_四月 = Calendar.APRIL;

	/**
	 * Java 日期月份常量：五月，月份编号从 0 开始。对应 {@code Calendar.MAY}。
	 */
	@SimpleDataElement
	public static final int 月份_五月 = Calendar.MAY;

	/**
	 * Java 日期月份常量：六月，月份编号从 0 开始。对应 {@code Calendar.JUNE}。
	 */
	@SimpleDataElement
	public static final int 月份_六月 = Calendar.JUNE;

	/**
	 * Java 日期月份常量：七月，月份编号从 0 开始。对应 {@code Calendar.JULY}。
	 */
	@SimpleDataElement
	public static final int 月份_七月 = Calendar.JULY;

	/**
	 * Java 日期月份常量：八月，月份编号从 0 开始。对应 {@code Calendar.AUGUST}。
	 */
	@SimpleDataElement
	public static final int 月份_八月 = Calendar.AUGUST;

	/**
	 * Java 日期月份常量：九月，月份编号从 0 开始。对应 {@code Calendar.SEPTEMBER}。
	 */
	@SimpleDataElement
	public static final int 月份_九月 = Calendar.SEPTEMBER;

	/**
	 * Java 日期月份常量：十月，月份编号从 0 开始。对应 {@code Calendar.OCTOBER}。
	 */
	@SimpleDataElement
	public static final int 月份_十月 = Calendar.OCTOBER;

	/**
	 * Java 日期月份常量：十一月，月份编号从 0 开始。对应 {@code Calendar.NOVEMBER}。
	 */
	@SimpleDataElement
	public static final int 月份_十一月 = Calendar.NOVEMBER;

	/**
	 * Java 日期月份常量：十二月，月份编号从 0 开始。对应 {@code Calendar.DECEMBER}。
	 */
	@SimpleDataElement
	public static final int 月份_十二月 = Calendar.DECEMBER;

	/* 时区：北京时间为 UTC+08:00，换算为 GMT 时应减去 8 小时。 */

	/**
	 * 格林尼治时区标识。
	 */
	@SimpleDataElement
	public static final String 时区_格林尼治 = "GMT";

	/**
	 * 上海时区标识。
	 */
	@SimpleDataElement
	public static final String 时区_上海 = "Asia/Shanghai";

	// 日期时间文本格式数组
	public static final String[] DATE_FORMATS = new String[] {
		"yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd/HH:mm:ss", "yyyy/MM/dd/HH/mm/ss",
		"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd-HH:mm:ss", "yyyy-MM-dd-HH-mm-ss",
		"yyyy.MM.dd HH:mm:ss", "yyyy.MM.dd.HH:mm:ss", "yyyy.MM.dd.HH.mm.ss",
		"yyyy年MM月dd日 HH时mm分ss秒", "yyyy年MM月dd日HH时mm分ss秒", "yyyyMMddHHmmss",
		"yyyy/MM/dd", "yyyy-MM-dd", "yyyy.MM.dd", "yyyy年MM月dd日", "yyyyMMdd",
	};

	private 日期时间操作() {  // COV_NF_LINE
	}                  // COV_NF_LINE

	/**
	 * 在传入的日期对象上增减指定日历字段，不创建副本。
	 *
	 * @param date 需要修改的日期时间。
	 * @param intervalKind 间隔类型，参见 {@link #日期_年}、{@link #日期_月}、{@link #日期_星期} 等常量。
	 * @param interval 增量；正数增加，负数减少。
	 * @throws IllegalArgumentException 间隔类型不受支持。
	 */
	@SimpleFunction
	public static void 增减时间(Calendar date, int intervalKind, int interval) {
		switch (intervalKind) {
			default:
				throw new IllegalArgumentException("illegal date/time interval kind in function DateAdd()");

			case 日期_年:
			case 日期_月:
			case 日期_天:
			case 日期_星期:
			case 日期_时:
			case 日期_分:
			case 日期_秒:
				date.add(intervalKind, interval);
				break;
		}
	}

	/**
	 * 按预设格式依次解析设备默认时区的日期时间文本。
	 *
	 * <p>支持斜线、短横线、点、中文年月日及紧凑数字格式，完整列表见 {@link #DATE_FORMATS}。
	 * 沿用宽松解析：越界日期可能被调整，成功解析后允许尾随文本，不适合作为严格日期校验器。</p>
	 *
	 * @param value 日期时间文本，例如 2026-10-16 12:30:45。
	 * @return 设备默认时区的日期时间。
	 * @throws IllegalArgumentException 所有预设格式均无法解析。
	 */
	@SimpleFunction
	public static Calendar 日期值(String value) {
		Calendar calendar = new GregorianCalendar();

		for (String format : DATE_FORMATS) {
			try {
				DateFormat dateFormat = new SimpleDateFormat(format);
				calendar.setTime(dateFormat.parse(value));
				return calendar;
			} catch (ParseException e) {
				// 注意这里仅捕获解析异常，其它异常直接抛出。
				// 等待循环结束后，再抛出异常。
			}
		}
		throw new IllegalArgumentException("illegal date/time format in function DateValue()");
	}

	/**
	 * 读取日期时间的日。
	 *
	 * @param date 日期时间。
	 * @return 日，范围 1–31。
	 */
	@SimpleFunction
	public static int 取日(Calendar date) {
		return date.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 按 yyyy-MM-dd HH:mm:ss 格式输出日期时间，沿用对象自身时区。
	 *
	 * @param date 要格式化的日期时间，保留对象自身时区。
	 * @return 日期时间文本。
	 */
	@SimpleFunction
	public static String 格式化时间(Calendar date) {
		return 格式化时间(date, "yyyy-MM-dd HH:mm:ss");
	}

	/**
	 * 按给定格式和日期对象自身的时区生成文本。
	 *
	 * @param date 日期时间，不修改输入。
	 * @param pattern SimpleDateFormat 格式，例如 yyyy-MM-dd HH:mm:ss；月份用 MM，分钟用 mm。
	 * @return 格式化后的文本，文字名称随设备语言区域变化。
	 * @see SimpleDateFormat
	 */
	@SimpleFunction
	public static String 格式化时间(Calendar date, String pattern) {
		DateFormat dateFormat = new SimpleDateFormat(pattern);
		// 注意这里传递时区。
		dateFormat.setTimeZone(date.getTimeZone());
		return dateFormat.format(date.getTime());
	}

	/**
	 * 读取日期时间的小时。
	 *
	 * @param date 日期时间。
	 * @return 小时，范围 0–23。
	 */
	@SimpleFunction
	public static int 取小时(Calendar date) {
		return date.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * 读取日期时间的分钟。
	 *
	 * @param date 日期时间。
	 * @return 分钟，范围 0–59。
	 */
	@SimpleFunction
	public static int 取分钟(Calendar date) {
		return date.get(Calendar.MINUTE);
	}

	/**
	 * 读取日期时间的月份编号。
	 *
	 * @param date 日期时间。
	 * @return 月份编号，范围 0–11；一月为 0，与 {@link #月份_一月} 等常量一致。
	 */
	@SimpleFunction
	public static int 取月份(Calendar date) {
		return date.get(Calendar.MONTH);
	}

	/**
	 * 读取日期时间的本地化月份名称。
	 *
	 * @param date 日期时间。
	 * @return 月份名称，随设备语言区域变化。
	 */
	@SimpleFunction
	public static String 取月份名(Calendar date) {
		return String.format("%1$tB", date);
	}

	/**
	 * 读取设备当前日期时间。
	 *
	 * @return 设备默认时区的日期时间。
	 */
	@SimpleFunction
	public static Calendar 取现行时间() {
		return new GregorianCalendar();
	}

	/**
	 * 读取日期时间的秒。
	 *
	 * @param date 日期时间。
	 * @return 秒，范围 0–59。
	 */
	@SimpleFunction
	public static int 取秒(Calendar date) {
		return date.get(Calendar.SECOND);
	}

	/**
	 * 读取当前 Unix 毫秒时间戳，不是设备启动时长。
	 *
	 * @return 自 1970-01-01T00:00:00Z 起经过的毫秒数；不以文本位数定义单位。
	 */
	@SimpleFunction
	public static long 取现行时间戳() {
		return System.currentTimeMillis();
	}

	/**
	 * 读取日期时间的星期编号。
	 *
	 * @param date 日期时间。
	 * @return 星期编号，范围 1–7；星期天为 1、星期一为 2，参见星期常量。
	 */
	@SimpleFunction
	public static int 取星期(Calendar date) {
		return date.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 读取日期时间的本地化星期名称。
	 *
	 * @param date 日期时间。
	 * @return 星期名称，随设备语言区域变化。
	 */
	@SimpleFunction
	public static String 取星期名(Calendar date) {
		return String.format("%1$tA", date);
	}

	/**
	 * 读取日期时间的年份。
	 *
	 * @param date 日期时间。
	 * @return 年份。
	 */
	@SimpleFunction
	public static int 取年(Calendar date) {
		return date.get(Calendar.YEAR);
	}

	/* 扩展 */

	/**
	 * 按给定格式和设备默认时区解析日期时间，沿用宽松解析与允许尾随文本的行为。
	 *
	 * @param value 日期时间文本。
	 * @param format SimpleDateFormat 日期时间格式。
	 * @return 解析得到的日期时间。
	 * @throws IllegalArgumentException 日期时间无法解析或格式无效。
	 */
	@SimpleFunction
	public static Calendar 文本到时间(String value, String format) {
		return 文本到时间(value, format, TimeZone.getDefault().getID());
	}

	/**
	 * 按给定格式和时区解析日期时间文本。
	 *
	 * <p>沿用宽松解析及允许尾随文本的行为，不作为严格日期校验器。</p>
	 *
	 * @param value 日期时间文本，例如 2026-10-16 12:30:45。
	 * @param format SimpleDateFormat 格式，例如 yyyy-MM-dd HH:mm:ss。
	 * @param timeZoneId 时区标识；不识别的标识回退为 GMT。
	 * @return 指定时区的日期时间。
	 * @throws IllegalArgumentException 无法解析文本或格式模式无效。
	 */
	@SimpleFunction
	public static Calendar 文本到时间(String value, String format, String timeZoneId) {
		Calendar calendar = new GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
		try {
			DateFormat dateFormat = new SimpleDateFormat(format);
			dateFormat.setTimeZone(calendar.getTimeZone());
			calendar.setTime(dateFormat.parse(value));
			return calendar;
		} catch (ParseException e) {
			// 注意这里仅捕获解析异常，其它异常直接抛出。
			throw new IllegalArgumentException("无法解析日期时间文本");
		}
	}

	/**
	 * 按固定时长单位计算结束时间减去开始时间的差值，不修改输入。
	 *
	 * <p>沿用年按 365 天、月按 30 天、星期按 7 天、天按 24 小时的折算。
	 * 结果不是日历年月差或跨过的日期边界数；不足一个单位的部分向零截断，结束时间较早时可返回负数。</p>
	 *
	 * @param beginDate 开始时间。
	 * @param endDate 结束时间。
	 * @param timeUnit 时间单位，参见 {@link #日期_年}、{@link #日期_月}、{@link #日期_星期} 等常量。
	 * @return 结束时间减去开始时间后按指定单位折算的整数值。
	 * @throws IllegalArgumentException 时间单位不受支持。
	 */
	@SimpleFunction
	public static long 取时间间隔(Calendar beginDate, Calendar endDate, int timeUnit) {
		// 先计算实际经过的毫秒数，再沿用固定时长的整数折算。
		long time = endDate.getTimeInMillis() - beginDate.getTimeInMillis();
		switch (timeUnit) {
			default:
				throw new IllegalArgumentException("错误的时间单位");
			case 日期_年:
				return time / 1000 / 60 / 60 / 24 / 365;
			case 日期_月:
				return time / 1000 / 60 / 60 / 24 / 30;
			case 日期_星期:
				return time / 1000 / 60 / 60 / 24 / 7;
			case 日期_天:
				return time / 1000 / 60 / 60 / 24;
			case 日期_时:
				return time / 1000 / 60 / 60;
			case 日期_分:
				return time / 1000 / 60;
			case 日期_秒:
				return time / 1000;
		}
	}

	/**
	 * 将 Unix 毫秒时间戳转换为设备默认时区的日期时间。
	 *
	 * @param timestamp 自 1970-01-01T00:00:00Z 起经过的毫秒数。
	 * @return 设备默认时区的日期时间；数值为 0 时表示 Unix 纪元。
	 */
	@SimpleFunction
	public static Calendar 时间戳到时间(long timestamp) {
		Calendar date = new GregorianCalendar();
		date.setTimeInMillis(timestamp);
		return date;
	}

	/**
	 * 将指定日期时间转换为 Unix 毫秒时间戳。
	 *
	 * @param date 日期时间。
	 * @return 自 1970-01-01T00:00:00Z 起经过的毫秒数，不受显示时区影响。
	 */
	@SimpleFunction
	public static long 到时间戳(Calendar date) {
		return date.getTimeInMillis();
	}

	/**
	 * 读取设备默认时区标识。
	 *
	 * @return 时区标识，例如 Asia/Shanghai。
	 */
	@SimpleFunction
	public static String 取时区() {
		return new GregorianCalendar().getTimeZone().getID();
	}

	/**
	 * 读取当前时刻在指定时区下的日期时间。
	 *
	 * @param timeZoneId 时区标识；不识别的标识回退为 GMT。
	 * @return 指定时区的当前日期时间，不修改设备时区。
	 */
	@SimpleFunction
	public static Calendar 取时区时间(String timeZoneId) {
		// 其实 Calendar 对象内部是个时间戳
		return new GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
	}

	/**
	 * 读取日期对象自身的时区标识。
	 *
	 * @param date 日期时间。
	 * @return 时区标识。
	 */
	@SimpleFunction
	public static String 取时间时区(Calendar date) {
		return date.getTimeZone().getID();
	}

	/**
	 * 设置日期对象自身的显示时区，保持其时间戳不变。
	 *
	 * @param date 需要修改的日期时间。
	 * @param timeZoneId 时区标识；不识别的标识回退为 GMT。
	 */
	@SimpleFunction
	public static void 置时间时区(Calendar date, String timeZoneId) {
		date.setTimeZone(TimeZone.getTimeZone(timeZoneId));
	}

	/**
	 * 按设备默认时区取得当前日期，以连字符分隔年月日。
	 *
	 * @return yyyy-MM-dd 格式的当前日期文本。
	 */
	@SimpleFunction
	public static String 取现行日期文本() {
		return 取现行日期文本("-");
	}

	/**
	 * 按设备默认时区生成当前日期文本。
	 *
	 * @param separator 年、月、日之间的文本分隔符，例如 - 或 /；空文本表示不分隔。
	 * @return 依次包含四位年份、两位月份和两位日的文本。
	 */
	@SimpleFunction
	public static String 取现行日期文本(String separator) {
		return 格式化时间(取现行时间(), "yyyy-MM-dd").replace("-", separator);
	}

	/**
	 * 按设备默认时区取得当前时间，以冒号分隔时分秒。
	 *
	 * @return HH:mm:ss 格式的当前时间文本，使用 24 小时制。
	 */
	@SimpleFunction
	public static String 取现行时间文本() {
		return 取现行时间文本(":");
	}

	/**
	 * 按设备默认时区生成当前时间文本。
	 *
	 * @param separator 时、分、秒之间的文本分隔符，例如 :；空文本表示不分隔。
	 * @return 依次包含两位小时、两位分钟和两位秒的文本，使用 24 小时制。
	 */
	@SimpleFunction
	public static String 取现行时间文本(String separator) {
		return 格式化时间(取现行时间(), "HH:mm:ss").replace(":", separator);
	}

	/**
	 * 将约定长度的时间戳文本转换为设备默认时区的日期时间文本。
	 *
	 * <p>仅此便捷接口按文本长度区分单位：10 位按秒，13 位按毫秒。
	 * 需要处理其他长度或指定显示格式时，使用 时间戳到时间 与 格式化时间。</p>
	 *
	 * @param timestamp 10 位秒时间戳文本或 13 位毫秒时间戳文本。
	 * @return yyyy-MM-dd HH:mm:ss 文本；空引用、空文本或其他长度返回空文本。
	 * @throws NumberFormatException 长度为 10 或 13，但不能解析为长整数。
	 */
	@SimpleFunction
	public static String 时间戳到时间文本(String timestamp) {
		if (timestamp == null || (timestamp.length() != 10 && timestamp.length() != 13)) {
			return "";
		}

		// 保留秒、毫秒文本的单位约定，实际格式化仍复用日期对象自身的时区。
		long milliseconds = Long.parseLong(timestamp);
		if (timestamp.length() == 10) {
			milliseconds *= 1000;
		}

		return 格式化时间(时间戳到时间(milliseconds), "yyyy-MM-dd HH:mm:ss");
	}
}
