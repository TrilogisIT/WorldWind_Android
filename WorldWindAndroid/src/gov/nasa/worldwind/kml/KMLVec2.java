/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.*;

/**
 * Represents the KML <i>Vec2</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLVec2.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLVec2 extends KMLAbstractObject
{
    protected Double x;
    protected Double y;

    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLVec2(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventAttribute(String key, Object value, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if ("x".equals(key))
            this.setX(WWUtil.makeDouble((String) value));
        else if ("y".equals(key))
            this.setY(WWUtil.makeDouble((String) value));
        else
            super.doAddEventAttribute(key, value, ctx, event, args);
    }

    protected void setX(Double o)
    {
        this.x = o;
    }

    public Double getX()
    {
        return this.x;
    }

    protected void setY(Double o)
    {
        this.y = o;
    }

    public Double getY()
    {
        return this.y;
    }

    public String getXunits()
    {
        return (String) this.getField("xunits");
    }

    public String getYunits()
    {
        return (String) this.getField("yunits");
    }
}
