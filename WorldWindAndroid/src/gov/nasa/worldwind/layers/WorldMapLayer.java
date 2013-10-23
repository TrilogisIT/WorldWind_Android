/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Rect;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickSupport;
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
import java.util.ArrayList;
import java.util.List;
import android.graphics.Point;
import android.opengl.GLES20;

/**
 * Displays a world map overlay with a current-position crosshair in a screen corner.
 * <p/>
 * A {@link gov.nasa.worldwind.examples.ClickAndGoSelectListener} can be used in conjunction with this layer to move the view to a selected location when that
 * location is clicked within the layer's map. Specify <code>WorldMapLayer.class</code> when constructing the <code>ClickAndGoSelectListener</code>.
 * <p/>
 * Note: This layer may not be shared among multiple {@link WorldWindow}s. Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author Nicola Dorigatti
 * @version $Id: WorldMapLayer.java 1 2011-07-16 23:22:47Z dcollins $
 */
public class WorldMapLayer extends AbstractLayer {
	protected static final String VERTEX_SHADER_PATH_COLOR = "shaders/WorldMapLayerColor.vert";
	protected static final String FRAGMENT_SHADER_PATH_COLOR = "shaders/WorldMapLayerColor.frag";
	protected static final String VERTEX_SHADER_PATH_TEXTURE = "shaders/WorldMapLayerTexture.vert";
	protected static final String FRAGMENT_SHADER_PATH_TEXTURE = "shaders/WorldMapLayerTexture.frag";

	protected String iconFilePath = "images/earth-map-512x256.png";;
	protected double toViewportScale = 0.2;
	protected double iconScale = 0.5;
	protected int borderWidth = 20;
	protected String position = AVKey.NORTHWEST;
	protected String resizeBehavior = AVKey.RESIZE_SHRINK_ONLY;
	protected int iconWidth = 512;
	protected int iconHeight = 256;
	protected Vec4 locationCenter = null;
	protected Vec4 locationOffset = null;
	protected float[] color = new float[] { 255f, 255f, 255f, 255f };
	protected float[] backColor = new float[] { 0f, 0f, 0f, 0.4f };
	protected boolean showFootprint = true;
	protected ArrayList<? extends LatLon> footPrintPositions;
	protected PickSupport pickSupport = new PickSupport();
	protected final Object programColorKey = new Object();
	protected final Object programTextureKey = new Object();
	// Draw it as ordered with an eye distance of 0 so that it shows up in front of most other things.
	protected OrderedIcon orderedImage = new OrderedIcon();

	protected class OrderedIcon implements OrderedRenderable {
		public double getDistanceFromEye() {
			return 0;
		}

		public void pick(DrawContext dc, Point pickPoint) {
			WorldMapLayer.this.drawIcon(dc);
		}

		public void render(DrawContext dc) {
			WorldMapLayer.this.drawIcon(dc);
		}
	}

	/** Displays a world map overlay with a current position crosshair in a screen corner */
	public WorldMapLayer() {
		this.setOpacity(0.6);
		String configFilePath = Configuration.getStringValue(AVKey.WORLD_MAP_IMAGE_PATH);
		if (null != configFilePath && !configFilePath.trim().isEmpty()) {
			this.setIconFilePath(Configuration.getStringValue(AVKey.WORLD_MAP_IMAGE_PATH));
		}
	}

	/**
	 * Displays a world map overlay with a current position crosshair in a screen corner
	 * 
	 * @param iconFilePath
	 *            the world map image path and filename
	 */
	public WorldMapLayer(String iconFilePath) {
		this.setOpacity(0.6);
		this.setIconFilePath(iconFilePath);
	}

	// Public properties

	/**
	 * Returns the layer's current icon file path.
	 * 
	 * @return the icon file path
	 */
	public String getIconFilePath() {
		return iconFilePath;
	}

