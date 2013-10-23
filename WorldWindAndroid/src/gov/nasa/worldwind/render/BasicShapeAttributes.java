/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.util.Logging;

/**
 * Basic implementation of the {@link gov.nasa.worldwind.render.ShapeAttributes} interface.
 *
 * @author dcollins
 * @version $Id: BasicShapeAttributes.java 790 2012-09-24 17:12:51Z dcollins $
 */
public class BasicShapeAttributes implements ShapeAttributes
{
    /** Indicates whether or not some of the shape's attributes are unresolved. Initially <code>false</code>. */
    protected boolean unresolved;
    /** Indicates whether lighting is applied to the shape. Initially <code>false</code>. */
    protected boolean enableLighting;
    /** Indicates whether or not the shape's interior is drawn. Initially <code>false</code>. */
    protected boolean enableInterior;
    /** Indicates the RGBA color of the shape's interior. Initially <code>null</code>. */
    protected Color interiorColor;
    /** Indicates whether or not the shape's outline is drawn. Initially <code>false</code>. */
    protected boolean enableOutline;
    /** Indicates the RGBA color of the shape's outline. Initially <code>null</code>. */
    protected Color outlineColor;
    /** Indicates the line width (in pixels) used when rendering the shape's outline. Initially 0.0. */
    protected double outlineWidth;

    /**
     * Creates a new BasicShapeAttributes with the default attributes. The default attributes are as follows:
     * <p/>
     * <table> <tr><th>Attribute</th><th>Default Value</th></tr> <tr><td>unresolved</td><td><code>false</code></td></tr>
     * <tr><td>enableLighting</td><td><code>false</code></td></tr> <tr><td>enableInterior</td><td><code>true</code></td></tr>
     * <tr><td>interiorColor</td><td>white</td></tr> <tr><td>enableOutline</td><td><code>true</code></td></tr>
     * <tr><td>outlineColor</td><td>black</td></tr> <tr><td>outlineWidth</td><td>1.0</td></tr> </table>
     */
    public BasicShapeAttributes()
    {
        // Note: update the above constructor comment if these defaults change.

        this.enableLighting = false;
        this.enableInterior = true;
        this.interiorColor = Color.white(); // Returns a new instance; no need to insulate ourselves from changes.
        this.enableOutline = true;
        this.outlineColor = Color.black(); // Returns a new instance; no need to insulate ourselves from changes.
        this.outlineWidth = 1;
    }

    /**
     * Creates a new BasicShapeAttributes with the specified attributes. This does not retain any reference to the
     * specified attributes, or modify them in any way. The specified bundle's attributes are copied into the new
     * bundle. The RGBA components from each color attribute are copied into the new bundle's corresponding color
     * attributes.
     *
     * @param attributes the attribute values used to create the new bundle.
     *
     * @throws IllegalArgumentException if attributes is <code>null</code>.
     */
    public BasicShapeAttributes(ShapeAttributes attributes)
    {
        if (attributes == null)
        {
            String msg = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.enableLighting = attributes.isEnableLighting();
        this.enableInterior = attributes.isEnableInterior();
        this.interiorColor = new Color(attributes.getInteriorColor()); // Copy to insulate ourselves from changes.
        this.enableOutline = attributes.isEnableOutline();
        this.outlineColor = new Color(attributes.getOutlineColor()); // Copy to insulate ourselves from changes.
        this.outlineWidth = attributes.getOutlineWidth();
    }

    /** {@inheritDoc} */
    public void set(ShapeAttributes attributes)
    {
        if (attributes == null)
        {
            String msg = Logging.getMessage("nullValue.AttributesIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.enableLighting = attributes.isEnableLighting();
        this.enableInterior = attributes.isEnableInterior();
        this.interiorColor.set(attributes.getInteriorColor()); // Copy to insulate ourselves from changes.
        this.enableOutline = attributes.isEnableOutline();
        this.outlineColor.set(attributes.getOutlineColor()); // Copy to insulate ourselves from changes.
        this.outlineWidth = attributes.getOutlineWidth();
    }

    /** {@inheritDoc} */
    public boolean isUnresolved()
    {
        return unresolved;
    }

    /** {@inheritDoc} */
    public void setUnresolved(boolean unresolved)
    {
        this.unresolved = unresolved;
    }

    /** {@inheritDoc} */
    public boolean isEnableLighting()
    {
        return enableLighting;
    }

    /** {@inheritDoc} */
    public void setEnableLighting(boolean tf)
    {
        this.enableLighting = tf;
    }

    /** {@inheritDoc} */
    public boolean isEnableInterior()
    {
        return this.enableInterior;
    }

    /** {@inheritDoc} */
    public void setEnableInterior(boolean tf)
    {
        this.enableInterior = tf;
    }

    /** {@inheritDoc} */
    public Color getInteriorColor()
    {
        return this.interiorColor;
    }

    /** {@inheritDoc} */
    public void setInteriorColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.interiorColor.set(color);
    }

    /** {@inheritDoc} */
    public boolean isEnableOutline()
    {
        return this.enableOutline;
    }

    /** {@inheritDoc} */
    public void setEnableOutline(boolean tf)
    {
        this.enableOutline = tf;
    }

    /** {@inheritDoc} */
    public Color getOutlineColor()
    {
        return this.outlineColor;
    }

    /** {@inheritDoc} */
    public void setOutlineColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.outlineColor.set(color);
    }

    /** {@inheritDoc} */
    public double getOutlineWidth()
    {
        return this.outlineWidth;
    }

    /** {@inheritDoc} */
    public void setOutlineWidth(double width)
    {
        if (width <= 0)
        {
            String msg = Logging.getMessage("generic.WidthIsInvalid", width);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.outlineWidth = width;
    }
}
