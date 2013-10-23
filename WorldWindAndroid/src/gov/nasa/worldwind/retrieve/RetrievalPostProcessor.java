/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.retrieve;

import java.nio.ByteBuffer;

/**
 * @author Tom Gaskins
 * @version $Id: RetrievalPostProcessor.java 733 2012-09-02 17:15:09Z dcollins $
 */
public interface RetrievalPostProcessor
{
    ByteBuffer run(Retriever retriever);
}
