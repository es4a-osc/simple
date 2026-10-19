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

import simple.runtime.annotations.SimpleComponent;
import simple.runtime.annotations.SimpleDataElement;
import simple.runtime.annotations.SimpleEvent;
import simple.runtime.annotations.SimpleFunction;
import simple.runtime.annotations.SimpleObject;
import simple.runtime.annotations.SimpleProperty;

/**
 * 支持图形、文字、图片绘制和画面保存的画板组件。
 *
 * <p>绘制、字号、线宽及触摸坐标均使用画板内的绝对像素 px，不自动进行 dp 或 sp 换算。
 * 所有操作在界面线程调用；取点和保存须等待画板布局完成。调整尺寸保留左上区域，
 * 缩小时超出新边界的内容被裁去；清空和设置背景颜色会用背景色重置画面。</p>
 *
 * @author Herbert Czymontek
 * @author 树先生 xhwsd@qq.com
 */
@SimpleComponent
@SimpleObject
public interface 画板 extends 可视组件 {

	/* 绘制与保存常量 */

	/**
	 * 仅绘制轮廓。
	 */
	@SimpleDataElement
	int 画笔_描边 = 0;

	/**
	 * 填充图形内部。
	 */
	@SimpleDataElement
	int 画笔_填充 = 1;

	/**
	 * PNG 格式，保留透明度。
	 */
	@SimpleDataElement
	int 图片格式_PNG = 0;

	/**
	 * JPEG 格式，不保留透明度。
	 */
	@SimpleDataElement
	int 图片格式_JPEG = 1;

	/* 组件事件 */

	/**
	 * 默认按下事件处理方法。
	 *
	 * @param x 按下横向位置。
	 * @param y 按下纵向位置。
	 */
	@SimpleEvent
	void 被按下(int x, int y);

	/**
	 * 默认被弹起事件处理方法。
	 *
	 * @param x 弹起横向位置。
	 * @param y 弹起纵向位置。
	 */
	@SimpleEvent
	void 被弹起(int x, int y);

	/**
	 * 默认触摸移动事件处理方法。
	 *
	 * @param lastX 移动起始横向位置。
	 * @param lastY 移动起始纵向位置。
	 * @param currX 移动结束横向位置。
	 * @param currY 移动结束纵向位置。
	 */
	@SimpleEvent
	void 触摸移动(int lastX, int lastY, int currX, int currY);

	/**
	 * 默认触摸手势事件处理方法。
	 *
	 * @param direction 不断识别触摸手势的方向（@see 组件）。
	 */
	@SimpleEvent
	void 触摸手势(int direction);

	/* 组件属性 */

	/**
	 * 画笔颜色属性获取器方法。
	 *
	 * @return 画笔Alpha绘制RGB颜色。
	 */
	@SimpleProperty
	int 画笔颜色();

	/**
	 * 画笔颜色属性设置器方法。
	 *
	 * @param argb 画笔Alpha绘制RGB颜色。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = 组件.颜色_黑 + ""
	)
	void 画笔颜色(int argb);

	/**
	 * 画笔粗细属性获取器方法。
	 *
	 * @return 画笔粗细值。
	 */
	@SimpleProperty
	float 画笔粗细();

	/**
	 * 画笔粗细属性设置器方法。
	 *
	 * @param size 画笔粗细值。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_SINGLE,
		initializer = 1.0F + ""
	)
	void 画笔粗细(float size);

	/**
	 * 读取当前画笔的填充或描边类型。
	 *
	 * @return 画笔_描边或画笔_填充。
	 */
	@SimpleProperty
	int 画笔类型();

	/**
	 * 设置图形及文字使用的画笔类型。
	 *
	 * @param style 画笔_描边或画笔_填充，其它值无效。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_INTEGER,
		initializer = "1"
	)
	void 画笔类型(int style);

	/**
	 * 读取绘制文字使用的字号。
	 *
	 * @return 以 px 为单位的字号，不随系统字体缩放。
	 */
	@SimpleProperty
	float 字体大小();

