/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>Scale</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLScale.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLScale extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLScale(String namespaceURI)
    {
        super(namespaceURI);
    }

    public Double getX()
    {
        return (Double) this.getField("x");
    }

    public Double getY()
    {
        return (Double) this.getField("y");
    }

    public Double getZ()
    {
        return (Double) this.getField("z");
    }
}
