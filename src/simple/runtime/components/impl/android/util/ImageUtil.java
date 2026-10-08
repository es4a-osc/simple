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

package simple.runtime.components.impl.android.util;

import java.io.IOException;

import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;


/**
 * 图片辅助方法。
 *
 * @author 树先生 xhwsd@qq.com
 */
public class ImageUtil {


	private ImageUtil() {
	}

	/**
	 * 取图片可绘制对象。
	 *
	 * @param image 图片标识，支持资源标识、{@code @drawable/资源名}、{@code 资产文件名}、{@code /外部文件名}。
	 * @param context 活动上下文。
	 * @return 返回可绘制对象，如果{@code image}为空或空文本返回{@code null}。
	 * @throws IOException 如果找不到图片。
	 */
	public static Drawable getDrawable(String image) throws IOException {
		// 默认空文本属性
		if (image == null || image.isEmpty()) {
			return null;
		}

		// Res图片索引
		try {
			int resid = Integer.parseInt(image);
			if (resid > 0) {
				return ResourcesUtil.getResourcesDrawable(resid);
			}
		} catch (Exception e) {
			// 后续尝试
		}

		// Res图片路径
		if (ResourcesUtil.isDrawablePath(image)) {
			try {
				return ResourcesUtil.getResourcesDrawable(image);
			} catch (NotFoundException e) {
				// 抛出统一异常
				throw new IOException(image);
			}
		}

		// Assets图片文件名
		if (AssetsUtil.isAssetsPath(image)) {
			return AssetsUtil.getAssetsDrawable(image);
		}

		// 外部图片文件名，注意会受权限影响
		if (FileUtil.isFilePath(image)) {
			return FileUtil.getFileDrawable(image);
		}

		// 抛出异常
		throw new IOException(image);
	}
}
