/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.Movable;
import gov.nasa.worldwind.WWObjectImpl;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.cache.ShapeDataCache;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.terrain.Terrain;
import gov.nasa.worldwind.util.Logging;
import java.util.List;
import android.graphics.Point;
import android.opengl.GLES20;

/**
 * Provides a base class form several geometric {@link gov.nasa.worldwind.render.Renderable}s. Implements common
 * attribute handling and rendering flow for outlined shapes. Provides common defaults and common export code.
 * <p/>
 * In order to support simultaneous use of this shape with multiple globes (windows), this shape maintains a cache of data computed relative to each globe.
 * During rendering, the data for the currently active globe, as indicated in the draw context, is made current. Subsequently called methods rely on the
 * existence of this current data cache entry.
 * 
 * @author tag
 * @version $Id: AbstractShape.java 844 2012-10-11 00:35:07Z tgaskins $
 */
public abstract class AbstractShape extends WWObjectImpl implements OrderedRenderable, Highlightable, Movable {
	/** The default interior color. */
	protected static final Color DEFAULT_INTERIOR_COLOR = Color.lightGray();
	/** The default outline color. */
	protected static final Color DEFAULT_OUTLINE_COLOR = Color.darkGray();
	/** The default highlight color. */
	protected static final Color DEFAULT_HIGHLIGHT_COLOR = Color.white();
	/** The default outline pick width. */
	protected static final int DEFAULT_OUTLINE_PICK_WIDTH = 10;
	/** The default geometry regeneration interval. */
	protected static final int DEFAULT_GEOMETRY_GENERATION_INTERVAL = 3000;
	/** The default vertex shader path. This specifies the location of a file within the World Wind archive. */
	protected static final String DEFAULT_VERTEX_SHADER_PATH = "shaders/AbstractShape.vert";
	/** The default fragment shader path. This specifies the location of a file within the World Wind archive. */
	protected static final String DEFAULT_FRAGMENT_SHADER_PATH = "shaders/AbstractShape.frag";

	/** The attributes used if attributes are not specified. */
	protected static ShapeAttributes defaultAttributes;

	static {
		// Create and populate the default attributes.
		defaultAttributes = new BasicShapeAttributes();
		defaultAttributes.setInteriorColor(DEFAULT_INTERIOR_COLOR);
		defaultAttributes.setOutlineColor(DEFAULT_OUTLINE_COLOR);
	}

	/**
	 * Compute the intersections of a specified line with this shape. If the shape's altitude mode is other than {@link AVKey#ABSOLUTE}, the shape's geometry is
	 * created relative to the specified terrain rather than the terrain used
	 * during rendering, which may be at lower level of detail than required for accurate intersection determination.
	 * 
	 * @param line
	 *            the line to intersect.
	 * @param terrain
	 *            the {@link Terrain} to use when computing the shape's geometry.
	 * @return a list of intersections identifying where the line intersects the shape, or null if the line does not
	 *         intersect the shape.
	 * @throws InterruptedException
	 *             if the operation is interrupted.
	 * @see Terrain
	 */
	abstract public List<Intersection> intersect(Line line, Terrain terrain) throws InterruptedException;

	/**
	 * Called during construction to establish any subclass-specific state such as different default values than those
	 * set by this class.
	 */
	abstract protected void initialize();

	/**
	 * Produces the geometry and other state necessary to represent this shape as an ordered renderable. Places this
	 * shape on the draw context's ordered renderable list for subsequent rendering. This method is called during {@link #pick(DrawContext, Point)} and
	 * {@link #render(DrawContext)} when it's been determined that the shape is likely to
	 * be visible.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return true if the ordered renderable state was successfully computed, otherwise false, in which case the
	 *         current pick or render pass is terminated for this shape. Subclasses should return false if it is not
	 *         possible to create the ordered renderable state.
	 * @see #pick(DrawContext, Point)
	 * @see #render(DrawContext)
	 */
	abstract protected boolean doMakeOrderedRenderable(DrawContext dc);

	/**
	 * Determines whether this shape's ordered renderable state is valid and can be rendered. Called by {@link #makeOrderedRenderable(DrawContext)}just prior to
	 * adding the shape to the ordered renderable list.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return true if this shape is ready to be rendered as an ordered renderable.
	 */
	abstract protected boolean isOrderedRenderableValid(DrawContext dc);

	/**
	 * Draws this shape's outline. Called immediately after calling {@link #prepareToDrawOutline(DrawContext, ShapeAttributes, ShapeAttributes)}, which
	 * establishes OpenGL state for lighting, blending, pick color and line
	 * attributes. Subclasses should execute the drawing commands specific to the type of shape.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	abstract protected void doDrawOutline(DrawContext dc);

	/**
	 * Draws this shape's interior. Called immediately after calling {@link #prepareToDrawInterior(DrawContext, ShapeAttributes, ShapeAttributes)}, which
	 * establishes OpenGL state for lighting, blending, pick color and
	 * interior attributes. Subclasses should execute the drawing commands specific to the type of shape.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	abstract protected void doDrawInterior(DrawContext dc);

	/**
	 * Fill this shape's vertex buffer objects. If the vertex buffer object resource IDs don't yet exist, create them.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	abstract protected void fillVBO(DrawContext dc);

	/**
	 * Creates and returns a new cache entry specific to the subclass.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return a data cache entry for the state in the specified draw context.
	 */
	protected abstract AbstractShapeData createCacheEntry(DrawContext dc);

