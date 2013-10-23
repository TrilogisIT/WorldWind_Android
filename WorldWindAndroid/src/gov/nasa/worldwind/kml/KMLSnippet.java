/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.*;

/**
 * Represents the KML <i>Snippet</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLSnippet.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLSnippet extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLSnippet(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventAttribute(String key, Object value, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if ("maxLines".equals(key))
            this.setMaxLines(WWUtil.makeInteger(value.toString()));
        else
            super.doAddEventAttribute(key, value, ctx, event, args);
    }

    public Integer getMaxLines()
    {
        return (Integer) this.getField("maxLines");
    }

    public void setMaxLines(Integer o)
    {
        this.setField("maxLines", o);
    }
}
