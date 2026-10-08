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

package simple.runtime.components.impl.android;

import simple.runtime.像素转换;
import simple.runtime.日志输出;
import simple.runtime.components.可视组件;
import simple.runtime.components.布局;
import simple.runtime.components.组件容器;
import simple.runtime.components.impl.组件Impl;
import simple.runtime.components.impl.android.util.ImageUtil;
import simple.runtime.components.impl.android.util.ViewUtil;
import simple.runtime.errors.文件未存在错误;
import simple.runtime.errors.索引超出界限错误;
import simple.runtime.variants.Variant;
import simple.runtime.variants.IntegerVariant;

import java.io.IOException;

import android.graphics.drawable.Drawable;
import android.view.View;

/**
 * 具有Android视图的所有组件的基础基类。
 *
 * @author Herbert Czymontek
 */
public abstract class 视图组件 extends 组件Impl implements 可视组件 {
	/*
	android.view.View：
	https://developer.android.google.cn/reference/android/view/View
	*/

	// 视图
	private final View view;

	// 属性支持
	private int backgroundColor;
	private String backgroundImage;
	private int justification;

	private int column;
	private int row;

	private int left;
	private int top;  
	private int width;
	private int height; 

	/**
	 * 创建新的安卓视图组件。
	 *
	 * @param container  将容纳组件的容器（不得为{@code null}，因为非可见组件必须是窗口）
	 */
	protected 视图组件(组件容器 container) {
		super(container);
		
		// 预先设置，行和列属性会检验如果2个属性都设置了将会添加到布局中
		column = 布局_无_列;
		row = 布局_无_行;

		// 预先设置绝对布局下的子组件的可宽度和高度
		width = 长度_适应内容;
		height = 长度_适应内容;

		// 初始化并将组件添加到其容器中
		view = createView();

		// 面板创建视图将返回空，所以这里的判断下
		if (view != null) {
			// 给视图分配标识
			ViewUtil.allocationId(view);
			getComponentContainer().addComponent(this);
		}
	}

	/**
	 * 创建并初始化基础安卓视图。
	 *
	 * <p>由于该方法是由组件构造函数间接调用的，
	 * 所以实现者不应该假设关于实例的状态，
	 * 并且不应读取任何非静态字段。
	 */
	protected abstract View createView();

	/**
	 * 返回用户界面中显示的{@link View}。
	 *
	 * @return  视图
	 */
	public View getView() {
		return view;
	}

	@Override
	public void 销毁() {
		// 调用下父类的销毁方法
		super.销毁();

		// 将视图从父级中移除
		ViewUtil.removeView(getView());
	}

	/* 基础 */

	@Override
	public int 标识() {
		return ViewUtil.allocationId(getView());
	}

	@Override
	public void 标识(int id) {
		ViewUtil.setId(getView(), id);
	}

	@Override
	public boolean 可视() {
		return ViewUtil.isVisible(getView());
	}

	@Override
	public void 可视(boolean visible) {
		ViewUtil.setVisible(getView(), visible);
	}

	@Override
	public Variant 宽度() {
		布局 layout = getComponentContainer().getLayout();
		int value = layout instanceof 绝对布局Impl ? width : ViewUtil.getWidth(getView());
		return IntegerVariant.getIntegerVariant(value);
	}

	@Override
	public void 宽度(Variant value) {
		布局 layout = getComponentContainer().getLayout();
    	if (layout instanceof 绝对布局Impl) {
			width = 像素转换.解析像素(value);
        	((绝对布局Impl) layout).placeComponent(this);
      	} else {
            ViewUtil.setWidth(getView(), 像素转换.解析像素(value)); 
    	}
	}

	@Override
	public Variant 高度() {
		布局 layout = getComponentContainer().getLayout();
		int value = layout instanceof 绝对布局Impl ? height : ViewUtil.getHeight(getView());
		return IntegerVariant.getIntegerVariant(value);
	}

