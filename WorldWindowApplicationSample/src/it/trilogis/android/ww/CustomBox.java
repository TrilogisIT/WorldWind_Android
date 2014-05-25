/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package it.trilogis.android.ww;

import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GpuProgram;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.Logging;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import android.graphics.Point;
import android.opengl.GLES20;

/**
 * Created by kedzie on 3/20/14.
 */
public class CustomBox implements OrderedRenderable {
	protected Position position;
	protected float size;
	protected long frameTimestamp;
	protected Vec4 placePoint;
	protected double eyeDistance;
	protected Extent extent;
	protected PickSupport pickSupport = new PickSupport();

	protected FloatBuffer vertexBuffer;
	protected FloatBuffer textureBuffer;
	protected FloatBuffer normalBuffer;
	protected FloatBuffer colorBuffer;
	protected IntBuffer indexBuffer;
	Matrix currentMatrix = Matrix.fromIdentity();
	protected final Object programKey = new Object();
	protected boolean programCreationFailed;
	protected Layer pickLayer;
	protected Color currentColor = new Color(1, 0, 0);

	public CustomBox(Position position, float sizeInMeters)
	{
		this.position = position;
		this.size = sizeInMeters;

		float halfSize = size * .5f;
		float[] vertices = {
				// -- back
				halfSize, halfSize, halfSize, 			-halfSize, halfSize, halfSize,
				-halfSize, -halfSize, halfSize,			halfSize, -halfSize, halfSize, // 0-1-halfSize-3 front

				halfSize, halfSize, halfSize, 			halfSize, -halfSize, halfSize,
				halfSize, -halfSize, -halfSize, 		halfSize, halfSize, -halfSize,// 0-3-4-5 right
				// -- front
				halfSize, -halfSize, -halfSize, 		-halfSize, -halfSize, -halfSize,
				-halfSize, halfSize, -halfSize,			halfSize, halfSize, -halfSize,// 4-7-6-5 back

				-halfSize, halfSize, halfSize, 			-halfSize, halfSize, -halfSize,
				-halfSize, -halfSize, -halfSize,		-halfSize,	-halfSize, halfSize,// 1-6-7-halfSize left

				halfSize, halfSize, halfSize, 			halfSize, halfSize, -halfSize,
				-halfSize, halfSize, -halfSize, 		-halfSize, halfSize, halfSize, // top

				halfSize, -halfSize, halfSize, 			-halfSize, -halfSize, halfSize,
				-halfSize, -halfSize, -halfSize,		halfSize, -halfSize, -halfSize,// bottom
		};

		float[] textureCoords = new float[]
				{
						0, 1, 1, 1, 1, 0, 0, 0, // front
						0, 1, 1, 1, 1, 0, 0, 0, // up
						0, 1, 1, 1, 1, 0, 0, 0, // back
						0, 1, 1, 1, 1, 0, 0, 0, // down
						0, 1, 1, 1, 1, 0, 0, 0, // right
						0, 1, 1, 1, 1, 0, 0, 0, // left
				};

		float[] colors = new float[] {
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1,
				1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
		};

		float n = 1;

		float[] normals = {
				0, 0, n, 0, 0, n, 0, 0, n, 0, 0, n, // front
				n, 0, 0, n, 0, 0, n, 0, 0, n, 0, 0, // right
				0, 0, -n, 0, 0, -n, 0, 0, -n, 0, 0, -n, // back
				-n, 0, 0, -n, 0, 0, -n, 0, 0, -n, 0, 0, // left
				0, n, 0, 0, n, 0, 0, n, 0, 0, n, 0, // top
				0, -n, 0, 0, -n, 0, 0, -n, 0, 0, -n, 0, // bottom
		};

		int[] indices = {
				0, 1, 2, 0, 2, 3,
				4, 5, 6, 4, 6, 7,
				8, 9, 10, 8, 10, 11,
				12, 13, 14, 12, 14, 15,
				16, 17, 18, 16, 18, 19,
				20, 21, 22, 20, 22, 23,
		};

		vertexBuffer = ByteBuffer.allocateDirect(vertices.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		vertexBuffer.put(vertices).position(0);
		normalBuffer = ByteBuffer.allocateDirect(normals.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		normalBuffer.put(normals).position(0);
		textureBuffer = ByteBuffer.allocateDirect(textureCoords.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		textureBuffer.put(textureCoords).position(0);
		colorBuffer = ByteBuffer.allocateDirect(colors.length*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		colorBuffer.put(colors).position(0);
		indexBuffer = ByteBuffer.allocateDirect(indices.length*4).order(ByteOrder.nativeOrder()).asIntBuffer();
		indexBuffer.put(indices).position(0);
	}

	protected void beginDrawing(DrawContext dc)
	{
		GpuProgram program = this.getDefaultGpuProgram(dc.getGpuResourceCache());
		if (program == null) return; // Message already logged in getDefaultGpuProgram.

		// Bind this shape's gpu program as the current OpenGL program.
		dc.setCurrentProgram(program);
		program.bind();

		// Enable the gpu program's vertexPoint attribute, if one exists. The data for this attribute is specified by
		// each shape.
		int attribLocation = program.getAttribLocation("vertexPoint");
		if (attribLocation >= 0) GLES20.glEnableVertexAttribArray(attribLocation);

		// Set the OpenGL state that this shape depends on.
		GLES20.glDisable(GLES20.GL_CULL_FACE);

		Matrix transform = Matrix.fromTranslation(dc.getGlobe().computePointFromPosition(
				position.latitude, position.longitude, position.elevation*dc.getVerticalExaggeration()));
		transform = transform.multiply(Matrix.fromRotationY(position.longitude));
		// Rotate the coordinate system to match the latitude.
		// Latitude is treated clockwise as rotation about the X-axis. We flip the latitude value so that a positive
		// rotation produces a clockwise rotation (when facing the axis).
		transform = transform.multiply(Matrix.fromRotationX(position.latitude.multiply(-1.0)));
		currentMatrix.multiplyAndSet(dc.getView().getModelviewProjectionMatrix(), transform);
		dc.getCurrentProgram().loadUniformMatrix("mvpMatrix", this.currentMatrix);
	}

	protected void endDrawing(DrawContext dc)
	{
		GpuProgram program = dc.getCurrentProgram();
		if (program == null) return; // Message already logged in getDefaultGpuProgram via beginDrawing.

		// Disable the program's vertexPoint attribute, if one exists. This restores the program state modified in
		// beginRendering. This must be done while the program is still bound, because getAttribLocation depends on
		// the current OpenGL program state.
		int location = program.getAttribLocation("vertexPoint");
		if (location >= 0) GLES20.glDisableVertexAttribArray(location);

		// Restore the previous OpenGL program state.
		dc.setCurrentProgram(null);
		GLES20.glUseProgram(0);

		// Restore the OpenGL array and element array buffer bindings to 0.
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
		GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);

		// Restore the remaining OpenGL state values to their defaults.
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glDepthMask(true);
		GLES20.glLineWidth(1f);
	}

	protected void drawUnitCube(DrawContext dc)
	{
		int maPositionHandle = dc.getCurrentProgram().getAttribLocation("vertexPoint");
		GLES20.glVertexAttribPointer(maPositionHandle, 3, GLES20.GL_FLOAT, false,
				4*3, vertexBuffer.rewind());
		GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexBuffer.capacity(), GLES20.GL_UNSIGNED_INT, indexBuffer.rewind());
	}

	// From OrderedRenderable
	public void pick(DrawContext dc, Point pickPoint)
	{
		try {
			this.render(dc);
		} finally {
			this.pickSupport.resolvePick(dc, pickPoint, this.pickLayer);
		}
	}

	// From OrderedRenderable
	public double getDistanceFromEye()
	{
		return this.eyeDistance;
	}

	protected boolean intersectsFrustum(DrawContext dc)
	{
		if (this.extent == null)
			return true; // don't know the visibility, shape hasn't been computed yet

//		if (dc.isPickingMode())
//			return dc.getPickFrustums().intersectsAny(this.extent);

		return dc.getView().getFrustumInModelCoordinates().intersects(this.extent);
	}

	// From Renderable
	public void render(DrawContext dc)
	{
		if (this.extent != null)
		{
			if (!this.intersectsFrustum(dc)) {
				Logging.warning("!intersectFrustum");
				return;
			}

			// If the shape is less that a pixel in size, don't render it.
			if (dc.isSmall(this.extent, 1))
				return;
		}

		if (dc.isOrderedRenderingMode())
			this.drawOrderedRenderable(dc, this.pickSupport);
		else
			this.makeOrderedRenderable(dc);
	}

	protected void makeOrderedRenderable(DrawContext dc)
	{
		// This method is called twice each frame: once during picking and once during
		// rendering. We only need to compute the placePoint and eyedistance once per
		// frame, so check the frame timestamp to see if this is new frame.
		if (dc.getFrameTimeStamp() != this.frameTimestamp)
		{
			this.placePoint = dc.getGlobe().computePointFromPosition(this.position);
			Box box = Box.computeBoundingBox((FloatBuffer) vertexBuffer.rewind(), 3);
			this.extent = box.translate(placePoint);
			this.eyeDistance = dc.getView().getEyePoint().distanceTo3(this.placePoint);

			this.frameTimestamp = dc.getFrameTimeStamp();
		}
		if (dc.isPickingMode()) this.pickLayer = dc.getCurrentLayer();
		// Add the cube to the ordered renderable list.
		dc.addOrderedRenderable(this);
	}

	protected void drawOrderedRenderable(DrawContext dc, PickSupport pickCandidates)
	{
		this.beginDrawing(dc);
		try
		{
			if (dc.isPickingMode())
			{
				int pickColor = dc.getUniquePickColor();
				pickCandidates.addPickableObject(pickColor, this, this.position);
				dc.getCurrentProgram().loadUniformColor("color", currentColor.set(pickColor, false));
			} else {
				dc.getCurrentProgram().loadUniformColor("color", this.currentColor.set(1, 0, 0));
			}

			this.drawUnitCube(dc);
		}
		finally
		{
			this.endDrawing(dc);
		}
	}

	protected GpuProgram getDefaultGpuProgram(GpuResourceCache cache) {
		if (this.programCreationFailed) return null;

		GpuProgram program = cache.getProgram(this.programKey);

		if (program == null) {
			try {
				GpuProgram.GpuProgramSource source = GpuProgram.readProgramSource(R.raw.abstractshapevert, R.raw.abstractshapefrag);
				program = new GpuProgram(source);
				cache.put(this.programKey, program);
			} catch (Exception e) {
				String msg = Logging.getMessage("GL.ExceptionLoadingProgram", R.raw.abstractshapevert, R.raw.abstractshapefrag);
				Logging.error(msg);
				this.programCreationFailed = true;
			}
		}

		return program;
	}

	@Override
	public Layer getLayer() {
		// TODO Auto-generated method stub
		return null;
	}

}