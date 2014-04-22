/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.terrain;

import android.graphics.Point;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.render.DrawContext;

import java.util.List;

/**
 * @author dcollins
 * @version $Id: SectorGeometryList.java 802 2012-09-26 01:37:27Z dcollins $
 */
public interface SectorGeometryList extends List<SectorGeometry>
{
    Sector getSector();

    /**
     * Computes the point in model coordinates on the geometry's surface at specified geographic location.
     *
     * @param latitude  the latitude of the point to compute.
     * @param longitude the longitude of the point to compute.
     * @param result    contains the model coordinate point in meters, relative to an origin of (0, 0, 0) after this
     *                  method exits. The result parameter is left unchanged if this method returns <code>false</code>.
     *
     * @return <code>true</code> if there is sector geometry in this list for the specified location, otherwise
     *         <code>false</code>.
     *
     * @throws IllegalArgumentException if any of the latitude, longitude or result are <code>null</code>.
     */
    boolean getSurfacePoint(Angle latitude, Angle longitude, Vec4 result);

    void beginRendering(DrawContext dc);

    void endRendering(DrawContext dc);

    void pick(DrawContext dc, Point pickPoint);

	/**
	 * Determines if and where a ray intersects the geometry.
	 *
	 * @param line the <code>Line</code> for which an intersection is to be found.
	 *
	 * @return the <Vec4> point closest to the ray origin where an intersection has been found or null if no
	 *         intersection was found.
	 */
	public Intersection[] intersect(Line line);

	/**
	 * Determines if and where the geometry intersects the ellipsoid at a given elevation.
	 * <p/>
	 * The returned array of <code>Intersection</code> describes a list of individual segments - two
	 * <code>Intersection</code> for each, corresponding to each geometry triangle that intersects the given elevation.
	 * <p/>
	 * Note that the provided bounding <code>Sector</code> only serves as a 'hint' to avoid processing unnecessary
	 * geometry tiles. The returned intersection list may contain segments outside that sector.
	 *
	 * @param elevation the elevation for which intersections are to be found.
	 * @param sector    the sector inside which intersections are to be found.
	 *
	 * @return a list of <code>Intersection</code> pairs/segments describing a contour line at the given elevation.
	 */
	public Intersection[] intersect(double elevation, Sector sector);
}
