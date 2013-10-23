/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>PolyStyle</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLPolyStyle.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLPolyStyle extends KMLAbstractColorStyle
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLPolyStyle(String namespaceURI)
    {
        super(namespaceURI);
    }

    public Boolean getFill()
    {
        return (Boolean) this.getField("fill");
    }

    public boolean isFill()
    {
        return this.getFill() == null || this.getFill();
    }

    public Boolean getOutline()
    {
        return (Boolean) this.getField("outline");
    }

    public boolean isOutline()
    {
        return this.getOutline() == null || this.getOutline();
    }
}
