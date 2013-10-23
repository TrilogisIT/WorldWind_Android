/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Model;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.terrain.Terrain;
import gov.nasa.worldwind.terrain.VisibleTerrain;
import gov.nasa.worldwind.util.BufferUtil;
import gov.nasa.worldwind.util.Logging;
import java.nio.ByteBuffer;
import java.util.PriorityQueue;
import android.graphics.Point;
import android.opengl.GLES20;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: DrawContext.java 834 2012-10-08 22:25:55Z dcollins $
 */
public class DrawContext extends WWObjectImpl {
	protected static class OrderedRenderableEntry implements Comparable<OrderedRenderableEntry> {
		protected OrderedRenderable or;
		protected double distanceFromEye;
		protected long time;

		public OrderedRenderableEntry(OrderedRenderable orderedRenderable, double distanceFromEye, long insertionTime) {
			this.or = orderedRenderable;
			this.distanceFromEye = distanceFromEye;
			this.time = insertionTime;
		}

		public int compareTo(OrderedRenderableEntry that) {
			double dA = this.distanceFromEye;
			double dB = that.distanceFromEye;

			return dA > dB ? -1 : dA == dB ? (this.time < that.time ? -1 : this.time == that.time ? 0 : 1) : 1;
		}
	}

	protected static final float DEFAULT_DEPTH_OFFSET_FACTOR = 1f;
	protected static final float DEFAULT_DEPTH_OFFSET_UNITS = 1f;
	protected static final double DEFAULT_VERTICAL_EXAGGERATION = 1;

	protected int viewportWidth;
	protected int viewportHeight;
	protected Position viewportCenterPosition = null;
	protected int clearColor;
	protected Model model;
	protected View view;
	protected double verticalExaggeration = DEFAULT_VERTICAL_EXAGGERATION;
	protected GpuResourceCache gpuResourceCache;
	protected long frameTimestamp;
	protected Sector visibleSector;
	protected Terrain visibleTerrain = new VisibleTerrain(this);
	protected SectorGeometryList surfaceGeometry;
	protected SurfaceTileRenderer surfaceTileRenderer = new SurfaceTileRenderer();
	protected Layer currentLayer;
	protected GpuProgram currentProgram;
	protected boolean orderedRenderingMode;
	protected PriorityQueue<OrderedRenderableEntry> orderedRenderables = new PriorityQueue<OrderedRenderableEntry>(100);
	protected boolean pickingMode;
	protected boolean deepPickingMode;
	protected int uniquePickNumber;
	protected ByteBuffer pickColor = BufferUtil.newByteBuffer(4);
	protected Point pickPoint;
	protected PickedObjectList objectsAtPickPoint = new PickedObjectList();

