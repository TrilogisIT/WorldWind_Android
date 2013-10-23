/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.*;

import java.net.*;

/**
 * @author tag
 * @version $Id: WMSBasicElevationModel.java 755 2012-09-06 18:44:44Z tgaskins $
 */
public class WMSBasicElevationModel extends BasicElevationModel
{
    public WMSBasicElevationModel(AVList params)
    {
        super(params);
    }

    public WMSBasicElevationModel(Element domElement, AVList params)
    {
        this(wmsGetParamsFromDocument(domElement, params));
    }

    // TODO implement on Android
//    public WMSBasicElevationModel(WMSCapabilities caps, AVList params)
//    {
//        this(wmsGetParamsFromCapsDoc(caps, params));
//    }

    protected static AVList wmsGetParamsFromDocument(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        DataConfigurationUtils.getWMSLayerConfigParams(domElement, params);
        BasicElevationModel.getBasicElevationModelConfigParams(domElement, params);
        wmsSetFallbacks(params);

        params.setValue(AVKey.TILE_URL_BUILDER, new URLBuilder(params.getStringValue(AVKey.WMS_VERSION), params));

        return params;
    }

    protected static void wmsSetFallbacks(AVList params)
    {
        if (params.getValue(AVKey.LEVEL_ZERO_TILE_DELTA) == null)
        {
            Angle delta = Angle.fromDegrees(20);
            params.setValue(AVKey.LEVEL_ZERO_TILE_DELTA, new LatLon(delta, delta));
        }

        if (params.getValue(AVKey.TILE_WIDTH) == null)
            params.setValue(AVKey.TILE_WIDTH, 150);

        if (params.getValue(AVKey.TILE_HEIGHT) == null)
            params.setValue(AVKey.TILE_HEIGHT, 150);

        if (params.getValue(AVKey.FORMAT_SUFFIX) == null)
            params.setValue(AVKey.FORMAT_SUFFIX, ".bil");

        if (params.getValue(AVKey.MISSING_DATA_SIGNAL) == null)
            params.setValue(AVKey.MISSING_DATA_SIGNAL, -9999d);

        if (params.getValue(AVKey.NUM_LEVELS) == null)
            params.setValue(AVKey.NUM_LEVELS, 19); // approximately 0.1 meters per pixel

        if (params.getValue(AVKey.NUM_EMPTY_LEVELS) == null)
            params.setValue(AVKey.NUM_EMPTY_LEVELS, 0);
    }

    // TODO: consolidate common code in WMSTiledImageLayer.URLBuilder and WMSBasicElevationModel.URLBuilder
    protected static class URLBuilder implements TileUrlBuilder
    {
        protected static final String MAX_VERSION = "1.3.0";

        protected final String layerNames;
        protected final String styleNames;
        protected final String imageFormat;
        protected final String bgColor;
        protected final String wmsVersion;
        protected final String crs;
        protected String URLTemplate = null;

        protected URLBuilder(String version, AVList params)
        {
            Double d = (Double) params.getValue(AVKey.MISSING_DATA_SIGNAL);

            this.layerNames = params.getStringValue(AVKey.LAYER_NAMES);
            this.styleNames = params.getStringValue(AVKey.STYLE_NAMES);
            this.imageFormat = params.getStringValue(AVKey.IMAGE_FORMAT);
            this.bgColor = (d != null) ? d.toString() : null;

            if (version == null || version.compareTo(MAX_VERSION) >= 0)
            {
                this.wmsVersion = MAX_VERSION;
//                this.crs = "&crs=CRS:84";
                this.crs = "&crs=EPSG:4326"; // TODO: what's the correct CRS value for these versions?
            }
            else
            {
                this.wmsVersion = version;
                this.crs = "&srs=EPSG:4326";
            }
        }

        public URL getURL(gov.nasa.worldwind.util.Tile tile, String altImageFormat) throws MalformedURLException
        {
            StringBuffer sb;
            if (this.URLTemplate == null)
            {
                sb = new StringBuffer(tile.getLevel().getService());

                if (!sb.toString().toLowerCase().contains("service=wms"))
                    sb.append("service=WMS");
                sb.append("&request=GetMap");
                sb.append("&version=");
                sb.append(this.wmsVersion);
                sb.append(this.crs);
                sb.append("&layers=");
                sb.append(this.layerNames);
                sb.append("&styles=");
                sb.append(this.styleNames != null ? this.styleNames : "");
                sb.append("&format=");
                if (altImageFormat == null)
                    sb.append(this.imageFormat);
                else
                    sb.append(altImageFormat);
                if (this.bgColor != null)
                {
                    sb.append("&bgColor=");
                    sb.append(this.bgColor);
                }

                this.URLTemplate = sb.toString();
            }
            else
            {
                sb = new StringBuffer(this.URLTemplate);
            }

            sb.append("&width=");
            sb.append(tile.getWidth());
            sb.append("&height=");
            sb.append(tile.getHeight());

            Sector s = tile.getSector();
            sb.append("&bbox=");
            sb.append(s.minLongitude.degrees);
            sb.append(",");
            sb.append(s.minLatitude.degrees);
            sb.append(",");
            sb.append(s.maxLongitude.degrees);
            sb.append(",");
            sb.append(s.maxLatitude.degrees);
            sb.append("&"); // terminate the query string

            return new java.net.URL(sb.toString().replace(" ", "%20"));
        }
    }

