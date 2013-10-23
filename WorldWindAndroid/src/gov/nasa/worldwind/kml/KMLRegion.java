/*
 * Copyright (C) 2012 DreamHammer.com
 */

package gov.nasa.worldwind.kml;

/**
 * Represents the KML <i>Region</i> element and provides access to its contents. Regions define an area of interest
 * described by a geographic bounding box and an optional minimum and maximum altitude.
 * <p/>
 * <strong>Bounding Box</strong> </br> A Region's bounding box controls when the Region is active by defining a volume
 * that must intersect the viewing frustum. The bounding box is computed according to the <code>altitudeMode</code>
 * attribute of a Region's geographic <code>LatLonAltBox</code> as follows:
 * <p/>
 * <ul> <li><strong>clampToGround (default)</strong>: The bounding box encloses the terrain surface in the sector
 * defined by the north, south, east, and west limits of this Region's <code>LatLonAltBox</code>.</li>
 * <li><strong>relativeToGround</strong>: The bounding box encloses the volume in the sector defined by the north,
 * south, east, and west limits of the Region's <code>LatLonAltBox</code>, and who's upper and lower altitude are
 * specified by its minAltitude and maxAltitude, relative to ground level.</li> <li><strong>absolute</strong>: The
 * bounding box encloses the volume in the sector defined by the north, south, east, and west limits of the Region's
 * <code>LatLonAltBox</code>, and who's upper and lower altitude are specified by its minAltitude and maxAltitude,
 * relative to mean sea level.</li> </ul>
 * <p/>
 * <strong>Level of Detail</strong> <br/> A Region's level of detail determines when it is active by defining an upper
 * and lower boundary on the Region's screen area or the Region's distance to the view. The level of detail is computed
 * according to the <code>altitudeMode</code> attribute of a Region's geographic <code>LatLonAltBox</code> as follows:
 * <p/>
 * <ul> <li><strong>clampToGround (default)</strong>: The level of detail is determined by computing the distance from
 * the eye point and Region sector, scaling the distance by the <code>KMLTraversalContext's</code> detail hint, then
 * comparing that scaled distance to the Region's min and max pixel sizes in meters (the Region sector's area divided by
 * <code>minLodPixels</code> and <code>maxLodPixels</code>, respectively). The Region is active when the scaled distance
 * is less than or equal to the min pixel size in meters and greater than the max pixel size in meters. The detail hint
 * may be specified by calling <code>setDetailHint</code> on the top level <code>KMLRoot</code> (the KMLRoot loaded by
 * the application).</li> <li><strong>relativeToGround</strong>: The level of detail is determined by computing the
 * number of pixels the Region occupies on screen, and comparing that pixel count to the Region's
 * <code>minLodPixels</code> and <code>maxLodPixels</code>. The Region is active when the pixel count is greater or
 * equal to <code>minLodPixels</code> and less than <code>maxLodPixels</code>.</li> <li><strong>absolute</strong>: The
 * level of detail is determined by computing the number of pixels the Region occupies on screen, and comparing that
 * pixel count to the Region's <code>minLodPixels</code> and <code>maxLodPixels</code>. The Region is active when the
 * pixel count is greater or equal to <code>minLodPixels</code> and less than <code>maxLodPixels</code>.</li> </ul>
 * <p/>
 * In order to prevent Regions with adjacent level of detail ranges from activating at the same time, Region gives
 * priority to higher level of detail ranges. For example, suppose that two KML features representing different detail
 * levels of a Collada model have Regions with LOD range 100-200 and 200-300. Region avoids activating both features in
 * the event that both their level of detail criteria are met by giving priority to the second range: 200-300.
 * <p/>
 * <strong>KML Feature Hierarchies</strong> <br/> When a Region is attached to a KML feature, the feature and its
 * descendants are displayed only when the Region is active. A Region is active when its bounding box is in view and its
 * level of detail criteria are met. Region provides the <code>isActive</code> method for determining if a Region is
 * active for a specified <code>DrawContext</code>.
 * <p/>
 * Regions do not apply directly to KML containers, because a descendant feature can override the container's Region
 * with its own Region. If a feature does not specify a Region it inherits the Region of its nearest ancestor. Since a
 * child feature's Region may be larger or have a less restrictive level of detail range than its ancestor's Region, the
 * visibility of an entire KML feature tree cannot be determined based on a container's Region. Instead, visibility must
 * be determined at each leaf feature.
 * <p/>
 * <strong>Limitations</strong> <br/> The Region bounding box must lie between -90 to 90 degrees latitude, and -180 to
 * 180 degrees longitude. Regions that span the date line are currently not supported.
 *
 * @author tag
 * @version $Id: KMLRegion.java 771 2012-09-14 19:30:10Z tgaskins $
 */
public class KMLRegion
{
    /**
     * The default time in milliseconds a <code>RegionData</code> element may exist in this Region's
     * <code>regionDataCache</code> before it must be regenerated: 6 seconds.
     */
    protected static final int DEFAULT_DATA_GENERATION_INTERVAL = 6000;
    /**
     * The default time in milliseconds a <code>RegionData</code> element may exist in this Region's
     * <code>regionDataCache</code> without being used before it is evicted: 1 minute.
     */
    protected static final int DEFAULT_UNUSED_DATA_LIFETIME = 60000;
    /**
     * The default value that configures KML scene resolution to screen resolution as the viewing distance changes:
     * 2.8.
     */
    protected static final double DEFAULT_DETAIL_HINT_ORIGIN = 2.8;
}
