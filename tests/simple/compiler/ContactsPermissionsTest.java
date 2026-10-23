package simple.compiler;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import junit.framework.TestCase;
import simple.compiler.parser.Parser;
import simple.compiler.scanner.Scanner;

/** 从实际示例验证新增通讯录的权限收集，不给未使用通讯录的项目增加权限。 */
public final class ContactsPermissionsTest extends TestCase {
	private Set<?> permissions(String path) throws Exception {
		ByteArrayOutputStream diagnostics = new ByteArrayOutputStream();
		PrintStream messages = new PrintStream(diagnostics, true, "UTF-8");
		Compiler compiler = new Compiler(Compiler.Platform.Android, messages, messages);
		for (Project.SourceDescriptor descriptor : new Project(path, null).getSources()) {
			String source = new String(Files.readAllBytes(descriptor.getFile().toPath()), StandardCharsets.UTF_8);
			new Parser(compiler, new Scanner(compiler, source), descriptor.getQualifiedName()).parse();
		}
		if (compiler.getErrorCount() == 0) compiler.resolve();
		assertEquals(diagnostics.toString("UTF-8"), 0, compiler.getErrorCount());
		Method method = Compiler.class.getDeclaredMethod("generatePermissions");
		method.setAccessible(true);
		return (Set<?>) method.invoke(compiler);
	}

	public void testContactsExampleRequestsOnlyContactsPermissions() throws Exception {
		assertEquals(new HashSet<String>(Arrays.asList("android.permission.READ_CONTACTS", "android.permission.WRITE_CONTACTS")),
			permissions("examples/通讯录/project.properties"));
	}

	public void testLabelExampleDoesNotRequestContactsPermissions() throws Exception {
		Set<?> permissions = permissions("examples/标签/project.properties");
		assertFalse(permissions.contains("android.permission.READ_CONTACTS"));
		assertFalse(permissions.contains("android.permission.WRITE_CONTACTS"));
	}
}
