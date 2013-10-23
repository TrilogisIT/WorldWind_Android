/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.*;
import gov.nasa.worldwind.avlist.*;
import gov.nasa.worldwind.cache.FileStore;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.util.*;
import org.w3c.dom.Element;

import javax.xml.xpath.XPath;

/**
 * @author dcollins
 * @version $Id: AbstractElevationModel.java 757 2012-09-06 19:31:03Z tgaskins $
 */
public abstract class AbstractElevationModel extends WWObjectImpl implements ElevationModel
{
    protected FileStore dataFileStore = WorldWind.getDataFileStore();
    protected double missingDataFlag = -Double.MAX_VALUE;
    protected double missingDataValue = 0;

    protected boolean networkRetrievalEnabled = true;
    protected long expiryTime = 0;

    public void dispose()
    {
    }

    public String getName()
    {
        Object n = this.getValue(AVKey.DISPLAY_NAME);

        return n != null ? n.toString() : this.toString();
    }

    public void setName(String name)
    {
        this.setValue(AVKey.DISPLAY_NAME, name);
    }

    public String toString()
    {
        Object n = this.getValue(AVKey.DISPLAY_NAME);

        return n != null ? n.toString() : super.toString();
    }

    public boolean isNetworkRetrievalEnabled()
    {
        return this.networkRetrievalEnabled;
    }

    public void setNetworkRetrievalEnabled(boolean enabled)
    {
        this.networkRetrievalEnabled = enabled;
    }

    public long getExpiryTime()
    {
        return this.expiryTime;
    }

    public void setExpiryTime(long expiryTime)
    {
        this.expiryTime = expiryTime;
    }

    public double getMissingDataSignal()
    {
        return missingDataFlag;
    }

    public void setMissingDataSignal(double missingDataFlag)
    {
        this.missingDataFlag = missingDataFlag;
    }

    public double getMissingDataReplacement()
    {
        return missingDataValue;
    }

    public void setMissingDataReplacement(double missingDataValue)
    {
        this.missingDataValue = missingDataValue;
    }

    public double getDetailHint(Sector sector)
    {
        if (sector == null)
        {
            String msg = Logging.getMessage("nullValue.SectorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return 0.0;
    }

    public FileStore getDataFileStore()
    {
        return dataFileStore;
    }

    public double getElevation(Angle latitude, Angle longitude)
    {
        if (latitude == null || longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        double e = this.getUnmappedElevation(latitude, longitude);
        return e == this.missingDataFlag ? this.missingDataValue : e;
    }

    protected boolean isTransparentValue(Double value)
    {
        return ((value == null || value.equals(this.getMissingDataSignal()))
            && this.getMissingDataReplacement() == this.getMissingDataSignal());
    }

    //**************************************************************//
    //********************  Configuration  *************************//
    //**************************************************************//

    /**
     * Parses elevation model configuration parameters from the specified DOM document. This writes output as key-value
     * pairs to params. If a parameter from the XML document already exists in params, that parameter is ignored.
     * Supported parameters are: <table> <th><td>Parameter</td><td>Element Path</td><td>Type</td></th> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#DISPLAY_NAME}</td><td>DisplayName</td><td>String</td></tr> <tr><td>{@link
     * gov.nasa.worldwind.avlist.AVKey#NETWORK_RETRIEVAL_ENABLED}</td><td>NetworkRetrievalEnabled</td><td>Boolean</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MISSING_DATA_SIGNAL}</td><td>MissingData/@signal</td><td>Double</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#MISSING_DATA_REPLACEMENT}</td><td>MissingData/@replacement</td><td>Double</td></tr>
     * <tr><td>{@link gov.nasa.worldwind.avlist.AVKey#DETAIL_HINT}</td><td>DataDetailHint</td><td>Double</td></tr>
     * </table>
     *
     * @param domElement the XML document root to parse for elevation model configuration elements.
     * @param params     the output key-value pairs which recieve the elevation model configuration parameters. A null
     *                   reference is permitted.
     *
     * @return a reference to params, or a new AVList if params is null.
     *
     * @throws IllegalArgumentException if the document is null.
     */
    public static AVList getElevationModelConfigParams(Element domElement, AVList params)
    {
        if (domElement == null)
        {
            String message = Logging.getMessage("nullValue.DocumentIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (params == null)
            params = new AVListImpl();

        XPath xpath = WWXML.makeXPath();

        WWXML.checkAndSetStringParam(domElement, params, AVKey.DISPLAY_NAME, "DisplayName", xpath);
        WWXML.checkAndSetBooleanParam(domElement, params, AVKey.NETWORK_RETRIEVAL_ENABLED, "NetworkRetrievalEnabled",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MISSING_DATA_SIGNAL, "MissingData/@signal", xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.MISSING_DATA_REPLACEMENT, "MissingData/@replacement",
            xpath);
        WWXML.checkAndSetDoubleParam(domElement, params, AVKey.DETAIL_HINT, "DataDetailHint", xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MAX_ABSENT_TILE_ATTEMPTS, "MaxAbsentTileAttempts",
            xpath);
        WWXML.checkAndSetIntegerParam(domElement, params, AVKey.MIN_ABSENT_TILE_CHECK_INTERVAL,
            "MinAbsentTileCheckInterval", xpath);

        return params;
    }
}