    //**************************************************************//
    //********************  Configuration  *************************//
    //**************************************************************//

    /**
     * Parses WMSBasicElevationModel configuration parameters from a specified WMS Capabilities source. This writes
     * output as key-value pairs to params. Supported key and parameter names are: <table>
     * <th><td>Parameter</td><td>Value</td><td>Type</td></th> <tr><td>{@link AVKey#ELEVATION_MAX}</td><td>WMS layer's
     * maximum extreme elevation</td><td>Double</td></tr> <tr><td>{@link AVKey#ELEVATION_MIN}</td><td>WMS layer's
     * minimum extreme elevation</td><td>Double</td></tr> <tr><td>{@link AVKey#DATA_TYPE}</td><td>Translate WMS layer's
     * image format to a matching data type</td><td>String</td></tr> </table> This also parses common WMS layer
     * parameters by invoking {@link DataConfigurationUtils#getWMSLayerConfigParams(gov.nasa.worldwind.ogc.wms.WMSCapabilities,
     * String[], gov.nasa.worldwind.avlist.AVList)}.
     *
     * @param caps                  the WMS Capabilities source to parse for WMSBasicElevationModel configuration
     *                              parameters.
     * @param formatOrderPreference an ordered array of preferred image formats, or null to use the default format.
     * @param params                the output key-value pairs which recieve the WMSBasicElevationModel configuration
     *                              parameters.
     *
     * @return a reference to params.
     *
     * @throws IllegalArgumentException if either the document or params are null, or if params does not contain the
     *                                  required key-value pairs.
     * @throws gov.nasa.worldwind.exception.WWRuntimeException
     *                                  if the Capabilities document does not contain any of the required information.
     */
    // TODO implement on Android
//    public static AVList getWMSElevationModelConfigParams(WMSCapabilities caps, String[] formatOrderPreference,
//        AVList params)
//    {
//        if (caps == null)
//        {
//            String message = Logging.getMessage("nullValue.WMSCapabilities");
//            Logging.error(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        if (params == null)
//        {
//            String message = Logging.getMessage("nullValue.ElevationModelConfigParams");
//            Logging.error(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        // Get common WMS layer parameters.
//        DataConfigurationUtils.getWMSLayerConfigParams(caps, formatOrderPreference, params);
//
//        // Attempt to extract the WMS layer names from the specified parameters.
//        String layerNames = params.getStringValue(AVKey.LAYER_NAMES);
//        if (layerNames == null || layerNames.length() == 0)
//        {
//            String message = Logging.getMessage("nullValue.WMSLayerNames");
//            Logging.error(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        String[] names = layerNames.split(",");
//        if (names == null || names.length == 0)
//        {
//            String message = Logging.getMessage("nullValue.WMSLayerNames");
//            Logging.error(message);
//            throw new IllegalArgumentException(message);
//        }
//
//        // Get the layer's extreme elevations.
//        Double[] extremes = caps.getLayerExtremeElevations(caps, names);
//
//        Double d = (Double) params.getValue(AVKey.ELEVATION_MIN);
//        if (d == null && extremes != null && extremes[0] != null)
//            params.setValue(AVKey.ELEVATION_MIN, extremes[0]);
//
//        d = (Double) params.getValue(AVKey.ELEVATION_MAX);
//        if (d == null && extremes != null && extremes[1] != null)
//            params.setValue(AVKey.ELEVATION_MAX, extremes[1]);
//
//        // Compute the internal pixel type from the image format.
//        if (params.getValue(AVKey.DATA_TYPE) == null && params.getValue(AVKey.IMAGE_FORMAT) != null)
//        {
//            String s = WWIO.makeDataTypeForMimeType(params.getValue(AVKey.IMAGE_FORMAT).toString());
//            if (s != null)
//                params.setValue(AVKey.DATA_TYPE, s);
//        }
//
//        // Use the default data type.
//        if (params.getValue(AVKey.DATA_TYPE) == null)
//            params.setValue(AVKey.DATA_TYPE, AVKey.INT16);
//
//        // Use the default byte order.
//        if (params.getValue(AVKey.BYTE_ORDER) == null)
//            params.setValue(AVKey.BYTE_ORDER, AVKey.LITTLE_ENDIAN);
//
//        return params;
//    }
}
