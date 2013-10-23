/* Copyright (C) 2001, 2012 United States Government as represented by 
the Administrator of the National Aeronautics and Space Administration. 
All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.exception.WWUnrecognizedException;
import gov.nasa.worldwind.util.Logging;

/**
 * Color represents an RGBA color where each component is represented as a floating-point value in the range [0.0, 1.0],
 * where 0.0 indicates zero intensity and 1.0 indicates full intensity. The four components of color are read-write, and
 * are stored as double-precision floating-point values. GpuProgram provides a convenience method {@link GpuProgram#loadUniformColor(String, Color)} for loading
 * a color into an OpenGL ES shader uniform variable as a vec4.
 * <p/>
 * <strong>Mutability</strong>
 * <p/>
 * Color is mutable and provides public read and write access to each of its four components as properties <code>r</code>, <code>g</code>, <code>b</code>, and
 * <code>a</code>. Additionally, color provides an overloaded <code>set</code> method for specifying all RGB or RGBA components in bulk. It is important to
 * avoid assumptions that the application can know when a color changes. In particular, rendering code should should be written to automatically display changes
 * to any colors it depends on.
 * <p/>
 * <p/>
 * <strong>Color Space</strong>
 * <p/>
 * Color's four RGBA components are assumed to exist in the standard RGBA color space. Commonly used colors are provided via static methods that return a new
 * color instance who's components are configured to represent the specified RGB color. Since color is mutable the returned instances are unique, and therefore
 * should not be assumed to be constant.
 * <p/>
 * Color makes no attempts to represent a premultiplied RGBA color, but does provide the {@link #premultiply()} method for converting a color from the standard
 * RGBA color space to the premultiplied RGBA color space. color does not track whether an instance has been premultiplied; it is the responsibility of the
 * application to do so.
 * <p/>
 * <strong>Color Int</strong>
 * <p/>
 * A color int refers to an RGB or ARGB color specified by a 32-bit packed int. Each component is represented by an 8-bit value in the range [0, 255] where 0
 * indicates zero intensity and 255 indicates full intensity. The components are understood to be packed as follows: alpha in bits 24-31, red in bits 16-23,
 * green in bits 8-15, and blue in bits 0-7. This format is compatible with Android's {@link android.graphics.Color} class.
 * <p/>
 * Colors can be converted between floating-point RGBA colors and packed 32-bit color ints, and vice versa. This is done by mapping each 8-bit component in the
 * range [0, 255] to the range [0.0, 1.0].
 * <p/>
 * Converting a component from 8-bit to floating-point is accomplished by dividing the value by 255.
 * <p/>
 * Converting a component from floating-point to 8-bit is accomplished by multiplying the value by 255, adding 0.5, then taking the floor of the result. The
 * additional step of adding 0.5 ensures that rounding errors do not produce values that are too small. For example, if the 8-bit value 1 is converted to
 * floating-point by dividing by 255 then converted back to 8-bit by multiplying by 255, the result is 0. This is because the result of 255 * (1/255) is
 * slightly less than 1, which results in 0 after taking the floor. Adding 0.5 before taking the floor compensates for limitations in floating point precision.
 * <p/>
 * <strong>Color String</strong>
 * <p/>
 * A color string refers to an ABGR color specified by an eight character a hexadecimal string. Each component is represented by an 8-bit hexadecimal value in
 * the range [00, FF] where 0 indicates zero intensity and FF indicates full intensity. The components are understood to be tightly packed in the orger
 * AARRGGBB. This format is compatible with KML's color elements. The color string may have an optional prefix, and therefore as three valid forms:
 * <code><ul> <li>AABBGGRR</li> <li>0xAABBGGRR</li> or <li>#AABBGGRR</li></ul><code>
 * <p/>
 * Colors can be converted between floating-point RGBA colors and ABGR hexadecimal color strings, and vice versa. This
 * is done by mapping each 8-bit hexadecimal component in the range [00, FF] to the range [0.0, 1.0].
 * <p/>
 * Converting a component from hexadecimal 8-bit to floating-point is accomplished converting from hexadecimal to
 * decimal, then dividing the value by 255.
 * <p/>
 * Converting a component from floating-point to hexadecimal 8-bit is accomplished by multiplying the value by 255,
 * adding 0.5, taking the floor of the result, then coverting from decimal to hexadecimal. The additional step of adding
 * 0.5 ensures that rounding errors do not produce values that are too small. For example, if the 8-bit value 1 is
 * converted to floating-point by dividing by 255 then converted back to 8-bit by multiplying by 255, the result is 0.
 * This is because the result of 255 * (1/255) is slightly less than 1, which results in 0 after taking the floor.
 * Adding 0.5 before taking the floor compensates for limitations in floating point precision.
 * 
 * @author dcollins
 * @version $Id: Color.java 821 2012-09-28 03:12:58Z dcollins $
 */
