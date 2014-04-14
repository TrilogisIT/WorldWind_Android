package gov.nasa.worldwind.util.pkm;

import gov.nasa.worldwind.util.Logging;
import java.io.IOException;
import java.io.InputStream;
import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.ETC1Util.ETC1Texture;

/**
 * @author nicastel
 * @version $Id: PKMReader.java 2014-14-04 ndorigatti $
 */
public class PKMReader {

    public PKMReader() {
    }
    
	public PKMGpuTextureData read(InputStream stream) throws IOException {
	    if (stream == null)
        {
            String msg = Logging.getMessage("nullValue.InputStreamIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }
		try {
			stream.mark(1024);
			ETC1Texture texture = ETC1Util.createTexture(stream);

            if (texture != null) {
                //Estimate the memory size
                int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE + texture.getHeight() * texture.getWidth() / 2;
                return PKMGpuTextureData.fromETCCompressedData(texture, estimatedMemorySize);
            } else {
                return null;
            }
		} catch (IOException e) {
			//stream.reset();
			//texture = encodeTexture(stream);
			//e.printStackTrace();
			//System.out.println("Texture PKM failed ");
			String msg = Logging.getMessage("nullValue.InputStreamIOException");
			Logging.error(msg);			
			return null;
		}
	}

}
