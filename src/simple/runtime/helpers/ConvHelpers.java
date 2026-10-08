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

package simple.runtime.helpers;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Calendar;

import simple.runtime.errors.转换错误;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.ByteVariant;
import simple.runtime.variants.DateVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.LongVariant;
import simple.runtime.variants.ObjectVariant;
import simple.runtime.variants.ShortVariant;
import simple.runtime.variants.SingleVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.UninitializedVariant;
import simple.runtime.variants.Variant;

/**
 * Helper methods for doing runtime value conversions from one type to another.
 *
 * @author Herbert Czymontek
 */
public final class ConvHelpers {

	private ConvHelpers() {  // COV_NF_LINE
	}                        // COV_NF_LINE

	/**
	 * Converts a Boolean value to an Integer value.
	 *
	 * @param b  Boolean value
	 * @return  Integer value
	 */
	public static int boolean2integer(boolean b) {
		return b ? -1 : 0;
	}

	/**
	 * Converts a Boolean value to a Long value.
	 *
	 * @param b  Boolean value
	 * @return  Long value
	 */
	public static long boolean2long(boolean b) {
		return b ? -1L : 0L;
	}

	/**
	 * Converts a Boolean value to a Single value.
	 *
	 * @param b  Boolean value
	 * @return  Single value
	 */
	public static float boolean2single(boolean b) {
		return b ? -1F : 0F;
	}

	/**
	 * Converts a Boolean value to a Double value.
	 *
	 * @param b  Boolean value
	 * @return  Double value
	 */
	public static double boolean2double(boolean b) {
		return b ? -1D : 0D;
	}

	/**
	 * Converts a Boolean value to a String value.
	 *
	 * @param b  Boolean value
	 * @return  String value
	 */
	public static String boolean2string(boolean b) {
		return b ? "真" : "假";
	}

	/**
	 * Converts a Byte value to an Integer value.
	 *
	 * @param b  Byte value
	 * @return  Integer value
	 */
	public static int byte2integer(byte b) {
		return (b << 24) >> 24;
	}

	/**
	 * Converts a Short value to a Byte value.
	 *
	 * @param s  Short value
	 * @return  Byte value
	 */
	public static byte short2byte(short s) {
		return (byte) ((s << 24) >> 24);
	}

	/**
	 * Converts an Integer value to a Boolean value.
	 *
	 * @param i  Integer value
	 * @return  Boolean value
	 */
	public static boolean integer2boolean(int i) {
		return i != 0;
	}

	/**
	 * Converts an Integer value to a Byte value.
	 *
	 * @param i  Integer value
	 * @return  Byte value
	 */
	public static byte integer2byte(int i) {
		return (byte) ((i << 24) >> 24);
	}

	/**
	 * Converts an Integer value to a Short value.
	 *
	 * @param i  Integer value
	 * @return  Short value
	 */
	public static short integer2short(int i) {
		return (short) ((i << 16) >> 16);
	}

	/**
	 * Converts a Long value to a Boolean value.
	 *
	 * @param l  Long value
	 * @return  Boolean value
	 */
	public static boolean long2boolean(long l) {
		return l != 0;
	}

	/**
	 * Converts a Long value to a Byte value.
	 *
	 * @param l  Long value
	 * @return  Byte value
	 */
	public static byte long2byte(long l) {
		return (byte) ((l << 56) >> 56);
	}

	/**
	 * Converts a Long value to a Short value.
	 *
	 * @param l  Long value
	 * @return  Short value
	 */
	public static short long2short(long l) {
		return (short) ((l << 48) >> 48);
	}

	/**
	 * Converts a Single value to a Boolean value.
	 *
	 * @param f  Single value
	 * @return  Boolean value
	 */
	public static boolean single2boolean(float f) {
		return f != 0;
	}

	/**
	 * Converts a Single value to a Byte value.
	 *
	 * @param f  Single value
	 * @return  Byte value
	 */
	public static byte single2byte(float f) {
		return (byte) (((long) f << 56) >> 56);
	}

	/**
	 * Converts a Single value to a Short value.
	 *
	 * @param f  Single value
	 * @return  Short value
	 */
	public static short single2short(float f) {
		return (short) (((long) f << 48) >> 48);
	}

	/**
	 * Converts a Double value to a Boolean value.
	 *
	 * @param d  Double value
	 * @return  Boolean value
	 */
	public static boolean double2boolean(double d) {
		return d != 0;
	}

	/**
	 * Converts a Double value to a Byte value.
	 *
	 * @param d  Double value
	 * @return  Byte value
	 */
	public static byte double2byte(double d) {
		return (byte) (((long) d << 56) >> 56);
	}

	/**
	 * Converts a Double value to a Short value.
	 *
	 * @param d  Double value
	 * @return  Short value
	 */
	public static short double2short(double d) {
		return (short) (((long) d << 48) >> 48);
	}

	/**
	 * Converts a String value to a Boolean value.
	 *
	 * @param s  String value
	 * @return  Boolean value
	 */
	public static boolean string2boolean(String s) {
		s = s.trim();
		if (s.equals("真")) {
			return true;
		} else if (s.equals("假")) {
			return false;
		} else {
			return string2double(s) != 0;
		}
	}

