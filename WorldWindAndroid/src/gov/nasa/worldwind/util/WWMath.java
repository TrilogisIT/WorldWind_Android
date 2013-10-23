/*
 * Copyright (C) 2012 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */
package gov.nasa.worldwind.util;

import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.globes.Globe;

import java.nio.FloatBuffer;
import java.util.*;

/**
 * WWMath provides a collection of static utility methods for common World Wind mathematical operations.
 *
 * @author dcollins
 * @version $Id: WWMath.java 852 2012-10-12 19:35:43Z dcollins $
 */
public class WWMath
{
    /** The ratio of milliseconds per second. Used to convert time in seconds to time in milliseconds. */
    protected static final double SECOND_TO_MILLIS = 1000.0;
    /** The ratio of milliseconds per minute. Used to convert time in minutes to time in milliseconds. */
    protected static final double MINUTE_TO_MILLIS = 60.0 * SECOND_TO_MILLIS;
    /** The ratio of milliseconds per hour. Used to convert time in hours to time in milliseconds. */
    protected static final double HOUR_TO_MILLIS = 60.0 * MINUTE_TO_MILLIS;

    // Temporary properties used to avoid constant reallocation of primitive types.
    protected static Vec4 point1 = new Vec4();
    protected static Vec4 point2 = new Vec4();
    protected static Matrix matrix = Matrix.fromIdentity();
    protected static Matrix matrixInv = Matrix.fromIdentity();

    /**
     * Converts time in seconds to time in milliseconds.
     *
     * @param seconds time in seconds.
     *
     * @return time in milliseconds.
     */
    public static double convertSecondsToMillis(double seconds)
    {
        return seconds * SECOND_TO_MILLIS;
    }

    /**
     * Converts time in minutes to time in milliseconds.
     *
     * @param minutes time in minutes.
     *
     * @return time in milliseconds.
     */
    public static double convertMinutesToMillis(double minutes)
    {
        return minutes * MINUTE_TO_MILLIS;
    }

    /**
     * Converts time in hours to time in milliseconds.
     *
     * @param hours time in hours.
     *
     * @return time in milliseconds.
     */
    public static double convertHoursToMillis(double hours)
    {
        return hours * HOUR_TO_MILLIS;
    }

