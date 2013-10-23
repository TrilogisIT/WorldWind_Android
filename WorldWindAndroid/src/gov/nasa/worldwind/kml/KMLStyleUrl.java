/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.util.*;

/**
 * @author tag
 * @version $Id: KMLStyleUrl.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLStyleUrl extends KMLAbstractObject
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLStyleUrl(String namespaceURI)
    {
        super(namespaceURI);
    }

    /**
     * Resolves a <i>styleUrl</i> to a style selector, which is either a style or style map.
     * <p/>
     * If the url refers to a remote resource and the resource has not been retrieved and cached locally, this method
     * returns null and initiates a retrieval.
     *
     * @return the style or style map referred to by the style URL.
     */
    public KMLAbstractStyleSelector resolveStyleUrl()
    {
        if (WWUtil.isEmpty(this.getCharacters()))
            return null;

        Object o = this.getRoot().resolveReference(this.getCharacters());
        return o instanceof KMLAbstractStyleSelector ? (KMLAbstractStyleSelector) o : null;
    }

    @Override
    public void applyChange(KMLAbstractObject sourceValues)
    {
        if (!(sourceValues instanceof KMLStyleUrl))
        {
            String message = Logging.getMessage("KML.InvalidElementType", sourceValues.getClass().getName());
            Logging.warning(message);
            throw new IllegalArgumentException(message);
        }

        super.applyChange(sourceValues);

        this.onChange(new Message(KMLAbstractObject.MSG_STYLE_CHANGED, this));
    }
}