	/**
	 * Returns the shape's geographic extent.
	 * 
	 * @return the shape's geographic extent.
	 */
	protected abstract Sector getSector();

	/** This shape's normal, non-highlighted attributes. */
	protected ShapeAttributes normalAttrs;
	/** This shape's highlighted attributes. */
	protected ShapeAttributes highlightAttrs;
	/**
	 * The attributes active for a particular pick and render pass. These are determined according to the highlighting
	 * mode.
	 */
	protected ShapeAttributes activeAttributes = new BasicShapeAttributes(); // re-determined each frame

	protected boolean highlighted;
	protected boolean visible = true;
	protected String altitudeMode;
	protected boolean enableBatchRendering = true;
	protected boolean enableBatchPicking = true;
	protected boolean enableDepthOffset;
	protected int outlinePickWidth = DEFAULT_OUTLINE_PICK_WIDTH;
	protected Sector sector; // the shape's bounding sector
	protected Position referencePosition; // the location/position to use as the shape's reference point
	protected Object delegateOwner; // for app use to identify an owner of this shape other than the current layer
	protected long maxExpiryTime = DEFAULT_GEOMETRY_GENERATION_INTERVAL;
	protected long minExpiryTime = Math.max(DEFAULT_GEOMETRY_GENERATION_INTERVAL - 500, 0);
	protected boolean viewDistanceExpiration = true;

	// Volatile values used only during frame generation.
	protected final Object programKey = new Object();
	protected boolean programCreationFailed;
	protected Matrix currentMatrix = Matrix.fromIdentity();
	protected Color currentColor = new Color();
	protected PickSupport pickSupport = new PickSupport();
	protected Layer pickLayer;

	/** Holds globe-dependent computed data. One entry per globe encountered during {@link #render(DrawContext)}. */
	protected ShapeDataCache shapeDataCache = new ShapeDataCache(60000);

	/**
	 * Identifies the active globe-dependent data for the current invocation of {@link #render(DrawContext)}. The active
	 * data is drawn from this shape's data cache at the beginning of the <code>render</code> method.
	 */
	protected AbstractShapeData currentData;

	/**
	 * Returns the data cache entry for the current rendering.
	 * 
	 * @return the data cache entry for the current rendering.
	 */
	protected AbstractShapeData getCurrentData() {
		return this.currentData;
	}

	/** Holds the globe-dependent data captured in this shape's data cache. */
	protected static class AbstractShapeData extends ShapeDataCache.ShapeDataCacheEntry {
		/** Identifies the frame used to calculate this entry's values. */
		protected long frameNumber = -1;
		/* This entry's transform matrix. */
		protected Matrix transformMatrix = Matrix.fromTranslation(new Vec4());
		/** This entry's reference point. */
		protected Vec4 referencePoint = new Vec4();
		/** A quick-to-compute metric to determine eye distance changes that invalidate this entry's geometry. */
		protected Double referenceDistance;
		/** The GPU-resource cache key to use for this entry's VBOs. */
		protected Object vboCacheKey = new Object();

		/**
		 * Constructs a data cache entry and initializes its globe-dependent state key for the globe in the specified
		 * draw context and capture the current vertical exaggeration. The entry becomes invalid when these values
		 * change or when the entry's expiration timer expires.
		 * 
		 * @param dc
		 *            the current draw context.
		 * @param minExpiryTime
		 *            the minimum number of milliseconds to use this shape before regenerating its geometry.
		 * @param maxExpiryTime
		 *            the maximum number of milliseconds to use this shape before regenerating its geometry.
		 */
		protected AbstractShapeData(DrawContext dc, long minExpiryTime, long maxExpiryTime) {
			super(dc, minExpiryTime, maxExpiryTime);
		}

		public long getFrameNumber() {
			return frameNumber;
		}

		public void setFrameNumber(long frameNumber) {
			this.frameNumber = frameNumber;
		}

		public Matrix getTransformMatrix() {
			return this.transformMatrix;
		}

		public Vec4 getReferencePoint() {
			return referencePoint;
		}

		public Object getVboCacheKey() {
			return vboCacheKey;
		}

		public void setVboCacheKey(Object vboCacheKey) {
			this.vboCacheKey = vboCacheKey;
		}

		public Double getReferenceDistance() {
			return referenceDistance;
		}

		public void setReferenceDistance(Double referenceDistance) {
			this.referenceDistance = referenceDistance;
		}

		public void setTransformMatrixFromReferencePosition() {
			this.transformMatrix.m[3] = this.referencePoint.x;
			this.transformMatrix.m[7] = this.referencePoint.y;
			this.transformMatrix.m[11] = this.referencePoint.z;
		}
	}

	/** Outlined shapes are drawn as {@link gov.nasa.worldwind.render.OutlinedShape}s. */
	protected OutlinedShape outlineShapeRenderer = new OutlinedShape() {
		public boolean isDrawOutline(DrawContext dc, Object shape) {
			return ((AbstractShape) shape).mustDrawOutline();
		}

		public boolean isDrawInterior(DrawContext dc, Object shape) {
			return ((AbstractShape) shape).mustDrawInterior();
		}

		public boolean isEnableDepthOffset(DrawContext dc, Object shape) {
			return ((AbstractShape) shape).isEnableDepthOffset();
		}

		public void drawOutline(DrawContext dc, Object shape) {
			((AbstractShape) shape).drawOutline(dc);
		}

		public void drawInterior(DrawContext dc, Object shape) {
			((AbstractShape) shape).drawInterior(dc);
		}

		public Double getDepthOffsetFactor(DrawContext dc, Object shape) {
			return null;
		}

		public Double getDepthOffsetUnits(DrawContext dc, Object shape) {
			return null;
		}
	};

