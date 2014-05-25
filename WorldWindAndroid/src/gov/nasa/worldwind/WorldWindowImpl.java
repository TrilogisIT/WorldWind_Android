/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind;

import android.content.Context;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.os.SystemClock;
import android.view.MotionEvent;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.BasicGpuResourceCache;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.event.*;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;
import gov.nasa.worldwind.util.GLTextureView;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.PerformanceStatistic;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * WorldWindow implementation.  Manages rendering thread for automatic rendering.
 *
 * Created by kedzie on 4/23/14.
 */
public class WorldWindowImpl extends WWObjectImpl implements WorldWindow, GLSurfaceView.Renderer, android.view.View.OnTouchListener {

	public static boolean DEBUG = true;

	protected ScheduledExecutorService mTimer; //Timer used to schedule drawing

	private WeakReference<Context> mContext;
	protected SceneController sceneController;
	protected InputHandler inputHandler;
	protected GpuResourceCache gpuResourceCache;
	protected List<RenderingListener> renderingListeners = new ArrayList<RenderingListener>();
	protected List<SelectListener> selectListeners = new ArrayList<SelectListener>();
	protected List<PositionListener> positionListeners = new ArrayList<PositionListener>();
	protected int viewportWidth;
	protected int viewportHeight;

	protected double mFrameRate; //Target frame rate to render at
	protected int mFrameCount; //Used for determining FPS
	private long mStartTime = System.nanoTime(); //Used for determining FPS
	protected double mLastMeasuredFPS; //Last measured FPS value
	protected long mLastMeasuredFrameTime;
	private long mLastRender; //Time of last rendering. Used for animation delta time
	protected int mLastReportedGLError = 0; // Keep track of the last reported OpenGL error

	private GLTextureView textureView;

	public WorldWindowImpl(Context context, GLTextureView view) {
		mContext = new WeakReference<Context>(context);
		this.textureView = view;

		// Create the SceneController and assign its View before attaching it to this WorldWindow. We do this to avoid
		// receiving property change events from the SceneController before the superclass GLSurfaceView is properly
		// initialized.
		SceneController sc = this.createSceneController();
		if (sc != null) {
			sc.setView(this.createView());
		}
		this.setSceneController(sc);
		this.setInputHandler(this.createInputHandler());
		this.setGpuResourceCache(this.createGpuResourceCache());
	}

