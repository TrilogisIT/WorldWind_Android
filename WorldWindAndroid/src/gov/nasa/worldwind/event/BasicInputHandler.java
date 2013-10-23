/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.event;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.view.*;
import android.view.View;
import android.widget.TextView;
import gov.nasa.worldwind.*;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;

/**
 * @author ccrick
 * @version $Id: BasicInputHandler.java 852 2012-10-12 19:35:43Z dcollins $
 */
public class BasicInputHandler extends WWObjectImpl implements InputHandler
{
    // TODO: put this value in a configuration file
    protected static final int SINGLE_TAP_INTERVAL = 300;
    protected static final int DOUBLE_TAP_INTERVAL = 300;
    protected static final int JUMP_THRESHOLD = 100;
    protected static final double PINCH_WIDTH_DELTA_THRESHOLD = 5;
    protected static final double PINCH_ROTATE_DELTA_THRESHOLD = 1;

    protected WorldWindow eventSource;

    protected float mPreviousX = -1;
    protected float mPreviousY = -1;
    protected int mPrevPointerCount = 0;

    protected float mPreviousX2 = -1;
    protected float mPreviousY2 = -1;
    protected double mPrevPinchWidth = -1;

    protected boolean mIsTap = false;
    protected long mLastTap = -1;       // system time in ms of last tap

    // Temporary properties used to avoid constant allocation when responding to input events.
    protected Point screenPoint = new Point();
    protected Position position = new Position();
    protected Vec4 point1 = new Vec4();
    protected Vec4 point2 = new Vec4();

    public BasicInputHandler()
    {
    }

    public WorldWindow getEventSource()
    {
        return this.eventSource;
    }

    public void setEventSource(WorldWindow eventSource)
    {
        this.eventSource = eventSource;
    }

    public boolean onTouch(final View view, MotionEvent motionEvent)
    {
        int pointerCount = motionEvent.getPointerCount();

        final float x = motionEvent.getX(0);
        final float y = motionEvent.getY(0);

        switch (motionEvent.getAction())
        {
            case MotionEvent.ACTION_DOWN:
            {
                if (pointerCount == 1)
                    mIsTap = true;

                // display lat-lon under first finger down

                break;
            }

            // all fingers have left the tablet screen
            case MotionEvent.ACTION_UP:
            {
                if (mIsTap && pointerCount == 1)
                {
                    long curTime = System.currentTimeMillis();
                    long timeSinceLastTap = curTime - mLastTap;

                    // double tap has occurred
                    if (mLastTap > 0 && (timeSinceLastTap < DOUBLE_TAP_INTERVAL))
                    {
                        // handle double tap here
                        mLastTap = 0;
                    }
                    // otherwise, single tap has occurred
                    else if (mLastTap < 0 || timeSinceLastTap > SINGLE_TAP_INTERVAL)
                    {
                        // handle single tap here
                        mLastTap = curTime;      // last tap is now this tap
                    }

                    eventSource.invokeInRenderingThread(new Runnable()
                    {
                        public void run()
                        {
                            displayLatLonAtScreenPoint(view.getContext(), x, y);
                        }
                    });
                    eventSource.redraw();
                }

                // reset previous variables
                mPreviousX = -1;
                mPreviousY = -1;
                mPreviousX2 = -1;
                mPreviousY2 = -1;
                mPrevPinchWidth = -1;
                mPrevPointerCount = 0;

                break;
            }

            case MotionEvent.ACTION_MOVE:

                float dx = 0;
                float dy = 0;
                if (mPreviousX > -1 && mPreviousY > -1)
                {
                    dx = x - mPreviousX;
                    dy = y - mPreviousY;
                    mIsTap = false;
                }
                // return if detect a new gesture, as indicated by a large jump
                if (Math.abs(dx) > JUMP_THRESHOLD || Math.abs(dy) > JUMP_THRESHOLD)
                    return true;

                float width = view.getWidth();
                float height = view.getHeight();
                // normalize dx, dy with screen width and height, so they are in [0, 1]
                final double xVelocity = dx / width;
                final double yVelocity = dy / height;

                if (pointerCount != 2 || mPrevPointerCount != 2)
                {
                    // reset pinch variables
                    mPreviousX2 = -1;
                    mPreviousY2 = -1;
                    mPrevPinchWidth = -1;
                }

                // interpret the motionEvent
                if (pointerCount == 1 && !mIsTap)
                {
                    eventSource.invokeInRenderingThread(new Runnable()
                    {
                        public void run()
                        {
                            handlePan(xVelocity, yVelocity);
                        }
                    });
                }
                // handle zoom, rotate/revolve and tilt
                else if (pointerCount > 1)
                {
                    boolean upMove = dy > 0;
                    boolean downMove = dy < 0;

                    float slope = 2;    // arbitrary value indicating a vertical slope
                    if (dx != 0)
                        slope = dy / dx;

                    // separate gestures by number of fingers
                    if (pointerCount == 2)
                    {
                        float x2 = motionEvent.getX(1);
                        float y2 = motionEvent.getY(1);

                        float dy2 = 0;
                        if (mPreviousX > -1 && mPreviousY > -1)
                        {   // delta is only relevant if a previous location exists
                            dy2 = y2 - mPreviousY2;
                        }

                        final double yVelocity2 = dy2 / height;
                        double pinchWidth = Math.sqrt(Math.pow((x - x2), 2) + Math.pow((y - y2), 2));

                        // compute angle traversed
                        final double deltaPinchWidth = pinchWidth - mPrevPinchWidth;
                        final double deltaPinchAngle = computeRotationAngle(x, y, x2, y2,
                            mPreviousX, mPreviousY, mPreviousX2, mPreviousY2);

                        if (mPrevPinchWidth > 0 && Math.abs(deltaPinchWidth) > PINCH_WIDTH_DELTA_THRESHOLD)
                        {
                            eventSource.invokeInRenderingThread(new Runnable()
                            {
                                public void run()
                                {
                                    handlePinchZoom(deltaPinchWidth);
                                }
                            });
                        }
                        // TODO: prevent this from confusion with pinch-rotate
                        else if ((upMove || downMove) && Math.abs(slope) > 1
                            && (yVelocity > 0 && yVelocity2 > 0) || (yVelocity < 0 && yVelocity2 < 0))
                        {
                            eventSource.invokeInRenderingThread(new Runnable()
                            {
                                public void run()
                                {
                                    handleLookAtTilt(yVelocity);
                                }
                            });
                        }
                        else if (deltaPinchAngle != 0 && deltaPinchAngle > PINCH_ROTATE_DELTA_THRESHOLD)
                        {
                            eventSource.invokeInRenderingThread(new Runnable()
                            {
                                public void run()
                                {
                                    handlePinchRotate(deltaPinchAngle);
                                }
                            });
                        }

                        mPreviousX2 = x2;
                        mPreviousY2 = y2;
                        mPrevPinchWidth = pinchWidth;
                    }
                    else if (pointerCount >= 3)
                    {
                        eventSource.invokeInRenderingThread(new Runnable()
                        {
                            public void run()
                            {
                                handleRestoreNorth(xVelocity, yVelocity);
                            }
                        });
                    }
                }

                eventSource.redraw();

                mPreviousX = x;
                mPreviousY = y;
                mPrevPointerCount = pointerCount;

                break;
        }

        return true;
    }

