package nicastel.renderscripttexturecompressor.etc1.rs;

import java.nio.ByteBuffer;

import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Allocation.MipmapControl;
import android.support.v8.renderscript.Element;
import android.support.v8.renderscript.RenderScript;

public class RsETC1 {
	// Copyright 2009 Google Inc.
	// Copyright 2011 Nicolas CASTEL
	//
	// Licensed under the Apache License, Version 2.0 (the "License");
	// you may not use this file except in compliance with the License.
	// You may obtain a copy of the License at
	//
	// http://www.apache.org/licenses/LICENSE-2.0
	//
	// Unless required by applicable law or agreed to in writing, software
	// distributed under the License is distributed on an "AS IS" BASIS,
	// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
	// See the License for the specific language governing permissions and
	// limitations under the License.

	/**
	 * Size in bytes of an encoded block.
	 */
	public static final int ENCODED_BLOCK_SIZE = 8;

	/**
	 * Size in pixel of a decoded block.
	 */
	public static final int DECODED_BLOCK_SIZE = 48;

	/**
	 * Accepted by the internalformat parameter of glCompressedTexImage2D.
	 */
	public static final int ETC1_RGB8_OES = 0x8D64;

	/**
	 * Return the size of the encoded image data (does not include size of PKM
	 * header).
	 */
	public static int getEncodedDataSize(int width, int height) {
		return (((width + 3) & ~3) * ((height + 3) & ~3)) >> 1;
	}
	
	/**
	 * Encode an entire image. pIn - pointer to the image data. Formatted such
	 * that the Red component of pixel (x,y) is at pIn + pixelSize * x + stride
	 * * y + redOffset; pOut - pointer to encoded data. Must be large enough to
	 * store entire encoded image.
	 * @param script 
	 * @param containMipmaps 
	 */
	public static int encodeImage(RenderScript rs, ScriptC_etc1compressor script, Allocation aIn, int width, int height,
			int pixelSize, int stride, ByteBuffer compressedImage, boolean containMipmaps) {
		
		long tInitArray = java.lang.System.currentTimeMillis();
		
		script.set_height(height);
		script.set_width(width);
		script.set_containMipmaps(containMipmaps);
		script.set_pixelSize(pixelSize);
		
		if (pixelSize < 2 || pixelSize > 4) {
			return -1;
		}
		
		// int iOut = 0;
		
		int size = Math.max(aIn.getBytesSize() / ((DECODED_BLOCK_SIZE/3)*pixelSize), 1);
		Allocation aout = Allocation.createSized(rs, Element.U16_4(rs), size);

		tInitArray = java.lang.System.currentTimeMillis() - tInitArray;
		//System.out.println("tInitArray : "+tInitArray+" ms");
		
		long tFillAlloc = java.lang.System.currentTimeMillis();		
		script.bind_pInA(aIn);		
		tFillAlloc = java.lang.System.currentTimeMillis() - tFillAlloc;
		//System.out.println("tFillAlloc : "+tFillAlloc+" ms");
		
		long tExec = java.lang.System.currentTimeMillis();
		script.forEach_root(aout);
		tExec = java.lang.System.currentTimeMillis() - tExec;
		//System.out.println("tExec : "+tExec+" ms");
		
		long tFillOut = java.lang.System.currentTimeMillis();
		
		short[] arrayOut3Temp = new short[4*size];
		aout.copyTo(arrayOut3Temp);
		aout.destroy();
		
		Allocation aout2 = Allocation.createSized(rs, Element.U8(rs), 8*size);
		aout2.copyFromUnchecked(arrayOut3Temp);
		
		aout2.copyTo(compressedImage.array());	
		aout2.destroy();
		
		tFillOut = java.lang.System.currentTimeMillis() - tFillOut;
		
		compressedImage.rewind();
		
		//System.out.println("tFillOut : "+tFillOut+" ms");
		
		return 0;
	}
	
	

}
