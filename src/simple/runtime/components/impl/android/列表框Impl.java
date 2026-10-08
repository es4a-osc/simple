package simple.runtime.components.impl.android;

import simple.runtime.components.列表框;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.errors.索引超出界限错误;
import simple.runtime.android.MainActivity;
import simple.runtime.events.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

/**
 * 列表框组件的Android实现
 *
 * @author 树先生 xhwsd@qq.com
 */
public class 列表框Impl extends 列表视图组件 implements 列表框 {

	private List<String> list;
	private ArrayAdapter<String> adapter;

	public 列表框Impl(组件容器 container) {
		super(container);
	}

	/* 列表框 实现 */

	@Override
	public void 项目被滚动(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		EventDispatcher.dispatchEvent(this, "项目被滚动", firstVisibleItem, visibleItemCount, totalItemCount);
	}

	@Override
	public void 项目被单击(int index) {
		EventDispatcher.dispatchEvent(this, "项目被单击", index);
	}

	@Override
	public void 项目被长按(int index) {
		EventDispatcher.dispatchEvent(this, "项目被长按", index);
	}

	@Override
	public boolean 获取焦点() {
		return ViewUtil.requestFocus(getView());
	}

	@Override
	public void 清除焦点() {
		ViewUtil.clearFocus(getView());
	}

	@Override
	public void 垂直滚动(int x) {
		ListView view = (ListView) getView();
		view.scrollBy(0, view.getBottom() * x / 100);
	}

	@Override
	public int 取项目数() {
		return list.size();
	}

	@Override
	public void 选择项目(int index) {
		((ListView) getView()).setSelection(index);
	}

	@Override
	public void 删除项目(int index) {
		try {
			list.remove(index);
			adapter.notifyDataSetChanged();
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
	}

	@Override
	public void 清空项目() {
		list.clear();
		adapter.notifyDataSetChanged();
	}

	@Override
	public int 添加项目(String text) {
		list.add(text);
		adapter.notifyDataSetChanged();
		return list.size() - 1;
	}

	@Override
	public String 取项目文本(int index) {
		try {
			return list.get(index);
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
	}

	@Override
	public void 置项目文本(int index, String text) {
		try {
			list.set(index, text);
			adapter.notifyDataSetChanged();
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
	}

	/* 视图组件 相关方法 */ 

	@Override
	protected View createView() {
		list = new ArrayList<String>();
		/*
		ArrayAdapter
		https://developer.android.google.cn/reference/android/widget/ArrayAdapter
		*/
		
		// 一行文本列表布局
		adapter = new ArrayAdapter<String>(MainActivity.getContext(), android.R.layout.simple_list_item_1, list) {
			// 重写该方法，实现对文本视图的属性自定义
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView textView = (TextView) super.getView(position, convertView, parent);
				//textView.setTextColor(Color.RED);
				return textView;
			}
		};

		// ListView https://developer.android.google.cn/reference/android/widget/ListView
		ListView view = new ListView(MainActivity.getContext());
		view.setFocusable(true);
		view.setAdapter(adapter);

		// 去除拖动项目时的背景色
		view.setCacheColorHint(0);

		// 项目点击监听器
		view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				项目被单击(position);
			}
		});

		// 项目长按监听器
		view.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
				项目被长按(position);
				return true; // 不再触发项目点击事件
			}
		});

		// 滚动监听器
		view.setOnScrollListener(new AbsListView.OnScrollListener() {
			private int firstVisibleItem;
			private int visibleItemCount;
			private int totalItemCount;

			@Override
			public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
				this.firstVisibleItem = firstVisibleItem;
				this.visibleItemCount = visibleItemCount;
				this.totalItemCount = totalItemCount;
			}

			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				switch (scrollState) {
					case AbsListView.OnScrollListener.SCROLL_STATE_IDLE:
						//应用操作.弹出提示("停止滚动");
						项目被滚动(firstVisibleItem, visibleItemCount, totalItemCount);
						break;
					case AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL:
						//应用操作.弹出提示("正在滚动");
						break;
					case AbsListView.OnScrollListener.SCROLL_STATE_FLING:
						//应用操作.弹出提示("继续滑动");
						break;
				}
			}
		});

		return view;
	}
}