    /**
     * Computes the distance to the horizon from a viewer at the specified elevation. Only the globe's ellipsoid is
     * considered; terrain elevations are not incorporated. This returns zero if the specified elevation is less than or
     * equal to zero.
     *
     * @param globe     the globe to compute a horizon distance for.
     * @param elevation the viewer's elevation, in meters relative to mean sea level.
     *
     * @return the distance to the horizon, in meters.
     *
     * @throws IllegalArgumentException if the globe is <code>null</code>.
     */
    public static double computeHorizonDistance(Globe globe, double elevation)
    {
        if (globe == null)
        {
            String message = Logging.getMessage("nullValue.GlobeIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (elevation <= 0)
            return 0;

        return Math.sqrt(elevation * (2 * globe.getRadius() + elevation));
    }

    /**
     * Returns an array of normalized vectors defining the three principal axes of the x-, y-, and z-coordinates from
     * the specified points Iterable, sorted from the most prominent axis to the least prominent. This does not retain
     * any reference to the specified iterable or its vectors, nor does this modify the vectors in any way.
     * <p/>
     * This returns <code>null</code> if the points Iterable is empty, or if all of the points are <code>null</code>.
     * The returned array contains three normalized orthogonal vectors defining a coordinate system which best fits the
     * distribution of the points Iterable about its arithmetic mean.
     *
     * @param iterable the Iterable of points for which to compute the principal axes.
     *
     * @return the normalized principal axes of the points Iterable, sorted from the most prominent axis to the least
     *         prominent.
     *
     * @throws IllegalArgumentException if the points Iterable is <code>null</code>.
     */
    public static Vec4[] computePrincipalAxes(Iterable<? extends Vec4> iterable)
    {
        if (iterable == null)
        {
            String msg = Logging.getMessage("nullValue.IterableIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compute the covariance matrix of the specified points Iterable. Note that Matrix.fromCovarianceOfVertices
        // returns null if the points Iterable is empty, or if all of the points are null.
        Matrix covariance = Matrix.fromCovarianceOfPoints(iterable);
        if (covariance == null)
            return null;

        // Compute the eigenvalues and eigenvectors of the covariance matrix. Since the covariance matrix is symmetric
        // by definition, we can safely use the method Matrix.computeEigensystemFromSymmetricMatrix3().
        final double[] eigenValues = new double[3];
        final Vec4[] eigenVectors = new Vec4[3];
        Matrix.computeEigensystemFromSymmetricMatrix3(covariance, eigenValues, eigenVectors);

        // Compute an index array who's entries define the order in which the eigenValues array can be sorted in
        // ascending order.
        Integer[] indexArray = {0, 1, 2};
        Arrays.sort(indexArray, new Comparator<Integer>()
        {
            public int compare(Integer a, Integer b)
            {
                return Double.compare(eigenValues[a], eigenValues[b]);
            }
        });

        // Return the normalized eigenvectors in order of decreasing eigenvalue. This has the effect of returning three
        // normalized orthogonal vectors defining a coordinate system, which are sorted from the most prominent axis to
        // the least prominent.
        return new Vec4[]
            {
                eigenVectors[indexArray[2]].normalize3(),
                eigenVectors[indexArray[1]].normalize3(),
                eigenVectors[indexArray[0]].normalize3()
            };
    }

    /**
     * Returns an array of normalized vectors defining the three principal axes of the x-, y-, and z-coordinates from
     * the specified buffer of points, sorted from the most prominent axis to the least prominent. This does not retain
     * any reference to the specified buffer or modify its contents in any way.
     * <p/>
     * This returns <code>null</code> if the buffer is empty or contains only a partial point. The returned array
     * contains three normalized orthogonal vectors defining a coordinate system which best fits the distribution of the
     * points about its arithmetic mean.
     * <p/>
     * The buffer must contain XYZ coordinate tuples which are either tightly packed or offset by the specified stride.
     * The stride specifies the number of buffer elements between the first coordinate of consecutive tuples. For
     * example, a stride of 3 specifies that each tuple is tightly packed as XYZXYZXYZ, whereas a stride of 5 specifies
     * that there are two elements between each tuple as XYZabXYZab (the elements "a" and "b" are ignored). The stride
     * must be at least 3. If the buffer's length is not evenly divisible into stride-sized tuples, this ignores the
     * remaining elements that follow the last complete tuple.
     *
     * @param buffer the buffer containing the point coordinates for which to compute the principal axes.
     * @param stride the number of elements between the first coordinate of consecutive points. If stride is 3, this
     *               interprets the buffer has having tightly packed XYZ coordinate tuples.
     *
     * @return the normalized principal axes of the points, sorted from the most prominent axis to the least prominent.
     *
     * @throws IllegalArgumentException if the buffer is <code>null</code>, or if the stride is less than three.
     */
    public static Vec4[] computePrincipalAxes(FloatBuffer buffer, int stride)
    {
        if (buffer == null)
        {
            String message = Logging.getMessage("nullValue.BufferIsNull");
            Logging.error(message);
            throw new IllegalArgumentException(message);
        }

        if (stride < 3)
        {
            String msg = Logging.getMessage("generic.StrideIsInvalid", stride);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compute the covariance matrix of the specified points Iterable. Note that Matrix.fromCovarianceOfVertices
        // returns null if the points Iterable is empty, or if all of the points are null.
        Matrix covariance = Matrix.fromCovarianceOfPoints(buffer, stride);
        if (covariance == null)
            return null;

        // Compute the eigenvalues and eigenvectors of the covariance matrix. Since the covariance matrix is symmetric
        // by definition, we can safely use the method Matrix.computeEigensystemFromSymmetricMatrix3().
        final double[] eigenValues = new double[3];
        final Vec4[] eigenVectors = new Vec4[3];
        Matrix.computeEigensystemFromSymmetricMatrix3(covariance, eigenValues, eigenVectors);

        // Compute an index array who's entries define the order in which the eigenValues array can be sorted in
        // ascending order.
        Integer[] indexArray = {0, 1, 2};
        Arrays.sort(indexArray, new Comparator<Integer>()
        {
            public int compare(Integer a, Integer b)
            {
                return Double.compare(eigenValues[a], eigenValues[b]);
            }
        });

        // Return the normalized eigenvectors in order of decreasing eigenvalue. This has the effect of returning three
        // normalized orthognal vectors defining a coordinate system, which are sorted from the most prominent axis to
        // the least prominent.
        return new Vec4[]
            {
                eigenVectors[indexArray[2]].normalize3(),
                eigenVectors[indexArray[1]].normalize3(),
                eigenVectors[indexArray[0]].normalize3()
            };
    }

    /**
     * Computes a line in model coordinates that originates from the eye point and passes through the screen point (x,
     * y). This does not retain any reference to the specified parameters or modify them in any way.
     * <p/>
     * The specified modelview, projection, and viewport define the properties used to transform from screen coordinates
     * to model coordinates. The screen point is relative to the lower left corner.
     *
     * @param x          the screen point's x-coordinate, relative to the lower left corner.
     * @param y          the screen point's y-coordinate, relative to the lower left corner.
     * @param modelview  the modelview matrix, transforms model coordinates to eye coordinates.
     * @param projection the projection matrix, transforms eye coordinates to clip coordinates.
     * @param viewport   the viewport rectangle, transforms clip coordinates to screen coordinates.
     * @param result     contains the line in model coordinates after this method returns. This value is not modified if
     *                   this returns <code>false</code>.
     *
     * @return <code>true</code> if a ray is successfully computed, and <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if any of the modelview, projection, viewport, or result are <code>null</code>,
     *                                  or if either of the viewport width or height are less than or equal to zero.
     */
    public static boolean computeRayFromScreenPoint(double x, double y, Matrix modelview, Matrix projection,
        Rect viewport, Line result)
    {
        if (modelview == null)
        {
            String msg = Logging.getMessage("nullValue.ModelviewMatrixIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (projection == null)
        {
            String msg = Logging.getMessage("nullValue.ProjectionMatrixIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport == null)
        {
            String msg = Logging.getMessage("nullValue.ViewportIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport.width <= 0)
        {
            String msg = Logging.getMessage("generic.ViewportWidthIsInvalid", viewport.width);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport.height <= 0)
        {
            String msg = Logging.getMessage("generic.ViewportHeightIsInvalid", viewport.height);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Taken from the "OpenGL Technical FAQ & Troubleshooting Guide", section 20.010.
        // "How can I know which primitive a user has selected with the mouse?"
        // http://www.opengl.org/resources/faq/technical/selection.htm#sele0010

        // Compute the combined modelview-projection matrix.
        matrix.multiplyAndSet(projection, modelview);

        // Compute the model coordinate point on the near clip plane that corresponds to the specified screen point.
        // Return false if this point cannot be computed for any reason. This method uses the point and matrix temporary
        // properties to compute the result. We must make this computation before doing anything that depends on the
        // values of the point and matrix properties.
        if (!unProject(x, y, 0, matrix, viewport, point1))
            return false;

        // Compute the model coordinate point on the far clip plane that corresponds to the specified screen point.
        // Return false if this point cannot be computed for any reason. This method uses the point and matrix temporary
        // properties to compute the result. We must make this computation before doing anything that depends on the
        // values of the point and matrix properties.
        if (!unProject(x, y, 1, matrix, viewport, point2))
            return false;

        // Compute the ray origin as the eye point in model coordinates. The eye point is computed by transforming the
        // origin (0.0, 0.0, 0.0, 1.0) by the inverse of the modelview matrix. We have pre-computed the result and
        // stored it inline here to avoid an unnecessary matrix inverse and vector transform. This is equivalent to
        // result.getOrigin().set(0, 0, 0).transformBy4AndSet(matrix.invert(modelview)). The resultant point is stored
        // in the result's origin property.
        matrixInv.invertTransformMatrix(modelview);
        result.getOrigin().set(matrixInv.m[3], matrixInv.m[7], matrixInv.m[11]); // origin = eye point

        // Compute the ray direction as the vector pointing from the near clip plane to the far clip plane, and passing
        // through the specified screen point. We compute this vector buy subtracting the near point from the far point,
        // resulting in a vector that points from near to far. The resultant vector is stored in the result's direction
        // property.
        result.getDirection().subtract3AndSet(point2, point1).normalize3AndSet(); // direction = far - near

        return true;
    }

    /**
     * Transforms the model coordinate point (x, y, z) to screen coordinates using the specified transform parameters.
     * This does not retain any reference to the specified parameters or modify them in any way.
     * <p/>
     * The specified mvpMatrix and viewport define transformation from model coordinates to screen coordinates. After
     * this method returns, the result's x and y values represent the point's screen coordinates relative to the lower
     * left corner. The result's z value represents the point's depth as a value in the range [0.0, 1.0], where 0.0
     * corresponds to the near clip plane and 1.0 corresponds to the far clip plane.
     *
     * @param x         the model point's x-coordinate.
     * @param y         the model point's y-coordinate.
     * @param z         the model point's z-coordinate.
     * @param mvpMatrix the modelview-projection matrix, transforms model coordinates to clip coordinates.
     * @param viewport  the viewport rectangle, transforms clip coordinates to screen coordinates.
     * @param result    contains the point in screen coordinates after this method returns. This value is not modified
     *                  if this returns <code>false</code>.
     *
     * @return <code>true</code> if the point is successfully transformed, and <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if any of the mvpMatrix, viewport, or result are <code>null</code>, or if either
     *                                  of the viewport width or height are less than or equal to zero.
     */
    public static boolean project(double x, double y, double z, Matrix mvpMatrix, Rect viewport, Vec4 result)
    {
        if (mvpMatrix == null)
        {
            String msg = Logging.getMessage("nullValue.MVPMatrixIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport == null)
        {
            String msg = Logging.getMessage("nullValue.ViewportIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport.width <= 0)
        {
            String msg = Logging.getMessage("generic.ViewportWidthIsInvalid", viewport.width);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport.height <= 0)
        {
            String msg = Logging.getMessage("generic.ViewportHeightIsInvalid", viewport.height);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Set the point to the specified model coordinates, and set the w-coordinate to 1. The computations below
        // depend on a w-coordinate of 1.
        result.x = x;
        result.y = y;
        result.z = z;
        result.w = 1;

        // Transform the model coordinate point by the modelview matrix, then transform it by the projection matrix.
        // This transforms the point from model coordinates to clip coordinates, and is equivalent to transforming the
        // point by the concatenated modelview-projection matrix. This assumes that mvpMatrix has been computed by
        // multiplying the modelview and projection matrices together in the following order:
        // mvpMatrix = projection * modelview
        result.transformBy4AndSet(mvpMatrix);

        if (result.w == 0.0)
            return false;

        // Transform the point from clip coordinates in the range [-1.0, 1.0] to coordinates in the range [0.0, 1.0].
        // This intermediate step makes the final step of transforming to screen coordinates easier.
        result.w = (1.0 / result.w) * 0.5;
        result.x = result.x * result.w + 0.5;
        result.y = result.y * result.w + 0.5;
        result.z = result.z * result.w + 0.5;

        // Transform the point to screen coordinates, and assign it to the caller's result parameter.
        result.x = (result.x * viewport.width) + viewport.x;
        result.y = (result.y * viewport.height) + viewport.y;

        return true;
    }

    /**
     * Transforms the screen coordinate point (x, y, z) to model coordinates, using the specified transform parameters.
     * This does not retain any reference to the specified parameters or modify them in any way.
     * <p/>
     * The specified mvpMatrix and viewport define transformation from screen coordinates to model coordinates. The
     * screen point's x and y values represent its coordinates relative to the lower left corner. The screen point's z
     * value represents its depth as a value in the range [0.0, 1.0]. After this method returns, the results x, y, and z
     * coordinates represent the point's model coordinates.
     *
     * @param x         the screen point's x-coordinate, relative to the lower left corner.
     * @param y         the screen point's y-coordinate, relative to the lower left corner.
     * @param z         the screen point's z-coordinate, in the range [0.0, 1.0].
     * @param mvpMatrix the modelview-projection matrix, transforms model coordinates to clip coordinates.
     * @param viewport  the viewport rectangle, transforms clip coordinates to screen coordinates.
     * @param result    contains the point in model coordinates after this method returns. This value is not modified if
     *                  this returns <code>false</code>.
     *
     * @return <code>true</code> if the point is successfully transformed, and <code>false</code> otherwise.
     *
     * @throws IllegalArgumentException if any of the mvpMatrix, viewport, or result are <code>null</code>, or if either
     *                                  of the viewport width or height are less than or equal to zero.
     */
    public static boolean unProject(double x, double y, double z, Matrix mvpMatrix, Rect viewport, Vec4 result)
    {
        if (mvpMatrix == null)
        {
            String msg = Logging.getMessage("nullValue.MVPMatrixIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport == null)
        {
            String msg = Logging.getMessage("nullValue.ViewportIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport.width <= 0)
        {
            String msg = Logging.getMessage("generic.ViewportWidthIsInvalid", viewport.width);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (viewport.height <= 0)
        {
            String msg = Logging.getMessage("generic.ViewportHeightIsInvalid", viewport.height);
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        if (result == null)
        {
            String msg = Logging.getMessage("nullValue.ResultIsNull");
            Logging.error(msg);
            throw new IllegalArgumentException(msg);
        }

        // Compute a matrix that transforms a screen coordinate point by the inverse of the projection matrix then by
        // the inverse of the modelview matrix. This transforms a point from clip coordinates to model coordinates, and
        // is equivalent to transforming the point by the inverse of the concatenated projection-modelview matrix:
        // pmvInv = inverse(modelview * projection).
        if (matrixInv.invert(mvpMatrix) == null)
            return false;

        // Set the point to the specified screen coordinates, and set the w-coordinate to 1. The computations below
        // depend on a w-coordinate of 1.
        result.x = x;
        result.y = y;
        result.z = z;
        result.w = 1;

        // Transform the point from screen coordinates to coordinates in the range [0.0, 1.0].
        result.x = (result.x - viewport.x) / viewport.width;
        result.y = (result.y - viewport.y) / viewport.height;

        // Transform the point to clip coordinates in the range [-1, 1].
        result.x = (result.x * 2 - 1);
        result.y = (result.y * 2 - 1);
        result.z = result.z * 2 - 1;

        // Transform the point from clip coordinates to model coordinates.
        result.transformBy4AndSet(matrixInv);

        if (result.w == 0.0)
            return false;

        result.w = 1.0 / result.w;
        result.x *= result.w;
        result.y *= result.w;
        result.z *= result.w;

        return true;
    }

    /**
     * Convenience method to compute the log base 2 of a value.
     *
     * @param value the value to take the log of.
     *
     * @return the log base 2 of the specified value.
     */
    public static double logBase2(double value)
    {
        return Math.log(value) / Math.log(2d);
    }

    /**
     * Convenience method for testing whether a value is a power of two.
     *
     * @param value the value to test for power of 2
     *
     * @return true if power of 2, else false
     */
    public static boolean isPowerOfTwo(int value)
    {
        return (value == powerOfTwoCeiling(value));
    }

    /**
     * Returns the value that is the nearest power of 2 greater than or equal to the given value.
     *
     * @param reference the reference value. The power of 2 returned is greater than or equal to this value.
     *
     * @return the value that is the nearest power of 2 greater than or equal to the reference value
     */
    public static int powerOfTwoCeiling(int reference)
    {
        int power = (int) Math.ceil(Math.log(reference) / Math.log(2d));
        return (int) Math.pow(2d, power);
    }
}
