/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.geom.Position;

/**
 * Represents the KML <i>LineString</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLLineString.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLLineString extends KMLAbstractGeometry
{
    public KMLLineString(String namespaceURI)
    {
        super(namespaceURI);
    }

    public boolean isExtrude()
    {
        return this.getExtrude() == Boolean.TRUE;
    }

    public Boolean getExtrude()
    {
        return (Boolean) this.getField("extrude");
    }

    public boolean isTessellate()
    {
        return this.getTessellate() == Boolean.TRUE;
    }

    public Boolean getTessellate()
    {
        return (Boolean) this.getField("tessellate");
    }

    public String getAltitudeMode()
    {
        return (String) this.getField("altitudeMode");
    }

    public Position.PositionList getCoordinates()
    {
        return (Position.PositionList) this.getField("coordinates");
    }
}
