/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.BasicView;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Rect;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GpuProgram;
import gov.nasa.worldwind.render.GpuTexture;
import gov.nasa.worldwind.render.GpuTextureData;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.Logging;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.graphics.Point;
import android.opengl.GLES20;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author Nicola Dorigatti
 * @version $Id: CompassLayer.java 1 2013-08-08 $
 */
public class CompassLayer extends AbstractLayer {
	protected static final String VERTEX_SHADER_PATH_TEXTURE = "shaders/CompassLayerTexture.vert";
	protected static final String FRAGMENT_SHADER_PATH_TEXTURE = "shaders/CompassLayerTexture.frag";
	protected final Object programTextureKey = new Object();
	protected String iconFilePath = "images/notched-compass.png"; // TODO: make configurable
	protected double compassToViewportScale = 0.2; // TODO: make configurable
	protected double iconScale = 0.5;
	protected int borderWidth = 20; // TODO: make configurable
	protected String position = AVKey.NORTHEAST; // TODO: make configurable
	protected String resizeBehavior = AVKey.RESIZE_SHRINK_ONLY;
	protected int iconWidth;
	protected int iconHeight;
	protected Vec4 locationCenter = null;
	protected Vec4 locationOffset = null;
	protected boolean showTilt = true;
	protected PickSupport pickSupport = new PickSupport();

	// Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
	protected OrderedIcon orderedImage = new OrderedIcon();

	protected class OrderedIcon implements OrderedRenderable {
		public double getDistanceFromEye() {
			return 0;
		}

		public void pick(DrawContext dc, Point pickPoint) {
			CompassLayer.this.draw(dc);
		}

		public void render(DrawContext dc) {
			CompassLayer.this.draw(dc);
		}
	}

	public CompassLayer() {
		this.setOpacity(0.8); // TODO: make configurable
		this.setPickEnabled(false); // Default to no picking
	}

	public CompassLayer(String iconFilePath) {
		this.setIconFilePath(iconFilePath);
		this.setOpacity(0.8); // TODO: make configurable
		this.setPickEnabled(false); // Default to no picking
	}

	/**
	 * Returns the layer's current icon file path.
	 * 
	 * @return the icon file path
	 */
	public String getIconFilePath() {
		return iconFilePath;
	}

	/**
	 * Sets the compass icon's image location. The layer first searches for this location in the current Java classpath.
	 * If not found then the specified path is assumed to refer to the local file system. found there then the
	 * 
	 * @param iconFilePath
	 *            the path to the icon's image file
	 */
	public void setIconFilePath(String iconFilePath) {
		if (iconFilePath == null) {
			String message = Logging.getMessage("nullValue.IconFilePath");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}
		this.iconFilePath = iconFilePath;
	}

	/**
	 * Returns the layer's compass-to-viewport scale factor.
	 * 
	 * @return the compass-to-viewport scale factor
	 */
	public double getCompassToViewportScale() {
		return compassToViewportScale;
	}

	/**
	 * Sets the scale factor applied to the viewport size to determine the displayed size of the compass icon. This
	 * scale factor is used only when the layer's resize behavior is AVKey.RESIZE_STRETCH or AVKey.RESIZE_SHRINK_ONLY.
	 * The icon's width is adjusted to occupy the proportion of the viewport's width indicated by this factor. The
	 * icon's height is adjusted to maintain the compass image's native aspect ratio.
	 * 
	 * @param compassToViewportScale
	 *            the compass to viewport scale factor
	 */
	public void setCompassToViewportScale(double compassToViewportScale) {
		this.compassToViewportScale = compassToViewportScale;
	}

	/**
	 * Returns the icon scale factor. See {@link #setIconScale(double)} for a description of the scale factor.
	 * 
	 * @return the current icon scale
	 */
	public double getIconScale() {
		return iconScale;
	}

