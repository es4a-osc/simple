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

package simple.runtime.components;

import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;

/**
 * Simple组件
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleObject
public interface 组件 {

	/* 对齐常量。 */

	/**
	 * 内容水平靠左对齐。
	 */
	@SimpleDataElement
	static final int 水平_对齐_左 = 0;

	/**
	 * 内容水平居中对齐。
	 */
	@SimpleDataElement
	static final int 水平_对齐_中 = 1;

	/**
	 * 内容水平靠右对齐。
	 */
	@SimpleDataElement
	static final int 水平_对齐_右 = 2;

	/**
	 * 内容垂直靠顶对齐。
	 */
	@SimpleDataElement
	static final int 垂直_对齐_顶 = 0;

	/**
	 * 内容垂直居中对齐。
	 */
	@SimpleDataElement
	static final int 垂直_对齐_中 = 1;

	/**
	 * 内容垂直靠底对齐。
	 */
	@SimpleDataElement
	static final int 垂直_对齐_底 = 2;

	/* 颜色常量。 */

	/**
	 * 完全透明的 ARGB 颜色值。
	 */
	@SimpleDataElement
	static final int 颜色_无 = 0x00FFFFFF;

	/**
	 * ARGB 颜色常量：黑。
	 */
	@SimpleDataElement
	static final int 颜色_黑 = 0xFF000000;

	/**
	 * ARGB 颜色常量：蓝。
	 */
	@SimpleDataElement
	static final int 颜色_蓝 = 0xFF0000FF;

	/**
	 * ARGB 颜色常量：青绿。
	 */
	@SimpleDataElement
	static final int 颜色_青绿 = 0xFF00FFFF;

	/**
	 * ARGB 颜色常量：深灰。
	 */
	@SimpleDataElement
	static final int 颜色_深灰 = 0xFF444444;

	/**
	 * ARGB 颜色常量：灰。
	 */
	@SimpleDataElement
	static final int 颜色_灰 = 0xFF888888;

	/**
	 * ARGB 颜色常量：绿。
	 */
	@SimpleDataElement
	static final int 颜色_绿 = 0xFF00FF00;

	/**
	 * ARGB 颜色常量：浅灰。
	 */
	@SimpleDataElement
	static final int 颜色_浅灰 = 0xFFCCCCCC;

	/**
	 * ARGB 颜色常量：品红。
	 */
	@SimpleDataElement
	static final int 颜色_品红 = 0xFFFF00FF;

	/**
	 * ARGB 颜色常量：红。
	 */
	@SimpleDataElement
	static final int 颜色_红 = 0xFFFF0000;

	/**
	 * ARGB 颜色常量：白。
	 */
	@SimpleDataElement
	static final int 颜色_白 = 0xFFFFFFFF;

	/**
	 * ARGB 颜色常量：黄。
	 */
	@SimpleDataElement
	static final int 颜色_黄 = 0xFFFFFF00;

	/* 字体常量。 */

	/**
	 * 组件默认字体大小。
	 */
	@SimpleDataElement
	static final float 字体_默认_大小 = 14;

	/*
	组件重心（其实就是停靠）。

	 西北   北  东北
		↖  ↑  ↗
	  西 ←  中  → 东
		↙　↓　↘
	 西南   南  东南

	口诀：上北 下南 左西 右东
	*/
	/**
	 * 内容重心位置：中。
	 */
	@SimpleDataElement
	static final float 重心_中 = 0x11; // 17 ㊣
	/**
	 * 内容重心位置：北。
	 */
	@SimpleDataElement
	static final float 重心_北 = 0x31; // 49 ↑
	/**
	 * 内容重心位置：西北。
	 */
	@SimpleDataElement
	static final float 重心_西北 = 0x33; // 51 ↖
	/**
	 * 内容重心位置：西。
	 */
	@SimpleDataElement
	static final float 重心_西 = 0x13; // 19 ←
	/**
	 * 内容重心位置：西南。
	 */
	@SimpleDataElement
	static final float 重心_西南 = 0x53; // 83 ↙
	/**
	 * 内容重心位置：南。
	 */
	@SimpleDataElement
	static final float 重心_南 = 0x51; // 81 ↓
	/**
	 * 内容重心位置：东南。
	 */
	@SimpleDataElement
	static final float 重心_东南 = 0x55; // 85 ↘
	/**
	 * 内容重心位置：东。
	 */
	@SimpleDataElement
	static final float 重心_东 = 0x15;  // 21 →
	/**
	 * 内容重心位置：东北。
	 */
	@SimpleDataElement
	static final float 重心_东北 = 0x35; // 53 ↗

	/* 对齐常量 */

	/**
	 * 内容对齐方式：左。
	 */
	@SimpleDataElement
	static final int 对齐_左 = 0;

	/**
	 * 内容对齐方式：水平居中。
	 */
	@SimpleDataElement
	static final int 对齐_水平居中 = 1;

	/**
	 * 内容对齐方式：右。
	 */
	@SimpleDataElement
	static final int 对齐_右 = 2;

	/**
	 * 内容对齐方式：上。
	 */
	@SimpleDataElement
	static final int 对齐_上 = 3;

	/**
	 * 内容对齐方式：垂直居中。
	 */
	@SimpleDataElement
	static final int 对齐_垂直居中 = 4;

	/**
	 * 内容对齐方式：下。
	 */
	@SimpleDataElement
	static final int 对齐_下 = 5;

	/**
	 * 内容对齐方式：左上。
	 */
	@SimpleDataElement
	static final int 对齐_左上 = 6;

	/**
	 * 内容对齐方式：左中。
	 */
	@SimpleDataElement
	static final int 对齐_左中 = 8;

	/**
	 * 内容对齐方式：左下。
	 */
	@SimpleDataElement
	static final int 对齐_左下 = 9;

	/**
	 * 内容对齐方式：中上。
	 */
	@SimpleDataElement
	static final int 对齐_中上 = 10;

	/**
	 * 内容对齐方式：居中。
	 */
	@SimpleDataElement
	static final int 对齐_居中 = 11;

	/**
	 * 内容对齐方式：中下。
	 */
	@SimpleDataElement
	static final int 对齐_中下 = 12;

	/**
	 * 内容对齐方式：右上。
	 */
	@SimpleDataElement
	static final int 对齐_右上 = 13;

	/**
	 * 内容对齐方式：右中。
	 */
	@SimpleDataElement
	static final int 对齐_右中 = 14;

	/**
	 * 内容对齐方式：右下。
	 */
	@SimpleDataElement
	static final int 对齐_右下 = 15;

	/* 布局常量。 */

	/**
	 * 容器布局类型：线性布局。
	 */
	@SimpleDataElement
	static final int 布局_线性 = 1;

	/**
	 * 容器布局类型：表格布局。
	 */
	@SimpleDataElement
	static final int 布局_表格 = 2;

	/**
	 * 容器布局类型：单帧布局。
	 */
	@SimpleDataElement
	static final int 布局_单帧 = 3;

	/**
	 * 容器布局类型：相对布局。
	 */
	@SimpleDataElement
	static final int 布局_相对 = 4;

	/**
	 * 容器布局类型：绝对布局。
	 */
	@SimpleDataElement
	static final int 布局_绝对 = 5; // 该布局已于API3弃用，所以排末尾

	/**
	 * 水平排列子组件。
	 */
	@SimpleDataElement
	static final int 布局_方向_水平 = 0;

	/**
	 * 垂直排列子组件。
	 */
	@SimpleDataElement
	static final int 布局_方向_垂直 = 1;

	static final int 布局_无_列 = -1;
	static final int 布局_无_行 = -1;

	/*
	相对布局规则 相关常量

	参考文档：
	https://developer.android.google.cn/reference/android/widget/RelativeLayout.html#constants_2
	*/

	/**
	 * 主要用于相对父级规则。
	 *
	 * <p>{@code RelativeLayout.TRUE}
	 */
	@SimpleDataElement
	static final int 布局_规则_真 = -1;

	/**
	 * 在某元素左边
	 *
	 * <p>{@code RelativeLayout.LEFT_OF}
	 */
	@SimpleDataElement
	static final int 布局_规则_左 = 0;

	/**
	 * 在某元素右边。
	 *
	 * <p>{@code RelativeLayout.RIGHT_OF}
	 */
	@SimpleDataElement
	static final int 布局_规则_右 = 1;

	/**
	 * 在某元素顶边。
	 *
	 * <p>{@code RelativeLayout.ABOVE}
	 */
	@SimpleDataElement
	static final int 布局_规则_顶 = 2;

	/**
	 * 在某元素底边。
	 *
	 * <p>{@code RelativeLayout.BELOW}
	 */
	@SimpleDataElement
	static final int 布局_规则_底 = 3;

	/**
	 * 与某元素对齐基线。
	 *
	 * <p>{@code RelativeLayout.ALIGN_BASELINE}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_基线 = 4;

	/**
	 * 与某元素对齐左边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_LEFT}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_左 = 5;

	/**
	 * 与某元素对齐顶边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_TOP}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_顶 = 6;

	/**
	 * 与某元素对齐右边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_RIGHT}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_右 = 7;

	/**
	 * 与某元素对齐底边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_BOTTOM}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_底 = 8;

	/**
	 * 贴紧父元素左边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_PARENT_LEFT}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_父_左 = 9;

	/**
	 * 贴紧父元素顶边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_PARENT_TOP}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_父_顶 = 10;

	/**
	 * 贴紧父元素右边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_PARENT_RIGHT}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_父_右 = 11;

	/**
	 * 贴紧父元素底边。
	 *
	 * <p>{@code RelativeLayout.ALIGN_PARENT_BOTTOM}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_父_底 = 12;

	/**
	 * 相对于父元素完全居中（水平和垂直）。
	 *
	 * <p>{@code RelativeLayout.CENTER_IN_PARENT}
	 */
	@SimpleDataElement
	static final int 布局_规则_居中_于_父 = 13;

	/**
	 * 相对于父元素水平居中。
	 *
	 * <p>{@code RelativeLayout.CENTER_HORIZONTAL}
	 */
	@SimpleDataElement
	static final int 布局_规则_居中_水平 = 14;

	/**
	 * 相对于父元素垂直居中。
	 *
	 * <p>{@code RelativeLayout.CENTER_VERTICAL}
	 */
	@SimpleDataElement
	static final int 布局_规则_居中_垂直 = 15;

	/**
	 * 将子组件的结束边缘与另一个子组件的开始边缘对齐。
	 *
	 * <p>添加于API17，相当于{@link 可视组件#布局_规则_左}
	 * <p>{@code RelativeLayout.START_OF}
	 */
	@SimpleDataElement
	static final int 布局_规则_开始 = 16;

	/**
	 * 将子组件的开始边缘与另一个子组件的结束边缘对齐。
	 *
	 * <p>添加于API17，相当于{@link 可视组件#布局_规则_右}
	 * <p>{@code RelativeLayout.END_OF}
	 */
	@SimpleDataElement
	static final int 布局_规则_结束 = 17;

	/**
	 * 与某元素对齐开始。
	 *
	 * <p>添加于API17，相当于{@link 可视组件#布局_规则_对齐_左}
	 * <p>{@code RelativeLayout.ALIGN_START}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_开始 = 18;

	/**
	 * 与某元素对齐结束。
	 *
	 * <p>添加于API17，相当于{@link 可视组件#布局_规则_对齐_右}
	 * <p>{@code RelativeLayout.ALIGN_END}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_结束 = 19;

	/**
	 * 贴紧父元素开始。
	 *
	 * <p>添加于API17，相当于{@link 可视组件#布局_规则_对齐_父_左}
	 * <p>{@code RelativeLayout.ALIGN_PARENT_START}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_父_开始 = 20;

	/**
	 * 贴紧父元素结束。
	 *
	 * <p>添加于API17，相当于{@link 可视组件#布局_规则_对齐_父_右}。
	 * <p>{@code RelativeLayout.ALIGN_PARENT_END}
	 */
	@SimpleDataElement
	static final int 布局_规则_对齐_父_结束 = 21;

	/*
	字体类型常量。

	参考文档： <a href="https://developer.android.google.cn/reference/android/graphics/Typeface">参考说明</a>
	*/
	/**
	 * 字体类型：默认。
	 */
	@SimpleDataElement
	static final int 字体类型_默认 = 0;

	/**
	 * 字体类型：无衬线。
	 */
	@SimpleDataElement
	static final int 字体类型_无衬线 = 1;

	/**
	 * 字体类型：衬线。
	 */
	@SimpleDataElement
	static final int 字体类型_衬线 = 2;

	/**
	 * 字体类型：等宽。
	 */
	@SimpleDataElement
	static final int 字体类型_等宽 = 3;

	/*
	长度常量（宽度和高度）。

	注意转换关系：
	长度_适应内容 = -1 ---> android.view.ViewGroup.LayoutParams.WRAP_CONTENT = -2
	长度_匹配父级 = -2 ---> android.view.ViewGroup.LayoutParams.MATCH_PARENT = -1

	ViewGroup.LayoutParams
	https://developer.android.google.cn/reference/android/view/ViewGroup.LayoutParams#FILL_PARENT
	*/
	/**
	 * 尺寸随内容收缩，对应布局长度 -1。
	 */
	@SimpleDataElement
	static final int 长度_适应内容 = -1;

	/**
	 * 尺寸填满父容器的可用空间，对应布局长度 -2。
	 */
	@SimpleDataElement
	static final int 长度_匹配父级 = -2;

	/* 触摸事件常量。 */

	/**
	 * 触摸手势类型：单击。
	 */
	@SimpleDataElement
	static final int 触摸_单击 = 0x00000000;

	/**
	 * 触摸手势类型：双击。
	 */
	@SimpleDataElement
	static final int 触摸_双击 = 0x00000001;

	/**
	 * 触摸手势类型：上滑。
	 */
	@SimpleDataElement
	static final int 触摸_上滑 = 0x00000002;

	/**
	 * 触摸手势类型：下滑。
	 */
	@SimpleDataElement
	static final int 触摸_下滑 = 0x00000003;

	/**
	 * 触摸手势类型：左滑。
	 */
	@SimpleDataElement
	static final int 触摸_左滑 = 0x00000004;

	/**
	 * 触摸手势类型：右滑。
	 */
	@SimpleDataElement
	static final int 触摸_右滑 = 0x00000005;

	/**
	 * 触摸手势类型：上移。
	 */
	@SimpleDataElement
	static final int 触摸_上移 = 0x00000006;

	/**
	 * 触摸手势类型：下移。
	 */
	@SimpleDataElement
	static final int 触摸_下移 = 0x00000007;

	/**
	 * 触摸手势类型：左移。
	 */
	@SimpleDataElement
	static final int 触摸_左移 = 0x00000008;

	/**
	 * 触摸手势类型：右移。
	 */
	@SimpleDataElement
	static final int 触摸_右移 = 0x00000009;

	/*
	键代码常数。

	参考文档： <a href="https://www.cnblogs.com/bluestorm/p/4886662.html">参考说明</a>
	*/
	/**
	 * 按键代码：0。
	 */
	@SimpleDataElement
	static final int 键码_0 = 0x00000007;

	/**
	 * 按键代码：1。
	 */
	@SimpleDataElement
	static final int 键码_1 = 0x00000008;

	/**
	 * 按键代码：2。
	 */
	@SimpleDataElement
	static final int 键码_2 = 0x00000009;

	/**
	 * 按键代码：3。
	 */
	@SimpleDataElement
	static final int 键码_3 = 0x0000000A;

	/**
	 * 按键代码：4。
	 */
	@SimpleDataElement
	static final int 键码_4 = 0x0000000B;

	/**
	 * 按键代码：5。
	 */
	@SimpleDataElement
	static final int 键码_5 = 0x0000000C;

	/**
	 * 按键代码：6。
	 */
	@SimpleDataElement
	static final int 键码_6 = 0x0000000D;

	/**
	 * 按键代码：7。
	 */
	@SimpleDataElement
	static final int 键码_7 = 0x0000000E;

	/**
	 * 按键代码：8。
	 */
	@SimpleDataElement
	static final int 键码_8 = 0x0000000F;

	/**
	 * 按键代码：9。
	 */
	@SimpleDataElement
	static final int 键码_9 = 0x00000010;

	/**
	 * 按键代码：A。
	 */
	@SimpleDataElement
	static final int 键码_A = 0x0000001D;

	/**
	 * 按键代码：左 ALT。
	 */
	@SimpleDataElement
	static final int 键码_左_ALT = 0x00000039;

	/**
	 * 按键代码：右 ALT。
	 */
	@SimpleDataElement
	static final int 键码_右_ALT = 0x0000003A;

	/**
	 * 按键代码：引号。
	 */
	@SimpleDataElement
	static final int 键码_引号 = 0x0000004B; // '
	/**
	 * 按键代码：AT。
	 */
	@SimpleDataElement
	static final int 键码_AT = 0x0000004D; // @
	/**
	 * 按键代码：B。
	 */
	@SimpleDataElement
	static final int 键码_B = 0x0000001E;

	/**
	 * 按键代码：返回。
	 */
	@SimpleDataElement
	static final int 键码_返回 = 0x00000004;

	/**
	 * 按键代码：左斜杠。
	 */
	@SimpleDataElement
	static final int 键码_左斜杠 = 0x00000049; // \
	/**
	 * 按键代码：C。
	 */
	@SimpleDataElement
	static final int 键码_C = 0x0000001F;

	/**
	 * 按键代码：拨号。
	 */
	@SimpleDataElement
	static final int 键码_拨号 = 0x00000005;

	/**
	 * 按键代码：拍照。
	 */
	@SimpleDataElement
	static final int 键码_拍照 = 0x0000001B;

	/**
	 * 按键代码：CLEAR。
	 */
	@SimpleDataElement
	static final int 键码_CLEAR = 0x0000001C;

	/**
	 * 按键代码：逗号。
	 */
	@SimpleDataElement
	static final int 键码_逗号 = 0x00000037; // ,
	/**
	 * 按键代码：D。
	 */
	@SimpleDataElement
	static final int 键码_D = 0x00000020;

	/**
	 * 按键代码：退格。
	 */
	@SimpleDataElement
	static final int 键码_退格 = 0x00000043;

	/**
	 * 按键代码：导航 中。
	 */
	@SimpleDataElement
	static final int 键码_导航_中 = 0x00000017;

	/**
	 * 按键代码：导航 下。
	 */
	@SimpleDataElement
	static final int 键码_导航_下 = 0x00000014;

	/**
	 * 按键代码：导航 左。
	 */
	@SimpleDataElement
	static final int 键码_导航_左 = 0x00000015;

	/**
	 * 按键代码：导航 右。
	 */
	@SimpleDataElement
	static final int 键码_导航_右 = 0x00000016;

	/**
	 * 按键代码：导航 上。
	 */
	@SimpleDataElement
	static final int 键码_导航_上 = 0x00000013;

	/**
	 * 按键代码：E。
	 */
	@SimpleDataElement
	static final int 键码_E = 0x00000021;

	/**
	 * 按键代码：挂机。
	 */
	@SimpleDataElement
	static final int 键码_挂机 = 0x00000006;

	/**
	 * 按键代码：回车。
	 */
	@SimpleDataElement
	static final int 键码_回车 = 0x00000042;

	/**
	 * 按键代码：ENVELOPE。
	 */
	@SimpleDataElement
	static final int 键码_ENVELOPE = 0x00000041;

	/**
	 * 按键代码：EQUALS。
	 */
	@SimpleDataElement
	static final int 键码_EQUALS = 0x00000046;

	/**
	 * 按键代码：EXPLORER。
	 */
	@SimpleDataElement
	static final int 键码_EXPLORER = 0x00000040;

	/**
	 * 按键代码：F。
	 */
	@SimpleDataElement
	static final int 键码_F = 0x00000022;

	/**
	 * 按键代码：对焦。
	 */
	@SimpleDataElement
	static final int 键码_对焦 = 0x00000050;

	/**
	 * 按键代码：G。
	 */
	@SimpleDataElement
	static final int 键码_G = 0x00000023;

	/**
	 * 按键代码：GRAVE。
	 */
	@SimpleDataElement
	static final int 键码_GRAVE = 0x00000044; // `
	/**
	 * 按键代码：H。
	 */
	@SimpleDataElement
	static final int 键码_H = 0x00000024;

	/**
	 * 按键代码：耳机。
	 */
	@SimpleDataElement
	static final int 键码_耳机 = 0x0000004F;

	/**
	 * 按键代码：主页。
	 */
	@SimpleDataElement
	static final int 键码_主页 = 0x00000003;

	/**
	 * 按键代码：I。
	 */
	@SimpleDataElement
	static final int 键码_I = 0x00000025;

	/**
	 * 按键代码：J。
	 */
	@SimpleDataElement
	static final int 键码_J = 0x00000026;

	/**
	 * 按键代码：K。
	 */
	@SimpleDataElement
	static final int 键码_K = 0x00000027;

	/**
	 * 按键代码：L。
	 */
	@SimpleDataElement
	static final int 键码_L = 0x00000028;

	/**
	 * 按键代码：左 方括号。
	 */
	@SimpleDataElement
	static final int 键码_左_方括号 = 0x00000047; // [
	/**
	 * 按键代码：M。
	 */
	@SimpleDataElement
	static final int 键码_M = 0x00000029;

	/**
	 * 按键代码：多媒体 快进。
	 */
	@SimpleDataElement
	static final int 键码_多媒体_快进 = 0x0000005A;

	/**
	 * 按键代码：多媒体 下一个。
	 */
	@SimpleDataElement
	static final int 键码_多媒体_下一个 = 0x00000057;

	/**
	 * 按键代码：多媒体 播放 暂停。
	 */
	@SimpleDataElement
	static final int 键码_多媒体_播放_暂停 = 0x00000055;

	/**
	 * 按键代码：多媒体 上一个。
	 */
	@SimpleDataElement
	static final int 键码_多媒体_上一个 = 0x00000058;

	/**
	 * 按键代码：多媒体 快退。
	 */
	@SimpleDataElement
	static final int 键码_多媒体_快退 = 0x00000059;

	/**
	 * 按键代码：多媒体 暂停。
	 */
	@SimpleDataElement
	static final int 键码_多媒体_暂停 = 0x00000056;

	/**
	 * 按键代码：菜单。
	 */
	@SimpleDataElement
	static final int 键码_菜单 = 0x00000052;

	/**
	 * 按键代码：减号。
	 */
	@SimpleDataElement
	static final int 键码_减号 = 0x00000045; // -
	/**
	 * 按键代码：话筒静音。
	 */
	@SimpleDataElement
	static final int 键码_话筒静音 = 0x0000005B;

	/**
	 * 按键代码：N。
	 */
	@SimpleDataElement
	static final int 键码_N = 0x0000002A;

	/**
	 * 按键代码：通知。
	 */
	@SimpleDataElement
	static final int 键码_通知 = 0x00000053;

	/**
	 * 按键代码：NUM。
	 */
	@SimpleDataElement
	static final int 键码_NUM = 0x0000004E;

	/**
	 * 按键代码：O。
	 */
	@SimpleDataElement
	static final int 键码_O = 0x0000002B;

	/**
	 * 按键代码：P。
	 */
	@SimpleDataElement
	static final int 键码_P = 0x0000002C;

	/**
	 * 按键代码：句号。
	 */
	@SimpleDataElement
	static final int 键码_句号 = 0x00000038; // .
	/**
	 * 按键代码：加号。
	 */
	@SimpleDataElement
	static final int 键码_加号 = 0x00000051; // +
	/**
	 * 按键代码：井号。
	 */
	@SimpleDataElement
	static final int 键码_井号 = 0x00000012; // #
	/**
	 * 按键代码：电源。
	 */
	@SimpleDataElement
	static final int 键码_电源 = 0x000000201A;

	/**
	 * 按键代码：Q。
	 */
	@SimpleDataElement
	static final int 键码_Q = 0x0000002D;

	/**
	 * 按键代码：R。
	 */
	@SimpleDataElement
	static final int 键码_R = 0x0000002E;

	/**
	 * 按键代码：右 方括号。
	 */
	@SimpleDataElement
	static final int 键码_右_方括号 = 0x00000048;  // ]
	/**
	 * 按键代码：S。
	 */
	@SimpleDataElement
	static final int 键码_S = 0x0000002F;

	/**
	 * 按键代码：搜索。
	 */
	@SimpleDataElement
	static final int 键码_搜索 = 0x00000054;

	/**
	 * 按键代码：分号。
	 */
	@SimpleDataElement
	static final int 键码_分号 = 0x0000004A; // ;

	/**
	 * 按键代码：左 SHIFT。
	 */
	@SimpleDataElement
	static final int 键码_左_SHIFT = 0x0000003B;

	/**
	 * 按键代码：右 SHIFT。
	 */
	@SimpleDataElement
	static final int 键码_右_SHIFT = 0x0000003C; // /
	/**
	 * 按键代码：右斜杠。
	 */
	@SimpleDataElement
	static final int 键码_右斜杠 = 0x0000004C;

	/**
	 * 按键代码：左。
	 */
	@SimpleDataElement
	static final int 键码_左 = 0x00000001;

	/**
	 * 按键代码：右。
	 */
	@SimpleDataElement
	static final int 键码_右 = 0x00000002;

	/**
	 * 按键代码：空格。
	 */
	@SimpleDataElement
	static final int 键码_空格 = 0x0000003E;

	/**
	 * 按键代码：星号。
	 */
	@SimpleDataElement
	static final int 键码_星号 = 0x00000011; // *
	/**
	 * 按键代码：SYM。
	 */
	@SimpleDataElement
	static final int 键码_SYM = 0x0000003F;

	/**
	 * 按键代码：T。
	 */
	@SimpleDataElement
	static final int 键码_T = 0x00000030;

	/**
	 * 按键代码：TAB。
	 */
	@SimpleDataElement
	static final int 键码_TAB = 0x0000003D;

	/**
	 * 按键代码：U。
	 */
	@SimpleDataElement
	static final int 键码_U = 0x00000031;

	/**
	 * 按键代码：V。
	 */
	@SimpleDataElement
	static final int 键码_V = 0x00000032;

	/**
	 * 按键代码：音量 减。
	 */
	@SimpleDataElement
	static final int 键码_音量_减 = 0x00000019;

	/**
	 * 按键代码：音量 增。
	 */
	@SimpleDataElement
	static final int 键码_音量_增 = 0x00000018;

	/**
	 * 按键代码：W。
	 */
	@SimpleDataElement
	static final int 键码_W = 0x00000033;

	/**
	 * 按键代码：X。
	 */
	@SimpleDataElement
	static final int 键码_X = 0x00000034;

	/**
	 * 按键代码：Y。
	 */
	@SimpleDataElement
	static final int 键码_Y = 0x00000035;

	/**
	 * 按键代码：Z。
	 */
	@SimpleDataElement
	static final int 键码_Z = 0x00000036;

	/* 输入模式 */

	/**
	 * 编辑框输入模式：文本。
	 */
	@SimpleDataElement
	static final int 输入模式_文本 = 0;

	/**
	 * 编辑框输入模式：数字。
	 */
	@SimpleDataElement
	static final int 输入模式_数字 = 1;

	/**
	 * 编辑框输入模式：邮箱。
	 */
	@SimpleDataElement
	static final int 输入模式_邮箱 = 2;

	/**
	 * 编辑框输入模式：密码。
	 */
	@SimpleDataElement
	static final int 输入模式_密码 = 3;

	/**
	 * 编辑框输入模式：链接。
	 */
	@SimpleDataElement
	static final int 输入模式_链接 = 4;

	/* 组件事件 */

	/**
	 * 默认初始化事件处理程序。
	 */
	@SimpleEvent
	void 初始化();

	/* 组件方法 */

	/**
	 * 销毁释放组件相关资源。
	 *
	 * <p>注意仅为相关资源等释放，最终还需等待组件变量释放。
	 */
	@SimpleFunction
	void 销毁();
}
