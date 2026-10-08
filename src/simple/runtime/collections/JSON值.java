package simple.runtime.collections;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;
import simple.runtime.variants.Variant;

import java.util.ArrayList;
import java.util.Iterator;

import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * JSON对象。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class JSON值 {
	/*
	SDK文档：
	https://developer.android.google.cn/reference/org/json/package-summary
	*/

	@SimpleDataElement
	public static final int 值类型_空 = 0;
	@SimpleDataElement
	public static final int 值类型_对象 = 1;
	@SimpleDataElement
	public static final int 值类型_数组 = 2;

	//
	private Object object;

	/**
	 * 构造方法，Simple创建方法。
	 */
	public JSON值() {
		创建空();
	}

	/**
	 * 构造方法，仅供类库使用。
	 *
	 * @param type JSON值类型，可先值请参考{@link JSON值#值类型_空}等。
	 */
	public JSON值(int type) {
		switch (type) {
			case 值类型_对象:
				创建对象();
				break;
			case 值类型_数组:
				创建数组();
				break;
			default:
				创建空();
				break;
		}
	}

	/**
	 * 构造方法，仅供类库使用。
	 *
	 * @param text JSON文本
	 */
	public JSON值(String text) {
		try {
			object = new JSONObject(text);
		} catch (JSONException e1) {
			try {
				object = new JSONArray(text);
			} catch (JSONException e2) {
				throw new IllegalArgumentException("无法解析JSON文本");
			}
		}
	}

	/**
	 * 构造方法，仅供类库使用。
	 *
	 * @param object JSON对象实例。
	 */
	public JSON值(Object object) {
		this.object = object;
	}

	/**
	 * 取内部对象，仅供类库使用。
	 *
	 * @return 内部对象
	 */
	public Object getObject() {
		return object;
	}

	/**
	 * 创建JSON空。
	 */
	@SimpleFunction
	public void 创建空() {
		object = JSONObject.NULL;
	}

	/**
	 * 创建JSON对象。
	 */
	@SimpleFunction
	public void 创建对象() {
		object = new JSONObject();
	}

	/**
	 * 创建JSON数组。
	 */
	@SimpleFunction
	public void 创建数组() {
		object = new JSONArray();
	}

	/**
	 * 解析JSON文本。
	 *
	 * @param JSON文本 JSON文本
	 * @return 成功返回{@code true}，失败返回{@code false}
	 */
	@SimpleFunction
	public boolean 解析(String JSON文本) {
		try {
			object = new JSONObject(JSON文本);
		} catch (JSONException e1) {
			try {
				object = new JSONArray(JSON文本);
			} catch (JSONException e2) {
				return false;
			}
		}
		return true;
	}

	/**
	 * 解析JSON对象文本。
	 *
	 * @param JSON文本 JSON对象文本
	 * @return 成功返回{@code true}，失败返回{@code false}
	 */
	@SimpleFunction
	public boolean 解析对象(String JSON文本) {
		try {
			object = new JSONObject(JSON文本);
		} catch (JSONException e) {
			return false;
		}
		return true;
	}

	/**
	 * 解析JSON数组文本。
	 *
	 * @param JSON文本 JSON数组文本
	 * @return 成功返回{@code true}，失败返回{@code false}
	 */
	@SimpleFunction
	public boolean 解析数组(String JSON文本) {
		try {
			object = new JSONArray(JSON文本);
		} catch (JSONException e) {
			return false;
		}
		return true;
	}

	/**
	 * 将当前JSON对象值或JSON数组值格式到文本。
	 *
	 * @param indentSpaces 缩进空格数
	 * @return 成功返回格式化文本，否则返回{@code null}
	 */
	@SimpleFunction
	public String 到文本(int indentSpaces) {
		try {
			if (object instanceof JSONObject) {
				return ((JSONObject) object).toString(indentSpaces);
			} else if (object instanceof JSONArray) {
				return ((JSONArray) object).toString(indentSpaces);
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return null;
	}

	/**
	 * 取属性或成员数目。
	 *
	 * @return 数目
	 */
	@SimpleProperty
	public int 计数() {
		if (object instanceof JSONObject) {
			return ((JSONObject) object).length();
		} else if (object instanceof JSONArray) {
			return ((JSONArray) object).length();
		}
		return 0;
	}

	/**
	 * 删除指定属性或成员。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 删除(Variant item) {
		if (object instanceof JSONObject) {
			((JSONObject) object).remove(item.getString());
			return true;
		} else if (object instanceof JSONArray) {
			((JSONArray) object).remove(item.getInteger());
			return true;
		}
		return false;
	}

	/**
	 * 取JSON对象值所有属性名。
	 *
	 * @return 成功返回所有属性名，否则返回{@code null}
	 */
	@SimpleFunction
	public String[] 取所有属性名() {
		if (object instanceof JSONObject) {
			ArrayList<String> list = new ArrayList<String>();
			Iterator<String> iter = ((JSONObject) object).keys();
			while (iter.hasNext()) {
				list.add(iter.next());
			}
			return list.toArray(new String[0]);
		}
		return null;
	}

	/**
	 * 检验指定属性或成员的是否存在。
	 *
	 * @param item 属性名或成员索引
	 * @return 存在返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 是否存在(Variant item) {
		if (object instanceof JSONObject) {
			return ((JSONObject) object).has(item.getString());
		} else if (object instanceof JSONArray) {
			return item.getInteger() >= 0 && item.getInteger() < ((JSONArray) object).length();
		}
		return false;
	}

	/**
	 * 检验指定属性或成员的值是否为对象，或检验当前JOSN值。
	 *
	 * @param item 属性名或成员索引，为{@code null}将检验当前JOSN值
	 * @return 是返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 是否为对象(Variant item) {
		if (item != null) {
			Object value = null;
			try {
				if (object instanceof JSONObject) {
					value = ((JSONObject) object).get(item.getString());
				} else if (object instanceof JSONArray) {
					value = ((JSONArray) object).get(item.getInteger());
				}
			} catch (JSONException e) {
				//e.printStackTrace();
			}
			return value instanceof JSONObject;
		} else {
			return object instanceof JSONObject;
		}
	}

	/**
	 * 检验指定属性或成员的值是否为数组，或检验当前JOSN值。
	 *
	 * @param item 属性名或成员索引，为{@code null}将检验当前JOSN值
	 * @return 是返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 是否为数组(Variant item) {
		if (item != null) {
			Object value = null;
			try {
				if (object instanceof JSONObject) {
					value = ((JSONObject) object).get(item.getString());
				} else if (object instanceof JSONArray) {
					value = ((JSONArray) object).get(item.getInteger());
				}
			} catch (JSONException e) {
				//e.printStackTrace();
			}
			return value instanceof JSONArray;
		} else {
			return object instanceof JSONArray;
		}
	}

	/**
	 * 检验指定属性或成员的值是否为空，或检验当前JOSN值。
	 *
	 * @param item 属性名或成员索引，为{@code null}将检验当前JOSN值
	 * @return 是返回{@code false}，否则返回{@code true}
	 */
	@SimpleFunction
	public boolean 是否为空(Variant item) {
		if (item != null) {
			if (object instanceof JSONObject) {
				return ((JSONObject) object).isNull(item.getString());
			} else if (object instanceof JSONArray) {
				return ((JSONArray) object).isNull(item.getInteger());
			}
			return false;
		} else {
			return object == JSONObject.NULL;
		}
	}

	/**
	 * 取指定属性或成员的对象值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回JSON值实例，否则返回{@code null}
	 */
	@SimpleFunction
	public JSON值 取对象(Variant item) {
		if (object instanceof JSONObject) {
			JSONObject value = ((JSONObject) object).optJSONObject(item.getString());
			return new JSON值(value);
		} else if (object instanceof JSONArray) {
			JSONObject value = ((JSONArray) object).optJSONObject(item.getInteger());
			return new JSON值(value);
		}
		return null;
	}

	/**
	 * 取指定属性或成员的数组值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回{@code JSON值}实例，否则返回{@code null}
	 */
	@SimpleFunction
	public JSON值 取数组(Variant item) {
		if (object instanceof JSONObject) {
			JSONArray value = ((JSONObject) object).optJSONArray(item.getString());
			return new JSON值(value);
		} else if (object instanceof JSONArray) {
			JSONArray value = ((JSONArray) object).optJSONArray(item.getInteger());
			return new JSON值(value);
		}
		return null;
	}

	/**
	 * 取指定属性或成员的文本值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回文本值，否则返回空文本
	 */
	@SimpleFunction
	public String 取文本(Variant item) {
		if (object instanceof JSONObject) {
			return ((JSONObject) object).optString(item.getString());
		} else if (object instanceof JSONArray) {
			return ((JSONArray) object).optString(item.getInteger());
		}
		return "";
	}

	/**
	 * 取指定属性或成员的双精度小数值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回双精度小数值，否则返回{@code 0}
	 */
	@SimpleFunction
	public double 取双精度小数(Variant item) {
		if (object instanceof JSONObject) {
			return ((JSONObject) object).optDouble(item.getString());
		} else if (object instanceof JSONArray) {
			return ((JSONArray) object).optDouble(item.getInteger());
		}
		return 0;
	}

	/**
	 * 取指定属性或成员的长整数值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回长整数值，否则返回空{@code 0}
	 */
	@SimpleFunction
	public long 取长整数(Variant item) {
		if (object instanceof JSONObject) {
			return ((JSONObject) object).optLong(item.getString());
		} else if (object instanceof JSONArray) {
			return ((JSONArray) object).optLong(item.getInteger());
		}
		return 0;
	}

	/**
	 * 取指定属性或成员的整数值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回整数值，否则返回{@code 0}
	 */
	@SimpleFunction
	public int 取整数(Variant item) {
		if (object instanceof JSONObject) {
			return ((JSONObject) object).optInt(item.getString());
		} else if (object instanceof JSONArray) {
			return ((JSONArray) object).optInt(item.getInteger());
		}
		return 0;
	}

	/**
	 * 取指定属性或成员的逻辑值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回逻辑值，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 取逻辑(Variant item) {
		if (object instanceof JSONObject) {
			return ((JSONObject) object).optBoolean(item.getString());
		} else if (object instanceof JSONArray) {
			return ((JSONArray) object).optBoolean(item.getInteger());
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的对象值。
	 *
	 * @param item 属性名或成员索引
	 * @param value JSON对象值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置对象(Variant item, JSON值 value) {
		if (value.getObject() instanceof JSONObject) {
			try {
				if (object instanceof JSONObject) {
					((JSONObject) object).put(item.getString(), value.getObject());
					return true;
				} else if (object instanceof JSONArray) {
					((JSONArray) object).put(item.getInteger(), value.getObject());
					return true;
				}
			} catch (JSONException e) {
				//e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的数组值。
	 *
	 * @param item 属性名或成员索引
	 * @param value JSON数组值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置数组(Variant item, JSON值 value) {
		if (value.getObject() instanceof JSONArray) {
			try {
				if (object instanceof JSONObject) {
					((JSONObject) object).put(item.getString(), value.getObject());
					return true;
				} else if (object instanceof JSONArray) {
					((JSONArray) object).put(item.getInteger(), value.getObject());
					return true;
				}
			} catch (JSONException e) {
				//e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的文本值。
	 *
	 * @param item 属性名或成员索引
	 * @param value 文本值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置文本(Variant item, String value) {
		try {
			if (object instanceof JSONObject) {
				((JSONObject) object).put(item.getString(), value);
				return true;
			} else if (object instanceof JSONArray) {
				((JSONArray) object).put(item.getInteger(), value);
				return true;
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的双精度小数值。
	 *
	 * @param item 属性名或成员索引
	 * @param value 双精度小数值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置双精度小数(Variant item, double value) {
		try {
			if (object instanceof JSONObject) {
				((JSONObject) object).put(item.getString(), value);
				return true;
			} else if (object instanceof JSONArray) {
				((JSONArray) object).put(item.getInteger(), value);
				return true;
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的长整数值。
	 *
	 * @param item 属性名或成员索引
	 * @param value 长整数值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置长整数(Variant item, long value) {
		try {
			if (object instanceof JSONObject) {
				((JSONObject) object).put(item.getString(), value);
				return true;
			} else if (object instanceof JSONArray) {
				((JSONArray) object).put(item.getInteger(), value);
				return true;
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的整数值。
	 *
	 * @param item 属性名或成员索引
	 * @param value 整数值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置整数(Variant item, int value) {
		try {
			if (object instanceof JSONObject) {
				((JSONObject) object).put(item.getString(), value);
				return true;
			} else if (object instanceof JSONArray) {
				((JSONArray) object).put(item.getInteger(), value);
				return true;
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的逻辑值。
	 *
	 * @param item 属性名或成员索引
	 * @param value 逻辑值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置逻辑(Variant item, boolean value) {
		try {
			if (object instanceof JSONObject) {
				((JSONObject) object).put(item.getString(), value);
				return true;
			} else if (object instanceof JSONArray) {
				((JSONArray) object).put(item.getInteger(), value);
				return true;
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * 设置指定属性或成员的空值。
	 *
	 * @param item 属性名或成员索引
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 置空(Variant item) {
		try {
			if (object instanceof JSONObject) {
				((JSONObject) object).put(item.getString(), JSONObject.NULL);
				return true;
			} else if (object instanceof JSONArray) {
				((JSONArray) object).put(item.getInteger(), JSONObject.NULL);
				return true;
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * 将JSON对象值添加到数组。
	 *
	 * @param value JSON对象值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入对象(JSON值 value) {
		if (object instanceof JSONArray && value.getObject() instanceof JSONObject) {
			((JSONArray) object).put(value.getObject());
			return true;
		}
		return false;
	}

	/**
	 * 将JSON数组值添加到数组。
	 *
	 * @param value JSON数组值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入数组(JSON值 value) {
		if (object instanceof JSONArray && value.getObject() instanceof JSONArray) {
			((JSONArray) object).put(value.getObject());
			return true;
		}
		return false;
	}

	/**
	 * 将文本值添加到数组。
	 *
	 * @param value 文本值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入文本(String value) {
		if (object instanceof JSONArray) {
			((JSONArray) object).put(value);
			return true;
		}
		return false;
	}

	/**
	 * 将双精度小数值添加到数组。
	 *
	 * @param value 双精度小数值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入双精度小数(double value) {
		try {
			if (object instanceof JSONArray) {
				((JSONArray) object).put(value);
				return true;
			}
		} catch (JSONException e) {
			//e.printStackTrace();
		}
		return false;
	}

	/**
	 * 将长整数值添加到数组。
	 *
	 * @param value 长整数值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入长整数(long value) {
		if (object instanceof JSONArray) {
			((JSONArray) object).put(value);
			return true;
		}
		return false;
	}

	/**
	 * 将整数值添加到数组。
	 *
	 * @param value 整数值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入整数(int value) {
		if (object instanceof JSONArray) {
			((JSONArray) object).put(value);
			return true;
		}
		return false;
	}

	/**
	 * 将逻辑值添加到数组。
	 *
	 * @param value 逻辑值
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入逻辑(boolean value) {
		if (object instanceof JSONArray) {
			((JSONArray) object).put(value);
			return true;
		}
		return false;
	}

	/**
	 * 将空值添加到数组。
	 *
	 * @return 成功返回{@code true}，否则返回{@code false}
	 */
	@SimpleFunction
	public boolean 加入空() {
		if (object instanceof JSONArray) {
			((JSONArray) object).put(JSONObject.NULL);
			return true;
		}
		return false;
	}
}
