/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util.pkm;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.opengl.ETC1Util.ETC1Texture;
import gov.nasa.worldwind.render.GpuTextureData;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWIO;
import gov.nasa.worldwind.util.WWUtil;

/**
 * @author nicastel
 * @version $Id: PKMGpuTextureData.java 2014-14-04 ndorigatti $
 */
public class PKMGpuTextureData extends GpuTextureData
{
    protected ETC1Texture etcCompressedData;
    
    public static PKMGpuTextureData fromETCCompressedData(ETC1Texture etctex, long estimatedMemorySize) {
        if (etctex == null || etctex.getHeight() == 0) {
            String msg = Logging.getMessage("nullValue.ETCTextureNotValid"); 
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (estimatedMemorySize <= 0) {
            String msg = Logging.getMessage("generic.SizeIsInvalid", estimatedMemorySize);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        PKMGpuTextureData textureData = new PKMGpuTextureData();
        textureData.etcCompressedData = etctex;
        textureData.estimatedMemorySize = estimatedMemorySize;

        return textureData;
    }

    protected PKMGpuTextureData() {
        super();
    }

	public ETC1Texture getEtcCompressedData() {
		return etcCompressedData;
	}
	
	 public static GpuTextureData createTextureData(Object source)
	    {
	        if (WWUtil.isEmpty(source))
	        {
	            String msg = Logging.getMessage("nullValue.SourceIsNull");
	            Logging.error(msg);
	            throw new IllegalArgumentException(msg);
	        }

	        GpuTextureData data = null;

	        try
	        {
	            if (source instanceof Bitmap)
	            {
	                data = new GpuTextureData((Bitmap) source, estimateMemorySize((Bitmap) source));
	            }
	            else
	            {
	                // Attempt to open the source as an InputStream. This handle URLs, Files, InputStreams, a String
	                // containing a valid URL, a String path to a file on the local file system, and a String path to a
	                // class path resource.
	                InputStream stream = WWIO.openStream(source);
	                try
	                {
	                    if (stream != null)
	                    {
	                        // Wrap the stream in a BufferedInputStream to provide the mark/reset capability required to
	                        // avoid destroying the stream when it is read more than once. BufferedInputStream also improves
	                        // file read performance.
	                        if (!(stream instanceof BufferedInputStream))
	                            stream = new BufferedInputStream(stream);
	                        data = fromStream(stream);
	                    }
	                }
	                finally
	                {
	                    WWIO.closeStream(stream, source.toString()); // This method call is benign if the stream is null.
	                }
	            }
	        }
	        catch (Exception e)
	        {
	            String msg = Logging.getMessage("GpuTextureFactory.TextureDataCreationFailed", source);
	            Logging.error(msg);
	        }

	        return data;
	    }
	
    protected static GpuTextureData fromStream(InputStream stream)
    {
        GpuTextureData data = null;
        try
        {
            stream.mark(DEFAULT_MARK_LIMIT);
          	 
            PKMReader pkmReader = new PKMReader();
            data = pkmReader.read(stream);
            if (data != null)
                return data;

            stream.reset(); 

            Bitmap bitmap = BitmapFactory.decodeStream(stream);
            return bitmap != null ? new GpuTextureData(bitmap, estimateMemorySize(bitmap)) : null;
        }
        catch (IOException e)
        {
            // TODO
        }

        return data;
    }
}
