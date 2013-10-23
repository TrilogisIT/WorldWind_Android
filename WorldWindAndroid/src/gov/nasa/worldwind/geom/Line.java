/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

/**
 * @author Tom Gaskins
 * @version $Id: Line.java 810 2012-09-26 17:43:27Z dcollins $
 */
public class Line
{
    public static double distanceToSegment(Vec4 p0, Vec4 p1, Vec4 point)
    {
        Vec4 pb = nearestPointToSegment(p0, p1, point);

        return point.distanceTo3(pb);
    }

    /**
     * Finds the closest point to a third point of a segment defined by two points.
     *
     * @param p0    The first endpoint of the segment.
     * @param p1    The second endpoint of the segment.
     * @param point The point outside the segment whose closest point on the segment is desired.
     *
     * @return The closest point on (p0, p1) to point. Note that this will be p0 or p1 themselves whenever the closest
     *         point on the <em>line</em> defined by p0 and p1 is outside the segment (i.e., the results are bounded by
     *         the segment endpoints).
     */
    public static Vec4 nearestPointToSegment(Vec4 p0, Vec4 p1, Vec4 point)
    {
        if (p0 == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (p1 == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 v = p1.subtract3(p0);
        Vec4 w = point.subtract3(p0);

        double c1 = w.dot3(v);
        double c2 = v.dot3(v);

        if (c1 <= 0)
            return p0;
        if (c2 <= c1)
            return p1;

        return p0.add3(v.multiply3(c1 / c2));
    }

    protected final Vec4 origin;
    protected final Vec4 direction;

    /** Creates a new line with its origin set to (0, 0, 0) and its direction set to (1, 0, 0). */
    public Line()
    {
        this.origin = new Vec4(0, 0, 0);
        this.direction = new Vec4(1, 0, 0);
    }

    /**
     * Creates a new line with the specified origin and direction vectors. The direction vector must not have zero
     * length.
     *
     * @param origin    the line's origin.
     * @param direction the line's direction.
     *
     * @throws IllegalArgumentException if either the origin or direction are <code>null</code>, or if the direction has
     *                                  zero length.
     */
    public Line(Vec4 origin, Vec4 direction)
    {
        if (origin == null)
        {
            String msg = Logging.getMessage("nullValue.OriginIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (direction == null)
        {
            String msg = Logging.getMessage("nullValue.DirectionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (direction.getLength3() <= 0)
        {
            String msg = Logging.getMessage("generic.DirectionIsZero");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.origin = origin;
        this.direction = direction;
    }

    /**
     * Creates a new line with the specified origin and direction coordinates. The direction coordinates must not have
     * zero length.
     *
     * @param ox the line origin's x-coordinate.
     * @param oy the line origin's y-coordinate.
     * @param oz the line origin's z-coordinate.
     * @param dx the line direction's x-coordinate.
     * @param dy the line direction's y-coordinate.
     * @param dz the line direction's z-coordinate.
     *
     * @throws IllegalArgumentException if the direction coordinates have zero length.
     */
    public Line(double ox, double oy, double oz, double dx, double dy, double dz)
    {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0)
        {
            String msg = Logging.getMessage("generic.DirectionIsZero");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.origin = new Vec4(ox, oy, oz);
        this.direction = new Vec4(dx, dy, dz);
    }

    public Line copy()
    {
        return new Line(this.origin.copy(), this.direction.copy());
    }

    public Line set(Line line)
    {
        if (line == null)
        {
            String msg = Logging.getMessage("nullValue.LineIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.origin.set(line.origin);
        this.direction.set(line.direction);

        return this;
    }

    public Line set(Vec4 origin, Vec4 direction)
    {
        if (origin == null)
        {
            String msg = Logging.getMessage("nullValue.OriginIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (direction == null)
        {
            String msg = Logging.getMessage("nullValue.DirectionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (direction.getLength3() <= 0)
        {
            String msg = Logging.getMessage("generic.DirectionIsZero");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.origin.set(origin);
        this.direction.set(direction);

        return this;
    }

    public Line set(double ox, double oy, double oz, double dx, double dy, double dz)
    {
        double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len <= 0)
        {
            String msg = Logging.getMessage("generic.DirectionIsZero");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.origin.set(ox, oy, oz);
        this.direction.set(dx, dy, dz);

        return this;
    }

    /**
     * Create the line containing a line segement between two points.
     *
     * @param pa the first point of the line segment.
     * @param pb the second point of the line segment.
     *
     * @return The line containing the two points.
     *
     * @throws IllegalArgumentException if either point is null or they are coincident.
     */
    public Line setSegment(Vec4 pa, Vec4 pb)
    {
        return this.set(pa.x, pa.y, pa.z, pb.x - pa.x, pb.y - pa.y, pb.z - pa.z);
    }

    public Vec4 getOrigin()
    {
        return this.origin;
    }

    public Vec4 getDirection()
    {
        return this.direction;
    }

    public void getPointAt(double t, Vec4 result)
    {
        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        result.setPointOnLine3(this.origin, t, this.direction);
    }

    public double selfDot()
    {
        return this.origin.dot3(this.direction);
    }

    /**
     * Compute the shortest distance between this line and a specified point.
     *
     * @param point the point who's distance id computed.
     *
     * @return the distance between this line and the specified point.
     *
     * @throws IllegalArgumentException if the point is <code>null</code>.
     */
    public double distanceTo(Vec4 point)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 nearestPoint = new Vec4();
        this.nearestPointTo(point, nearestPoint);

        return nearestPoint.distanceTo3(point);
    }

    public void nearestPointTo(Vec4 point, Vec4 result)
    {
        if (point == null)
        {
            String msg = Logging.getMessage("nullValue.PointIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        result.subtract3AndSet(point, this.origin);

        double c = result.dot3(this.direction) / this.direction.dot3(this.direction);

        result.x = this.origin.x + this.direction.x * c;
        result.y = this.origin.y + this.direction.y * c;
        result.z = this.origin.z + this.direction.z * c;
    }

    /**
     * Performs a comparison to test whether this Object is internally identical to the other Object <code>o</code>.
     * This method takes into account both direction and origin, so two lines which may be equivalent may not be
     * considered equal.
     *
     * @param o the object to be compared against.
     *
     * @return true if these two objects are equal, false otherwise
     */
    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        Line that = (Line) o;
        return this.direction.equals(that.direction) && this.origin.equals(that.origin);
    }

    @Override
    public int hashCode()
    {
        int result;
        result = this.origin.hashCode();
        result = 29 * result + this.direction.hashCode();
        return result;
    }

    public String toString()
    {
        return "Origin: " + this.origin + ", Direction: " + this.direction;
    }
}
