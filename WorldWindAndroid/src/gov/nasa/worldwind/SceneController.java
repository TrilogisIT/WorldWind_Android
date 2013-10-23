/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.render.Color;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.util.Logging;
import android.graphics.Point;
import android.opengl.GLES20;

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
	protected boolean deepPick;
	protected Point pickPoint;
	protected PickedObjectList objectsAtPickPoint = new PickedObjectList();

	protected SceneController() {
		this.setVerticalExaggeration(Configuration.getDoubleValue(AVKey.VERTICAL_EXAGGERATION));
		this.dc = this.createDrawContext();
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
		Double oldVE = this.verticalExaggeration;
		this.verticalExaggeration = verticalExaggeration;
		this.firePropertyChange(AVKey.VERTICAL_EXAGGERATION, oldVE, verticalExaggeration);
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
	 *             if either viewportWidth or viewportHeight are less than zero.
	 */
	public void drawFrame(int viewportWidth, int viewportHeight) {
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

		// Prepare the drawing context for a new frame then cause this scene controller to draw its content. There is no
		// need to explicitly swap the front and back buffers here, as the owner WorldWindow does this for us. In the
		// case of WorldWindowGLSurfaceView, the GLSurfaceView automatically swaps the front and back buffers for us.
		this.initializeDrawContext(this.dc, viewportWidth, viewportHeight);
		this.doDrawFrame(this.dc);
	}

	protected void doDrawFrame(DrawContext dc) {
		this.initializeFrame(dc);
		try {
			this.applyView(dc);
			this.createTerrain(dc);
			this.clearFrame(dc);
			this.pick(dc);
			this.clearFrame(dc);
			this.draw(dc);
		} finally {
			this.finalizeFrame(dc);
		}
	}

	protected void initializeDrawContext(DrawContext dc, int viewportWidth, int viewportHeight) {
		long timeStamp = System.currentTimeMillis();

		dc.initialize(viewportWidth, viewportHeight);
		dc.setModel(this.model);
		dc.setView(this.view);
		dc.setVerticalExaggeration(this.verticalExaggeration);
		dc.setGpuResourceCache(this.gpuResourceCache);
		dc.setFrameTimeStamp(timeStamp);
		dc.setPickPoint(this.pickPoint);
	}

	protected void initializeFrame(DrawContext dc) {
		GLES20.glEnable(GLES20.GL_BLEND);
		GLES20.glEnable(GLES20.GL_CULL_FACE);
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA); // Blend in premultiplied alpha mode.
		GLES20.glDepthFunc(GLES20.GL_LEQUAL);
		// We do not specify glCullFace, because the default cull face state GL_BACK is appropriate for our needs.
	}

	protected void finalizeFrame(DrawContext dc) {
		// Restore the default GL state values we modified in initializeFrame.
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_CULL_FACE);
		GLES20.glDisable(GLES20.GL_DEPTH_TEST);
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO);
		GLES20.glDepthFunc(GLES20.GL_LESS);
		GLES20.glClearColor(0f, 0f, 0f, 0f);
	}

	protected void clearFrame(DrawContext dc) {
		// Separate the DrawContext's background color components into the SceneController's clearColor.
		this.clearColor.set(dc.getClearColor());
		// Set the DrawContext's clear color, then clear the framebuffer's color buffer and depth buffer. This fills
		// the color buffer with the background color, and fills the depth buffer with 1 (the default).
		GLES20.glClearColor((float) this.clearColor.r, (float) this.clearColor.g, (float) this.clearColor.b, (float) this.clearColor.a);
		GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
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

	protected void draw(DrawContext dc) {
		this.drawLayers(dc);
		this.drawOrderedRenderables(dc);
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
				or.render(dc);
			} catch (Exception e) {
				String msg = Logging.getMessage("generic.ExceptionRenderingOrderedRenderable", or);
				Logging.error(msg, e);
				// Don't abort; continue on to the next ordered renderable.
			}
		}

		dc.setOrderedRenderingMode(false);
	}

	protected void pick(DrawContext dc) {
		try {
			this.beginPicking(dc);
			this.doPick(dc);
		} finally {
			this.endPicking(dc);
		}
	}

	/**
	 * Configures the draw context and GL state for picking. This ensures that pick colors are drawn into the
	 * framebuffer as specified, and are not modified by any GL state. This makes the following GL state changes:
	 * <ul>
	 * <li>Disable blending</li>
	 * <li>Disable dithering</li>
	 * </ul>
	 * 
	 * @param dc
	 *            the draw context to configure.
	 */
	protected void beginPicking(DrawContext dc) {
		dc.setPickingMode(true);
		GLES20.glDisable(GLES20.GL_BLEND); // Blending is disabled by default, but is enabled in initializeFrame.
		GLES20.glDisable(GLES20.GL_DITHER); // Dithering is enabled by default.
	}

	/**
	 * Restores the draw context and GL state modified in beginPicking. This makes the following GL state changes:
	 * <ul>
	 * <li>Enable blending</li>
	 * <li>Enable dithering</li>
	 * </ul>
	 * 
	 * @param dc
	 *            the draw context on which to restore state.
	 */
	protected void endPicking(DrawContext dc) {
		dc.setPickingMode(false);
		GLES20.glEnable(GLES20.GL_BLEND); // Blending is disabled by default, but is enabled in initializeFrame.
		GLES20.glEnable(GLES20.GL_DITHER); // Dithering is enabled by default.
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
}
