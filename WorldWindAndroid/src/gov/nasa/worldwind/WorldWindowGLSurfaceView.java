/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.cache.BasicGpuResourceCache;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.InputHandler;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.event.NoOpInputHandler;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.event.RenderingEvent;
import gov.nasa.worldwind.event.RenderingListener;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.PerformanceStatistic;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * @author dcollins
 * @version $Id: WorldWindowGLSurfaceView.java 831 2012-10-08 20:51:39Z tgaskins $
 */
public class WorldWindowGLSurfaceView extends GLSurfaceView implements GLSurfaceView.Renderer, WorldWindow, WWObject
{
    protected WWObjectImpl wwo = new WWObjectImpl(this);
    protected SceneController sceneController;
    protected InputHandler inputHandler;
    protected GpuResourceCache gpuResourceCache;
    protected List<RenderingListener> renderingListeners = new ArrayList<RenderingListener>();
	protected List<SelectListener> selectListeners = new ArrayList<SelectListener>();
	protected List<PositionListener> positionListeners = new ArrayList<PositionListener>();
    protected int viewportWidth;
    protected int viewportHeight;

    public WorldWindowGLSurfaceView(Context context)
    {
        super(context);

        try
        {
            this.init(null);
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("WorldWindow.UnableToCreateWorldWindow");
            Logging.error(msg);
            throw new WWRuntimeException(msg, e);
        }
    }

    public WorldWindowGLSurfaceView(Context context, AttributeSet attrs)
    {
        super(context, attrs);

        try
        {
            this.init(null);
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("WorldWindow.UnableToCreateWorldWindow");
            Logging.error(msg);
            throw new WWRuntimeException(msg, e);
        }
    }

    public WorldWindowGLSurfaceView(Context context, EGLConfigChooser configChooser)
    {
        super(context);

        try
        {
            this.init(configChooser);
        }
        catch (Exception e)
        {
            String msg = Logging.getMessage("WorldWindow.UnableToCreateWorldWindow");
            Logging.error(msg);
            throw new WWRuntimeException(msg, e);
        }
    }

    protected void init(EGLConfigChooser configChooser)
    {
        this.setEGLContextClientVersion(2); // Specify that this view requires an OpenGL ES 2.0 compatible context.

        if (configChooser != null)
            this.setEGLConfigChooser(configChooser);
        else
            this.setEGLConfigChooser(8, 8, 8, 8, 16, 0); // RGBA8888, 16-bit depth buffer, no stencil buffer.

        // Create the SceneController and assign its View before attaching it to this WorldWindow. We do this to avoid
        // receiving property change events from the SceneController before the superclass GLSurfaceView is properly
        // initialized.
        SceneController sc = this.createSceneController();
        if (sc != null)
            sc.setView(this.createView());
        this.setSceneController(sc);
        this.setInputHandler(this.createInputHandler());
        this.setGpuResourceCache(this.createGpuResourceCache());

        this.setRenderer(this);
        this.setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // Must be called after setRenderer.
    }

    protected SceneController createSceneController()
    {
        return (SceneController) WorldWind.createConfigurationComponent(AVKey.SCENE_CONTROLLER_CLASS_NAME);
    }

    protected InputHandler createInputHandler()
    {
        return (InputHandler) WorldWind.createConfigurationComponent(AVKey.INPUT_HANDLER_CLASS_NAME);
    }

    protected View createView()
    {
        return (View) WorldWind.createConfigurationComponent(AVKey.VIEW_CLASS_NAME);
    }

    protected GpuResourceCache createGpuResourceCache()
    {
        long size = Configuration.getLongValue(AVKey.GPU_RESOURCE_CACHE_SIZE);
        return new BasicGpuResourceCache((long) (0.8 * size), size);
    }

    /** {@inheritDoc} */
    public void onDrawFrame(GL10 glUnused)
    {
        // Ignore the passed-in GL10 interface, and use the GLES20 class's static methods instead.
        this.drawFrame();
    }

    /** {@inheritDoc} */
    public void onSurfaceChanged(GL10 glUnused, int width, int height)
    {
        // Ignore the passed-in GL10 interface, and use the GLES20 class's static methods instead.

        // Set the viewport each time the surface size changes. The SceneController and View automatically adapt to the
        // current viewport dimensions each frame.
        GLES20.glViewport(0, 0, width, height); 
		WorldWindowImpl.glCheckError("glViewport");
        this.viewportWidth = width;
        this.viewportHeight = height;
    }

