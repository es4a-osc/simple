package simple.compiler;

import java.io.File;
import junit.framework.TestCase;
import simple.runtime.ApplicationFunctions;
import simple.runtime.events.EventDispatcher;

/**
 * 验证宿主终止契约、事件拥有者释放和实际活动及服务示例。
 *
 * @author 树先生 xhwsd@qq.com
 */
public final class MainActivityContractTest extends TestCase {

	/**
	 * 用两个事件源模拟窗口持有的组件，并记录事件派发次数。
	 */
	public static final class Owner {
		public final Object first = new Object();
		public final Object second = new Object();
		public int received;

		/**
		 * 创建独立事件拥有者。
		 */
		public Owner() {
		}

		/**
		 * 记录第一个组件的事件。
		 */
		public void first$activityProbe() {
			received++;
		}

		/**
		 * 记录第二个组件的事件。
		 */
		public void second$activityProbe() {
			received++;
		}
	}

	/**
	 * 普通 Activity.finish 与显式终止应用必须具有不同的运行库入口。
	 *
	 * @throws Exception 内部终止方法不存在
	 */
	public void testExplicitTerminationContract() throws Exception {
		assertEquals(void.class, ApplicationFunctions.class.getMethod("terminate").getReturnType());
	}

	/**
	 * 窗口释放时移除自己拥有的全部组件事件，不影响其它窗口或服务的事件。
	 */
	public void testUnregisterOwnerPreservesOtherOwners() {
		Owner removed = new Owner();
		Owner retained = new Owner();
		EventDispatcher.registerEvent(removed, "first", "activityProbe");
		EventDispatcher.registerEvent(removed, "second", "activityProbe");
		EventDispatcher.registerEvent(retained, "first", "activityProbe");
		try {
			EventDispatcher.dispatchEvent(removed.first, "activityProbe");
			assertEquals(1, removed.received);
			EventDispatcher.unregisterEvent(removed);
			EventDispatcher.dispatchEvent(removed.first, "activityProbe");
			EventDispatcher.dispatchEvent(removed.second, "activityProbe");
			EventDispatcher.dispatchEvent(retained.first, "activityProbe");
			assertEquals(1, removed.received);
			assertEquals(1, retained.received);
		} finally {
			EventDispatcher.unregisterEvent(removed);
			EventDispatcher.unregisterEvent(retained);
		}
	}

	/**
	 * 单独销毁一个组件不能注销同一窗口其它组件的事件。
	 */
	public void testUnregisterSourcePreservesSiblingEvents() {
		Owner owner = new Owner();
		EventDispatcher.registerEvent(owner, "first", "activityProbe");
		EventDispatcher.registerEvent(owner, "second", "activityProbe");
		try {
			EventDispatcher.unregisterEvent(owner.first);
			EventDispatcher.dispatchEvent(owner.first, "activityProbe");
			EventDispatcher.dispatchEvent(owner.second, "activityProbe");
			assertEquals(1, owner.received);
		} finally {
			EventDispatcher.unregisterEvent(owner);
		}
	}

	/**
	 * 完整应用设置、后台服务和定时任务示例均应继续通过当前编译器。
	 *
	 * @throws Exception 项目加载或编译失败
	 */
	public void testExistingActivityAndServiceExamplesCompile() throws Exception {
		for (String name : new String[]{"应用操作", "服务", "定时任务"}) {
			assertNotNull(Compiler.compileForUnitTesting(new Project(new File("examples/" + name + "/project.properties"))));
		}
	}
}