	/**
	 * Sets the world map icon's image location. The layer first searches for this location in the current Java
	 * classpath. If not found then the specified path is assumed to refer to the local file system. found there then
	 * the
	 * 
	 * @param iconFilePath
	 *            the path to the icon's image file
	 */
	public void setIconFilePath(String iconFilePath) {
		if (iconFilePath == null || iconFilePath.trim().isEmpty()) {
			String message = Logging.getMessage("nullValue.FilePathIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}
		this.iconFilePath = iconFilePath;
	}

	/**
	 * Returns the layer's world map-to-viewport scale factor.
	 * 
	 * @return the world map-to-viewport scale factor
	 */
	public double getToViewportScale() {
		return toViewportScale;
	}

	/**
	 * Sets the scale factor applied to the viewport size to determine the displayed size of the world map icon. This
	 * scale factor is used only when the layer's resize behavior is AVKey.RESIZE_STRETCH or AVKey.RESIZE_SHRINK_ONLY.
	 * The icon's width is adjusted to occupy the proportion of the viewport's width indicated by this factor. The
	 * icon's height is adjusted to maintain the world map image's native aspect ratio.
	 * 
	 * @param toViewportScale
	 *            the world map to viewport scale factor
	 */
	public void setToViewportScale(double toViewportScale) {
		this.toViewportScale = toViewportScale;
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
	 * Sets the scale factor defining the displayed size of the world map icon relative to the icon's width and height
	 * in its image file. Values greater than 1 magify the image, values less than one minify it. If the layer's resize
	 * behavior is other than AVKey.RESIZE_KEEP_FIXED_SIZE, the icon's displayed sized is further affected by the value
	 * specified by {@link #setToViewportScale(double)} and the current viewport size.
	 * 
	 * @param iconScale
	 *            the icon scale factor
	 */
	public void setIconScale(double iconScale) {
		this.iconScale = iconScale;
	}

	/**
	 * Returns the world map icon's resize behavior.
	 * 
	 * @return the icon's resize behavior
	 */
	public String getResizeBehavior() {
		return resizeBehavior;
	}

	/**
	 * Sets the behavior the layer uses to size the world map icon when the viewport size changes, typically when the
	 * World Wind window is resized. If the value is AVKey.RESIZE_KEEP_FIXED_SIZE, the icon size is kept to the size
	 * specified in its image file scaled by the layer's current icon scale. If the value is AVKey.RESIZE_STRETCH, the
	 * icon is resized to have a constant size relative to the current viewport size. If the viewport shrinks the icon
	 * size decreases; if it expands then the icon file enlarges. The relative size is determined by the current world
	 * map-to-viewport scale and by the icon's image file size scaled by the current icon scale. If the value is
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
	 * Sets the world map icon offset from the viewport border.
	 * 
	 * @param borderWidth
	 *            the number of pixels to offset the world map icon from the borders indicated by {@link #setPosition(String)}.
	 */
	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
	}

	/**
	 * Returns the current relative world map icon position.
	 * 
	 * @return the current world map position
	 */
	public String getPosition() {
		return position;
	}

	/**
	 * Sets the relative viewport location to display the world map icon. Can be one of AVKey.NORTHEAST, AVKey.NORTHWEST
	 * (the default), AVKey.SOUTHEAST, or SOUTHWEST. These indicate the corner of the viewport to place the icon.
	 * 
	 * @param position
	 *            the desired world map position
	 */
	public void setPosition(String position) {
		if (position == null) {
			String message = Logging.getMessage("nullValue.PositionIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}
		this.position = position;
	}

	/**
	 * Returns the current worldmap image location.
	 * 
	 * @return the current location center. May be null.
	 */
	public Vec4 getLocationCenter() {
		return locationCenter;
	}

	/**
	 * Specifies the screen location of the worldmap image, relative to the image's center. May be null. If this value
	 * is non-null, it overrides the position specified by #setPosition. The location is specified in pixels. The origin
	 * is the window's lower left corner. Positive X values are to the right of the origin, positive Y values are
	 * upwards from the origin. The final image location will be affected by the currently specified location offset if
	 * a non-null location offset has been specified (see #setLocationOffset).
	 * 
	 * @param locationCenter
	 *            the location center. May be null.
	 * @see #locationCenter the screen location at which to place the map.
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
	 * Specifies a placement offset from the worldmap's position on the screen.
	 * 
	 * @param locationOffset
	 *            the number of pixels to shift the worldmap image from its specified screen position. A
	 *            positive X value shifts the image to the right. A positive Y value shifts the image up. If
	 *            null, no offset is applied. The default offset is null.
	 * @see #setLocationCenter(gov.nasa.worldwind.geom.Vec4)
	 * @see #setPosition(String)
	 */
	public void setLocationOffset(Vec4 locationOffset) {
		this.locationOffset = locationOffset;
	}

	public float[] getBackgrounColor() {
		return this.backColor;
	}

	public void setBackgroundColor(float[] color) {
		if (color == null) {
			String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.backColor = color;
	}

	public boolean getShowFootprint() {
		return this.showFootprint;
	}

	public void setShowFootprint(boolean state) {
		this.showFootprint = state;
	}

	/**
	 * Get the current view footprint position list. May be null if no footprint is displayed or none has been
	 * computed.
	 * 
	 * @return the current view footprint position list - may be null.
	 */
	public List<? extends LatLon> getFootPrintPositions() {
		return this.footPrintPositions;
	}

	@Override
	public void doRender(DrawContext dc) {
		// Delegate drawing to the ordered renderable list
		dc.addOrderedRenderable(this.orderedImage);
	}

	@Override
	public void doPick(DrawContext dc, Point pickPoint) {
		// Delegate drawing to the ordered renderable list
		dc.addOrderedRenderable(this.orderedImage);
	}

	protected void drawIcon(DrawContext dc) {
		if (this.getIconFilePath() == null) return;

		try {
			// Initialize texture if necessary
			GpuTexture iconTexture = dc.getGpuResourceCache().getTexture(this.getIconFilePath());

			if (iconTexture == null) {
				this.initializeTexture(dc);
				iconTexture = dc.getGpuResourceCache().getTexture(this.getIconFilePath());
				if (iconTexture == null) {
					String msg = Logging.getMessage("generic.ImageReadFailed");
					Logging.info(msg);
					return;
				}
			}

			GLES20.glDisable(GLES20.GL_DEPTH_TEST);

			// iconWidth = 512;
			double width = this.getScaledIconWidth();
			// iconHeight = 256;
			double height = this.getScaledIconHeight();

			// Load a parallel projection with xy dimensions (viewportWidth, viewportHeight)
			// into the GL projection matrix.
			Rect viewport = dc.getView().getViewport();
			double maxwh = width > height ? width : height;
			Matrix projection = Matrix.fromIdentity().setOrthographic(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

			// Translate and scale
			double scale = this.computeScale(viewport);
			Vec4 locationSW = this.computeLocation(viewport, scale);
			Matrix modelview = Matrix.fromIdentity();
			modelview.multiplyAndSet(Matrix.fromTranslation(locationSW.x, locationSW.y, locationSW.z));
			// Scale to 0..1 space
			modelview.multiplyAndSet(Matrix.fromScale(scale, scale, 1));
			modelview.multiplyAndSet(Matrix.fromScale(width, height, 1));
			// modelview projection matrix
			Matrix mvp = Matrix.fromIdentity().multiplyAndSet(projection, modelview);

			if (!dc.isPickingMode()) {
				GLES20.glEnable(GLES20.GL_BLEND);
				GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
				GpuProgram colorProgram = this.getGpuProgram(dc.getGpuResourceCache(), programColorKey, VERTEX_SHADER_PATH_COLOR, FRAGMENT_SHADER_PATH_COLOR);
				// Draw background color behind the map
				if (colorProgram != null) {
					colorProgram.bind();
					colorProgram.loadUniformMatrix("mvpMatrix", mvp);
					colorProgram.loadUniform4f("uColor", backColor[0], backColor[1], backColor[2], backColor[3] * this.getOpacity());
					float[] unitQuadVerts = new float[] { 0, 0, 1, 0, 1, 1, 0, 1 };

					int pointLocation = colorProgram.getAttribLocation("vertexPoint");
					GLES20.glEnableVertexAttribArray(pointLocation);
					FloatBuffer vertexBuf = ByteBuffer.allocateDirect(unitQuadVerts.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
					vertexBuf.put(unitQuadVerts);
					vertexBuf.rewind();
					GLES20.glVertexAttribPointer(pointLocation, 2, GLES20.GL_FLOAT, false, 0, vertexBuf);
					GLES20.glDrawArrays(GLES20.GL_TRIANGLE_FAN, 0, unitQuadVerts.length / 2);
					GLES20.glDisableVertexAttribArray(pointLocation);
					GLES20.glUseProgram(0);
				}

				// Draw world map icon
				GpuProgram textureProgram = this.getGpuProgram(dc.getGpuResourceCache(), programTextureKey, VERTEX_SHADER_PATH_TEXTURE, FRAGMENT_SHADER_PATH_TEXTURE);
				if (textureProgram != null) {
					textureProgram.bind();
					textureProgram.loadUniformMatrix("mvpMatrix", mvp);
					GLES20.glEnable(GLES20.GL_TEXTURE_2D);
					GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
					iconTexture.bind();
					textureProgram.loadUniformSampler("sTexture", 0);

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
					GLES20.glDisable(GLES20.GL_TEXTURE_2D);
				}
				// Draw crosshair for current location
				modelview = Matrix.fromIdentity();
				modelview.multiplyAndSet(Matrix.fromTranslation(locationSW.x, locationSW.y, locationSW.z));
				// Scale to width x height space
				modelview.multiplyAndSet(Matrix.fromScale(scale, scale, 1));
				mvp = Matrix.fromIdentity().multiplyAndSet(projection, modelview);
				if (colorProgram != null) {
					colorProgram.bind();
					colorProgram.loadUniformMatrix("mvpMatrix", mvp);
					// Set color
					colorProgram.loadUniform4f("uColor", color[0], color[1], color[2], this.getOpacity());
					// Draw crosshair
					Position groundPos = this.computeGroundPosition(dc, dc.getView());
					if (groundPos != null) {
						int x = (int) (width * (groundPos.longitude.degrees + 180) / 360);
						int y = (int) (height * (groundPos.latitude.degrees + 90) / 180);
						int w = 10; // cross branch length
						// Draw
						int pointLocation = colorProgram.getAttribLocation("vertexPoint");
						GLES20.glEnableVertexAttribArray(pointLocation);
						float[] verts = new float[] { x - w, y, 0, x + w + 1, y, 0 };
						FloatBuffer vertBuf = createBuffer(verts);
						GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, vertBuf);
						GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, verts.length / 3);
						verts = new float[] { x, y - w, 0, x, y + w + 1, 0 };
						vertBuf = createBuffer(verts);
						GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, vertBuf);
						GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, verts.length / 3);
						GLES20.glDisableVertexAttribArray(pointLocation);
					}
				}
				// Draw view footprint in map icon space
				if (this.showFootprint) {
					this.footPrintPositions = this.computeViewFootPrint(dc, 32);
					if (this.footPrintPositions != null && colorProgram != null) {
						ArrayList<ArrayList<Float>> lineStrips = new ArrayList<ArrayList<Float>>();
						ArrayList<Float> curLineStrip = new ArrayList<Float>();
						lineStrips.add(curLineStrip);
						LatLon p1 = this.footPrintPositions.get(0);
						for (LatLon p2 : this.footPrintPositions) {
							int x = (int) (width * (p2.longitude.degrees + 180) / 360);
							int y = (int) (height * (p2.latitude.degrees + 90) / 180);
							// Draw
							if (LatLon.locationsCrossDateline(p1, p2)) {
								int y1 = (int) (height * (p1.latitude.degrees + 90) / 180);
								curLineStrip.add((float) (x < width / 2 ? width : 0));
								curLineStrip.add((float) ((y1 + y) / 2));
								curLineStrip.add((float) 0);
								curLineStrip = new ArrayList<Float>();
								lineStrips.add(curLineStrip);
								curLineStrip.add((float) (x < width / 2 ? 0 : width));
								curLineStrip.add((float) ((y1 + y) / 2));
								curLineStrip.add((float) 0);
							}
							curLineStrip.add((float) x);
							curLineStrip.add((float) y);
							curLineStrip.add((float) 0);
							p1 = p2;
						}
						int pointLocation = colorProgram.getAttribLocation("vertexPoint");
						GLES20.glEnableVertexAttribArray(pointLocation);
						for (ArrayList<Float> lineStrip : lineStrips) {
							float[] verts = convertToArray(lineStrip);
							FloatBuffer vertBuf = createBuffer(verts);
							GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, vertBuf);
							GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, verts.length / 3);
						}
						GLES20.glDisableVertexAttribArray(pointLocation);
					}
				}
				// Draw 1px border around and inside the map
				if (colorProgram != null) {
					int pointLocation = colorProgram.getAttribLocation("vertexPoint");
					GLES20.glEnableVertexAttribArray(pointLocation);
					float[] vertices = new float[] { 0, 0, 0, (float) width, 0, 0, (float) width, (float) (height - 1), 0, 0, (float) (height - 1), 0, 0, 0, 0 };
					FloatBuffer vertBuf = createBuffer(vertices);
					GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, vertBuf);
					GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, vertices.length / 3);
					GLES20.glDisableVertexAttribArray(pointLocation);
				}
			} else {
				// Picking TODO Copied from compass layer and not tested
				this.pickSupport.clearPickList();
				this.pickSupport.beginPicking(dc);
				// Where in the world are we picking ?
				Position pickPosition = computePickPosition(dc, locationSW, new Rect(0, 0, (width * scale), (height * scale)));
				int colorCode = dc.getPickColor(dc.getPickPoint());
				this.pickSupport.addPickableObject(colorCode, this, pickPosition, false);
				GpuProgram textureProgram = this.getGpuProgram(dc.getGpuResourceCache(), programTextureKey, VERTEX_SHADER_PATH_TEXTURE, FRAGMENT_SHADER_PATH_TEXTURE);
				textureProgram.bind();
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
				this.pickSupport.endPicking(dc);
				this.pickSupport.resolvePick(dc, dc.getPickPoint(), this);
			}
		} catch (Throwable t) {
			t.printStackTrace();
			Logging.error("Exception drawing WorldMapLayer: " + t.getMessage());
		} finally {
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		}
	}

