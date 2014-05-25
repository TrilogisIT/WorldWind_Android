/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util;

import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

import static gov.nasa.worldwind.WorldWindowImpl.glCheckError;

import static android.opengl.GLES20.*;

/**
 * Created by kedzie on 5/9/14.
 */
public class OGLUtil {

	/**
	 * Sets the GL blending state according to the specified color mode. If <code>havePremultipliedColors</code> is
	 * true, this applies a blending function appropriate for colors premultiplied by the alpha component. Otherwise,
	 * this applies a blending function appropriate for non-premultiplied colors.
	 *
	 * @param havePremultipliedColors true to configure blending for colors premultiplied by the alpha components, and
	 *                                false to configure blending for non-premultiplied colors.
	 *
	 * @throws IllegalArgumentException if the GL is null.
	 */
	public static void applyBlending(boolean havePremultipliedColors)
	{
		if (havePremultipliedColors)
		{
			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
			glCheckError("glBlendFunc");
		}
		else
		{
			// The separate blend function correctly handles regular (non-premultiplied) colors. We want
			//     Cd = Cs*As + Cf*(1-As)
			//     Ad = As    + Af*(1-As)
			// So we use GL_EXT_blend_func_separate to specify different blending factors for source color and source
			// alpha.
//			String extensions = glGetString(GL_EXTENSIONS);

//			boolean haveExtBlendFuncSeparate = isExtensionAvailable(GL_EXT_BLEND_FUNC_SEPARATE);
//			if (haveExtBlendFuncSeparate)
//			{
				glBlendFuncSeparate(
						GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, // rgb   blending factors
						GL_ONE, GL_ONE_MINUS_SRC_ALPHA);      // alpha blending factors
				glCheckError("glBlendFuncSeparate");
//			}
//			else
//			{
//				glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
//			}
		}
	}

	/**
	* @param internalFormat the OpenGL texture internal format.
	* @param width          the texture width, in pixels.
	* @param height         the texture height, in pixels.
	* @param includeMipmaps true to include the texture's mip map data in the estimated size; false otherwise.
	*
	* @return a pixel format corresponding to the texture internal format, or 0 if the internal format is not
	*         recognized.
	*
	* @throws IllegalArgumentException if either the width or height is less than or equal to zero.
	*/
	public static long estimateMemorySize(int internalFormat, int pixelType, int width, int height, boolean includeMipmaps)
	{
		if (width < 0)
		{
			String message = Logging.getMessage("Geom.WidthInvalid", width);
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (height < 0)
		{
			String message = Logging.getMessage("Geom.HeightInvalid", height);
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		int numPixels = width * height;

		// Add the number of pixels from each level in the mipmap chain to the total number of pixels.
		if (includeMipmaps)
		{
			int maxLevel = Math.max((int) WWMath.logBase2(width), (int) WWMath.logBase2(height));
			for (int level = 1; level <= maxLevel; level++)
			{
				int w = Math.max(width >> level, 1);
				int h = Math.max(height >> level, 1);
				numPixels += w * h;
			}
		}

		switch(internalFormat) {
			case GL_ALPHA:
			case GL_LUMINANCE:
				// Alpha and luminance pixel data is always stored as 1 byte per pixel. See OpenGL ES Specification, version 2.0.25,
				// section 3.6.2, table 3.4.
				return numPixels;
			case GL_LUMINANCE_ALPHA:
				// Luminance-alpha pixel data is always stored as 2 bytes per pixel. See OpenGL ES Specification,
				// version 2.0.25, section 3.6.2, table 3.4.
				return 2 * numPixels; // Type must be GL_UNSIGNED_BYTE.
			case GL_RGB:
				// RGB pixel data is stored as either 2 or 3 bytes per pixel, depending on the type used during texture
				// image specification. See OpenGL ES Specification, version 2.0.25, section 3.6.2, table 3.4.
				// Default to type GL_UNSIGNED_BYTE.
				return (pixelType == GLES20.GL_UNSIGNED_SHORT_5_6_5 ? 2 : 3) * numPixels;
			default:	 // Default to internal format GL_RGBA.
				// RGBA pixel data is stored as either 2 or 4 bytes per pixel, depending on the type used during texture
				// image specification. See OpenGL ES Specification, version 2.0.25, section 3.6.2, table 3.4.
				// Default to type GL_UNSIGNED_BYTE.
				return ((pixelType == GLES20.GL_UNSIGNED_SHORT_4_4_4_4 || pixelType == GLES20.GL_UNSIGNED_SHORT_5_5_5_1) ? 2 : 4) * numPixels;
		}
	}
}