    /** {@inheritDoc} */
    public void onSurfaceCreated(GL10 glUnused, EGLConfig config)
    {
        // Ignore the passed-in GL10 interface, and use the GLES20 class's static methods instead.

        // Clear the GPU resource cache each time the surface is created or recreated. This happens when the rendering
        // thread starts or when the EGL context is lost. All GPU object names are invalid, and must be recreated. Since
        // the EGL context has changed, the currently active context is not the one used to create the Gpu resources in
        // the cache. The cache is emptied and the GL silently ignores deletion of resource names that it does not
        // recognize.
        if (this.gpuResourceCache != null)
            this.gpuResourceCache.clear();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        // Let the InputHandler process the touch event first. If it returns true indicating that it handled the event,
        // then we suppress the default functionality.
        // noinspection SimplifiableIfStatement
        if (this.inputHandler != null && this.inputHandler.onTouch(this, event))
            return true;

        return super.onTouchEvent(event);
    }

    protected final RenderingEvent beforeRenderingEvent = new RenderingEvent(this, RenderingEvent.BEFORE_RENDERING);
    protected final RenderingEvent afterRenderingEvent = new RenderingEvent(this, RenderingEvent.AFTER_RENDERING);

    protected void drawFrame()
    {
        if (this.sceneController == null)
        {
            Logging.error(Logging.getMessage("WorldWindow.ScnCntrllerNullOnRepaint"));
            return;
        }

		Position positionAtStart = this.getCurrentPosition();
		PickedObject selectionAtStart = this.getCurrentSelection();
//		PickedObjectList boxSelectionAtStart = this.getCurrentBoxSelection();

        // Calls to rendering listeners are wrapped in a try/catch block to prevent any exception thrown by a listener
        // from terminating this frame.
        this.sendRenderingEvent(this.beforeRenderingEvent);

        try
        {
            this.sceneController.drawFrame(0, this.viewportWidth, this.viewportHeight);

        }
        catch (Exception e)
        {
            Logging.error(Logging.getMessage("WorldWindow.ExceptionDrawingWorldWindow"), e);
        }

        // Calls to rendering listeners are wrapped in a try/catch block to prevent any exception thrown by a listener
        // from terminating this frame.
        this.sendRenderingEvent(this.afterRenderingEvent);

		// Position and selection notification occurs only on triggering conditions, not same-state conditions:
		// start == null, end == null: nothing selected -- don't notify
		// start == null, end != null: something now selected -- notify
		// start != null, end == null: something was selected but no longer is -- notify
		// start != null, end != null, start != end: something new was selected -- notify
		// start != null, end != null, start == end: same thing is selected -- don't notify

		Position positionAtEnd = this.getCurrentPosition();
		if (positionAtStart != null || positionAtEnd != null)
		{
			// call the listener if both are not null or positions are the same
			if (positionAtStart != null && positionAtEnd != null)
			{
				if (!positionAtStart.equals(positionAtEnd))
					this.sendPositionEvent(new PositionEvent(this, sceneController.getPickPoint(),
							positionAtStart, positionAtEnd));
			}
			else
			{
				this.sendPositionEvent(new PositionEvent(this, sceneController.getPickPoint(),
						positionAtStart, positionAtEnd));
			}
		}

		PickedObject selectionAtEnd = this.getCurrentSelection();
		if (selectionAtStart != null || selectionAtEnd != null)
		{
			this.sendSelectEvent(new SelectEvent(this, SelectEvent.ROLLOVER,
					sceneController.getPickPoint(), sceneController.getObjectsAtPickPoint()));
		}

//		PickedObjectList boxSelectionAtEnd = this.getCurrentBoxSelection();
//		if (boxSelectionAtStart != null || boxSelectionAtEnd != null)
//		{
//			this.sendSelectEvent(new SelectEvent(this.drawable, SelectEvent.BOX_ROLLOVER,
//					sc.getPickRectangle(), sc.getObjectsInPickRectangle()));
//		}
    }

    /** {@inheritDoc} */
    public Model getModel()
    {
        return this.sceneController != null ? this.sceneController.getModel() : null;
    }