	/**
	 * Converts a String value to a Byte value.
	 *
	 * @param s  String value
	 * @return  Byte value
	 */
	public static byte string2byte(String s) {
		return double2byte(string2double(s));
	}

	/**
	 * Converts a String value to a Short value.
	 *
	 * @param s  String value
	 * @return  Short value
	 */
	public static short string2short(String s) {
		return double2short(string2double(s));
	}

	/**
	 * Converts a String value to an Integer value.
	 *
	 * @param s  String value
	 * @return  Integer value
	 */
	public static int string2integer(String s) {
		return (int) string2double(s);
	}

	/**
	 * Converts a String value to a Long value.
	 *
	 * @param s  String value
	 * @return  Long value
	 */
	public static long string2long(String s) {
		try {
			return Long.parseLong(s);
		} catch (NumberFormatException nfe) {
			// TODO: this behaves inconsistently when compared with the way string2integer works
			throw new 转换错误();
		}
	}

	/**
	 * Converts a String value to a Single value.
	 *
	 * @param s  String value
	 * @return  Single value
	 */
	public static float string2single(String s) {
		return (float) string2double(s);
	}

	/**
	 * Converts a String value to a Double value.
	 *
	 * @param s  String value
	 * @return  Double value
	 */
	public static double string2double(String s) {
		try {
			return Double.parseDouble(s);
		} catch (NumberFormatException nfe) {
			throw new 转换错误();
		}
	}

	/** 自定义扩展 **/

	/**
	 * 将数组对象转为对象列表
	 *
	 * @param o 数组对象
	 * @return 对象列表
	 */
	public static ArrayList<Object> object2list(Object o) {
		int length = Array.getLength(o);
		ArrayList<Object> list = new ArrayList<Object>();
		for (int i = 0; i < length; i++) {
			list.add(Array.get(o, i));
		}
		return list;
	}

	/**
	 * 将对象换为变体
	 *
	 * @param o 对象
	 * @return 变体
	 */
	public static Variant object2variant(Object o) {
		if (o == null) {
			return null;
		}

		Class<?> t = o.getClass();
		if (t == Boolean.class) {
			return BooleanVariant.getBooleanVariant(((Boolean) o).booleanValue());
		} else if (t == Byte.class) {
			return ByteVariant.getByteVariant(integer2byte(((Byte) o).intValue()));
		} else if (t == Short.class) {
			return ShortVariant.getShortVariant(integer2short(((Short) o).intValue()));
		} else if (t == Integer.class) {
			return IntegerVariant.getIntegerVariant(((Integer) o).intValue());
		} else if (t == Long.class) {
			return LongVariant.getLongVariant(((Long) o).longValue());
		} else if (t == Float.class) {
			return SingleVariant.getSingleVariant(((Float) o).floatValue());
		} else if (t == Double.class) {
			return DoubleVariant.getDoubleVariant(((Double) o).doubleValue());
		}  else if (t == String.class) {
			return StringVariant.getStringVariant((String) o);
		}  else if (o instanceof Calendar) {
			return DateVariant.getDateVariant((Calendar) o);
		} else if (t.isArray()) {
			return ArrayVariant.getArrayVariant(o);
		} else if (t == Variant.class) {
			return (Variant) o;
		}
		return ObjectVariant.getObjectVariant(o);
	}

	/**
	 * 将变体转为对象数组。
	 *
	 * @param v 变体
	 * @return 对象数组
	 */
	public static Object[] variant2objects(Variant v) {
		if (v instanceof ArrayVariant) {
			Object[] objects = object2objects(v.getArray());
			for (int i = 0; i < objects.length; i++) {
				if (objects[i] instanceof Variant) {
					objects[i] = variant2object((Variant) objects[i]);
				}
			}
			return objects;
		} else if (v != null) {
			return new Object[]{variant2object(v)};
		}
		return null;
	}

	/**
	 * 将数组对象转为对象数组
	 *
	 * @param o 数组对象
	 * @return 对象数组
	 */
	public static Object[] object2objects(Object o) {
		Object[] objects = new Object[Array.getLength(o)];
		for (int i = 0; i < objects.length; i++) {
			objects[i] = Array.get(o, i);
		}
		return objects;
	}

	/**
	 * 将变体转为对象
	 *
	 * @param v 变体
	 * @return 对象
	 */
	public static Object variant2object(Variant v) {
		if (v == null) {
			return null;
		} else if (v instanceof ArrayVariant) {
			return v.getArray();
		} else if (v instanceof BooleanVariant) {
			return Boolean.valueOf(v.getBoolean());
		} else if (v instanceof ByteVariant) {
			return Byte.valueOf(v.getByte());
		} else if (v instanceof ShortVariant) {
			return Short.valueOf(v.getShort());
		} else if (v instanceof IntegerVariant) {
			return Integer.valueOf(v.getInteger());
		} else if (v instanceof LongVariant) {
			return Long.valueOf(v.getLong());
		} else if (v instanceof SingleVariant) {
			return Float.valueOf(v.getSingle());
		} else if (v instanceof DoubleVariant) {
			return Double.valueOf(v.getDouble());
		} else if (v instanceof StringVariant) {
			return v.getString();
		} else if (v instanceof DateVariant) {
			return v.getDate();
		} else if (v instanceof UninitializedVariant) {
			return null;
		}
		return v.getObject();
	}
}
