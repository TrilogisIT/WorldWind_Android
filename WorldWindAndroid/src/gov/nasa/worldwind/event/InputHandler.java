/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.event;

import android.view.View;
import gov.nasa.worldwind.*;

/**
 * @author dcollins
 * @version $Id: InputHandler.java 733 2012-09-02 17:15:09Z dcollins $
 */
public interface InputHandler extends WWObject, View.OnTouchListener
{
    WorldWindow getEventSource();

    void setEventSource(WorldWindow eventSource);
}
