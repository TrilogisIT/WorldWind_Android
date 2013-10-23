/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.geom.Position;

/**
 * Represents the KML <i>Location</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLLocation.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLLocation extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLLocation(String namespaceURI)
    {
        super(namespaceURI);
    }

    public Double getLongitude()
    {
        return (Double) this.getField("longitude");
    }

    public Double getLatitude()
    {
        return (Double) this.getField("latitude");
    }

    public Double getAltitude()
    {
        return (Double) this.getField("altitude");
    }

    /**
     * Retrieves this location as a {@link gov.nasa.worldwind.geom.Position}. Fields that are not set are treated as zero.
     *
     * @return Position object representing this location.
     */
    public Position getPosition()
    {
        Double lat = this.getLatitude();
        Double lon = this.getLongitude();
        Double alt = this.getAltitude();

        return Position.fromDegrees(
            lat != null ? lat : 0,
            lon != null ? lon : 0,
            alt != null ? alt : 0);
    }
}