    /** {@inheritDoc} */
    public void setModel(Model model)
    {
        // model can be null, that's ok - it indicates no model.
        if (this.sceneController != null)
            this.sceneController.setModel(model);
    }

    /** {@inheritDoc} */
    public View getView()
    {
        return this.sceneController != null ? this.sceneController.getView() : null;
    }

    /** {@inheritDoc} */
    public void setView(View view)
    {
        // view can be null, that's ok - it indicates no view.
        if (this.sceneController != null)
            this.sceneController.setView(view);
    }

    public SceneController getSceneController()
    {
        return this.sceneController;
    }

    public void setSceneController(SceneController sceneController)
    {
        if (this.sceneController != null)
        {
            this.sceneController.removePropertyChangeListener(this);
            this.sceneController.setGpuResourceCache(null);
        }

        if (sceneController != null)
        {
            sceneController.addPropertyChangeListener(this);
            sceneController.setGpuResourceCache(this.gpuResourceCache);
        }

        this.sceneController = sceneController;
    }

    /** {@inheritDoc} */
    public InputHandler getInputHandler()
    {
        return this.inputHandler;
    }

    /** {@inheritDoc} */
    public void setInputHandler(InputHandler inputHandler)
    {
        if (this.inputHandler != null)
            this.inputHandler.setEventSource(null);

        // Fall back to a no-op input handler if the caller specifies null.
        this.inputHandler = inputHandler != null ? inputHandler : new NoOpInputHandler();

        // Configure this world window as the input handler's event source.
        this.inputHandler.setEventSource(this);
    }

	/** {@inheritDoc} */
    public GpuResourceCache getGpuResourceCache()
    {
        return this.gpuResourceCache;
    }

    public void setGpuResourceCache(GpuResourceCache cache)
    {
        this.gpuResourceCache = cache;

        if (this.sceneController != null)
            this.sceneController.setGpuResourceCache(cache);
    }

