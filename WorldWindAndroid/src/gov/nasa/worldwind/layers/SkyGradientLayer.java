/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GpuProgram;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWMath;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import android.opengl.GLES20;

/**
 * Renders an atmosphere around the globe and a sky dome at low altitude.
 * <p/>
 * Note : based on a spherical globe.<br />
 * Issue : Ellipsoidal globe doesnt match the spherical atmosphere everywhere. Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author Patrick Murris
 * @version $Id: SkyGradientLayer.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class SkyGradientLayer extends AbstractLayer {
	protected static final String VERTEX_SHADER_PATH = "shaders/SkyGradientLayer.vert";
	protected static final String FRAGMENT_SHADER_PATH = "shaders/SkyGradientLayer.frag";
	protected final static int STACKS = 12;
	protected final static int SLICES = 64;

	// TODO: make configurable
	protected double thickness = 100e3; // Atmosphere thickness
	// protected float[] horizonColor = new float[] { 0.66f, 0.70f, 0.81f, 1.0f }; // horizon color (same as fog)
	protected float[] horizonColor = new float[] { 0.76f, 0.76f, 0.80f, 1.0f }; // horizon color
	protected float[] zenithColor = new float[] { 0.26f, 0.47f, 0.83f, 1.0f }; // zenith color
	protected double lastRebuildHorizon = 0;
	protected Object vertexArraysCacheKey = new Object();
	protected ArrayList<float[]> vertexArrays = null;
	private boolean programCreationFailed;
	protected final Object programKey = new Object();

	/** Renders an atmosphere around the globe */
	public SkyGradientLayer() {
		this.setPickEnabled(false);
	}

	/**
	 * Get the atmosphere thickness in meter
	 * 
	 * @return the atmosphere thickness in meter
	 */
	public double getAtmosphereThickness() {
		return this.thickness;
	}

	/**
	 * Set the atmosphere thickness in meter
	 * 
	 * @param thickness
	 *            the atmosphere thickness in meter
	 */
	public void setAtmosphereThickness(double thickness) {
		if (thickness < 0) {
			String msg = Logging.getMessage("generic.ArgumentOutOfRange");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.thickness = thickness;
		this.lastRebuildHorizon = 0;
	}

	/**
	 * Get the horizon color
	 * 
	 * @return the horizon color
	 */
	public float[] getHorizonColor() {
		return this.horizonColor;
	}

	/**
	 * Set the horizon color
	 * 
	 * @param color
	 *            the horizon color
	 */
	public void setHorizonColor(float[] color) {
		if (color == null) {
			String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		horizonColor = color;
		this.lastRebuildHorizon = 0;
	}

	/**
	 * Get the zenith color
	 * 
	 * @return the zenith color
	 */
	public float[] getZenithColor() {
		return zenithColor;
	}

	/**
	 * Set the zenith color
	 * 
	 * @param color
	 *            the zenith color
	 */
	public void setZenithColor(float[] color) {
		if (color == null) {
			String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.zenithColor = color;
		this.lastRebuildHorizon = 0;
	}

	protected boolean isValid(DrawContext dc) {
		// Build or rebuild sky dome if horizon distance changed more then 100m
		// Note: increasing this threshold may produce artifacts like far clipping at very low altitude
		return vertexArrays != null && Math.abs(this.lastRebuildHorizon - dc.getView().getFarClipDistance()) <= .100;
	}

	@Override
	public void doRender(DrawContext dc) {

		try {
			GpuProgram program = this.getGpuProgram(dc.getGpuResourceCache());
			if (program == null) return; // Exception logged in loadGpuProgram.
			program.bind();
			if (!this.isValid(dc)) vertexArrays = this.updateSkyDome(dc);
			GLES20.glDisable(GLES20.GL_CULL_FACE);
			GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			// GLES20.glDisable(GLES20.GL_DEPTH_TEST);
			// GLES20.glDepthMask(false);
			// GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			// GLES20.glEnable(GLES20.GL_BLEND);

			Matrix projection = this.createProjectionMatrix(dc);
			// this.applyDrawProjection(dc);
			Matrix modelview = this.createModelViewMatrix(dc);
			Matrix mvp = Matrix.fromIdentity().multiplyAndSet(projection, modelview);
			// this.applyDrawTransform(dc);
			program.loadUniformMatrix("mvpMatrix", mvp);
			// Draw sky
			this.drawVertexArrays(dc, vertexArrays, program);
		} finally {
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			GLES20.glEnable(GLES20.GL_CULL_FACE);
			// GLES20.glDisable(GLES20.GL_BLEND);
		}
	}

	protected GpuProgram getGpuProgram(GpuResourceCache cache) {
		if (this.programCreationFailed) return null;

		GpuProgram program = cache.getProgram(this.programKey);

		if (program == null) {
			try {
				GpuProgram.GpuProgramSource source = GpuProgram.readProgramSource(VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
				program = new GpuProgram(source);
				cache.put(this.programKey, program);
			} catch (Exception e) {
				String msg = Logging.getMessage("GL.ExceptionLoadingProgram", VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
				Logging.error(msg);
				this.programCreationFailed = true;
			}
		}

		return program;
	}

	protected void drawVertexArrays(DrawContext dc, ArrayList<float[]> vertexArrays, GpuProgram program) {
		int pointLocation = program.getAttribLocation("vertexPoint");
		GLES20.glEnableVertexAttribArray(pointLocation);
		int colorLocation = program.getAttribLocation("vertexColor");
		GLES20.glEnableVertexAttribArray(colorLocation);
		for (int i = 0; i < vertexArrays.size(); i = i + 2) {
			float[] vertexArray = vertexArrays.get(i);
			float[] colorArray = vertexArrays.get(i + 1);
			FloatBuffer vertexBuf = ByteBuffer.allocateDirect(vertexArray.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			vertexBuf.put(vertexArray);
			vertexBuf.rewind();
			GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, vertexBuf);
			FloatBuffer colorBuf = ByteBuffer.allocateDirect(colorArray.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
			colorBuf.put(colorArray);
			colorBuf.rewind();
			GLES20.glVertexAttribPointer(colorLocation, 4, GLES20.GL_FLOAT, false, 0, colorBuf);
			GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, vertexArray.length / 3);
		}
		GLES20.glDisableVertexAttribArray(pointLocation);
		GLES20.glDisableVertexAttribArray(colorLocation);
	}

	protected Matrix createModelViewMatrix(DrawContext dc) {
		View view = dc.getView();
		// Place sky - TODO: find another ellipsoid friendlier way (the sky dome is not exactly normal...
		// to the ground at higher latitude)
		Vec4 camPoint = view.getEyePoint();
		Vec4 camPosFromPoint = CartesianToSpherical(camPoint.x, camPoint.y, camPoint.z);
		Matrix retval = dc.getView().getModelviewMatrix().copy();
		Matrix rotMatrix = Matrix.fromRotationY(Angle.fromRadians(camPosFromPoint.z));
		retval.multiplyAndSet(rotMatrix);
		rotMatrix = Matrix.fromRotationX(Angle.fromDegrees((-Angle.fromRadians(camPosFromPoint.y).degrees + 90)));
		retval.multiplyAndSet(rotMatrix);
		Matrix transMatrix = Matrix.fromTranslation(0, view.getEyePoint().getLength3(), 0);
		retval.multiplyAndSet(transMatrix);
		return retval;
	}

	protected Matrix createProjectionMatrix(DrawContext dc) {
		View view = dc.getView();
		double viewportWidth = view.getViewport().width;
		double viewportHeight = view.getViewport().height;

		// If either the viewport width or height is zero, then treat the dimension as if it had value 1.
		if (viewportWidth <= 0) viewportWidth = 1;
		if (viewportHeight <= 0) viewportHeight = 1;

		double horizonDist = WWMath.computeHorizonDistance(dc.getGlobe(), view.getEyePosition(dc.getGlobe()).elevation);
		Matrix projection = Matrix.fromPerspective(view.getFieldOfView(), viewportWidth, viewportHeight, 100, horizonDist + 10e3);
		return projection;
	}

	protected ArrayList<float[]> updateSkyDome(DrawContext dc) {
		View view = dc.getView();
		ArrayList<float[]> retval = null;

		double tangentialDistance = WWMath.computeHorizonDistance(dc.getGlobe(), view.getEyePosition(dc.getGlobe()).elevation);
		double distToCenterOfPlanet = view.getEyePoint().getLength3();
		Position camPos = dc.getGlobe().computePositionFromPoint(view.getEyePoint());
		double worldRadius = dc.getGlobe().computePointFromPosition(camPos, 0).getLength3();
		double camAlt = camPos.elevation;

		// horizon latitude degrees
		double horizonLat = (-Math.PI / 2 + Math.acos(tangentialDistance / distToCenterOfPlanet)) * 180 / Math.PI;
		// zenith latitude degrees
		double zenithLat = 90;
		float zenithOpacity = 1f;
		float gradientBias = 2f;
		if (camAlt >= thickness) {
			// Eye is above atmosphere
			double tangentalDistanceZenith = Math.sqrt(distToCenterOfPlanet * distToCenterOfPlanet - (worldRadius + thickness) * (worldRadius + thickness));
			zenithLat = (-Math.PI / 2 + Math.acos(tangentalDistanceZenith / distToCenterOfPlanet)) * 180 / Math.PI;
			zenithOpacity = 0f;
			gradientBias = 1f;
		}
		if (camAlt < thickness && camAlt > thickness * 0.7) {
			// Eye is entering atmosphere - outer 30%
			double factor = (thickness - camAlt) / (thickness - thickness * 0.7);
			zenithLat = factor * 90;
			zenithOpacity = (float) factor;
			gradientBias = 1f + (float) factor;
		}

		retval = this.computeSkyDome(dc, (float) (tangentialDistance), horizonLat, zenithLat, SLICES, STACKS, zenithOpacity, gradientBias);
		this.lastRebuildHorizon = tangentialDistance;
		return retval;
	}

	/**
	 * Draws the sky dome
	 * 
	 * @param dc
	 *            the current DrawContext
	 * @param radius
	 *            the sky dome radius
	 * @param startLat
	 *            the horizon latitude
	 * @param endLat
	 *            the zenith latitude
	 * @param slices
	 *            the number of slices - vertical divisions
	 * @param stacks
	 *            the nuber os stacks - horizontal divisions
	 * @param zenithOpacity
	 *            the sky opacity at zenith
	 * @param gradientBias
	 *            determines how fast the sky goes from the horizon color to the zenith color. A value of <code>1</code> with produce a balanced gradient, a
	 *            value greater then <code>1</code> will
	 *            have the zenith color dominate and a value less then <code>1</code> will have the opposite
	 *            effect.
	 */
	protected ArrayList<float[]> computeSkyDome(DrawContext dc, float radius, double startLat, double endLat, int slices, int stacks, float zenithOpacity, float gradientBias) {
		double latitude, longitude, latitudeTop = endLat;

		ArrayList<float[]> retval = new ArrayList<float[]>();

		// TODO: Simplify code
		double linear, linearTop, k, kTop, colorFactorZ, colorFactorZTop = 0;
		double colorFactorH, colorFactorHTop = 0;
		double alphaFactor, alphaFactorTop = 0;

		// bottom fade
		latitude = startLat - Math.max((endLat - startLat) / 4, 3);
		float[] vertexArray = new float[(slices + 1) * 6];
		float[] colorArray = new float[(slices + 1) * 8];
		for (int slice = 0; slice <= slices; slice++) {
			longitude = 180 - ((float) slice / slices * (float) 360);
			Vec4 v = SphericalToCartesian(latitude, longitude, radius);
			int colorIndex = slice * 8;
			colorArray[colorIndex] = zenithColor[0];
			colorArray[colorIndex + 1] = zenithColor[1];
			colorArray[colorIndex + 2] = zenithColor[2];
			colorArray[colorIndex + 3] = 0;
			int vertexIndex = slice * 6;
			vertexArray[vertexIndex] = (float) v.x;
			vertexArray[vertexIndex + 1] = (float) v.y;
			vertexArray[vertexIndex + 2] = (float) v.z;
			v = SphericalToCartesian(startLat, longitude, radius);
			colorArray[colorIndex + 4] = horizonColor[0];
			colorArray[colorIndex + 5] = horizonColor[1];
			colorArray[colorIndex + 6] = horizonColor[2];
			colorArray[colorIndex + 7] = horizonColor[3];
			vertexArray[vertexIndex + 3] = (float) v.x;
			vertexArray[vertexIndex + 4] = (float) v.y;
			vertexArray[vertexIndex + 5] = (float) v.z;
		}
		retval.add(vertexArray);
		retval.add(colorArray);

		// stacks and slices
		for (int stack = 1; stack < stacks - 1; stack++) {
			// bottom vertex
			linear = (float) (stack - 1) / (stacks - 1f);
			k = 1 - Math.cos(linear * Math.PI / 2);
			latitude = startLat + k * (endLat - startLat);
			colorFactorZ = Math.min(1f, linear * gradientBias); // coef zenith color
			colorFactorH = 1 - colorFactorZ; // coef horizon color
			alphaFactor = 1 - Math.pow(linear, 4) * (1 - zenithOpacity); // coef alpha transparency
			// top vertex
			linearTop = (float) (stack) / (stacks - 1f);
			kTop = 1 - Math.cos(linearTop * Math.PI / 2);
			latitudeTop = startLat + kTop * (endLat - startLat);
			colorFactorZTop = Math.min(1f, linearTop * gradientBias); // coef zenith color
			colorFactorHTop = 1 - colorFactorZTop; // coef horizon color
			alphaFactorTop = 1 - Math.pow(linearTop, 4) * (1 - zenithOpacity); // coef alpha transparency
			// Draw stack
			vertexArray = new float[(slices + 1) * 6];
			colorArray = new float[(slices + 1) * 8];
			for (int slice = 0; slice <= slices; slice++) {
				longitude = 180 - ((float) slice / slices * (float) 360);
				Vec4 v = SphericalToCartesian(latitude, longitude, radius);
				int colorIndex = slice * 8;
				colorArray[colorIndex] = (float) (horizonColor[0] * colorFactorH + zenithColor[0] * colorFactorZ);
				colorArray[colorIndex + 1] = (float) (horizonColor[1] * colorFactorH + zenithColor[1] * colorFactorZ);
				colorArray[colorIndex + 2] = (float) (horizonColor[2] * colorFactorH + zenithColor[2] * colorFactorZ);
				colorArray[colorIndex + 3] = (float) ((horizonColor[3] * colorFactorH + zenithColor[3] * colorFactorZ) * alphaFactor);
				int vertexIndex = slice * 6;
				vertexArray[vertexIndex] = (float) v.x;
				vertexArray[vertexIndex + 1] = (float) v.y;
				vertexArray[vertexIndex + 2] = (float) v.z;
				v = SphericalToCartesian(latitudeTop, longitude, radius);
				colorArray[colorIndex + 4] = (float) (horizonColor[0] * colorFactorHTop + zenithColor[0] * colorFactorZTop);
				colorArray[colorIndex + 5] = (float) (horizonColor[1] * colorFactorHTop + zenithColor[1] * colorFactorZTop);
				colorArray[colorIndex + 6] = (float) (horizonColor[2] * colorFactorHTop + zenithColor[2] * colorFactorZTop);
				colorArray[colorIndex + 7] = (float) ((horizonColor[3] * colorFactorHTop + zenithColor[3] * colorFactorZTop) * alphaFactorTop);
				vertexArray[vertexIndex + 3] = (float) v.x;
				vertexArray[vertexIndex + 4] = (float) v.y;
				vertexArray[vertexIndex + 5] = (float) v.z;
			}
			retval.add(vertexArray);
			retval.add(colorArray);
		}

		// Top fade
		vertexArray = new float[(slices + 1) * 6];
		colorArray = new float[(slices + 1) * 8];
		for (int slice = 0; slice <= slices; slice++) {
			longitude = 180 - ((float) slice / slices * (float) 360);
			Vec4 v = SphericalToCartesian(latitudeTop, longitude, radius);
			int colorIndex = slice * 8;
			colorArray[colorIndex] = (float) (horizonColor[0] * colorFactorHTop + zenithColor[0] * colorFactorZTop);
			colorArray[colorIndex + 1] = (float) (horizonColor[1] * colorFactorHTop + zenithColor[1] * colorFactorZTop);
			colorArray[colorIndex + 2] = (float) (horizonColor[2] * colorFactorHTop + zenithColor[2] * colorFactorZTop);
			colorArray[colorIndex + 3] = (float) ((horizonColor[3] * colorFactorHTop + zenithColor[3] * colorFactorZTop) * alphaFactorTop);
			int vertexIndex = slice * 6;
			vertexArray[vertexIndex] = (float) v.x;
			vertexArray[vertexIndex + 1] = (float) v.y;
			vertexArray[vertexIndex + 2] = (float) v.z;
			v = SphericalToCartesian(endLat, longitude, radius);
			colorArray[colorIndex + 4] = zenithColor[0];
			colorArray[colorIndex + 5] = zenithColor[1];
			colorArray[colorIndex + 6] = zenithColor[2];
			colorArray[colorIndex + 7] = zenithOpacity < 1 ? 0 : zenithColor[3];
			vertexArray[vertexIndex + 3] = (float) v.x;
			vertexArray[vertexIndex + 4] = (float) v.y;
			vertexArray[vertexIndex + 5] = (float) v.z;
		}
		retval.add(vertexArray);
		retval.add(colorArray);
		return retval;
	}

	/**
	 * Converts position in spherical coordinates (lat/lon/altitude) to cartesian (XYZ) coordinates.
	 * 
	 * @param latitude
	 *            Latitude in decimal degrees
	 * @param longitude
	 *            Longitude in decimal degrees
	 * @param radius
	 *            Radius
	 * @return the corresponding Point
	 */
	protected static Vec4 SphericalToCartesian(double latitude, double longitude, double radius) {
		latitude *= Math.PI / 180.0f;
		longitude *= Math.PI / 180.0f;

		double radCosLat = radius * Math.cos(latitude);

		return new Vec4(radCosLat * Math.sin(longitude), radius * Math.sin(latitude), radCosLat * Math.cos(longitude));
	}

	/**
	 * Converts position in cartesian coordinates (XYZ) to spherical (radius, lat, lon) coordinates.
	 * 
	 * @param x
	 *            X coordinate
	 * @param y
	 *            Y coordinate
	 * @param z
	 *            Z coordinate
	 * @return a <code>Vec4</code> point for the spherical coordinates {radius, lat, lon}
	 */
	protected static Vec4 CartesianToSpherical(double x, double y, double z) {
		double rho = Math.sqrt(x * x + y * y + z * z);
		double longitude = Math.atan2(x, z);
		double latitude = Math.asin(y / rho);

		return new Vec4(rho, latitude, longitude);
	}

	@Override
	public String toString() {
		return Logging.getMessage("layers.Earth.SkyGradientLayer.Name");
	}
}
