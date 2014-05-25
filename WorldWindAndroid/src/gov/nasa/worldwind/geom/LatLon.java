/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.geom;

import android.os.Parcel;
import android.os.Parcelable;
import gov.nasa.worldwind.util.Logging;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author dcollins
 * @version $Id: LatLon.java 812 2012-09-26 22:03:40Z dcollins $
 */
public class LatLon implements Parcelable {
	public static final LatLon ZERO = new LatLon(Angle.ZERO, Angle.ZERO);

	public final Angle latitude;
	public final Angle longitude;

	public LatLon() {
		this.latitude = new Angle();
		this.longitude = new Angle();
	}

	public LatLon(Angle latitude, Angle longitude) {
		if (latitude == null) {
			String msg = Logging.getMessage("nullValue.LatitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (longitude == null) {
			String msg = Logging.getMessage("nullValue.LongitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.latitude = latitude;
		this.longitude = longitude;
	}

	/**
	 * Obtains the latitude of this <code>LatLon</code>.
	 *
	 * @return this <code>LatLon</code>'s latitude
	 */
	public final Angle getLatitude()
	{
		return this.latitude;
	}

	/**
	 * Obtains the longitude of this <code>LatLon</code>.
	 *
	 * @return this <code>LatLon</code>'s longitude
	 */
	public final Angle getLongitude()
	{
		return this.longitude;
	}


	public static LatLon fromDegrees(double latitude, double longitude) {
		return new LatLon(Angle.fromDegrees(latitude), Angle.fromDegrees(longitude));
	}

	public static LatLon fromRadians(double latitude, double longitude) {
		return new LatLon(Angle.fromRadians(latitude), Angle.fromRadians(longitude));
	}

	public LatLon add(LatLon that)
	{
		if (that == null)
		{
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Angle lat = Angle.normalizedLatitude(this.latitude.add(that.latitude));
		Angle lon = Angle.normalizedLongitude(this.longitude.add(that.longitude));

		return new LatLon(lat, lon);
	}

	public LatLon subtract(LatLon that)
	{
		if (that == null)
		{
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Angle lat = Angle.normalizedLatitude(this.latitude.subtract(that.latitude));
		Angle lon = Angle.normalizedLongitude(this.longitude.subtract(that.longitude));

		return new LatLon(lat, lon);
	}

	/**
	 * Returns the linear interpolation of <code>value1</code> and <code>value2</code>, treating the geographic
	 * locations as simple 2D coordinate pairs.
	 *
	 * @param amount the interpolation factor
	 * @param value1 the first location.
	 * @param value2 the second location.
	 *
	 * @return the linear interpolation of <code>value1</code> and <code>value2</code>.
	 *
	 * @throws IllegalArgumentException if either location is null.
	 */
	public static LatLon interpolate(double amount, LatLon value1, LatLon value2)
	{
		if (value1 == null || value2 == null)
		{
			String message = Logging.getMessage("nullValue.LatLonIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (value1.equals(value2))
			return value1;

		Line line;
		try
		{
			line = Line.fromSegment(
					new Vec4(value1.getLongitude().radians, value1.getLatitude().radians, 0),
					new Vec4(value2.getLongitude().radians, value2.getLatitude().radians, 0));
		}
		catch (IllegalArgumentException e)
		{
			// Locations became coincident after calculations.
			return value1;
		}

		Vec4 p = new Vec4();
		line.getPointAt(amount, p);

		return LatLon.fromRadians(p.y, p.x);
	}

	/**
	 * Returns the an interpolated location along the great-arc between the specified locations. This does not retain
	 * any reference to the specified locations, or modify them in any way.
	 * <p/>
	 * The interpolation factor amount is a floating-point value in the range [0.0, 1.0] which defines the weight given to each location.
	 * 
	 * @param amount
	 *            the interpolation factor as a floating-point value in the range [0.0, 1.0].
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return an interpolated location along the great-arc between lhs and rhs.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static LatLon interpolateGreatCircle(double amount, LatLon lhs, LatLon rhs) {
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

		if (lhs.equals(rhs)) return lhs;

		double t = (amount < 0 ? 0 : (amount > 1 ? 1 : amount));

		Angle azimuth = LatLon.greatCircleAzimuth(lhs, rhs);
		Angle distance = LatLon.greatCircleDistance(lhs, rhs);
		Angle pathLength = Angle.fromDegrees(t * distance.degrees);

		return LatLon.greatCircleEndPosition(lhs, azimuth, pathLength);
	}

	/**
	 * Returns the an interpolated location along the rhumb line between the specified locations. This does not retain
	 * any reference to the specified locations, or modify them in any way.
	 * <p/>
	 * The interpolation factor amount is a floating-point value in the range [0.0, 1.0] which defines the weight given to each location.
	 * 
	 * @param amount
	 *            the interpolation factor as a floating-point value in the range [0.0, 1.0].
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return an interpolated location along the rhumb line between lhs and rhs.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static LatLon interpolateRhumb(double amount, LatLon lhs, LatLon rhs) {
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

		if (lhs.equals(rhs)) return lhs;

		double t = (amount < 0 ? 0 : (amount > 1 ? 1 : amount));

		Angle azimuth = LatLon.rhumbAzimuth(lhs, rhs);
		Angle distance = LatLon.rhumbDistance(lhs, rhs);
		Angle pathLength = Angle.fromDegrees(t * distance.degrees);

		return LatLon.rhumbEndPosition(lhs, azimuth, pathLength);
	}

	/**
	 * Computes the azimuth angle (clockwise from North) that points from the first location to the second location
	 * along a great circle arc. This angle can be used as the starting azimuth for a great circle arc that begins at
	 * the first location, and passes through the second location. Note that this angle is valid only at the first
	 * location; the azimuth along a great circle arc varies continuously at every point along the arc. This does not
	 * retain any reference to the specified locations, or modify them in any way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return Angle that points from the first location to the second location.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle greatCircleAzimuth(LatLon lhs, LatLon rhs) {
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
		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		if (lon1 == lon2) return lat1 > lat2 ? Angle.fromDegrees(180) : Angle.fromRadians(0);

		// Taken from "Map Projections - A Working Manual", page 30, equation 5-4b.
		// The atan2() function is used in place of the traditional atan(y/x) to simplify the case when x==0.
		double y = Math.cos(lat2) * Math.sin(lon2 - lon1);
		double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(lon2 - lon1);
		double azimuthRadians = Math.atan2(y, x);

		return Double.isNaN(azimuthRadians) ? Angle.fromRadians(0) : Angle.fromRadians(azimuthRadians);
	}

	/**
	 * Computes the great circle angular distance between two locations. The return value gives the distance as the
	 * angle between the two positions on the pi radius circle. In radians, this angle is also the arc length of the
	 * segment between the two positions on that circle. To compute a distance in meters from this value, multiply it by
	 * the radius of the globe. This does not retain any reference to the specified locations, or modify them in any
	 * way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return the angular distance between the two locations. In radians, this value is the arc length on the radius pi
	 *         circle.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle greatCircleDistance(LatLon lhs, LatLon rhs) {
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

		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		// "Haversine formula," taken from http://en.wikipedia.org/wiki/Great-circle_distance#Formul.C3.A6
		double a = Math.sin((lat2 - lat1) / 2.0);
		double b = Math.sin((lon2 - lon1) / 2.0);
		double c = a * a + +Math.cos(lat1) * Math.cos(lat2) * b * b;
		double distanceRadians = 2.0 * Math.asin(Math.sqrt(c));

		return Double.isNaN(distanceRadians) ? Angle.fromRadians(0) : Angle.fromRadians(distanceRadians);
	}

	/**
	 * Computes the location on a great circle arc with the given starting location, azimuth, and arc distance. This
	 * does not retain any reference to the location or angles, or modify them in any way.
	 * 
	 * @param location
	 *            the starting location.
	 * @param greatCircleAzimuth
	 *            great circle azimuth angle (clockwise from North).
	 * @param pathLength
	 *            arc distance to travel.
	 * @return a location on the great circle arc.
	 * @throws IllegalArgumentException
	 *             if any of the location, azimuth, or pathLength are <code>null</code>.
	 */
	public static LatLon greatCircleEndPosition(LatLon location, Angle greatCircleAzimuth, Angle pathLength) {
		if (location == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (greatCircleAzimuth == null) {
			String msg = Logging.getMessage("nullValue.AzimuthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (pathLength == null) {
			String msg = Logging.getMessage("nullValue.PathLengthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double lat = location.latitude.radians;
		double lon = location.longitude.radians;
		double azimuth = greatCircleAzimuth.radians;
		double distance = pathLength.radians;

		if (distance == 0) return location;

		// Taken from "Map Projections - A Working Manual", page 31, equation 5-5 and 5-6.
		double endLatRadians = Math.asin(Math.sin(lat) * Math.cos(distance) + Math.cos(lat) * Math.sin(distance) * Math.cos(azimuth));
		double endLonRadians = lon
				+ Math.atan2(Math.sin(distance) * Math.sin(azimuth), Math.cos(lat) * Math.cos(distance) - Math.sin(lat) * Math.sin(distance) * Math.cos(azimuth));

		if (Double.isNaN(endLatRadians) || Double.isNaN(endLonRadians)) return location;

		return LatLon.fromDegrees(Angle.normalizedDegreesLatitude(Angle.fromRadians(endLatRadians).degrees),
				Angle.normalizedDegreesLongitude(Angle.fromRadians(endLonRadians).degrees));
	}

	/**
	 * Computes the location on a great circle arc with the given starting location, azimuth, and arc distance.
	 *
	 * @param p                         LatLon of the starting location
	 * @param greatCircleAzimuthRadians great circle azimuth angle (clockwise from North), in radians
	 * @param pathLengthRadians         arc distance to travel, in radians
	 *
	 * @return LatLon location on the great circle arc.
	 */
	public static LatLon greatCircleEndPosition(LatLon p, double greatCircleAzimuthRadians, double pathLengthRadians)
	{
		if (p == null)
		{
			String message = Logging.getMessage("nullValue.LatLonIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		return greatCircleEndPosition(p,
				Angle.fromRadians(greatCircleAzimuthRadians), Angle.fromRadians(pathLengthRadians));
	}

	public static boolean locationsCrossDateline(LatLon p1, LatLon p2) {
		if (p1 == null || p2 == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// A segment cross the line if end pos have different longitude signs
		// and are more than 180 degrees longitude apart
		if (Math.signum(p1.longitude.degrees) != Math.signum(p2.longitude.degrees)) {
			double delta = Math.abs(p1.longitude.degrees - p2.longitude.degrees);
			if (delta > 180 && delta < 360) return true;
		}

		return false;
	}

	public static boolean locationsCrossDateLine(Iterable<? extends LatLon> locations)
	{
		if (locations == null)
		{
			String msg = Logging.getMessage("nullValue.LocationsListIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		LatLon pos = null;
		for (LatLon posNext : locations)
		{
			if (pos != null)
			{
				// A segment cross the line if end pos have different longitude signs
				// and are more than 180 degrees longitude apart
				if (Math.signum(pos.getLongitude().degrees) != Math.signum(posNext.getLongitude().degrees))
				{
					double delta = Math.abs(pos.getLongitude().degrees - posNext.getLongitude().degrees);
					if (delta > 180 && delta < 360)
						return true;
				}
			}
			pos = posNext;
		}

		return false;
	}

	/**
	 * Computes the azimuth angle (clockwise from North) of a rhumb line (a line of constant heading) between two
	 * locations. This does not retain any reference to the specified locations, or modify them in any way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return azimuth the angle of a rhumb line between the two locations.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle rhumbAzimuth(LatLon lhs, LatLon rhs) {
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

		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		// Taken from http://www.movable-type.co.uk/scripts/latlong.html
		double dLon = lon2 - lon1;
		double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
		// If lonChange over 180 take shorter rhumb across 180 meridian.
		if (Math.abs(dLon) > Math.PI) {
			dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
		}
		double azimuthRadians = Math.atan2(dLon, dPhi);

		return Double.isNaN(azimuthRadians) ? Angle.fromRadians(0) : Angle.fromRadians(azimuthRadians);
	}

	/**
	 * Returns two locations with the most extreme latitudes on the great circle with the given starting location and
	 * azimuth.
	 *
	 * @param location location on the great circle.
	 * @param azimuth  great circle azimuth angle (clockwise from North).
	 *
	 * @return two locations where the great circle has its extreme latitudes.
	 *
	 * @throws IllegalArgumentException if either <code>location</code> or <code>azimuth</code> are null.
	 */
	public static LatLon[] greatCircleExtremeLocations(LatLon location, Angle azimuth)
	{
		if (location == null)
		{
			String message = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (azimuth == null)
		{
			String message = Logging.getMessage("nullValue.AzimuthIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		double lat0 = location.getLatitude().radians;
		double az = azimuth.radians;

		// Derived by solving the function for longitude on a great circle against the desired longitude. We start with
		// the equation in "Map Projections - A Working Manual", page 31, equation 5-5:
		//
		// lat = asin( sin(lat0) * cos(c) + cos(lat0) * sin(c) * cos(Az) )
		//
		// Where (lat0, lon) are the starting coordinates, c is the angular distance along the great circle from the
		// starting coordinate, and Az is the azimuth. All values are in radians.
		//
		// Solving for angular distance gives distance to the equator:
		//
		// tan(c) = -tan(lat0) / cos(Az)
		//
		// The great circle is by definition centered about the Globe's origin. Therefore intersections with the
		// equator will be antipodal (exactly 180 degrees opposite each other), as will be the extreme latitudes.
		// By observing the symmetry of a great circle, it is also apparent that the extreme latitudes will be 90
		// degrees from either intersection with the equator.
		//
		// d1 = c + 90
		// d2 = c - 90

		double tanDistance = -Math.tan(lat0) / Math.cos(az);
		double distance = Math.atan(tanDistance);

		Angle extremeDistance1 = Angle.fromRadians(distance + (Math.PI / 2.0));
		Angle extremeDistance2 = Angle.fromRadians(distance - (Math.PI / 2.0));

		return new LatLon[]
				{
						greatCircleEndPosition(location, azimuth, extremeDistance1),
						greatCircleEndPosition(location, azimuth, extremeDistance2)
				};
	}

	/**
	 * Returns two locations with the most extreme latitudes on the great circle arc defined by, and limited to, the two
	 * locations.
	 *
	 * @param begin beginning location on the great circle arc.
	 * @param end   ending location on the great circle arc.
	 *
	 * @return two locations with the most extreme latitudes on the great circle arc.
	 *
	 * @throws IllegalArgumentException if either <code>begin</code> or <code>end</code> are null.
	 */
	public static LatLon[] greatCircleArcExtremeLocations(LatLon begin, LatLon end)
	{
		if (begin == null)
		{
			String message = Logging.getMessage("nullValue.BeginIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		if (end == null)
		{
			String message = Logging.getMessage("nullValue.EndIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		LatLon minLatLocation = null;
		LatLon maxLatLocation = null;
		double minLat = Angle.POS90.degrees;
		double maxLat = Angle.NEG90.degrees;

		// Compute the min and max latitude and associated locations from the arc endpoints.
		for (LatLon ll : java.util.Arrays.asList(begin, end))
		{
			if (minLat >= ll.getLatitude().degrees)
			{
				minLat = ll.getLatitude().degrees;
				minLatLocation = ll;
			}
			if (maxLat <= ll.getLatitude().degrees)
			{
				maxLat = ll.getLatitude().degrees;
				maxLatLocation = ll;
			}
		}

		// Compute parameters for the great circle arc defined by begin and end. Then compute the locations of extreme
		// latitude on entire the great circle which that arc is part of.
		Angle greatArcAzimuth = greatCircleAzimuth(begin, end);
		Angle greatArcDistance = greatCircleDistance(begin, end);
		LatLon[] greatCircleExtremes = greatCircleExtremeLocations(begin, greatArcAzimuth);

		// Determine whether either of the extreme locations are inside the arc defined by begin and end. If so,
		// adjust the min and max latitude accordingly.
		for (LatLon ll : greatCircleExtremes)
		{
			Angle az = LatLon.greatCircleAzimuth(begin, ll);
			Angle d = LatLon.greatCircleDistance(begin, ll);

			// The extreme location must be between the begin and end locations. Therefore its azimuth relative to
			// the begin location should have the same signum, and its distance relative to the begin location should
			// be between 0 and greatArcDistance, inclusive.
			if (Math.signum(az.degrees) == Math.signum(greatArcAzimuth.degrees))
			{
				if (d.degrees >= 0 && d.degrees <= greatArcDistance.degrees)
				{
					if (minLat >= ll.getLatitude().degrees)
					{
						minLat = ll.getLatitude().degrees;
						minLatLocation = ll;
					}
					if (maxLat <= ll.getLatitude().degrees)
					{
						maxLat = ll.getLatitude().degrees;
						maxLatLocation = ll;
					}
				}
			}
		}

		return new LatLon[] {minLatLocation, maxLatLocation};
	}

	/**
	 * Returns two locations with the most extreme latitudes on the sequence of great circle arcs defined by each pair
	 * of locations in the specified iterable.
	 *
	 * @param locations the pairs of locations defining a sequence of great circle arcs.
	 *
	 * @return two locations with the most extreme latitudes on the great circle arcs.
	 *
	 * @throws IllegalArgumentException if <code>locations</code> is null.
	 */
	public static LatLon[] greatCircleArcExtremeLocations(Iterable<? extends LatLon> locations)
	{
		if (locations == null)
		{
			String message = Logging.getMessage("nullValue.LocationsListIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		LatLon minLatLocation = null;
		LatLon maxLatLocation = null;

		LatLon lastLocation = null;

		for (LatLon ll : locations)
		{
			if (lastLocation != null)
			{
				LatLon[] extremes = LatLon.greatCircleArcExtremeLocations(lastLocation, ll);
				if (extremes == null)
					continue;

				if (minLatLocation == null || minLatLocation.getLatitude().degrees > extremes[0].getLatitude().degrees)
					minLatLocation = extremes[0];
				if (maxLatLocation == null || maxLatLocation.getLatitude().degrees < extremes[1].getLatitude().degrees)
					maxLatLocation = extremes[1];
			}

			lastLocation = ll;
		}

		return new LatLon[] {minLatLocation, maxLatLocation};
	}

	/**
	 * Computes the length of the rhumb line between two locations. The return value gives the distance as the angular
	 * distance between the two positions on the pi radius circle. In radians, this angle is also the arc length of the
	 * segment between the two positions on that circle. To compute a distance in meters from this value, multiply it by
	 * the radius of the globe. This does not retain any reference to the specified locations, or modify them in any
	 * way.
	 * 
	 * @param lhs
	 *            the first location.
	 * @param rhs
	 *            the second location.
	 * @return the arc length of the rhumb line between the two locations. In radians, this value is the arc length on
	 *         the radius pi circle.
	 * @throws IllegalArgumentException
	 *             if either location is <code>null</code>.
	 */
	public static Angle rhumbDistance(LatLon lhs, LatLon rhs) {
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

		double lat1 = lhs.latitude.radians;
		double lon1 = lhs.longitude.radians;
		double lat2 = rhs.latitude.radians;
		double lon2 = rhs.longitude.radians;

		if (lat1 == lat2 && lon1 == lon2) return Angle.fromRadians(0);

		// Taken from http://www.movable-type.co.uk/scripts/latlong.html
		double dLat = lat2 - lat1;
		double dLon = lon2 - lon1;
		double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
		double q = dLat / dPhi;
		if (Double.isNaN(dPhi) || Double.isNaN(q)) {
			q = Math.cos(lat1);
		}
		// If lonChange over 180 take shorter rhumb across 180 meridian.
		if (Math.abs(dLon) > Math.PI) {
			dLon = dLon > 0 ? -(2 * Math.PI - dLon) : (2 * Math.PI + dLon);
		}

		double distanceRadians = Math.sqrt(dLat * dLat + q * q * dLon * dLon);

		return Double.isNaN(distanceRadians) ? Angle.fromRadians(0) : Angle.fromRadians(distanceRadians);
	}

	/**
	 * Computes the location on a rhumb line with the given starting location, rhumb azimuth, and arc distance along the
	 * line. This does not retain any reference to the specified location or angles, or modify them in any way.
	 * 
	 * @param location
	 *            the starting location.
	 * @param rhumbAzimuth
	 *            rhumb azimuth angle (clockwise from North).
	 * @param pathLength
	 *            arc distance to travel.
	 * @return a location on the rhumb line.
	 * @throws IllegalArgumentException
	 *             if any of the location, azimuth, or pathLength are <code>null</code>.
	 */
	public static LatLon rhumbEndPosition(LatLon location, Angle rhumbAzimuth, Angle pathLength) {
		if (location == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (rhumbAzimuth == null) {
			String msg = Logging.getMessage("nullValue.AzimuthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (pathLength == null) {
			String msg = Logging.getMessage("nullValue.PathLengthIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double lat1 = location.latitude.radians;
		double lon1 = location.longitude.radians;
		double azimuth = rhumbAzimuth.radians;
		double distance = pathLength.radians;

		if (distance == 0) return location;

		// Taken from http://www.movable-type.co.uk/scripts/latlong.html
		double lat2 = lat1 + distance * Math.cos(azimuth);
		double dPhi = Math.log(Math.tan(lat2 / 2.0 + Math.PI / 4.0) / Math.tan(lat1 / 2.0 + Math.PI / 4.0));
		double q = (lat2 - lat1) / dPhi;
		if (Double.isNaN(dPhi) || Double.isNaN(q) || Double.isInfinite(q)) {
			q = Math.cos(lat1);
		}
		double dLon = distance * Math.sin(azimuth) / q;
		// Handle latitude passing over either pole.
		if (Math.abs(lat2) > Math.PI / 2.0) {
			lat2 = lat2 > 0 ? Math.PI - lat2 : -Math.PI - lat2;
		}
		double lon2 = (lon1 + dLon + Math.PI) % (2 * Math.PI) - Math.PI;

		if (Double.isNaN(lat2) || Double.isNaN(lon2)) return location;

		return LatLon.fromDegrees(Angle.normalizedDegreesLatitude(Angle.fromRadians(lat2).degrees), Angle.normalizedDegreesLongitude(Angle.fromRadians(lon2).degrees));
	}

	/**
	 * Computes the location on a rhumb line with the given starting location, rhumb azimuth, and arc distance along the
	 * line.
	 *
	 * @param p                   LatLon of the starting location
	 * @param rhumbAzimuthRadians rhumb azimuth angle (clockwise from North), in radians
	 * @param pathLengthRadians   arc distance to travel, in radians
	 *
	 * @return LatLon location on the rhumb line.
	 */
	public static LatLon rhumbEndPosition(LatLon p, double rhumbAzimuthRadians, double pathLengthRadians)
	{
		if (p == null)
		{
			String message = Logging.getMessage("nullValue.LatLonIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		return rhumbEndPosition(p, Angle.fromRadians(rhumbAzimuthRadians), Angle.fromRadians(pathLengthRadians));
	}

	public LatLon copy() {
		return new LatLon(this.latitude.copy(), this.longitude.copy());
	}

	public LatLon set(LatLon location) {
		if (location == null) {
			String msg = Logging.getMessage("nullValue.LocationIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.latitude.set(location.latitude);
		this.longitude.set(location.longitude);

		return this;
	}

	public LatLon set(Angle latitude, Angle longitude) {
		if (latitude == null) {
			String msg = Logging.getMessage("nullValue.LatitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (longitude == null) {
			String msg = Logging.getMessage("nullValue.LongitudeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.latitude.set(latitude);
		this.longitude.set(longitude);

		return this;
	}

	public LatLon setDegrees(double latitude, double longitude) {
		this.latitude.setDegrees(latitude);
		this.longitude.setDegrees(longitude);

		return this;
	}

	public LatLon setRadians(double latitude, double longitude) {
		this.latitude.setRadians(latitude);
		this.longitude.setRadians(longitude);

		return this;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || this.getClass() != o.getClass()) return false;

		LatLon that = (LatLon) o;
		return this.latitude.equals(that.latitude) && this.longitude.equals(that.longitude);
	}

	@Override
	public int hashCode() {
		int result;
		result = this.latitude.hashCode();
		result = 29 * result + this.longitude.hashCode();
		return result;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("(");
		sb.append(this.latitude.toString()).append(", ");
		sb.append(this.longitude.toString());
		sb.append(")");
		return sb.toString();
	}

	private static final ClassLoader LOADER = LatLon.class.getClassLoader();

	public static final Creator<LatLon> CREATOR = new Creator<LatLon>() {
		@Override
		public LatLon createFromParcel(Parcel in) {
			return new LatLon(in.<Angle>readParcelable(LOADER), in.<Angle>readParcelable(LOADER));
		}

		@Override
		public LatLon[] newArray(int size) {
			return new LatLon[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeParcelable(this.latitude, flags);
		dest.writeParcelable(this.longitude, flags);
	}
}
