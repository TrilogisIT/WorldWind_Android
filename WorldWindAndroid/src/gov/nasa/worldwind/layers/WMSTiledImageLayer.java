/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.layers;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.exception.WWRuntimeException;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.util.DataConfigurationUtils;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.Tile;
import gov.nasa.worldwind.util.TileUrlBuilder;
import gov.nasa.worldwind.util.WWXML;
import java.net.MalformedURLException;
import java.net.URL;
import org.w3c.dom.Element;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author pabercrombie
 * @version $Id: WMSTiledImageLayer.java 758 2012-09-06 20:11:41Z tgaskins $
 */
public class WMSTiledImageLayer extends TiledImageLayer {
	private static final String[] formatOrderPreference = new String[] { "image/png", "image/jpeg" };

	public WMSTiledImageLayer(AVList params) {
		super(params);
	}

	public WMSTiledImageLayer(Element domElement, AVList params) {
		super(wmsGetParamsFromDocument(domElement, params));
	}

	public WMSTiledImageLayer(WMSCapabilities caps, AVList params) {
		this(wmsGetParamsFromCapsDoc(caps, params));
	}

	/**
	 * Extracts parameters necessary to configure the layer from an XML DOM element.
	 * 
	 * @param domElement
	 *            the element to search for parameters.
	 * @param params
	 *            an attribute-value list in which to place the extracted parameters. May be null, in which case
	 *            a new attribue-value list is created and returned.
	 * @return the attribute-value list passed as the second parameter, or the list created if the second parameter is
	 *         null.
	 * @throws IllegalArgumentException
	 *             if the DOM element is null.
	 */
	protected static AVList wmsGetParamsFromDocument(Element domElement, AVList params) {
		if (domElement == null) {
			String message = Logging.getMessage("nullValue.DocumentIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (params == null) params = new AVListImpl();

		DataConfigurationUtils.getWMSLayerConfigParams(domElement, params);
		TiledImageLayer.getParamsFromDocument(domElement, params);

		params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(params));

		return params;
	}

	/**
	 * Extracts parameters necessary to configure the layer from a WMS capabilities document.
	 * 
	 * @param caps
	 *            the capabilities document.
	 * @param params
	 *            an attribute-value list in which to place the extracted parameters. May be null, in which case a
	 *            new attribute-value list is created and returned.
	 * @return the attribute-value list passed as the second parameter, or the list created if the second parameter is
	 *         null.
	 * @throws IllegalArgumentException
	 *             if the capabilities document reference is null.
	 */
	public static AVList wmsGetParamsFromCapsDoc(WMSCapabilities caps, AVList params) {
		if (caps == null) {
			String message = Logging.getMessage("nullValue.WMSCapabilities");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (params == null) params = new AVListImpl();

		try {
			DataConfigurationUtils.getWMSLayerConfigParams(caps, formatOrderPreference, params);
		} catch (IllegalArgumentException e) {
			String message = Logging.getMessage("WMS.MissingLayerParameters");
			Logging.error(message, e);
			throw new IllegalArgumentException(message, e);
		} catch (WWRuntimeException e) {
			String message = Logging.getMessage("WMS.MissingCapabilityValues");
			Logging.error(message, e);
			throw new IllegalArgumentException(message, e);
		}

		setFallbacks(params);

		// Setup WMS URL builder.
		params.setValue(AVKey.WMS_VERSION, caps.getVersion());
		params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(params));
		// Setup default WMS tiled image layer behaviors.
		params.setValue(AVKey.USE_TRANSPARENT_TEXTURES, true);

		return params;
	}

	public static class URLBuilder implements TileUrlBuilder {
		private static final String MAX_VERSION = "1.3.0";

		private final String layerNames;
		private final String styleNames;
		private final String imageFormat;
		private final String wmsVersion;
		private final String crs;
		private final String backgroundColor;
		public String URLTemplate;

		public URLBuilder(AVList params) {
			this.layerNames = params.getStringValue(AVKey.LAYER_NAMES);
			this.styleNames = params.getStringValue(AVKey.STYLE_NAMES);
			this.imageFormat = params.getStringValue(AVKey.IMAGE_FORMAT);
			this.backgroundColor = params.getStringValue(AVKey.WMS_BACKGROUND_COLOR);
			String version = params.getStringValue(AVKey.WMS_VERSION);

			if (version == null || version.compareTo(MAX_VERSION) >= 0) {
				this.wmsVersion = MAX_VERSION;
				this.crs = "&crs=CRS:84";
			} else {
				this.wmsVersion = version;
				this.crs = "&srs=EPSG:4326";
			}
		}

		public URL getURL(Tile tile, String altImageFormat) throws MalformedURLException {
			StringBuffer sb;
			if (this.URLTemplate == null) {
				sb = new StringBuffer(WWXML.fixGetMapString(tile.getLevel().getService()));

				if (!sb.toString().toLowerCase().contains("service=wms")) sb.append("service=WMS");
				sb.append("&request=GetMap");
				sb.append("&version=").append(this.wmsVersion);
				sb.append(this.crs);
				sb.append("&layers=").append(this.layerNames);
				sb.append("&styles=").append(this.styleNames != null ? this.styleNames : "");
				sb.append("&transparent=TRUE");
				if (this.backgroundColor != null) sb.append("&bgcolor=").append(this.backgroundColor);

				this.URLTemplate = sb.toString();
			} else {
				sb = new StringBuffer(this.URLTemplate);
			}

			String format = (altImageFormat != null) ? altImageFormat : this.imageFormat;
			if (null != format) sb.append("&format=").append(format);

			sb.append("&width=").append(tile.getWidth());
			sb.append("&height=").append(tile.getHeight());

			Sector s = tile.getSector();
			sb.append("&bbox=");
			sb.append(s.minLongitude.degrees);
			sb.append(",");
			sb.append(s.minLatitude.degrees);
			sb.append(",");
			sb.append(s.maxLongitude.degrees);
			sb.append(",");
			sb.append(s.maxLatitude.degrees);

			return new java.net.URL(sb.toString().replace(" ", "%20"));
		}
	}
}
