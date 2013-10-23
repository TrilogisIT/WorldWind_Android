/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>Model</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLModel.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLModel extends KMLAbstractGeometry
{
    /** Flag to indicate that the link has been fetched from the hash map. */
    protected boolean linkFetched = false;
//    protected KMLLink link; // TODO

    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLModel(String namespaceURI)
    {
        super(namespaceURI);
    }

    public String getAltitudeMode()
    {
        return (String) this.getField("altitudeMode");
    }

    public KMLLocation getLocation()
    {
        return (KMLLocation) this.getField("Location");
    }

    public KMLOrientation getOrientation()
    {
        return (KMLOrientation) this.getField("Orientation");
    }

    public KMLScale getScale()
    {
        return (KMLScale) this.getField("Scale");
    }
//
//    public KMLLink getLink() // TODO
//    {
//        if (!this.linkFetched)
//        {
//            this.link = (KMLLink) this.getField("Link");
//            this.linkFetched = true;
//        }
//        return this.link;
//    }

    public KMLResourceMap getResourceMap()
    {
        return (KMLResourceMap) this.getField("ResourceMap");
    }
}
