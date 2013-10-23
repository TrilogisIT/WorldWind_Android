/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.geom.*;

/**
 * @author dcollins
 * @version $Id: SurfaceTile.java 733 2012-09-02 17:15:09Z dcollins $
 */
public interface SurfaceTile
{
    Sector getSector();

    boolean bind(DrawContext dc);

    void applyInternalTransform(DrawContext dc, Matrix matrix);
}
