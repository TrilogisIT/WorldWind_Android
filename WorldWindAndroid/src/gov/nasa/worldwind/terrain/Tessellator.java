/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.WWObject;
import gov.nasa.worldwind.render.DrawContext;

/**
 * @author dcollins
 * @version $Id: Tessellator.java 733 2012-09-02 17:15:09Z dcollins $
 */
public interface Tessellator extends WWObject
{
    SectorGeometryList tessellate(DrawContext dc);
}
