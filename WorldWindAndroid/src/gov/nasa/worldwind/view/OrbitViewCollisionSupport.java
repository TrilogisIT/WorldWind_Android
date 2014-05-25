/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.view;

import gov.nasa.worldwind.BasicView;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;

/**
 * @author dcollins
 * @version $Id$
 */
public class OrbitViewCollisionSupport
{
    private double collisionThreshold;
    private int numIterations;

    public OrbitViewCollisionSupport()
    {
        setNumIterations(1);
    }

    public double getCollisionThreshold()
    {
        return this.collisionThreshold;
    }

    public void setCollisionThreshold(double collisionThreshold)
    {
        if (collisionThreshold < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", collisionThreshold);
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.collisionThreshold = collisionThreshold;
    }

    public int getNumIterations()
    {
        return this.numIterations;
    }

    public void setNumIterations(int numIterations)
    {
        if (numIterations < 1)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", numIterations);
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        this.numIterations = numIterations;
    }

    public boolean isColliding(BasicView orbitView, double nearDistance, DrawContext dc)
    {
        if (orbitView == null)
        {
            String message = Logging.getMessage("nullValue.BasicViewIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (nearDistance < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", nearDistance);
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        Globe globe = dc.getGlobe();
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        Matrix modelviewInv = getModelviewInverse(dc,
            orbitView.getLookAtPosition(), orbitView.getHeading(), orbitView.getTilt(), orbitView.getRoll(),
            orbitView.getRange());
        if (modelviewInv != null)
        {
            // BasicView is colliding when its eye point is below the collision threshold.
            double heightAboveSurface = computeViewHeightAboveSurface(dc, modelviewInv,
                orbitView.getFieldOfView(), orbitView.getViewport(), nearDistance);
            return heightAboveSurface < this.collisionThreshold;
        }

        return false;
    }

    public Position computeCenterPositionToResolveCollision(BasicView orbitView, double nearDistance,
        DrawContext dc)
    {
        if (orbitView == null)
        {
            String message = Logging.getMessage("nullValue.BasicViewIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (nearDistance < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", nearDistance);
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        Globe globe = dc.getGlobe();
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        Position newCenter = null;

        for (int i = 0; i < this.numIterations; i++)
        {
            Matrix modelviewInv = getModelviewInverse(dc,
                newCenter != null ? newCenter : orbitView.getLookAtPosition(),
                orbitView.getHeading(), orbitView.getTilt(), orbitView.getRoll(), orbitView.getRange());
            if (modelviewInv != null)
            {
                double heightAboveSurface = computeViewHeightAboveSurface(dc, modelviewInv,
                    orbitView.getFieldOfView(), orbitView.getViewport(), nearDistance);
                double adjustedHeight = heightAboveSurface - this.collisionThreshold;
                if (adjustedHeight < 0)
                {
                    newCenter = new Position(
                        newCenter != null ? newCenter : orbitView.getLookAtPosition(),
                        (newCenter != null ? newCenter.elevation : orbitView.getLookAtPosition().elevation)
                            - adjustedHeight);
                }
            }
        }

        return newCenter;
    }

    public Angle computePitchToResolveCollision(BasicView orbitView, double nearDistance, DrawContext dc)
    {
        if (orbitView == null)
        {
            String message = Logging.getMessage("nullValue.BasicViewIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (nearDistance < 0)
        {
            String message = Logging.getMessage("generic.ArgumentOutOfRange", nearDistance);
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        if (dc == null)
        {
            String message = Logging.getMessage("nullValue.DrawContextIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }
        Globe globe = dc.getGlobe();
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        Angle newPitch = null;

        for (int i = 0; i < this.numIterations; i++)
        {
            Matrix modelviewInv = getModelviewInverse(dc,
                orbitView.getLookAtPosition(), orbitView.getHeading(),
                newPitch != null ? newPitch : orbitView.getTilt(), orbitView.getRoll(),
                orbitView.getRange());
            if (modelviewInv != null)
            {
                double heightAboveSurface = computeViewHeightAboveSurface(dc, modelviewInv,
                    orbitView.getFieldOfView(), orbitView.getViewport(), nearDistance);
                double adjustedHeight = heightAboveSurface - this.collisionThreshold;
                if (adjustedHeight < 0)
                {
                    Vec4 eyePoint = getEyePoint(modelviewInv);
                    Vec4 centerPoint = globe.computePointFromPosition(orbitView.getLookAtPosition());
                    if (eyePoint != null && centerPoint != null)
                    {
                        Position eyePos = globe.computePositionFromPoint(eyePoint);
                        // Compute the eye point required to resolve the collision.
                        Vec4 newEyePoint = globe.computePointFromPosition(eyePos.getLatitude(), eyePos.getLongitude(),
                            eyePos.elevation - adjustedHeight);
                        // Compute the pitch that corresponds with the elevation of the eye point
                        // (but not necessarily the latitude and longitude).
						Vec4 normalAtCenter = new Vec4();
                        globe.computeSurfaceNormalAtPoint(centerPoint, normalAtCenter);
                        Vec4 newEye_sub_center = newEyePoint.subtract3(centerPoint).normalize3();
                        double dot = normalAtCenter.dot3(newEye_sub_center);
                        if (dot >= -1 || dot <= 1)
                        {
                            double angle = Math.acos(dot);
                            newPitch = Angle.fromRadians(angle);
                        }
                    }
                }
            }
        }

        return newPitch;
    }

    public static double computeViewHeightAboveSurface(DrawContext dc, Matrix modelviewInv,
        Angle fieldOfView, Rect viewport, double nearDistance)
    {
        double height = Double.POSITIVE_INFINITY;
        if (dc != null && modelviewInv != null && fieldOfView != null && viewport != null && nearDistance >= 0)
        {
            Vec4 eyePoint = getEyePoint(modelviewInv);
            if (eyePoint != null)
            {
                double eyeHeight = computePointHeightAboveSurface(dc, eyePoint);
                if (eyeHeight < height)
                    height = eyeHeight;
            }

            Vec4 nearPoint = getPointOnNearPlane(modelviewInv, fieldOfView, viewport, nearDistance);
            if (nearPoint != null)
            {
                double nearHeight = computePointHeightAboveSurface(dc, nearPoint);
                if (nearHeight < height)
                    height = nearHeight;
            }
        }
        return height;
    }

    public static double computePointHeightAboveSurface(DrawContext dc, Vec4 point)
    {
        if (dc != null && dc.getGlobe() != null && point != null)
        {
            final Globe globe = dc.getGlobe();
			Position position = globe.computePositionFromPoint(point);
            // Look for the surface geometry point at 'position'.
            Vec4 pointOnGlobe = dc.getVisibleTerrain().getSurfacePoint(position.latitude, position.longitude, 0);
			if (pointOnGlobe != null) {
				Vec4 eyeElevation = new Vec4();
				eyeElevation.subtract3AndSet(point, pointOnGlobe);
				return eyeElevation.getLength3();
			}
            // Fallback to using globe elevation values.
            Position surfacePosition = new Position(position,
                    globe.getElevation(position.getLatitude(), position.getLongitude()) * dc.getVerticalExaggeration());
            return position.elevation - surfacePosition.elevation;
        }
        return Double.POSITIVE_INFINITY;
    }

    public static Matrix getModelviewInverse(DrawContext dc,
        Position centerPosition, Angle heading, Angle pitch, Angle roll, double zoom)
    {
        if (dc != null && centerPosition != null && heading != null && pitch != null)
        {
			Matrix modelview = Matrix.fromIdentity();
			modelview.setLookAt(dc.getVisibleTerrain(), centerPosition.latitude, centerPosition.longitude, centerPosition.elevation, AVKey.CLAMP_TO_GROUND, zoom,
					heading, pitch, roll);
            if (modelview != null)
                return modelview.invert();
        }

        return null;
    }

    public static Vec4 getEyePoint(Matrix modelviewInv)
    {
        return modelviewInv != null ? Vec4.UNIT_W.transformBy4(modelviewInv) : null;
    }

    public static Vec4 getPointOnNearPlane(Matrix modelviewInv, Angle fieldOfView, Rect viewport,
        double nearDistance)
    {
        if (modelviewInv != null && fieldOfView != null && viewport != null && nearDistance >= 0)
        {
            // If either either the viewport width or height is zero, then fall back to an aspect ratio of 1. 
            // Otherwise, compute the standard aspect ratio.
            double aspect = (viewport.width <= 0 || viewport.height <= 0) ?
                1d : (viewport.height / viewport.width);
            double nearClipHeight = 2 * aspect * nearDistance * fieldOfView.tanHalfAngle();
            // Computes the point on the bottom center of the near clip plane.
            Vec4 nearClipVec = new Vec4(0, -nearClipHeight / 2.0, -nearDistance, 1);
            return nearClipVec.transformBy4(modelviewInv);
        }

        return null;
    }
}
