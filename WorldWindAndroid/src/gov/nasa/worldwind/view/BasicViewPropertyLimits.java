/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

/**
 * @author jym
 * @version $Id$
 */
public class BasicViewPropertyLimits implements ViewPropertyLimits
{

    protected Sector eyeLocationLimits;
    protected Angle minHeading;
    protected Angle maxHeading;
    protected Angle minPitch;
    protected Angle maxPitch;
    protected Angle minRoll;
    protected Angle maxRoll;
    protected double minEyeElevation;
    protected double maxEyeElevation;

    public BasicViewPropertyLimits()
    {
        this.eyeLocationLimits = Sector.fromFullSphere();
        this.minEyeElevation = -Double.MAX_VALUE;
        this.maxEyeElevation = Double.MAX_VALUE;
        this.minHeading = Angle.NEG180;
        this.maxHeading = Angle.POS180;
        this.minPitch = Angle.ZERO;
        this.maxPitch = Angle.POS90;
        this.minRoll = Angle.NEG180;
        this.maxRoll = Angle.POS180;
    }

    public Sector getEyeLocationLimits()
    {
        return this.eyeLocationLimits;
    }

    public void setEyeLocationLimits(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.eyeLocationLimits = sector;
    }

    public double[] getEyeElevationLimits()
    {
        return new double[] {this.minEyeElevation, this.maxEyeElevation};
    }

    public void setEyeElevationLimits(double minValue, double maxValue)
    {
        this.minEyeElevation = minValue;
        this.maxEyeElevation = maxValue;
    }

     public Angle[] getHeadingLimits()
    {
        return new Angle[] {this.minHeading, this.maxHeading};
    }

    public void setHeadingLimits(Angle minAngle, Angle maxAngle)
    {
        if (minAngle == null || maxAngle == null)
        {
            String message = Logging.getMessage("nullValue.MinOrMaxAngleIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.minHeading = minAngle;
        this.maxHeading = maxAngle;
    }

    public Angle[] getPitchLimits()
    {
        return new Angle[] {this.minPitch, this.maxPitch};
    }

    public void setPitchLimits(Angle minAngle, Angle maxAngle)
    {
        if (minAngle == null || maxAngle == null)
        {
            String message = Logging.getMessage("nullValue.MinOrMaxAngleIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.minPitch = minAngle;
        this.maxPitch = maxAngle;
    }

    /**
     * Get the limits for roll.
     *
     * @return The roll limits as a two element array {minRoll, maxRoll},
     */
    public Angle[] getRollLimits()
    {
        return new Angle[] { this.minRoll, this.maxRoll };
    }

    /**
     * Set the roll limits.
     *
     * @param minAngle The smallest allowable roll.
     * @param maxAngle The largest allowable roll.
     */
    public void setRollLimits(Angle minAngle, Angle maxAngle)
    {
        if (minAngle == null || maxAngle == null)
        {
            String message = Logging.getMessage("nullValue.MinOrMaxAngleIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.minRoll = minAngle;
        this.maxRoll = maxAngle;
    }

     public static Angle limitHeading(Angle angle, ViewPropertyLimits viewLimits)
    {
        if (angle == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        Angle[] limits = viewLimits.getHeadingLimits();
        Angle newAngle = angle;

        if (angle.compareTo(limits[0]) < 0)
        {
            newAngle = limits[0];
        }
        else if (angle.compareTo(limits[1]) > 0)
        {
            newAngle = limits[1];
        }

        return newAngle;
    }

    public static Angle limitPitch(Angle angle, ViewPropertyLimits viewLimits)
    {
        if (angle == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        Angle[] limits = viewLimits.getPitchLimits();
        Angle newAngle = angle;
        if (angle.compareTo(limits[0]) < 0)
        {
            newAngle = limits[0];
        }
        else if (angle.compareTo(limits[1]) > 0)
        {
            newAngle = limits[1];
        }

        return newAngle;
    }

    /**
     * Clamp a roll angle to the range specified in a limit object.
     *
     * @param angle      Angle to clamp to the allowed range.
     * @param viewLimits defines the roll limits. 
     */
    public static Angle limitRoll(Angle angle, ViewPropertyLimits viewLimits)
    {
        if (angle == null)
        {
            String message = Logging.getMessage("nullValue.AngleIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        Angle[] limits = viewLimits.getRollLimits();
        Angle newAngle = angle;
        if (angle.compareTo(limits[0]) < 0)
        {
            newAngle = limits[0];
        }
        else if (angle.compareTo(limits[1]) > 0)
        {
            newAngle = limits[1];
        }

        return newAngle;
    }

    public static double limitEyeElevation(double elevation, ViewPropertyLimits viewLimits)
    {
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        double newElevation = elevation;
        double[] elevLimits = viewLimits.getEyeElevationLimits();

        if (elevation < elevLimits[0])
        {
             newElevation = elevLimits[0];
        }
        else if (elevation > elevLimits[1])
        {
            newElevation = elevLimits[1];
        }
        return(newElevation);
    }

    public static LatLon limitEyePositionLocation(Angle latitude, Angle longitude, ViewPropertyLimits viewLimits)
    {
        if (latitude == null || longitude == null)
        {
            String message = Logging.getMessage("nullValue.LatitudeOrLongitudeIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        Sector limits = viewLimits.getEyeLocationLimits();
        Angle newLatitude = latitude;
        Angle newLongitude = longitude;

        if (latitude.compareTo(limits.minLatitude) < 0)
        {
            newLatitude = limits.minLatitude;
        }
        else if (latitude.compareTo(limits.maxLatitude) > 0)
        {
            newLatitude = limits.maxLatitude;
        }

        if (longitude.compareTo(limits.minLongitude) < 0)
        {
            newLongitude = limits.minLongitude;
        }
        else if (longitude.compareTo(limits.maxLongitude) > 0)
        {
            newLongitude = limits.maxLongitude;
        }

        return new LatLon(newLatitude, newLongitude);
    }
}
