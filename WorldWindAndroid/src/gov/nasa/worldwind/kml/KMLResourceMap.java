/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.util.xml.*;

import java.util.*;

/**
 * Represents the KML <i>ResourceMap</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLResourceMap.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLResourceMap extends KMLAbstractObject
{
    protected List<KMLAlias> aliases = new ArrayList<KMLAlias>();

    public KMLResourceMap(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventContent(Object o, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if (o instanceof KMLAlias)
            this.addAlias((KMLAlias) o);
        else
            super.doAddEventContent(o, ctx, event, args);
    }

    protected void addAlias(KMLAlias o)
    {
        this.aliases.add(o);
    }

    public List<KMLAlias> getAliases()
    {
        return this.aliases;
    }
}
