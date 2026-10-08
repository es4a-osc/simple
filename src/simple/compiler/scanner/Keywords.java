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

package simple.compiler.scanner;

/**
 * 定义Simple语言的关键字。
 *
 * <p>请注意我们在此处使用自己的哈希实现来实现扫描器的最大性能。
 *
 * @author Herbert Czymontek
 */
final class Keywords {

	/*
	 *  将关键字映射到其对应的令牌。
	 */
	private static class KeywordRecord {
		// 哈希表中的下一个关键字
		private KeywordRecord next;

		//关键字到令牌的映射
		private final String keyword;
		private final TokenKind tokenKind;

		KeywordRecord(String keyword, TokenKind tokenKind) {
			this.keyword = keyword;
			this.tokenKind = tokenKind;
		}
	}

	// 关键字哈希表
	private KeywordRecord[] keywords;

	// 关键字哈希表大小：必须为2的幂。
	private static final int HASHTABLE_SIZE = 1 << 8;

	/**
	 * 初始化关键字哈希表。
	 */
	protected Keywords() {
		keywords = new KeywordRecord[HASHTABLE_SIZE];

		// Simple属性部分关键字
		enterKeyword(TokenKind.TOK_$AS);
		enterKeyword(TokenKind.TOK_$DEFINE);
		enterKeyword(TokenKind.TOK_$END);
		enterKeyword(TokenKind.TOK_$FORM);
		enterKeyword(TokenKind.TOK_$INTERFACE);
		enterKeyword(TokenKind.TOK_$OBJECT);
		enterKeyword(TokenKind.TOK_$PROPERTIES);
		enterKeyword(TokenKind.TOK_$SOURCE);
		// xhwsd@qq.com 2021-6-5 服务
		enterKeyword(TokenKind.TOK_$SERVICE);

		// Simple语言关键字
		enterKeyword(TokenKind.TOK_ALIAS);
		enterKeyword(TokenKind.TOK_AND);
		enterKeyword(TokenKind.TOK_AS);
		enterKeyword(TokenKind.TOK_BOOLEAN);
		enterKeyword(TokenKind.TOK_BYREF);
		enterKeyword(TokenKind.TOK_BYTE);
		enterKeyword(TokenKind.TOK_BYVAL);
		enterKeyword(TokenKind.TOK_CASE);
		enterKeyword(TokenKind.TOK_CONST);
		enterKeyword(TokenKind.TOK_DATE);
		enterKeyword(TokenKind.TOK_DIM);
		enterKeyword(TokenKind.TOK_DO);
		enterKeyword(TokenKind.TOK_DOUBLE);
		enterKeyword(TokenKind.TOK_EACH);
		enterKeyword(TokenKind.TOK_ELSE);
		enterKeyword(TokenKind.TOK_ELSEIF);
		enterKeyword(TokenKind.TOK_END);
		enterKeyword(TokenKind.TOK_ERROR);
		enterKeyword(TokenKind.TOK_EVENT);
		enterKeyword(TokenKind.TOK_EXIT);
		enterKeyword(TokenKind.TOK_FOR);
		enterKeyword(TokenKind.TOK_FUNCTION);
		enterKeyword(TokenKind.TOK_GET);
		enterKeyword(TokenKind.TOK_IF);
		enterKeyword(TokenKind.TOK_IN);
		enterKeyword(TokenKind.TOK_INTEGER);
		enterKeyword(TokenKind.TOK_IS);
		enterKeyword(TokenKind.TOK_ISNOT);
		enterKeyword(TokenKind.TOK_LIKE);
		enterKeyword(TokenKind.TOK_LONG);
		enterKeyword(TokenKind.TOK_ME);
		enterKeyword(TokenKind.TOK_NEXT);
		enterKeyword(TokenKind.TOK_NEW);
		enterKeyword(TokenKind.TOK_NOT);
		enterKeyword(TokenKind.TOK_NOTHING);
		enterKeyword(TokenKind.TOK_OBJECT);
		enterKeyword(TokenKind.TOK_ON);
		enterKeyword(TokenKind.TOK_OR);
		enterKeyword(TokenKind.TOK_PROPERTY);
		enterKeyword(TokenKind.TOK_RAISEEVENT);
		enterKeyword(TokenKind.TOK_SELECT);
		enterKeyword(TokenKind.TOK_SET);
		enterKeyword(TokenKind.TOK_SHORT);
		enterKeyword(TokenKind.TOK_SINGLE);
		enterKeyword(TokenKind.TOK_STATIC);
		enterKeyword(TokenKind.TOK_STEP);
		enterKeyword(TokenKind.TOK_STRING);
		enterKeyword(TokenKind.TOK_SUB);
		enterKeyword(TokenKind.TOK_THEN);
		enterKeyword(TokenKind.TOK_TO);
		enterKeyword(TokenKind.TOK_TYPEOF);
		enterKeyword(TokenKind.TOK_UNTIL);
		enterKeyword(TokenKind.TOK_VARIANT);
		enterKeyword(TokenKind.TOK_WHILE);
		enterKeyword(TokenKind.TOK_XOR);

		// 以下并不是真正的关键字，但是在扫描器中检测相应的文字时，这样定义它们是有帮助的
		//（注意扫描器永远不会返回这些令牌！）
		enterKeyword(TokenKind.TOK_FALSE);
		enterKeyword(TokenKind.TOK_TRUE);
	}

	/*
	 * 计算将要检查的标识符作为关键字的哈希码。
	 */
	private int hashCode(String str) {
		// 这就是为什么哈希表的大小需要是2的幂的原因 - 以便我们可以轻松快速地计算哈希代码。
		return str.hashCode() & (HASHTABLE_SIZE - 1);
	}

	/*
	 * 在关键字哈希表中输入关键字及其关联的令牌。
	 */
	private void enterKeyword(TokenKind tokenKind) {
		String keyword = tokenKind.toString();
		int hash = hashCode(keyword);
		KeywordRecord kr = new KeywordRecord(keyword, tokenKind);
		kr.next = keywords[hash];
		keywords[hash] = kr;
	}

	/**
	 * 检查标识符是否为关键字。如果是这样则返回关键字的令牌，否则返回通用标识符令牌。
	 *
	 * @param identifier  要检查的标识符
	 * @return  标识符的正确令牌
	 */
	protected TokenKind checkForKeyword(String identifier) {
		// Simple哈希表查找，以检查标识符是否与关键字哈希表中的任何关键字匹配。
		for (KeywordRecord kr = keywords[hashCode(identifier)]; kr != null; kr = kr.next) {
			if (kr.keyword.equals(identifier)) {
				return kr.tokenKind;
			}
		}

		// 未找到关键字。它只是一个标识符。
		return TokenKind.TOK_IDENTIFIER;
	}
}