    protected void displayLatLonAtScreenPoint(Context context, float x, float y)
    {
        BasicView view = (BasicView) this.eventSource.getView();
        Globe globe = this.eventSource.getModel().getGlobe();
        this.screenPoint.set((int) x, (int) y);

        if (view.computePositionFromScreenPoint(globe, this.screenPoint, this.position))
        {
            final String latText = this.position.latitude.toString();
            final String lonText = this.position.longitude.toString();

            ((Activity) context).runOnUiThread(new Runnable()
            {
                public void run()
                {
                    updateLatLonText(latText, lonText);
                }
            });
        }
        else
        {
            ((Activity) context).runOnUiThread(new Runnable()
            {
                public void run()
                {
                    updateLatLonText("off globe", "off globe");
                }
            });
        }
    }

    protected void updateLatLonText(String latitudeText, String longitudeText)
    {
        // update displayed lat/lon
        TextView latText = ((WorldWindowGLSurfaceView) this.eventSource).getLatitudeText();
        TextView lonText = ((WorldWindowGLSurfaceView) this.eventSource).getLongitudeText();

        if (latText != null && lonText != null)
        {
            latText.setText(latitudeText);
            lonText.setText(longitudeText);
        }
    }

    // given the current and previous locations of two points, compute the angle of the
    // rotation they trace out
    protected double computeRotationAngle(float x, float y, float x2, float y2,
        float xPrev, float yPrev, float xPrev2, float yPrev2)
    {
        // can't compute if no previous points
        if (xPrev < 0 || yPrev < 0 || xPrev2 < 0 || yPrev2 < 0)
            return 0;

        if ((x - x2) == 0 || (xPrev - xPrev2) == 0)
            return 0;

        // 1. compute lines connecting pt1 to pt2, and pt1' to pt2'
        float slope = (y - y2) / (x - x2);
        float slopePrev = (yPrev - yPrev2) / (xPrev - xPrev2);

        // b = y - mx
        float b = y - slope * x;
        float bPrev = yPrev - slopePrev * xPrev;

        // 2. use Cramer's Rule to find the intersection of the two lines
        float det1 = -slope * 1 + slopePrev * 1;
        float det2 = b * 1 - bPrev * 1;
        float det3 = (-slope * bPrev) - (-slopePrev * b);

        // check for case where lines are parallel
        if (det1 == 0)
            return 0;

        // compute the intersection point
        float isectX = det2 / det1;
        float isectY = det3 / det1;

        // 3. use the law of Cosines to determine the angle covered

        // compute lengths of sides of triangle created by pt1, pt1Prev and the intersection pt
        double BC = Math.sqrt(Math.pow(x - isectX, 2) + Math.pow(y - isectY, 2));
        double AC = Math.sqrt(Math.pow(xPrev - isectX, 2) + Math.pow(yPrev - isectY, 2));
        double AB = Math.sqrt(Math.pow(x - xPrev, 2) + Math.pow(y - yPrev, 2));

        this.point1.set(xPrev - isectX, yPrev - isectY, 0);
        this.point2.set(x - isectX, y - isectY, 0);

        // if one finger stayed fixed, may have degenerate triangle, so use other triangle instead
        if (BC == 0 || AC == 0 || AB == 0)
        {
            BC = Math.sqrt(Math.pow(x2 - isectX, 2) + Math.pow(y2 - isectY, 2));
            AC = Math.sqrt(Math.pow(xPrev2 - isectX, 2) + Math.pow(yPrev2 - isectY, 2));
            AB = Math.sqrt(Math.pow(x2 - xPrev2, 2) + Math.pow(y2 - yPrev2, 2));

            this.point1.set(xPrev2 - isectX, yPrev2 - isectY, 0);
            this.point2.set(x2 - isectX, y2 - isectY, 0);

            if (BC == 0 || AC == 0 || AB == 0)
                return 0;
        }

        // Law of Cosines
        double num = (Math.pow(BC, 2) + Math.pow(AC, 2) - Math.pow(AB, 2));
        double denom = (2 * BC * AC);
        double BCA = Math.acos(num / denom);

        // use cross product to determine if rotation is positive or negative
        if (this.point1.cross3(this.point2).z < 0)
            BCA = 2 * Math.PI - BCA;

        return Math.toDegrees(BCA);
    }

