/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>ItemIcon</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLItemIcon.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLItemIcon extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLItemIcon(String namespaceURI)
    {
        super(namespaceURI);
    }

    public String getHref()
    {
        return (String) this.getField("href");
    }

    public String getState()
    {
        return (String) this.getField("state");
    }
}
