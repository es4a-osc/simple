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

package simple.compiler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import junit.framework.TestCase;

/** 验证project.properties中的类库清单宏读取规则。 */
public final class ProjectManifestMacroTest extends TestCase {

	public void testReadsNamespacedMacroAndUsesDefault() throws Exception {
		File projectFile = new File("build/tests/project-manifest-macro/project.properties")
				.getAbsoluteFile();
		File parent = projectFile.getParentFile();
		if (!parent.isDirectory()) {
			assertTrue(parent.mkdirs());
		}

		OutputStreamWriter writer = new OutputStreamWriter(
				new FileOutputStream(projectFile), "UTF-8");
		try {
			writer.write("测试对象.应用标识=project-value\n");
		} finally {
			writer.close();
		}

		try {
			Project project = new Project(projectFile);
			assertEquals("project-value",
					project.getMacroValue("测试对象", "应用标识", "default-value"));
			assertEquals("default-value",
					project.getMacroValue("测试对象", "未配置", "default-value"));
			assertNull(project.getMacroValue("测试对象", "必填值", null));
		} finally {
			assertTrue(projectFile.delete());
		}
	}
}