	@Override
	public Context getContext() {
		return mContext.get();
	}

	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig config) {
		// Clear the GPU resource cache each time the surface is created or recreated. This happens when the rendering
		// thread starts or when the EGL context is lost. All GPU object names are invalid, and must be recreated. Since
		// the EGL context has changed, the currently active context is not the one used to create the Gpu resources in
		// the cache. The cache is emptied and the GL silently ignores deletion of resource names that it does not
		// recognize.
		if (this.gpuResourceCache != null)
			this.gpuResourceCache.clear();
	}

	@Override
	public void onSurfaceDestroyed() {
		stopRendering();
	}

	@Override
	public void onPause() {
		stopRendering();
	}

	@Override
	public void onResume() {
		startRendering();
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		GLES20.glViewport(0, 0, width, height);
		glCheckError("glViewport");
		this.viewportWidth = width;
		this.viewportHeight = height;
		startRendering();
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		final double deltaTime = (SystemClock.elapsedRealtime() - mLastRender) / 1000d;
		mLastRender = SystemClock.elapsedRealtime();

		drawFrame(deltaTime);

		++mFrameCount;
		if (mFrameCount % 50 == 0) {
			long now = System.nanoTime();
			double elapsedS = (now - mStartTime) / 1.0e9;
			double msPerFrame = (1000 * elapsedS / mFrameCount);
			mLastMeasuredFPS = 1000 / msPerFrame;

			mFrameCount = 0;
			mStartTime = now;

			getPerFrameStatistics().put(PerformanceStatistic.FRAME_RATE, new PerformanceStatistic(PerformanceStatistic.FRAME_RATE, "Frame Rate (fps)", (int) this.mLastMeasuredFPS));
			firePropertyChange(PerformanceStatistic.FRAME_RATE, 0, mLastMeasuredFPS);
		}

		getPerFrameStatistics().put(PerformanceStatistic.FRAME_TIME, new PerformanceStatistic(PerformanceStatistic.FRAME_TIME, "Frame Time (ms)", (int) this.mLastMeasuredFrameTime));

		int error = GLES20.glGetError();

		if(error > 0)
		{
			if(error != mLastReportedGLError)
			{
				mLastReportedGLError = error;
				throw new RuntimeException("OpenGL Error: " + GLU.gluErrorString(error) + " " + error);
			}
		} else {
			mLastReportedGLError = 0;
		}
	}

	protected void drawFrame(double deltaTime)
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
			this.sceneController.drawFrame(deltaTime, this.viewportWidth, this.viewportHeight);

			this.setValue(PerformanceStatistic.FRAME_TIME, mLastMeasuredFrameTime);
			this.setValue(PerformanceStatistic.FRAME_RATE, mLastMeasuredFPS);
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

	public static void glCheckError(String op) {
		if(!DEBUG) return;
		int error;
		while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
			final StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
			StringBuilder sb = new StringBuilder(stackTrace.length*80);
			for(StackTraceElement element : stackTrace) {
				sb.append("\n").append(element.toString());
			}
			Logging.error(op + ": glError " + GLUtils.getEGLErrorString(error) + sb.toString());
//			throw new RuntimeException(op + ": glError " + GLUtils.getEGLErrorString(error));
		}
	}

	protected final RenderingEvent beforeRenderingEvent = new RenderingEvent(this, RenderingEvent.BEFORE_RENDERING);
	protected final RenderingEvent afterRenderingEvent = new RenderingEvent(this, RenderingEvent.AFTER_RENDERING);

	@Override
	public Model getModel()
	{
		return this.sceneController != null ? this.sceneController.getModel() : null;
	}

	@Override
	public void setModel(Model model)
	{
		// model can be null, that's ok - it indicates no model.
		if (this.sceneController != null)
			this.sceneController.setModel(model);
	}

	@Override
	public View getView()
	{
		return this.sceneController != null ? this.sceneController.getView() : null;
	}

	@Override
	public void setView(View view)
	{
		// view can be null, that's ok - it indicates no view.
		if (this.sceneController != null)
			this.sceneController.setView(view);
	}

	@Override
	public SceneController getSceneController()
	{
		return this.sceneController;
	}

	@Override
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

	@Override
	public InputHandler getInputHandler()
	{
		return this.inputHandler;
	}

	@Override
	public void setInputHandler(InputHandler inputHandler)
	{
		if (this.inputHandler != null)
			this.inputHandler.setEventSource(null);

		// Fall back to a no-op input handler if the caller specifies null.
		this.inputHandler = inputHandler != null ? inputHandler : new NoOpInputHandler();

		// Configure this world window as the input handler's event source.
		this.inputHandler.setEventSource(this);
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
	public boolean onTouch(android.view.View v, MotionEvent event) {
		// Let the InputHandler process the touch event first. If it returns true indicating that it handled the event,
		// then we suppress the default functionality.
		// noinspection SimplifiableIfStatement
		if (this.inputHandler != null && this.inputHandler.onTouch(v, event))
			return true;
		return false;
	}

//	protected PickedObjectList getCurrentBoxSelection()
//	{
//		if (this.sceneController == null)
//			return null;
//
//		PickedObjectList pol = this.sceneController.getObjectsInPickRectangle();
//		return pol != null && pol.size() > 0 ? pol : null;
//	}

	private class RequestRenderTask implements Runnable {
		public void run() {
			if (textureView != null) {
				textureView.requestRender();
			}
		}
	}

	public void startRendering() {
		mLastRender = SystemClock.elapsedRealtime();

		if (mTimer!=null || mFrameRate==0) {return;}

		mTimer = Executors.newScheduledThreadPool(1);
		mTimer.scheduleAtFixedRate(new RequestRenderTask(), 0, (long) (1000 / mFrameRate), TimeUnit.MILLISECONDS);
	}

	/**
	 * Stop rendering the scene.
	 *
	 * @return true if rendering was stopped, false if rendering was already
	 *         stopped (no action taken)
	 */
	protected boolean stopRendering() {
		if (mTimer != null) {
			mTimer.shutdownNow();
			mTimer = null;
			return true;
		}
		return false;
	}

	public double getFrameRate() {
		return mFrameRate;
	}

	public void setFrameRate(double frameRate) {
		this.mFrameRate = frameRate;
		if (stopRendering()) {
			// Restart timer with new frequency
			startRendering();
		}
	}

	@Override
	public void redraw()
	{
		if(mFrameRate==0)
			textureView.requestRender();
	}

	public void setFrameRate(int frameRate) {
		setFrameRate((double) frameRate);
	}

	@Override
	public void invokeInRenderingThread(Runnable runnable)
	{
		textureView.queueEvent(runnable);
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

	@Override
	public void setPerFrameStatisticsKeys(Set<String> keys)
	{
		if (this.sceneController != null)
			this.sceneController.setPerFrameStatisticsKeys(keys);
	}

	@Override
	public Map<String, PerformanceStatistic> getPerFrameStatistics()
	{
		if (this.sceneController == null || this.sceneController.getPerFrameStatistics() == null)
			return new HashMap<String, PerformanceStatistic>(0);

		return this.sceneController.getPerFrameStatistics();
	}
}
