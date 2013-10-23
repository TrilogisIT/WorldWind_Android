/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

import gov.nasa.worldwind.event.Message;

/**
 * Represents the KML <i>IconStyle</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLIconStyle.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLIconStyle extends KMLAbstractColorStyle
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLIconStyle(String namespaceURI)
    {
        super(namespaceURI);
    }

    public Double getScale()
    {
        return (Double) this.getField("scale");
    }

    public Double getHeading()
    {
        return (Double) this.getField("heading");
    }

    public KMLVec2 getHotSpot()
    {
        return (KMLVec2) this.getField("hotSpot");
    }
// TODO
//    public KMLIcon getIcon()
//    {
//        return (KMLIcon) this.getField("Icon");
//    }

    @Override
    public void onChange(Message msg)
    {
        if (KMLAbstractObject.MSG_LINK_CHANGED.equals(msg.getName()))
            this.onChange(new Message(KMLAbstractObject.MSG_STYLE_CHANGED, this));

        super.onChange(msg);
    }
}