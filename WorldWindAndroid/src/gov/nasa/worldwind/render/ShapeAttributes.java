/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.render;

/**
 * ShapeAttributes is a common attribute bundle for World Wind shapes. Instances of ShapeAttributes may be shared by
 * many shapes, thereby reducing the memory normally required to store attributes for each shape.
 * <p/>
 * <strong>Mutability</strong>
 * <p/>
 * ShapeAttributes is mutable and provides standard accessors to each of its attributes. Additionally, ShapeAttributes
 * provides a <code>set</code> method for specifying all attributes in bulk. Changes made to the attributes are
 * generally applied to a shape when the <code>WorldWindow</code> renders the next frame. It is important to avoid
 * writing code that depends on knowing when an attribute changes. In particular, rendering code should should be
 * written to automatically display changes to any attributes during the next frame.
 * <p/>
 * <strong>Color and Lighting</strong>
 * <p/>
 * The interiorColor and outlineColor attributes provided by ShapeAttributes define both the RGB color and the opacity
 * for a shape's interior and outline. Each of the four RGBA components is represented as a floating-point value in the
 * range [0.0, 1.0], where 0.0 indicates zero intensity and 1.0 indicates full intensity. See the {@link Color}
 * documentation for more information.
 * <p/>
 * Each color's four RGBA components are assumed to exist in the standard RGBA color space, and do not represent
 * premultiplied colors. It is important that applications do not specify colors with premultiplied alpha, or modify the
 * color's RGB components to make them premultiplied. Doing so will cause a shape's interior and outline to appear in a
 * different color than desired.
 * <p/>
 * ShapeAttributes is enabled for lighting by default. Shapes are lit using a combination of their ShapeAttributes and
 * the DrawContext's lighting attributes. The ShapeAttributes' interior color and outline color attributes specify the
 * diffuse contribution to lighting. Shapes automatically compute the ambient contribution to lighting. The specular
 * contribution and shininess parameter are specified by the DrawContext's lighting attributes. Most shapes do not
 * provide an emissive contribution to lighting.
 * <p/>
 * ShapeAttributes' colors are themselves mutable, and are guaranteed to be non-null. This enables color attributes to
 * be modified either by calling the setter or by modifying the reference returned by the getter. For example, the
 * interior RGB color and interior opacity may be specified as follows:
 * <p/>
 * <code>
 * <pre>
 * ShapeAttributes attrs = shape.getAttributes(); // Get the attributes from the shape.
 *
 * // Interior color and outline color can be specified by calling setInteriorColor and setOutlineColor. This has the
 * // effect of specifying the entire RGBA color. Note that the reference passed to these methods is not retained; its
 * // RGBA color components are copied into the ShapeAttributes' internal color reference.
 * attrs.setInteriorColor(new Color(1.0, 0.0, 0.0)); // Set the interior color to opaque red.
 * attrs.setOutlineColor(new Color(1.0, 1.0, 1.0, 0.5)); // Set the outline color to 50% transparent white.
 *
 * // Interior color and outline color can also be specified by modifying the reference returned by getInteriorColor or
 * // getOutlineColor. The references returned by these methods is guaranteed to be non-null. This has the effect of
 * // enabling an application to optionally specify either the RGB or alpha components without modifying the others.
 * attrs.getInteriorColor().set(1.0, 0.0, 0.0); // Set the interior RGB color to red.
 * attrs.getOutlineColor().a = 0.5; // Set the outline opacity to 50%.
 * </pre>
 * </code>
 *
 * @author dcollins
 * @version $Id: ShapeAttributes.java 790 2012-09-24 17:12:51Z dcollins $
 */
public interface ShapeAttributes
{
    /**
     * Sets this bundle's attributes to those of the specified bundle. This does not retain any reference to the
     * specified attributes, or modify them in any way. The specified bundle's attributes are copied into this bundle.
     * The RGBA components from each color attribute are copied into this bundle's corresponding color attributes.
     *
     * @param attributes the new attribute values.
     *
     * @throws IllegalArgumentException if attributes is <code>null</code>.
     */
    void set(ShapeAttributes attributes);

    /**
     * Indicates whether some of the shape's attributes are unresolved.
     *
     * @return <code>true</code> to indicate that one or more attributes are unresolved, otherwise <code>false</code>.
     *
     * @see #setUnresolved(boolean)
     */
    boolean isUnresolved();

