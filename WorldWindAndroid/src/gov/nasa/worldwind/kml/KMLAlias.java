/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>Alias</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLAlias.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLAlias extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLAlias(String namespaceURI)
    {
        super(namespaceURI);
    }

    public String getTargetHref()
    {
        return (String) this.getField("targetHref");
    }

    public String getSourceRef()
    {
        return (String) this.getField("sourceHref");
    }
}
