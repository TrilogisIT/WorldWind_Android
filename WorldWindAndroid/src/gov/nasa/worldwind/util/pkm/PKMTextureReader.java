package gov.nasa.worldwind.util.pkm;

import gov.nasa.worldwind.util.Logging;
import java.io.IOException;
import java.io.InputStream;
import nicastel.renderscripttexturecompressor.etc1.rs.RsETC1Util;
import nicastel.renderscripttexturecompressor.etc1.rs.RsETC1Util.ETC1Texture;
import android.opengl.ETC1;

/**
 * @author nicastel
 * @version $Id: PKMTextureReader.java 2014-14-04 ndorigatti $
 */
public class PKMTextureReader {

	public PKMGpuTextureData read(InputStream stream) throws IOException {
		if (stream == null) {
			String msg = Logging.getMessage("nullValue.InputStreamIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		try {
			stream.mark(1024);
			ETC1Texture texture = RsETC1Util.createTexture(stream);

			if (texture != null) {
				int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE + texture.getHeight() * texture.getWidth() / 2;
				return PKMGpuTextureData.fromPKMETC1CompressedData(texture, estimatedMemorySize);
			} else {
				return null;
			}
		} catch (IOException e) {
			// stream.reset();
			// return encodeTexture(stream);
			e.printStackTrace();
			System.out.println("Texture PKM failed ");
			String msg = Logging.getMessage("nullValue.InputStreamIOException");
			Logging.error(msg);
			return null;
		}
	}
}
