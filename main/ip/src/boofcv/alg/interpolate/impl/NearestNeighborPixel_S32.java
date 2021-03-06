/*
 * Copyright (c) 2011-2014, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.alg.interpolate.impl;

import boofcv.alg.interpolate.NearestNeighborPixel;
import boofcv.struct.image.ImageSInt32;
import boofcv.struct.image.ImageType;


/**
 * Performs nearest neighbor interpolation to extract values between pixels in an image.
 *
 * @author Peter Abeles
 */
public class NearestNeighborPixel_S32 extends NearestNeighborPixel<ImageSInt32> {

	private int data[];

	public NearestNeighborPixel_S32() {
	}

	public NearestNeighborPixel_S32(ImageSInt32 orig) {
		setImage(orig);
	}

	@Override
	public void setImage(ImageSInt32 image) {
		this.orig = image;
		this.data = orig.data;
		this.stride = orig.getStride();
		this.width = orig.getWidth();
		this.height = orig.getHeight();
	}

	@Override
	public float get_fast(float x, float y) {
		return data[ orig.startIndex + ((int)y)*stride + (int)x];
	}

	@Override
	public float get(float x, float y) {
		if (x < 0 || y < 0 || x > width-1 || y > height-1 )
			throw new IllegalArgumentException("Point is outside of the image");
		int xx = (int)x;
		int yy = (int)y;

		return data[ orig.startIndex + yy*stride + xx];
	}

	@Override
	public ImageType<ImageSInt32> getImageType() {
		return ImageType.single(ImageSInt32.class);
	}
}