	/**
	 * 设置绘制文字使用的字号，不影响已绘制内容。
	 *
	 * @param size 必须为正数且有限，单位 px。
	 */
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_SINGLE,
		initializer = "16.0"
	)
	void 字体大小(float size);

	/* 组件方法 */

	/**
	 * 清除画板（用背景颜色填充）。
	 */
	@SimpleFunction
	void 清空();

	/**
	 * 在画板上的给定坐标处绘制点。
	 *
	 * @param x x坐标
	 * @param y y坐标
	 */
	@SimpleFunction
	void 画点(int x, int y);

	/**
	 * 在画板上给定的坐标，用给定的半径画一个圆。
	 *
	 * @param x x坐标
	 * @param y y坐标
	 * @param radius 非负且有限的半径，单位 px。
	 */
	@SimpleFunction
	void 画圆(int x, int y, float radius);

	/**
	 * 在画板上给定的坐标之间绘制一条直线。
	 *
	 * @param x1 第一点的x坐标
	 * @param y1 第一点的y坐标
	 * @param x2 第二点的x坐标
	 * @param y2 第二点的y坐标
	 */
	@SimpleFunction
	void 画直线(int x1, int y1, int x2, int y2);

	/**
	 * 按左上角和宽高绘制矩形，填充或描边由画笔类型决定。
	 *
	 * @param x 左边横坐标，单位 px。
	 * @param y 顶边纵坐标，单位 px。
	 * @param width 矩形宽度，非负 px。
	 * @param height 矩形高度，非负 px。
	 */
	@SimpleFunction
	void 画矩形(int x, int y, int width, int height);

	/**
	 * 绘制圆角矩形，填充或描边由画笔类型决定。
	 *
	 * @param x 左边横坐标，单位 px。
	 * @param y 顶边纵坐标，单位 px。
	 * @param width 矩形宽度，非负 px。
	 * @param height 矩形高度，非负 px。
	 * @param radius 非负且有限的圆角半径，单位 px。
	 */
	@SimpleFunction
	void 画圆角矩形(int x, int y, int width, int height, float radius);

	/**
	 * 在包围矩形内绘制椭圆弧或扇形，角度单位为度。
	 *
	 * @param x 包围矩形左边，单位 px。
	 * @param y 包围矩形顶边，单位 px。
	 * @param width 包围矩形宽度，非负 px。
	 * @param height 包围矩形高度，非负 px。
	 * @param startAngle 有限的起始角度，正右方为 0 度。
	 * @param sweepAngle 有限的扫过角度，正数为顺时针。
	 * @param useCenter 真表示连接圆心形成扇形。
	 */
	@SimpleFunction
	void 画弧形(int x, int y, int width, int height, float startAngle, float sweepAngle, boolean useCenter);

	/**
	 * 使用一个控制点绘制二次贝塞尔曲线，沿用当前画笔类型。
	 *
	 * @param startX 起点横坐标，单位 px。
	 * @param startY 起点纵坐标，单位 px。
	 * @param controlX 控制点横坐标，单位 px。
	 * @param controlY 控制点纵坐标，单位 px。
	 * @param endX 终点横坐标，单位 px。
	 * @param endY 终点纵坐标，单位 px。
	 */
	@SimpleFunction
	void 画贝塞尔曲线(int startX, int startY, int controlX, int controlY, int endX, int endY);

	/**
	 * 按当前画笔颜色、类型和字体大小绘制单行文字。
	 *
	 * @param x 文字左边横坐标，单位 px。
	 * @param y 文字基线纵坐标，单位 px，并非文字顶边。
	 * @param text 单行文字；换行符不会自动产生多行布局。
	 */
	@SimpleFunction
	void 画文字(int x, int y, String text);

	/**
	 * 按图片原始像素尺寸绘制，不按屏幕密度自动缩放。
	 *
	 * @param path 资产文件名或绝对文件路径，不支持网址或 content URI。
	 * @param x 目标左边横坐标，单位 px。
	 * @param y 目标顶边纵坐标，单位 px。
	 * @throws simple.runtime.errors.文件读写错误 图片无法读取、解码或保存。
	 */
	@SimpleFunction
	void 画图片(String path, int x, int y);

	/**
	 * 将图片缩放到指定宽高后绘制，不自动保持宽高比例。
	 *
	 * @param path 资产文件名或绝对文件路径，不支持网址或 content URI。
	 * @param x 目标左边横坐标，单位 px。
	 * @param y 目标顶边纵坐标，单位 px。
	 * @param width 目标宽度，必须为正数，单位 px。
	 * @param height 目标高度，必须为正数，单位 px。
	 * @throws simple.runtime.errors.文件读写错误 图片无法读取、解码或保存。
	 */
	@SimpleFunction
	void 画图片(String path, int x, int y, int width, int height);

	/**
	 * 读取布局完成后的画板像素；坐标越界时抛出索引超出界限错误。
	 *
	 * @param x 横坐标，范围为 0 至实际画板宽度减 1。
	 * @param y 纵坐标，范围为 0 至实际画板高度减 1。
	 * @return 指定位置的 ARGB 颜色，包含画板背景与已绘制内容，不含边距或其它组件。
	 * @throws simple.runtime.errors.索引超出界限错误 坐标不在画板范围内。
	 */
	@SimpleFunction
	int 取点颜色(int x, int y);

	/**
	 * 以 PNG 保存布局完成后的完整画板，保留透明度，覆盖同名文件。
	 *
	 * @param path 目标路径；相对路径以应用私有 files 目录为基准，父目录须已存在。
	 * @throws simple.runtime.errors.文件读写错误 图片无法读取、解码或保存。
	 */
	@SimpleFunction
	void 保存画面(String path);

	/**
	 * 保存布局完成后的完整画板；JPEG 质量为 100，并以白色衬底处理透明像素。
	 *
	 * @param path 目标路径；相对路径以应用私有 files 目录为基准，父目录须已存在。
	 * @param format 图片格式_PNG 或图片格式_JPEG；文件后缀不会自动决定格式。
	 * @throws simple.runtime.errors.文件读写错误 图片无法读取、解码或保存。
	 */
	@SimpleFunction
	void 保存画面(String path, int format);

	/* 覆盖属性默认值 */

	@Override
	@SimpleProperty(
		type = SimpleProperty.PROPERTY_TYPE_COLOR,
		initializer = 组件.颜色_白 + ""
	)
	void 背景颜色(int argb);
}