	/**
	 * Sets the scale factor defining the displayed size of the compass icon relative to the icon's width and height in
	 * its image file. Values greater than 1 magify the image, values less than one minify it. If the layer's resize
	 * behavior is other than AVKey.RESIZE_KEEP_FIXED_SIZE, the icon's displayed sized is further affected by the value
	 * specified by {@link #setCompassToViewportScale(double)} and the current viewport size.
	 * <p/>
	 * The default icon scale is 0.5.
	 * 
	 * @param iconScale
	 *            the icon scale factor
	 */
	public void setIconScale(double iconScale) {
		this.iconScale = iconScale;
	}

	/**
	 * Returns the compass icon's resize behavior.
	 * 
	 * @return the icon's resize behavior
	 */
	public String getResizeBehavior() {
		return resizeBehavior;
	}

	/**
	 * Sets the behavior the layer uses to size the compass icon when the viewport size changes, typically when the
	 * World Wind window is resized. If the value is AVKey.RESIZE_KEEP_FIXED_SIZE, the icon size is kept to the size
	 * specified in its image file scaled by the layer's current icon scale. If the value is AVKey.RESIZE_STRETCH, the
	 * icon is resized to have a constant size relative to the current viewport size. If the viewport shrinks the icon
	 * size decreases; if it expands then the icon file enlarges. The relative size is determined by the current
	 * compass-to-viewport scale and by the icon's image file size scaled by the current icon scale. If the value is
	 * AVKey.RESIZE_SHRINK_ONLY (the default), icon sizing behaves as for AVKey.RESIZE_STRETCH but the icon will not
	 * grow larger than the size specified in its image file scaled by the current icon scale.
	 * 
	 * @param resizeBehavior
	 *            the desired resize behavior
	 */
	public void setResizeBehavior(String resizeBehavior) {
		this.resizeBehavior = resizeBehavior;
	}

	public int getBorderWidth() {
		return borderWidth;
	}

