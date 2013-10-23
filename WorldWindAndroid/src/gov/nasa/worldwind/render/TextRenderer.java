package gov.nasa.worldwind.render;

import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Rect;
import gov.nasa.worldwind.util.Logging;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.HashMap;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.opengl.GLES20;

/**
 * Class used to render text on view
 * 
 * @author Nicola Dorigatti Trilogis SRL
 * @version 1
 */
public class TextRenderer {

	protected static final String VERTEX_SHADER_PATH = "shaders/TextRenderer.vert";
	protected static final String FRAGMENT_SHADER_PATH = "shaders/TextRenderer.frag";
	protected static final Object shaderKey = new Object();

	private HashMap<String, Object> textKeys = new HashMap<String, Object>();
	private DrawContext drawContext;
	private Paint paint;
	private float[] color = new float[] { 1, 1, 1, 1 };

	public TextRenderer(DrawContext dc, Paint paint) {
		this.drawContext = dc;
		this.paint = paint;
	}

	public Rect getBounds(String text) {
		android.graphics.Rect bounds = new android.graphics.Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		Rect retval = new Rect(0, 0, bounds.width(), bounds.height());
		return retval;
	}

	public void setColor(float[] color) {
		this.color = color;
	}

	public void draw(String text, int x, int y) {
		Rect viewport = drawContext.getView().getViewport();
		Rect bounds = getBounds(text);
		Matrix projection = Matrix.fromIdentity().setOrthographic(0d, viewport.width, 0d, viewport.height, -0.6 * bounds.width, 0.6 * bounds.width);
		Matrix modelview = Matrix.fromIdentity();
		modelview.multiplyAndSet(Matrix.fromTranslation(x, y, 0));
		modelview.multiplyAndSet(Matrix.fromScale(bounds.width, bounds.height, 1d));
		Matrix mvp = Matrix.fromIdentity().multiplyAndSet(projection, modelview);
		GpuProgram program = this.getGpuProgram(drawContext.getGpuResourceCache(), shaderKey, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
		program.bind();
		program.loadUniformMatrix("mvpMatrix", mvp);
		GLES20.glEnable(GLES20.GL_TEXTURE_2D);
		GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
		GpuTexture texture = getGpuTexture(text);
		texture.bind();
		program.loadUniform4f("uTextureColor", color[0], color[1], color[2], color[3]);
		program.loadUniformSampler("sTexture", 0);

		float[] unitQuadVerts = new float[] { 0, 0, 1, 0, 1, 1, 0, 1 };
		int pointLocation = program.getAttribLocation("vertexPoint");
		GLES20.glEnableVertexAttribArray(pointLocation);
		FloatBuffer vertexBuf = ByteBuffer.allocateDirect(unitQuadVerts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexBuf.put(unitQuadVerts);
		vertexBuf.rewind();
		GLES20.glVertexAttribPointer(pointLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuf);
		float[] textureVerts = new float[] { 0, 1, 1, 1, 1, 0, 0, 0 };
		int textureLocation = program.getAttribLocation("aTextureCoord");
		GLES20.glEnableVertexAttribArray(textureLocation);
		FloatBuffer textureBuf = ByteBuffer.allocateDirect(textureVerts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		textureBuf.put(textureVerts);
		textureBuf.rewind();
		GLES20.glVertexAttribPointer(textureLocation, 2, GLES20.GL_FLOAT, false, 0, textureBuf);
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, unitQuadVerts.length / 2);
		GLES20.glDisableVertexAttribArray(pointLocation);
		GLES20.glDisableVertexAttribArray(textureLocation);
		GLES20.glUseProgram(0);
		GLES20.glDisable(GLES20.GL_TEXTURE_2D);
	}

	protected GpuTexture getGpuTexture(String text) {
		GpuResourceCache cache = drawContext.getGpuResourceCache();
		Object key = textKeys.get(text);
		if (key == null) {
			key = new Object();
			textKeys.put(text, key);
		}
		GpuTexture texture = cache.getTexture(key);
		if (texture == null) {
			// TODO: load the texture on a non-rendering thread.
			texture = this.loadTextTexture(text);
			if (texture != null) // Don't add the texture to the cache if
									// texture creation failed.
			cache.put(key, texture);
		}

		return texture;
	}

	private GpuTexture loadTextTexture(String text) {
		android.graphics.Rect bounds = new android.graphics.Rect();
		paint.getTextBounds(text, 0, text.length(), bounds);
		Bitmap image = Bitmap.createBitmap(bounds.width() + 1, bounds.height() + 1, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(image);
		canvas.drawText(text, 0, bounds.height(), paint);

		GpuTexture texture = null;
		GpuTextureData textureData = GpuTextureData.createTextureData(image);
		// GpuTextureData textureData = BasicGpuTextureFactory.createTextureData(AVKey.GPU_TEXTURE_FACTORY, image, null);
		if (textureData != null) {
			texture = GpuTexture.createTexture(drawContext, textureData);
			// texture = BasicGpuTextureFactory.createTexture(AVKey.GPU_TEXTURE_FACTORY, drawContext, textureData, null);
		}
		return texture;
	}

	protected GpuProgram getGpuProgram(GpuResourceCache cache, Object programKey, String shaderPath, String fragmentPath) {

		GpuProgram program = cache.getProgram(programKey);

		if (program == null) {
			try {
				GpuProgram.GpuProgramSource source = GpuProgram.readProgramSource(shaderPath, fragmentPath);
				program = new GpuProgram(source);
				cache.put(programKey, program);
			} catch (Exception e) {
				String msg = Logging.getMessage("GL.ExceptionLoadingProgram", shaderPath, fragmentPath);
				Logging.error(msg);
			}
		}

		return program;
	}
}
