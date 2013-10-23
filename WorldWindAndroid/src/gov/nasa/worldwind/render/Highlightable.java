/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

/**
 * Highlightable provides an interface to control an object's highlighting. Objects implementing this interface have
 * their own highlighting behaviors and attributes and the means for setting them.
 *
 * @author tag
 * @version $Id: Highlightable.java 791 2012-09-24 17:14:43Z dcollins $
 */
public interface Highlightable
{
    /**
     * Indicates whether this object is currently highlighted.
     *
     * @return <code>true</code> if this object is highlighted, otherwise <code>false</code>.
     */
    boolean isHighlighted();

    /**
     * Specifies whether to highlight this object.
     *
     * @param highlighted <code>true</code> to highlight this object, otherwise <code>false</code>.
     */
    void setHighlighted(boolean highlighted);
}
