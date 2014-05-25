/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.pick;

import android.graphics.Color;
import android.graphics.Point;
import android.opengl.GLES20;
import gov.nasa.worldwind.R;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GpuProgram;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.Logging;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Creates color buffer used for depth values.
 *
 * @author Marek Kedzierski
 */
public class DepthBufferSupport {

	private int mFrameBufferHandle = -1;
	private int mDepthBufferHandle = -1;
	private boolean mIsInitialized = false;
	private int mTextureId = -1;
	private int mViewportWidth = -1;
	private int mViewportHeight = -1;
	private int mTextureWidth;
	private int mTextureHeight;
	private ByteBuffer mBuffer;

	private boolean programCreationFailed;
	private Object programKey;

	public void initialize() {
		mTextureWidth=mTextureHeight= Math.max(mViewportWidth, mViewportHeight);
		genTexture();
		genBuffers();
		mIsInitialized = true;
	}

	public void setup(int width, int height) {
		if(mViewportWidth==width && mViewportHeight==height)
			return;
		mViewportWidth=width;
		mViewportHeight=height;
		if (mIsInitialized) {
			destroy();
		}
		initialize();
	}

	public Vec4 getPosition(DrawContext dc, Point pickPoint) {
		int pixelIndex = (mViewportHeight-pickPoint.y)*mTextureWidth + pickPoint.x;
		mBuffer.position(pixelIndex*4);
		Vec4 bufferVal = new Vec4(mBuffer.get() & 0xFF, mBuffer.get() & 0xFF, mBuffer.get() & 0xFF, mBuffer.get() & 0xFF);
		Vec4 readPixelsVal = getPositionReadPixels(pickPoint.x, pickPoint.y);
		if(!bufferVal.equals(readPixelsVal))
			Logging.warning("Depth Buffer Position mismatch!");
		return bufferVal;
	}

	public Vec4 getPositionReadPixels(int x, int y) {
		final ByteBuffer pixelBuffer = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder());

		GLES20.glReadPixels(x, mViewportHeight - y, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
				pixelBuffer);
		pixelBuffer.rewind();

		return new Vec4(mBuffer.get() & 0xFF, mBuffer.get() & 0xFF, mBuffer.get() & 0xFF, mBuffer.get() & 0xFF);
	}

	protected GpuProgram getGpuProgram(GpuResourceCache cache)
	{
		if (this.programCreationFailed)
			return null;

		GpuProgram program = cache.getProgram(this.programKey);

		if (program == null)
		{
			try
			{
				GpuProgram.GpuProgramSource source = GpuProgram.readProgramSource(R.raw.simple_vert, R.raw.depth_frag);
				program = new GpuProgram(source);
				cache.put(this.programKey, program);
			}
			catch (Exception e)
			{
				String msg = Logging.getMessage("GL.ExceptionLoadingProgram", R.raw.simple_vert, R.raw.depth_frag);
				Logging.error(msg);
				this.programCreationFailed = true;
			}
		}

		return program;
	}

	public void begin(DrawContext dc) {
		if (!mIsInitialized)
			initialize();

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

	public void draw(DrawContext dc) {
		begin(dc);

		SectorGeometryList sgList = dc.getSurfaceGeometry();
		if (sgList == null)
		{
			Logging.warning(Logging.getMessage("generic.NoSurfaceGeometry"));
			return;
		}

		GpuProgram program = this.getGpuProgram(dc.getGpuResourceCache());
		if (program == null)
			return; // Exception logged in loadGpuProgram.
		program.bind();
		dc.setCurrentProgram(program);

		sgList.beginRendering(dc);
		try
		{
			for (SectorGeometry sg : sgList)
			{
				sg.beginRendering(dc);
				try
				{
					sg.render(dc);
				}
				finally
				{
					sg.endRendering(dc);
				}
			}
		}
		finally
		{
			sgList.endRendering(dc);
		}

		end(dc);
	}

	public void end(DrawContext dc) {
		GLES20.glReadPixels(0, 0, mTextureWidth, mTextureHeight, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE,
				mBuffer.rewind());
		mBuffer.rewind();
		GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
		GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, 0);
	}

	private void destroy() {
		GLES20.glDeleteTextures(1, new int[] { mTextureId }, 0);
		GLES20.glDeleteRenderbuffers(1, new int[] { mDepthBufferHandle } , 0);
		GLES20.glDeleteFramebuffers(1, new int[] { mFrameBufferHandle }, 0);
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

		mBuffer = ByteBuffer.allocateDirect(4*mTextureWidth*mTextureHeight).order(ByteOrder.nativeOrder());
	}
}
