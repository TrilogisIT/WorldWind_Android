/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import static android.opengl.GLES20.GL_BACK;
import static android.opengl.GLES20.GL_BLEND;
import static android.opengl.GLES20.GL_COLOR_BUFFER_BIT;
import static android.opengl.GLES20.GL_CULL_FACE;
import static android.opengl.GLES20.GL_ONE;
import static android.opengl.GLES20.GL_ONE_MINUS_SRC_ALPHA;
import static android.opengl.GLES20.glBlendFunc;
import static android.opengl.GLES20.glCullFace;
import static android.opengl.GLES20.glEnable;
import static gov.nasa.worldwind.util.OGLStackHandler.GL_POLYGON_BIT;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.DepthBufferSupport;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.render.PreRenderable;
import gov.nasa.worldwind.render.SurfaceObjectTileBuilder;
import gov.nasa.worldwind.render.SurfaceTile;
import gov.nasa.worldwind.terrain.SectorGeometry;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.GLRuntimeCapabilities;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.OGLStackHandler;
import gov.nasa.worldwind.util.PerformanceStatistic;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import android.graphics.Point;
import android.opengl.GLES20;
import android.os.SystemClock;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 *
 * @author dcollins
 * @version $Id: SceneController.java 834 2012-10-08 22:25:55Z dcollins $
 */
public class SceneController extends WWObjectImpl {
	protected Model model;
	protected View view;
	protected double verticalExaggeration;
	protected DrawContext dc;
	protected Color clearColor = new Color();
	protected GpuResourceCache gpuResourceCache;
	protected DepthBufferSupport mDepthSupport = new DepthBufferSupport();
	protected boolean deepPick;
	protected PickSupport pickSupport = new PickSupport();
	protected Point pickPoint;
	protected PickedObjectList objectsAtPickPoint = new PickedObjectList();

	protected Set<String> perFrameStatisticsKeys = new HashSet<String>();
	protected final Map<String, PerformanceStatistic> perFrameStatistics = Collections.synchronizedMap(new ConcurrentHashMap<String, PerformanceStatistic>());
	protected GLRuntimeCapabilities glRuntimeCaps = new GLRuntimeCapabilities();

	/** Support class used to build the composite representation of surface objects as a list of SurfaceTiles. */
	protected SurfaceObjectTileBuilder surfaceObjectTileBuilder;
	/** The composite surface object representation. Populated each frame by the {@link #surfaceObjectTileBuilder}. */
	protected List<SurfaceTile> surfaceObjectTiles = new ArrayList<SurfaceTile>();
	/** The display name for the surface object tile count performance statistic. */
	protected static final String SURFACE_OBJECT_TILE_COUNT_NAME = "Surface Object Tiles";

	protected SceneController() {
		this.setVerticalExaggeration(Configuration.getDoubleValue(AVKey.VERTICAL_EXAGGERATION));
		this.dc = this.createDrawContext();
	}

	protected PickSupport getPickSupport() {
		pickSupport.setup((int)view.getViewport().width, (int)view.getViewport().height);
		return pickSupport;
	}

	protected DepthBufferSupport getDepthBufferSupport() {
		mDepthSupport.setup((int)view.getViewport().width, (int)view.getViewport().height);
		return mDepthSupport;
	}

	protected DrawContext createDrawContext() {
		return new DrawContext();
	}

	/**
	 * Indicates the scene controller's model. This returns <code>null</code> if the scene controller has no model.
	 *
	 * @return the scene controller's model, or <code>null</code> if the scene controller has no model.
	 */
	public Model getModel() {
		return this.model;
	}

	/**
	 * Specifies the scene controller's model. This method fires an {@link gov.nasa.worldwind.avlist.AVKey#MODEL} property change event.
	 *
	 * @param model
	 *            the scene controller's model.
	 */
	public void setModel(Model model) {
		if (this.model != null) this.model.removePropertyChangeListener(this);
		if (model != null) model.addPropertyChangeListener(this);

		Model oldModel = this.model;
		this.model = model;
		this.firePropertyChange(AVKey.MODEL, oldModel, model);
	}

