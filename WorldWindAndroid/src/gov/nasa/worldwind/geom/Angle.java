/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.geom;

import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: Angle.java 733 2012-09-02 17:15:09Z dcollins $
 */
public class Angle implements Comparable<Angle>
{
	/** Represents an angle of zero degrees */
	public final static Angle ZERO = Angle.fromDegrees(0);

	/** Represents a right angle of positive 90 degrees */
	public final static Angle POS90 = Angle.fromDegrees(90);

	/** Represents a right angle of negative 90 degrees */
	public final static Angle NEG90 = Angle.fromDegrees(-90);

	/** Represents an angle of positive 180 degrees */
	public final static Angle POS180 = Angle.fromDegrees(180);

	/** Represents an angle of negative 180 degrees */
	public final static Angle NEG180 = Angle.fromDegrees(-180);

	/** Represents an angle of positive 360 degrees */
	public final static Angle POS360 = Angle.fromDegrees(360);

	/** Represents an angle of negative 360 degrees */
	public final static Angle NEG360 = Angle.fromDegrees(-360);

	/** Represents an angle of 1 minute */
	public final static Angle MINUTE = Angle.fromDegrees(1d / 60d);

	/** Represents an angle of 1 second */
	public final static Angle SECOND = Angle.fromDegrees(1d / 3600d);


    protected static final double DEGREES_TO_RADIANS = Math.PI / 180d;
    protected static final double RADIANS_TO_DEGREES = 180d / Math.PI;

    public double degrees;
    public double radians;

    public Angle()
    {
    }

    protected Angle(double degrees, double radians)
    {
        this.degrees = degrees;
        this.radians = radians;
    }

	public double getDegrees() {
		return degrees;
	}

	public double getRadians() {
		return radians;
	}

    /**
     * Obtains an angle from a specified number of degrees.
     *
     * @param degrees the size in degrees of the angle to be obtained
     *
     * @return a new angle, whose size in degrees is given by <code>degrees</code>
     */
    public static Angle fromDegrees(double degrees)
    {
        return new Angle(degrees, DEGREES_TO_RADIANS * degrees);
    }

    /**
     * Obtains an angle from a specified number of radians.
     *
     * @param radians the size in radians of the angle to be obtained.
     *
     * @return a new angle, whose size in radians is given by <code>radians</code>.
     */
    public static Angle fromRadians(double radians)
    {
        return new Angle(RADIANS_TO_DEGREES * radians, radians);
    }

    public static double normalizedDegreesLatitude(double degrees)
    {
        double lat = degrees % 180;
        return lat > 90 ? 180 - lat : lat < -90 ? -180 - lat : lat;
    }

    public static double normalizedDegreesLongitude(double degrees)
    {
        double lon = degrees % 360;
        return lon > 180 ? lon - 360 : lon < -180 ? 360 + lon : lon;
    }

    public Angle copy()
    {
        return new Angle(this.degrees, this.radians);
    }

    public Angle set(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.degrees = angle.degrees;
        this.radians = angle.radians;

        return this;
    }

    public void setDegrees(double degrees)
    {
        this.degrees = degrees;
        this.radians = DEGREES_TO_RADIANS * degrees;
    }

    public void setRadians(double radians)
    {
        this.degrees = RADIANS_TO_DEGREES * radians;
        this.radians = radians;
    }

	public void setDegreesF(float degrees) {
		setDegrees(degrees);
	}

	public void setRadiansF(float radians) {
		setRadians(radians);
	}

	public float getDegreesF() {
		return (float)degrees;
	}

	public float getRadiansF() {
		return (float)radians;
	}

    /**
     * Obtains the sine of this angle.
     *
     * @return the trigonometric sine of this angle.
     */
    public double sin()
    {
        return Math.sin(this.radians);
    }

    public double sinHalfAngle()
    {
        return Math.sin(0.5 * this.radians);
    }

    /**
     * Obtains the cosine of this angle.
     *
     * @return the trigonometric cosine of this angle.
     */
    public double cos()
    {
        return Math.cos(this.radians);
    }

