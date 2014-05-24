/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package nicastel.renderscripttexturecompressor.dds;

import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.dds.BasicColorBlockExtractor;
import gov.nasa.worldwind.util.dds.ColorBlockExtractor;
import gov.nasa.worldwind.util.dds.DXTCompressionAttributes;
import gov.nasa.worldwind.util.dds.DXTCompressor;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import nicastel.renderscripttexturecompressor.etc1.rs.RsETC1;
import nicastel.renderscripttexturecompressor.etc1.rs.ScriptC_etc1compressor;
import android.graphics.Bitmap;
import android.support.v8.renderscript.Allocation;
import android.support.v8.renderscript.Allocation.MipmapControl;
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
        return ETCConstants.D3DFMT_ETC1;
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
//    	ByteBuffer bufferIn = ByteBuffer.allocateDirect(
//    			image.getRowBytes() * image.getHeight()).order(
//				ByteOrder.nativeOrder());
//    	image.copyPixelsToBuffer(bufferIn);
//    	bufferIn.rewind();       
        
        MipmapControl control = MipmapControl.MIPMAP_NONE;
        int usage = Allocation.USAGE_SHARED;
        if(attributes.isBuildMipmaps()) {
        	// Needs an ARGB 8888 Bitmap as input
        	control = MipmapControl.MIPMAP_FULL;
        	usage = Allocation.USAGE_SCRIPT;
        	
        }
    	
    	Allocation alloc = Allocation.createFromBitmap(rs, image, control, usage);
    	alloc.generateMipmaps();
    	
    	int pixelSize = image.getRowBytes()/image.getWidth();
    	
    	int encodedImageSize =  Math.max(alloc.getBytesSize() / ((RsETC1.DECODED_BLOCK_SIZE/3)*pixelSize), 1)*8;
        //System.out.println("encodedImageSize : "+encodedImageSize);
    	
    	ByteBuffer bufferOut = ByteBuffer.allocateDirect(encodedImageSize);
        
        RsETC1.encodeImage(rs, script, alloc, image.getWidth(), image.getHeight(), pixelSize, image.getRowBytes(), bufferOut, attributes.isBuildMipmaps());
        
        alloc.destroy();
        
        bufferOut.rewind();   
        
        buffer.put(bufferOut);        
    }

    protected ColorBlockExtractor getColorBlockExtractor(Bitmap image)
    {
        return new BasicColorBlockExtractor(image);
    }
}
