/*
 * Copyright 2011 Peter Abeles
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package gecv.alg.wavelet;

import gecv.core.image.border.BorderIndex1D;
import gecv.struct.image.ImageBase;
import gecv.struct.image.ImageDimension;
import gecv.struct.wavelet.WlBorderCoef;
import gecv.struct.wavelet.WlCoef;
import gecv.struct.wavelet.WlCoef_F32;


/**
 * @author Peter Abeles
 */
public class UtilWavelet {

	/**
	 * The original image can have an even or odd number of width/height.  While the transformed
	 * image must have an even number of pixels.  If the original image is even then the sames
	 * are the same, otherwise the transformed image's shape is rounded up.
	 *
	 * @param original Original input image.
	 * @param transformed Image which has been transformed.
	 */
	public static void checkShape( ImageBase original , ImageBase transformed )
	{
		if( transformed.width % 2 == 1 || transformed.height % 2 == 1 )
			throw new IllegalArgumentException("Image containing the wavelet transform must have an even width and height.");

		int w = original.width + original.width%2;
		int h = original.height + original.height%2;

		if( transformed.width < w || transformed.height < h)
			throw new IllegalArgumentException("Transformed image must be larger than the original image. " +
					"("+w+","+h+") vs ("+transformed.width+","+transformed.height+")");
	}


	public static void checkShape( WlCoef_F32 desc , ImageBase original , ImageBase transformed , int level )
	{
		ImageDimension tranDim = UtilWavelet.transformDimension(original,level);

		if( transformed.width != tranDim.width || transformed.height != tranDim.height ) {
			throw new IllegalArgumentException("Image containing the wavelet transform must be "+tranDim.width+" x "+tranDim.height);
		}

		if( original.width < desc.scaling.length || original.height < desc.scaling.length )
			throw new IllegalArgumentException("Original image's width and height must be large enough the number of scaling coefficients.");

		if( original.width < desc.wavelet.length || original.height < desc.wavelet.length )
			throw new IllegalArgumentException("Original image's width and height must be large enough the number of wavelet coefficients.");
	}
	
	public static int computeScale( int level ) {
		if( level <= 1 )
			return 1;
		return (int)Math.pow(2,level-1);
	}

	/**
	 * Returns the number that the output image needs to be divisible by.
	 */
	public static int computeDiv( int level ) {
		if( level <= 1 )
			return 2;
		return (int)Math.pow(2,level-1);
	}

	public static ImageDimension transformDimension( ImageBase orig , int level )
	{
		return transformDimension(orig.width,orig.height,level);
	}
	public static ImageDimension transformDimension( int width , int height , int level )
	{
		int div = computeDiv(level);
		int w = width%div;
		int h = height%div;
		width += w > 0 ? div-w : 0;
		height += h > 0 ? div-h : 0;

		return new ImageDimension(width,height);
	}

