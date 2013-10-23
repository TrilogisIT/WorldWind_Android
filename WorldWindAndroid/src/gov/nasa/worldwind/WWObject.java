/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.event.MessageListener;

import java.beans.PropertyChangeListener;

/**
 * An interface provided by the major World Wind components to provide attribute-value list management and property
 * change management. Classifies implementers as property-change listeners, allowing them to receive property-change
 * events.
 *
 * @author dcollins
 * @version $Id: WWObject.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public interface WWObject extends AVList, PropertyChangeListener, MessageListener
{
}
