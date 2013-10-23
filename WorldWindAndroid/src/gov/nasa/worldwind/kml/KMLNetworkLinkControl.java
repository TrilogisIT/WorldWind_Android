/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.util.xml.*;

/**
 * Represents the KML <i>NetworkLinkControl</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLNetworkLinkControl.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLNetworkLinkControl extends AbstractXMLEventParser
{
    public KMLNetworkLinkControl(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventContent(Object o, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if (o instanceof KMLAbstractView)
            this.setField("AbstractView", o);
        else
            super.doAddEventContent(o, ctx, event, args);
    }

    public Double getMinRefreshPeriod()
    {
        return (Double) this.getField("minRefreshPeriod");
    }

    public Double getMaxSessionLength()
    {
        return (Double) this.getField("maxSessionLength");
    }

    public String getCookie()
    {
        return (String) this.getField("cookie");
    }

    public String getMessage()
    {
        return (String) this.getField("message");
    }

    public String getLinkName()
    {
        return (String) this.getField("linkName");
    }

    public String getLinkDescription()
    {
        return (String) this.getField("linkDescription");
    }

    public KMLSnippet getLinkSnippet()
    {
        return (KMLSnippet) this.getField("linkSnippet");
    }

    public String getExpires()
    {
        return (String) this.getField("expires");
    }

    public KMLUpdate getUpdate()
    {
        return (KMLUpdate) this.getField("Update");
    }

    public KMLAbstractView getView()
    {
        return (KMLAbstractView) this.getField("AbstractView");
    }
}