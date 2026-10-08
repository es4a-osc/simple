package simple.runtime;

import java.security.MessageDigest;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 加密相关函数。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 加密操作 {

	@SimpleFunction
	public static String 取MD5值(byte[] bytes) {
		char[] hexDigits = {
				'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
				'A', 'B', 'C', 'D', 'E', 'F' };
		try {
			MessageDigest mdInst = MessageDigest.getInstance("MD5");
			mdInst.update(bytes);
			byte[] md = mdInst.digest();
			int j = md.length;
			char[] str = new char[j * 2];
			int k = 0;
			for (int i = 0; i < j; i++) {
				byte byte0 = md[i];
				str[k++] = hexDigits[byte0 >>> 4 & 0xF];
				str[k++] = hexDigits[byte0 & 0xF];
			}
			return new String(str);
		} catch (Exception e) {
			e.printStackTrace();
			return "";
		}
	}
}
