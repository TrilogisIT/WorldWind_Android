/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

/**
 * @author dcollins
 * @version $Id: TaskService.java 733 2012-09-02 17:15:09Z dcollins $
 */
public interface TaskService
{
    void runTask(Runnable task);

    boolean contains(Runnable task);

    boolean isFull();
}