    public double cosHalfAngle()
    {
        return Math.cos(0.5 * this.radians);
    }

    public double tan()
    {
        return Math.tan(this.radians);
    }

    /**
     * Obtains the tangent of half of this angle.
     *
     * @return the trigonometric tangent of half of this angle.
     */
    public double tanHalfAngle()
    {
        return Math.tan(0.5 * this.radians);
    }

    /**
     * Computes the sum of this angle and the specified angle, then sets this angle to the result.
     *
     * @param angle the angle to add to this one.
     *
     * @return a reference to this angle.
     *
     * @throws IllegalArgumentException if the specified angle is <code>null</code>.
     */
    public Angle add(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return Angle.fromDegrees(this.degrees + angle.degrees);
    }

    /**
     * Divides this angle by <code>divisor</code>. This angle remains unchanged. The result is returned as a new angle.
     * Behaviour is undefined if <code>divisor</code> equals zero.
     *
     * @param divisor the number to be divided by.
     *
     * @return a new angle equivalent to this angle divided by <code>divisor</code>.
     */
    public Angle divide(double divisor)
    {
        return Angle.fromDegrees(this.degrees / divisor);
    }

    public Angle addDegrees(double degrees)
    {
        return Angle.fromDegrees(this.degrees + degrees);
    }

    public Angle addRadians(double radians)
    {
        return Angle.fromRadians(this.radians + radians);
    }

    /**
     * Computes the sum of this angle and the specified angle, then sets this angle to the result.
     *
     * @param angle the angle to add to this one.
     *
     * @return a reference to this angle.
     *
     * @throws IllegalArgumentException if the specified angle is <code>null</code>.
     */
    public Angle addAndSet(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setDegrees(this.degrees + angle.degrees);
		return this;
    }

