/*
 * Copyright (C) 2011 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Rect;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GpuProgram;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.TextRenderer;
import gov.nasa.worldwind.util.Logging;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.opengl.GLES20;

/**
 * Renders a scalebar graphic in a screen corner.
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author Patrick Murris
 * @version $Id: ScalebarLayer.java 508 2012-04-06 01:05:50Z tgaskins $
 */
public class ScalebarLayer extends AbstractLayer {
	protected static final String VERTEX_SHADER_PATH_COLOR = "shaders/ScalebarLayerColor.vert";
	protected static final String FRAGMENT_SHADER_PATH_COLOR = "shaders/ScalebarLayerColor.frag";
	// Units constants
	public final static String UNIT_METRIC = "gov.nasa.worldwind.ScalebarLayer.Metric";
	public final static String UNIT_IMPERIAL = "gov.nasa.worldwind.ScalebarLayer.Imperial";
	public final static String UNIT_NAUTICAL = "gov.nasa.worldwind.ScalebarLayer.Nautical";

	// Display parameters - TODO: make configurable
	private Rect size = new Rect(0, 0, 150, 10);
	private float[] color = new float[] { 1, 1, 1, 1 };
	private int borderWidth = 20;
	private String position = AVKey.SOUTHEAST;
	private String resizeBehavior = AVKey.RESIZE_SHRINK_ONLY;
	private String unit = UNIT_METRIC;
	private Paint defaultPaint;
	private double toViewportScale = 0.2;

	@SuppressWarnings("unused")
	private PickSupport pickSupport = new PickSupport();
	private Vec4 locationCenter = null;
	private Vec4 locationOffset = null;
	private double pixelSize;
	protected final Object programColorKey = new Object();

	// Draw it as ordered with an eye distance of 0 so that it shows up in front
	// of most other things.
	// TODO: Add general support for this common pattern.
	private OrderedIcon orderedImage = new OrderedIcon();
	private TextRenderer textRenderer;

	private class OrderedIcon implements OrderedRenderable {
		public double getDistanceFromEye() {
			return 0;
		}

		public void pick(DrawContext dc, Point pickPoint) {
			ScalebarLayer.this.draw(dc);
		}

		public void render(DrawContext dc) {
			ScalebarLayer.this.draw(dc);
		}
	}

	/** Renders a scalebar graphic in a screen corner */
	public ScalebarLayer() {
		setPickEnabled(false);
		defaultPaint = new Paint();
		defaultPaint.setColor(Color.WHITE);
	}

	// Public properties

	/**
	 * Get the apparent pixel size in meter at the reference position.
	 * 
	 * @return the apparent pixel size in meter at the reference position.
	 */
	public double getPixelSize() {
		return this.pixelSize;
	}

	/**
	 * Get the scalebar graphic Dimension (in pixels)
	 * 
	 * @return the scalebar graphic Dimension
	 */
	public Rect getSize() {
		return this.size;
	}

