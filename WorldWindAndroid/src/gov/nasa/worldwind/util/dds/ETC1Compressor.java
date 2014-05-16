/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.util.dds;

import gov.nasa.worldwind.util.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nicastel.renderscripttexturecompressor.etc1.rs.RsETC1;
import nicastel.renderscripttexturecompressor.etc1.rs.ScriptC_etc1compressor;
import android.graphics.Bitmap;
import android.support.v8.renderscript.RenderScript;

/**
 * @author nicastel
 * @author dcollins
 * @version $Id: DXT1Compressor.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class ETC1Compressor implements DXTCompressor
{
    public ETC1Compressor()
    {
    }

    public int getDXTFormat()
    {
        return DDSConstants.D3DFMT_ETC1;
    }

    public int getCompressedSize(Bitmap image, DXTCompressionAttributes attributes)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        // TODO: comment, provide documentation reference

        int width = Math.max(image.getWidth(), 4);
        int height = Math.max(image.getHeight(), 4);

        return (width * height) / 2;
    }
    
    public static RenderScript rs;
    public static ScriptC_etc1compressor script;

    public void compressImage(Bitmap image, DXTCompressionAttributes attributes,
        java.nio.ByteBuffer buffer)
    {
        if (image == null)
        {
            String message = Logging.getMessage("nullValue.ImageIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (attributes == null)
        {
            String message = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        
        // TODO
	   	int width = Math.max(image.getWidth(), 4);
	    int height = Math.max(image.getHeight(), 4);
        
        int encodedImageSize = RsETC1.getEncodedDataSize(width, height);
        System.out.println("encodedImageSize : "+encodedImageSize);
        
        // TODO
    	ByteBuffer bufferIn = ByteBuffer.allocateDirect(
    			image.getRowBytes() * image.getHeight()).order(
				ByteOrder.nativeOrder());
    	image.copyPixelsToBuffer(bufferIn);
    	bufferIn.rewind();       
    	
    	ByteBuffer bufferOut = ByteBuffer.allocateDirect(encodedImageSize);
    	

        
        RsETC1.encodeImage(rs, script, bufferIn, image.getWidth(), image.getHeight(), image.getRowBytes()/image.getWidth(), image.getRowBytes(), bufferOut);
        
        bufferOut.rewind();   
        
        buffer.put(bufferOut);        
    }

    protected ColorBlockExtractor getColorBlockExtractor(Bitmap image)
    {
        return new BasicColorBlockExtractor(image);
    }
}
