package simple.runtime.testing;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/** 为编译器测试提供与保留词同名的外部成员，不进入产品运行库。 */
@SimpleObject
public final class KeywordMemberObject {
	@SimpleDataElement
	public int 结束;
	@SimpleDataElement
	public int 真;
	@SimpleDataElement
	public int 假;
	@SimpleDataElement
	public static final int 空 = 7;
	@SimpleDataElement
	public KeywordMemberObject 下级;
	private int value;

	@SimpleProperty
	public int 设置() {
		return value;
	}

	@SimpleProperty
	public void 设置(int value) {
		this.value = value;
	}

	@SimpleFunction
	public String 匹配(String input) {
		return input;
	}

	@SimpleFunction
	public String 匹配() {
		return "default";
	}

	@SimpleFunction
	public static int 执行() {
		return 8;
	}
}
