/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.BasicView;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id$
 */
public class BasicOrbitViewLimits extends BasicViewPropertyLimits implements OrbitViewLimits
{
    protected Sector centerLocationLimits;
    protected double minCenterElevation;
    protected double maxCenterElevation;
    protected double minZoom;
    protected double maxZoom;

    public BasicOrbitViewLimits()
    {
        this.centerLocationLimits = Sector.fromFullSphere();
        this.minCenterElevation = -Double.MAX_VALUE;
        this.maxCenterElevation = Double.MAX_VALUE;
        this.minHeading = Angle.NEG180;
        this.maxHeading = Angle.POS180;
        this.minPitch = Angle.ZERO;
        this.maxPitch = Angle.POS90;
        this.minZoom = 0;
        this.maxZoom = Double.MAX_VALUE;
    }

    public Sector getCenterLocationLimits()
    {
        return this.centerLocationLimits;
    }

    public void setCenterLocationLimits(Sector sector)
    {
        if (sector == null)
        {
            String message = Logging.getMessage("nullValue.SectorIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.centerLocationLimits = sector;
    }

    public double[] getCenterElevationLimits()
    {
        return new double[] {this.minCenterElevation, this.maxCenterElevation};
    }

    public void setCenterElevationLimits(double minValue, double maxValue)
    {
        this.minCenterElevation = minValue;
        this.maxCenterElevation = maxValue;
    }

    public double[] getZoomLimits()
    {
        return new double[] {this.minZoom, this.maxZoom};
    }

    public void setZoomLimits(double minValue, double maxValue)
    {
        this.minZoom = minValue;
        this.maxZoom = maxValue;
    }

    public static void applyLimits(BasicView view, OrbitViewLimits viewLimits)
    {
        if (view == null)
        {
            String message = Logging.getMessage("nullValue.ViewIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        view.setLookAtPosition(limitLookAtPosition(view.getLookAtPosition(), viewLimits));
        view.setHeading(limitHeading(view.getHeading(), viewLimits));
        view.setTilt(limitPitch(view.getTilt(), viewLimits));
        view.setRange(limitZoom(view.getRange(), viewLimits));
    }

    public static Position limitLookAtPosition(Position position, OrbitViewLimits viewLimits)
    {
        if (position == null)
        {
            String message = Logging.getMessage("nullValue.PositionIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        return new Position(
            limitCenterLocation(position.getLatitude(), position.getLongitude(), viewLimits), 
            limitCenterElevation(position.elevation, viewLimits));

    }

    public static LatLon limitCenterLocation(Angle latitude, Angle longitude, OrbitViewLimits viewLimits)
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

        Sector limits = viewLimits.getCenterLocationLimits();
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

    public static double limitCenterElevation(double value, OrbitViewLimits viewLimits)
    {
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        double[] limits = viewLimits.getCenterElevationLimits();
        double newValue = value;

        if (value < limits[0])
        {
            newValue = limits[0];
        }
        else if (value > limits[1])
        {
            newValue = limits[1];
        }

        return newValue;
    }

   

    public static double limitZoom(double value, OrbitViewLimits viewLimits)
    {
        if (viewLimits == null)
        {
            String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        
        double[] limits = viewLimits.getZoomLimits();
        double newValue = value;

        if (value < limits[0])
        {
            newValue = limits[0];
        }
        else if (value > limits[1])
        {
            newValue = limits[1];
        }

        return newValue;
    }
}