	/** Invokes {@link #initialize()} during construction and sets the data cache's expiration time to a default value. */
	protected AbstractShape() {
		this.initialize();
	}

	/** Invalidates computed values. Called when this shape's contents or certain attributes change. */
	protected void reset() {
		this.shapeDataCache.removeAllEntries();
		this.sector = null;
	}

	/**
	 * Returns this shape's normal (as opposed to highlight) attributes.
	 * 
	 * @return this shape's normal attributes. May be null.
	 */
	public ShapeAttributes getAttributes() {
		return this.normalAttrs;
	}

	/**
	 * Specifies this shape's normal (as opposed to highlight) attributes.
	 * 
	 * @param normalAttrs
	 *            the normal attributes. May be null, in which case default attributes are used.
	 */
	public void setAttributes(ShapeAttributes normalAttrs) {
		this.normalAttrs = normalAttrs;
	}

	/**
	 * Returns this shape's highlight attributes.
	 * 
	 * @return this shape's highlight attributes. May be null.
	 */
	public ShapeAttributes getHighlightAttributes() {
		return highlightAttrs;
	}

	/**
	 * Specifies this shape's highlight attributes.
	 * 
	 * @param highlightAttrs
	 *            the highlight attributes. May be null, in which case default attributes are used.
	 */
	public void setHighlightAttributes(ShapeAttributes highlightAttrs) {
		this.highlightAttrs = highlightAttrs;
	}

	public boolean isHighlighted() {
		return highlighted;
	}

	public void setHighlighted(boolean highlighted) {
		this.highlighted = highlighted;
	}

	/**
	 * Indicates whether this shape is drawn during rendering.
	 * 
	 * @return true if this shape is drawn, otherwise false.
	 * @see #setVisible(boolean)
	 */
	public boolean isVisible() {
		return visible;
	}

	/**
	 * Specifies whether this shape is drawn during rendering.
	 * 
	 * @param visible
	 *            true to draw this shape, otherwise false. The default value is true.
	 * @see #setAttributes(ShapeAttributes)
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
	}

	/**
	 * Returns this shape's altitude mode. If no altitude mode is set, this returns <code>null</code> to indicate that
	 * the default altitude mode {@link AVKey#ABSOLUTE} is used.
	 * 
	 * @return this shape's altitude mode, or <code>null</code> indicating the default altitude mode.
	 * @see #setAltitudeMode(String)
	 */
	public String getAltitudeMode() {
		return altitudeMode;
	}

	/**
	 * Specifies this shape's altitude mode, one of {@link AVKey#ABSOLUTE}, {@link AVKey#RELATIVE_TO_GROUND}, {@link AVKey#CLAMP_TO_GROUND}, or
	 * <code>null</code> to indicate the default altitude mode {@link AVKey#ABSOLUTE} should
	 * be used.
	 * <p/>
	 * Note: If the altitude mode is unrecognized, {@link AVKey#ABSOLUTE} is used.
	 * <p/>
	 * Note: Subclasses may recognize additional altitude modes or may not recognize the ones described above.
	 * 
	 * @param altitudeMode
	 *            the altitude mode. The default value is {@link AVKey#ABSOLUTE}.
	 */
	public void setAltitudeMode(String altitudeMode) {
		if (this.altitudeMode != null ? this.altitudeMode.equals(altitudeMode) : altitudeMode == null) return;

		this.altitudeMode = altitudeMode;
		this.reset();
	}

	public double getDistanceFromEye() {
		return this.getCurrentData() != null ? this.getCurrentData().getEyeDistance() : 0;
	}

	/**
	 * Indicates whether batch rendering is enabled for the concrete shape type of this shape.
	 * 
	 * @return true if batch rendering is enabled, otherwise false.
	 * @see #setEnableBatchRendering(boolean).
	 */
	public boolean isEnableBatchRendering() {
		return enableBatchRendering;
	}

	/**
	 * Specifies whether adjacent shapes of this shape's concrete type in the ordered renderable list may be rendered
	 * together if they are contained in the same layer. This increases performance. There is seldom a reason to disable
	 * it.
	 * 
	 * @param enableBatchRendering
	 *            true to enable batch rendering, otherwise false.
	 */
	public void setEnableBatchRendering(boolean enableBatchRendering) {
		this.enableBatchRendering = enableBatchRendering;
	}

	/**
	 * Indicates whether batch picking is enabled.
	 * 
	 * @return true if batch rendering is enabled, otherwise false.
	 * @see #setEnableBatchPicking(boolean).
	 */
	public boolean isEnableBatchPicking() {
		return enableBatchPicking;
	}

	/**
	 * Specifies whether adjacent shapes of this shape's concrete type in the ordered renderable list may be pick-tested
	 * together if they are contained in the same layer. This increases performance but allows only the top-most of the
	 * polygons to be reported in a SelectEvent even if several of the polygons are at the pick position.
	 * <p/>
	 * Batch rendering ({@link #setEnableBatchRendering(boolean)}) must be enabled in order for batch picking to occur.
	 * 
	 * @param enableBatchPicking
	 *            true to enable batch rendering, otherwise false.
	 */
	public void setEnableBatchPicking(boolean enableBatchPicking) {
		this.enableBatchPicking = enableBatchPicking;
	}

