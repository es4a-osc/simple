package simple.runtime.components.impl.android;

import simple.runtime.android.MainActivity;
import simple.runtime.components.组件容器;
import simple.runtime.components.下拉列表框;
import simple.runtime.errors.索引超出界限错误;
import simple.runtime.events.EventDispatcher;

import java.util.ArrayList;
import java.util.List;

import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.Spinner;

public class 下拉列表框Impl extends 列表视图组件 implements 下拉列表框 {

	private List<String> list;
	private ArrayAdapter<String> adapter;
	private int selection;

	public 下拉列表框Impl(组件容器 container) {
		super(container);
	}

	@Override
	protected View createView() {
		list = new ArrayList<String>();
		adapter = new ArrayAdapter<String>(MainActivity.getContext(), android.R.layout.simple_spinner_item, list);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

		// Spinner https://developer.android.google.cn/reference/android/widget/Spinner
		Spinner view = new Spinner(MainActivity.getContext());
		view.setAdapter(adapter);

		// 项目选择监听器
		view.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				selection = position;
				项目被选择(position);
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// 无可做事...
			}
		});
		return view;
	}

	// 下拉列表框 实现

	@Override
	public void 项目被选择(int index) {
		EventDispatcher.dispatchEvent(this, "项目被选择", index);
	}

	@Override
	public int 现行选中项() {
		return selection;
	}

	@Override
	public void 现行选中项(int index) {
		Spinner view = (Spinner) getView();
		view.setSelection(index);
		view.invalidate();
		selection = index;
	}

	@Override
	public int 取项目数() {
		return list.size();
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
}
