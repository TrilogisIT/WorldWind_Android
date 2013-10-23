/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.util.Logging;

/**
 * Represents the KML <i>AbstractView</i> element.
 *
 * @author tag
 * @version $Id: KMLAbstractView.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public abstract class KMLAbstractView extends KMLAbstractObject
{
    protected KMLAbstractView(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    public void applyChange(KMLAbstractObject sourceValues)
    {
        if (!(sourceValues instanceof KMLAbstractView))
        {
            String message = Logging.getMessage("KML.InvalidElementType", sourceValues.getClass().getName());
            Logging.warning(message);
            throw new IllegalArgumentException(message);
        }

        super.applyChange(sourceValues);

        this.onChange(new Message(KMLAbstractObject.MSG_VIEW_CHANGED, this));
    }
}
