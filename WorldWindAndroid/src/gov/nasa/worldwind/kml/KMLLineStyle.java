/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>LineStyle</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLLineStyle.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLLineStyle extends KMLAbstractColorStyle
{
    public KMLLineStyle(String namespaceURI)
    {
        super(namespaceURI);
    }

    public Double getWidth()
    {
        return (Double) this.getField("width");
    }
}