    public Angle addAndSet(Angle lhs, Angle rhs)
    {
        if (lhs == null)
        {
            String msg = Logging.getMessage("nullValue.LhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (rhs == null)
        {
            String msg = Logging.getMessage("nullValue.RhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setDegrees(lhs.degrees + rhs.degrees);
		return this;
    }

    public Angle addDegreesAndSet(double degrees)
    {
        this.setDegrees(this.degrees + degrees);
		return this;
    }

    public Angle addRadiansAndSet(double radians)
    {
        this.setRadians(this.radians + radians);
		return this;
    }

    public Angle subtract(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return Angle.fromDegrees(this.degrees - angle.degrees);
    }

    public Angle subtractDegrees(double degrees)
    {
        return Angle.fromDegrees(this.degrees - degrees);
    }

    public Angle subtractRadians(double radians)
    {
        return Angle.fromRadians(this.radians - radians);
    }

    /**
     * Computes the difference of these two angles, then sets this angle to the result.
     *
     * @param angle the angle to subtract from this angle.
     *
     * @return a reference to this angle.
     *
     * @throws IllegalArgumentException if the specified angle is <code>null</code>.
     */
    public Angle subtractAndSet(Angle angle)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setDegrees(this.degrees - angle.degrees);
		return this;
    }

    public Angle subtractAndSet(Angle lhs, Angle rhs)
    {
        if (lhs == null)
        {
            String msg = Logging.getMessage("nullValue.LhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (rhs == null)
        {
            String msg = Logging.getMessage("nullValue.RhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setDegrees(lhs.degrees - rhs.degrees);
		return this;
    }

    public Angle subtractDegreesAndSet(double degrees)
    {
        this.setDegrees(this.degrees - degrees);
		return this;
    }

    public Angle subtractRadiansAndSet(double radians)
    {
        this.setRadians(this.radians - radians);
		return this;
    }

    /**
     * Multiplies this angle by <code>multiplier</code>. This angle remains unchanged. The result is returned as a new
     * angle.
     *
     * @param value a scalar by which this angle is multiplied.
     *
     * @return a new angle whose size equals this angle's size multiplied by <code>multiplier</code>.
     */
    public Angle multiply(double value)
    {
        return Angle.fromDegrees(this.degrees * value);
    }

    public Angle multiplyAndSet(double value)
    {
        this.setDegrees(this.degrees * value);
		return this;
    }

    public Angle multiplyAndSet(Angle angle, double value)
    {
        if (angle == null)
        {
            String msg = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.setDegrees(angle.degrees * value);
		return this;
    }

    /**
     * Obtains the average of two angles. This method is commutative, so <code>midAngle(m, n)</code> and
     * <code>midAngle(n, m)</code> are equivalent.
     *
     * @param lhs the first angle.
     * @param rhs the second angle.
     *
     * @return the average of <code>a1</code> and <code>a2</code> throws IllegalArgumentException if either angle is
     *         null.
     */
    public static Angle midAngle(Angle lhs, Angle rhs)
    {
        if (lhs == null)
        {
            String msg = Logging.getMessage("nullValue.LhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (rhs == null)
        {
            String msg = Logging.getMessage("nullValue.RhsIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        return Angle.fromDegrees(0.5 * (lhs.degrees + rhs.degrees));
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
            return true;
        if (o == null || this.getClass() != o.getClass())
            return false;

        Angle that = (Angle) o;
        return this.degrees == that.degrees;
    }

    @Override
    public int hashCode()
    {
        long temp = this.degrees != +0.0d ? Double.doubleToLongBits(this.degrees) : 0L;
        return (int) (temp ^ (temp >>> 32));
    }

	/**
	 * Compares this {@link Angle} with another. Returns a negative integer if this is the smaller angle, a positive
	 * integer if this is the larger, and zero if both angles are equal.
	 *
	 * @param angle the angle to compare against.
	 *
	 * @return -1 if this angle is smaller, 0 if both are equal and +1 if this angle is larger.
	 *
	 * @throws IllegalArgumentException if angle is null.
	 */
	public final int compareTo(Angle angle)
	{
		if (angle == null)
		{
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (this.degrees < angle.degrees)
			return -1;

		if (this.degrees > angle.degrees)
			return 1;

		return 0;
	}

    /**
     * Obtains the amount of memory this {@link Angle} consumes.
     *
     * @return the memory footprint of this angle in bytes.
     */
    public long getSizeInBytes()
    {
        return Double.SIZE / 8;
    }

    /**
     * Obtains a <code>String</code> representation of this angle.
     *
     * @return the value of this angle in degrees and as a <code>String</code>.
     */
    @Override
    public String toString()
    {
        return Double.toString(this.degrees) + '\u00B0';
    }

	public static Angle normalizedLatitude(Angle unnormalizedAngle)
	{
		if (unnormalizedAngle == null)
		{
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return Angle.fromDegrees(normalizedDegreesLatitude(unnormalizedAngle.degrees));
	}

	public static Angle normalizedLongitude(Angle unnormalizedAngle)
	{
		if (unnormalizedAngle == null)
		{
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return Angle.fromDegrees(normalizedDegreesLongitude(unnormalizedAngle.degrees));
	}

	public Angle normalizedLatitude()
	{
		return normalizedLatitude(this);
	}

	public Angle normalizedLongitude()
	{
		return normalizedLongitude(this);
	}

	/**
	 * Linearly interpolates between two angles.
	 *
	 * @param amount the interpolant.
	 * @param value1 the first angle.
	 * @param value2 the second angle.
	 *
	 * @return a new angle between <code>value1</code> and <code>value2</code>.
	 */
	public static Angle mix(double amount, Angle value1, Angle value2)
	{
		if (value1 == null || value2 == null)
		{
			String message = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (amount < 0)
			return value1;
		else if (amount > 1)
			return value2;

		Quaternion quat = Quaternion.slerp(
				amount,
				Quaternion.fromAxisAngle(value1, Vec4.UNIT_X),
				Quaternion.fromAxisAngle(value2, Vec4.UNIT_X));

		Angle angle = quat.getRotationX();
		if (Double.isNaN(angle.degrees))
			return null;

		return angle;
	}
}
