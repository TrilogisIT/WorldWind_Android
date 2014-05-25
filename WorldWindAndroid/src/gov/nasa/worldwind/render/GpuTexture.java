/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Disposable;
import gov.nasa.worldwind.WorldWindowImpl;
import gov.nasa.worldwind.cache.Cacheable;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.util.Logging;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLUtils;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 *
 * @author dcollins
 * @version $Id: GpuTexture.java 762 2012-09-07 00:22:58Z tgaskins $
 */
public class GpuTexture implements Cacheable, Disposable {

	public static GpuTexture createTexture(DrawContext dc, GpuTextureData textureData) {
		return createTexture(textureData);
	}

	public static GpuTexture createTexture(GpuTextureData textureData) {
		if (textureData == null) {
			String msg = Logging.getMessage("nullValue.TextureDataIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		GpuTexture texture = null;

		try {
			if (textureData.getBitmapData() != null) {
				texture = doCreateFromBitmapData(textureData);
			} else if (textureData.getCompressedData() != null) {
				texture = doCreateFromCompressedData(textureData);
			} else {
				String msg = Logging.getMessage("generic.TextureDataUnrecognized", textureData);
				Logging.error(msg);
			}
		} catch (Exception e) {
			String msg = Logging.getMessage("GpuTextureFactory.TextureCreationFailed", textureData);
			Logging.error(msg);
		}

		return texture;
	}

	protected static GpuTexture doCreateFromBitmapData(GpuTextureData data) throws Exception {
		Bitmap bitmap = data.getBitmapData().bitmap;

		int[] texture = new int[1];
		try {
			GLES20.glGenTextures(1, texture, 0);
			WorldWindowImpl.glCheckError("glGenTextures");
			if (texture[0] <= 0) {
				String msg = Logging.getMessage("GL.UnableToCreateObject", Logging.getMessage("term.Texture"));
				Logging.error(msg);
				return null;
			}

			// OpenGL ES provides support for non-power-of-two textures, including its associated mipmaps, provided that
			// the s and t wrap modes are both GL_CLAMP_TO_EDGE.
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
			WorldWindowImpl.glCheckError("glBindTexture");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR_MIPMAP_LINEAR);
			WorldWindowImpl.glCheckError("glTexParameteri");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			WorldWindowImpl.glCheckError("glTexParameteri");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			WorldWindowImpl.glCheckError("glTexParameteri");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			WorldWindowImpl.glCheckError("glTexParameteri");

			GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
			WorldWindowImpl.glCheckError("texImage2D");

			GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D);
			WorldWindowImpl.glCheckError("glGenerateMipmap");
		} catch (Exception e) {
			GLES20.glDeleteTextures(1, texture, 0);
			WorldWindowImpl.glCheckError("glDeleteTextures");
			throw e;
		} finally {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			WorldWindowImpl.glCheckError("glBindTexture");
		}

		return new GpuTexture(GLES20.GL_TEXTURE_2D, texture, bitmap.getWidth(), bitmap.getHeight(), data.getSizeInBytes(), createVerticalFlipTransform());
	}

	protected static GpuTexture doCreateFromCompressedData(GpuTextureData data) throws Exception {
		int format = data.getCompressedData().format;
		GpuTextureData.MipmapData[] levelData = data.getCompressedData().levelData;
		GpuTextureData.MipmapData[] alphaData = data.getCompressedData().alphaData;

		int[] texture = alphaData != null ? new int[2] : new int[1];
		try {
			GLES20.glGenTextures(texture.length, texture, 0);
			WorldWindowImpl.glCheckError("glGenTextures");
			if (texture[0] <= 0) {
				String msg = Logging.getMessage("GL.UnableToCreateObject", Logging.getMessage("term.Texture"));
				Logging.error(msg);
				return null;
			}

			// OpenGL ES provides support for non-power-of-two textures, including its associated mipmaps, provided that
			// the s and t wrap modes are both GL_CLAMP_TO_EDGE.
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
			WorldWindowImpl.glCheckError("glActiveTexture");
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[0]);
			WorldWindowImpl.glCheckError("glBindTexture");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, levelData.length > 1 ? GLES20.GL_LINEAR_MIPMAP_LINEAR : GLES20.GL_LINEAR);
			WorldWindowImpl.glCheckError("glTexParameteri");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			WorldWindowImpl.glCheckError("glTexParameteri");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
			WorldWindowImpl.glCheckError("glTexParameteri");
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
			WorldWindowImpl.glCheckError("glTexParameteri");

