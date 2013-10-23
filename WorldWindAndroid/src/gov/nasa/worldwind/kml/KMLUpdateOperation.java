/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * @author tag
 * @version $Id: KMLUpdateOperation.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public interface KMLUpdateOperation
{
    public void applyOperation(KMLRoot operationsRoot);
}