	private float[] convertToArray(ArrayList<Float> lineStrip) {
		float[] retval = new float[lineStrip.size()];
		int index = 0;
		for (Float f : lineStrip) {
			retval[index] = f;
			index++;
		}
		return retval;
	}

	protected FloatBuffer createBuffer(float[] array) {
		FloatBuffer retval = ByteBuffer.allocateDirect(array.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		retval.put(array);
		retval.rewind();
		return retval;
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

	protected double computeScale(Rect viewport) {
		if (this.resizeBehavior.equals(AVKey.RESIZE_SHRINK_ONLY)) {
			return Math.min(1d, (this.toViewportScale) * viewport.width / this.getScaledIconWidth());
		} else if (this.resizeBehavior.equals(AVKey.RESIZE_STRETCH)) {
			return (this.toViewportScale) * viewport.width / this.getScaledIconWidth();
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
		} else // use North East
		{
			x = viewport.width - scaledWidth / 2 - this.borderWidth;
			y = viewport.height - scaledHeight / 2 - this.borderWidth;
		}

		if (this.locationOffset != null) {
			x += this.locationOffset.x;
			y += this.locationOffset.y;
		}

		return new Vec4(x, y, 0);
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

	}

	/**
	 * Compute the lat/lon position of the view center
	 * 
	 * @param dc
	 *            the current DrawContext
	 * @param view
	 *            the current View
	 * @return the ground position of the view center or null
	 */
	protected Position computeGroundPosition(DrawContext dc, View view) {
		if (view == null) return null;

		Position groundPosition = new Position();
		boolean success = view
				.computePositionFromScreenPoint(dc.getGlobe(), new Point((int) (view.getViewport().width / 2), (int) (view.getViewport().height / 2)), groundPosition);
		if (!success) return null;

		double elevation = dc.getGlobe().getElevation(groundPosition.latitude, groundPosition.longitude);
		return new Position(groundPosition.latitude, groundPosition.longitude, elevation * dc.getVerticalExaggeration());
	}

	/**
	 * Computes the lat/lon of the pickPoint over the world map
	 * 
	 * @param dc
	 *            the current <code>DrawContext</code>
	 * @param locationSW
	 *            the screen location of the bottom left corner of the map
	 * @param mapSize
	 *            the world map screen dimension in pixels
	 * @return the picked Position
	 */
	protected Position computePickPosition(DrawContext dc, Vec4 locationSW, Rect mapSize) {
		Position pickPosition = null;
		Point pickPoint = dc.getPickPoint();
		if (pickPoint != null) {
			Rect viewport = dc.getView().getViewport();
			// Check if pickpoint is inside the map
			if (pickPoint.x >= locationSW.x && pickPoint.x < locationSW.x + mapSize.width && viewport.height - pickPoint.y >= locationSW.y
					&& viewport.height - pickPoint.y < locationSW.y + mapSize.height) {
				double lon = (pickPoint.x - locationSW.x) / mapSize.width * 360 - 180;
				double lat = (viewport.height - pickPoint.y - locationSW.y) / mapSize.height * 180 - 90;
				double pickAltitude = 1000e3;
				pickPosition = new Position(Angle.fromDegrees(lat), Angle.fromDegrees(lon), pickAltitude);
			}
		}
		return pickPosition;
	}

	/**
	 * Compute the view range footprint on the globe.
	 * 
	 * @param dc
	 *            the current <code>DrawContext</code>
	 * @param steps
	 *            the number of steps.
	 * @return an array list of <code>LatLon</code> forming a closed shape.
	 */
	protected ArrayList<LatLon> computeViewFootPrint(DrawContext dc, int steps) {
		ArrayList<LatLon> positions = new ArrayList<LatLon>();
		Position eyePos = dc.getView().getEyePosition(dc.getGlobe());
		Angle distance = Angle.fromRadians(Math.asin(dc.getView().getFarClipDistance() / (dc.getGlobe().getRadius() + eyePos.elevation)));
		if (distance.degrees > 10) {
			double headStep = 360d / steps;
			Angle heading = Angle.fromDegrees(0);
			for (int i = 0; i <= steps; i++) {
				LatLon p = LatLon.greatCircleEndPosition(eyePos, heading, distance);
				positions.add(p);
				heading = heading.addDegrees(headStep);
			}
			return positions;
		} else return null;
	}

	@Override
	public String toString() {
		return Logging.getMessage("layers.Earth.WorldMapLayer.Name");
	}

}
