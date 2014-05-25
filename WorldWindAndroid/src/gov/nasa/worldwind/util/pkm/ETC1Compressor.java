/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util.pkm;

import android.graphics.*;
import android.opengl.ETC1Util;
import android.opengl.ETC1Util.ETC1Texture;
import gov.nasa.worldwind.WorldWindowImpl;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Compress ETC1 Images
 *
 * Created by kedzie on 4/14/14.
 */
public class ETC1Compressor {

	/**
	 * Compress Bitmap to ETC1 Texture without alpha channel
	 * @param buffer	buffer containing the image to compress, alpha channel is discarded
	 * @return array containing color texture
	 */
	public static ETC1Texture[] compressImageBuffer(ByteBuffer buffer) {
		return compressImageBuffer(buffer, false);
	}

	/**
	 * Compress Bitmap to ETC1 Texture, optionally with alpha channel in seperate texture
	 * @param buffer 	buffer containing the image to compress, with alpha channel (if applicable)
	 * @param extractAlpha	whether to extract alpha channel into seperate texture
	 * @return array containing color texture and alpha texture, if extracted alphaMap
	 */
	public static ETC1Texture[] compressImageBuffer(ByteBuffer buffer, boolean extractAlpha) {
		return compressImage(BitmapFactory.decodeStream(WWIO.getInputStreamFromByteBuffer(buffer)), extractAlpha);
	}

	/**
	 * Compress Bitmap to ETC1 Texture without alpha channel
	 * @param image	the image to compress, alpha channel is discarded
	 * @return array containing color texture
	 */
	public static ETC1Texture[] compressImage(Bitmap image) {
		return compressImage(image, false);
	}

	/**
	 * Compress Bitmap to ETC1 Texture, optionally with alpha channel in seperate texture
	 * @param image	the image to compress, with alpha channel (if applicable)
	 * @param extractAlpha	whether to extract alpha channel into seperate texture
	 * @return array containing color texture and alpha texture, if extracted alphaMap
	 */
	public static ETC1Texture[] compressImage(Bitmap image, boolean extractAlpha) {
		if(WorldWindowImpl.DEBUG)
			Logging.verbose("Compressing ETC1 texture ");
		ETC1Texture color = compressBitmap565(convert(image, Bitmap.Config.RGB_565));

		if(extractAlpha && image.hasAlpha()) {
			if(WorldWindowImpl.DEBUG)
				Logging.verbose("Extracting ETC1 alpha texture..");
			Bitmap alphaMap = extractAlpha(image);
			ETC1Texture alphaTex = compressBitmap565(alphaMap);
			return new ETC1Texture[] { color, alphaTex };
		} else {
			return new ETC1Texture[]{color};
		}
	}

	/**
	 * Compress RGB_565 bitmap
	 * @param bitmap565 Bitmap in RGB_565 format
	 * @return compressed texture
	 */
	private static ETC1Texture compressBitmap565(Bitmap bitmap565) {
		if(!bitmap565.getConfig().equals(Bitmap.Config.RGB_565))
			throw new IllegalArgumentException("Bitmap must be RGB_565");

		final int width = bitmap565.getWidth();
		final int height = bitmap565.getHeight();
		int size = bitmap565.getRowBytes() * height;
		ByteBuffer bb = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		bitmap565.copyPixelsToBuffer(bb);
		bb.position(0);
		int pixelBytes = 2;
		return ETC1Util.compressTexture(bb, width, height, pixelBytes, pixelBytes * width);
	}

	public static Bitmap convert(Bitmap input, Bitmap.Config format) {
		Bitmap output = Bitmap.createBitmap( input.getWidth(), input.getHeight(), format );
		Canvas c = new Canvas(output);
		Paint p = new Paint();
		p.setFilterBitmap(true);
		p.setDither(true);
		c.drawBitmap(input,0,0,p);
		input.recycle();
		return output;
	}

	protected static Bitmap extractAlpha(Bitmap input) {
		final int width = input.getWidth();
		final int height = input.getHeight();

		Bitmap alphaMap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

		int[] pixels = new int[width*height];
		input.getPixels(pixels, 0, width, 0, 0, width, height);
		for(int i=0; i<pixels.length; i++) {
			int alpha = Color.alpha(pixels[i]);
			pixels[i] = Color.rgb(alpha, alpha, alpha);
		}
		alphaMap.setPixels(pixels, 0, width, 0, 0, width, height);
		return alphaMap;
	}

	protected static Bitmap extractAlphaInline(Bitmap input) {
		final int width = input.getWidth();
		final int height = input.getHeight();

		Bitmap alphaMap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);

		for(int y=0; y<height; y++) {
			for(int x=0; x<width; x++) {
				int alpha = Color.alpha(input.getPixel(x,y));
				alphaMap.setPixel(x, y, Color.rgb(alpha, alpha, alpha));
			}
		}
		return alphaMap;
	}
}