	/**
	 * Returns the current view. This method fires an {@link gov.nasa.worldwind.avlist.AVKey#VIEW} property change
	 * event.
	 *
	 * @return the current view.
	 */
	public View getView() {
		return this.view;
	}

	/**
	 * Sets the current view.
	 *
	 * @param view
	 *            the view.
	 */
	public void setView(View view) {
		if (this.view != null) this.view.removePropertyChangeListener(this);
		if (view != null) view.addPropertyChangeListener(this);

		View oldView = this.view;
		this.view = view;

		this.firePropertyChange(AVKey.VIEW, oldView, view);
	}

	/**
	 * Indicates the current vertical exaggeration.
	 *
	 * @return the current vertical exaggeration.
	 */
	public double getVerticalExaggeration() {
		return this.verticalExaggeration;
	}

	/**
	 * Specifies the exaggeration to apply to elevation values of terrain and other displayed items.
	 *
	 * @param verticalExaggeration
	 *            the vertical exaggeration to apply.
	 */
	public void setVerticalExaggeration(double verticalExaggeration) {
		if(this.verticalExaggeration != verticalExaggeration) {
			double oldVE = this.verticalExaggeration;
			this.verticalExaggeration = verticalExaggeration;
			this.firePropertyChange(AVKey.VERTICAL_EXAGGERATION, oldVE, this.verticalExaggeration);
		}
	}

	/**
	 * Returns this scene controller's GPU Resource cache.
	 *
	 * @return this scene controller's GPU Resource cache.
	 */
	public GpuResourceCache getGpuResourceCache() {
		return this.gpuResourceCache;
	}

	/**
	 * Specifies the GPU Resource cache to use.
	 *
	 * @param gpuResourceCache
	 *            the texture cache.
	 */
	public void setGpuResourceCache(GpuResourceCache gpuResourceCache) {
		this.gpuResourceCache = gpuResourceCache;
	}

	/**
	 * Indicates whether all items under the cursor are identified during picking.
	 *
	 * @return true if all items under the cursor are identified during picking, otherwise false.
	 */
	public boolean isDeepPickEnabled() {
		return this.deepPick;
	}

	/**
	 * Specifies whether all items under the cursor are identified during picking.
	 *
	 * @param tf
	 *            true to identify all items under the cursor during picking, otherwise false.
	 */
	public void setDeepPickEnabled(boolean tf) {
		this.deepPick = tf;
	}

	/**
	 * Returns the current pick point in AWT screen coordinates.
	 *
	 * @return the current pick point, or <code>null</code> if no pick point is current.
	 * @see #setPickPoint(Point)
	 */
	public Point getPickPoint() {
		return this.pickPoint;
	}

	/**
	 * Specifies the current pick point in AWT screen coordinates, or <code>null</code> to indicate that there is no
	 * pick point. Each frame, this scene controller determines which objects are drawn at the pick point and places
	 * them in a PickedObjectList. This list can be accessed by calling {@link #getObjectsAtPickPoint()}.
	 * <p/>
	 * If the pick point is <code>null</code>, this scene controller ignores the pick point and the list of objects returned by getPickedObjectList is empty.
	 *
	 * @param pickPoint
	 *            the current pick point, or <code>null</code>.
	 */
	public void setPickPoint(Point pickPoint) {
		this.pickPoint = pickPoint;
	}

	/**
	 * Returns the list of picked objects at the current pick point. The returned list is computed during the most
	 * recent call to repaint.
	 *
	 * @return the list of picked objects at the pick point, or null if no objects are currently picked.
	 */
	public PickedObjectList getObjectsAtPickPoint() {
		return this.objectsAtPickPoint;
	}