public class Color {
	/**
	 * The color's red component as a floating-point value in the range [0.0, 1.0], where 0.0 indicates zero intensity
	 * and 1.0 indicates full intensity. Initially 0.0.
	 */
	public double r;
	/**
	 * The color's green component as a floating-point value in the range [0.0, 1.0], where 0.0 indicates zero intensity
	 * and 1.0 indicates full intensity. Initially 0.0.
	 */
	public double g;
	/**
	 * The color's blue component as a floating-point value in the range [0.0, 1.0], where 0.0 indicates zero intensity
	 * and 1.0 indicates full intensity. Initially 0.0.
	 */
	public double b;
	/**
	 * The color's alpha component as a floating-point value in the range [0.0, 1.0], where 0.0 indicates zero intensity
	 * and 1.0 indicates full intensity. Initially 1.0.
	 */
	public double a = 1;

	/** Creates a new color representing black. The color's RGBA components are set to (0.0, 0.0, 0.0, 1.0). */
	public Color() {
	}

	/**
	 * Creates a new color with the RGBA components from the specified color. This does not retain any reference to the
	 * specified color, or modify it in any way. The color's RGBA components are copied into this color's RGBA
	 * components.
	 * 
	 * @param color
	 *            the color's RGBA components as a color.
	 * @throws IllegalArgumentException
	 *             if the color is <code>null</code>.
	 */
	public Color(Color color) {
		if (color == null) {
			String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.r = color.r;
		this.g = color.g;
		this.b = color.b;
		this.a = color.a;
	}

	/**
	 * Creates a new opaque color with the specified RGB components. Each component is floating-point value in the range
	 * [0.0, 1.0], where 0.0 indicates zero intensity and 1.0 indicates full intensity. The new color's alpha component
	 * is set to 1.0. The behavior for values outside of this range is undefined.
	 * 
	 * @param r
	 *            the color's red component as a floating-point value in the range [0.0, 1.0].
	 * @param g
	 *            the color's green component as a floating-point value in the range [0.0, 1.0].
	 * @param b
	 *            the color's blue component as a floating-point value in the range [0.0, 1.0].
	 */
	public Color(double r, double g, double b) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = 1;
	}

	/**
	 * Creates a new color with the specified RGBA components. Each component is floating-point value in the range [0.0,
	 * 1.0], where 0.0 indicates zero intensity and 1.0 indicates full intensity. The behavior for values outside of
	 * this range is undefined.
	 * 
	 * @param r
	 *            the color's red component as a floating-point value in the range [0.0, 1.0].
	 * @param g
	 *            the color's green component as a floating-point value in the range [0.0, 1.0].
	 * @param b
	 *            the color's blue component as a floating-point value in the range [0.0, 1.0].
	 * @param a
	 *            the color's alpha component as a floating-point value in the range [0.0, 1.0].
	 */
	public Color(double r, double g, double b, double a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;
	}

