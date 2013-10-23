/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.event;

import android.view.*;
import android.view.View;
import gov.nasa.worldwind.*;

/**
 * @author dcollins
 * @version $Id: NoOpInputHandler.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class NoOpInputHandler extends WWObjectImpl implements InputHandler
{
    public NoOpInputHandler()
    {
    }

    public WorldWindow getEventSource()
    {
        return null;
    }

    public void setEventSource(WorldWindow eventSource)
    {
    }

    public boolean onTouch(View view, MotionEvent motionEvent)
    {
        return false;
    }
}