	/**
	 * Indicates the outline line width to use during picking. A larger width than normal typically makes the outline
	 * easier to pick.
	 * 
	 * @return the outline line width used during picking.
	 */
	public int getOutlinePickWidth() {
		return this.outlinePickWidth;
	}

	/**
	 * Specifies the outline line width to use during picking. A larger width than normal typically makes the outline
	 * easier to pick.
	 * <p/>
	 * Note that the size of the pick aperture also affects the precision necessary to pick.
	 * 
	 * @param outlinePickWidth
	 *            the outline pick width. The default is 10.
	 * @throws IllegalArgumentException
	 *             if the width is less than 1.
	 */
	public void setOutlinePickWidth(int outlinePickWidth) {
		if (outlinePickWidth < 1) {
			String msg = Logging.getMessage("generic.WidthIsInvalid", outlinePickWidth);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.outlinePickWidth = outlinePickWidth;
	}

	/**
	 * Indicates whether the filled sides of this shape should be offset towards the viewer to help eliminate artifacts
	 * when two or more faces of this or other filled shapes are coincident.
	 * 
	 * @return true if depth offset is applied, otherwise false.
	 */
	public boolean isEnableDepthOffset() {
		return this.enableDepthOffset;
	}

	/**
	 * Specifies whether the filled sides of this shape should be offset towards the viewer to help eliminate artifacts
	 * when two or more faces of this or other filled shapes are coincident.
	 * 
	 * @param enableDepthOffset
	 *            true if depth offset is applied, otherwise false.
	 */
	public void setEnableDepthOffset(boolean enableDepthOffset) {
		this.enableDepthOffset = enableDepthOffset;
	}

	/**
	 * Indicates the maximum length of time between geometry regenerations. See {@link #setGeometryRegenerationInterval(int)} for the regeneration-interval's
	 * description.
	 * 
	 * @return the geometry regeneration interval, in milliseconds.
	 * @see #setGeometryRegenerationInterval(int)
	 */
	public long getGeometryRegenerationInterval() {
		return this.maxExpiryTime;
	}

	/**
	 * Specifies the maximum length of time between geometry regenerations. The geometry is regenerated when this
	 * shape's altitude mode is {@link AVKey#CLAMP_TO_GROUND} or {@link AVKey#RELATIVE_TO_GROUND} in order to capture
	 * changes to the terrain. (The terrain changes when its resolution changes or when new elevation data is returned
	 * from a server.) Decreasing this value causes the geometry to more quickly track terrain changes but at the cost
	 * of performance. Increasing this value often does not have much effect because there are limiting factors other
	 * than geometry regeneration.
	 * 
	 * @param geometryRegenerationInterval
	 *            the geometry regeneration interval, in milliseconds. The default is two
	 *            seconds.
	 */
	public void setGeometryRegenerationInterval(int geometryRegenerationInterval) {
		this.maxExpiryTime = Math.max(geometryRegenerationInterval, 0);
		this.minExpiryTime = (long) (0.6 * (double) this.maxExpiryTime);

		for (ShapeDataCache.ShapeDataCacheEntry shapeData : this.shapeDataCache) {
			if (shapeData != null) shapeData.getTimer().setExpiryTime(this.minExpiryTime, this.maxExpiryTime);
		}
	}

	/**
	 * Specifies the position to use as a reference position for computed geometry. This value should typically left to
	 * the default value of the first position in the polygon's outer boundary.
	 * 
	 * @param referencePosition
	 *            the reference position. May be null, in which case the first position of the outer
	 *            boundary is the reference position.
	 */
	public void setReferencePosition(Position referencePosition) {
		this.referencePosition = referencePosition;
		this.reset();
	}

	public Object getDelegateOwner() {
		return delegateOwner;
	}

	public void setDelegateOwner(Object delegateOwner) {
		this.delegateOwner = delegateOwner;
	}

	/**
	 * Returns this shape's extent in model coordinates.
	 * 
	 * @return this shape's extent, or null if an extent has not been computed.
	 */
	public Extent getExtent() {
		return this.getCurrentData().getExtent();
	}

	/**
	 * Returns the Cartesian coordinates of this shape's reference position as computed during the most recent
	 * rendering.
	 * 
	 * @return the Cartesian coordinates corresponding to this shape's reference position, or null if the point has not
	 *         been computed.
	 */
	public Vec4 getReferencePoint() {
		return this.currentData.getReferencePoint();
	}

	public Extent getExtent(Globe globe, double verticalExaggeration) {
		if (globe == null) return null;

		ShapeDataCache.ShapeDataCacheEntry entry = this.shapeDataCache.getEntry(globe);

		return (entry != null && !entry.isExpired(null) && entry.getExtent() != null) ? entry.getExtent() : null;
	}

	// TODO: Restore this method after implementing World Wind Android general texturing support.
	// /**
	// * Creates a {@link WWTexture} for a specified image source.
	// *
	// * @param imageSource the image source for which to create the texture.
	// *
	// * @return the new <code>WWTexture</code>.
	// *
	// * @throws IllegalArgumentException if the image source is null.
	// */
	// protected WWTexture makeTexture(Object imageSource)
	// {
	// return new LazilyLoadedTexture(imageSource, true);
	// }

	public void pick(DrawContext dc, Point pickPoint) {
		// This method is called only when ordered renderables are being drawn.

		if (dc == null) {
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		try {
			this.render(dc);
		} finally {
			this.pickSupport.resolvePick(dc, pickPoint, this.pickLayer);
		}
	}

	public void render(DrawContext dc) {
		// This render method is called twice for each of the pick and render passes during frame generation. It's first
		// called as a Renderable during layer processing, then a second time as an OrderedRenderable. The first two
		// call determines whether to add the shape to the ordered renderable list during pick and render. The third
		// call just draws the ordered renderable.

		if (dc == null) {
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Retrieve the cached data for the current globe. If it doesn't yet exist, create it. Most code subsequently
		// executed depends on currentData being non-null.
		this.currentData = (AbstractShapeData) this.shapeDataCache.getEntry(dc.getGlobe());
		if (this.currentData == null) {
			this.currentData = this.createCacheEntry(dc);
			this.shapeDataCache.addEntry(this.currentData);
		}

		if (!this.isVisible()) return;

		if (this.isTerrainDependent()) this.checkViewDistanceExpiration(dc);

		if (this.getExtent() != null) {
			if (!this.intersectsFrustum(dc)) return;

			// If the shape is less that a pixel in size, don't render it.
			if (dc.isSmall(this.getExtent(), 1)) return;
		}

		if (dc.isOrderedRenderingMode()) this.drawOrderedRenderable(dc);
		else this.makeOrderedRenderable(dc);
	}

	/**
	 * Determines whether to add this shape to the draw context's ordered renderable list. Creates this shapes
	 * renderable geometry.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void makeOrderedRenderable(DrawContext dc) {
		// Re-use values already calculated this frame.
		if (dc.getFrameTimeStamp() != this.getCurrentData().getFrameNumber()) {
			this.determineActiveAttributes();
			if (this.getActiveAttributes() == null) return;

			// Regenerate the positions and shape at a specified frequency.
			if (this.mustRegenerateGeometry(dc)) {
				if (!this.doMakeOrderedRenderable(dc)) return;

				this.fillVBO(dc);

				this.getCurrentData().restartTimer(dc);
			}

			this.getCurrentData().setFrameNumber(dc.getFrameTimeStamp());
		}

		if (!this.isOrderedRenderableValid(dc)) return;

		if (dc.isPickingMode()) this.pickLayer = dc.getCurrentLayer();

		this.addOrderedRenderable(dc);
	}

	/**
	 * Adds this shape to the draw context's ordered renderable list.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void addOrderedRenderable(DrawContext dc) {
		dc.addOrderedRenderable(this);
	}

	/**
	 * Determines which attributes -- normal, highlight or default -- to use each frame. Places the result in this
	 * shape's current active attributes.
	 * 
	 * @see #getActiveAttributes()
	 */
	protected void determineActiveAttributes() {
		if (this.isHighlighted()) {
			if (this.getHighlightAttributes() != null) this.activeAttributes.set(this.getHighlightAttributes());
			else {
				// If no highlight attributes have been specified we need to use the normal attributes but adjust them
				// to cause highlighting.
				if (this.getAttributes() != null) this.activeAttributes.set(this.getAttributes());
				else this.activeAttributes.set(defaultAttributes);

				this.activeAttributes.setInteriorColor(DEFAULT_HIGHLIGHT_COLOR);
				this.activeAttributes.setOutlineColor(DEFAULT_HIGHLIGHT_COLOR);
			}
		} else if (this.getAttributes() != null) {
			this.activeAttributes.set(this.getAttributes());
		} else {
			this.activeAttributes.set(defaultAttributes);
		}
	}

	/**
	 * Returns this shape's currently active attributes, as determined during the most recent call to {@link #determineActiveAttributes()}. The active
	 * attributes are either the normal or highlight attributes, depending on
	 * this shape's highlight flag, and incorporates default attributes for those not specified in the applicable
	 * attribute set.
	 * 
	 * @return this shape's currently active attributes.
	 */
	public ShapeAttributes getActiveAttributes() {
		return this.activeAttributes;
	}

	/**
	 * Indicates whether this shape's renderable geometry must be recomputed, either as a result of an attribute or
	 * property change or the expiration of the geometry regeneration interval.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return true if this shape's geometry must be regenerated, otherwise false.
	 */
	protected boolean mustRegenerateGeometry(DrawContext dc) {
		return this.getCurrentData().isExpired(dc) || !this.getCurrentData().isValid(dc);
	}

	/**
	 * Indicates whether this shape's interior must be drawn.
	 * 
	 * @return true if an interior must be drawn, otherwise false.
	 */
	protected boolean mustDrawInterior() {
		return this.getActiveAttributes().isEnableInterior();
	}

	/**
	 * Indicates whether this shape's outline must be drawn.
	 * 
	 * @return true if the outline should be drawn, otherwise false.
	 */
	protected boolean mustDrawOutline() {
		return this.getActiveAttributes().isEnableOutline();
	}

	/**
	 * Indicates whether standard lighting must be applied.
	 * 
	 * @param dc
	 *            the current draw context
	 * @return true if lighting must be applied, otherwise false.
	 */
	protected boolean mustApplyLighting(DrawContext dc) {
		return this.activeAttributes.isEnableLighting();
	}

	/**
	 * Indicates whether standard lighting must be applied.
	 * 
	 * @param dc
	 *            the current draw context
	 * @param activeAttrs
	 *            the attribute bundle to consider when determining whether lighting is applied. May be null, in
	 *            which case the current active attributes are used.
	 * @return true if lighting must be applied, otherwise false.
	 */
	protected boolean mustApplyLighting(DrawContext dc, ShapeAttributes activeAttrs) {
		return activeAttrs != null ? activeAttrs.isEnableLighting() : this.activeAttributes.isEnableLighting();
	}

	/**
	 * Indicates whether normal vectors must be computed by consulting the current active attributes.
	 * 
	 * @param dc
	 *            the current draw context
	 * @return true if normal vectors must be computed, otherwise false.
	 */
	protected boolean mustCreateNormals(DrawContext dc) {
		return this.mustCreateNormals(dc, this.getActiveAttributes());
	}

	/**
	 * Indicates whether standard lighting must be applied.
	 * 
	 * @param dc
	 *            the current draw context
	 * @param activeAttrs
	 *            the attribute bundle to consider when determining whether normals should be computed. May be
	 *            null, in which case the current active attributes are used.
	 * @return true if normal vectors must be computed, otherwise false.
	 */
	protected boolean mustCreateNormals(DrawContext dc, ShapeAttributes activeAttrs) {
		return activeAttrs != null ? activeAttrs.isEnableLighting() : this.getActiveAttributes().isEnableLighting();
	}

	/**
	 * Indicates whether this shape's geometry depends on the terrain.
	 * 
	 * @return true if this shape's geometry depends on the terrain, otherwise false.
	 */
	protected boolean isTerrainDependent() {
		return this.getAltitudeMode() != null && !this.getAltitudeMode().equals(AVKey.ABSOLUTE);
	}

	/**
	 * Indicates whether this shape's terrain-dependent geometry is continually computed as its distance from the eye
	 * point changes. This is often necessary to ensure that the shape is updated as the terrain precision changes. But
	 * it's often not necessary as well, and can be disabled.
	 * 
	 * @return true if the terrain dependent geometry is updated as the eye distance changes, otherwise false. The
	 *         default is true.
	 */
	public boolean isViewDistanceExpiration() {
		return viewDistanceExpiration;
	}

	/**
	 * Specifies whether this shape's terrain-dependent geometry is continually computed as its distance from the eye
	 * point changes. This is often necessary to ensure that the shape is updated as the terrain precision changes. But
	 * it's often not necessary as well, and can be disabled.
	 * 
	 * @param viewDistanceExpiration
	 *            true to enable view distance expiration, otherwise false.
	 */
	public void setViewDistanceExpiration(boolean viewDistanceExpiration) {
		this.viewDistanceExpiration = viewDistanceExpiration;
	}

	/**
	 * Determines whether this shape's geometry should be invalidated because the view distance changed, and if so,
	 * invalidates the geometry.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void checkViewDistanceExpiration(DrawContext dc) {
		// Determine whether the distance of this shape from the eye has changed significantly. Invalidate the previous
		// extent and expire the shape geometry if it has. "Significantly" is considered a 25% difference.

		if (!this.isViewDistanceExpiration()) return;

		Vec4 refPt = this.currentData.getReferencePoint();
		if (refPt == null) return;

		double newRefDistance = dc.getView().getEyePoint().distanceTo3(refPt);
		Double oldRefDistance = this.currentData.getReferenceDistance();
		if (oldRefDistance == null || Math.abs(newRefDistance - oldRefDistance) / oldRefDistance > 0.25) {
			this.currentData.setExpired(true);
			this.currentData.setExtent(null);
			this.currentData.setReferenceDistance(newRefDistance);
		}
	}

	/**
	 * Determines whether this shape intersects the view frustum.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return true if this shape intersects the frustum, otherwise false.
	 */
	protected boolean intersectsFrustum(DrawContext dc) {
		if (this.getExtent() == null) return true; // don't know the visibility, shape hasn't been computed yet

		// TODO: Restore these two lines after implementing World Wind Android picking frustums.
		// if (dc.isPickingMode())
		// return dc.getPickFrustums().intersectsAny(this.getExtent());

		return dc.getView().getFrustumInModelCoordinates().intersects(this.getExtent());
	}

	/**
	 * Draws this shape as an ordered renderable.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void drawOrderedRenderable(DrawContext dc) {
		this.beginDrawing(dc);
		try {
			this.doDrawOrderedRenderable(dc, this.pickSupport);

			if (this.isEnableBatchRendering()) this.drawBatched(dc);
		} finally {
			this.endDrawing(dc);
		}
	}

	/**
	 * Establish the OpenGL state needed to draw this shape.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void beginDrawing(DrawContext dc) {
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
	}

	/**
	 * Pop the state set in {@link #beginDrawing(DrawContext)}.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void endDrawing(DrawContext dc) {
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

	protected GpuProgram getDefaultGpuProgram(GpuResourceCache cache) {
		if (this.programCreationFailed) return null;

		GpuProgram program = cache.getProgram(this.programKey);

		if (program == null) {
			try {
				GpuProgram.GpuProgramSource source = GpuProgram.readProgramSource(DEFAULT_VERTEX_SHADER_PATH, DEFAULT_FRAGMENT_SHADER_PATH);
				program = new GpuProgram(source);
				cache.put(this.programKey, program);
			} catch (Exception e) {
				String msg = Logging.getMessage("GL.ExceptionLoadingProgram", DEFAULT_VERTEX_SHADER_PATH, DEFAULT_FRAGMENT_SHADER_PATH);
				Logging.error(msg);
				this.programCreationFailed = true;
			}
		}

		return program;
	}

	/**
	 * Draws this ordered renderable and all subsequent Path ordered renderables in the ordered renderable list. If the
	 * current pick mode is true, only shapes within the same layer are drawn as a batch.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void drawBatched(DrawContext dc) {
		// Draw as many as we can in a batch to save ogl state switching.
		Object nextItem = dc.peekOrderedRenderables();

		if (!dc.isPickingMode()) {
			while (nextItem != null && nextItem.getClass() == this.getClass()) {
				AbstractShape shape = (AbstractShape) nextItem;
				if (!shape.isEnableBatchRendering()) break;

				dc.pollOrderedRenderables(); // take it off the queue
				shape.doDrawOrderedRenderable(dc, this.pickSupport);

				nextItem = dc.peekOrderedRenderables();
			}
		} else if (this.isEnableBatchPicking()) {
			while (nextItem != null && nextItem.getClass() == this.getClass()) {
				AbstractShape shape = (AbstractShape) nextItem;
				if (!shape.isEnableBatchRendering() || !shape.isEnableBatchPicking()) break;

				if (shape.pickLayer != this.pickLayer) // batch pick only within a single layer
				break;

				dc.pollOrderedRenderables(); // take it off the queue
				shape.doDrawOrderedRenderable(dc, this.pickSupport);

				nextItem = dc.peekOrderedRenderables();
			}
		}
	}

	/**
	 * Draw this shape as an ordered renderable. If in picking mode, add it to the picked object list of specified {@link PickSupport}. The
	 * <code>PickSupport</code> may not be the one associated with this instance. During batch
	 * picking the <code>PickSupport</code> of the instance initiating the batch picking is used so that all shapes
	 * rendered in batch are added to the same pick list.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param pickCandidates
	 *            a pick support holding the picked object list to add this shape to.
	 */
	protected void doDrawOrderedRenderable(DrawContext dc, PickSupport pickCandidates) {
		if (dc.getCurrentProgram() == null) return; // No gpu program to run. Message already logged in getDefaultGpuProgram via beginDrawing.

		if (dc.isPickingMode()) {
			int color = dc.getUniquePickColor();
			pickCandidates.addPickableObject(this.createPickedObject(color));
			dc.getCurrentProgram().loadUniformColor("color", this.currentColor.set(color, false)); // Ignore alpha.
		}

		this.applyModelviewProjectionMatrix(dc);
		dc.drawOutlinedShape(this.outlineShapeRenderer, this);
	}

	protected void applyModelviewProjectionMatrix(DrawContext dc) {
		// Multiply the View's modelview-projection matrix by the shape's transform matrix to correctly transform shape
		// points into eye coordinates. This achieves the resolution we need on Gpus with limited floating point
		// precision keeping both the modelview-projection matrix and the point coordinates the Gpu uses as small as
		// possible when the eye point is near the shape.
		this.currentMatrix.multiplyAndSet(dc.getView().getModelviewProjectionMatrix(), this.getCurrentData().getTransformMatrix());
		dc.getCurrentProgram().loadUniformMatrix("mvpMatrix", this.currentMatrix);
	}

	/**
	 * Creates a {@link gov.nasa.worldwind.pick.PickedObject} for this shape and the specified unique pick color code.
	 * The PickedObject returned by this method will be added to the pick list to represent the current shape.
	 * 
	 * @param colorCode
	 *            the unique color code for this shape.
	 * @return a new picked object.
	 */
	protected PickedObject createPickedObject(int colorCode) {
		return new PickedObject(colorCode, this.getDelegateOwner() != null ? this.getDelegateOwner() : this);
	}

	/**
	 * Draws this shape's interior.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void drawInterior(DrawContext dc) {
		this.prepareToDrawInterior(dc, this.getActiveAttributes(), defaultAttributes);

		this.doDrawInterior(dc);
	}

	/**
	 * Establishes OpenGL state for drawing the interior, including setting the color/material. Enabling texture is left
	 * to the subclass.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param activeAttrs
	 *            the attributes indicating the state value to set.
	 * @param defaultAttrs
	 *            the attributes to use if <code>activeAttrs</code> does not contain a necessary value.
	 */
	protected void prepareToDrawInterior(DrawContext dc, ShapeAttributes activeAttrs, ShapeAttributes defaultAttrs) {
		if (activeAttrs == null || !activeAttrs.isEnableInterior()) return;

		if (!dc.isPickingMode()) {
			Color color = activeAttrs.getInteriorColor();
			if (color == null) color = defaultAttrs.getInteriorColor();

			// Disable writing the shape's interior fragments to the OpenGL depth buffer when the interior is
			// semi-transparent.
			if (color.a < 1) GLES20.glDepthMask(false);

			// Load the current interior color into the gpu program's color uniform variable. We first copy the outline
			// color into the current color so we can premultiply it. The SceneController configures the OpenGL blending
			// mode for premultiplied alpha colors.
			this.currentColor.set(color).premultiply();
			dc.getCurrentProgram().loadUniformColor("color", this.currentColor);
		}
	}

	/**
	 * Draws this shape's outline.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void drawOutline(DrawContext dc) {
		ShapeAttributes activeAttrs = this.getActiveAttributes();

		this.prepareToDrawOutline(dc, activeAttrs, defaultAttributes);

		this.doDrawOutline(dc);
	}

	/**
	 * Establishes OpenGL state for drawing the outline, including setting the color/material, line smoothing, line
	 * width and stipple. Disables texture.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param activeAttrs
	 *            the attributes indicating the state value to set.
	 * @param defaultAttrs
	 *            the attributes to use if <code>activeAttrs</code> does not contain a necessary value.
	 */
	protected void prepareToDrawOutline(DrawContext dc, ShapeAttributes activeAttrs, ShapeAttributes defaultAttrs) {
		if (activeAttrs == null || !activeAttrs.isEnableOutline()) return;

		if (!dc.isPickingMode()) {
			Color color = activeAttrs.getOutlineColor();
			if (color == null) color = defaultAttrs.getOutlineColor();

			// Load the current outline color into the gpu program's color uniform variable. We first copy the outline
			// color into the current color so we can premultiply it. The SceneController configures the OpenGL blending
			// mode for premultiplied alpha colors.
			this.currentColor.set(color).premultiply();
			dc.getCurrentProgram().loadUniformColor("color", this.currentColor);
		}

		if (dc.isPickingMode() && activeAttrs.getOutlineWidth() < this.getOutlinePickWidth()) GLES20.glLineWidth(this.getOutlinePickWidth());
		else GLES20.glLineWidth((float) activeAttrs.getOutlineWidth());
	}

	/**
	 * Computes a model-coordinate point from a position, applying this shape's altitude mode.
	 * 
	 * @param terrain
	 *            the terrain to compute a point relative to the globe's surface.
	 * @param position
	 *            the position to compute a point for.
	 * @return the model-coordinate point corresponding to the position and this shape's shape type.
	 */
	protected Vec4 computePoint(Terrain terrain, Position position) {
		String altitudeMode = this.getAltitudeMode();

		if (AVKey.CLAMP_TO_GROUND.equals(altitudeMode)) return terrain.getSurfacePoint(position.latitude, position.longitude, 0d);
		else if (AVKey.RELATIVE_TO_GROUND.equals(altitudeMode)) return terrain.getSurfacePoint(position);

		// Raise the shape to accommodate vertical exaggeration applied to the terrain.
		double height = position.elevation * terrain.getVerticalExaggeration();

		return terrain.getGlobe().computePointFromPosition(position, height);
	}

	/**
	 * Computes this shape's approximate extent from its positions.
	 * 
	 * @param globe
	 *            the globe to use to compute the extent.
	 * @param verticalExaggeration
	 *            the vertical exaggeration to apply to computed terrain points.
	 * @param positions
	 *            the positions to compute the extent for.
	 * @return the extent, or null if an extent cannot be computed. Null is returned if either <code>globe</code> or <code>positions</code> is null.
	 */
	protected Extent computeExtentFromPositions(Globe globe, double verticalExaggeration, Iterable<? extends LatLon> positions) {
		if (globe == null || positions == null) return null;

		Sector mySector = this.getSector();
		if (mySector == null) return null;

		double[] extremes;
		double[] minAndMaxElevations = globe.getMinAndMaxElevations(mySector);
		if (!AVKey.CLAMP_TO_GROUND.equals(this.getAltitudeMode())) {
			extremes = new double[] { Double.MAX_VALUE, -Double.MAX_VALUE };
			for (LatLon pos : positions) {
				double elevation = pos instanceof Position ? ((Position) pos).elevation : 0;
				if (AVKey.RELATIVE_TO_GROUND.equals(this.getAltitudeMode())) elevation += minAndMaxElevations[1];

				if (extremes[0] > elevation) extremes[0] = elevation * verticalExaggeration; // min
				if (extremes[1] < elevation) extremes[1] = elevation * verticalExaggeration; // max
			}
		} else {
			extremes = minAndMaxElevations;
		}

		return Sector.computeBoundingBox(globe, verticalExaggeration, mySector, extremes[0], extremes[1]);
	}

	/**
	 * Get or create OpenGL resource IDs for the current data cache entry.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return an array containing the coordinate vertex buffer ID in the first position and the index vertex buffer ID
	 *         in the second position.
	 */
	protected int[] getVboIds(DrawContext dc) {
		return (int[]) dc.getGpuResourceCache().get(this.getCurrentData().getVboCacheKey());
	}

	/**
	 * Removes from the GPU resource cache the entry for the current data cache entry's VBOs.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void clearCachedVbos(DrawContext dc) {
		dc.getGpuResourceCache().remove(this.getCurrentData().getVboCacheKey());
	}

	protected int countTriangleVertices(List<List<Integer>> prims, List<Integer> primTypes) {
		int numVertices = 0;

		for (int i = 0; i < prims.size(); i++) {
			switch (primTypes.get(i)) {
				case GLES20.GL_TRIANGLES:
					numVertices += prims.get(i).size();
					break;

				case GLES20.GL_TRIANGLE_FAN:
					numVertices += (prims.get(i).size() - 2) * 3; // N tris from N + 2 vertices
					break;

				case GLES20.GL_TRIANGLE_STRIP:
					numVertices += (prims.get(i).size() - 2) * 3; // N tris from N + 2 vertices
					break;
			}
		}

		return numVertices;
	}

	public void move(Position delta) {
		if (delta == null) {
			String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Position refPos = this.getReferencePosition();

		// The reference position is null if this shape has no positions. In this case moving the shape by a
		// relative delta is meaningless because the shape has no geographic location. Therefore we fail softly by
		// exiting and doing nothing.
		if (refPos == null) return;

		this.moveTo(refPos.add(delta));
	}
}
