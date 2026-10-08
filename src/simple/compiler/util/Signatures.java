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

package simple.compiler.util;

/**
 * 用于处理类型名称和签名的辅助方法。
 *
 * @author Herbert Czymontek
 */
public final class Signatures {

	private Signatures() {
	}

	/**
	 * 从复合名中提取包名（假设包名是复合名的最后一个组件）。
	 *
	 * @param name 复合名
	 * @param separator 分隔符
	 */
	private static String getPackageName(String name, char separator) {
		int index = name.lastIndexOf(separator);
		return index < 0 ? "" : name.substring(0, index);
	}

	/**
	 * 从复合名中提取类名（假设类名是复合名的最后一个组成部分）。
	 *
	 * @param name 复合名
	 * @param separator 分隔符
	 */
	private static String getClassName(String name, char separator) {
		int index = name.lastIndexOf(separator);
		return index < 0 ? name : name.substring(index + 1);
	}

	/**
	 * 返回点（.）限定类名称的包名部分。
	 *
	 * <p>{@code my.app.Main -> my.app}
	 *
	 * @param qualifiedName  限定类名称
	 * @return  包名称
	 */
	public static String getPackageName(String qualifiedName) {
		return getPackageName(qualifiedName, '.');
	}

	/**
	 * 返回点（.）限定类名的类名部分。
	 *
	 * <p>{@code my.app.Main -> Main}
	 *
	 * @param qualifiedName  限定类名称
	 * @return  类名称
	 */
	public static String getClassName(String qualifiedName) {
		return getClassName(qualifiedName, '.');
	}

	/**
	 * 返回内部名的包名部分（根据JavaVM规范）。
	 *
	 * <p>{@code my/app/Main -> my/app}
	 *
	 * @param internalName  JavaVM样式内部名
	 * @return  包名
	 */
	public static String getInternalPackageName(String internalName) {
		return getPackageName(internalName, '/');
	}

	/**
	 * 返回内部名的类名部分（根据JavaVM规范）。
	 *
	 * <p>{@code my/app/Main -> Main}
	 *
	 * @param internalName  JavaVM样式内部名
	 * @return  类名
	 */
	public static String getInternalClassName(String internalName) {
		return getClassName(internalName, '/');
	}
}
