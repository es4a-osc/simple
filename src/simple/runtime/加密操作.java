package simple.runtime;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * 提供 MD5 摘要、RC4、DES 和 Authcode 加解密。
 *
 * <p>保留既有协议的数据格式和失败返回约定。RC4、DES 用于旧协议互通，
 * 不提供现代加密算法的安全保证；Base64 等编码操作由编码转换提供。</p>
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class 加密操作 {

	private static final Charset GBK = Charset.forName("GBK");
	private static final Charset UTF8 = Charset.forName("UTF-8");
	private static final Charset ASCII = Charset.forName("US-ASCII");
	private static final char[] HEX = "0123456789ABCDEF".toCharArray();
	private static final String AUTHCODE_PREFIX_CHARS = "abcdefghjklmnopqrstuvwxyz0123456789";

	/**
	 * 静态方法集，不创建实例。
	 */
	private 加密操作() {
	}

	/* 消息摘要 */

	/**
	 * 计算字节数组的 MD5 摘要，不修改输入数据。
	 *
	 * @param bytes 原始字节数组。
	 * @return 32 位大写十六进制摘要；空引用或计算失败返回空文本。
	 */
	@SimpleFunction
	public static String 取MD5值(byte[] bytes) {
		try {
			return hex(MessageDigest.getInstance("MD5").digest(bytes));
		} catch (Exception exception) {
			exception.printStackTrace();
			return "";
		}
	}

	/* RC4 文本加解密 */

	/**
	 * 将明文和密钥按 GBK 编码后执行 RC4。
	 *
	 * @param text 明文；GBK 无法表示的字符沿用字符集的替换规则。
	 * @param key 文本密钥，按 GBK 编码，不得为空文本。
	 * @return 大写十六进制密文；空引用、无效密钥或处理失败返回空文本。
	 */
	@SimpleFunction
	public static String RC4加密(String text, String key) {
		if (text == null || key == null) {
			return "";
		}

		try {
			return hex(rc4(text.getBytes(GBK), key.getBytes(GBK)));
		} catch (Exception exception) {
			exception.printStackTrace();
			return "";
		}
	}

	/**
	 * 解析十六进制密文，执行 RC4 后按 GBK 还原文本。
	 *
	 * @param text 十六进制密文，不含分隔符；奇数长度的最后一位沿用原规则忽略。
	 * @param key 加密时使用的文本密钥，按 GBK 编码。
	 * @return 明文；空引用、无效密钥或处理失败返回空文本。此格式不验证密钥或密文完整性。
	 */
	@SimpleFunction
	public static String RC4解密(String text, String key) {
		if (text == null || key == null) {
			return "";
		}

		try {
			return new String(rc4(parseHex(text), key.getBytes(GBK)), GBK);
		} catch (Exception exception) {
			exception.printStackTrace();
			return "";
		}
	}

	/* RC4 字节集接口 */

	/**
	 * 对原始字节执行 RC4，并将密文转换为大写十六进制字符的字节数组。
	 *
	 * <p>返回长度是原始数据的两倍，不是原始密文字节，不能直接交给 RC4解密2；
	 * 先将这些十六进制字符还原为密文字节，才能使用字节集解密接口。</p>
	 *
	 * @param data 原始字节数组，不修改输入。
	 * @param key 文本密钥，按 GBK 编码。
	 * @return 十六进制密文的 ASCII 字节数组；空引用或处理失败返回空数组。
	 */
	@SimpleFunction
	public static byte[] RC4加密2(byte[] data, String key) {
		if (data == null || key == null) {
			return new byte[0];
		}

		try {
			return hex(rc4(data, key.getBytes(GBK))).getBytes(ASCII);
		} catch (Exception exception) {
			exception.printStackTrace();
			return new byte[0];
		}
	}

	/**
	 * 对原始密文字节执行 RC4，不先解析十六进制字符。
	 *
	 * <p>与 RC4加密2 的输出格式不对称；直接传入 RC4加密2 的返回值不能还原原文。</p>
	 *
	 * @param data 原始密文字节数组，不是十六进制文本的字节数组。
	 * @param key 加密时使用的文本密钥，按 GBK 编码。
	 * @return 解密字节数组；空引用或处理失败返回空数组。不验证密文完整性。
	 */
	@SimpleFunction
	public static byte[] RC4解密2(byte[] data, String key) {
		if (data == null || key == null) {
			return new byte[0];
		}

		try {
			return rc4(data, key.getBytes(GBK));
		} catch (Exception exception) {
			exception.printStackTrace();
			return new byte[0];
		}
	}

	/* DES 文本加解密 */

	/**
	 * 按 DES/CBC/PKCS5Padding 加密 GBK 文本。
	 *
	 * <p>沿用固定 IV：字节 1、2、3、4、5、6、7、8，仅用于既有格式互通。</p>
	 *
	 * @param text 明文，按 GBK 编码。
	 * @param key 密钥，GBK 编码后必须为 8 字节。
	 * @return 标准 Base64 密文；空引用、无效密钥或处理失败返回空文本。
	 */
	@SimpleFunction
	public static String DES加密(String text, String key) {
		try {
			byte[] encrypted = des(Cipher.ENCRYPT_MODE, text.getBytes(GBK), key);
			return 编码转换.Base64编码(encrypted);
		} catch (Exception exception) {
			return "";
		}
	}

	/**
	 * 解码 Base64 密文，使用固定 IV 执行 DES 后按 GBK 还原文本。
	 *
	 * @param text 标准 Base64 密文。
	 * @param key 加密时的密钥，GBK 编码后必须为 8 字节。
	 * @return 明文；空引用、无效密钥、填充错误或处理失败返回空文本。不验证密文完整性。
	 */
	@SimpleFunction
	public static String DES解密(String text, String key) {
		try {
			byte[] encrypted = 编码转换.decodeLegacyBase64(text);
			return new String(des(Cipher.DECRYPT_MODE, encrypted, key), GBK);
		} catch (Exception exception) {
			return "";
		}
	}

	/* Authcode 文本加解密 */

	/**
	 * 按既有 Authcode 格式封装 UTF-8 文本。
	 *
	 * <p>密文由四位随机前缀和标准 Base64 内容组成，内部组合 MD5 与 RC4。
	 * 保留十位零前缀，不增加有效期参数，也不升级原算法。</p>
	 *
	 * @param text 明文，按 UTF-8 编码。
	 * @param key 文本密钥，允许空文本。
	 * @return Authcode 密文；空引用或处理失败返回空文本。同一明文的密文可以不同。
	 */
	@SimpleFunction
	public static String Authcode加密(String text, String key) {
		if (text == null || key == null) {
			return "";
		}

		try {
			String digest = md5Lower(key);
			String keyA = md5Lower(digest.substring(0, 16));
			String keyB = md5Lower(digest.substring(16));
			String prefix = authcodePrefix();
			String cipherKey = keyA + md5Lower(keyA + prefix);

			// 校验片段放在明文之前；四位随机前缀参与派生本次 RC4 密钥。
			String payload = "0000000000" + md5Lower(text + keyB).substring(0, 16) + text;
			byte[] encrypted = rc4(payload.getBytes(UTF8), cipherKey.getBytes(UTF8));
			return prefix + 编码转换.Base64编码(encrypted);
		} catch (Exception exception) {
			return "";
		}
	}

	/**
	 * 解析既有 Authcode 密文并核对其中的 MD5 校验片段。
	 *
	 * <p>不将十位前缀解释为有效期。校验失败沿用文本“2”的返回约定；
	 * 因此返回“2”也可能是合法明文，调用方不能仅据此区分成功与失败。</p>
	 *
	 * @param text 四位前缀和 Base64 内容组成的密文。
	 * @param key 加密时的文本密钥，允许空文本。
	 * @return 明文；校验失败返回文本“2”，空引用或解析异常返回空文本。
	 */
	@SimpleFunction
	public static String Authcode解密(String text, String key) {
		if (text == null || key == null) {
			return "";
		}

		try {
			String digest = md5Lower(key);
			String keyA = md5Lower(digest.substring(0, 16));
			String keyB = md5Lower(digest.substring(16));
			String prefix = cut(text, 0, 4);
			byte[] cipherKey = (keyA + md5Lower(keyA + prefix)).getBytes(UTF8);
			String encoded = cut(text, 4, text.length());

			// 保留无补位、补一位、补两位的尝试顺序，不更换原有校验算法。
			for (String padding : new String[] {"", "=", "=="}) {
				byte[] encrypted = 编码转换.decodeLegacyBase64(encoded + padding);
				String payload = new String(rc4(encrypted, cipherKey), UTF8);
				String content = cut(payload, 26, payload.length());

				if (cut(payload, 10, 16).equals(md5Lower(content + keyB).substring(0, 16))) {
					return content;
				}
			}

			return "2";
		} catch (Exception exception) {
			return "";
		}
	}

	/* 内部算法与格式辅助 */

	/**
	 * 根据密钥初始化 RC4 状态并处理数据，每次调用使用独立状态。
	 *
	 * @param data 原始数据，不修改输入。
	 * @param key 密钥字节，非空数据要求密钥非空。
	 * @return 与输入等长的新字节数组。
	 */
	private static byte[] rc4(byte[] data, byte[] key) {
		byte[] result = new byte[data.length];
		if (data.length == 0) {
			return result;
		}

		if (key.length == 0) {
			throw new IllegalArgumentException("RC4 密钥不能为空。");
		}

		// 密钥调度只操作本地状态表，不修改调用方的数据或密钥。
		int[] state = new int[256];

		for (int index = 0; index < state.length; index++) {
			state[index] = index;
		}

		int second = 0;

		for (int index = 0; index < state.length; index++) {
			second = (second + state[index] + (key[index % key.length] & 255)) & 255;
			int previous = state[index];
			state[index] = state[second];
			state[second] = previous;
		}

		// 每个输入字节与生成的密钥流异或；加密和解密使用同一核心过程。
		int first = 0;
		second = 0;
		for (int index = 0; index < data.length; index++) {
			first = (first + 1) & 255;
			second = (second + state[first]) & 255;
			int previous = state[first];
			state[first] = state[second];
			state[second] = previous;
			result[index] = (byte) (data[index] ^ state[(state[first] + state[second]) & 255]);
		}

		return result;
	}

	/**
	 * 执行既有 DES 模式，编码转换由调用方负责。
	 *
	 * @param mode Cipher 的加密或解密模式。
	 * @param data 需要处理的字节。
	 * @param key GBK 文本密钥。
	 * @return 加密或解密后的字节。
	 * @throws Exception 密钥、填充或密码提供程序处理失败。
	 */
	private static byte[] des(int mode, byte[] data, String key) throws Exception {
		byte[] iv = {1, 2, 3, 4, 5, 6, 7, 8};
		SecretKeySpec secret = new SecretKeySpec(key.getBytes(GBK), "DES");
		Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");

		cipher.init(mode, secret, new IvParameterSpec(iv));
		return cipher.doFinal(data);
	}

	/**
	 * 将字节格式化为大写十六进制文本。
	 *
	 * @param data 字节数组。
	 * @return 每字节对应两个十六进制字符的文本。
	 */
	private static String hex(byte[] data) {
		char[] result = new char[data.length * 2];

		for (int index = 0; index < data.length; index++) {
			int value = data[index] & 255;
			result[index * 2] = HEX[value >>> 4];
			result[index * 2 + 1] = HEX[value & 15];
		}

		return new String(result);
	}

	/**
	 * 按原格式解析完整字符对，忽略奇数长度的最后一位。
	 *
	 * @param text 十六进制文本。
	 * @return 密文字节；无效字符返回空数组。
	 */
	private static byte[] parseHex(String text) {
		byte[] result = new byte[text.length() / 2];
		try {
			for (int index = 0; index < result.length; index++) {
				int high = Byte.decode("0x" + text.charAt(index * 2));
				int low = Byte.decode("0x" + text.charAt(index * 2 + 1));
				result[index] = (byte) ((high << 4) ^ low);
			}

			return result;
		} catch (Exception exception) {
			exception.printStackTrace();
			return new byte[0];
		}
	}

	/**
	 * 计算 Authcode 派生密钥及校验片段使用的小写 MD5。
	 *
	 * @param text UTF-8 文本。
	 * @return 32 位小写十六进制摘要。
	 * @throws Exception 摘要算法不可用。
	 */
	private static String md5Lower(String text) throws Exception {
		return hex(MessageDigest.getInstance("MD5").digest(text.getBytes(UTF8))).toLowerCase(Locale.ROOT);
	}

	/**
	 * 按原字符表生成四位随机前缀，不改变既有格式。
	 *
	 * @return 不含字母 i 的小写字母及数字前缀。
	 */
	private static String authcodePrefix() {
		Random random = new Random();
		StringBuilder result = new StringBuilder(4);

		for (int index = 0; index < 4; index++) {
			result.append(AUTHCODE_PREFIX_CHARS.charAt(random.nextInt(AUTHCODE_PREFIX_CHARS.length())));
		}

		return result.toString();
	}

	/**
	 * 截取协议中的可用字符，超出末尾时返回空文本。
	 *
	 * @param text 原文本。
	 * @param start 非负起点。
	 * @param length 期望长度。
	 * @return 截取结果。
	 */
	private static String cut(String text, int start, int length) {
		return start >= text.length() ? "" : text.substring(start, start + Math.min(length, text.length() - start));
	}
}
