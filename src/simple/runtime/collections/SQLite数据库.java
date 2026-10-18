package simple.runtime.collections;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import simple.runtime.android.MainActivity;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.errors.数据库错误;

/**
 * Android SQLite 普通对象；每个实例独立持有连接，实例内的操作串行执行。
 * 相对数据库名使用应用私有数据库目录，调用者应在后台执行并显式关闭。
 *
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public final class SQLite数据库 {

	private SQLiteDatabase database;

	/**
	 * 创建尚未打开数据库的独立实例。
	 */
	public SQLite数据库() {
	}

	/**
	 * 创建并初始化新的 SQLite 文件；不改变本对象已打开的连接。
	 *
	 * @param path 应用私有数据库名，或可访问的绝对路径。
	 * @return 新建成功返回真；目标已存在返回假，不覆盖已有文件。
	 * @throws 数据库错误 路径无效、目录或文件无法创建，或 SQLite 初始化失败。
	 */
	@SimpleFunction
	public synchronized boolean 创建数据库(String path) {
		try {
			File file = resolve(path);
			prepareParent(file);
			if (!file.createNewFile()) return false;

			SQLiteDatabase created = SQLiteDatabase.openOrCreateDatabase(file, null);

			try {
				// 通过 SQLite 初始化文件，不把普通空文件当作已经完成初始化的数据库。
				created.setVersion(0);
			} finally {
				created.close();
			}

			return true;
		} catch (IOException | RuntimeException error) {
			throw failure("创建数据库", error);
		}
	}

	/**
	 * 打开或创建数据库；新连接打开成功后关闭旧连接，打开失败保留旧连接。
	 *
	 * @param path 应用私有数据库名，或可访问的绝对路径。
	 * @throws 数据库错误 路径无效、目录无法创建，或 SQLite 连接打开、切换失败。
	 */
	@SimpleFunction
	public synchronized void 打开数据库(String path) {
		SQLiteDatabase next = null;

		try {
			File file = resolve(path);
			prepareParent(file);
			next = SQLiteDatabase.openOrCreateDatabase(file, null);
			if (database != null) database.close();
			database = next;
			next = null;
		} catch (IOException | RuntimeException error) {
			throw failure("打开数据库", error);
		} finally {
			if (next != null) next.close();
		}
	}

	/**
	 * 关闭本实例的数据库连接；未打开或重复关闭时不执行操作。
	 *
	 * @throws 数据库错误 关闭 SQLite 连接失败。
	 */
	@SimpleFunction
	public synchronized void 关闭数据库() {
		SQLiteDatabase current = database;
		database = null;

		try {
			if (current != null) current.close();
		} catch (RuntimeException error) {
			throw failure("关闭数据库", error);
		}
	}

	/**
	 * 数据表不存在时创建，已存在时保持原表。
	 *
	 * @param table 单个数据表名，按字面标识符处理。
	 * @param fields SQL 字段定义，例如 编号 integer, 姓名 text。
	 * @throws 数据库错误 数据库未打开、表名或字段定义为空，或建表失败。
	 */
	@SimpleFunction
	public synchronized void 创建数据表(String table, String fields) {
		数据库执行("CREATE TABLE IF NOT EXISTS " + identifier(table) + " (" + text(fields, "字段定义") + ")");
	}

	/**
	 * 删除指定数据表，表不存在时报告错误。
	 *
	 * @param table 单个数据表名。
	 * @throws 数据库错误 数据库未打开、表名为空、表不存在，或删除失败。
	 */
	@SimpleFunction
	public synchronized void 删除数据表(String table) {
		数据库执行("DROP TABLE " + identifier(table));
	}

	/**
	 * 插入一条记录；值列表是 SQL 片段，文本值需按 SQL 规则引用。
	 *
	 * @param table 单个数据表名。
	 * @param values 按字段顺序排列的 SQL 值列表，例如 1,'张三'。
	 * @throws 数据库错误 数据库未打开、表名或记录值为空，或插入失败。
	 */
	@SimpleFunction
	public synchronized void 插入记录(String table, String values) {
		数据库执行("INSERT INTO " + identifier(table) + " VALUES (" + text(values, "记录值") + ")");
	}

	/**
	 * 删除符合 SQL 条件的记录；空条件拒绝执行，全部删除可明确传入 1=1。
	 *
	 * @param table 单个数据表名。
	 * @param condition SQL 条件，不含 WHERE。
	 * @throws 数据库错误 数据库未打开、表名或条件为空，或删除失败。
	 */
	@SimpleFunction
	public synchronized void 删除记录(String table, String condition) {
		数据库执行("DELETE FROM " + identifier(table) + " WHERE " + text(condition, "筛选条件"));
	}

	/**
	 * 修改符合 SQL 条件的记录。
	 *
	 * @param table 单个数据表名。
	 * @param assignments SQL 赋值列表，例如 姓名='李四'。
	 * @param condition SQL 条件，不含 WHERE；空条件拒绝执行。
	 * @throws 数据库错误 数据库未打开、表名、赋值列表或条件为空，或修改失败。
	 */
	@SimpleFunction
	public synchronized void 修改记录(String table, String assignments, String condition) {
		数据库执行("UPDATE " + identifier(table) + " SET " + text(assignments, "修改字段") + " WHERE " + text(condition, "筛选条件"));
	}

	/**
	 * 查询符合条件的全部字段；文本格式与 {@link #数据库查询(String, String, String)} 一致。
	 *
	 * @param table 单个数据表名。
	 * @param condition SQL 条件，不含 WHERE。
	 * @param columnSeparator 列分隔符，可为空文本。
	 * @param rowSeparator 行分隔符，可为空文本。
	 * @return 查询文本；无记录为空文本，失败抛出数据库错误。
	 * @throws 数据库错误 数据库未打开、表名或条件为空、分隔符未初始化，或查询失败。
	 */
	@SimpleFunction
	public synchronized String 条件查询(String table, String condition, String columnSeparator, String rowSeparator) {
		return 数据库查询("SELECT * FROM " + identifier(table) + " WHERE " + text(condition, "筛选条件"), columnSeparator, rowSeparator);
	}

	/**
	 * 查询指定范围的记录；未指定排序，稳定分页应使用含 ORDER BY 的数据库查询。
	 *
	 * @param table 单个数据表名。
	 * @param offset 从 0 开始的偏移，不得为负数。
	 * @param count 记录数，不得为负数；0 返回空文本。
	 * @param columnSeparator 列分隔符。
	 * @param rowSeparator 行分隔符。
	 * @return 查询文本；保留末尾列、行分隔符。
	 * @throws 数据库错误 数据库未打开、表名无效、偏移或条数为负数、分隔符未初始化，或查询失败。
	 */
	@SimpleFunction
	public synchronized String 范围查询(String table, int offset, int count, String columnSeparator, String rowSeparator) {
		if (offset < 0 || count < 0) throw new 数据库错误("查询偏移与记录数不能为负数。");

		return 数据库查询("SELECT * FROM " + identifier(table) + " LIMIT " + offset + "," + count, columnSeparator, rowSeparator);
	}

	/**
	 * 返回整数列的最大值，沿用 32 位整数返回类型。
	 *
	 * @param table 单个数据表名。
	 * @param column 单个字段名。
	 * @return 最大值；空表或全部为 SQL NULL 时为 0。
	 * @throws 数据库错误 数据库未打开、表名或字段名为空，或查询失败。
	 */
	@SimpleFunction
	public synchronized int 查询最大值(String table, String column) {
		Cursor cursor = null;

		try {
			cursor = connection().rawQuery("SELECT max(" + identifier(column) + ") FROM " + identifier(table), null);
			return cursor.moveToFirst() && !cursor.isNull(0) ? cursor.getInt(0) : 0;
		} catch (RuntimeException error) {
			throw failure("查询最大值", error);
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	/**
	 * 执行 SQL 查询，将每列及每行追加指定分隔符，包括末尾分隔符。
	 *
	 * @param sql 一条 SQL 查询语句；需要稳定顺序时显式指定 ORDER BY。
	 * @param columnSeparator 列分隔符，可为空文本。
	 * @param rowSeparator 行分隔符，可为空文本。
	 * @return 文本结果；无记录为空文本，SQL NULL 沿用文本 null，不适合无损表示二进制数据。
	 * @throws 数据库错误 数据库未打开、SQL 为空、分隔符未初始化，或查询失败。
	 */
	@SimpleFunction
	public synchronized String 数据库查询(String sql, String columnSeparator, String rowSeparator) {
		if (columnSeparator == null || rowSeparator == null) throw new 数据库错误("分隔符不能为未初始化值。");

		Cursor cursor = null;

		try {
			cursor = connection().rawQuery(text(sql, "SQL 查询"), null);
			StringBuilder result = new StringBuilder();

			while (cursor.moveToNext()) {
				for (int column = 0; column < cursor.getColumnCount(); column++) {
					result.append(cursor.getString(column)).append(columnSeparator);
				}

				result.append(rowSeparator);
			}

			return result.toString();
		} catch (RuntimeException error) {
			throw failure("数据库查询", error);
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	/**
	 * 执行一条非查询 SQL 语句，失败抛出数据库错误。
	 *
	 * @param sql SQL 建表、插入、修改或删除语句。
	 * @throws 数据库错误 数据库未打开、SQL 为空，或语句执行失败。
	 */
	@SimpleFunction
	public synchronized void 数据库执行(String sql) {
		try {
			connection().execSQL(text(sql, "SQL 命令"));
		} catch (RuntimeException error) {
			throw failure("数据库执行", error);
		}
	}

	/**
	 * 获取指定表的记录总数。
	 *
	 * @param table 单个数据表名。
	 * @return 记录总数，使用长整数；失败抛出数据库错误。
	 * @throws 数据库错误 数据库未打开、表名为空，或查询失败。
	 */
	@SimpleFunction
	public synchronized long 取记录数(String table) {
		Cursor cursor = null;

		try {
			cursor = connection().rawQuery("SELECT count(*) FROM " + identifier(table), null);
			return cursor.moveToFirst() ? cursor.getLong(0) : 0;
		} catch (RuntimeException error) {
			throw failure("取记录数", error);
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	/**
	 * 判断当前数据库是否存在指定表。
	 *
	 * @param table 数据表名，精确匹配保存的名称。
	 * @return 存在返回真，不存在返回假；连接或 SQL 失败抛出数据库错误。
	 * @throws 数据库错误 数据库未打开、表名为空，或查询失败。
	 */
	@SimpleFunction
	public synchronized boolean 数据表是否存在(String table) {
		Cursor cursor = null;

		try {
			// 表名作为查询值绑定，不拼接到 SQL 条件中。
			cursor = connection().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?", new String[] {text(table, "数据表名")});
			return cursor.moveToFirst();
		} catch (RuntimeException error) {
			throw failure("数据表是否存在", error);
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	/**
	 * 判断表是否包含指定字段；表不存在返回假。
	 *
	 * @param table 单个数据表名。
	 * @param column 字段名，不区分大小写。
	 * @return 存在返回真，不存在返回假。
	 * @throws 数据库错误 数据库未打开、表名或字段名为空，或查询失败。
	 */
	@SimpleFunction
	public synchronized boolean 字段是否存在(String table, String column) {
		text(column, "字段名");
		Cursor cursor = null;

		try {
			cursor = connection().rawQuery("PRAGMA table_info(" + identifier(table) + ")", null);

			while (cursor.moveToNext()) {
				if (column.equalsIgnoreCase(cursor.getString(cursor.getColumnIndexOrThrow("name")))) return true;
			}

			return false;
		} catch (RuntimeException error) {
			throw failure("字段是否存在", error);
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	/**
	 * 获取所有数据表，按名称排序，包括存在的 SQLite/Android 元数据表。
	 *
	 * @return 表名数组；没有表时为空数组，不返回空占位项。
	 * @throws 数据库错误 数据库未打开，或表名查询失败。
	 */
	@SimpleFunction
	public synchronized String[] 取所有数据表() {
		Cursor cursor = null;

		try {
			cursor = connection().rawQuery("SELECT name FROM sqlite_master WHERE type='table' ORDER BY name", null);
			ArrayList<String> names = new ArrayList<String>();

			while (cursor.moveToNext()) names.add(cursor.getString(0));

			return names.toArray(new String[names.size()]);
		} catch (RuntimeException error) {
			throw failure("取所有数据表", error);
		} finally {
			if (cursor != null) cursor.close();
		}
	}

	/**
	 * 取得当前已打开的数据库连接。
	 *
	 * @return 已经打开的数据库连接
	 */
	private SQLiteDatabase connection() {
		if (database == null || !database.isOpen()) throw new 数据库错误("请先打开数据库。");

		return database;
	}

	/**
	 * 要求文本非空且包含非空白字符。
	 *
	 * @param value 需要验证的非空文本。
	 * @param name 要操作的名称
	 * @return 校验或规范化后的文本
	 */
	private static String text(String value, String name) {
		if (value == null || value.trim().length() == 0) throw new 数据库错误(name + "不能为空。");

		return value;
	}

	/**
	 * 校验并引用 SQL 标识符。
	 *
	 * @param value 原始数据库标识符。
	 * @return 可用于 SQL 的标识符
	 */
	private static String identifier(String value) {
		return "\"" + text(value, "数据库标识符").replace("\"", "\"\"") + "\"";
	}

	/**
	 * 解析数据库文件位置；单独文件名使用应用数据库目录。
	 *
	 * @param path 文件或目录路径
	 * @return 解析后的文件对象
	 * @throws IOException 访问文件、流或网络资源失败时
	 */
	private static File resolve(String path) throws IOException {
		File file = new File(text(path, "数据库路径"));
		if (!file.isAbsolute()) {
			if (path.indexOf('/') >= 0 || path.indexOf('\\') >= 0 || ".".equals(path) || "..".equals(path)) {
				throw new 数据库错误("相对路径请只填写数据库文件名，需要目录时使用绝对路径。");
			}

			Context context = MainActivity.getContext().getApplicationContext();
			file = context.getDatabasePath(path);
		}

		return file.getCanonicalFile();
	}

	/**
	 * 创建目标文件需要的父目录。
	 *
	 * @param file 本次操作的文件
	 * @throws IOException 访问文件、流或网络资源失败时
	 */
	private static void prepareParent(File file) throws IOException {
		File parent = file.getParentFile();
		if (parent != null && !parent.isDirectory() && !parent.mkdirs() && !parent.isDirectory()) {
			throw new IOException("无法创建数据库目录。");
		}
	}

	/**
	 * 把底层失败转换为运行库错误。
	 *
	 * @param operation 失败的操作名称
	 * @param cause 底层失败原因
	 * @return 包装底层原因的运行库错误
	 */
	private static 数据库错误 failure(String operation, Exception cause) {
		if (cause instanceof 数据库错误) return (数据库错误) cause;

		数据库错误 error = new 数据库错误(operation + "失败：" + cause.getMessage());
		error.initCause(cause);
		return error;
	}
}
