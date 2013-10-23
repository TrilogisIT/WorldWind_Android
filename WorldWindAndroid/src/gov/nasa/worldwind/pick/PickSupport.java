/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.pick;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.DrawContext;
import java.util.HashMap;
import java.util.Map;
import android.graphics.Point;
import android.opengl.GLES20;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author tag
 * @version $Id: PickSupport.java 805 2012-09-26 01:47:35Z dcollins $
 */
public class PickSupport {
	protected Map<Integer, PickedObject> pickableObjects = new HashMap<Integer, PickedObject>();

	public PickSupport() {
	}

	public void addPickableObject(PickedObject po) {
		this.getPickableObjects().put(po.getColorCode(), po);
	}

	public void addPickableObject(int colorCode, Object o) {
		this.getPickableObjects().put(colorCode, new PickedObject(colorCode, o));
	}

	public void addPickableObject(int colorCode, Object o, Position position) {
		this.getPickableObjects().put(colorCode, new PickedObject(colorCode, o, position, false));
	}

	public void addPickableObject(int colorCode, Object o, Position position, boolean isTerrain) {
		this.getPickableObjects().put(colorCode, new PickedObject(colorCode, o, position, isTerrain));
	}

	public void clearPickList() {
		this.getPickableObjects().clear();
	}

	protected Map<Integer, PickedObject> getPickableObjects() {
		return this.pickableObjects;
	}

	public PickedObject getTopObject(DrawContext dc, Point pickPoint) {
		if (this.getPickableObjects().isEmpty()) return null;

		int colorCode = dc.getPickColor(pickPoint);
		if (colorCode == 0) // getPickColor returns 0 if the pick point selects the clear color.
		return null;

		PickedObject pickedObject = getPickableObjects().get(colorCode);
		if (pickedObject == null) return null;

		return pickedObject;
	}

	public PickedObject resolvePick(DrawContext dc, Point pickPoint, Layer layer) {
		PickedObject pickedObject = this.getTopObject(dc, pickPoint);
		if (pickedObject != null) {
			if (layer != null) pickedObject.setParentLayer(layer);

			dc.addPickedObject(pickedObject);
		}

		this.clearPickList();

		return pickedObject;
	}

	// ARE THESE NECESSARY???
	public void beginPicking(DrawContext dc) {

		// gl.glPushAttrib(GL.GL_ENABLE_BIT | GL.GL_CURRENT_BIT);

		GLES20.glDisable(GLES20.GL_DITHER);
		// GLES20.glDisable(GLES20.GL_GL_LIGHTING);
		// GLES20.glDisable(GLES20.GL_FOG);
		GLES20.glDisable(GLES20.GL_BLEND);
		GLES20.glDisable(GLES20.GL_TEXTURE_2D);

		if (dc.isDeepPickingEnabled()) GLES20.glDisable(GLES20.GL_DEPTH_TEST);
	}

	public void endPicking(DrawContext dc) {
		// dc.restoreDefaultBlending();
		GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ZERO);
		GLES20.glDisable(GLES20.GL_BLEND);
		// dc.restoreDefaultDepthTesting();
		GLES20.glEnable(GLES20.GL_DEPTH_TEST);
		GLES20.glDepthMask(true);
		// ??dc.restoreDefaultCurrentColor();
	}
}
