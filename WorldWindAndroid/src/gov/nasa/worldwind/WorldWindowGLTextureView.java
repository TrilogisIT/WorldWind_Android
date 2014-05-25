/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.GLTextureView;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.PerformanceStatistic;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * @author dcollins
 * @version $Id: WorldWindowGLSurfaceView.java 831 2012-10-08 20:51:39Z tgaskins $
 */
public class WorldWindowGLTextureView extends GLTextureView implements WorldWindow
{
    private WorldWindowImpl wwo;

	public WorldWindowGLTextureView(Context context)
	{
		this(context, null, null);
	}

	public WorldWindowGLTextureView(Context context, AttributeSet attrs)
	{
		this(context, attrs, null);
	}

	public WorldWindowGLTextureView(Context context, AttributeSet attrs, EGLConfigChooser configChooser)
	{
		super(context, attrs);

		try
		{
			this.setEGLContextClientVersion(2); // Specify that this view requires an OpenGL ES 2.0 compatible context.

			if (configChooser != null)
				this.setEGLConfigChooser(configChooser);
			else
				this.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // RGBA8888, 16-bit depth buffer, no stencil buffer.

			wwo = new WorldWindowImpl(getContext(), this);
			setOnTouchListener(wwo);
			this.setRenderer(wwo);
			this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // Must be called after setRenderer.
		}
		catch (Exception e)
		{
			String msg = Logging.getMessage("WorldWindow.UnableToCreateWorldWindow");
			Logging.error(msg);
			throw new WWRuntimeException(msg, e);
		}
	}

	public void setFrameRate(double frameRate) {
		wwo.setFrameRate(frameRate);
	}

	public PickedObject getCurrentSelection() {
		return wwo.getCurrentSelection();
	}

	public GpuResourceCache createGpuResourceCache() {
		return wwo.createGpuResourceCache();
	}

	public double getFrameRate() {
		return wwo.getFrameRate();
	}

	public void setFrameRate(int frameRate) {
		wwo.setFrameRate(frameRate);
	}

	public InputHandler createInputHandler() {
		return wwo.createInputHandler();
	}

	public View createView() {
		return wwo.createView();
	}

	public SceneController createSceneController() {
		return wwo.createSceneController();
	}

	public void setGpuResourceCache(GpuResourceCache cache) {
		wwo.setGpuResourceCache(cache);
	}

	public static Long getLongValue(AVList avList, String key) {
		return AVListImpl.getLongValue(avList, key);
	}

	@Override
	public void firePropertyChange(PropertyChangeEvent event) {
		wwo.firePropertyChange(event);
	}

	@Override
	public void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		wwo.firePropertyChange(propertyName, oldValue, newValue);
	}

	@Override
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		wwo.removePropertyChangeListener(listener);
	}

	@Override
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		wwo.addPropertyChangeListener(listener);
	}

	@Override
	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		wwo.removePropertyChangeListener(propertyName, listener);
	}

	@Override
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		wwo.addPropertyChangeListener(propertyName, listener);
	}

	@Override
	public AVList clearList() {
		return wwo.clearList();
	}

	@Override
	public AVList copy() {
		return wwo.copy();
	}

	@Override
	public Object removeKey(String key) {
		return wwo.removeKey(key);
	}

	@Override
	public boolean hasKey(String key) {
		return wwo.hasKey(key);
	}

	@Override
	public Set<Map.Entry<String, Object>> getEntries() {
		return wwo.getEntries();
	}

	@Override
	public AVList setValues(AVList list) {
		return wwo.setValues(list);
	}

	@Override
	public Collection<Object> getValues() {
		return wwo.getValues();
	}

	@Override
	public Object setValue(String key, Object value) {
		return wwo.setValue(key, value);
	}

	@Override
	public String getStringValue(String key) {
		return wwo.getStringValue(key);
	}

	@Override
	public Object getValue(String key) {
		return wwo.getValue(key);
	}

	@Override
	public void onMessage(Message message) {
		wwo.onMessage(message);
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		wwo.propertyChange(event);
	}

	@Override
	public Map<String, PerformanceStatistic> getPerFrameStatistics() {
		return wwo.getPerFrameStatistics();
	}

	@Override
	public void setPerFrameStatisticsKeys(Set<String> keys) {
		wwo.setPerFrameStatisticsKeys(keys);
	}

	@Override
	public void redraw() {
		wwo.redraw();
	}

	@Override
	public void removePositionListener(PositionListener listener) {
		wwo.removePositionListener(listener);
	}

	@Override
	public void addPositionListener(PositionListener listener) {
		wwo.addPositionListener(listener);
	}

	@Override
	public PickedObjectList getObjectsAtCurrentPosition() {
		return wwo.getObjectsAtCurrentPosition();
	}

	@Override
	public Position getCurrentPosition() {
		return wwo.getCurrentPosition();
	}

	@Override
	public void removeSelectListener(SelectListener listener) {
		wwo.removeSelectListener(listener);
	}

	@Override
	public void addSelectListener(SelectListener listener) {
		wwo.addSelectListener(listener);
	}

	@Override
	public void removeRenderingListener(RenderingListener listener) {
		wwo.removeRenderingListener(listener);
	}

	@Override
	public void addRenderingListener(RenderingListener listener) {
		wwo.addRenderingListener(listener);
	}

	@Override
	public Model getModel() {
		return wwo.getModel();
	}

	@Override
	public void setModel(Model model) {
		wwo.setModel(model);
	}

	@Override
	public View getView() {
		return wwo.getView();
	}

	@Override
	public void setView(View view) {
		wwo.setView(view);
	}

	@Override
	public SceneController getSceneController() {
		return wwo.getSceneController();
	}

	@Override
	public void setSceneController(SceneController sceneController) {
		wwo.setSceneController(sceneController);
	}

	@Override
	public InputHandler getInputHandler() {
		return wwo.getInputHandler();
	}

	@Override
	public void setInputHandler(InputHandler inputHandler) {
		wwo.setInputHandler(inputHandler);
	}

	@Override
	public GpuResourceCache getGpuResourceCache() {
		return wwo.getGpuResourceCache();
	}

	@Override
	public void onResume() {
		super.onResume();
		wwo.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		wwo.onPause();
	}

	@Override
	public void onSurfaceDestroyed() {
		wwo.onSurfaceDestroyed();
	}

	@Override
	public void invokeInRenderingThread(Runnable runnable) {
		wwo.invokeInRenderingThread(runnable);
	}
}
