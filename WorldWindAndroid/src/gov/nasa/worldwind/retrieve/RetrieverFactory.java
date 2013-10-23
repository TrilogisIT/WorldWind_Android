/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.retrieve;

import gov.nasa.worldwind.avlist.AVList;

/**
 * @author tag
 * @version $Id: RetrieverFactory.java 733 2012-09-02 17:15:09Z dcollins $
 */
public interface RetrieverFactory
{
    Retriever createRetriever(AVList params, RetrievalPostProcessor postProcessor);
}