	public void setPerFrameStatisticsKeys(Set<String> keys)
	{
		this.perFrameStatisticsKeys.clear();
		if (keys == null)
			return;

		for (String key : keys)
		{
			if (key != null)
				this.perFrameStatisticsKeys.add(key);
		}
	}

	public Map<String, PerformanceStatistic> getPerFrameStatistics()
	{
		return perFrameStatistics;
	}

	/**
	 * Cause the window to regenerate the frame, including pick resolution.
	 *
	 * @param viewportWidth
	 *            the width of the current viewport this scene controller is associated with, in pixels. Must
	 *            not be less than zero.
	 * @param viewportHeight
	 *            the height of the current viewport this scene controller is associated with, in pixels.
	 *            Must not be less than zero.
	 * @throws IllegalArgumentException
	 *             if either viewportWidth or viewportHeight are last ess than zero.
	 */
	public void drawFrame(double deltaTime, int viewportWidth, int viewportHeight) {
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

//		perFrameStatistics.clear();
		this.surfaceObjectTiles.clear(); // Clear the surface object tiles generated during the last frame.
		this.glRuntimeCaps.initialize();
		// Prepare the drawing context for a new frame then cause this scene controller to draw its content. There is no
		// need to explicitly swap the front and back buffers here, as the owner WorldWindow does this for us. In the
		// case of WorldWindowGLSurfaceView, the GLSurfaceView automatically swaps the front and back buffers for us.
		this.initializeDrawContext(this.dc, viewportWidth, viewportHeight);
		pickSupport.setup(viewportWidth, viewportHeight);
		mDepthSupport.setup(viewportWidth, viewportHeight);
		dc.setDeltaTime(deltaTime);

		this.doDrawFrame(this.dc);

		Set<String> perfKeys = dc.getPerFrameStatisticsKeys();

		if (perfKeys.contains(PerformanceStatistic.MEMORY_CACHE) || perfKeys.contains(PerformanceStatistic.ALL))
		{
			this.dc.setPerFrameStatistics(WorldWind.getMemoryCacheSet().getPerformanceStatistics());
		}

		if (perfKeys.contains(PerformanceStatistic.TEXTURE_CACHE) || perfKeys.contains(PerformanceStatistic.ALL))
		{
			if (dc.getTextureCache() != null)
				this.dc.setPerFrameStatistic(PerformanceStatistic.TEXTURE_CACHE,
						"Texture Cache size (Kb)", this.dc.getTextureCache().getUsedCapacity() / 1000);
		}

		if (perfKeys.contains(PerformanceStatistic.JVM_HEAP) || perfKeys.contains(PerformanceStatistic.ALL))
		{
			long totalMemory = Runtime.getRuntime().totalMemory();
			this.dc.setPerFrameStatistic(PerformanceStatistic.JVM_HEAP,
					"JVM total memory (Kb)", totalMemory / 1000);

			this.dc.setPerFrameStatistic(PerformanceStatistic.JVM_HEAP_USED,
					"JVM used memory (Kb)", (totalMemory - Runtime.getRuntime().freeMemory()) / 1000);
		}
	}

	protected void doDrawFrame(DrawContext dc) {
		this.initializeFrame(dc);
		try {
			this.applyView(dc);
			this.createPickFrustum(dc);
			this.createTerrain(dc);
			this.preRender(dc);
//			this.clearFrame(dc);
//			this.pick(dc);
			this.clearFrame(dc);
			this.draw(dc);
		} finally {
			this.finalizeFrame(dc);
		}
	}

	protected void initializeDrawContext(DrawContext dc, int viewportWidth, int viewportHeight) {
		dc.initialize(viewportWidth, viewportHeight);
		dc.setFrameTimeStamp(SystemClock.elapsedRealtime());
		dc.setGLRuntimeCapabilities(glRuntimeCaps);
		dc.setModel(this.model);
		dc.setView(this.view);
		dc.setVerticalExaggeration(this.verticalExaggeration);
		dc.setGpuResourceCache(this.gpuResourceCache);
		dc.setPickPoint(this.pickPoint);
		dc.setPerFrameStatisticsKeys(this.perFrameStatisticsKeys, this.perFrameStatistics);
	}

