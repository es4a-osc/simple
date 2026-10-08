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
	@SimpleDataElement
	static final int 水平_对齐_左 = 0;
	@SimpleDataElement
	static final int 水平_对齐_中 = 1;
	@SimpleDataElement
	static final int 水平_对齐_右 = 2;
	@SimpleDataElement
	static final int 垂直_对齐_顶 = 0;
	@SimpleDataElement
	static final int 垂直_对齐_中 = 1;
	@SimpleDataElement
	static final int 垂直_对齐_底 = 2;

	/* 颜色常量。 */
	@SimpleDataElement
	static final int 颜色_无 = 0x00FFFFFF;
	@SimpleDataElement
	static final int 颜色_黑 = 0xFF000000;
	@SimpleDataElement
	static final int 颜色_蓝 = 0xFF0000FF;
	@SimpleDataElement
	static final int 颜色_青绿 = 0xFF00FFFF;
	@SimpleDataElement
	static final int 颜色_深灰 = 0xFF444444;
	@SimpleDataElement
	static final int 颜色_灰 = 0xFF888888;
	@SimpleDataElement
	static final int 颜色_绿 = 0xFF00FF00;
	@SimpleDataElement
	static final int 颜色_浅灰 = 0xFFCCCCCC;
	@SimpleDataElement
	static final int 颜色_品红 = 0xFFFF00FF;
	@SimpleDataElement
	static final int 颜色_红 = 0xFFFF0000;
	@SimpleDataElement
	static final int 颜色_白 = 0xFFFFFFFF;
	@SimpleDataElement
	static final int 颜色_黄 = 0xFFFFFF00;

	/* 字体常量。 */
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
	@SimpleDataElement
	static final float 重心_中 = 0x11; // 17 ㊣
	@SimpleDataElement
	static final float 重心_北 = 0x31; // 49 ↑
	@SimpleDataElement
	static final float 重心_西北 = 0x33; // 51 ↖
	@SimpleDataElement
	static final float 重心_西 = 0x13; // 19 ←
	@SimpleDataElement
	static final float 重心_西南 = 0x53; // 83 ↙
	@SimpleDataElement
	static final float 重心_南 = 0x51; // 81 ↓
	@SimpleDataElement
	static final float 重心_东南 = 0x55; // 85 ↘
	@SimpleDataElement
	static final float 重心_东 = 0x15;  // 21 →
	@SimpleDataElement
	static final float 重心_东北 = 0x35; // 53 ↗

	/* 对齐常量 */
	@SimpleDataElement
	static final int 对齐_左 = 0;
	@SimpleDataElement
	static final int 对齐_水平居中 = 1;
	@SimpleDataElement
	static final int 对齐_右 = 2;
	@SimpleDataElement
	static final int 对齐_上 = 3;
	@SimpleDataElement
	static final int 对齐_垂直居中 = 4;
	@SimpleDataElement
	static final int 对齐_下 = 5;
	@SimpleDataElement
	static final int 对齐_左上 = 6;
	@SimpleDataElement
	static final int 对齐_左中 = 8;
	@SimpleDataElement
	static final int 对齐_左下 = 9;
	@SimpleDataElement
	static final int 对齐_中上 = 10;
	@SimpleDataElement
	static final int 对齐_居中 = 11;
	@SimpleDataElement
	static final int 对齐_中下 = 12;
	@SimpleDataElement
	static final int 对齐_右上 = 13;
	@SimpleDataElement
	static final int 对齐_右中 = 14;
	@SimpleDataElement
	static final int 对齐_右下 = 15;

	/* 布局常量。 */
	@SimpleDataElement
	static final int 布局_线性 = 1;
	@SimpleDataElement
	static final int 布局_表格 = 2;
	@SimpleDataElement
	static final int 布局_单帧 = 3;
	@SimpleDataElement
	static final int 布局_相对 = 4;
	@SimpleDataElement
	static final int 布局_绝对 = 5; // 该布局已于API3弃用，所以排末尾


	@SimpleDataElement
	static final int 布局_方向_水平 = 0;
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

	参考文档： {@link https://developer.android.google.cn/reference/android/graphics/Typeface}
	*/
	@SimpleDataElement
	static final int 字体类型_默认 = 0;
	@SimpleDataElement
	static final int 字体类型_无衬线 = 1;
	@SimpleDataElement
	static final int 字体类型_衬线 = 2;
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
	@SimpleDataElement
	static final int 长度_适应内容 = -1;
	@SimpleDataElement
	static final int 长度_匹配父级 = -2; 

	/* 触摸事件常量。 */
	@SimpleDataElement
	static final int 触摸_单击 = 0x00000000;
	@SimpleDataElement
	static final int 触摸_双击 = 0x00000001;
	@SimpleDataElement
	static final int 触摸_上滑 = 0x00000002;
	@SimpleDataElement
	static final int 触摸_下滑 = 0x00000003;
	@SimpleDataElement
	static final int 触摸_左滑 = 0x00000004;
	@SimpleDataElement
	static final int 触摸_右滑 = 0x00000005;
	@SimpleDataElement
	static final int 触摸_上移 = 0x00000006;
	@SimpleDataElement
	static final int 触摸_下移 = 0x00000007;
	@SimpleDataElement
	static final int 触摸_左移 = 0x00000008;
	@SimpleDataElement
	static final int 触摸_右移 = 0x00000009;

	/*
	键代码常数。
	
	参考文档： {@link https://www.cnblogs.com/bluestorm/p/4886662.html}
	*/
	@SimpleDataElement
	static final int 键码_0 = 0x00000007;
	@SimpleDataElement
	static final int 键码_1 = 0x00000008;
	@SimpleDataElement
	static final int 键码_2 = 0x00000009;
	@SimpleDataElement
	static final int 键码_3 = 0x0000000A;
	@SimpleDataElement
	static final int 键码_4 = 0x0000000B;
	@SimpleDataElement
	static final int 键码_5 = 0x0000000C;
	@SimpleDataElement
	static final int 键码_6 = 0x0000000D;
	@SimpleDataElement
	static final int 键码_7 = 0x0000000E;
	@SimpleDataElement
	static final int 键码_8 = 0x0000000F;
	@SimpleDataElement
	static final int 键码_9 = 0x00000010;
	@SimpleDataElement
	static final int 键码_A = 0x0000001D;
	@SimpleDataElement
	static final int 键码_左_ALT = 0x00000039;
	@SimpleDataElement
	static final int 键码_右_ALT = 0x0000003A;
	@SimpleDataElement
	static final int 键码_引号 = 0x0000004B; // '
	@SimpleDataElement
	static final int 键码_AT = 0x0000004D; // @
	@SimpleDataElement
	static final int 键码_B = 0x0000001E;
	@SimpleDataElement
	static final int 键码_返回 = 0x00000004;
	@SimpleDataElement
	static final int 键码_左斜杠 = 0x00000049; // \
	@SimpleDataElement
	static final int 键码_C = 0x0000001F;
	@SimpleDataElement
	static final int 键码_拨号 = 0x00000005;
	@SimpleDataElement
	static final int 键码_拍照 = 0x0000001B;
	@SimpleDataElement
	static final int 键码_CLEAR = 0x0000001C;
	@SimpleDataElement
	static final int 键码_逗号 = 0x00000037; // ,
	@SimpleDataElement
	static final int 键码_D = 0x00000020;
	@SimpleDataElement
	static final int 键码_退格 = 0x00000043;
	@SimpleDataElement
	static final int 键码_导航_中 = 0x00000017;
	@SimpleDataElement
	static final int 键码_导航_下 = 0x00000014;
	@SimpleDataElement
	static final int 键码_导航_左 = 0x00000015;
	@SimpleDataElement
	static final int 键码_导航_右 = 0x00000016;
	@SimpleDataElement
	static final int 键码_导航_上 = 0x00000013;
	@SimpleDataElement
	static final int 键码_E = 0x00000021;
	@SimpleDataElement
	static final int 键码_挂机 = 0x00000006;
	@SimpleDataElement
	static final int 键码_回车 = 0x00000042;
	@SimpleDataElement
	static final int 键码_ENVELOPE = 0x00000041;
	@SimpleDataElement
	static final int 键码_EQUALS = 0x00000046;
	@SimpleDataElement
	static final int 键码_EXPLORER = 0x00000040;
	@SimpleDataElement
	static final int 键码_F = 0x00000022;
	@SimpleDataElement
	static final int 键码_对焦 = 0x00000050;
	@SimpleDataElement
	static final int 键码_G = 0x00000023;
	@SimpleDataElement
	static final int 键码_GRAVE = 0x00000044; // `
	@SimpleDataElement
	static final int 键码_H = 0x00000024;
	@SimpleDataElement
	static final int 键码_耳机 = 0x0000004F;
	@SimpleDataElement
	static final int 键码_主页 = 0x00000003;
	@SimpleDataElement
	static final int 键码_I = 0x00000025;
	@SimpleDataElement
	static final int 键码_J = 0x00000026;
	@SimpleDataElement
	static final int 键码_K = 0x00000027;
	@SimpleDataElement
	static final int 键码_L = 0x00000028;
	@SimpleDataElement
	static final int 键码_左_方括号 = 0x00000047; // [
	@SimpleDataElement
	static final int 键码_M = 0x00000029;
	@SimpleDataElement
	static final int 键码_多媒体_快进 = 0x0000005A;
	@SimpleDataElement
	static final int 键码_多媒体_下一个 = 0x00000057;
	@SimpleDataElement
	static final int 键码_多媒体_播放_暂停 = 0x00000055;
	@SimpleDataElement
	static final int 键码_多媒体_上一个 = 0x00000058;
	@SimpleDataElement
	static final int 键码_多媒体_快退 = 0x00000059;
	@SimpleDataElement
	static final int 键码_多媒体_暂停 = 0x00000056;
	@SimpleDataElement
	static final int 键码_菜单 = 0x00000052;
	@SimpleDataElement
	static final int 键码_减号 = 0x00000045; // -
	@SimpleDataElement
	static final int 键码_话筒静音 = 0x0000005B;
	@SimpleDataElement
	static final int 键码_N = 0x0000002A;
	@SimpleDataElement
	static final int 键码_通知 = 0x00000053;
	@SimpleDataElement
	static final int 键码_NUM = 0x0000004E;
	@SimpleDataElement
	static final int 键码_O = 0x0000002B;
	@SimpleDataElement
	static final int 键码_P = 0x0000002C;
	@SimpleDataElement
	static final int 键码_句号 = 0x00000038; // .
	@SimpleDataElement
	static final int 键码_加号 = 0x00000051; // +
	@SimpleDataElement
	static final int 键码_井号 = 0x00000012; // #
	@SimpleDataElement
	static final int 键码_电源 = 0x000000201A;
	@SimpleDataElement
	static final int 键码_Q = 0x0000002D;
	@SimpleDataElement
	static final int 键码_R = 0x0000002E;
	@SimpleDataElement
	static final int 键码_右_方括号 = 0x00000048;  // ]
	@SimpleDataElement
	static final int 键码_S = 0x0000002F;
	@SimpleDataElement
	static final int 键码_搜索 = 0x00000054;
	@SimpleDataElement
	static final int 键码_分号 = 0x0000004A; // ;
	@SimpleDataElement
	static final int 键码_左_SHIFT = 0x0000003B;
	@SimpleDataElement
	static final int 键码_右_SHIFT = 0x0000003C; // /
	@SimpleDataElement
	static final int 键码_右斜杠 = 0x0000004C;
	@SimpleDataElement
	static final int 键码_左 = 0x00000001;
	@SimpleDataElement
	static final int 键码_右 = 0x00000002;
	@SimpleDataElement
	static final int 键码_空格 = 0x0000003E;
	@SimpleDataElement
	static final int 键码_星号 = 0x00000011; // *
	@SimpleDataElement
	static final int 键码_SYM = 0x0000003F;
	@SimpleDataElement
	static final int 键码_T = 0x00000030;
	@SimpleDataElement
	static final int 键码_TAB = 0x0000003D;
	@SimpleDataElement
	static final int 键码_U = 0x00000031;
	@SimpleDataElement
	static final int 键码_V = 0x00000032;
	@SimpleDataElement
	static final int 键码_音量_减 = 0x00000019;
	@SimpleDataElement
	static final int 键码_音量_增 = 0x00000018;
	@SimpleDataElement
	static final int 键码_W = 0x00000033;
	@SimpleDataElement
	static final int 键码_X = 0x00000034;
	@SimpleDataElement
	static final int 键码_Y = 0x00000035;
	@SimpleDataElement
	static final int 键码_Z = 0x00000036;

	/* 输入模式 */
	@SimpleDataElement
	static final int 输入模式_文本 = 0;
	@SimpleDataElement
	static final int 输入模式_数字 = 1;
	@SimpleDataElement
	static final int 输入模式_邮箱 = 2;
	@SimpleDataElement
	static final int 输入模式_密码 = 3;
	@SimpleDataElement
	static final int 输入模式_链接 = 4;

	/**
	 * 默认初始化事件处理程序。
	 */
	@SimpleEvent
	void 初始化();

	/**
	 * 销毁释放组件相关资源。
	 *
	 * <p>注意仅为相关资源等释放，最终还需等待组件变量释放。
	 */
	@SimpleFunction
	void 销毁();
}