    /** {@inheritDoc} */
    public void addRenderingListener(RenderingListener listener)
    {
        if (listener == null)
        {
            String msg = Logging.getMessage("nullValue.ListenerIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.renderingListeners.add(listener);
    }

    /** {@inheritDoc} */
    public void removeRenderingListener(RenderingListener listener)
    {
        if (listener == null)
        {
            String msg = Logging.getMessage("nullValue.ListenerIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.renderingListeners.remove(listener);
    }

    protected void sendRenderingEvent(RenderingEvent event)
    {
        if (this.renderingListeners.isEmpty())
            return;

        for (int i = 0; i < this.renderingListeners.size(); i++)
        {
            RenderingListener listener = this.renderingListeners.get(i);
            try
            {
                // This method is called during rendering, so we wrao each rendering listener call in a try/catch block
                // to prevent exceptions thrown by rendering listeners from terminating the current frame. This also
                // ensures that an exception thrown by one listener does not prevent the others from receiving the
                // event.
                listener.stageChanged(event);
            }
            catch (Exception e)
            {
                Logging.error(Logging.getMessage("generic.ExceptionSendingEvent", event, listener), e);
            }
        }
    }

	public void addSelectListener(SelectListener listener)
	{
		if (listener == null)
		{
			String msg = Logging.getMessage("nullValue.ListenerIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.selectListeners.add(listener);
		this.getInputHandler().addSelectListener(listener);
	}

	public void removeSelectListener(SelectListener listener)
	{
		if (listener == null)
		{
			String msg = Logging.getMessage("nullValue.ListenerIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.selectListeners.remove(listener);
		this.getInputHandler().removeSelectListener(listener);
	}

	protected void sendSelectEvent(SelectEvent event)
	{
		for(SelectListener l : selectListeners) {
			l.selected(event);
		}
	}

	public void addPositionListener(PositionListener listener)
	{
		if (listener == null)
		{
			String msg = Logging.getMessage("nullValue.ListenerIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.positionListeners.add(listener);
	}

	public void removePositionListener(PositionListener listener)
	{
		if (listener == null)
		{
			String msg = Logging.getMessage("nullValue.ListenerIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}
		this.positionListeners.remove(listener);
	}

	protected void sendPositionEvent(PositionEvent event)
	{
		for(PositionListener l : positionListeners) {
			l.moved(event);
		}
	}

    /** {@inheritDoc} */
    public Position getCurrentPosition()
    {
        PickedObjectList pol = this.getObjectsAtCurrentPosition();
        if (pol == null || pol.isEmpty())
            return null;

        PickedObject po = pol.getTopPickedObject();
        if (po != null && po.hasPosition())
            return po.getPosition();

        po = pol.getTerrainObject();
        if (po != null)
            return po.getPosition();

        return null;
    }

    /** {@inheritDoc} */
    public PickedObjectList getObjectsAtCurrentPosition()
    {
        return this.sceneController != null ? this.sceneController.getObjectsAtPickPoint() : null;
    }

	protected PickedObject getCurrentSelection()
	{
		if (this.sceneController == null)
			return null;

		PickedObjectList pol = getObjectsAtCurrentPosition();
		if (pol == null || pol.size() < 1)
			return null;

		PickedObject top = pol.getTopPickedObject();
		return top.isTerrain() ? null : top;
	}

//	protected PickedObjectList getCurrentBoxSelection()
//	{
//		if (this.sceneController == null)
//			return null;
//
//		PickedObjectList pol = this.sceneController.getObjectsInPickRectangle();
//		return pol != null && pol.size() > 0 ? pol : null;
//	}

    /** {@inheritDoc} */
    public void redraw()
    {
        this.requestRender();
    }

    /** {@inheritDoc} */
    public void invokeInRenderingThread(Runnable runnable)
    {
        this.queueEvent(runnable);
    }

    /** {@inheritDoc} */
    public Object getValue(String key)
    {
        return this.wwo.getValue(key);
    }

    /** {@inheritDoc} */
    public String getStringValue(String key)
    {
        return this.wwo.getStringValue(key);
    }

    /** {@inheritDoc} */
    public Object setValue(String key, Object value)
    {
        return this.wwo.setValue(key, value);
    }

    /** {@inheritDoc} */
    public Collection<Object> getValues()
    {
        return this.wwo.getValues();
    }

    /** {@inheritDoc} */
    public AVList setValues(AVList list)
    {
        return this.wwo.setValues(list);
    }

    /** {@inheritDoc} */
    public Set<Map.Entry<String, Object>> getEntries()
    {
        return this.wwo.getEntries();
    }

    /** {@inheritDoc} */
    public boolean hasKey(String key)
    {
        return this.wwo.hasKey(key);
    }

    /** {@inheritDoc} */
    public Object removeKey(String key)
    {
        return this.wwo.removeKey(key);
    }

    /** {@inheritDoc} */
    public AVList copy()
    {
        return this.wwo.copy();
    }

    /** {@inheritDoc} */
    public AVList clearList()
    {
        return this.wwo.clearList();
    }

    /** {@inheritDoc} */
    public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        this.wwo.addPropertyChangeListener(propertyName, listener);
    }

    /** {@inheritDoc} */
    public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener)
    {
        this.wwo.removePropertyChangeListener(propertyName, listener);
    }

    /** {@inheritDoc} */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        this.wwo.addPropertyChangeListener(listener);
    }

    /** {@inheritDoc} */
    public void removePropertyChangeListener(PropertyChangeListener listener)
    {
        this.wwo.removePropertyChangeListener(listener);
    }

    /** {@inheritDoc} */
    public void firePropertyChange(String propertyName, Object oldValue, Object newValue)
    {
        this.wwo.firePropertyChange(propertyName, oldValue, newValue);
    }

    /** {@inheritDoc} */
    public void firePropertyChange(PropertyChangeEvent event)
    {
        this.wwo.firePropertyChange(event);
    }

    /** {@inheritDoc} */
    public void propertyChange(PropertyChangeEvent event)
    {
        this.wwo.propertyChange(event);

        // Redraw this WorldWindow when we receive a property change event from the SceneController or the application.
        this.redraw();
    }

    /** Empty implementation of MessageListener. */
    public void onMessage(Message message)
    {
        // Empty implementation
    }

	public void setPerFrameStatisticsKeys(Set<String> keys)
	{
		if (this.sceneController != null)
			this.sceneController.setPerFrameStatisticsKeys(keys);
	}

	public Map<String, PerformanceStatistic> getPerFrameStatistics()
	{
		if (this.sceneController == null || this.sceneController.getPerFrameStatistics() == null)
			return new HashMap<String, PerformanceStatistic>(0);

		return this.sceneController.getPerFrameStatistics();
	}

	@Override
	public void onSurfaceDestroyed() {

	}
}
