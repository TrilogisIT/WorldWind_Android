/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.util.xml.*;

/**
 * Represents the KML <i>Pair</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLPair.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLPair extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLPair(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventContent(Object o, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if (o instanceof KMLAbstractStyleSelector)
            this.setStyleSelector((KMLAbstractStyleSelector) o);
        else
            super.doAddEventContent(o, ctx, event, args);
    }

    public String getKey()
    {
        return (String) this.getField("key");
    }

    public KMLStyleUrl getStyleUrl()
    {
        return (KMLStyleUrl) this.getField("styleUrl");
    }

    public KMLAbstractStyleSelector getStyleSelector()
    {
        return (KMLAbstractStyleSelector) this.getField("StyleSelector");
    }

    protected void setStyleSelector(KMLAbstractStyleSelector o)
    {
        this.setField("StyleSelector", o);
    }
}
