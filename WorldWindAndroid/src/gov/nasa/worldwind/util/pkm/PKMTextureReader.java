package gov.nasa.worldwind.util.pkm;

import java.io.IOException;
import java.io.InputStream;

import nicastel.renderscripttexturecompressor.etc1.java.JavaETC1Util;
import nicastel.renderscripttexturecompressor.etc1.java.JavaETC1Util.ETC1Texture;
import android.opengl.ETC1;

public class PKMTextureReader {

	public PKMGpuTextureData read(InputStream stream) throws IOException {
		try {
			stream.mark(1024);
			ETC1Texture texture = JavaETC1Util.createTexture(stream);

			if (texture != null) {
				int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
						+ texture.getHeight() * texture.getWidth() / 2;
				return PKMGpuTextureData.fromETCCompressedData(texture,
						estimatedMemorySize);
			} else {
				return null;
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Texture PKM failed ");
			return null;
		}
	}
	
}
