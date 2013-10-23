/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.util.*;
import gov.nasa.worldwind.util.xml.*;

import java.util.*;

/**
 * Represents the KML <i>MultiGeometry</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLMultiGeometry.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLMultiGeometry extends KMLAbstractGeometry
{
    protected List<KMLAbstractGeometry> geometries = new ArrayList<KMLAbstractGeometry>();

    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLMultiGeometry(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventContent(Object o, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if (o instanceof KMLAbstractGeometry)
            this.addGeometry((KMLAbstractGeometry) o);
        else
            super.doAddEventContent(o, ctx, event, args);
    }

    protected void addGeometry(KMLAbstractGeometry o)
    {
        this.geometries.add(o);
    }

    public List<KMLAbstractGeometry> getGeometries()
    {
        return this.geometries;
    }

    @Override
    public void applyChange(KMLAbstractObject sourceValues)
    {
        if (!(sourceValues instanceof KMLMultiGeometry))
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.warning(message);
            throw new IllegalArgumentException(message);
        }

        KMLMultiGeometry multiGeometry = (KMLMultiGeometry) sourceValues;

        if (multiGeometry.getGeometries() != null && multiGeometry.getGeometries().size() > 0)
            this.mergeGeometries(multiGeometry);

        super.applyChange(sourceValues);
    }

    /**
     * Merge a list of incoming geometries with the current list. If an incoming geometry has the same ID as
     * an existing one, replace the existing one, otherwise just add the incoming one.
     *
     * @param sourceMultiGeometry the incoming geometries.
     */
    protected void mergeGeometries(KMLMultiGeometry sourceMultiGeometry)
    {
        // Make a copy of the existing list so we can modify it as we traverse the copy.
        List<KMLAbstractGeometry> geometriesCopy = new ArrayList<KMLAbstractGeometry>(this.getGeometries().size());
        Collections.copy(geometriesCopy, this.getGeometries());

        for (KMLAbstractGeometry sourceGeometry : sourceMultiGeometry.getGeometries())
        {
            String id = sourceGeometry.getId();
            if (!WWUtil.isEmpty(id))
            {
                for (KMLAbstractGeometry existingGeometry : geometriesCopy)
                {
                    String currentId = existingGeometry.getId();
                    if (!WWUtil.isEmpty(currentId) && currentId.equals(id))
                    {
                        this.getGeometries().remove(existingGeometry);
                    }
                }
            }

            this.getGeometries().add(sourceGeometry);
        }
    }
}
