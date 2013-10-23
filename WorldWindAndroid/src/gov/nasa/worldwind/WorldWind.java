/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.cache.MemoryCache;
import gov.nasa.worldwind.cache.MemoryCacheSet;
import gov.nasa.worldwind.cache.SessionCache;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.retrieve.RetrievalService;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.NetworkStatus;
import gov.nasa.worldwind.util.TaskService;
import gov.nasa.worldwind.util.WWUtil;
import java.beans.PropertyChangeListener;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: WorldWind.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class WorldWind {
	public static final String SHUTDOWN_EVENT = "gov.nasa.worldwind.ShutDown";

	protected static WorldWind instance = new WorldWind();

	private WWObjectImpl wwo;
	protected MemoryCacheSet memoryCacheSet;
	protected RetrievalService remoteRetrievalService;
	protected RetrievalService localRetrievalService;
	protected NetworkStatus networkStatus;
	protected FileStore dataFileStore;
	protected TaskService taskService;
	protected SessionCache sessionCache;

	// Singleton, prevent public instantiation.
	protected WorldWind() {
		this.initialize();
	}

	protected void initialize() {
		this.wwo = new WWObjectImpl();
		this.remoteRetrievalService = (RetrievalService) createConfigurationComponent(AVKey.RETRIEVAL_SERVICE_CLASS_NAME);
		this.localRetrievalService = (RetrievalService) createConfigurationComponent(AVKey.RETRIEVAL_SERVICE_CLASS_NAME);

		this.dataFileStore = (FileStore) createConfigurationComponent(AVKey.DATA_FILE_STORE_CLASS_NAME);
		this.memoryCacheSet = (MemoryCacheSet) createConfigurationComponent(AVKey.MEMORY_CACHE_SET_CLASS_NAME);
		this.networkStatus = (NetworkStatus) createConfigurationComponent(AVKey.NETWORK_STATUS_CLASS_NAME);
		this.sessionCache = (SessionCache) createConfigurationComponent(AVKey.SESSION_CACHE_CLASS_NAME);
		this.taskService = (TaskService) createConfigurationComponent(AVKey.TASK_SERVICE_CLASS_NAME);
	}

	public static RetrievalService getRetrievalService() {
		return instance.remoteRetrievalService;
	}

	public static RetrievalService getLocalRetrievalService() {
		return instance.remoteRetrievalService;
	}

	public static RetrievalService getRemoteRetrievalService() {
		return getRetrievalService();
	}

	public static MemoryCacheSet getMemoryCacheSet() {
		return instance.memoryCacheSet;
	}

	public static MemoryCache getMemoryCache(String key) {
		return instance.memoryCacheSet.get(key);
	}

	public static NetworkStatus getNetworkStatus() {
		return instance.networkStatus;
	}

	public static SessionCache getSessionCache() {
		return instance.sessionCache;
	}

	public static TaskService getTaskService() {
		return instance.taskService;
	}

	public static FileStore getDataFileStore() {
		return instance.dataFileStore;
	}

	/**
	 * @param className
	 *            the full name, including package names, of the component to create
	 * @return the new component
	 * @throws gov.nasa.worldwind.exception.WWRuntimeException
	 *             if the <code>Object</code> could not be created
	 * @throws IllegalArgumentException
	 *             if <code>className</code> is null or zero length
	 */
	public static Object createComponent(String className) {
		if (WWUtil.isEmpty(className)) {
			String msg = Logging.getMessage("nullValue.ClassNameIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		try {
			Class<?> c = Class.forName(className.trim());
			return c.newInstance();
		} catch (Throwable t) {
			String msg = Logging.getMessage("WorldWind.UnableToCreateClass", className);
			Logging.error(msg, t);
			throw new WWRuntimeException(msg, t);
		}
	}

	/**
	 * @param classNameKey
	 *            the key identifying the component
	 * @return the new component
	 * @throws IllegalStateException
	 *             if no name could be found which corresponds to <code>classNameKey</code>
	 * @throws IllegalArgumentException
	 *             if <code>classNameKey<code> is null
	 * @throws WWRuntimeException
	 *             if the component could not be created
	 */
	public static Object createConfigurationComponent(String classNameKey) {
		if (WWUtil.isEmpty(classNameKey)) {
			String msg = Logging.getMessage("nullValue.ClassNameKeyIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		String name = Configuration.getStringValue(classNameKey);
		if (name == null) {
			String msg = Logging.getMessage("WorldWind.NoClassNameInConfigurationForKey", classNameKey);
			Logging.error(msg);
			throw new WWRuntimeException(msg);
		}

		return WorldWind.createComponent(name);
	}

	public static void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		instance.wwo.addPropertyChangeListener(propertyName, listener);
	}

	public static void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		instance.wwo.removePropertyChangeListener(propertyName, listener);
	}
}
