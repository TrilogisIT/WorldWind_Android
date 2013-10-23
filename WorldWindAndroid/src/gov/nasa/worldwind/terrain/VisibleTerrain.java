/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
*/
package gov.nasa.worldwind.terrain;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id: VisibleTerrain.java 847 2012-10-12 18:17:56Z dcollins $
 */
public class VisibleTerrain implements Terrain
{
    protected DrawContext dc;
    protected Vec4 point = new Vec4();

    public VisibleTerrain(DrawContext dc)
    {
        this.dc = dc;
    }

    /** {@inheritDoc} */
    public Globe getGlobe()
    {
        return this.dc.getGlobe();
    }

    /** {@inheritDoc} */
    public double getVerticalExaggeration()
    {
        return this.dc.getVerticalExaggeration();
    }

    /** {@inheritDoc} */
    public Double getElevation(Angle latitude, Angle longitude)
    {
        if (latitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LongitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 pt = this.getSurfacePoint(latitude, longitude, 0);
        if (pt == null)
            return null;

        Vec4 p = this.getGlobe().computePointFromPosition(latitude, longitude, 0);

        return p.distanceTo3(pt);
    }

    /** {@inheritDoc} */
    public Vec4 getSurfacePoint(Position position)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 result = new Vec4();
        this.getSurfacePoint(position.latitude, position.longitude, position.elevation, result);
        return result;
    }

    /** {@inheritDoc} */
    public Vec4 getSurfacePoint(Angle latitude, Angle longitude, double metersOffset)
    {
        if (latitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LongitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        Vec4 result = new Vec4();
        this.getSurfacePoint(latitude, longitude, metersOffset, result);
        return result;
    }

    /** {@inheritDoc} */
    public void getSurfacePoint(Position position, Vec4 result)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.getSurfacePoint(position.latitude, position.longitude, position.elevation, result);
    }

    /** {@inheritDoc} */
    public void getSurfacePoint(Angle latitude, Angle longitude, double metersOffset, Vec4 result)
    {
        if (latitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LongitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        SectorGeometryList sectorGeometry = this.dc.getSurfaceGeometry();
        if (sectorGeometry != null && sectorGeometry.getSurfacePoint(latitude, longitude, result))
        {
            // The sector geometry already has vertical exaggeration applied. This has the effect of interpreting
            // metersOffset as height above the terrain after vertical exaggeration is applied.
            this.getGlobe().computeSurfaceNormalAtPoint(result, this.point);
            this.point.multiply3AndSet(metersOffset);
            result.add3AndSet(this.point);
        }
        else
        {
            // Scale the elevation height by the vertical exaggeration to accommodate for vertical exaggeration applied
            // to the terrain. This has the effect of interpreting metersOffset as height above the terrain after
            // vertical exaggeration is applied.
            double height = metersOffset
                + this.getGlobe().getElevation(latitude, longitude) * this.getVerticalExaggeration();
            this.getGlobe().computePointFromPosition(latitude, longitude, height, result);
        }
    }

    /** {@inheritDoc} */
    public void getPoint(Position position, String altitudeMode, Vec4 result)
    {
        if (position == null)
        {
            String msg = Logging.getMessage("nullValue.PositionIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        this.getPoint(position.latitude, position.longitude, position.elevation, altitudeMode, result);
    }

    /** {@inheritDoc} */
    public void getPoint(Angle latitude, Angle longitude, double metersOffset, String altitudeMode, Vec4 result)
    {
        if (latitude == null)
        {
            String msg = Logging.getMessage("nullValue.LatitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (longitude == null)
        {
            String msg = Logging.getMessage("nullValue.LongitudeIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (AVKey.CLAMP_TO_GROUND.equals(altitudeMode))
        {
            this.getSurfacePoint(latitude, longitude, 0.0, result);
        }
        else if (AVKey.RELATIVE_TO_GROUND.equals(altitudeMode))
        {
            this.getSurfacePoint(latitude, longitude, metersOffset, result);
        }
        else // ABSOLUTE
        {
            // Raise the height to accommodate vertical exaggeration applied to the terrain. This has the effect of
            // interpreting metersOffset with an ABSOLUTE altitude mode as height above the geoid with vertical
            // exaggeration applied.
            double height = metersOffset * this.getVerticalExaggeration();
            this.getGlobe().computePointFromPosition(latitude, longitude, height, result);
        }
    }
};
