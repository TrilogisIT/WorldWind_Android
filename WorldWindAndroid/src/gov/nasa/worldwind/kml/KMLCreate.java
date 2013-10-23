/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.util.WWUtil;
import gov.nasa.worldwind.util.xml.*;

import java.util.*;

/**
 * Represents the KML <i>Create</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLCreate.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLCreate extends AbstractXMLEventParser implements KMLUpdateOperation
{
    protected List<KMLAbstractContainer> containers = new ArrayList<KMLAbstractContainer>();

    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLCreate(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventContent(Object o, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if (o instanceof KMLAbstractContainer)
            this.addContainer((KMLAbstractContainer) o);
        else
            super.doAddEventContent(o, ctx, event, args);
    }

    protected void addContainer(KMLAbstractContainer o)
    {
        this.containers.add(o);
    }

    public List<KMLAbstractContainer> getContainers()
    {
        return this.containers;
    }

    public void applyOperation(KMLRoot targetRoot)
    {
        for (KMLAbstractContainer container : this.containers)
        {
            String targetId = container.getTargetId();
            if (WWUtil.isEmpty(targetId))
                continue;

            Object o = targetRoot.getItemByID(targetId);
            if (o == null || !(o instanceof KMLAbstractContainer))
                continue;

            KMLAbstractContainer receivingContainer = (KMLAbstractContainer) o;

            for (KMLAbstractFeature feature : container.getFeatures())
            {
                receivingContainer.addFeature(feature);
            }
        }
    }
}
