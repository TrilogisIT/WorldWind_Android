/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.pick;

import android.graphics.Color;
import gov.nasa.worldwind.WorldWindowGLSurfaceView;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Point;
import android.opengl.GLES20;
import gov.nasa.worldwind.util.Logging;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 *
 * @author tag
 * @version $Id: PickSupport.java 805 2012-09-26 01:47:35Z dcollins $
 */
public class PickSupport {
	protected Map<Integer, PickedObject> pickableObjects = new HashMap<Integer, PickedObject>();

	private int mFrameBufferHandle = -1;
	private int mDepthBufferHandle = -1;
	private boolean mIsInitialized = false;
	private int mTextureId = -1;
	private int mViewportWidth;
	private int mViewportHeight;
	private int mTextureWidth;
	private int mTextureHeight;

	public PickSupport() {}

	public PickSupport(int viewportWidth, int viewportHeight) {
		this.mViewportHeight = viewportHeight;
		this.mViewportWidth = viewportWidth;
	}

	public void addPickableObject(PickedObject po) {
		this.getPickableObjects().put(po.getColorCode(), po);
	}

	public void addPickableObject(int colorCode, Object o) {
		this.getPickableObjects().put(colorCode, new PickedObject(colorCode, o));
	}

	public void addPickableObject(int colorCode, Object o, Position position) {
		this.getPickableObjects().put(colorCode, new PickedObject(colorCode, o, position, false));
	}

	public void addPickableObject(int colorCode, Object o, Position position, boolean isTerrain) {
		this.getPickableObjects().put(colorCode, new PickedObject(colorCode, o, position, isTerrain));
	}

	public void clearPickList() {
		this.getPickableObjects().clear();
	}

	protected Map<Integer, PickedObject> getPickableObjects() {
		return this.pickableObjects;
	}

	public PickedObject getTopObject(DrawContext dc, Point pickPoint) {
		if (this.getPickableObjects().isEmpty()) return null;

		int colorCode = dc.getPickColor(pickPoint);
		if (colorCode == 0) // getPickColor returns 0 if the pick point selects the clear color.
			return null;

		PickedObject pickedObject = getPickableObjects().get(colorCode);
		if (pickedObject == null) return null;

		return pickedObject;
	}

	public PickedObject resolvePick(DrawContext dc, Point pickPoint, Layer layer) {
		PickedObject pickedObject = this.getTopObject(dc, pickPoint);
		if (pickedObject != null) {
			if (layer != null) pickedObject.setParentLayer(layer);

			dc.addPickedObject(pickedObject);
		}

		this.clearPickList();

		return pickedObject;
	}

	// ARE THESE NECESSARY???
	public void beginPicking(DrawContext dc) {
		GLES20.glDisable(GLES20.GL_DITHER);
		WorldWindowGLSurfaceView.glCheckError("glDisable: GL_DITHER");

		GLES20.glDisable(GLES20.GL_BLEND);
		WorldWindowGLSurfaceView.glCheckError("glDisable: GL_BLEND");

		if (dc.isDeepPickingEnabled()) {
			GLES20.glDisable(GLES20.GL_DEPTH_TEST);
			WorldWindowGLSurfaceView.glCheckError("glDisable: GL_DEPTH_TEST");
			GLES20.glDepthMask(false);
		}
	}

	public void endPicking(DrawContext dc) {
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO);
		WorldWindowGLSurfaceView.glCheckError("glBlendFunc");

		GLES20.glDisable(GLES20.GL_BLEND);
		WorldWindowGLSurfaceView.glCheckError("glDisable: GL_BLEND");

		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		WorldWindowGLSurfaceView.glCheckError("glEnable: GL_DEPTH_TEST");

		GLES20.glDepthMask(true);
		WorldWindowGLSurfaceView.glCheckError("glDepthMask");
	}

	public void initialize() {
		mTextureWidth=mTextureHeight= Math.max(mViewportWidth, mViewportHeight);
		genTexture();
		
		genBuffers();

		mIsInitialized = true;
	}

	public void reload() {
		if (!mIsInitialized)
			return;

		genBuffers();
	}

	private void genTexture() {
		int[] textures = new int[1];
		GLES20.glGenTextures(1, textures, 0);
		int textureId = textures[0];

		if (textureId > 0) {
			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
			GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

			GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, mTextureWidth, mTextureHeight, 0,
					GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);

			GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
			mTextureId = textureId;
		}
	}

	private void destroyTexture() {
		GLES20.glDeleteTextures(1, new int[] { mTextureId }, 0);
	}

	public void genBuffers() {
		final int[] frameBuffers = new int[1];
		GLES20.glGenFramebuffers(1, frameBuffers, 0);
		mFrameBufferHandle = frameBuffers[0];

		final int[] depthBuffers = new int[1];
		GLES20.glGenRenderbuffers(1, depthBuffers, 0);
		mDepthBufferHandle = depthBuffers[0];

		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, mDepthBufferHandle);
		GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16,
				mTextureWidth, mTextureHeight);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
	}

	public void bindFrameBuffer() {
		if (!mIsInitialized)
		{
			initialize();
			return;
		}
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBufferHandle);
		GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
				GLES20.GL_TEXTURE_2D, mTextureId, 0);
		int status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER);
		if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
			GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
			Logging.debug("Could not bind FrameBuffer for color picking." + mTextureId);
		}
		GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT,
				GLES20.GL_RENDERBUFFER, mDepthBufferHandle);
	}

	public void unbindFrameBuffer() {
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
	}

	public int getPickColor(int x, int y) {
		final ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(4);
		pixelBuffer.order(ByteOrder.nativeOrder());

		GLES20.glReadPixels(x, mViewportHeight - y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
				pixelBuffer);
		pixelBuffer.rewind();

		final int r = pixelBuffer.get(0) & 0xff;
		final int g = pixelBuffer.get(1) & 0xff;
		final int b = pixelBuffer.get(2) & 0xff;
		final int a = pixelBuffer.get(3) & 0xff;
		return Color.argb(a, r, g, b);
	}


}