	/**
	 * Initializes this <code>DrawContext</code>. This method should be called at the beginning of each frame to prepare
	 * the <code>DrawContext</code> for the coming render pass.
	 */
	public void initialize(int viewportWidth, int viewportHeight) {
		if (viewportWidth < 0) {
			String msg = Logging.getMessage("generic.WidthIsInvalid", viewportWidth);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (viewportHeight < 0) {
			String msg = Logging.getMessage("generic.HeightIsInvalid", viewportHeight);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.model = null;
		this.view = null;
		this.verticalExaggeration = DEFAULT_VERTICAL_EXAGGERATION;
		this.gpuResourceCache = null;
		this.frameTimestamp = 0;
		this.visibleSector = null;
		this.surfaceGeometry = null;
		this.currentLayer = null;
		this.currentProgram = null;
		this.orderedRenderingMode = false;
		this.orderedRenderables.clear();
		this.pickingMode = false;
		this.deepPickingMode = false;
		this.uniquePickNumber = 0;
		this.pickPoint = null;
		this.objectsAtPickPoint.clear();
	}

	public int getViewportWidth() {
		return this.viewportWidth;
	}

	public int getViewportHeight() {
		return this.viewportHeight;
	}

	/**
	 * Returns the background color as a packed 32-bit ARGB color int. See the section on <i>Color Int</i> in the {@link Color} class documentation for more
	 * information on the color int format.
	 * <p/>
	 * The returned int can be converted to a floating-point RGBA color using the Color constructor {@link Color#Color(int)}.
	 * 
	 * @return a packed 32-bit ARGB color representing the background color.
	 */
	public int getClearColor() {
		return this.clearColor;
	}

	/**
	 * Retrieves the current <code>Model</code>, which may be null.
	 * 
	 * @return the current <code>Model</code>, which may be null
	 */
	public Model getModel() {
		return this.model;
	}

	/**
	 * Assign a new <code>Model</code>. Some layers cannot function properly with a null <code>Model</code>. It is
	 * recommended that the <code>Model</code> is never set to null during a normal render pass.
	 * 
	 * @param model
	 *            the new <code>Model</code>
	 */
	public void setModel(Model model) {
		this.model = model;
	}

	/**
	 * Retrieves the current <code>View</code>, which may be null.
	 * 
	 * @return the current <code>View</code>, which may be null
	 */
	public View getView() {
		return this.view;
	}

	/**
	 * Assigns a new <code>View</code>. Some layers cannot function properly with a null <code>View</code>. It is
	 * recommended that the <code>View</code> is never set to null during a normal render pass.
	 * 
	 * @param view
	 *            the enw <code>View</code>
	 */
	public void setView(View view) {
		this.view = view;
	}

	/**
	 * Retrieves the current <code>Globe</code>, which may be null.
	 * 
	 * @return the current <code>Globe</code>, which may be null
	 */
	public Globe getGlobe() {
		return this.model != null ? this.model.getGlobe() : null;
	}

	/**
	 * Retrieves a list containing all the current layers. No guarantee is made about the order of the layers.
	 * 
	 * @return a <code>LayerList</code> containing all the current layers
	 */
	public LayerList getLayers() {
		return this.model != null ? this.model.getLayers() : null;
	}

	/**
	 * Retrieves the current vertical exaggeration. Vertical exaggeration affects the appearance of areas with varied
	 * elevation. A vertical exaggeration of zero creates a surface which exactly fits the shape of the underlying <code>Globe</code>. A vertical exaggeration
	 * of 3 will create mountains and valleys which are three times as
	 * high/deep as they really are.
	 * 
	 * @return the current vertical exaggeration
	 */
	public double getVerticalExaggeration() {
		return this.verticalExaggeration;
	}

	/**
	 * Sets the vertical exaggeration. Vertical exaggeration affects the appearance of areas with varied elevation. A
	 * vertical exaggeration of zero creates a surface which exactly fits the shape of the underlying <code>Globe</code>. A vertical exaggeration of 3 will
	 * create mountains and valleys which are three times as
	 * high/deep as they really are.
	 * 
	 * @param verticalExaggeration
	 *            the new vertical exaggeration.
	 */
	public void setVerticalExaggeration(double verticalExaggeration) {
		this.verticalExaggeration = verticalExaggeration;
	}

	/**
	 * Returns the GPU resource cache used by this draw context.
	 * 
	 * @return the GPU resource cache used by this draw context.
	 */
	public GpuResourceCache getGpuResourceCache() {
		return this.gpuResourceCache;
	}

	/**
	 * Specifies the GPU resource cache for this draw context.
	 * 
	 * @param gpuResourceCache
	 *            the GPU resource cache for this draw context.
	 */
	public void setGpuResourceCache(GpuResourceCache gpuResourceCache) {
		if (gpuResourceCache == null) {
			String msg = Logging.getMessage("nullValue.CacheIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.gpuResourceCache = gpuResourceCache;
	}

	/**
	 * Returns the time stamp corresponding to the beginning of a pre-render, pick, render sequence. The stamp remains
	 * constant across these three operations so that called objects may avoid recomputing the same values during each
	 * of the calls in the sequence.
	 * 
	 * @return the frame time stamp. See {@link System#currentTimeMillis()} for its numerical meaning.
	 */
	public long getFrameTimeStamp() {
		return this.frameTimestamp;
	}

	/**
	 * Specifies the time stamp corresponding to the beginning of a pre-render, pick, render sequence. The stamp must
	 * remain constant across these three operations so that called objects may avoid recomputing the same values during
	 * each of the calls in the sequence.
	 * 
	 * @param timeStamp
	 *            the frame time stamp. See {@link System#currentTimeMillis()} for its numerical meaning.
	 */
	public void setFrameTimeStamp(long timeStamp) {
		this.frameTimestamp = timeStamp;
	}

	/**
	 * Retrieves a <code>Sector</code> which is at least as large as the current visible sector. The value returned is
	 * the value passed to <code>SetVisibleSector</code>. This method may return null.
	 * 
	 * @return a <code>Sector</code> at least the size of the current visible sector, null if unavailable
	 */
	public Sector getVisibleSector() {
		return this.visibleSector;
	}

	/**
	 * Sets the visible <code>Sector</code>. The new visible sector must completely encompass the Sector which is
	 * visible on the display.
	 * 
	 * @param sector
	 *            the new visible <code>Sector</code>
	 */
	public void setVisibleSector(Sector sector) {
		this.visibleSector = sector;
	}

	/**
	 * Indicates an interface to the terrain that is visible this frame. The returned terrain object is the preferred
	 * interface for performing terrain queries and analysis such as line/terrain intersection and surface point
	 * computation against the visible terrain.
	 * <p/>
	 * The returned interface does not provide any method for drawing the currently visible terrain geometry. The methods {@link #getSurfaceGeometry()} and
	 * {@link #getSurfaceTileRenderer()} return interfaces suited for terrain rendering.
	 * 
	 * @return an interface to perform queries against the currently visible terrain.
	 */
	public Terrain getVisibleTerrain() {
		return this.visibleTerrain;
	}

	/**
	 * Indicates the surface geometry that is visible this frame.
	 * 
	 * @return the visible surface geometry.
	 */
	public SectorGeometryList getSurfaceGeometry() {
		return this.surfaceGeometry;
	}

	/**
	 * Specifies the surface geometry that is visible this frame.
	 * 
	 * @param surfaceGeometry
	 *            the visible surface geometry.
	 */
	public void setSurfaceGeometry(SectorGeometryList surfaceGeometry) {
		this.surfaceGeometry = surfaceGeometry;
	}

	public SurfaceTileRenderer getSurfaceTileRenderer() {
		return this.surfaceTileRenderer;
	}

	/**
	 * Returns the current layer. The field is informative only and enables layer contents to determine their containing
	 * layer.
	 * 
	 * @return the current layer, or null if no layer is current.
	 */
	public Layer getCurrentLayer() {
		return this.currentLayer;
	}

	/**
	 * Sets the current layer field to the specified layer or null. The field is informative only and enables layer
	 * contents to determine their containing layer.
	 * 
	 * @param layer
	 *            the current layer or null.
	 */
	public void setCurrentLayer(Layer layer) {
		this.currentLayer = layer;
	}

	public GpuProgram getCurrentProgram() {
		return this.currentProgram;
	}

	public void setCurrentProgram(GpuProgram program) {
		this.currentProgram = program;
	}

	public boolean isOrderedRenderingMode() {
		return this.orderedRenderingMode;
	}

	public void setOrderedRenderingMode(boolean tf) {
		this.orderedRenderingMode = tf;
	}

	public OrderedRenderable peekOrderedRenderables() {
		OrderedRenderableEntry ore = this.orderedRenderables.peek();

		return ore != null ? ore.or : null;
	}

	public OrderedRenderable pollOrderedRenderables() {
		OrderedRenderableEntry ore = this.orderedRenderables.poll();

		return ore != null ? ore.or : null;
	}

	public void addOrderedRenderable(OrderedRenderable orderedRenderable) {
		if (orderedRenderable == null) {
			String msg = Logging.getMessage("nullValue.OrderedRenderableIsNull");
			Logging.warning(msg);
			return; // benign event
		}

		this.orderedRenderables.add(new OrderedRenderableEntry(orderedRenderable, orderedRenderable.getDistanceFromEye(), System.nanoTime()));
	}

	public void addOrderedRenderableToBack(OrderedRenderable orderedRenderable) {
		if (orderedRenderable == null) {
			String msg = Logging.getMessage("nullValue.OrderedRenderableIsNull");
			Logging.warning(msg);
			return; // benign event
		}

		// The ordered renderable should be treated as behind other ordered renderables, so we give it an eye distance
		// of Double.MAX_VALUE and ignore the actual eye distance. If multiple ordered renderables are added in this
		// way, they are drawn according to the order in which they are added.
		this.orderedRenderables.add(new OrderedRenderableEntry(orderedRenderable, Double.MAX_VALUE, System.nanoTime()));
	}

	/**
	 * Indicates whether the drawing is occurring in picking picking mode. In picking mode, each unique object is drawn
	 * in a unique RGB color by calling {@link #getUniquePickColor()} prior to rendering. Any OpenGL state that could
	 * cause an object to draw a color other than the unique RGB pick color must be disabled. This includes
	 * antialiasing, blending, and dithering.
	 * 
	 * @return true if drawing should occur in picking mode, otherwise false.
	 */
	public boolean isPickingMode() {
		return this.pickingMode;
	}

	/**
	 * Specifies whether drawing should occur in picking mode. See {@link #isPickingMode()} for more information.
	 * 
	 * @param tf
	 *            true to specify that drawing should occur in picking mode, otherwise false.
	 */
	public void setPickingMode(boolean tf) {
		this.pickingMode = tf;
	}

	/**
	 * Indicates whether all items under the pick point are picked.
	 * 
	 * @return true if all items under the pick point are picked, otherwise false .
	 */
	public boolean isDeepPickingEnabled() {
		return this.deepPickingMode;
	}

	/**
	 * Specifies whether all items under the pick point are picked.
	 * 
	 * @param tf
	 *            true to pick all objects under the pick point.
	 */
	public void setDeepPickingEnabled(boolean tf) {
		this.deepPickingMode = tf;
	}

	/**
	 * Returns a unique packed 32-bit RGB color int to serve as an identifier during picking. The bits normally reserved
	 * for alpha in the returned value are filled with 0. See the section on <i>Color Int</i> in the {@link Color} class
	 * documentation for more information on the color int format.
	 * <p/>
	 * The returned int can be converted to a floating-point RGB color using the Color constructor {@link Color#Color(int, boolean)} and passing
	 * <code>false</code> in hasAlpha.
	 * 
	 * @return a packed 32-bit RGB color representing a unique pick color.
	 */
	public int getUniquePickColor() {
		this.uniquePickNumber++; // Increment to the next pick number. This causes the pick numbers to start at 1.

		if (this.uniquePickNumber == this.clearColor) // Skip the clear color.
		this.uniquePickNumber++;

		if (this.uniquePickNumber >= 0x00FFFFFF) // We have run out of available pick numbers.
		{
			this.uniquePickNumber = 1; // Do not use black or white as a pick color. Pick numbers start at 1.
			if (this.uniquePickNumber == this.clearColor) // Skip the clear color.
			this.uniquePickNumber++;
		}

		return this.uniquePickNumber;
	}

	/**
	 * Returns the packed 32-bit RGB color int from the framebuffer at the specified screen point. The bits normally
	 * reserved for alpha in the returned value are filled with 0. See the section on <i>Color Int</i> in the {@link Color} class documentation for more
	 * information on the color int format.
	 * <p/>
	 * This returns 0 if the point specifies a framebuffer pixel containing the clear color.
	 * <p/>
	 * The returned int can be converted to a floating-point RGB color using the Color constructor {@link Color#Color(int, boolean)} and passing
	 * <code>false</code> in hasAlpha.
	 * 
	 * @param point
	 *            the screen point who's RGB color is returned.
	 * @return a packed 32-bit RGB color representing the color at the screen point, or 0 if the point indicates a pixel
	 *         filled with the clear color.
	 * @throws IllegalArgumentException
	 *             if the point is <code>null</code>.
	 */
	public int getPickColor(Point point) {
		if (point == null) {
			String msg = Logging.getMessage("nullValue.PointIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Read the RGBA color at the specified point as a 4-component tuple of unsigned bytes. OpenGL ES does not
		// support reading only the RGB values, so we read the RGBA value and ignore the alpha component. We convert the
		// y coordinate from system screen coordinates to OpenGL screen coordinates.
		int yInGLCoords = this.viewportHeight - point.y;
		GLES20.glReadPixels(point.x, yInGLCoords, 1, 1, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, this.pickColor);

		// OpenGL places the pixel's RGBA components in the first 4 bytes of the buffer, in that order. We ignore the
		// alpha component and compose a packed 24-bit RGB color int equivalent to those returned by getUniquePickColor.
		int colorInt = Color.makeColorInt(this.pickColor.get(0), this.pickColor.get(1), this.pickColor.get(2));
		return colorInt != this.clearColor ? colorInt : 0;
	}

	/**
	 * Returns the current pick point.
	 * 
	 * @return the current pick point, or null if no pick point is available.
	 */
	public Point getPickPoint() {
		return pickPoint;
	}

	/**
	 * Specifies the pick point.
	 * 
	 * @param point
	 *            the pick point, or null to indicate there is no pick point.
	 */
	public void setPickPoint(Point point) {
		this.pickPoint = point;
	}

	/**
	 * Indicates the geographic coordinates of the point on the terrain at the current viewport's center.
	 * 
	 * @return the geographic coordinates of the current viewport's center. Returns null if the globe's surface is not
	 *         under the viewport's center point.
	 */
	public Position getViewportCenterPosition() {
		return viewportCenterPosition;
	}

	public void setViewportCenterPosition(Position viewportCenterPosition) {
		this.viewportCenterPosition = viewportCenterPosition;
	}

	/**
	 * Returns the World Wind objects at the current pick point. The list of objects is determined while drawing in
	 * picking mode, and is cleared each time this draw context is initialized.
	 * 
	 * @return the list of currently picked objects.
	 */
	public PickedObjectList getObjectsAtPickPoint() {
		return this.objectsAtPickPoint;
	}

	/**
	 * Adds a single picked object to the current picked-object list.
	 * 
	 * @param pickedObject
	 *            the object to add.
	 * @throws IllegalArgumentException
	 *             if the pickedObject is null.
	 */
	public void addPickedObject(PickedObject pickedObject) {
		if (pickedObject == null) {
			String msg = Logging.getMessage("nullValue.PickedObject");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.objectsAtPickPoint.add(pickedObject);
	}

	/**
	 * Indicates whether a specified extent is smaller than a specified number of pixels for the current view.
	 * 
	 * @param extent
	 *            the extent to test. May be null, in which case this method returns false.
	 * @param numPixels
	 *            the number of pixels at and below which the extent is considered too small.
	 * @return true if the projected extent is smaller than the specified number of pixels, otherwise false.
	 * @throws IllegalArgumentException
	 *             if the extend is <code>null</code>.
	 */
	public boolean isSmall(Extent extent, int numPixels) {
		if (extent == null) {
			String msg = Logging.getMessage("nullValue.ExtentIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// TODO: John Burkey proposed the following in 2011, in reference to Collada model performance:
		// Couldnt we make this minimum dimension so box could return small when one dim is narrow? I see really skinny
		// telephone poles that dont need to be rendered at distance but are tall.
		double distance = this.getView().getEyePoint().distanceTo3(extent.getCenter());
		double extentInMeters = 2 * extent.getRadius();
		double numPixelsIn = numPixels * this.getView().computePixelSizeAtDistance(distance);

		return extentInMeters <= numPixelsIn;
	}

	/**
	 * Performs a multi-pass rendering technique to ensure that outlines around filled shapes are drawn correctly when
	 * blending or ant-aliasing is performed, and that filled portions of the shape resolve depth-buffer fighting with
	 * shapes previously drawn in favor of the current shape.
	 * 
	 * @param renderer
	 *            an object implementing the {@link gov.nasa.worldwind.render.OutlinedShape} interface for the
	 *            shape.
	 * @param shape
	 *            the shape to render.
	 * @throws IllegalArgumentException
	 *             if either the renderer or the shape are <code>null</code>.
	 * @see gov.nasa.worldwind.render.OutlinedShape
	 */
	public void drawOutlinedShape(OutlinedShape renderer, Object shape) {
		if (renderer == null) {
			String msg = Logging.getMessage("nullValue.RendererIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (shape == null) {
			String msg = Logging.getMessage("nullValue.ShapeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Draw the outlined shape using a multiple pass algorithm. The motivation for this algorithm is twofold:
		//
		// * The outline appears both in front of and behind the shape. If the outline is drawn using GL line smoothing
		// or GL blending, then either the line must be broken into two parts, or rendered in two passes.
		//
		// * If depth offset is enabled, we want draw the shape on top of other intersecting shapes with similar depth
		// values to eliminate z-fighting between shapes. However we do not wish to offset both the depth and color
		// values, which would cause a cascading increase in depth offset when many shapes are drawn.
		//
		// These issues are resolved by making several passes for the interior and outline, as follows:

		if (this.isDeepPickingEnabled()) {
			if (renderer.isDrawInterior(this, shape)) renderer.drawInterior(this, shape);

			if (renderer.isDrawOutline(this, shape)) // the line might extend outside the interior's projection
			renderer.drawOutline(this, shape);

			return;
		}

		try {
			// If the outline and interior are enabled, then draw the outline but do not affect the depth buffer. The
			// fill pixels contribute the depth values. When the interior is drawn, it draws on top of these colors, and
			// the outline is be visible behind the potentially transparent interior.
			if (renderer.isDrawOutline(this, shape) && renderer.isDrawInterior(this, shape)) {
				GLES20.glColorMask(true, true, true, true);
				GLES20.glDepthMask(false);

				renderer.drawOutline(this, shape);
			}

			// If the interior is enabled, then make two passes as follows. The first pass draws the interior depth
			// values with a depth offset (likely away from the eye). This enables the shape to contribute to the
			// depth buffer and occlude other geometries as it normally would. The second pass draws the interior color
			// values without a depth offset, and does not affect the depth buffer. This giving the shape outline depth
			// priority over the fill, and gives the fill depth priority over other shapes drawn with depth offset
			// enabled. By drawing the colors without depth offset, we avoid the problem of having to use ever
			// increasing depth offsets.
			if (renderer.isDrawInterior(this, shape)) {
				if (renderer.isEnableDepthOffset(this, shape)) {
					// Draw depth.
					Double depthOffsetFactor = renderer.getDepthOffsetFactor(this, shape);
					Double depthOffsetUnits = renderer.getDepthOffsetUnits(this, shape);
					GLES20.glColorMask(false, false, false, false);
					GLES20.glDepthMask(true);
					GLES20.glEnable(GLES20.GL_POLYGON_OFFSET_FILL);
					GLES20.glPolygonOffset(depthOffsetFactor != null ? depthOffsetFactor.floatValue() : DEFAULT_DEPTH_OFFSET_FACTOR,
							depthOffsetUnits != null ? depthOffsetUnits.floatValue() : DEFAULT_DEPTH_OFFSET_UNITS);

					renderer.drawInterior(this, shape);

					// Draw color.
					GLES20.glColorMask(true, true, true, true);
					GLES20.glDepthMask(false);
					GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);

					renderer.drawInterior(this, shape);
				} else {
					GLES20.glColorMask(true, true, true, true);
					GLES20.glDepthMask(true);

					renderer.drawInterior(this, shape);
				}
			}

			// If the outline is enabled, then draw the outline color and depth values. This blends outline colors with
			// the interior colors.
			if (renderer.isDrawOutline(this, shape)) {
				GLES20.glColorMask(true, true, true, true);
				GLES20.glDepthMask(true);

				renderer.drawOutline(this, shape);
			}
		} finally {
			// Restore the default GL state values we modified above.
			GLES20.glDisable(GLES20.GL_POLYGON_OFFSET_FILL);
			GLES20.glColorMask(true, true, true, true);
			GLES20.glDepthMask(true);
			GLES20.glPolygonOffset(0f, 0f);
		}
	}
}
