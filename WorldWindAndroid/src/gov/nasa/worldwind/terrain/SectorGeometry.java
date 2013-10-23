/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import android.graphics.Point;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;

/**
 * @author dcollins
 * @version $Id: SectorGeometry.java 802 2012-09-26 01:37:27Z dcollins $
 */
public interface SectorGeometry
{
    Sector getSector();

    Extent getExtent();

    /**
     * Computes the point in model coordinates on the geometry's surface at the specified location.
     *
     * @param latitude  the latitude of the point to compute.
     * @param longitude the longitude of the point to compute.
     * @param result    contains the model coordinate point in meters, relative to an origin of (0, 0, 0) after this
     *                  method exits. The result parameter is left unchanged if this method returns <code>false</code>.
     *
     * @return <code>true</code> if the specified location is within this geometry's sector and its internal geometry
     *         exists, otherwise <code>false</code>.
     *
     * @throws IllegalArgumentException if any of the latitude, longitude or result are <code>null</code>.
     */
    boolean getSurfacePoint(Angle latitude, Angle longitude, Vec4 result);

    void render(DrawContext dc);

    void renderWireframe(DrawContext dc);

    void renderOutline(DrawContext dc);

    void beginRendering(DrawContext dc);

    void endRendering(DrawContext dc);

    void pick(DrawContext dc, Point pickPoint);
}