	/**
	 * <p>
	 * Compute the energy of the specified array.
	 * </p>
	 *
	 * <p>
	 * E = sum( i=1..N , a[i]*a[i] )
	 * </p>
	 */
	public static double computeEnergy( float []array  ) {
		double total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i]*array[i];
		}

		return total;
	}

	/**
	 * <p>
	 * Compute the energy of the specified array.
	 * </p>
	 *
	 * <p>
	 * E = sum( i=1..N , a[i]*a[i] ) / (N*d*d)
	 * </p>
	 */
	public static double computeEnergy( int []array  , int denominator) {
		double total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i]*array[i];
		}

		total /= denominator*denominator;

		return total;
	}

	public static double sumCoefficients( float []array  ) {
		double total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i];
		}

		return total;
	}

	public static int sumCoefficients( int []array  ) {
		int total = 0;

		for( int i = 0; i < array.length; i++ ) {
			total += array[i];
		}

		return total;
	}

	/**
	 * Returns the lower border for a forward wavelet transform.
	 */
	public static int borderForwardLower( WlCoef desc ) {
		int ret =  -Math.min(desc.offsetScaling,desc.offsetWavelet);

		return ret + (ret % 2);
	}

	/**
	 * Returns the upper border (offset from image edge) for a forward wavelet transform.
	 */
	public static int borderForwardUpper( WlCoef desc , int dataLength) {
		int w = Math.max( desc.offsetScaling+desc.getScalingLength() , desc.offsetWavelet+desc.getWaveletLength());

		return Math.max((w + (w%2))-2,0) + dataLength%2;
	}

	/**
	 * Returns the lower border for an inverse wavelet transform.
	 */
	public static int borderInverseLower( WlBorderCoef<?> desc, BorderIndex1D border  ) {

		WlCoef inner = desc.getInnerCoefficients();
		int borderSize = borderForwardLower(inner);

		WlCoef ll = borderSize > 0 ? inner : null;
		WlCoef lu = ll;
		WlCoef uu = inner;
		int indexLU = 0;

		if( desc.getLowerLength() > 0 ) {
			ll = desc.getBorderCoefficients(0);
			indexLU = desc.getLowerLength()*2-2;
			lu = desc.getBorderCoefficients(indexLU);
		}

		if( desc.getUpperLength() > 0 ) {
			uu = desc.getBorderCoefficients(-2);
		}

		border.setLength(2000);
		borderSize = checkInverseLower(ll,0,border,borderSize);
		borderSize = checkInverseLower(lu,indexLU,border,borderSize);
		borderSize = checkInverseLower(uu,1998,border,borderSize);

		return borderSize;
	}

	public static int checkInverseLower( WlCoef coef, int index , BorderIndex1D border , int current ) {
		if( coef == null )
			return current;

		// how far up and down the coefficients go
		int a = index + Math.max( coef.getScalingLength()+coef.offsetScaling,coef.getWaveletLength()+coef.offsetScaling);
		int b = index + Math.min( coef.offsetScaling , coef.offsetWavelet ) -1;
		// the above -1 is needed because the lower bound is becoming an upper bound.
		// lower bounds are inclusive and upper bounds are exclusive

		// take in account the border
		a = border.getIndex(a);
		b = border.getIndex(b);

		if( a > 1000 ) a = -1;
		if( b > 1000 ) b = -1;

		a = Math.max(a,b);
		a += a%2;

		return Math.max(a,current);
	}

	/**
	 * Returns the upper border (offset from image edge) for an inverse wavelet transform.
	 */
	public static int borderInverseUpper( WlBorderCoef<?> desc , BorderIndex1D border, int dataLength ) {

		WlCoef inner = desc.getInnerCoefficients();
		int borderSize = borderForwardUpper(inner,dataLength);
		borderSize += borderSize%2;

		WlCoef uu = borderSize > 0 ? inner : null;
		WlCoef ul = uu;
		WlCoef ll = inner;
		int indexUL = 1998;

		if( desc.getUpperLength() > 0 ) {
			uu = desc.getBorderCoefficients(-2);
			indexUL = 2000-desc.getUpperLength()*2;
			ul = desc.getBorderCoefficients(2000-indexUL);
		}

		if( desc.getLowerLength() > 0 ) {
			ll = desc.getBorderCoefficients(0);
		}

		border.setLength(2000);
		int initial = borderSize;
		borderSize = checkInverseUpper(uu,1998,border,borderSize);
		borderSize = checkInverseUpper(ul,indexUL,border,borderSize);
		borderSize = checkInverseUpper(ll,0,border,borderSize);

//		if( borderSize != initial ) {
//			borderSize += 2*(dataLength%2);
//		}

		return borderSize;
	}

	public static int checkInverseUpper( WlCoef coef, int index , BorderIndex1D border , int current ) {
		if( coef == null )
			return current;

		// how far up and down the coefficients go
		int a = index + Math.max( coef.getScalingLength()+coef.offsetScaling,coef.getWaveletLength()+coef.offsetScaling)-1;
		int b = index + Math.min( coef.offsetScaling , coef.offsetWavelet );
		// the plus 1 for 'a' is needed because the lower bound is inclusive not exclusive

		// take in account the border
		a = border.getIndex(a);
		b = border.getIndex(b);

		if( a < 1000 ) a = 10000;
		if( b < 1000 ) b = 10000;

		a = 2000-Math.min(a,b);
		a += a%2;

		return Math.max(a,current);
	}
}