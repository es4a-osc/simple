package simple.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * 使用真实 Java 子进程验证双管道排空、末尾诊断和取消后的线程回收。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class ExecutionTest {

	/**
	 * 大量输出超过系统管道容量时，捕获和丢弃都必须正常结束；返回前保留末尾中文诊断。
	 *
	 * @throws Exception 子进程启动或输出读取失败。
	 */
	@Test(timeout = 15000)
	public void testDrainCapturedAndDiscardedOutput() throws Exception {
		ByteArrayOutputStream standard = new ByteArrayOutputStream();
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		PrintStream out = new PrintStream(standard, true, "UTF-8");
		PrintStream err = new PrintStream(errors, true, "UTF-8");
		assertTrue(Execution.execute(null, command("flood"), out, err, StandardCharsets.UTF_8));
		assertTrue(standard.toString("UTF-8").endsWith("普通输出结束" + System.lineSeparator()));
		assertTrue(errors.toString("UTF-8").endsWith("错误输出结束" + System.lineSeparator()));
		assertTrue(Execution.execute(null, command("flood"), null, null, StandardCharsets.UTF_8));
	}

	/**
	 * 不提供输入的工具收到 EOF；失败退出保留真实错误，启动失败写入指定错误流。
	 *
	 * @throws Exception 子进程启动或输出读取失败。
	 */
	@Test(timeout = 15000)
	public void testInputEofAndFailures() throws Exception {
		ByteArrayOutputStream errors = new ByteArrayOutputStream();
		PrintStream err = new PrintStream(errors, true, "UTF-8");
		assertTrue(Execution.execute(null, command("eof"), null, err, StandardCharsets.UTF_8));
		assertFalse(Execution.execute(null, command("fail"), null, err, StandardCharsets.UTF_8));
		assertTrue(errors.toString("UTF-8").contains("真实失败诊断"));
		assertFalse(Execution.execute(null, new String[] {"missing-es4a-test-command"}, null, err));
		assertTrue(errors.toString("UTF-8").contains("工具执行失败"));
	}

	/**
	 * 取消等待中的工具后，执行线程应及时结束，保留中断标志且不再异步追加输出。
	 *
	 * @throws Exception 探针启动、线程等待或输出读取失败。
	 */
	@Test(timeout = 15000)
	public void testInterruptedExecutionFinishesReaders() throws Exception {
		final CountDownLatch ready = new CountDownLatch(1);
		final AtomicBoolean result = new AtomicBoolean(true);
		final AtomicBoolean interrupted = new AtomicBoolean(false);
		final ByteArrayOutputStream bytes = new ByteArrayOutputStream() {
			@Override
			public synchronized void write(byte[] data, int offset, int length) {
				super.write(data, offset, length);
				if (new String(toByteArray(), StandardCharsets.UTF_8).contains("ready")) {
					ready.countDown();
				}
			}
		};
		final PrintStream messages = new PrintStream(bytes, true, "UTF-8");
		Thread worker = new Thread(new Runnable() {
			@Override
			public void run() {
				result.set(Execution.execute(null, command("wait"), messages, messages, StandardCharsets.UTF_8));
				interrupted.set(Thread.currentThread().isInterrupted());
			}
		});
		worker.start();
		try {
			assertTrue(ready.await(5, TimeUnit.SECONDS));
		} finally {
			worker.interrupt();
			worker.join(5000);
		}
		assertFalse(worker.isAlive());
		assertFalse(result.get());
		assertTrue(interrupted.get());
		assertTrue(bytes.toString("UTF-8").contains("工具执行已中断"));
	}

	/**
	 * 沿用测试进程的实际 JDK 和类路径，不依赖 PATH 或额外脚本解释器。
	 *
	 * @param mode 子进程探针行为。
	 * @return 可直接传入工具执行器的命令。
	 */
	private static String[] command(String mode) {
		return new String[] {new File(System.getProperty("java.home"), "bin/java").toString(),
				"-Dfile.encoding=UTF-8", "-cp", System.getProperty("java.class.path"), Probe.class.getName(), mode};
	}

	/**
	 * 产生超过管道容量的输出，或模拟需要 EOF、失败和等待的真实工具进程。
	 *
	 * @author 树先生 xhwsd@qq.com
	 */
	public static final class Probe {

		/**
		 * 禁止实例化进程探针。
		 */
		private Probe() {
		}

		/**
		 * 按模式产生可验证的输出或退出状态。
		 *
		 * @param arguments 探针模式。
		 * @throws Exception 标准输入无法读取或等待被中断。
		 */
		public static void main(String[] arguments) throws Exception {
			if ("flood".equals(arguments[0])) {
				for (int index = 0; index < 20000; index++) {
					System.out.println("普通工具输出：" + index);
					System.err.println("错误工具输出：" + index);
				}
				System.out.print("普通输出结束");
				System.err.print("错误输出结束");
			} else if ("eof".equals(arguments[0])) {
				System.exit(System.in.read() == -1 ? 0 : 2);
			} else if ("fail".equals(arguments[0])) {
				System.err.print("真实失败诊断");
				System.exit(7);
			} else {
				System.out.println("ready");
				Thread.sleep(60000);
			}
		}
	}
}
