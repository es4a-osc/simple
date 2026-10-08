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

package simple.runtime.events;

import simple.runtime.errors.运行错误;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Iterator;

/**
 * 将事件调度到每个对象事件处理方法。
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
public class EventDispatcher {

	/*
	 * Simple事件处理程序将数据成员与定义数据成员的类中的事件处理程序相关联。
	 * 我们使用反射的原因是, 我们不必跟踪对数据成员的分配。
	 */
	private final static class EventHandlerClosure {
		// 事件组件归属单元对象；定义数据成员的对象实例
		private final Object object;

		// 事件组件字段；数据成员与相关联的事件处理程序
		private Field dataMember;

		// 事件组件方法；其实就是单元对象的一个成员方法；事件处理函数与数据成员相关
		private Method eventHandler;

		/**
		 * 创建一个新的数据对象/事件处理程序闭包。
		 *
		 * @param object  事件组件归属单元对象；定义数据成员的实例
		 * @param dataMemberName  事件组件的变量名；与事件处理程序关联的数据成员的名称
		 * @param eventName  事件组件的事件名；与数据成员关联的事件处理程序的名称
		 */
		EventHandlerClosure(Object object, String dataMemberName, String eventName) {
			this.object = object;

			// 查找事件处理程序方法（不关心参数列表 - 在编译时检查编译器 - 这样我们就不必传播信息）
			// 取单元对象类
			Class<?> cls = object.getClass();
			// 遍历单元对象所有方法
			for (Method method : cls.getMethods()) {
				// 取当前方法名 =  组件名$事件名
				if (method.getName().equals(dataMemberName + '$' + eventName)) {
					eventHandler = method;
					break;
				}
			}

			// 查找数据成员
			// 取事件对象所在单元的成员变量字段。
			try {
				dataMember = cls.getField(dataMemberName);
			} catch (NoSuchFieldException ignored) {
			}
		}

		/**
		 * 获取由数据成员持有的当前对象实例。
		 *
		 * @return  数据成员的内容或 {@code null}
		 */
		Object getDataMemberObject() {
			if (dataMember == null) {
				return null;
			}
			try {
				// 取这个成员变量字段的对象。
				return dataMember.get(object);
			} catch (IllegalAccessException ignored) {
				// 不应发生，因为所有Simple数据成员都是公共的
				return null;
			}
		}

		/**
		 * 触发事件处理方法。
		 *
		 * @param args  实际事件处理程序参数
		 */
		void invokeEvent(Object...args) {
			try {
				// 调用组件事件，其实就是调用单元对象的方法
				eventHandler.invoke(object, args);
			} catch (Throwable t) {
				运行错误.convertToRuntimeError(t);
			}
		}
	}

	// 将事件处理程序名称映射到对象/数据成员/事件处理程序闭包
	private static Map<String, List<EventHandlerClosure>> registry = new HashMap<String, List<EventHandlerClosure>>();

	// xhwsd@qq.com 2021-6-10 事件连接表
	private static Map<Object, Object> linkObject = new HashMap<Object, Object>();

	// 当前事件对象
	private static Object eventObject;

	private EventDispatcher() {
	}

	/**
	 * 注册对象实例及其数据成员/事件处理程序关联以进行事件分派。从对象实例的构造函数调用。
	 *
	 * @param object  事件组件归属单元对象；对象实例定义数据成员和事件处理程序
	 * @param dataMemberName  事件组件的变量名；与事件处理程序关联的数据成员的名称
	 * @param eventName  事件组件的事件名；与数据成员关联的事件的名称
	 */
	public static void registerEvent(Object object, String dataMemberName, String eventName) {
		// registry -> k= 事件名 v=事件处理程序闭合...
		List<EventHandlerClosure> closures = registry.get(eventName);
		if (closures == null) {
			closures = new ArrayList<EventHandlerClosure>();
			registry.put(eventName, closures);
		}

		// 事件名一样的放到一个列表中
		closures.add(new EventHandlerClosure(object, dataMemberName, eventName));
	}

	/**
	 * 将基于事件名称的事件调度到任何已注册的处理方法。
	 *
	 * @param that  触发事件对象（必须匹配数据成员值）
	 * @param eventName  引发的名称事件
	 * @param args  事件处理程序的参数
	 */
	public static synchronized void dispatchEvent(Object that, String eventName, Object...args) {
		// xhwsd@qq.com 2021-6-10 支持取当前事件对象和触发转义事件
		// 保存当前触发事件对象
		eventObject = that;

		// 将事件对象重置为连接对象
		if (that != null) {
			Object object = linkObject.get(that);
			if (object != null) {
				that = object;
			}
		}

		// 先从事件名为键的哈希码中取事件处理列表
		List<EventHandlerClosure> closures = registry.get(eventName);
		if (closures != null) {
			// 然后遍历这个数据处理列表
			for (EventHandlerClosure closure : closures.toArray(new EventHandlerClosure[closures.size()])) {
				// 成员数据对象 = 事件对象
				// 注意这里，无论该组件在哪个单元，只要是这个对象就会触发事件。
				if (closure.getDataMemberObject() == that) {
					// 调用该事件
					closure.invokeEvent(args);
				}
			}
		}

		// 已经分发完事件，并且取事件组件(getEventObject)仅可在事件过程中使用。
		// 所以这里释放掉
		eventObject = null;
	}

	/**
	 * 将原对象事件转移到另一个对象触发
	 *
	 * @param source 被转移事件的组件对象
	 * @param target 触发事件的组件对象
	 */
	public static synchronized void transferEvent(Object source, Object target) {
		linkObject.put(source, target);
	}

	/**
	 * 注销对象所有关联的事件。
	 *
	 * xhwsd@qq.com 2021-6-10
	 *
	 * @param that 事件对象
	 */
	public static void unregisterEvent(Object that) {
		// 注销该对象的所有事件
		Iterator<String> iterator = registry.keySet().iterator();
		while (iterator.hasNext()) {
			List<EventHandlerClosure> closures = registry.get(iterator.next());
			if (closures != null) {
				// 注意因删除操作，需递减遍历
				for (int i = closures.size() - 1; i >= 0; i--) {
					if (closures.get(i).getDataMemberObject() == that) {
						closures.remove(i);
					}
				}
			}
		}

		// 注销该对象转移事件
		if (linkObject.containsKey(that)) {
			linkObject.remove(that);
		}

		// 注销该对象接收事件，
		Collection<Object> values = linkObject.values();
		while (values.contains(that)) {
			// 注意这里删除操作会同步哈希码
			values.remove(that);
		}
	}

	/**
	 * 取本次触发事件的对象
	 *
	 * <p>注意仅在事件过程中调用有效。
	 *
	 * @return 事件对象
	 */
	public static Object getEventObject() {
		return eventObject;
	}
}
