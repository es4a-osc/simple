package simple;

/**
 * 临时测试类
 *
 * @author 树先生 xhwsd@qq.com
 */
public class MyDebug {

		public static final int 常量3 = 100;
	// Exception in thread "main" java.lang.Error: Unresolved compilation problem:
		public static final int 常量4 = MyDebug.常量3 + 2;

	public static void main(String[] args) {
		System.out.println(MyDebug.常量4);
		//TestClass test = new TestClass();
		//test.test2();
	}


	public static class TestClass {

		public static void test1() {
			System.out.println("test1");
			//test2();
		}

		public void test2() {
			System.out.println("test2");
		}
	}
}
