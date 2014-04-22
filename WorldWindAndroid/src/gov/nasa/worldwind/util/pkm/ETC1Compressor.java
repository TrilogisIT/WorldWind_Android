/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util.pkm;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.ETC1Util;
import gov.nasa.worldwind.WorldWindow;
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

	public static ETC1Util.ETC1Texture compressImageBuffer(ByteBuffer buffer) {
		return compressImage(BitmapFactory.decodeStream(WWIO.getInputStreamFromByteBuffer(buffer)));
	}

	public static ETC1Util.ETC1Texture compressImage(Bitmap image) {
		if(WorldWindow.DEBUG)
			Logging.verbose("Compressing ETC1 texture ");
		Bitmap bitmap565 = convert(image, Bitmap.Config.RGB_565);
		int size = bitmap565.getRowBytes() * bitmap565.getHeight();
		ByteBuffer bb = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
		bitmap565.copyPixelsToBuffer(bb);
		bb.position(0);
		final int width = bitmap565.getWidth();
		int pixelBytes = 2;
		return ETC1Util.compressTexture(bb, width, bitmap565.getHeight(), pixelBytes, pixelBytes * width);
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
}