	/**
	 * Sets the compass icon offset from the viewport border.
	 * 
	 * @param borderWidth
	 *            the number of pixels to offset the compass icon from the borders indicated by {@link #setPosition(String)}.
	 */
	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
	}

	/**
	 * Returns the current relative compass icon position.
	 * 
	 * @return the current compass position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * Sets the relative viewport location to display the compass icon. Can be one of AVKey.NORTHEAST (the default),
	 * AVKey.NORTHWEST, AVKey.SOUTHEAST, or AVKey.SOUTHWEST. These indicate the corner of the viewport to place the
	 * icon.
	 * 
	 * @param position
	 *            the desired compass position
	 */
	public void setPosition(String position) {
		if (position == null) {
			String message = Logging.getMessage("nullValue.CompassPositionIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}
		this.position = position;
	}

	/**
	 * Returns the current compass image location.
	 * 
	 * @return the current location center. May be null.
	 */
	public Vec4 getLocationCenter() {
		return locationCenter;
	}

	/**
	 * Specifies the screen location of the compass image, relative to the image's center. May be null. If this value is
	 * non-null, it overrides the position specified by {@link #setPosition(String)}. The location is specified in
	 * pixels. The origin is the window's lower left corner. Positive X values are to the right of the origin, positive
	 * Y values are upwards from the origin. The final image location will be affected by the currently specified
	 * location offset if a non-null location offset has been specified (see {@link #setLocationOffset(gov.nasa.worldwind.geom.Vec4)}).
	 * 
	 * @param locationCenter
	 *            the location center. May be null.
	 * @see #setPosition(String)
	 * @see #setLocationOffset(gov.nasa.worldwind.geom.Vec4)
	 */
	public void setLocationCenter(Vec4 locationCenter) {
		this.locationCenter = locationCenter;
	}

	/**
	 * Returns the current location offset. See #setLocationOffset for a description of the offset and its values.
	 * 
	 * @return the location offset. Will be null if no offset has been specified.
	 */
	public Vec4 getLocationOffset() {
		return locationOffset;
	}

	/**
	 * Specifies a placement offset from the compass' position on the screen.
	 * 
	 * @param locationOffset
	 *            the number of pixels to shift the compass image from its specified screen position. A
	 *            positive X value shifts the image to the right. A positive Y value shifts the image up. If
	 *            null, no offset is applied. The default offset is null.
	 * @see #setLocationCenter(gov.nasa.worldwind.geom.Vec4)
	 * @see #setPosition(String)
	 */
	public void setLocationOffset(Vec4 locationOffset) {
		this.locationOffset = locationOffset;
	}

	protected void doRender(DrawContext dc) {
		dc.addOrderedRenderable(this.orderedImage);
	}

	protected void doPick(DrawContext dc, Point pickPoint) {
		dc.addOrderedRenderable(this.orderedImage);
	}

	public boolean isShowTilt() {
		return showTilt;
	}

	public void setShowTilt(boolean showTilt) {
		this.showTilt = showTilt;
	}

	protected void draw(DrawContext dc) {
		if (this.getIconFilePath() == null) return;

		try {
			GLES20.glDisable(GLES20.GL_DEPTH_TEST);

			double width = this.getScaledIconWidth();
			double height = this.getScaledIconHeight();

			// Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
			// into the GL projection matrix.
			Rect viewport = dc.getView().getViewport();
			double maxwh = width > height ? width : height;
			if (maxwh == 0) maxwh = 1;
			Matrix projection = Matrix.fromIdentity().setOrthographic(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

			Matrix modelview = Matrix.fromIdentity();
			double scale = this.computeScale(viewport);
			Vec4 locationSW = this.computeLocation(viewport, scale);
			double heading = this.computeHeading(/* XXX dc, */dc.getView());
			double pitch = this.computePitch(/* XXX dc, */dc.getView());

			modelview.multiplyAndSet(Matrix.fromTranslation(locationSW.x, locationSW.y, locationSW.z));
			modelview.multiplyAndSet(Matrix.fromScale(scale, scale, 1));

			if (!dc.isPickingMode()) {
				modelview.multiplyAndSet(Matrix.fromTranslation(width / 2, height / 2, 0));
				if (this.showTilt) // formula contributed by Ty Hayden
				modelview.multiplyAndSet(Matrix.fromRotationX(Angle.fromDegrees(70d * (pitch / 90.0))));
				modelview.multiplyAndSet(Matrix.fromRotationZ(Angle.fromDegrees(-heading)));
				modelview.multiplyAndSet(Matrix.fromTranslation(-width / 2, -height / 2, 0));
				modelview.multiplyAndSet(Matrix.fromScale(width, height, 1d));

				Matrix mvp = Matrix.fromIdentity().multiplyAndSet(projection, modelview);

				GpuTexture iconTexture = dc.getGpuResourceCache().getTexture(this.getIconFilePath());
				if (iconTexture == null) {
					this.initializeTexture(dc);
					iconTexture = dc.getGpuResourceCache().getTexture(this.getIconFilePath());
					// iconTexture = dc.getTextureCache().getTexture(this.getIconFilePath());
					if (iconTexture == null) {
						String msg = Logging.getMessage("generic.ImageReadFailed");
						Logging.error(msg);
						return;
					}
				}

				GpuProgram textureProgram = this.getGpuProgram(dc.getGpuResourceCache(), programTextureKey, VERTEX_SHADER_PATH_TEXTURE, FRAGMENT_SHADER_PATH_TEXTURE);
				if (iconTexture != null && textureProgram != null) {
					textureProgram.bind();
					textureProgram.loadUniformMatrix("mvpMatrix", mvp);
					GLES20.glEnable(GLES20.GL_TEXTURE_2D);
					GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
					iconTexture.bind();
					textureProgram.loadUniformSampler("sTexture", 0);

					GLES20.glEnable(GLES20.GL_BLEND);
					GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

					float[] unitQuadVerts = new float[] { 0, 0, 1, 0, 1, 1, 0, 1 };
					int pointLocation = textureProgram.getAttribLocation("vertexPoint");
					GLES20.glEnableVertexAttribArray(pointLocation);
					FloatBuffer vertexBuf = ByteBuffer.allocateDirect(unitQuadVerts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
					vertexBuf.put(unitQuadVerts);
					vertexBuf.rewind();
					GLES20.glVertexAttribPointer(pointLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuf);
					float[] textureVerts = new float[] { 0, 1, 1, 1, 1, 0, 0, 0 };
					int textureLocation = textureProgram.getAttribLocation("aTextureCoord");
					GLES20.glEnableVertexAttribArray(textureLocation);
					FloatBuffer textureBuf = ByteBuffer.allocateDirect(textureVerts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
					textureBuf.put(textureVerts);
					textureBuf.rewind();
					GLES20.glVertexAttribPointer(textureLocation, 2, GLES20.GL_FLOAT, false, 0, textureBuf);
					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, unitQuadVerts.length / 2);
					GLES20.glDisableVertexAttribArray(pointLocation);
					GLES20.glDisableVertexAttribArray(textureLocation);
					GLES20.glUseProgram(0);
				}
			} else {
				// Picking - XXX This else has not been tested, it could make rendering crash! Be aware!
				this.pickSupport.clearPickList();
				this.pickSupport.beginPicking(dc);
				try {
					// Add a picked object for the compass to the list of pickable objects.
					int colorCode = dc.getPickColor(dc.getPickPoint());
					PickedObject po = new PickedObject(colorCode, this, null, false);
					this.pickSupport.addPickableObject(po);
					//
					if (dc.getPickPoint() != null) {
						// If the pick point is not null, compute the pick point 'heading' relative to the compass
						// center and set the picked heading on our picked object. The pick point is null if a pick
						// rectangle is specified but a pick point is not.
						Vec4 center = new Vec4(locationSW.x + width * scale / 2, locationSW.y + height * scale / 2, 0);
						double px = dc.getPickPoint().x - center.x;
						double py = viewport.height - dc.getPickPoint().y - center.y;
						Angle pickHeading = Angle.fromRadians(Math.atan2(px, py));
						pickHeading = pickHeading.degrees >= 0 ? pickHeading : pickHeading.addDegrees(360);
						po.setValue("Heading", pickHeading);
					}
					// Draw the compass in the unique pick color. gl.glColor3ub((byte) color.getRed(), (byte) color.getGreen(), (byte) color.getBlue());
					GpuProgram textureProgram = this.getGpuProgram(dc.getGpuResourceCache(), programTextureKey, VERTEX_SHADER_PATH_TEXTURE, FRAGMENT_SHADER_PATH_TEXTURE);
					textureProgram.bind();

					modelview.multiplyAndSet(Matrix.fromScale(width, height, 1d));
					float[] unitQuadVerts = new float[] { 0, 0, 1, 0, 1, 1, 0, 1 };
					FloatBuffer vertexBuf = ByteBuffer.allocateDirect(unitQuadVerts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
					vertexBuf.put(unitQuadVerts);
					vertexBuf.rewind();
					int pointLocation = textureProgram.getAttribLocation("vertexPoint");
					GLES20.glEnableVertexAttribArray(pointLocation);
					GLES20.glVertexAttribPointer(pointLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuf);
					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, unitQuadVerts.length / 2);
					GLES20.glDisableVertexAttribArray(pointLocation);
					GLES20.glUseProgram(0);
					// dc.drawUnitQuad();
				} finally {
					// Done picking
					this.pickSupport.endPicking(dc);
					this.pickSupport.resolvePick(dc, dc.getPickPoint(), this);
				}
			}
		} finally {

			if (!dc.isPickingMode()) {
				GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
				GLES20.glDisable(GLES20.GL_TEXTURE_2D); // restore to default texture state
				GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
			}
		}
	}

	protected double computeScale(Rect viewport) {
		if (this.resizeBehavior.equals(AVKey.RESIZE_SHRINK_ONLY)) {
			return Math.min(1d, (this.compassToViewportScale) * viewport.width / this.getScaledIconWidth());
		} else if (this.resizeBehavior.equals(AVKey.RESIZE_STRETCH)) {
			return (this.compassToViewportScale) * viewport.width / this.getScaledIconWidth();
		} else if (this.resizeBehavior.equals(AVKey.RESIZE_KEEP_FIXED_SIZE)) {
			return 1d;
		} else {
			return 1d;
		}
	}

	protected double getScaledIconWidth() {
		return this.iconWidth * this.iconScale;
	}

	protected double getScaledIconHeight() {
		return this.iconHeight * this.iconScale;
	}

	protected Vec4 computeLocation(Rect viewport, double scale) {
		double width = this.getScaledIconWidth();
		double height = this.getScaledIconHeight();

		double scaledWidth = scale * width;
		double scaledHeight = scale * height;

		double x;
		double y;

		if (this.locationCenter != null) {
			x = this.locationCenter.x - scaledWidth / 2;
			y = this.locationCenter.y - scaledHeight / 2;
		} else if (this.position.equals(AVKey.NORTHEAST)) {
			x = viewport.width - scaledWidth - this.borderWidth;
			y = viewport.height - scaledHeight - this.borderWidth;
		} else if (this.position.equals(AVKey.SOUTHEAST)) {
			x = viewport.width - scaledWidth - this.borderWidth;
			y = 0d + this.borderWidth;
		} else if (this.position.equals(AVKey.NORTHWEST)) {
			x = 0d + this.borderWidth;
			y = viewport.height - scaledHeight - this.borderWidth;
		} else if (this.position.equals(AVKey.SOUTHWEST)) {
			x = 0d + this.borderWidth;
			y = 0d + this.borderWidth;
		} else // use North East as default
		{
			x = viewport.width - scaledWidth - this.borderWidth;
			y = viewport.height - scaledHeight - this.borderWidth;
		}

		if (this.locationOffset != null) {
			x += this.locationOffset.x;
			y += this.locationOffset.y;
		}

		return new Vec4(x, y, 0);
	}

	protected double computeHeading(View view) {
		if (view == null) return 0.0;

		return -view.getHeading().degrees;
	}

	protected double computeHeading(DrawContext dc, View view) {
		if (view == null) return 0.0;

		if (!(view instanceof BasicView)) return 0.0;

		BasicView basicView = (BasicView) view;
		return -basicView.getLookAtHeading(dc.getGlobe()).degrees;
	}

	protected double computePitch(View view) {
		if (view == null) return 0.0;

		if (!(view instanceof BasicView)) return 0.0;
		BasicView basicView = (BasicView) view;
		return basicView.getTilt().degrees;

		/*
		 * if (!(view instanceof OrbitView)) return 0.0;
		 * OrbitView orbitView = (OrbitView) view;
		 * return orbitView.getPitch().getDegrees();
		 */
	}

	protected double computePitch(DrawContext dc, View view) {
		if (view == null) return 0.0;

		if (!(view instanceof BasicView)) return 0.0;

		BasicView basicView = (BasicView) view;
		return basicView.getLookAtTilt(dc.getGlobe()).degrees;
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

	protected void initializeTexture(DrawContext dc) {
		GpuTexture iconTexture = dc.getGpuResourceCache().getTexture(this.getIconFilePath());
		if (iconTexture != null) return;
		try {
			InputStream iconStream = this.getClass().getResourceAsStream("/" + this.getIconFilePath());
			if (iconStream == null) {
				File iconFile = new File(this.iconFilePath);
				if (iconFile.exists()) {
					iconStream = new FileInputStream(iconFile);
				}
			}

			iconTexture = GpuTexture.createTexture(dc, GpuTextureData.createTextureData(iconStream));// TextureIO.newTexture(iconStream, false, null);
			iconTexture.bind();
			this.iconWidth = iconTexture.getWidth();
			this.iconHeight = iconTexture.getHeight();
			dc.getGpuResourceCache().put(this.getIconFilePath(), iconTexture);
		} catch (IOException e) {
			String msg = Logging.getMessage("layers.IOExceptionDuringInitialization");
			Logging.error(msg);
			throw new WWRuntimeException(msg, e);
		}
		// GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);// _MIPMAP_LINEAR);
		// GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
		// GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
		// GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
	}

	@Override
	public String toString() {
		return Logging.getMessage("layers.CompassLayer.Name");
	}
}
