/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.xml.*;

/**
 * Represents the KML <i>Point</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLPoint.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLPoint extends KMLAbstractGeometry
{
    protected Position coordinates;

    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLPoint(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    protected void doAddEventContent(Object o, XMLEventParserContext ctx, XMLEvent event, Object... args)
    {
        if (event.getName().getLocalPart().equals("coordinates"))
            this.setCoordinates((Position.PositionList) o);
        else
            super.doAddEventContent(o, ctx, event, args);
    }

    public boolean isExtrude()
    {
        return this.getExtrude() == Boolean.TRUE;
    }

    public Boolean getExtrude()
    {
        return (Boolean) this.getField("extrude");
    }

    public String getAltitudeMode()
    {
        return (String) this.getField("altitudeMode");
    }

    public Position getCoordinates()
    {
        return this.coordinates;
    }

    protected void setCoordinates(Position.PositionList coordsList)
    {
        if (coordsList != null && coordsList.list.size() > 0)
            this.coordinates = coordsList.list.get(0);
    }

    @Override
    public void applyChange(KMLAbstractObject sourceValues)
    {
        if (!(sourceValues instanceof KMLPoint))
        {
            String message = Logging.getMessage("nullValue.SourceIsNull");
            Logging.warning(message);
            throw new IllegalArgumentException(message);
        }

        KMLPoint point = (KMLPoint) sourceValues;

        if (point.getCoordinates() != null)
            this.coordinates = point.getCoordinates();

        super.applyChange(sourceValues); // sends geometry-changed notification
    }
}
