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
    protected Material interiorMaterial;
    /** Indicates whether or not the shape's outline is drawn. Initially <code>false</code>. */
    protected boolean enableOutline;
    /** Indicates the RGBA color of the shape's outline. Initially <code>null</code>. */
    protected Material outlineMaterial;
    /** Indicates the line width (in pixels) used when rendering the shape's outline. Initially 0.0. */
    protected double outlineWidth;
	/** Indicates the image source that is applied as a texture to the shape's interior. Initially <code>null</code>. */
	protected Object imageSource;
	/** Indicates the amount the balloon's texture is scaled by as a floating-point value. Initially 0.0. */
	protected double imageScale;
	/** Indicates the opacity of the shape's interior as a floating-point value in the range 0.0 to 1.0. Initially 0.0. */
	protected double interiorOpacity;
	/** Indicates the opacity of the shape's outline as a floating-point value in the range 0.0 to 1.0. Initially 0.0. */
	protected double outlineOpacity;

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
        this.interiorMaterial = Material.WHITE; // Returns a new instance; no need to insulate ourselves from changes.
        this.enableOutline = true;
        this.outlineMaterial = Material.BLACK; // Returns a new instance; no need to insulate ourselves from changes.
        this.outlineWidth = 1;
		this.imageSource = null;
		this.imageScale = 1;
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

        set(attributes);
    }

	@Override
	public ShapeAttributes copy() {
		return new BasicShapeAttributes(this);
	}

	@Override
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
        this.interiorMaterial = attributes.getInteriorMaterial(); // Copy to insulate ourselves from changes.
        this.enableOutline = attributes.isEnableOutline();
        this.outlineMaterial = attributes.getOutlineMaterial(); // Copy to insulate ourselves from changes.
        this.outlineWidth = attributes.getOutlineWidth();
		this.interiorOpacity = attributes.getInteriorOpacity();
		this.outlineOpacity = attributes.getOutlineOpacity();
		this.imageSource = attributes.getImageSource();
		this.imageScale = attributes.getImageScale();
    }

    @Override
    public boolean isUnresolved()
    {
        return unresolved;
    }

    @Override
    public void setUnresolved(boolean unresolved)
    {
        this.unresolved = unresolved;
    }

    @Override
    public boolean isEnableLighting()
    {
        return enableLighting;
    }

    @Override
    public void setEnableLighting(boolean tf)
    {
        this.enableLighting = tf;
    }

    @Override
    public boolean isEnableInterior()
    {
        return this.enableInterior;
    }

    @Override
    public void setEnableInterior(boolean tf)
    {
        this.enableInterior = tf;
    }

    @Override
    public Color getInteriorColor()
    {
        return this.interiorMaterial.getDiffuse();
    }

    @Override
    public void setInteriorColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.interiorMaterial.getDiffuse().set(color);
    }

    @Override
    public boolean isEnableOutline()
    {
        return this.enableOutline;
    }

    @Override
    public void setEnableOutline(boolean tf)
    {
        this.enableOutline = tf;
    }

    @Override
    public Color getOutlineColor()
    {
        return this.outlineMaterial.getDiffuse();
    }

    @Override
    public void setOutlineColor(Color color)
    {
        if (color == null)
        {
            String msg = Logging.getMessage("nullValue.ColorIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.outlineMaterial.getDiffuse().set(color);
    }

    @Override
    public double getOutlineWidth()
    {
        return this.outlineWidth;
    }

    @Override
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

	@Override
	public Material getInteriorMaterial() {
		return this.interiorMaterial;
	}

	@Override
	public void setInteriorMaterial(Material material) {
		this.interiorMaterial = material;
	}

	@Override
	public Material getOutlineMaterial() {
		return this.outlineMaterial;
	}

	@Override
	public void setOutlineMaterial(Material material) {
		this.outlineMaterial = material;
	}

	@Override
	public double getInteriorOpacity() {
		return this.interiorOpacity;
	}

	@Override
	public void setInteriorOpacity(double opacity) {
		this.interiorOpacity = opacity;
	}

	@Override
	public double getOutlineOpacity() {
		return this.outlineOpacity;
	}

	@Override
	public void setOutlineOpacity(double opacity) {
		this.outlineOpacity = opacity;
	}

	@Override
	public Object getImageSource() {
		return this.imageSource;
	}

	@Override
	public void setImageSource(Object imageSource) {
		this.imageSource = imageSource;
	}

	@Override
	public double getImageScale() {
		return this.imageScale;
	}

	@Override
	public void setImageScale(double scale) {
		this.imageScale = scale;
	}
}