	/**
	 * Creates a new color from a packed 32-bit ARGB color int. See the section above on <i>Color Int</i> for more
	 * information on the color int format. Each of the four components are converted from 8-bit to floating-point and
	 * stored in this color's components.
	 * 
	 * @param colorInt
	 *            the color's ARGB components as a packed 32-bit color int.
	 */
	public Color(int colorInt) {
		this.r = ((colorInt >> 16) & 0xFF) / 255.0;
		this.g = ((colorInt >> 8) & 0xFF) / 255.0;
		this.b = (colorInt & 0xFF) / 255.0;
		this.a = (colorInt >>> 24) / 255.0;
	}

	/**
	 * Creates a new color from a packed 32-bit RGB color int. See the section above on <i>Color Int</i> for more
	 * information on the color int format. Each of the three RGB components are converted from 8-bit to floating-point
	 * and stored in this color's components.
	 * <p/>
	 * If hasAlpha is <code>true</code> this color's alpha component is set using bits 24-31 of the color int. Otherwise, this ignores bits 24-31 and this
	 * color's alpha component is set to 1.0.
	 * 
	 * @param colorInt
	 *            the color's RGB or ARGB components as a packed 32-bit color int.
	 * @param hasAlpha
	 *            <code>true</code> to indicate that this color's alpha component should be set from the colorInt's
	 *            alpha, or <code>false</code> to ignore the colorInt's alpha and set this color's alpha to 1.0.
	 */
	public Color(int colorInt, boolean hasAlpha) {
		this.r = ((colorInt >> 16) & 0xFF) / 255.0;
		this.g = ((colorInt >> 8) & 0xFF) / 255.0;
		this.b = (colorInt & 0xFF) / 255.0;
		this.a = hasAlpha ? (colorInt >>> 24) / 255.0 : 1.0;
	}

