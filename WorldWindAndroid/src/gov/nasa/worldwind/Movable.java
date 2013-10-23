/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind;

import gov.nasa.worldwind.geom.Position;

/**
 * Movable provides an interface for objects that can be moved geographically. The object's position is measured with
 * respect to a reference position, which is defined by the implementing object. Typically, implementing objects move
 * the entire object as a whole in response to the methods in this interface. See the documentation for each
 * implementing class to determine whether the class deviates from this.
 *
 * @author tag
 * @version $Id: Movable.java 791 2012-09-24 17:14:43Z dcollins $
 */
public interface Movable
{
    /**
     * Indicates the aggregate geographic position associated with this object. The returned position should be used as
     * a reference when specifying a new position for the object, or when computing an increment in the object's current
     * position.
     * <p/>
     * The chosen position varies among implementers of this interface. For objects defined by a list of positions, the
     * reference position is typically the first position in the list. For symmetric objects the reference position is
     * often the center of the object. In many cases the object's reference position may be explicitly specified by the
     * application.
     *
     * @return the object's reference position, or <code>null</code> if no reference position is available.
     */
    Position getReferencePosition();

    /**
     * Moves the shape over the globe's surface by the specified increment while maintaining its original azimuth, its
     * orientation relative to North. This does not retain any reference to the specified position, or modify it in any
     * way.
     *
     * @param position the latitude, longitude, and elevation to add to the shape's reference position.
     *
     * @throws IllegalArgumentException if the position is <code>null</code>.
     */
    void move(Position position);

    /**
     * Specifies a new reference position for this object by shifting the shape over the globe's surface while
     * maintaining its original azimuth, its orientation relative to North. This does not retain any reference to the
     * specified position, or modify it in any way.
     *
     * @param position the new position of the shape's reference position.
     *
     * @throws IllegalArgumentException if the position is <code>null</code>.
     */
    void moveTo(Position position);
}
