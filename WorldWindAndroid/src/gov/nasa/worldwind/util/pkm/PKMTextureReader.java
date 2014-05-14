package gov.nasa.worldwind.util.pkm;

import java.io.IOException;
import java.io.InputStream;

import android.opengl.ETC1;
//import android.opengl.ETC1;
import android.opengl.ETC1Util;
import android.opengl.ETC1Util.ETC1Texture;

public class PKMTextureReader {

	public PKMGpuTextureData read(InputStream stream) throws IOException {
		try {
			stream.mark(1024);
			ETC1Texture texture = ETC1Util.createTexture(stream);

			if (texture != null) {
				int estimatedMemorySize = ETC1.ETC_PKM_HEADER_SIZE
						+ texture.getHeight() * texture.getWidth() / 2;
				return PKMGpuTextureData.fromPKMETC1CompressedData(texture,
						estimatedMemorySize);
			} else {
				return null;
			}
		} catch (IOException e) {
//			stream.reset();
//			return encodeTexture(stream);
			e.printStackTrace();
			System.out.println("Texture PKM failed ");
			return null;
		}
	}
}
