package nicastel.renderscripttexturecompressor.etc1.rs;

import java.nio.ByteBuffer;

import nicastel.renderscripttexturecompressor.etc1.rs.ScriptC_etc1compressor;

import android.support.v8.renderscript.RenderScript;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Element;

public class RsETC1 {
	// Copyright 2009 Google Inc.
	// 2011 Nicolas CASTEL
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
	 * Size in bytes of a decoded block.
	 */
	public static final int DECODED_BLOCK_SIZE = 48;

	/**
	 * Size of a PKM file header, in bytes.
	 */
	public static final int ETC_PKM_HEADER_SIZE = 16;

	/**
	 * Accepted by the internalformat parameter of glCompressedTexImage2D.
	 */
	public static final int ETC1_RGB8_OES = 0x8D64;

	short etc1_byte;
	int etc1_bool;
	/* unsigned */long etc1_uint32;


	static short convert4To8(int b) {
		int c = b & 0xf;
		return (short) ((c << 4) | c);
	}

	static short convert4To8(long b) {
		long c = b & 0xf;
		return (short) ((c << 4) | c);
	}

	static short convert5To8(int b) {
		int c = b & 0x1f;
		return (short) ((c << 3) | (c >> 2));
	}

	static short convert5To8(long b) {
		long c = b & 0x1f;
		return (short) ((c << 3) | (c >> 2));
	}

	static short convert6To8(int b) {
		int c = b & 0x3f;
		return (short) ((c << 2) | (c >> 4));
	}

	static short convert6To8(long b) {
		long c = b & 0x3f;
		return (short) ((c << 2) | (c >> 4));
	}

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
	 */
	public static int encodeImage(RenderScript rs, ScriptC_etc1compressor script, ByteBuffer pIn, int width, int height,
			int pixelSize, int stride, ByteBuffer compressedImage) {
		
		long tInitArray = java.lang.System.currentTimeMillis();
		
		script.set_height(height);
		script.set_width(width);
		
		if (pixelSize < 2 || pixelSize > 3) {
			System.out.println("unsupported pixelSize");
			return -1;
		}

		// int iOut = 0;
		
		int size = width * height / (DECODED_BLOCK_SIZE / 3);
		
		Allocation p00 = Allocation.createSized(rs, Element.U8(rs), width * height * pixelSize);
		Allocation aout = Allocation.createSized(rs, Element.U16_4(rs), size);

		tInitArray = java.lang.System.currentTimeMillis() - tInitArray;
		System.out.println("tInitArray : "+tInitArray+" ms");
		
		long tFillAlloc = java.lang.System.currentTimeMillis();
		p00.copyFrom(pIn.array());	
		
		script.bind_pInA(p00);
		
		tFillAlloc = java.lang.System.currentTimeMillis() - tFillAlloc;
		System.out.println("tFillAlloc : "+tFillAlloc+" ms");
		
		long tExec = java.lang.System.currentTimeMillis();
		script.forEach_root(aout);
		tExec = java.lang.System.currentTimeMillis() - tExec;
		System.out.println("tExec : "+tExec+" ms");
		
		long tFillOut = java.lang.System.currentTimeMillis();
		short[] arrayOut3Temp = new short[4*size];
		aout.copyTo(arrayOut3Temp);
		
		Allocation aout2 = Allocation.createSized(rs, Element.U8(rs), 8*size);
		aout2.copyFromUnchecked(arrayOut3Temp);
		
		aout2.copyTo(compressedImage.array());	
		aout2.destroy();
		
		tFillOut = java.lang.System.currentTimeMillis() - tFillOut;
		System.out.println("tFillOut : "+tFillOut+" ms");
		
		compressedImage.position(0);
		return 0;
	}

	static final byte kMagic[] = { 'P', 'K', 'M', ' ', '1', '0' };

	static final int ETC1_PKM_FORMAT_OFFSET = 6;
	static final int ETC1_PKM_ENCODED_WIDTH_OFFSET = 8;
	static final int ETC1_PKM_ENCODED_HEIGHT_OFFSET = 10;
	static final int ETC1_PKM_WIDTH_OFFSET = 12;
	static final int ETC1_PKM_HEIGHT_OFFSET = 14;

	static final int ETC1_RGB_NO_MIPMAPS = 0;

	static void writeBEUint16(ByteBuffer header, int iOut, int data) {
		header.position(iOut);
		header.put((byte) (data >> 8));
		header.put((byte) data);
	}

	static int readBEUint16(ByteBuffer headerBuffer, int iIn) {
		return (headerBuffer.get(iIn) << 8) | headerBuffer.get(iIn + 1);
	}

	// Format a PKM header

	public static void formatHeader(ByteBuffer header, int width, int height) {
		header.put(kMagic);
		int encodedWidth = (width + 3) & ~3;
		int encodedHeight = (height + 3) & ~3;
		writeBEUint16(header, ETC1_PKM_FORMAT_OFFSET, ETC1_RGB_NO_MIPMAPS);
		writeBEUint16(header, ETC1_PKM_ENCODED_WIDTH_OFFSET, encodedWidth);
		writeBEUint16(header, ETC1_PKM_ENCODED_HEIGHT_OFFSET, encodedHeight);
		writeBEUint16(header, ETC1_PKM_WIDTH_OFFSET, width);
		writeBEUint16(header, ETC1_PKM_HEIGHT_OFFSET, height);
	}

	// Check if a PKM header is correctly formatted.

	public static boolean isValid(ByteBuffer headerBuffer) {
		if (memcmp(headerBuffer, kMagic, kMagic.length)) {
			return false;
		}
		int format = readBEUint16(headerBuffer, ETC1_PKM_FORMAT_OFFSET);
		int encodedWidth = readBEUint16(headerBuffer,
				ETC1_PKM_ENCODED_WIDTH_OFFSET);
		int encodedHeight = readBEUint16(headerBuffer,
				ETC1_PKM_ENCODED_HEIGHT_OFFSET);
		int width = readBEUint16(headerBuffer, ETC1_PKM_WIDTH_OFFSET);
		int height = readBEUint16(headerBuffer, ETC1_PKM_HEIGHT_OFFSET);
		return format == ETC1_RGB_NO_MIPMAPS && encodedWidth >= width
				&& encodedWidth - width < 4 && encodedHeight >= height
				&& encodedHeight - height < 4;
	}

	static boolean memcmp(ByteBuffer headerBuffer, byte[] b, int lenght) {
		for (int i = 0; i < lenght; i++) {
			if (headerBuffer.get(i) != b[i]) {
				return true;
			}
		}
		return false;
	}

	// Read the image width from a PKM header

	public static int getWidth(ByteBuffer pHeader) {
		return readBEUint16(pHeader, ETC1_PKM_WIDTH_OFFSET);
	}

	// Read the image height from a PKM header

	public static int getHeight(ByteBuffer pHeader) {
		return readBEUint16(pHeader, ETC1_PKM_HEIGHT_OFFSET);
	}

}
