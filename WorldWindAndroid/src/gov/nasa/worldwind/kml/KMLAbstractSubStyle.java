/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.util.Logging;

/**
 * Represents the KML <i>SubStyle</i> element.
 *
 * @author tag
 * @version $Id: KMLAbstractSubStyle.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLAbstractSubStyle extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    protected KMLAbstractSubStyle(String namespaceURI)
    {
        super(namespaceURI);
    }

    @Override
    public void applyChange(KMLAbstractObject sourceValues)
    {
        if (!(sourceValues instanceof KMLAbstractSubStyle))
        {
            String message = Logging.getMessage("KML.InvalidElementType", sourceValues.getClass().getName());
            Logging.warning(message);
            throw new IllegalArgumentException(message);
        }

        super.applyChange(sourceValues);

        this.onChange(new Message(KMLAbstractObject.MSG_STYLE_CHANGED, this));
    }
}