	protected void initializeFrame(DrawContext dc) {
		GLES20.glEnable(GLES20.GL_BLEND);
		WorldWindowImpl.glCheckError("glEnable: GL_BLEND");
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		WorldWindowImpl.glCheckError("glEnable: GL_CULL_FACE");
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		WorldWindowImpl.glCheckError("glEnable: GL_DEPTH_TEST");
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA); // Blend in premultiplied alpha mode. 
		WorldWindowImpl.glCheckError("glBlendFunc");
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		WorldWindowImpl.glCheckError("glDepthFunc");
		// We do not specify glCullFace, because the default cull face state GL_BACK is appropriate for our needs.
	}

	protected void finalizeFrame(DrawContext dc) {
		// Restore the default GL state values we modified in initializeFrame.
		GLES20.glDisable(GLES20.GL_BLEND);
		WorldWindowImpl.glCheckError("glDisable: GL_BLEND");
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		WorldWindowImpl.glCheckError("glDisable: GL_CULL_FACE");
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		WorldWindowImpl.glCheckError("glDisable: GL_DEPTH_TEST");
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO);
		WorldWindowImpl.glCheckError("glBlendFunc");
		GLES20.glDepthFunc(GLES20.GL_LESS);
		WorldWindowImpl.glCheckError("glDepthFunc");
		GLES20.glClearColor(0f, 0f, 0f, 0f);
		WorldWindowImpl.glCheckError("glClearColor");
	}

	protected void clearFrame(DrawContext dc) {
		// Separate the DrawContext's background color components into the SceneController's clearColor.
		this.clearColor.set(dc.getClearColor());
		// Set the DrawContext's clear color, then clear the framebuffer's color buffer and depth buffer. This fills
		// the color buffer with the background color, and fills the depth buffer with 1 (the default).
		GLES20.glClearColor((float) this.clearColor.r, (float) this.clearColor.g, (float) this.clearColor.b, (float) this.clearColor.a);
		WorldWindowImpl.glCheckError("glClearColor");
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
		WorldWindowImpl.glCheckError("glClear");
	}

	protected void applyView(DrawContext dc) {
		if (this.view != null) this.view.apply(dc);
	}

	protected void createTerrain(DrawContext dc) {
		SectorGeometryList surfaceGeometry = null;

		try {
			if (dc.getGlobe() != null) surfaceGeometry = dc.getGlobe().tessellate(dc);

			// If there's no surface geometry, just log a warning and keep going. Some layers may have meaning without
			// surface geometry.
			if (surfaceGeometry == null) Logging.warning(Logging.getMessage("generic.NoSurfaceGeometry"));
		} catch (Exception e) {
			Logging.error(Logging.getMessage("generic.ExceptionCreatingSurfaceGeometry"), e);
		}

		dc.setSurfaceGeometry(surfaceGeometry);
		dc.setVisibleSector(surfaceGeometry != null ? surfaceGeometry.getSector() : null);
	}

	protected void preRender(DrawContext dc)
	{
		try
		{
			dc.setPreRenderMode(true);

			// Pre-render the layers.
			if (dc.getLayers() != null)
			{
				for (Layer layer : dc.getLayers())
				{
					try
					{
						dc.setCurrentLayer(layer);
						layer.preRender(dc);
					}
					catch (Exception e)
					{
						String message = Logging.getMessage("SceneController.ExceptionWhilePreRenderingLayer",
								(layer != null ? layer.getClass().getName() : Logging.getMessage("term.unknown")));
						Logging.error(message, e);
						// Don't abort; continue on to the next layer.
					}
				}
				dc.setCurrentLayer(null);
			}

			// Pre-render the deferred/ordered surface renderables.
			this.preRenderOrderedSurfaceRenderables(dc);
		}
		catch (Exception e)
		{
			Logging.error(Logging.getMessage("BasicSceneController.ExceptionDuringPreRendering"),
					e);
		}
		finally
		{
			dc.setPreRenderMode(false);
		}
	}

	protected void draw(DrawContext dc) {
		this.drawLayers(dc);
		// Draw the deferred/ordered surface renderables.
		this.drawOrderedSurfaceRenderables(dc);
		this.drawOrderedRenderables(dc);
		this.drawDiagnosticDisplays(dc);
	}

	protected void drawLayers(DrawContext dc) {
		if (dc.getLayers() == null) return;

		for (Layer layer : dc.getLayers()) {
			try {
				if (layer != null) {
					dc.setCurrentLayer(layer);
					layer.render(dc);
				}
			} catch (Exception e) {
				String msg = Logging.getMessage("generic.ExceptionRenderingLayer", (layer != null ? layer.getName() : Logging.getMessage("term.Unknown")));
				Logging.error(msg, e);
				// Don't abort; continue on to the next layer.
			}
		}

		dc.setCurrentLayer(null);
	}

	protected void drawOrderedRenderables(DrawContext dc) {
		dc.setOrderedRenderingMode(true);

		while (dc.peekOrderedRenderables() != null) {
			OrderedRenderable or = dc.pollOrderedRenderables();

			try {
				dc.setCurrentLayer(or.getLayer());
				or.render(dc);
				dc.setCurrentLayer(null);
			} catch (Exception e) {
				String msg = Logging.getMessage("generic.ExceptionRenderingOrderedRenderable", or);
				Logging.error(msg, e);
				// Don't abort; continue on to the next ordered renderable.
			}
		}

		dc.setOrderedRenderingMode(false);
	}

	protected void drawDiagnosticDisplays(DrawContext dc) {
		if (dc.getSurfaceGeometry() != null && dc.getModel() != null
				&& (dc.getModel().isShowWireframe() || dc.getModel().isShowTessellationBoundingVolumes()
				|| dc.getModel().isShowTessellationTileIds()))
		{
			Model model = dc.getModel();

			for (SectorGeometry sg : dc.getSurfaceGeometry())
			{
				if (model.isShowWireframe())
					sg.renderWireframe(dc);

				if (model.isShowTessellationBoundingVolumes())
					sg.renderBoundingVolume(dc);

				if(model.isShowTessellationTileIds())
					sg.renderTileID(dc);
			}
		}
	}

	protected void pick(DrawContext dc) {
		try {
			dc.setPickingMode(true);
			getPickSupport().beginPicking(dc);
			getPickSupport().bindFrameBuffer();
			this.doPick(dc);
		} finally {
			getPickSupport().unbindFrameBuffer();
			dc.setPickingMode(false);
			getPickSupport().endPicking(dc);
		}
	}

	protected void createPickFrustum(DrawContext dc)
	{
		dc.addPickPointFrustum();
//		dc.addPickRectangleFrustum();
	}

	protected void doPick(DrawContext dc) {
		this.doPickTerrain(dc);
		this.doPickNonTerrain(dc);
		this.resolveTopPick(dc);
		this.objectsAtPickPoint.set(dc.getObjectsAtPickPoint());

		if (this.isDeepPickEnabled() && this.objectsAtPickPoint.hasNonTerrainObjects()) this.doDeepPick(dc);
	}

	protected void doPickTerrain(DrawContext dc) {
		if (dc.getSurfaceGeometry() == null || dc.getPickPoint() == null) return;

		dc.getSurfaceGeometry().pick(dc, dc.getPickPoint());
	}

	protected void doPickNonTerrain(DrawContext dc) {
		if (dc.getPickPoint() == null) // Don't do the pick if there's no current pick point.
			return;

		this.pickLayers(dc);
		// Pick against the deferred/ordered surface renderables.
		this.pickOrderedSurfaceRenderables(dc);
		this.pickOrderedRenderables(dc);
	}

	protected void pickLayers(DrawContext dc) {
		if (dc.getLayers() == null) return;

		for (Layer layer : dc.getLayers()) {
			try {
				if (layer != null) {
					dc.setCurrentLayer(layer);
					layer.pick(dc, dc.getPickPoint());
				}
			} catch (Exception e) {
				String msg = Logging.getMessage("generic.ExceptionPickingLayer", (layer != null ? layer.getName() : Logging.getMessage("term.Unknown")));
				Logging.error(msg, e);
				// Don't abort; continue on to the next layer.
			}
		}

		dc.setCurrentLayer(null);
	}

	protected void pickOrderedRenderables(DrawContext dc) {
		dc.setOrderedRenderingMode(true);

		while (dc.peekOrderedRenderables() != null) {
			OrderedRenderable or = dc.pollOrderedRenderables();

			try {
				or.pick(dc, dc.getPickPoint());
			} catch (Exception e) {
				String msg = Logging.getMessage("generic.ExceptionPickingOrderedRenderable", or);
				Logging.error(msg, e);
				// Don't abort; continue on to the next ordered renderable.
			}
		}

		dc.setOrderedRenderingMode(false);
	}

	protected void resolveTopPick(DrawContext dc) {
		// Make a last reading to find out which is the top (resultant) color.
		PickedObjectList pickedObjects = dc.getObjectsAtPickPoint();
		if (pickedObjects != null && pickedObjects.size() == 1) {
			pickedObjects.get(0).setOnTop();
		} else if (pickedObjects != null && pickedObjects.size() > 1) {
			int colorCode = dc.getPickColor(dc.getPickPoint());
			if (colorCode != 0) {
				// Find the picked object in the list and set the "onTop" flag.
				for (PickedObject po : pickedObjects) {
					if (po != null && po.getColorCode() == colorCode) {
						po.setOnTop();
						break;
					}
				}
			}
		}
	}

	protected void doDeepPick(DrawContext dc) {
		try {
			this.beginDeepPicking(dc);
			this.doPickNonTerrain(dc);
		} finally {
			this.endDeepPicking(dc);
		}

		PickedObjectList currentPickedObjects = this.objectsAtPickPoint;
		this.objectsAtPickPoint = this.mergePickedObjectLists(currentPickedObjects, dc.getObjectsAtPickPoint());
	}

	/**
	 * Configures the draw context and GL state for deep picking. This ensures that an object can be picked regardless
	 * its depth relative to other objects. This makes the following GL state changes:
	 * <ul>
	 * <li>Disable depth test</li>
	 * </ul>
	 *
	 * @param dc
	 *            the draw context to configure.
	 */
	protected void beginDeepPicking(DrawContext dc) {
		dc.setDeepPickingEnabled(true);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST); // Depth test is disabled by default, but enabled in initializeFrame. 
		WorldWindowImpl.glCheckError("glDisable: GL_DEPTH_TEST");
	}

	/**
	 * Restores the draw context and the GL state modified in beginDeepPicking. This makes the following GL state
	 * changes:
	 * <ul>
	 * <li>Enable depth test</li>
	 * </ul>
	 *
	 * @param dc
	 *            the draw context on which to restore state.
	 */
	protected void endDeepPicking(DrawContext dc) {
		dc.setDeepPickingEnabled(false);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST); // Depth test is disabled by default, but enabled in initializeFrame. 
		WorldWindowImpl.glCheckError("glEnable: GL_DEPTH_TEST");
	}

	protected PickedObjectList mergePickedObjectLists(PickedObjectList listA, PickedObjectList listB) {
		if (listA == null || listB == null || !listA.hasNonTerrainObjects() || !listB.hasNonTerrainObjects()) return listA;

		for (PickedObject pb : listB) {
			if (pb.isTerrain()) continue;

			boolean common = false; // cannot modify listA within its iterator, so use a flag to indicate commonality
			for (PickedObject pa : listA) {
				if (pa.isTerrain()) continue;

				if (pa.getObject() == pb.getObject()) {
					common = true;
					break;
				}
			}

			if (!common) listA.add(pb);
		}

		return listA;
	}

	//**************************************************************//
	//********************  Ordered Surface Renderable  ************//
	//**************************************************************//

	protected void preRenderOrderedSurfaceRenderables(DrawContext dc)
	{
		if (dc.getOrderedSurfaceRenderables().isEmpty())
			return;

		dc.setOrderedRenderingMode(true);

		// Build a composite representation of the SurfaceObjects. This operation potentially modifies the framebuffer
		// contents to update surface tile textures, therefore it must be executed during the preRender phase.
		this.buildCompositeSurfaceObjects(dc);

		// PreRender the individual deferred/ordered surface renderables.
		int logCount = 0;
		while (dc.getOrderedSurfaceRenderables().peek() != null)
		{
			try
			{
				OrderedRenderable or = dc.getOrderedSurfaceRenderables().poll();
				if (or instanceof PreRenderable)
					((PreRenderable) or).preRender(dc);
			}
			catch (Exception e)
			{
				Logging.warning(Logging.getMessage("BasicSceneController.ExceptionDuringPreRendering"), e);

				// Limit how many times we log a problem.
				if (++logCount > Logging.getMaxMessageRepeatCount())
					break;
			}
		}

		dc.setOrderedRenderingMode(false);
	}

	protected void pickOrderedSurfaceRenderables(DrawContext dc)
	{
		dc.setOrderedRenderingMode(true);

		// Pick the individual deferred/ordered surface renderables. We don't use the composite representation of
		// SurfaceObjects because we need to distinguish between individual objects. Therefore we let each object handle
		// drawing and resolving picking.
		while (dc.getOrderedSurfaceRenderables().peek() != null)
		{
			OrderedRenderable or = dc.getOrderedSurfaceRenderables().poll();
			dc.setCurrentLayer(or.getLayer());
			or.pick(dc, dc.getPickPoint());
			dc.setCurrentLayer(null);
		}

		dc.setOrderedRenderingMode(false);
	}

	protected void drawOrderedSurfaceRenderables(DrawContext dc)
	{
		dc.setOrderedRenderingMode(true);

		// Draw the composite representation of the SurfaceObjects created during preRendering.
		this.drawCompositeSurfaceObjects(dc);

		// Draw the individual deferred/ordered surface renderables. SurfaceObjects that add themselves to the ordered
		// surface renderable queue during preRender are drawn in drawCompositeSurfaceObjects. Since this invokes
		// SurfaceObject.render during preRendering, SurfaceObjects should not add themselves to the ordered surface
		// renderable queue for rendering. We assume this queue is not populated with SurfaceObjects that participated
		// in the composite representation created during preRender.
		while (dc.getOrderedSurfaceRenderables().peek() != null)
		{
			try
			{
				OrderedRenderable or = dc.getOrderedSurfaceRenderables().poll();
				dc.setCurrentLayer(or.getLayer());
				or.render(dc);
			}
			catch (Exception e)
			{
				Logging.warning(Logging.getMessage("BasicSceneController.ExceptionDuringRendering"), e);
			} finally {
				dc.setCurrentLayer(null);
			}
		}

		dc.setOrderedRenderingMode(false);
	}

	/**
	 * Builds a composite representation for all {@link gov.nasa.worldwind.render.SurfaceObject} instances in the draw
	 * context's ordered surface renderable queue. While building the composite representation this invokes {@link
	 * gov.nasa.worldwind.render.SurfaceObject#render(gov.nasa.worldwind.render.DrawContext)} in ordered rendering mode.
	 * This does nothing if the ordered surface renderable queue is empty, or if it does not contain any
	 * SurfaceObjects.
	 * <p/>
	 * This method is called during the preRender phase, and is therefore free to modify the framebuffer contents to
	 * create the composite representation.
	 *
	 * @param dc The drawing context containing the surface objects to build a composite representation for.
	 *
	 * @see gov.nasa.worldwind.render.DrawContext#getOrderedSurfaceRenderables()
	 */
	protected void buildCompositeSurfaceObjects(DrawContext dc)
	{
		// If the the draw context's ordered surface renderable queue is empty, then there are no surface objects to
		// build a composite representation of.
		if (dc.getOrderedSurfaceRenderables().isEmpty())
			return;

		// Lazily create the support object used to build the composite representation. We keep a reference to the
		// SurfaceObjectTileBuilder used to build the tiles because it acts as a cache key to the tiles and determines
		// when the tiles must be updated. The tile builder does not retain any references the SurfaceObjects, so
		// keeping a reference to it does not leak memory should we never use it again.
		if (this.surfaceObjectTileBuilder == null)
			this.surfaceObjectTileBuilder = this.createSurfaceObjectTileBuilder();

		// Build the composite representation as a list of surface tiles.
		List<SurfaceTile> tiles = this.surfaceObjectTileBuilder.buildTiles(dc, dc.getOrderedSurfaceRenderables());
		if (tiles != null)
			this.surfaceObjectTiles.addAll(tiles);

		if(WorldWindowImpl.DEBUG)
			Logging.verbose("Built composite surface object tiles #"+tiles.size());
	}

	/**
	 * Causes the scene controller to draw the composite representation of all {@link
	 * gov.nasa.worldwind.render.SurfaceObject} instances in the draw context's ordered surface renderable queue. This
	 * representation was built during the preRender phase. This does nothing if the ordered surface renderable queue is
	 * empty, or if it does not contain any SurfaceObjects.
	 *
	 * @param dc The drawing context containing the surface objects who's composite representation is drawn.
	 */
	protected void drawCompositeSurfaceObjects(DrawContext dc)
	{
		// The composite representation is stored as a list of surface tiles. If the list is empty, then there are no
		// SurfaceObjects to draw.
		if (this.surfaceObjectTiles.isEmpty())
			return;

		int attributeMask =
				GL_COLOR_BUFFER_BIT   // For alpha test enable, blend enable, alpha func, blend func, blend ref.
						| GL_POLYGON_BIT; // For cull face enable, cull face, polygon mode.

		OGLStackHandler ogsh = new OGLStackHandler();
		ogsh.pushAttrib(attributeMask);
		try
		{
			glEnable(GL_BLEND);
			glEnable(GL_CULL_FACE);
			glCullFace(GL_BACK);
			// Enable blending in premultiplied color mode. The color components in each surface object tile are
			// premultiplied by the alpha component.
			glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

			dc.getSurfaceTileRenderer().renderTiles(dc, this.surfaceObjectTiles);
			dc.setPerFrameStatistic(PerformanceStatistic.IMAGE_TILE_COUNT, SURFACE_OBJECT_TILE_COUNT_NAME,
					this.surfaceObjectTiles.size());
		}
		finally
		{
			ogsh.popAttrib(attributeMask);
		}
	}

	/**
	 * Returns a new {@link gov.nasa.worldwind.render.SurfaceObjectTileBuilder} configured to build a composite
	 * representation of {@link gov.nasa.worldwind.render.SurfaceObject} instances.
	 *
	 * @return A new {@link gov.nasa.worldwind.render.SurfaceObjectTileBuilder}.
	 */
	protected SurfaceObjectTileBuilder createSurfaceObjectTileBuilder()
	{
		return new SurfaceObjectTileBuilder();
	}
}
