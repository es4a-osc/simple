package simple.runtime.collections;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.helpers.ConvHelpers;
import simple.runtime.variants.ArrayVariant;
import simple.runtime.variants.BooleanVariant;
import simple.runtime.variants.ByteVariant;
import simple.runtime.variants.DateVariant;
import simple.runtime.variants.DoubleVariant;
import simple.runtime.variants.IntegerVariant;
import simple.runtime.variants.LongVariant;
import simple.runtime.variants.ShortVariant;
import simple.runtime.variants.SingleVariant;
import simple.runtime.variants.StringVariant;
import simple.runtime.variants.Variant;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Calendar;

import android.content.Intent;

/**
 * 意图对象。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 意图 {

	// 意图实例
	private Intent intent;

	/**
	 * 构造新{@code Intent}的意图对象。
	 */
	public 意图() {
		intent = new Intent();
	}

	/**
	 * 构造指定{@code Intent}的意图对象。
	 *
	 * @param intent {@code Intent}实例。
	 */
	public 意图(Intent intent) {
		this.intent = intent;
	}

	/**
	 * 取{@code Intent}实例。
	 *
	 * @return {@code Intent}实例。
	 */
	public Intent getIntent() {
		return intent;
	}

	/**
	 * 取意图欲要执行的动作。
	 *
	 * @return 执行的动作。
	 */
	@SimpleFunction
	public String 取动作() {
		return intent.getAction();
	}

	/**
	 * 置意图欲要执行的动作。
	 *
	 * @param action 执行的动作。
	 */
	@SimpleFunction
	public void 置动作(String action) {
		intent.setAction(action);
	}

	/**
	 * 取意图仅限应用包名。
	 *
	 * @return 应用包名。
	 */
	@SimpleFunction
	public String 取包名() {
		return intent.getPackage();
	}

	/**
	 * 置意图仅限应用包名。
	 *
	 * @param packageName 应用包名。
	 */
	@SimpleFunction
	public void 置包名(String packageName) {
		intent.setPackage(packageName);
	}

	/**
	 * 置意图的附加参数。
	 *
	 * @param name 附加参数名。
	 * @param value 附加参数值，支持任意基本数据类型及数组。
	 */
	@SimpleFunction
	public void 置附加(String name, Variant value) {
		if (value == null) {
			// 什么的不需要做
		} else if (value instanceof BooleanVariant) {
			intent.putExtra(name, value.getBoolean());
		} else if (value instanceof ByteVariant) {
			intent.putExtra(name, value.getByte());
		} else if (value instanceof ShortVariant) {
			intent.putExtra(name, value.getShort());
		} else if (value instanceof IntegerVariant) {
			intent.putExtra(name, value.getInteger());
		} else if (value instanceof LongVariant) {
			intent.putExtra(name, value.getLong());
		} else if (value instanceof SingleVariant) {
			intent.putExtra(name, value.getSingle());
		} else if (value instanceof DoubleVariant) {
			intent.putExtra(name, value.getDouble());
		} else if (value instanceof StringVariant) {
			intent.putExtra(name, value.getString());
		} else if (value instanceof DateVariant) {
			intent.putExtra(name, value.getDate());
		} else if (value instanceof ArrayVariant) {
			// 因 intent.putExtra() 有参数类型限制，这里需要从数组变体中取出数组成员
			// 如果使用JDK1.8的方法将导致Android编译需要 dx8.jar
			Object array = value.getArray();
			Class<?> type = array.getClass().getComponentType();
			if (type == Boolean.class) {
				boolean[] values = new boolean[Array.getLength(array)];
				for (int i = 0; i < values.length; i++) {
					values[i] = Array.getBoolean(array, i);
				}
				intent.putExtra(name, values);
			} else if (type == Byte.class) {
				byte[] values = new byte[Array.getLength(array)];
				for (int i = 0; i < values.length; i++) {
					values[i] = Array.getByte(array, i);
				}
				intent.putExtra(name, values);
			} else if (type == Short.class) {
				short[] values = new short[Array.getLength(array)];
				for (int i = 0; i < values.length; i++) {
					values[i] = Array.getShort(array, i);
				}
				intent.putExtra(name, values);
			} else if (type == Integer.class) {
				int[] values = new int[Array.getLength(array)];
				for (int i = 0; i < values.length; i++) {
					values[i] = Array.getInt(array, i);
				}
				intent.putExtra(name, values);
			} else if (type == Long.class) {
				long[] values = new long[Array.getLength(array)];
				for (int i = 0; i < values.length; i++) {
					values[i] = Array.getLong(array, i);
				}
				intent.putExtra(name, values);
			} else if (type == Float.class) {
				float[] values = new float[Array.getLength(array)];
				for (int i = 0; i < values.length; i++) {
					values[i] = Array.getFloat(array, i);
				}
				intent.putExtra(name, values);
			} else if (type == Double.class) {
				double[] values = new double[Array.getLength(array)];
				for (int i = 0; i < values.length; i++) {
					values[i] = Array.getDouble(array, i);
				}
				intent.putExtra(name, values);
			} else if (type == String.class) {
				String[] values = ConvHelpers.object2list(array).toArray(new String[0]);
				intent.putExtra(name, values);
			} else if (Calendar.class.isAssignableFrom(type)) {
				intent.putExtra(name, (Serializable) array);
			} else {
				throw new IllegalArgumentException("意图.置附加 非法参数值");
			}
		} else {
			throw new IllegalArgumentException("意图.置附加 非法参数值");
		}
	}

	/**
	 * 取意图的指定附加参数的值。
	 *
	 * @param name 参数名。
	 * @return 成功返回参数值，否则返回{@code null}。
	 */
	@SimpleFunction
	public Variant 取附加(String name) {
		Object object = intent.getExtras().get(name);
		return ConvHelpers.object2variant(object);
	}
}