	@Override
	public void 高度(Variant value) {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 绝对布局Impl) {
			height = 像素转换.解析像素(value);
			((绝对布局Impl) layout).placeComponent(this);
		} else {    
			ViewUtil.setHeight(getView(), 像素转换.解析像素(value));
		}
	}

	/* 边距，该布局参数支持所有布局生效 */

	@Override
	public Variant 左边距() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getLeftMargin(getView()));
	}

	@Override
	public void 左边距(Variant leftMargin) {
		ViewUtil.setLeftMargin(getView(), 像素转换.解析像素(leftMargin));
	}

	@Override
	public Variant 顶边距() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getTopMargin(getView()));
	}

	@Override
	public void 顶边距(Variant topMargin) {
		ViewUtil.setTopMargin(getView(), 像素转换.解析像素(topMargin));
	}

	@Override
	public Variant 右边距() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getRightMargin(getView()));
	}

	@Override
	public void 右边距(Variant rightMargin) {
		ViewUtil.setRightMargin(getView(), 像素转换.解析像素(rightMargin));
	}

	@Override
	public Variant 底边距() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getBottomMargin(getView()));
	}

	@Override
	public void 底边距(Variant bottomMargin) {
		ViewUtil.setBottomMargin(getView(), 像素转换.解析像素(bottomMargin));
	}

	@Override
	public void 置边距(Variant leftMargin, Variant topMargin, Variant rightMargin, Variant bottomMargin) {
		ViewUtil.setMargin(
			getView(),
			像素转换.解析像素(leftMargin),
			像素转换.解析像素(topMargin),
			像素转换.解析像素(rightMargin),
			像素转换.解析像素(bottomMargin)
		);
	}

	/* 填充 */

	@Override
	public Variant 左填充() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getLeftPadding(getView()));
	}

	@Override
	public void 左填充(Variant leftPadding) {
		ViewUtil.setLeftPadding(getView(), 像素转换.解析像素(leftPadding));
	}

	@Override
	public Variant 顶填充() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getTopPadding(getView()));
	}

	@Override
	public void 顶填充(Variant topPadding) {
		ViewUtil.setTopPadding(getView(), 像素转换.解析像素(topPadding));
	}

	@Override
	public Variant 右填充() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getRightPadding(getView()));
	}

	@Override
	public void 右填充(Variant rightPadding) {
		ViewUtil.setRightPadding(getView(), 像素转换.解析像素(rightPadding));
	}

	@Override
	public Variant 底填充() {
		return IntegerVariant.getIntegerVariant(ViewUtil.getBottomPadding(getView()));
	}

	@Override
	public void 底填充(Variant bottomPadding) {
		ViewUtil.setBottomPadding(getView(), 像素转换.解析像素(bottomPadding));
	}

	@Override
	public void 置填充(Variant leftPadding, Variant topPadding, Variant rightPadding, Variant bottomPadding) {
		ViewUtil.setPadding(
			getView(),
			像素转换.解析像素(leftPadding),
			像素转换.解析像素(topPadding),
			像素转换.解析像素(rightPadding),
			像素转换.解析像素(bottomPadding)
		);
	}

	/* 背景 */ 

	@Override
	public int 背景颜色() {
		return backgroundColor;
	}

	@Override
	public void 背景颜色(int color) {
		backgroundColor = color;
		ViewUtil.setBackgroundColor(getView(), color);
	}

	@Override
	public String 背景图片() {
		return backgroundImage;
	}

	@Override
	public void 背景图片(String image) {
		backgroundImage = image;
		try {
			Drawable drawable = ImageUtil.getDrawable(image);
			if (drawable != null) {
				ViewUtil.setBackgroundDrawable(getView(), drawable);
			}
		} catch (IOException e) {
			throw new 文件未存在错误(image);
		}
	}


	/* 对齐，仅当自身位于这些布局时生效：线性布局、表格布局、单帧布局 */

	@Override
	public int 对齐() {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 线性布局Impl || layout instanceof 表格布局Impl || layout instanceof 单帧布局Impl) {
			return justification;
		} else {
			日志输出.warning("(%d) 获取对齐失败，该组件未位于线性、表格、单帧布局中", hashCode());
		}
		return justification;
	}

	@Override
	public void 对齐(int justification) {
		// 取自身视图的所在布局
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 线性布局Impl || layout instanceof 表格布局Impl || layout instanceof 单帧布局Impl) {
			this.justification = justification;
			ViewUtil.setGravity(getView(), ViewUtil.simpleToAndroidGravity(justification));
		} else {
			日志输出.warning("(%d) 设置对齐(%d)失败，该组件未位于线性、表格、单帧布局中", hashCode(), justification);
		}
	}

	/* 权重，仅当自身位于这些布局时生效：线性布局、表格布局、单帧布局 */

	@Override
	public float 权重() {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 线性布局Impl || layout instanceof 表格布局Impl || layout instanceof 单帧布局Impl) {
			return ViewUtil.getWeight(getView());
		} else {
			日志输出.warning("(%d) 获取权重失败，该组件未位于线性、表格、单帧布局中", hashCode());
		}
		return 0;
	}

	@Override
	public void 权重(float weight) {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 线性布局Impl || layout instanceof 表格布局Impl || layout instanceof 单帧布局Impl) {
			ViewUtil.setWeight(getView(), weight);
		} else {
			日志输出.warning("(%d) 设置权重(%f)失败，该组件未位于线性、表格、单帧布局中", hashCode(), weight);
		}
	}

	/* 行和列，仅当自身位于这些布局时生效：表格布局 */

	@Override
	public int 行() {
		return row;
	}

	@Override
	public void 行(int newRow) {
		// 无法重复设置
		if (row != 布局_无_行) {
			throw new 索引超出界限错误();
		}

		row = newRow;

		// 行列都已设置
		if (column != 布局_无_列) {
			布局 layout = getComponentContainer().getLayout();
			if (layout instanceof 表格布局Impl) {
				((表格布局Impl) layout).placeComponent(this);
			}
		}
	}

	@Override
	public int 列() {
		return column;
	}

	@Override
	public void 列(int newColumn) {
		// 无法重复设置
		if (column != 布局_无_列) {
			throw new 索引超出界限错误();
		}

		column = newColumn;

		// 行列都已设置
		if (row != 布局_无_行) {
			布局 layout = getComponentContainer().getLayout();
			if (layout instanceof 表格布局Impl) {
				((表格布局Impl) layout).placeComponent(this);
			}
		}
	}
	
	/* 规则，仅当自身位于这些布局时生效：相对布局 */

	@Override
	public void 添加规则(int verb) {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 相对布局Impl) {
			((相对布局Impl) layout).addComponentRule(this, verb);
		} else {
			日志输出.warning("(%d) 添加规则(%d)失败，该组件未位于相对布局中" , hashCode(), verb);
		}
	}

	@Override
	public void 添加规则(int verb, int anchor) {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 相对布局Impl) {
			((相对布局Impl) layout).addComponentRule(this, verb, anchor);
		} else {
			日志输出.warning("(%d) 添加规则(%d)瞄点(%d)失败，该组件未位于相对布局中" , hashCode(), verb, anchor);
		}
	}

	@Override
	public void 删除规则(int verb) {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 相对布局Impl) {
			((相对布局Impl) layout).removeComponentRule(this, verb);
		} else {
			日志输出.warning("(%d) 删除规则(%d)失败，该组件未位于相对布局中", hashCode(), verb);
		}
	}

	@Override
	public int 取瞄点(int verb) {
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 相对布局Impl) {
			return ((相对布局Impl) layout).getComponentAnchor(this, verb);
		} else {
			日志输出.warning("(%d) 获取规则(%d)瞄点失败，该组件未位于相对布局中", hashCode(), verb);
		}
		return 0;
	}

	@Override
	public int 位于左边() {
		return 取瞄点(布局_规则_左);
	}
	
	@Override
	public void 位于左边(int id) {
		添加规则(布局_规则_左, id);
	}

	@Override
	public int 位于顶边() {
		return 取瞄点(布局_规则_顶);
	}
	
	@Override
	public void 位于顶边(int id) {
		添加规则(布局_规则_顶, id);
	}

	@Override
	public int 位于右边() {
		return 取瞄点(布局_规则_右);
	}
	
	@Override
	public void 位于右边(int id) {
		添加规则(布局_规则_右, id);
	}

	@Override
	public int 位于底边() {
		return 取瞄点(布局_规则_底);
	}
	
	@Override
	public void 位于底边(int id) {
		添加规则(布局_规则_底, id);
	}

	@Override
	public int 对齐基线() {
		return 取瞄点(布局_规则_对齐_基线);
	}
	
	@Override
	public void 对齐基线(int id) {
		添加规则(布局_规则_对齐_基线, id);
	}

	@Override
	public int 对齐左边() {
		return 取瞄点(布局_规则_对齐_左);
	}

	@Override
	public void 对齐左边(int id) {
		添加规则(布局_规则_对齐_左, id);
	}

	@Override
	public int 对齐顶边() {
		return 取瞄点(布局_规则_对齐_顶);
	}

	@Override
	public void 对齐顶边(int id) {
		添加规则(布局_规则_对齐_顶, id);
	}

	@Override
	public int 对齐右边() {
		return 取瞄点(布局_规则_对齐_右);
	}

	@Override
	public void 对齐右边(int id) {
		添加规则(布局_规则_对齐_右, id);
	}
	
	@Override
	public int 对齐底边() {
		return 取瞄点(布局_规则_对齐_底);
	}

	@Override
	public void 对齐底边(int id) {
		添加规则(布局_规则_对齐_底, id);
	}
	
	@Override
	public boolean 对齐父左边() {
		return 取瞄点(布局_规则_对齐_父_左) == 布局_规则_真;
	}

	@Override
	public void 对齐父左边(boolean enable) {
		if (enable) {
			添加规则(布局_规则_对齐_父_左, 布局_规则_真);
		} else {
			删除规则(布局_规则_对齐_父_左);
		}
	}

	@Override
	public boolean 对齐父顶边() {
		return 取瞄点(布局_规则_对齐_父_顶) == 布局_规则_真;
	}
	
	@Override
	public void 对齐父顶边(boolean enable) {
		if (enable) {
			添加规则(布局_规则_对齐_父_顶, 布局_规则_真);
		} else {
			删除规则(布局_规则_对齐_父_顶);
		}
	}

	@Override
	public boolean 对齐父右边() {
		return 取瞄点(布局_规则_对齐_父_右) == 布局_规则_真;
	}
	
	@Override
	public void 对齐父右边(boolean enable) {
		if (enable) {
			添加规则(布局_规则_对齐_父_右, 布局_规则_真);
		} else {
			删除规则(布局_规则_对齐_父_右);
		}
	}

	@Override
	public boolean 对齐父底边() {
		return 取瞄点(布局_规则_对齐_父_底) == 布局_规则_真;
	}
	
	@Override
	public void 对齐父底边(boolean enable) {
		if (enable) {
			添加规则(布局_规则_对齐_父_底, 布局_规则_真);
		} else {
			删除规则(布局_规则_对齐_父_底);
		}
	}

	@Override
	public boolean 居中于父() {
		return 取瞄点(布局_规则_居中_于_父) == 布局_规则_真;
	}
	
	@Override
	public void 居中于父(boolean enable) {
		if (enable) {
			添加规则(布局_规则_居中_于_父, 布局_规则_真);
		} else {
			删除规则(布局_规则_居中_于_父);
		}
	}

	@Override
	public boolean 居中水平() {
		return 取瞄点(布局_规则_居中_水平) == 布局_规则_真;
	}

	@Override
	public void 居中水平(boolean enable) {
		if (enable) {
			添加规则(布局_规则_居中_水平, 布局_规则_真);
		} else {
			删除规则(布局_规则_居中_水平);
		}
	}

	@Override
	public boolean 居中垂直() {
		return 取瞄点(布局_规则_居中_垂直) == 布局_规则_真;
	}
	
	@Override
	public void 居中垂直(boolean enable) {
		if (enable) {
			添加规则(布局_规则_居中_垂直, 布局_规则_真);
		} else {
			删除规则(布局_规则_居中_垂直);
		}
	}

	@Override
	public int 位于开始() {
		return 取瞄点(布局_规则_开始);
	}
	
	@Override
	public void 位于开始(int id) {
		添加规则(布局_规则_开始, id);
	}

	@Override
	public int 位于结束() {
		return 取瞄点(布局_规则_结束);
	}
	
	@Override
	public void 位于结束(int id) {
		添加规则(布局_规则_结束, id);
	}

	@Override
	public int 对齐开始() {
		return 取瞄点(布局_规则_对齐_开始);
	}
	
	@Override
	public void 对齐开始(int id) {
		添加规则(布局_规则_对齐_开始, id);
	}
	
	@Override
	public int 对齐结束() {
		return 取瞄点(布局_规则_对齐_结束);
	}
	
	@Override
	public void 对齐结束(int id) {
		添加规则(布局_规则_对齐_结束, id);
	}
	
	@Override
	public boolean 对齐父开始() {
		return 取瞄点(布局_规则_对齐_父_开始) == 布局_规则_真;
	}
	
	@Override
	public void 对齐父开始(boolean enable) {
		if (enable) {
			添加规则(布局_规则_对齐_父_开始, 布局_规则_真);
		} else {
			删除规则(布局_规则_对齐_父_开始);
		}
	}

	@Override
	public boolean 对齐父结束() {
		return 取瞄点(布局_规则_对齐_父_结束) == 布局_规则_真;
	}
	
	@Override
	public void 对齐父结束(boolean enable) {
		if (enable) {
			添加规则(布局_规则_对齐_父_结束, 布局_规则_真);
		} else {
			删除规则(布局_规则_对齐_父_结束);
		}
	}

	/* 左边和顶边，仅当自身位于这些布局时生效：绝对布局 */

	@Override
	public Variant 左边() {
		return IntegerVariant.getIntegerVariant(left);
	}

  	@Override
  	public void 左边(Variant left) {
    	this.left = 像素转换.解析像素(left);

		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 绝对布局Impl) {
        	((绝对布局Impl) layout).placeComponent(this);
		}
    }

	@Override
	public Variant 顶边() {
    	return IntegerVariant.getIntegerVariant(top);
  	}

	@Override
	public void 顶边(Variant top) {
    	this.top = 像素转换.解析像素(top);
		
		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 绝对布局Impl) {
			((绝对布局Impl) layout).placeComponent(this);
		}
	}

	@Override
	public void 移动(Variant left, Variant top, Variant width, Variant height) {
		this.left = 像素转换.解析像素(left);
		this.top = 像素转换.解析像素(top);
		this.width = 像素转换.解析像素(width);
		this.height = 像素转换.解析像素(height);

		布局 layout = getComponentContainer().getLayout();
		if (layout instanceof 绝对布局Impl) {
			((绝对布局Impl) layout).placeComponent(this);
		}
	}
}
