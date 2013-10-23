/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.ogc.OGCCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.WWXML;
import gov.nasa.worldwind.util.xml.XMLParserException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * A basic implementation of the {@link Factory} interface.
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: BasicFactory.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class BasicFactory implements Factory {
	public BasicFactory() {
	}

	/**
	 * Static method to create an object from a factory and configuration source.
	 * 
	 * @param key
	 *            the key identifying the factory in {@link Configuration}.
	 * @param configSource
	 *            the configuration source. May be any of the types listed for {@link #createFromConfigSource(Object)}
	 * @return a new instance of the requested object.
	 * @throws IllegalArgumentException
	 *             if the factory key is null, or if the configuration source is null or an empty
	 *             string.
	 */
	public static Object create(String key, Object configSource) {
		if (WWUtil.isEmpty(key)) {
			String msg = Logging.getMessage("nullValue.KeyIsNull");
			throw new IllegalArgumentException(msg);
		}

		if (WWUtil.isEmpty(configSource)) {
			String msg = Logging.getMessage("nullValue.SourceIsNull");
			throw new IllegalArgumentException(msg);
		}

		Factory factory = (Factory) WorldWind.createConfigurationComponent(key);
		return factory.createFromConfigSource(configSource);
	}

	/**
	 * Creates an object from a general configuration source. The source can be one of the following:
	 * <ul>
	 * <li>{@link java.net.URL}</li>
	 * <li>{@link java.io.File}</li>
	 * <li>{@link java.io.InputStream}</li>
	 * <li>{@link org.w3c.dom.Element}</li>
	 * <li>{@link String} holding a file name, a name of a resource on the classpath, or a string representation of a URL</li>
	 * </ul>
	 * <p/>
	 * 
	 * @param configSource
	 *            the configuration source. See above for supported types.
	 * @return the new object.
	 * @throws IllegalArgumentException
	 *             if the configuration source is null or an empty string.
	 * @throws gov.nasa.worldwind.exception.WWUnrecognizedException
	 *             if the source type is unrecognized.
	 * @throws gov.nasa.worldwind.exception.WWRuntimeException
	 *             if object creation fails. The exception indicating the source of the failure is
	 *             included as the {@link Exception#initCause(Throwable)}.
	 */
	public Object createFromConfigSource(Object configSource) {
		return createFromConfigSource(configSource, null);
	}

	public Object createFromConfigSource(Object configSource, AVList params) {
		if (WWUtil.isEmpty(configSource)) {
			String msg = Logging.getMessage("nullValue.SourceIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Object o = null;

		try {
			if (configSource instanceof Element) {
				o = this.doCreateFromElement((Element) configSource);
			} else if (configSource instanceof OGCCapabilities) {
				o = this.doCreateFromCapabilities((OGCCapabilities) configSource, params);
			} else {
				Document doc = WWXML.openDocument(configSource);
				if (doc != null) o = this.doCreateFromElement(doc.getDocumentElement());
			}
		} catch (Exception e) {
			String msg = Logging.getMessage("generic.CreationFromConfigFileFailed", configSource);
			throw new WWRuntimeException(msg, e);
		}

		return o;
	}

	/**
	 * Create an object such as a layer or elevation model given a local OGC capabilities document containing named
	 * layer descriptions.
	 * 
	 * @param capsFileName
	 *            the path to the capabilities file. The file must be either an absolute path or a relative
	 *            path available on the classpath. The file contents must be a valid OGC capabilities
	 *            document.
	 * @param params
	 *            a list of configuration properties. These properties override any specified in the
	 *            capabilities document. The list should contain the {@link AVKey#LAYER_NAMES} property for
	 *            services that define layer, indicating which named layers described in the capabilities
	 *            document to create. If this argumet is null or contains no layers, the first named layer is
	 *            used.
	 * @return the requested object.
	 * @throws IllegalArgumentException
	 *             if the file name is null or empty.
	 * @throws IllegalStateException
	 *             if the capabilites document contains no named layer definitions.
	 * @throws WWRuntimeException
	 *             if an error occurs while opening, reading or parsing the capabilities document.
	 *             The exception indicating the source of the failure is included as the {@link Exception#initCause(Throwable)}.
	 */
	public Object createFromCapabilities(String capsFileName, AVList params) {
		if (WWUtil.isEmpty(capsFileName)) {
			String message = Logging.getMessage("nullValue.FilePathIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		WMSCapabilities caps = new WMSCapabilities(capsFileName);

		try {
			caps.parse();
		} catch (XMLParserException e) {
			String message = Logging.getMessage("generic.CannotParseCapabilities", capsFileName);
			Logging.error(message, e);
			throw new WWRuntimeException(message, e);
		}

		return this.doCreateFromCapabilities(caps, params);
	}

	/**
	 * Implemented by subclasses to perform the actual object creation. This default implementation always returns
	 * null.
	 * 
	 * @param caps
	 *            the capabilities document.
	 * @param params
	 *            a list of configuration properties. These properties override any specified in the capabilities
	 *            document. The list should contain the {@link AVKey#LAYER_NAMES} property for services that define
	 *            layers, indicating which named layers described in the capabilities document to create. If this
	 *            argumet is null or contains no layers, the first named layer is used.
	 * @return the requested object.
	 */
	protected Object doCreateFromCapabilities(OGCCapabilities caps, AVList params) {
		return null;
	}

	protected Object doCreateFromElement(Element domElement) throws Exception {
		return null;
	}
}