    /**
     * Specifies whether some of the shape's attributes are unresolved. This can be used to denote that a shape's
     * attributes are being retrieved from a non-local resource. During retrieval, the controlling code sets unresolved
     * to <code>true</code>, then sets it to <code>false</code> once the retrieval is complete. Code that interprets the
     * attributes knows that the attributes are complete when unresolved is <code>false</code>.
     *
     * @param unresolved <code>true</code> to specify that one or more attributes are unresolved, otherwise
     *                   <code>false</code>.
     *
     * @see #isUnresolved()
     */
    void setUnresolved(boolean unresolved);

    /**
     * Indicates whether lighting is applied to the shape. See the section above on <i>Color and Lighting</i> for more
     * information on shape lighting.
     *
     * @return <code>true</code> to apply lighting, otherwise <code>false</code>.
     *
     * @see #setEnableLighting(boolean)
     */
    boolean isEnableLighting();

    /**
     * Specifies whether to apply lighting to the shape. See the section above on <i>Color and Lighting</i> for more
     * information on shape lighting.
     *
     * @param tf <code>true</code> to apply lighting, otherwise <code>false</code>.
     *
     * @see #isEnableLighting()
     */
    void setEnableLighting(boolean tf);

    /**
     * Indicates whether the shape's interior geometry is drawn.
     *
     * @return <code>true</code> if the shape's interior is drawn, otherwise <code>false</code>.
     *
     * @see #setEnableInterior(boolean)
     */
    boolean isEnableInterior();

    /**
     * Specifies whether to enable the shape's interior geometry.
     *
     * @param tf <code>true</code> to enable the shape's interior, otherwise <code>false</code>.
     *
     * @see #isEnableInterior()
     */
    void setEnableInterior(boolean tf);

    /**
     * Indicates the RGBA color of the shape's interior. The returned color is guaranteed to be non-null, and can be
     * modified in place without calling setInteriorColor. See the section above on <i>Color and Lighting</i> for more
     * information on shape colors.
     *
     * @return the RGBA color applied to the shape's interior.
     *
     * @see #setInteriorColor(Color)
     */
    Color getInteriorColor();

    /**
     * Specifies the RGBA color of the shape's interior. This does not retain any reference to the specified color, or
     * modify it in any way. The color's RGBA components are copied into this bundle's interior color instance. See the
     * section above on <i>Color and Lighting</i> for more information on shape colors.
     *
     * @param color the RGBA color to apply to the shape's interior.
     *
     * @throws IllegalArgumentException if the color is <code>null</code>.
     * @see #getInteriorColor()
     */
    void setInteriorColor(Color color);

    /**
     * Indicates whether the shape's outline geometry is drawn.
     *
     * @return <code>true</code> if the shape's outline is drawn, otherwise <code>false</code>.
     *
     * @see #setEnableOutline(boolean)
     */
    boolean isEnableOutline();

    /**
     * Specifies whether to enable the shape's outline geometry.
     *
     * @param tf <code>true</code> to enable the shape's outline, otherwise <code>false</code>.
     *
     * @see #isEnableOutline()
     */
    void setEnableOutline(boolean tf);

    /**
     * Indicates the RGBA color of the shape's outline. The returned color is guaranteed to be non-null, and can be
     * modified in place without calling setOutlineColor. See the section above on <i>Color and Lighting</i> for more
     * information on shape colors.
     *
     * @return the RGBA color applied to the shape's outline.
     *
     * @see #setOutlineColor(Color)
     */
    Color getOutlineColor();

    /**
     * Specifies the color of the shape's outline. This does not retain any reference to the specified color, or modify
     * it in any way. The color's RGBA components are copied into this bundle's outline color instance. See the section
     * above on <i>Color and Lighting</i> for more information on shape colors.
     *
     * @param color the RGBA color to apply to the shape's outline.
     *
     * @throws IllegalArgumentException if the color is <code>null</code>.
     * @see #getOutlineColor()
     */
    void setOutlineColor(Color color);

    /**
     * Indicates the line width (in pixels) used when rendering the shape's outline. The returned value is guaranteed to
     * be a positive floating-point value.
     *
     * @return the line width in pixels.
     *
     * @see #setOutlineWidth(double)
     */
    double getOutlineWidth();

    /**
     * Specifies the line width (in pixels) to use when rendering the shape's outline. The specified <code>width</code>
     * must be a positive floating-point value. Outline with may be limited by an implementation-defined maximum during
     * rendering. The maximum width is typically greater than 10, but is only guaranteed to be no less than 1.
     *
     * @param width the line width in pixels.
     *
     * @throws IllegalArgumentException if width is less than or equal to zero.
     * @see #getOutlineWidth()
     */
    void setOutlineWidth(double width);
}
