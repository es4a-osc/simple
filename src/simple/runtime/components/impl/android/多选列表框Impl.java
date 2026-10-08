package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.多选列表框;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.errors.索引超出界限错误;
import simple.runtime.events.EventDispatcher;

import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class 多选列表框Impl extends 列表视图组件 implements 多选列表框 {

	private List<String> list;
	private ArrayAdapter<String> adapter;

	public 多选列表框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		list = new ArrayList<String>();
		// 多选列表布局 android.R.layout.simple_list_item_multiple_choice
		adapter = new ArrayAdapter<String>(MainActivity.getContext(),
				android.R.layout.simple_list_item_multiple_choice, list) {

			// 重写该方法，实现对文本视图的属性自定义
			@Override
			public View getView(int position, View convertView, ViewGroup parent) {
				TextView textView = (TextView) super.getView(position, convertView, parent);
				//textView.setTextColor(Color.RED);
				return textView;
			}
		};
		ListView view = new ListView(MainActivity.getContext());
		view.setFocusable(true);
		view.setAdapter(adapter);

		// 选择模式：多选
		view.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		view.setCacheColorHint(0);

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

		return view;
	}

	// 多选列表框 实现

	@Override
	public void 项目被滚动(int firstVisibleItem, int visibleItemCount, int totalItemCount) {
		EventDispatcher.dispatchEvent(this, "项目被滚动", firstVisibleItem, visibleItemCount, totalItemCount);
	}

	@Override
	public void 项目被单击(int position) {
		EventDispatcher.dispatchEvent(this, "项目被单击", position);
	}

	@Override
	public void 项目被长按(int position) {
		EventDispatcher.dispatchEvent(this, "项目被长按", position);
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
	public void 插入项目(int index, String text) {
		try {
			list.add(index, text);
			adapter.notifyDataSetChanged();
		} catch (IndexOutOfBoundsException e) {
			throw new 索引超出界限错误();
		}
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

	@Override
	public boolean 取项目选中(int index) {
		return ((ListView) getView()).isItemChecked(index);
	}

	@Override
	public void 置项目选中(int index, boolean value) {
		((ListView) getView()).setItemChecked(index, value);
	}

	@Override
	public int[] 取选中项目() {
		List<Integer> checked = new ArrayList<Integer>();
		SparseBooleanArray array = ((ListView) getView()).getCheckedItemPositions();
		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				checked.add(array.keyAt(i));
			}
		}
		// List<Integer> 转 int[]，这货是JDK1.8！
		//return checked.stream().mapToInt(Integer::valueOf).toArray();
		int[] indexs = new int[checked.size()];
		for (int i = 0; i < indexs.length; i++) {
			indexs[i] = checked.get(i);
		}
		return indexs;
	}

	@Override
	public int 取选中项目数() {
		int count = 0;
		SparseBooleanArray array = ((ListView) getView()).getCheckedItemPositions();
		for (int i = 0; i < array.size(); i++) {
			if (array.valueAt(i)) {
				count += 1;
			}
		}
		return count;
	}
}
