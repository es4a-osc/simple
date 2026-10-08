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

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * 实现各种与日期和时间相关的运行时功能。
 *
 * @author Herbert Czymontek
 */
@SimpleObject
public final class 日期时间操作 {

	/* 日期/时间间隔类型 */
	@SimpleDataElement
	public static final int 日期_年 = Calendar.YEAR;
	@SimpleDataElement
	public static final int 日期_月 = Calendar.MONTH;
	@SimpleDataElement
	public static final int 日期_天 = Calendar.DATE;
	@SimpleDataElement
	public static final int 日期_星期 = Calendar.WEEK_OF_YEAR;
	@SimpleDataElement
	public static final int 日期_时 = Calendar.HOUR_OF_DAY;
	@SimpleDataElement
	public static final int 日期_分 = Calendar.MINUTE;
	@SimpleDataElement
	public static final int 日期_秒 = Calendar.SECOND;

	/* 一星期中第几天 */
	@SimpleDataElement
	public static final int 星期_星期一 = Calendar.MONDAY;
	@SimpleDataElement
	public static final int 星期_星期二 = Calendar.TUESDAY;
	@SimpleDataElement
	public static final int 星期_星期三 = Calendar.WEDNESDAY;
	@SimpleDataElement
	public static final int 星期_星期四 = Calendar.THURSDAY;
	@SimpleDataElement
	public static final int 星期_星期五 = Calendar.FRIDAY;
	@SimpleDataElement
	public static final int 星期_星期六 = Calendar.SATURDAY;
	@SimpleDataElement
	public static final int 星期_星期天 = Calendar.SUNDAY;

	/* 月份 */
	@SimpleDataElement
	public static final int 月份_一月 = Calendar.JANUARY;
	@SimpleDataElement
	public static final int 月份_二月 = Calendar.FEBRUARY;
	@SimpleDataElement
	public static final int 月份_三月 = Calendar.MARCH;
	@SimpleDataElement
	public static final int 月份_四月 = Calendar.APRIL;
	@SimpleDataElement
	public static final int 月份_五月 = Calendar.MAY;
	@SimpleDataElement
	public static final int 月份_六月 = Calendar.JUNE;
	@SimpleDataElement
	public static final int 月份_七月 = Calendar.JULY;
	@SimpleDataElement
	public static final int 月份_八月 = Calendar.AUGUST;
	@SimpleDataElement
	public static final int 月份_九月 = Calendar.SEPTEMBER;
	@SimpleDataElement
	public static final int 月份_十月 = Calendar.OCTOBER;
	@SimpleDataElement
	public static final int 月份_十一月 = Calendar.NOVEMBER;
	@SimpleDataElement
	public static final int 月份_十二月 = Calendar.DECEMBER;

