package simple.runtime;

import simple.runtime.android.MainActivity;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.components.impl.android.util.AssetsUtil;

import java.io.IOException;
import java.io.InputStream;

/**
 * 资产（assets）相关函数。
 *
 * @author 树先生 xhwsd@qq.con
 */
@SimpleObject
public final class 资产操作 {

	/**
	 * 静态方法集，不创建实例。
	 */
	private 资产操作() {
	}

	/**
	 * 检验指定资产文件或非空目录是否存在。
	 *
	 * @param fileName 资产文件名，格式为{@code 文件名}。
	 * @return 存在返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	public static boolean 资产是否存在(String fileName) {
		try {
			// list 对文件和缺失路径均可能返回空数组，不能单凭未抛异常判断存在。
			if (AssetsUtil.getAssetsList(fileName).length > 0) {
				return true;
			}
			try (InputStream stream = MainActivity.getContext().getAssets().open(fileName)) {
				return true;
			}
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * 检验指定资产路径的资产是否为目录。
	 *
	 * <p>注意对于空目录不精确。
	 *
	 * @param fileName 资产文件名，格式为{@code 文件名}。
	 * @return 是目录返回{@code true}，否则返回{@code false}。
	 */
	@SimpleFunction
	public static boolean 资产是否为目录(String fileName) {
		try {
			// 这里并不精确，如果目录正好没子文件了？
			return AssetsUtil.getAssetsList(fileName).length > 0;
		} catch (IOException e) {
			return false;
		}
	}
}
