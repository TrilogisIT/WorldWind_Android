/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>Folder</i> element and provides access to its contents.
 *
 * @author tag
 * @version $Id: KMLFolder.java 822 2012-09-28 03:16:15Z dcollins $
 */
public class KMLFolder extends KMLAbstractContainer
{
    /**
     * Construct an instance.
     *
     * @param namespaceURI the qualifying namespace URI. May be null to indicate no namespace qualification.
     */
    public KMLFolder(String namespaceURI)
    {
        super(namespaceURI);
    }
}