			for (int levelNum = 0; levelNum < levelData.length; levelNum++) {
				GpuTextureData.MipmapData level = levelData[levelNum];
				GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, levelNum, format, level.width, level.height, 0, level.buffer.remaining(), level.buffer);
				WorldWindowImpl.glCheckError("glCompressedTexImage2D");
			}

			if (alphaData != null) {
				GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
				WorldWindowImpl.glCheckError("glActiveTexture");
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture[1]);
				WorldWindowImpl.glCheckError("glBindTexture");
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, alphaData.length > 1 ? GLES20.GL_LINEAR_MIPMAP_LINEAR : GLES20.GL_LINEAR);
				WorldWindowImpl.glCheckError("glTexParameteri");
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
				WorldWindowImpl.glCheckError("glTexParameteri");
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
				WorldWindowImpl.glCheckError("glTexParameteri");
				GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
				WorldWindowImpl.glCheckError("glTexParameteri");

				for (int levelNum = 0; levelNum < alphaData.length; levelNum++) {
					GpuTextureData.MipmapData level = alphaData[levelNum];
					GLES20.glCompressedTexImage2D(GLES20.GL_TEXTURE_2D, levelNum, format, level.width, level.height, 0, level.buffer.remaining(), level.buffer);
					WorldWindowImpl.glCheckError("glCompressedTexImage2D");
				}
			}
		} catch (Exception e) {
			GLES20.glDeleteTextures(texture.length, texture, 0);
			WorldWindowImpl.glCheckError("glDeleteTextures");
			throw e;
		} finally {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			WorldWindowImpl.glCheckError("glBindTexture");
		}

		return new GpuTexture(GLES20.GL_TEXTURE_2D, texture, levelData[0].width, levelData[0].height, data.getSizeInBytes(), createVerticalFlipTransform());
	}

	protected static Matrix createVerticalFlipTransform() {
		// Android places its graphics coordinate origin in the upper left corner, with the y axis pointing down. This
		// means that bitmap data must be interpreted as starting in the upper left corner. Since World Wind and OpenGL
		// expect the coordinate origin to be in the lower left corner, and interpret textures as having their data
		// origin in the lower left corner, images loaded by the Android BitmapFactory must always be flipped
		// vertically. Flipping an image vertically is accomplished by multiplying scaling the t-coordinate by -1 then
		// translating the t-coordinate by 1. We have pre-computed the product of the scaling and translation matrices
		// and stored the result inline here to avoid unnecessary matrix allocations and multiplications. The matrix
		// below is equivalent to the following:
		//
		// Matrix scale = Matrix.fromIdentity().setScale(1, -1, 1);
		// Matrix trans = Matrix.fromIdentity().setTranslation(0, 1, 0);
		// Matrix internalTransform = Matrix.fromIdentity();
		// internalTransform.multiplyAndSet(scale);
		// internalTransform.multiplyAndSet(trans);
		// return internalTransform;

		return new Matrix(1, 0, 0, 0, 0, -1, 0, 1, 0, 0, 1, 0, 0, 0, 0, 1);
	}

	protected int target;
	protected int[] textureId;
	protected int width;
	protected int height;
	protected long estimatedMemorySize;
	protected Matrix internalTransform;

	public GpuTexture(int target, int[] textureId, int width, int height, long estimatedMemorySize, Matrix texCoordMatrix) {
		if (target != GLES20.GL_TEXTURE_2D && target != GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_X && target != GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y
				&& target != GLES20.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z && target != GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_X && target != GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Y
				&& target != GLES20.GL_TEXTURE_CUBE_MAP_POSITIVE_Z) {
			String msg = Logging.getMessage("generic.TargetIsInvalid", target);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (textureId.length < 1 || textureId[0] <= 0) {
			String msg = Logging.getMessage("GL.GLObjectIsInvalid", textureId);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (width < 0) {
			String msg = Logging.getMessage("generic.WidthIsInvalid", width);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (height < 0) {
			String msg = Logging.getMessage("generic.HeightIsInvalid", height);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (estimatedMemorySize <= 0) {
			String msg = Logging.getMessage("generic.SizeIsInvalid", estimatedMemorySize);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.target = target;
		this.textureId = textureId;
		this.width = width;
		this.height = height;
		this.estimatedMemorySize = estimatedMemorySize;
		this.internalTransform = texCoordMatrix;
	}

	public int getTarget() {
		return this.target;
	}

	public int getTextureId() {
		return this.textureId[0];
	}

	public int getWidth() {
		return this.width;
	}

	public int getHeight() {
		return this.height;
	}

	public long getSizeInBytes() {
		return this.estimatedMemorySize;
	}

	public void bind() {
		for (int i = 0; i < textureId.length; i++) {
			GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i);
			WorldWindowImpl.glCheckError("glActiveTexture");
			GLES20.glBindTexture(this.target, this.textureId[i]);
			WorldWindowImpl.glCheckError("glBindTexture");
		}
	}

	public void dispose() {
		GLES20.glDeleteTextures(textureId.length, textureId, 0);
		WorldWindowImpl.glCheckError("glDeleteTextures");
	}

	public void applyInternalTransform(DrawContext dc, Matrix matrix) {
		if (dc == null) {
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (matrix == null) {
			String msg = Logging.getMessage("nullValue.MatrixIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (this.internalTransform != null) matrix.multiplyAndSet(this.internalTransform);
	}
}