	/**
	 * Creates a new color from a hexadecimal ABGR color string. See the section above on <i>Color String</i> for more
	 * information on the color string format. Each of the four RGBA components are converted from 8-bit hexadecimal to
	 * floating-point and stored in this color's components.
	 * 
	 * @param colorString
	 *            the color's ABGR components as a hexadecimal color string.
	 * @throws IllegalArgumentException
	 *             if the color string is <code>null</code>.
	 * @throws WWUnrecognizedException
	 *             or if the color string is in an unrecognized format, and cannot be decoded.
	 */
	public Color(String colorString) {
		if (colorString == null) {
			String msg = Logging.getMessage("nullValue.StringIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (colorString.startsWith("#")) {
			colorString = colorString.replaceFirst("#", "0x");
		} else if (!colorString.startsWith("0x") && !colorString.startsWith("0X")) {
			colorString = "0x" + colorString;
		}

		// The hexadecimal representation for an RGBA color can result in a value larger than Integer.MAX_VALUE
		// (for example, 0XFFFF). Therefore we decode the string as a long, then keep only the lower four bytes.
		Long longValue;
		try {
			longValue = Long.parseLong(colorString.substring(2), 16);
		} catch (NumberFormatException e) {
			String msg = Logging.getMessage("generic.ConversionError", colorString);
			Logging.error(msg, e);
			throw new WWUnrecognizedException(msg, e);
		}

		int i = (int) (longValue & 0xFFFFFFFFL);

		this.r = (i & 0xFF) / 255.0;
		this.g = ((i >> 8) & 0xFF) / 255.0;
		this.b = ((i >> 16) & 0xFF) / 255.0;
		this.a = ((i >> 24) & 0xFF) / 255.0;
	}

	/**
	 * Returns a new color who's four components are set to zero (0.0, 0.0, 0.0, 0.0).
	 * 
	 * @return a transparent color.
	 */
	public static Color transparent() {
		return new Color(0.0, 0.0, 0.0, 0.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color black (0.0, 0.0, 0.0). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color black.
	 */
	public static Color black() {
		return new Color(0.0, 0.0, 0.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color dark gray (0.25, 0.25, 0.25). The returned
	 * color's alpha component is set to 1.0.
	 * 
	 * @return the color dark gray.
	 */
	public static Color darkGray() {
		return new Color(0.25, 0.25, 0.25, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color gray (0.5, 0.5, 0.5). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color gray.
	 */
	public static Color gray() {
		return new Color(0.5, 0.5, 0.5, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color light gray (0.753, 0.753, 0.753). The
	 * returned color's alpha component is set to 1.0.
	 * 
	 * @return the color light gray.
	 */
	public static Color lightGray() {
		return new Color(0.753, 0.753, 0.753, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color white (1.0, 1.0, 1.0). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color white.
	 */
	public static Color white() {
		return new Color(1.0, 1.0, 1.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color red (1.0, 0.0, 0.0). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color red.
	 */
	public static Color red() {
		return new Color(1.0, 0.0, 0.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color pink (1.0, 0.686, 0.686). The returned
	 * color's alpha component is set to 1.0.
	 * 
	 * @return the color pink.
	 */
	public static Color pink() {
		return new Color(1.0, 0.686, 0.686, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color orange (1.0, 0.784, 0.0). The returned
	 * color's alpha component is set to 1.0.
	 * 
	 * @return the color orange.
	 */
	public static Color orange() {
		return new Color(1.0, 0.784, 0.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color yellow (1.0, 1.0, 0.0). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color yellow.
	 */
	public static Color yellow() {
		return new Color(1.0, 1.0, 0.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color green (0.0, 1.0, 0.0). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color green.
	 */
	public static Color green() {
		return new Color(0.0, 1.0, 0.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color magenta (1.0, 0.0, 1.0). The returned
	 * color's alpha component is set to 1.0.
	 * 
	 * @return the color magenta.
	 */
	public static Color magenta() {
		return new Color(1.0, 0.0, 1.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color cyan (0.0, 1.0, 1.0). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color cyan.
	 */
	public static Color cyan() {
		return new Color(0.0, 1.0, 1.0, 1.0);
	}

	/**
	 * Returns a new opaque color who's RGB components are set to the color blue (0.0, 0.0, 1.0). The returned color's
	 * alpha component is set to 1.0.
	 * 
	 * @return the color blue.
	 */
	public static Color blue() {
		return new Color(0.0, 0.0, 1.0, 1.0);
	}

	/**
	 * Generates a random RGB color between black and white. The returned color's alpha component is 1.0.
	 * 
	 * @return a new color with random red, green and blue components.
	 */
	public static Color randomColor() {
		return new Color(Math.random(), Math.random(), Math.random(), 1.0);
	}

	/**
	 * Generates a random RGB color by scaling each of the red, green and blue components of a specified color with
	 * independent random numbers. This does not retain any reference to the specified color, or modify it in any way.
	 * The returned color's RGB components can be any value between the specified color (or white if the color is <code>null</code>) and black. The returned
	 * color's alpha component is not scaled and is copied into the new
	 * color, or is set to 1.0 if the specified color is <code>null</code>.
	 * <p/>
	 * The returned color is consistent with KML's random colorMode. Unless there's a reason to use a specific input color, the best color to use is white.
	 * 
	 * @param color
	 *            the color to generate a random color from. If <code>null</code>, the color white (1.0, 1.0, 1.0) is
	 *            used.
	 * @return a new RGB color with random red, green and blue components.
	 */
	public static Color randomColor(Color color) {
		double r;
		double g;
		double b;
		double a;

		if (color != null) {
			r = color.r;
			g = color.g;
			b = color.b;
			a = color.a;
		} else {
			r = 1.0;
			g = 1.0;
			b = 1.0;
			a = 1.0;
		}

		return new Color(r * Math.random(), g * Math.random(), b * Math.random(), a);
	}

	/**
	 * Creates a packed 32-bit RGB color int from three separate values for each of the red, green, and blue components.
	 * See the section above on <i>Color Int</i> for more information on the color int format. Each component is
	 * interpreted as an 8-bit value in the range [0, 255] where 0 indicates zero intensity and 255 indicates full
	 * intensity. The behavior for values outside of this range is undefined.
	 * <p/>
	 * The bits normally reserved for alpha in the returned value are filled with 0.
	 * 
	 * @param r
	 *            the color's red component as an 8-bit value in the range [0, 255].
	 * @param g
	 *            the color's green component as an 8-bit value in the range [0, 255].
	 * @param b
	 *            the color's blue component as an 8-bit value in the range [0, 255].
	 * @return a packed 32-bit color int representing the specified RGB color.
	 */
	public static int makeColorInt(int r, int g, int b) {
		return ((0xFF & r) << 16) | ((0xFF & g) << 8) | (0xFF & b);
	}

	/**
	 * Creates a packed 32-bit ARGB color int from four separate values for each of the red, green, blue, and alpha
	 * components. See the section above on <i>Color Int</i> for more information on the color int format. Each
	 * component is interpreted as an 8-bit value in the range [0, 255] where 0 indicates zero intensity and 255
	 * indicates full intensity. The behavior for values outside of this range is undefined.
	 * 
	 * @param r
	 *            the color's red component as an 8-bit value in the range [0, 255].
	 * @param g
	 *            the color's green component as an 8-bit value in the range [0, 255].
	 * @param b
	 *            the color's blue component as an 8-bit value in the range [0, 255].
	 * @param a
	 *            the color's alpha component as an 8-bit value in the range [0, 255].
	 * @return a packed 32-bit color int representing the specified RGBA color.
	 */
	public static int makeColorInt(int r, int g, int b, int a) {
		return ((0xFF & a) << 24) | ((0xFF & r) << 16) | ((0xFF & g) << 8) | (0xFF & b);
	}

	/**
	 * Creates a hexadecimal ABGR color string from four separate values for each of the red, green, blue, and alpha
	 * components. See the section above on <i>Color String</i> for more information on the color string format. Each
	 * component is interpreted as an 8-bit value in the range [0, 255] where 0 indicates zero intensity and 255
	 * indicates full intensity. The behavior for values outside of this range is undefined.
	 * 
	 * @param r
	 *            the color's red component as an 8-bit value in the range [0, 255].
	 * @param g
	 *            the color's green component as an 8-bit value in the range [0, 255].
	 * @param b
	 *            the color's blue component as an 8-bit value in the range [0, 255].
	 * @param a
	 *            the color's alpha component as an 8-bit value in the range [0, 255].
	 * @return a hexadecimal ABGR color string representing the specified RGBA color.
	 */
	public static String makeColorString(int r, int g, int b, int a) {
		int abgrColorInt = ((0xFF & a) << 24) | ((0xFF & b) << 16) | ((0xFF & g) << 8) | (0xFF & r);

		return String.format("%#08X", abgrColorInt);
	}

	/**
	 * Returns the value of the red component from the specified packed 32-bit ARGB color int. See the section above on
	 * <i>Color Int</i> for more information on the color int format. The returned component is an 8-bit value in the
	 * range [0, 255] where 0 indicates zero intensity and 255 indicates full intensity.
	 * 
	 * @param colorInt
	 *            the packed 32-bit color int representing an ARGB color.
	 * @return an 8-bit value in the range [0, 255] representing the red component from the specified ARGB color.
	 */
	public static int getColorIntRed(int colorInt) {
		return (colorInt >> 16) & 0xFF;
	}

	/**
	 * Returns the value of the green component from the specified packed 32-bit ARGB color int. See the section above
	 * on <i>Color Int</i> for more information on the color int format. The returned component is an 8-bit value in the
	 * range [0, 255] where 0 indicates zero intensity and 255 indicates full intensity.
	 * 
	 * @param colorInt
	 *            the packed 32-bit color int representing an ARGB color.
	 * @return an 8-bit value in the range [0, 255] representing the green component from the specified ARGB color.
	 */
	public static int getColorIntGreen(int colorInt) {
		return (colorInt >> 8) & 0xFF;
	}

	/**
	 * Returns the value of the blue component from the specified packed 32-bit ARGB color int. See the section above on
	 * <i>Color Int</i> for more information on the color int format. The returned component is an 8-bit value in the
	 * range [0, 255] where 0 indicates zero intensity and 255 indicates full intensity.
	 * 
	 * @param colorInt
	 *            the packed 32-bit color int representing an ARGB color.
	 * @return an 8-bit value in the range [0, 255] representing the blue component from the specified ARGB color.
	 */
	public static int getColorIntBlue(int colorInt) {
		return colorInt & 0xFF;
	}

	/**
	 * Returns the value of the alpha component from the specified packed 32-bit ARGB color int. See the section above
	 * on <i>Color Int</i> for more information on the color int format. The returned component is an 8-bit value in the
	 * range [0, 255] where 0 indicates zero intensity and 255 indicates full intensity.
	 * 
	 * @param colorInt
	 *            the packed 32-bit color int representing an ARGB color.
	 * @return an 8-bit value in the range [0, 255] representing the alpha component from the specified ARGB color.
	 */
	public static int getColorIntAlpha(int colorInt) {
		return colorInt >>> 24;
	}

	/**
	 * Computes the component-wise linear interpolation of the specified colors and stores the output in the result
	 * parameter. This does not retain any reference to the specified colors, or modify them in any way.
	 * <p/>
	 * The interpolation factor amount is a floating-point value in the range [0.0, 1.0] which defines the weight given to each color. Each of the RGBA
	 * components in the colors are interpolated according to the function: <code>(1 -
	 * amount) * ca + amount * cb</code>, where ca and cb are components of lhs and rhs, respectively.
	 * <p/>
	 * If this method throws an IllegalArgumentException, the result is left unchanged.
	 * 
	 * @param amount
	 *            the interpolation factor as a floating-point value in the range [0.0, 1.0].
	 * @param lhs
	 *            the first color.
	 * @param rhs
	 *            the second color.
	 * @param result
	 *            contains the linear interpolation of lhs and rhs after this method exits.
	 * @return a reference to the specified result, which contains the linear interpolation of lhs and rhs.
	 * @throws IllegalArgumentException
	 *             if either <code>lhs</code> or <code>rhs</code> are <code>null</code>.
	 */
	public static Color interpolate(double amount, Color lhs, Color rhs, Color result) {
		if (lhs == null) {
			String msg = Logging.getMessage("nullValue.LhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhs == null) {
			String msg = Logging.getMessage("nullValue.RhsIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (result == null) {
			String msg = Logging.getMessage("nullValue.ResultIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double t = (amount < 0 ? 0 : (amount > 1 ? 1 : amount));

		result.r = lhs.r + t * (rhs.r - lhs.r);
		result.g = lhs.g + t * (rhs.g - lhs.g);
		result.b = lhs.b + t * (rhs.b - lhs.b);
		result.a = lhs.a + t * (rhs.a - lhs.a);

		return result;
	}

	/**
	 * Sets this color's RGBA components to those of the specified color. This does not retain any reference to the
	 * specified color, or modify it in any way. The color's RGBA components are copied into this color's RGBA
	 * components.
	 * <p/>
	 * If this method throws an IllegalArgumentException, this color is left unchanged.
	 * 
	 * @param color
	 *            the new RGBA components as a color.
	 * @return a reference to this color.
	 * @throws IllegalArgumentException
	 *             if the color is <code>null</code>.
	 */
	public Color set(Color color) {
		if (color == null) {
			String msg = Logging.getMessage("nullValue.ColorIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.r = color.r;
		this.g = color.g;
		this.b = color.b;
		this.a = color.a;

		return this;
	}

	/**
	 * Sets this color to the specified RGB components. Each component is floating-point value in the range [0.0, 1.0],
	 * where 0.0 indicates zero intensity and 1.0 indicates full intensity. This color's alpha component is left
	 * unchanged. The behavior for values outside of this range is undefined.
	 * 
	 * @param r
	 *            the new red component as a floating-point value in the range [0.0, 1.0].
	 * @param g
	 *            the new green component as a floating-point value in the range [0.0, 1.0].
	 * @param b
	 *            the new blue component as a floating-point value in the range [0.0, 1.0].
	 * @return a reference to this color.
	 */
	public Color set(double r, double g, double b) {
		this.r = r;
		this.g = g;
		this.b = b;

		return this;
	}

	/**
	 * Sets this color to the specified RGBA components. Each component is floating-point value in the range [0.0, 1.0],
	 * where 0.0 indicates zero intensity and 1.0 indicates full intensity. The behavior for values outside of this
	 * range is undefined.
	 * 
	 * @param r
	 *            the new red component as a floating-point value in the range [0.0, 1.0].
	 * @param g
	 *            the new green component as a floating-point value in the range [0.0, 1.0].
	 * @param b
	 *            the new blue component as a floating-point value in the range [0.0, 1.0].
	 * @param a
	 *            the new alpha component as a floating-point value in the range [0.0, 1.0].
	 * @return a reference to this color.
	 */
	public Color set(double r, double g, double b, double a) {
		this.r = r;
		this.g = g;
		this.b = b;
		this.a = a;

		return this;
	}

	/**
	 * Sets this color to the ARGB color specified by a packed 32-bit ARGB color int. See the section above on <i>Color
	 * Int</i> for more information on the color int format. Each of the four components are converted from 8-bit to
	 * floating-point and stored in this color's components.
	 * 
	 * @param colorInt
	 *            the color's ARGB components as a packed 32-bit color int.
	 * @return a reference to this color.
	 */
	public Color set(int colorInt) {
		this.r = ((colorInt >> 16) & 0xFF) / 255.0;
		this.g = ((colorInt >> 8) & 0xFF) / 255.0;
		this.b = (colorInt & 0xFF) / 255.0;
		this.a = (colorInt >>> 24) / 255.0;

		return this;
	}

	/**
	 * Sets this color to the RGB or ARGB color specified by a packed 32-bit RGB color int. See the section above on
	 * <i>Color Int</i> for more information on the color int format. Each of the three RGB components are converted
	 * from 8-bit to floating-point and stored in this color's components.
	 * <p/>
	 * If hasAlpha is <code>true</code> this color's alpha component is set using bits 24-31 of the color int. Otherwise, this ignores bits 24-31 and this
	 * color's alpha component is set to 1.0.
	 * 
	 * @param colorInt
	 *            the color's RGB or ARGB components as a packed 32-bit color int.
	 * @param hasAlpha
	 *            <code>true</code> to indicate that this color's alpha component should be set from the colorInt's
	 *            alpha, or <code>false</code> to ignore the colorInt's alpha and leave this color's alpha
	 *            unchanged.
	 * @return a reference to this color.
	 */
	public Color set(int colorInt, boolean hasAlpha) {
		this.r = ((colorInt >> 16) & 0xFF) / 255.0;
		this.g = ((colorInt >> 8) & 0xFF) / 255.0;
		this.b = (colorInt & 0xFF) / 255.0;

		if (hasAlpha) {
			this.a = (colorInt >>> 24) / 255.0;
		}

		return this;
	}

	/**
	 * Converts this RGBA color from the standard RGBA color space to the premultiplied RGBA color space by multiplying
	 * the red, green, and blue components by the alpha component. It is assumed that this color is in the standard RGBA
	 * color space before this method is called. Color does not track whether an instance has been premultiplied; it is
	 * the responsibility of the application to do so.
	 * 
	 * @return a reference to this color.
	 */
	public Color premultiply() {
		this.r *= this.a;
		this.g *= this.a;
		this.b *= this.a;

		return this;
	}

	/**
	 * Compares this color with the specified instance and indicates if they are equal. This returns <code>true</code> if the specified instance is a color and
	 * its four components are equivalent to this color's components, and
	 * returns <code>false</code> otherwise.
	 * 
	 * @param o
	 *            the object to compare this instance with.
	 * @return <code>true</code> if the specified object is equal to this object, and <code>false</code> otherwise.
	 */
	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;

		Color that = (Color) o;
		return this.r == that.r && this.g == that.g && this.b == that.b && this.a == that.a;
	}

	/** {@inheritDoc} */
	public int hashCode() {
		int result;
		long tmp;
		tmp = Double.doubleToLongBits(this.r);
		result = (int) (tmp ^ (tmp >>> 32));
		tmp = Double.doubleToLongBits(this.g);
		result = 29 * result + (int) (tmp ^ (tmp >>> 32));
		tmp = Double.doubleToLongBits(this.b);
		result = 29 * result + (int) (tmp ^ (tmp >>> 32));
		tmp = Double.doubleToLongBits(this.a);
		result = 29 * result + (int) (tmp ^ (tmp >>> 32));
		return result;
	}

	/**
	 * Returns a string representation of this RGBA color in the format "(r, g, b, a)". Where each component is
	 * represented as a string in double-precision.
	 * 
	 * @return a string representation of this RGBA color.
	 */
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(this.r).append(", ");
		sb.append(this.g).append(", ");
		sb.append(this.b).append(", ");
		sb.append(this.a);
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Returns a packed 32-bit ARGB color int representation of this RGBA color. See the section above on <i>Color
	 * Int</i> for more information on the color int format. Each of the four components are converted from
	 * floating-point to 8-bit and stored in the returned color int.
	 * 
	 * @return a packed 32-bit color int representing this RGBA color.
	 */
	public int toColorInt() {
		// Convert each component from a floating-point value in the range [0.0, 1.0] to an 8-bit value in the range
		// [0, 255]. See the above class comment on Color Int for why we add 0.5 to each value before rounding down.
		return makeColorInt((int) (255 * this.r + 0.5), (int) (255 * this.g + 0.5), (int) (255 * this.b + 0.5), (int) (255 * this.a + 0.5));
	}

	/**
	 * Returns a hexadecimal ABGR color string representation of this RGBA color. See the section above on <i>Color
	 * String</i> for more information on the color string format. Each of the four components are converted from
	 * floating-point to 8-bit hexadecimal and composed as a string in the pattern #AABBGGRR.
	 * 
	 * @return a hexadecimal ABGR color string representing this RGBA color.
	 */
	public String toColorString() {
		// Convert each component from a floating-point value in the range [0.0, 1.0] to an 8-bit value in the range
		// [0, 255]. See the above class comment on Color String for why we add 0.5 to each value before rounding down.
		return makeColorString((int) (255 * this.r + 0.5), (int) (255 * this.g + 0.5), (int) (255 * this.b + 0.5), (int) (255 * this.a + 0.5));
	}

	/**
	 * Writes this colors RGBA components into the specified array starting at the specified offset. The RGBA components
	 * are copied into an index of the specified array starting at offset and increasing by 1 for each component. This
	 * stores component values as 32-bit floating-point value in the range [0.0, 1.0], where 0.0 indicates zero
	 * intensity and 1.0 indicates full intensity.
	 * <p/>
	 * This throws an exception if the array has insufficient length to store four elements starting at offset.
	 * 
	 * @param array
	 *            the array this color's RGBA components are stored in.
	 * @param offset
	 *            the starting index, which receives the red component value.
	 * @throws IllegalArgumentException
	 *             if the array is <code>null</code>, if the array length is less than 4, if the
	 *             offset is less than 0, or if the offset specifies an index that would cause the
	 *             color to extend beyond the end of the array.
	 */
	public void toArray4f(float[] array, int offset) {
		if (array == null) {
			String msg = Logging.getMessage("nullValue.ArrayIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (array.length < 4) {
			String msg = Logging.getMessage("generic.ArrayInvalidLength", array.length);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (offset < 0 || offset + 4 > array.length) {
			String msg = Logging.getMessage("generic.OffsetIsInvalid", offset);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		array[offset] = (float) this.r;
		array[offset + 1] = (float) this.g;
		array[offset + 2] = (float) this.b;
		array[offset + 3] = (float) this.a;
	}
}