	/**
	 * Set the scalebar graphic Dimenion (in pixels)
	 * 
	 * @param size
	 *            the scalebar graphic Dimension
	 */
	public void setSize(Rect size) {
		if (size == null) {
			String message = Logging.getMessage("nullValue.DimensionIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}
		this.size = size;
	}

	/**
	 * Get the scalebar color
	 * 
	 * @return the scalebar Color
	 */
	public float[] getColor() {
		return this.color;
	}

	/**
	 * Set the scalbar Color
	 * 
	 * @param color
	 *            the scalebar Color
	 */
	public void setColor(float[] color) {
		if (color == null) {
			String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.color = color;
	}

	/**
	 * Returns the scalebar-to-viewport scale factor.
	 * 
	 * @return the scalebar-to-viewport scale factor
	 */
	public double getToViewportScale() {
		return toViewportScale;
	}

	/**
	 * Sets the scale factor applied to the viewport size to determine the
	 * displayed size of the scalebar. This scale factor is used only when the
	 * layer's resize behavior is AVKey.RESIZE_STRETCH or
	 * AVKey.RESIZE_SHRINK_ONLY. The scalebar's width is adjusted to occupy the
	 * proportion of the viewport's width indicated by this factor. The
	 * scalebar's height is adjusted to maintain the scalebar's Dimension aspect
	 * ratio.
	 * 
	 * @param toViewportScale
	 *            the scalebar to viewport scale factor
	 */
	public void setToViewportScale(double toViewportScale) {
		this.toViewportScale = toViewportScale;
	}

	public String getPosition() {
		return this.position;
	}

	/**
	 * Sets the relative viewport location to display the scalebar. Can be one
	 * of AVKey.NORTHEAST, AVKey.NORTHWEST, AVKey.SOUTHEAST (the default), or
	 * AVKey.SOUTHWEST. These indicate the corner of the viewport.
	 * 
	 * @param position
	 *            the desired scalebar position
	 */
	public void setPosition(String position) {
		if (position == null) {
			String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.position = position;
	}

	/**
	 * Returns the current scalebar center location.
	 * 
	 * @return the current location center. May be null.
	 */
	public Vec4 getLocationCenter() {
		return locationCenter;
	}

	/**
	 * Specifies the screen location of the scalebar center. May be null. If
	 * this value is non-null, it overrides the position specified by
	 * #setPosition. The location is specified in pixels. The origin is the
	 * window's lower left corner. Positive X values are to the right of the
	 * origin, positive Y values are upwards from the origin. The final scalebar
	 * location will be affected by the currently specified location offset if a
	 * non-null location offset has been specified (see #setLocationOffset).
	 * 
	 * @param locationCenter
	 *            the scalebar center. May be null.
	 * @see #setPosition
	 * @see #setLocationOffset
	 */
	public void setLocationCenter(Vec4 locationCenter) {
		this.locationCenter = locationCenter;
	}

	/**
	 * Returns the current location offset. See #setLocationOffset for a
	 * description of the offset and its values.
	 * 
	 * @return the location offset. Will be null if no offset has been
	 *         specified.
	 */
	public Vec4 getLocationOffset() {
		return locationOffset;
	}

	/**
	 * Specifies a placement offset from the scalebar's position on the screen.
	 * 
	 * @param locationOffset
	 *            the number of pixels to shift the scalebar from its specified
	 *            screen position. A positive X value shifts the image to the
	 *            right. A positive Y value shifts the image up. If null, no
	 *            offset is applied. The default offset is null.
	 * @see #setLocationCenter
	 * @see #setPosition
	 */
	public void setLocationOffset(Vec4 locationOffset) {
		this.locationOffset = locationOffset;
	}

	/**
	 * Returns the layer's resize behavior.
	 * 
	 * @return the layer's resize behavior
	 */
	public String getResizeBehavior() {
		return resizeBehavior;
	}

	/**
	 * Sets the behavior the layer uses to size the scalebar when the viewport
	 * size changes, typically when the World Wind window is resized. If the
	 * value is AVKey.RESIZE_KEEP_FIXED_SIZE, the scalebar size is kept to the
	 * size specified in its Dimension scaled by the layer's current icon scale.
	 * If the value is AVKey.RESIZE_STRETCH, the scalebar is resized to have a
	 * constant size relative to the current viewport size. If the viewport
	 * shrinks the scalebar size decreases; if it expands then the scalebar
	 * enlarges. If the value is AVKey.RESIZE_SHRINK_ONLY (the default),
	 * scalebar sizing behaves as for AVKey.RESIZE_STRETCH but it will not grow
	 * larger than the size specified in its Dimension.
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
	 * Sets the scalebar offset from the viewport border.
	 * 
	 * @param borderWidth
	 *            the number of pixels to offset the scalebar from the borders
	 *            indicated by {@link #setPosition(String)}.
	 */
	public void setBorderWidth(int borderWidth) {
		this.borderWidth = borderWidth;
	}

	public String getUnit() {
		return this.unit;
	}

	/**
	 * Sets the unit the scalebar uses to display distances. Can be one of {@link #UNIT_METRIC} (the default), or {@link #UNIT_IMPERIAL}.
	 * 
	 * @param unit
	 *            the desired unit
	 */
	public void setUnit(String unit) {
		this.unit = unit;
	}

	/**
	 * Get the scalebar legend Fon
	 * 
	 * @return the scalebar legend Font
	 */
	public Paint getPaint() {
		return this.defaultPaint;
	}

	/**
	 * Set the scalebar legend Fon
	 * 
	 * @param font
	 *            the scalebar legend Font
	 */
	public void setPaint(Paint paint) {
		if (paint == null) {
			String msg = Logging.getMessage("nullValue.FontIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.defaultPaint = paint;
		textRenderer = null;
	}

	// Rendering
	@Override
	public void doRender(DrawContext dc) {
		dc.addOrderedRenderable(this.orderedImage);
	}

	@Override
	public void doPick(DrawContext dc, Point pickPoint) {
		// Delegate drawing to the ordered renderable list
		dc.addOrderedRenderable(this.orderedImage);
	}

	// Rendering
	public void draw(DrawContext dc) {

		try {

			GLES20.glDisable(GLES20.GL_DEPTH_TEST);

			double width = this.size.width;
			double height = this.size.height;

			// Load a parallel projection with xy dimensions (viewportWidth,
			// viewportHeight)
			// into the GL projection matrix.
			Rect viewport = dc.getView().getViewport();
			double maxwh = width > height ? width : height;
			Matrix projection = Matrix.fromIdentity().setOrthographic(0d, viewport.width, 0d, viewport.height, -0.6 * maxwh, 0.6 * maxwh);

			// Scale to a width x height space
			// located at the proper position on screen
			Matrix modelview = Matrix.fromIdentity();
			double scale = this.computeScale(viewport);
			Vec4 locationSW = this.computeLocation(viewport, scale);
			modelview.multiplyAndSet(Matrix.fromTranslation(locationSW.x, locationSW.y, locationSW.z));
			modelview.multiplyAndSet(Matrix.fromScale(scale, scale, 1));

			// Compute scale size in real world
			Position referencePosition = getViewportCenterPosition(dc);
			if (referencePosition != null) {
				Vec4 groundTarget = dc.getGlobe().computePointFromPosition(referencePosition);
				Double distance = dc.getView().getEyePoint().distanceTo3(groundTarget);
				this.pixelSize = computePixelSizeAtDistance(distance, dc.getView().getFieldOfView(), dc.getView().getViewport());
				Double scaleSize = this.pixelSize * width * scale; // meter
				String unitLabel = "m";
				if (this.unit.equals(UNIT_METRIC)) {
					if (scaleSize > 10000) {
						scaleSize /= 1000;
						unitLabel = "Km";
					}
				} else if (this.unit.equals(UNIT_IMPERIAL)) {
					scaleSize *= 3.280839895; // feet
					unitLabel = "ft";
					if (scaleSize > 5280) {
						scaleSize /= 5280;
						unitLabel = "mile(s)";
					}
				} else if (this.unit.equals(UNIT_NAUTICAL)) {
					scaleSize *= 3.280839895; // feet
					unitLabel = "ft";
					if (scaleSize > 6076) {
						scaleSize /= 6076;
						unitLabel = "Nautical mile(s)";
					}
				}
				// Rounded division size
				int pot = (int) Math.floor(Math.log10(scaleSize));
				if (!Double.isNaN(pot)) {
					int digit = Integer.parseInt(String.format("%.0f", scaleSize).substring(0, 1));
					double divSize = digit * Math.pow(10, pot);
					if (digit >= 5) divSize = 5 * Math.pow(10, pot);
					else if (digit >= 2) divSize = 2 * Math.pow(10, pot);
					double divWidth = width * divSize / scaleSize;

					// Draw scale
					if (!dc.isPickingMode()) {
						GLES20.glEnable(GLES20.GL_BLEND);
						GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
						GpuProgram colorProgram = this.getGpuProgram(dc.getGpuResourceCache(), programColorKey, VERTEX_SHADER_PATH_COLOR, FRAGMENT_SHADER_PATH_COLOR);
						if (colorProgram != null) {
							// Set color using current layer opacity
							float[] backColor = this.getBackgroundColor(this.color);
							colorProgram.bind();
							colorProgram.loadUniform4f("uColor", backColor[0], backColor[1], backColor[2], backColor[3] * this.getOpacity());
							modelview.multiplyAndSet(Matrix.fromTranslation((width - divWidth) / 2, 0d, 0d));
							Matrix mvp = Matrix.fromIdentity().multiplyAndSet(projection, modelview);
							colorProgram.loadUniformMatrix("mvpMatrix", mvp);
							int pointLocation = colorProgram.getAttribLocation("vertexPoint");
							GLES20.glEnableVertexAttribArray(pointLocation);
							this.drawScale(dc, divWidth, height, pointLocation);

							colorProgram.loadUniform4f("uColor", color[0], color[1], color[2], this.getOpacity());
							modelview.multiplyAndSet(Matrix.fromTranslation(-1d / scale, 1d / scale, 0d));
							this.drawScale(dc, divWidth, height, pointLocation);

							// Draw label
							String label = String.format("%.0f ", divSize) + unitLabel;
							drawLabel(dc, label, locationSW.add3(new Vec4(divWidth * scale / 2 + (width - divWidth) / 2, height * scale, 0)));
						}
					} else {
						// Picking
						// this.pickSupport.clearPickList();
						// this.pickSupport.beginPicking(dc);
						// // Draw unique color across the map
						// Color color = dc.getUniquePickColor();
						// int colorCode = color.getRGB();
						// // Add our object(s) to the pickable list
						// this.pickSupport.addPickableObject(colorCode, this,
						// referencePosition, false);
						// gl.glColor3ub((byte) color.getRed(), (byte)
						// color.getGreen(), (byte) color.getBlue());
						// gl.glTranslated((width - divWidth) / 2, 0d, 0d);
						// this.drawRectangle(dc, divWidth, height);
						// // Done picking
						// this.pickSupport.endPicking(dc);
						// this.pickSupport.resolvePick(dc, dc.getPickPoint(),
						// this);
					}
				}
			}
		} finally {
			GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
		}
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

	private double computePixelSizeAtDistance(double distance, Angle fieldOfView, Rect viewport) {
		if (fieldOfView == null) {
			String message = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}
		if (viewport == null) {
			String message = Logging.getMessage("nullValue.RectangleIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		// If the viewport width is zero, than replace it with 1, which
		// effectively ignores the viewport width.
		double viewportWidth = viewport.width;
		double pixelSizeScale = 2 * fieldOfView.tanHalfAngle() / (viewportWidth <= 0 ? 1d : viewportWidth);

		return Math.abs(distance) * pixelSizeScale;
	}

	private Position getViewportCenterPosition(DrawContext dc) {
		Point centerPoint = new Point(dc.getViewportWidth() / 2, dc.getViewportHeight() / 2);
		Position result = new Position();

		boolean valid = dc.getView().computePositionFromScreenPoint(dc.getGlobe(), centerPoint, result);
		if (valid) return result;
		else return null;
	}

	// Draw scale graphic
	private void drawScale(DrawContext dc, double width, double height, int pointLocation) {
		float[] verts = new float[] { 0, (float) height, 0, 0, 0, 0, (float) width, 0, 0, (float) width, (float) height, 0 };
		FloatBuffer vertBuf = createBuffer(verts);
		GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, vertBuf);
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, verts.length / 3);
		verts = new float[] { (float) (width / 2), 0, 0, (float) (width / 2), (float) (height / 2), 0 };
		vertBuf = createBuffer(verts);
		GLES20.glVertexAttribPointer(pointLocation, 3, GLES20.GL_FLOAT, false, 0, vertBuf);
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, verts.length / 3);
	}

	protected FloatBuffer createBuffer(float[] array) {
		FloatBuffer retval = ByteBuffer.allocateDirect(array.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
		retval.put(array);
		retval.rewind();
		return retval;
	}

	// Draw the scale label
	private void drawLabel(DrawContext dc, String text, Vec4 screenPoint) {
		if (textRenderer == null) {
			textRenderer = new TextRenderer(dc, this.defaultPaint);
		}

		Rect nameBound = textRenderer.getBounds(text);
		int x = (int) (screenPoint.x - nameBound.width / 2d);
		int y = (int) screenPoint.y;

		textRenderer.setColor(this.getBackgroundColor(this.color));
		textRenderer.draw(text, x + 1, y - 1);
		textRenderer.setColor(this.color);
		textRenderer.draw(text, x, y);
	}

	private final float[] compArray = new float[4];

	// Compute background color for best contrast
	private float[] getBackgroundColor(float[] color) {
		Color.RGBToHSV((int) (color[0] * 255), (int) (color[1] * 255), (int) (color[2] * 255), compArray);
		if (compArray[2] > 0.5) return new float[] { 0, 0, 0, 0.7f };
		else return new float[] { 1, 1, 1, 0.7f };
	}

	private double computeScale(Rect viewport) {
		if (this.resizeBehavior.equals(AVKey.RESIZE_SHRINK_ONLY)) {
			return Math.min(1d, (this.toViewportScale) * viewport.width / this.size.width);
		} else if (this.resizeBehavior.equals(AVKey.RESIZE_STRETCH)) {
			return (this.toViewportScale) * viewport.width / this.size.width;
		} else if (this.resizeBehavior.equals(AVKey.RESIZE_KEEP_FIXED_SIZE)) {
			return 1d;
		} else {
			return 1d;
		}
	}

	private Vec4 computeLocation(Rect viewport, double scale) {
		double scaledWidth = scale * this.size.width;
		double scaledHeight = scale * this.size.height;

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

	@Override
	public String toString() {
		return Logging.getMessage("layers.Earth.ScalebarLayer.Name");
	}
}
