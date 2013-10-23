/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.globes;

import gov.nasa.worldwind.WWObject;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.terrain.ElevationModel;
import gov.nasa.worldwind.terrain.SectorGeometryList;
import gov.nasa.worldwind.terrain.Tessellator;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: Globe.java 827 2012-10-08 19:32:08Z tgaskins $
 */
public interface Globe extends WWObject {
	/**
	 * Returns a typed state key identifying this globe's current configuration. Can be used to subsequently determine
	 * whether the globe's configuration has changed.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return a state key for the globe's current configuration.
	 * @throws IllegalArgumentException
	 *             if the draw context is <code>null</code>.
	 */
	GlobeStateKey getGlobeStateKey(DrawContext dc);

	/**
	 * Indicates this globe's elevation model.
	 * 
	 * @return this globe's elevation model.
	 */
	ElevationModel getElevationModel();

	/**
	 * Specifies this globe's elevation model.
	 * 
	 * @param elevationModel
	 *            this globe's elevation model. May be null to indicate no elevation model.
	 */
	void setElevationModel(ElevationModel elevationModel);

	/**
	 * Returns this globe's current tessellator.
	 * 
	 * @return the globe's current tessellator.
	 */
	Tessellator getTessellator();

	/**
	 * Specifies this globe's tessellator.
	 * 
	 * @param tessellator
	 *            the new tessellator. Specify null to use the default tessellator.
	 */
	void setTessellator(Tessellator tessellator);

	/**
	 * Tessellate this globe for the currently visible region.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return the tessellation, or null if the tessellation failed or the draw context identifies no visible region.
	 * @throws IllegalStateException
	 *             if the globe has no tessellator and a default tessellator cannot be created.
	 */
	SectorGeometryList tessellate(DrawContext dc);

	double getRadius();

	Intersection[] intersect(Line line);

	boolean getIntersectionPosition(Line line, Position result);

	/**
	 * Indicates the best elevation resolution attainable for a specified sector.
	 * 
	 * @param sector
	 *            the sector in question. If null, the globe's best overall resolution is returned. This is the best
	 *            attainable at <em>some</em> locations but not necessarily at all locations.
	 * @return the best resolution attainable for the specified sector, in radians, or {@link Double#MAX_VALUE} if this
	 *         globe has no elevation model or if the sector does not intersect this globe's elevation model.
	 */
	double getBestResolution(Sector sector);

	double getMinElevation();

	double getMaxElevation();

	/**
	 * Returns the minimum and maximum elevations within a specified sector on this Globe. This returns a two-element
	 * array filled with zero if this Globe has no elevation model.
	 * 
	 * @param sector
	 *            the sector in question.
	 * @return A two-element <code>double</code> array indicating the sector's minimum and maximum elevations,
	 *         respectively. These elements are the global minimum and maximum if the local minimum and maximum values
	 *         are currently unknown, or zero if this Globe has no elevation model.
	 */
	double[] getMinAndMaxElevations(Sector sector);

	double getElevation(LatLon location);

	double getElevation(Angle latitude, Angle longitude);

	double getElevations(Sector sector, int numLat, int numLon, double targetResolution, double[] buffer);

	Vec4 computePointFromPosition(Position position);

	Vec4 computePointFromLocation(LatLon location);

	Vec4 computePointFromPosition(LatLon location, double metersElevation);

	Vec4 computePointFromPosition(Angle latitude, Angle longitude, double metersElevation);

	void computePointFromPosition(Position position, Vec4 result);

	void computePointFromPosition(LatLon location, double metersElevation, Vec4 result);

	void computePointFromPosition(Angle latitude, Angle longitude, double metersElevation, Vec4 result);

	/**
	 * Maps a geographic grid of positions to world Cartesian coordinates. The grid is evenly spaced locations in
	 * latitude and longitude defined the sector, numLat, and numLon. Each grid position's elevation in meters is
	 * defined by the metersElevation array. The result array is populated with the world Cartesian coordinate at each
	 * grid position. Both the metersElevation and result array are organized by assigning index zero to the sector's
	 * lower left corner and proceeding in row major order. Both the metersElevation and result array s must have length
	 * of at least numLat * numLon.
	 * 
	 * @param sector
	 *            the sector in question.
	 * @param numLat
	 *            the grid height in number of latitude positions.
	 * @param numLon
	 *            the grid width in number of longitude positions.
	 * @param metersElevation
	 *            an array containing the elevation for each position. The array must be pre-allocated and
	 *            contain at least numLat * numLon elements.
	 * @param result
	 *            contains the Cartesian points corresponding to each grid position after this method
	 *            returns. The array must be pre-allocated and contain at least numLat * numLon non-null
	 *            elements.
	 * @throws IllegalArgumentException
	 *             if the sector is <code>null</code>, if numLat or numLon are less than 1, if
	 *             metersElevation is <code>null</code>, if result is <code>null</code>, or if
	 *             either metersElevation or result or has length less than numLat * numLon.
	 */
	void computePointsFromPositions(Sector sector, int numLat, int numLon, double[] metersElevation, Vec4[] result);

	Position computePositionFromPoint(Vec4 point);

	void computePositionFromPoint(Vec4 point, Position result);

	/**
	 * Returns the normal to this Globe at the specified geographic location.
	 * 
	 * @param location
	 *            the latitude and longitude of the location.
	 * @return this Globe's Cartesian surface normal at the specified location.
	 */
	Vec4 computeSurfaceNormalAtLocation(LatLon location);

	Vec4 computeSurfaceNormalAtLocation(Angle latitude, Angle longitude);

	void computeSurfaceNormalAtLocation(LatLon location, Vec4 result);

	void computeSurfaceNormalAtLocation(Angle latitude, Angle longitude, Vec4 result);

	/**
	 * Computes a vector perpendicular to the surface of this globe, at a model coordinate point.
	 * 
	 * @param point
	 *            a point in model coordinates at which to compute the normal vector.
	 * @param result
	 *            contains a vector perpendicular to the surface of this globe at the specified point after this
	 *            method exits.
	 * @throws IllegalArgumentException
	 *             if either the point or the result are <code>null</code>.
	 */
	void computeSurfaceNormalAtPoint(Vec4 point, Vec4 result);

	Vec4 computeNorthPointingTangentAtLocation(LatLon location);

	Vec4 computeNorthPointingTangentAtLocation(Angle latitude, Angle longitude);

	void computeNorthPointingTangentAtLocation(LatLon location, Vec4 result);

	void computeNorthPointingTangentAtLocation(Angle latitude, Angle longitude, Vec4 result);

	Matrix computeViewOrientationAtPosition(Angle latitude, Angle longitude, double metersElevation);
}