	/*  时区。 提示：北京时间加上8个小时的时差就是格林平时。 */
	@SimpleDataElement
	public static final String 时区_格林尼治 = "GMT";
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
	 * 在给定日期添加时间间隔。
	 *
	 * @param date  要添加到的日期
	 * @param intervalKind  间隔类型，可选值请参考{@link 时间#日期_年}等常量
	 * @param interval  要添加的日期或时间量
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
	 * 从给定字符串创建日期。
	 *
	 * <p>支持日期时间格式：<br>
	 * {@code yyyy/MM/dd HH:mm:ss}，{@code yyyy/MM/dd/HH:mm:ss}，{@code yyyy/MM/dd/HH/mm/ss}，
	 * {@code yyyy-MM-dd HH:mm:ss}，{@code yyyy-MM-dd-HH:mm:ss}，{@code yyyy-MM-dd-HH-mm-ss}，
	 * {@code yyyy.MM.dd HH:mm:ss}，{@code yyyy.MM.dd.HH:mm:ss}，{@code yyyy.MM.dd.HH.mm.ss}，
	 * {@code yyyy年MM月dd日 HH时mm分ss秒}，{@code yyyy年MM月dd日HH时mm分ss秒}，{@code yyyyMMddHHmmss}，
	 * {@code yyyy/MM/dd}，{@code yyyy-MM-dd}，{@code yyyy.MM.dd}，{@code yyyy年MM月dd日}，{@code yyyyMMdd}。
	 *
	 * <p>其中{@code yyyy}为年（0000-9999），{@code MM}为月（01-12），{@code dd}为日（01-31）,
	 * {@code HH}为时（00-23），{@code mm}为分（00-59）和{@code ss}为秒（00-59）。<br>
	 *
	 * @param value  日期时间文本
	 * @return  日期
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
	 * 返回给定日期时间的日。
	 *
	 * @param date  取日的日期时间
	 * @return  日（1-31）
	 */
	@SimpleFunction
	public static int 取日(Calendar date) {
		return date.get(Calendar.DAY_OF_MONTH);
	}

	/**
	 * 将给定的日期转换为字符串并进行格式化。
	 *
	 * @see SimpleDateFormat
	 *
	 * @param date  欲格式化时间
	 * @param pattern 描述日期时间格式，例如{@code yyyy/MM/dd HH:mm:ss}
	 * @return  格式化后的文本
	 */
	@SimpleFunction
	public static String 格式化时间(Calendar date, String pattern) {
		DateFormat dateFormat = new SimpleDateFormat(pattern);
		// 注意这里传递时区。
		dateFormat.setTimeZone(date.getTimeZone());
		return dateFormat.format(date.getTime());
	}

	/**
	 * 返回给定日期时间的小时。
	 *
	 * @param date  取小时的日期时间
	 * @return  小时（0-23）
	 */
	@SimpleFunction
	public static int 取小时(Calendar date) {
		return date.get(Calendar.HOUR_OF_DAY);
	}

	/**
	 * 返回给定日期时间的分钟。
	 *
	 * @param date  取分钟的日期时间
	 * @return  分钟（0-59）
	 */
	@SimpleFunction
	public static int 取分钟(Calendar date) {
		return date.get(Calendar.MINUTE);
	}

	/**
	 * 返回给定日期时间的月份。
	 *
	 * @param date  取日期时间的月
	 * @return  月份（范围 1-12），可请参考{@link #月份_一月}等常量
	 */
	@SimpleFunction
	public static int 取月份(Calendar date) {
		return date.get(Calendar.MONTH);
	}

	/**
	 * 返回给定日期的月份名称。
	 *
	 * @param date  使用月份的日期
	 * @return  月份名称
	 */
	@SimpleFunction
	public static String 取月份名(Calendar date) {
		return String.format("%1$tB", date);
	}

	/**
	 * 取当前设备的日期时间。
	 *
	 * @return  日期时间
	 */
	@SimpleFunction
	public static Calendar 取现行时间() {
		return new GregorianCalendar();
	}

	/**
	 * 返回给定日期时间的秒。
	 *
	 * @param date  取秒的日期时间
	 * @return  秒（0-59）
	 */
	@SimpleFunction
	public static int 取秒(Calendar date) {
		return date.get(Calendar.SECOND);
	}

	/**
	 * 返回当前系统以毫秒为单位的时间戳（13位）。
	 *
	 * @return  毫秒为单位的时间戳（13位）
	 */
	@SimpleFunction
	public static long 取现行时间戳() {
		return System.currentTimeMillis();
	}

	/**
	 * 返回给定日期时间的星期。
	 *
	 * @param date  取日期时间的星期
	 * @return  星期（1-7），可请参考{@link #星期_星期天}等常量
	 */
	@SimpleFunction
	public static int 取星期(Calendar date) {
		return date.get(Calendar.DAY_OF_WEEK);
	}

	/**
	 * 返回给定日期的星期名称。
	 *
	 * @param date  使用星期的日期
	 * @return  星期名称
	 */
	@SimpleFunction
	public static String 取星期名(Calendar date) {
		return String.format("%1$tA", date);
	}

	/**
	 * 返返回给定日期时间的年。
	 *
	 * @param date  取日期时间的年
	 * @return  年
	 */
	@SimpleFunction
	public static int 取年(Calendar date) {
		return date.get(Calendar.YEAR);
	}

	/** 扩展 **/

	/**
	 * 从给定日期时间文本创建一个指定时区的日期时间。
	 *
	 * @param value 日期时间文本，例如{@code 2008/04/21 09:29:48}。
	 * @param format 日期时间格式，例如{@code yyyy/MM/dd HH:mm:ss}。
	 * @param timeZoneId 时区标识，无效将为{@link 日期时间操作#时区_格林尼治}。
	 * @return 日期时间。
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
			throw new IllegalArgumentException("[时间.文本到时间] 无法解析日期时间文本；日期时间文本=" + value
					+ " 日期时间格式=" + format + " 时区标识=" + timeZoneId);
		}
	}

	/**
	 * 计算两个时间相差值。
	 *
	 * @param beginDate 开始时间。
	 * @param endDate 结束时间。
	 * @param timeUnit 时间单位，可选值请参考{@link 日期时间操作#日期_年}等常量。
	 * @return 两个日期时间相差值。
	 */
	public static long 取时间间隔(Calendar beginDate, Calendar endDate, int timeUnit) {
		// 将2个日期时间转为毫秒数
		long time = endDate.getTimeInMillis() - beginDate.getTimeInMillis();
		switch (timeUnit) {
			default:
				throw new IllegalArgumentException("[时间.取时间间隔] 错误的时间单位；时间单位=" + timeUnit);
			case 日期_年:
				return time / 1000 / 60 / 60 / 24 / 365;
			case 日期_月:
				return time / 1000 / 60 / 60 / 24 / 30;
			case 日期_天:
				return time / 1000 / 60 / 60 / 24;
			case 日期_时:
				return time / 1000 /60 / 60;
			case 日期_分:
				return time / 1000 / 60;
			case 日期_秒:
				return time / 1000;
		}
	}

	/**
	 * 将时间戳转为设备时区日期时间。
	 *
	 * @param timestamp 毫秒时间戳（13位）。
	 * @return 日期时间。
	 */
	@SimpleFunction
	public static Calendar 时间戳到时间(long timestamp) {
		Calendar date = new GregorianCalendar();
		date.setTimeInMillis(timestamp);
		return date;
	}

	/**
	 * 将指点日期时间转为时间戳。
	 *
	 * @param date 日期时间。
	 * @return 时间戳。
	 */
	@SimpleFunction
	public static long 到时间戳(Calendar date) {
		return date.getTimeInMillis();
	}

	/**
	 * 取当前设备的时区。
	 *
	 * @return 时区标识。
	 */
	@SimpleFunction
	public static String 取时区() {
		return new GregorianCalendar().getTimeZone().getID();
	}

	/**
	 * 取指定时区当前设备的日期时间。
	 *
	 * @param timeZoneId 时区标识，无效将为{@link 日期时间操作#时区_格林尼治}。
	 * @return 日期时间。
	 */
	@SimpleFunction
	public static Calendar 取时区时间(String timeZoneId) {
		// 其实 Calendar 对象内部是个时间戳
		return new GregorianCalendar(TimeZone.getTimeZone(timeZoneId));
	}

	/**
	 * 取指定日期时间的时区。
	 *
	 * @param date 日期时间。
	 * @return 时区标识。
	 */
	@SimpleFunction
	public static String 取时间时区(Calendar date) {
		return date.getTimeZone().getID();
	}

	/**
	 * 取指定日期时间的时区。
	 *
	 * @param date 日期时间。
	 * @return 时区标识。
	 */
	@SimpleFunction
	public static void 置时间时区(Calendar date, String timeZoneId) {
		date.setTimeZone(TimeZone.getTimeZone(timeZoneId));
	}
}
