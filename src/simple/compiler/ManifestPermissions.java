package simple.compiler;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;

/**
 * 汇总实际引用对象的权限需求，按名称合并版本范围并约束清单注解的职责边界。
 *
 * @author 树先生 xhwsd@qq.com
 */
final class ManifestPermissions {

	/** 同名权限只保留一项；按名称稳定排序，0 表示不设版本上限。 */
	private final Map<String, Integer> permissions = new TreeMap<String, Integer>();

	/**
	 * 建立本轮清单使用的空权限集合。
	 */
	ManifestPermissions() {
	}

	/**
	 * 追加单项权限需求，最终范围必须覆盖每个已引用对象的需求。
	 *
	 * @param permissionName 单项权限名称。
	 * @param maxSdkVersion 最高 API 等级，0 表示不设上限。
	 * @throws IOException 名称为空、包含列表或空白，或版本上限为负数。
	 */
	void add(String permissionName, int maxSdkVersion) throws IOException {
		String name = permissionName == null ? "" : permissionName.trim();
		if (name.isEmpty() || name.indexOf(',') >= 0 || name.matches("(?s).*\\s+.*")) {
			throw new IOException("权限名称必须是单项非空名称：" + permissionName);
		}
		if (maxSdkVersion < 0) {
			throw new IOException("权限 " + name + " 的 maxSdkVersion 不能为负数：" + maxSdkVersion);
		}

		// 不限版本的需求覆盖所有有限范围；均有限制时取最大的上限，合并与输入顺序无关。
		Integer previous = permissions.get(name);
		int merged = previous == null ? maxSdkVersion
				: previous == 0 || maxSdkVersion == 0 ? 0 : Math.max(previous, maxSdkVersion);
		permissions.put(name, merged);
	}

	/**
	 * 追加批量注解中的权限名称，每项均不设版本上限。
	 *
	 * @param permissionNames 逗号分隔的权限列表；空值表示没有声明。
	 * @throws IOException 列表中存在无效的非空权限名称。
	 */
	void addNames(String permissionNames) throws IOException {
		if (permissionNames != null) {
			for (String name : permissionNames.split(",")) {
				if (!name.trim().isEmpty()) {
					add(name, 0);
				}
			}
		}
	}

	/**
	 * 读取类文件保存的权限注解字段，缺省版本上限与运行库注解保持一致。
	 *
	 * @param annotation 单项或批量注解字段。
	 * @throws IOException 权限名称、版本字段或版本范围无效。
	 */
	void addAnnotation(Map<String, String> annotation) throws IOException {
		if (annotation.containsKey("permissionNames")) {
			addNames(annotation.get("permissionNames"));
		} else {
			try {
				add(annotation.get("permissionName"), Integer.parseInt(annotation.getOrDefault("maxSdkVersion", "0")));
			} catch (NumberFormatException failure) {
				throw new IOException("权限 maxSdkVersion 必须为非负整数：" + annotation, failure);
			}
		}
	}

	/**
	 * 获取只读的权限及其最终版本上限，供清单生成使用。
	 *
	 * @return 按权限名称排序的只读映射。
	 */
	Map<String, Integer> getPermissions() {
		return Collections.unmodifiableMap(permissions);
	}

	/**
	 * 检查清单节点片段，不允许通过通用节点注解绕过权限汇总。
	 *
	 * @param xml 已完成宏替换的 XML 片段。
	 * @throws IOException 片段包含权限节点或不是有效 XML。
	 */
	static void validateManifestNodes(String xml) throws IOException {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(true);
			factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
			factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
			factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
			factory.setXIncludeAware(false);
			String wrapped = "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">" + xml + "</manifest>";
			NodeList nodes = factory.newDocumentBuilder().parse(new ByteArrayInputStream(
					wrapped.getBytes(StandardCharsets.UTF_8))).getElementsByTagName("*");
			for (int index = 0; index < nodes.getLength(); index++) {
				String name = nodes.item(index).getLocalName();
				if ("uses-permission".equals(name) || "uses-permission-sdk-23".equals(name)
						|| "uses-permission-sdk-m".equals(name)) {
					throw new IOException("权限必须通过 @UsesPermissions 或 @UsesPermission 声明，不能放入 @ManifestNodes。");
				}
			}
		} catch (IOException failure) {
			throw failure;
		} catch (Exception failure) {
			throw new IOException("清单注解 XML 无效：" + failure.getMessage(), failure);
		}
	}
}
