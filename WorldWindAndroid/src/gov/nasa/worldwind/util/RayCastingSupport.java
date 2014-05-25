/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;

/**
 * Contains methods to resolve ray intersections with the terrain.
 * @author Patrick Murris
 * @version $Id$
 */

public class RayCastingSupport
{
    private static double defaultSampleLength = 100; // meters
    private static double defaultPrecision = 10;     // meters

    /**
     * Compute the intersection <code>Position</code> of the globe terrain with the ray starting 
     * at origin in the given direction. Uses default sample length and result precision.
     * @param globe the globe to intersect with.
     * @param origin origin of the ray.
     * @param direction direction of the ray.
     * @return the <code>Position</code> found or <code>null</code>.
     */
    public static Position intersectRayWithTerrain(Globe globe, Vec4 origin, Vec4 direction, Position out)
    {
        return intersectRayWithTerrain(globe, origin, direction, defaultSampleLength, defaultPrecision, out);
    }

    /**
     * Compute the intersection <code>Position</code> of the globe terrain with the ray starting
     * at origin in the given direction. Uses the given sample length and result precision.
     * @param globe the globe to intersect with.
     * @param origin origin of the ray.
     * @param direction direction of the ray.
     * @param sampleLength the sampling step length in meters.
     * @param precision the maximum sampling error in meters.
     * @return the <code>Position</code> found or <code>null</code>.
     */
    public static Position intersectRayWithTerrain(Globe globe, Vec4 origin, Vec4 direction,
                                                   double sampleLength, double precision, Position pos)
    {
        if (globe == null)
        {
            String msg = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (origin == null || direction == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (sampleLength < 0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", sampleLength);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (precision < 0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", precision);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        direction = direction.normalize3();

        // Check whether we intersect the globe at it's highest elevation
        Intersection inters[] = globe.intersect(new Line(origin, direction), globe.getMaxElevation());
        if (inters != null)
        {
            // Sort out intersection points and direction
            Vec4 p1 = inters[0].getIntersectionPoint();
            Vec4 p2 = null;
            if (p1.subtract3(origin).dot3(direction) < 0)
                p1 = null; // wrong direction
            if (inters.length == 2)
            {
                p2 = inters[1].getIntersectionPoint();
                if (p2.subtract3(origin).dot3(direction) < 0)
                    p2 = null; // wrong direction
            }

            if (p1 == null && p2 == null)   // both points in wrong direction
                return null;

            if (p1 != null && p2 != null)
            {
                // Outside sphere move to closest point
                if (origin.distanceTo3(p1) > origin.distanceTo3(p2))
                {
                    // switch p1 and p2
                    Vec4 temp = p2;
                    p2 = p1;
                    p1 = temp;
                }
            }
            else
            {
                // single point in right direction: inside sphere
                p2 = p2 == null ? p1 : p2;
                p1 = origin;
            }

            // Sample between p1 and p2
			Vec4 point = new Vec4();
            intersectSegmentWithTerrain(globe, p1, p2, sampleLength, precision, point);
            if (point != null)
                pos = globe.computePositionFromPoint(point);
        }
        return pos;
    }

    /**
     * Compute the intersection <code>Vec4</code> point of the globe terrain with a line segment
     * defined between two points. Uses the default sample length and result precision.
     * @param globe the globe to intersect with.
     * @param p1 segment start point.
     * @param p2 segment end point.
     * @return the <code>Vec4</code> point found or <code>null</code>.
     */
    public static Vec4 intersectSegmentWithTerrain(Globe globe, Vec4 p1, Vec4 p2, Vec4 out)
    {
        return intersectSegmentWithTerrain(globe, p1, p2, defaultSampleLength, defaultPrecision, out);
    }

    /**
     * Compute the intersection <code>Vec4</code> point of the globe terrain with the a segment
     * defined between two points. Uses the given sample length and result precision.
     * @param globe the globe to intersect with.
     * @param p1 segment start point.
     * @param p2 segment end point.
     * @param sampleLength the sampling step length in meters.
     * @param precision the maximum sampling error in meters.
     * @return the <code>Vec4</code> point found or <code>null</code>.
     */
    public static Vec4 intersectSegmentWithTerrain(Globe globe, Vec4 p1, Vec4 p2,
                                                double sampleLength, double precision, Vec4 point)
    {
        if (globe == null)
        {
            String msg = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (p1 == null || p2 == null)
        {
            String msg = Logging.getMessage("nullValue.Vec4IsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (sampleLength < 0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", sampleLength);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }
        if (precision < 0)
        {
            String msg = Logging.getMessage("generic.ArgumentOutOfRange", precision);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Sample between p1 and p2
        Line ray = new Line(p1, p2.subtract3(p1).normalize3());
        double rayLength = p1.distanceTo3(p2);
        double sampledDistance = 0;
        Vec4 sample = p1;
        Vec4 lastSample = null;
        while (sampledDistance <= rayLength)
        {
            Position samplePos = globe.computePositionFromPoint(sample);
            if (samplePos.elevation <= globe.getElevation(samplePos.getLatitude(), samplePos.getLongitude()))
            {
                // Below ground, intersection found
                point = sample;
                break;
            }
            if (sampledDistance >= rayLength)
                break;    // break after last sample
            // Keep sampling
            lastSample = sample;
            sampledDistance = Math.min(sampledDistance + sampleLength, rayLength);
            ray.getPointAt(sampledDistance, sample);
        }

        // Recurse for more precision if needed
        if (point != null && sampleLength > precision && lastSample != null)
            intersectSegmentWithTerrain(globe, lastSample, point, sampleLength / 10, precision, point);

        return point;
    }
}