    // computes pan using velocity of swipe motion
    protected void handlePan(double xVelocity, double yVelocity)
    {
        BasicView view = (BasicView) this.eventSource.getView();
        Position pos = view.getLookAtPosition();
        Angle heading = view.getHeading();
        double range = view.getRange();

        double panScalingFactor = 0.00001f;
        double sin = heading.sin();
        double cos = heading.cos();

        double newLat = Angle.normalizedDegreesLatitude(pos.latitude.degrees
            + (cos * yVelocity + sin * xVelocity) * panScalingFactor * range);
        double newLon = Angle.normalizedDegreesLongitude(pos.longitude.degrees
            - (cos * xVelocity - sin * yVelocity) * panScalingFactor * range);

        pos.setDegrees(newLat, newLon);
    }

    protected void handlePinchZoom(double widthDelta)
    {
        BasicView view = (BasicView) this.eventSource.getView();
        double value = view.getRange();
        double zoomScalingFactor = 3E-3f;
        double newValue = value - widthDelta * zoomScalingFactor * value;

        if (newValue < 0)
            newValue = 0;

        view.setRange(newValue);
    }

    protected void handlePinchRotate(double rotAngleDegrees)
    {
        BasicView view = (BasicView) this.eventSource.getView();
        Angle angle = view.getHeading();
        double newAngle = (angle.degrees - rotAngleDegrees) % 360;

        if (newAngle < -180)
            newAngle = 360 + newAngle;
        else if (newAngle > 180)
            newAngle = newAngle - 360;

        angle.setDegrees(newAngle);
    }

    protected void handleLookAtTilt(double yVelocity)
    {
        BasicView view = (BasicView) this.eventSource.getView();
        Angle angle = view.getTilt();
        double scalingFactor = 100;
        double newAngle = (angle.degrees + yVelocity * scalingFactor) % 360;

        if (newAngle < 0)
            newAngle = 0;
        else if (newAngle > 90)
            newAngle = 90;

        angle.setDegrees(newAngle);
    }

    protected void handleRestoreNorth(double xVelocity, double yVelocity)
    {
        BasicView view = (BasicView) this.eventSource.getView();
        Angle heading = view.getHeading();
        Angle tilt = view.getTilt();

        // interpolate to zero heading and tilt
        double headingScalingFactor = 5;
        double tiltScalingFactor = 3;
        double delta = Math.sqrt(Math.pow(xVelocity, 2) + Math.pow(yVelocity, 2));
        double newHeading = heading.degrees + -heading.degrees * delta * headingScalingFactor;
        double newTilt = tilt.degrees + -tilt.degrees * delta * tiltScalingFactor;

        heading.setDegrees(newHeading);
        tilt.setDegrees(newTilt);
    }
}