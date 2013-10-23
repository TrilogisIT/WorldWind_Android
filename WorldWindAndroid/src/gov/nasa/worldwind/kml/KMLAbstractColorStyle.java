/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>ColorStyle</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLAbstractColorStyle.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public abstract class KMLAbstractColorStyle extends KMLAbstractSubStyle
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    protected KMLAbstractColorStyle(String namespaceURI)
    {
        super(namespaceURI);
    }

    public String getColor()
    {
        return (String) this.getField("color");
    }

    public String getColorMode()
    {
        return (String) this.getField("colorMode");
    }
}
