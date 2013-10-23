/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom;

/**
 * Represents a volume enclosing one or more objects or collections of points. Primarily used to test intersections with
 * other objects.
 *
 * @author Tom Gaskins
 * @version $Id: Extent.java 807 2012-09-26 17:40:25Z dcollins $
 */
public interface Extent
{
    /**
     * Returns the extent's center point.
     *
     * @return the extent's center point.
     */
    Vec4 getCenter();

    /**
     * Returns the extent's radius. The computation of the radius depends on the implementing class. See the
     * documentation for the individual classes to determine how they compute a radius.
     *
     * @return the extent's radius.
     */
    double getRadius();

    /**
     * Computes the distance between this extent and the specified point. This returns 0 if the point is inside this
     * extent. This does not retain any reference to the specified point, or modify it in any way.
     *
     * @param point the point who's distance to this extent is computed.
     *
     * @return the distance between the point and this extent, or 0 if the point is inside this extent.
     *
     * @throws IllegalArgumentException if the point is <code>null</code>.
     */
    double distanceTo(Vec4 point);

    /**
     * Computes the effective radius of the extent relative to a specified plane.
     *
     * @param plane the plane.
     *
     * @return the effective radius, or 0 if the plane is null.
     */
    double getEffectiveRadius(Plane plane);

    /**
     * Determines whether or not this <code>Extent</code> intersects <code>frustum</code>. Returns true if any part of
     * these two objects intersect, including the case where either object wholly contains the other, false otherwise.
     *
     * @param frustum the <code>Frustum</code> with which to test for intersection.
     *
     * @return true if there is an intersection, false otherwise.
     */
    boolean intersects(Frustum frustum);
}
