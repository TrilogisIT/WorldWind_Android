/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind.render;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.cache.ShapeDataCache;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.Cylinder;
import gov.nasa.worldwind.geom.Extent;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.terrain.Terrain;
import gov.nasa.worldwind.util.BufferUtil;
import gov.nasa.worldwind.util.Logging;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import android.opengl.GLES20;


/**
 * Class used to render a pyramid inverted, use for render Marker
 * 
 * @author Nicola Meneghini,Nicola Dorigatti Trilogis SRL
 * @version 1
 */
public class GPSMarker extends AbstractShape {
	/** The default interior color. */
	protected static final Color DEFAULT_INTERIOR_COLOR = Color.pink();
	/** The default outline color. */
	protected static final Color DEFAULT_OUTLINE_COLOR = Color.red();
	/** The default position color. */
	protected static final Color DEFAULT_POSITION_COLOR = Color.white();
	/** The default GPSMarker type. */
	protected static final String DEFAULT_GPSMarker_TYPE = AVKey.LINEAR;
	/**
	 * The offset applied to a terrain following GPSMarker's depth values to to ensure it shows over the terrain: -0.1.
	 * Values less than zero pull the GPSMarker's depth values in front of the terrain while values greater than push the
	 * GPSMarker's depth values behind the terrain.
	 */
	protected static final double SURFACE_GPSMarker_DEPTH_OFFSET = -0.01;
	/** The default number of tessellation points between the specified GPSMarker positions. */
	protected static final int DEFAULT_NUM_SUBSEGMENTS = 10;
	/** The default terrain conformance target. */
	protected static final double DEFAULT_TERRAIN_CONFORMANCE = 10;
	/** The default distance from the eye beyond which positions dots are not drawn. */
	protected static final double DEFAULT_DRAW_POSITIONS_THRESHOLD = 1e6;
	/** The default scale for position dots. The scale is applied to the current outline width to produce the dot size. */
	protected static final double DEFAULT_DRAW_POSITIONS_SCALE = 10;

	/**
	 * Overrides the default materials specified in the base class.
	 */
	static {
		defaultAttributes.setInteriorColor(DEFAULT_INTERIOR_COLOR);
		defaultAttributes.setOutlineColor(DEFAULT_OUTLINE_COLOR);
	}

	/** The PositionColors interface defines an RGBA color for each of a GPSMarker's original positions. */
	public static interface PositionColors {
		/**
		 * Returns an RGBA color corresponding to the specified position and ordinal. This returns <code>null</code> if
		 * a color cannot be determined for the specified position and ordinal. The specified <code>position</code> is
		 * guaranteed to be one of the same Position references passed to a GPSMarker at construction or in a call to {@link GPSMarker#setPositions(Iterable)}.
		 * <p/>
		 * The specified <code>ordinal</code> denotes the position's ordinal number as it appears in the position list passed to the GPSMarker. Ordinal numbers start
		 * with 0 and increase by 1 for every originally specified position. For example, the first three GPSMarker positions have ordinal values 0, 1, 2.
		 * <p/>
		 * The returned color's RGB components must <em>not</em> be premultiplied by its Alpha component.
		 * 
		 * @param position
		 *            the GPSMarker position the color corresponds to.
		 * @param ordinal
		 *            the ordinal number of the specified position.
		 * @return an RGBA color corresponding to the position and ordinal, or <code>null</code> if a color cannot be
		 *         determined.
		 */
		Color getColor(Position position, int ordinal);
	}

	/**
	 * Maintains globe-dependent computed data such as Cartesian vertices and extents. One entry exists for each
	 * distinct globe that this shape encounters in calls to {@link AbstractShape#render(DrawContext)}. See {@link AbstractShape}.
	 */
	protected static class GPSMarkerData extends AbstractShapeData {
		/** The positions formed from applying GPSMarker type and terrain conformance. */
		protected ArrayList<Position> tessellatedPositions;
		/**
		 * The colors corresponding to each tessellated position, or <code>null</code> if the GPSMarker's <code>positionColors</code> is <code>null</code>.
		 */
		protected ArrayList<Color> tessellatedColors;
		/**
		 * The model coordinate vertices to render, all relative to this shape data's reference center. If the GPSMarker is
		 * extruded, the base vertices are interleaved: Vcap, Vbase, Vcap, Vbase, ...
		 */
		protected FloatBuffer renderedGPSMarker;
		// /**
		// * Indices to the <code>renderedGPSMarker</code> identifying the vertices of the originally specified boundary
		// * positions and their corresponding terrain point. This is used to draw vertical lines at those positions when
		// * the GPSMarker is extruded.
		// */
		// protected IntBuffer polePositions; // identifies original positions and corresponding ground points
		// /**
		// * Indices to the <code>renderedGPSMarker</code> identifying the vertices of the originally specified boundary
		// * positions. (Not their terrain points as well, as <code>polePositions</code> does.)
		// */
		// protected IntBuffer positionPoints; // identifies the original positions in the rendered GPSMarker.
		/** Indicates whether the rendered GPSMarker has extrusion points in addition to GPSMarker points. */
		protected boolean hasExtrusionPoints; // true when the rendered GPSMarker contains extrusion points
		/**
		 * Indicates the offset in number of floats to the first RGBA color tuple in <code>renderedGPSMarker</code>. This is <code>0</code> if
		 * <code>renderedGPSMarker</code> has no RGBA color tuples.
		 */
		protected int colorOffset;
		/**
		 * Indicates the stride in number of floats between the first element of consecutive vertices in <code>renderedGPSMarker</code>.
		 */
		protected int vertexStride;
		/** Indicates the number of vertices represented by <code>renderedGPSMarker</code>. */
		protected int vertexCount;

		public GPSMarkerData(DrawContext dc, GPSMarker shape) {
			super(dc, shape.minExpiryTime, shape.maxExpiryTime);
		}

		/**
		 * The positions resulting from tessellating this GPSMarker. If the GPSMarker's attributes don't cause tessellation, then
		 * the positions returned are those originally specified.
		 * 
		 * @return the positions computed by GPSMarker tessellation.
		 */
		public List<Position> getTessellatedPositions() {
			return this.tessellatedPositions;
		}

		public void setTessellatedPositions(ArrayList<Position> tessellatedPositions) {
			this.tessellatedPositions = tessellatedPositions;
		}

		/**
		 * Indicates the colors corresponding to each position in <code>tessellatedPositions</code>, or <code>null</code> if the GPSMarker does not have per-position
		 * colors.
		 * 
		 * @return the colors corresponding to each GPSMarker position, or <code>null</code> if the GPSMarker does not have
		 *         per-position colors.
		 */
		public List<Color> getTessellatedColors() {
			return this.tessellatedColors;
		}

		/**
		 * Specifies the colors corresponding to each position in <code>tessellatedPositions</code>, or <code>null</code> to specify that the GPSMarker does not have
		 * per-position colors. The entries in the specified
		 * list must have a one-to-one correspondence with the entries in <code>tessellatedPositions</code>.
		 * 
		 * @param tessellatedColors
		 *            the colors corresponding to each GPSMarker position, or <code>null</code> if the GPSMarker
		 *            does not have per-position colors.
		 */
		public void setTessellatedColors(ArrayList<Color> tessellatedColors) {
			this.tessellatedColors = tessellatedColors;
		}

		/**
		 * The Cartesian coordinates of the tessellated positions. If GPSMarker verticals are enabled, this GPSMarker also
		 * contains the ground points corresponding to the GPSMarker positions.
		 * 
		 * @return the Cartesian coordinates of the tessellated positions.
		 */
		public FloatBuffer getRenderedGPSMarker() {
			return this.renderedGPSMarker;
		}

		public void setRenderedGPSMarker(FloatBuffer renderedGPSMarker) {
			this.renderedGPSMarker = renderedGPSMarker;
		}

		// /**
		// * Returns a buffer of indices into the rendered GPSMarker ({@link #renderedGPSMarker} that identify the originally
		// * specified positions that remain after tessellation. These positions are those of the position dots, if
		// * drawn.
		// *
		// * @return the GPSMarker's originally specified positions that survived tessellation.
		// */
		// public IntBuffer getPositionPoints()
		// {
		// return this.positionPoints;
		// }
		//
		// public void setPositionPoints(IntBuffer posPoints)
		// {
		// this.positionPoints = posPoints;
		// }

		// /**
		// * Returns a buffer of indices into the rendered GPSMarker ({@link #renderedGPSMarker} that identify the top and bottom
		// * vertices of this GPSMarker's vertical line segments.
		// *
		// * @return the GPSMarker's pole positions.
		// */
		// public IntBuffer getPolePositions()
		// {
		// return this.polePositions;
		// }
		//
		// public void setPolePositions(IntBuffer polePositions)
		// {
		// this.polePositions = polePositions;
		// }

		/**
		 * Indicates whether this GPSMarker is extruded and the extrusion points have been computed.
		 * 
		 * @return true if the GPSMarker is extruded and the extrusion points are computed, otherwise false.
		 */
		public boolean hasExtrusionPoints() {
			return this.hasExtrusionPoints;
		}

		public void setHasExtrusionPoints(boolean hasExtrusionPoints) {
			this.hasExtrusionPoints = hasExtrusionPoints;
		}

		/**
		 * Indicates the offset in number of floats to the first RGBA color tuple in <code>renderedGPSMarker</code>. This
		 * returns <code>0</code> if <code>renderedGPSMarker</code> has no RGBA color tuples.
		 * 
		 * @return the offset in number of floats to the first RGBA color tuple in <code>renderedGPSMarker</code>.
		 */
		public int getColorOffset() {
			return this.colorOffset;
		}

		/**
		 * Specifies the offset in number of floats to the first RGBA color tuple in <code>renderedGPSMarker</code>. Specify
		 * 0 if <code>renderedGPSMarker</code> has no RGBA color tuples.
		 * 
		 * @param offset
		 *            the offset in number of floats to the first RGBA color tuple in <code>renderedGPSMarker</code>.
		 */
		public void setColorOffset(int offset) {
			this.colorOffset = offset;
		}

		/**
		 * Indicates the stride in number of floats between the first element of consecutive vertices in <code>renderedGPSMarker</code>.
		 * 
		 * @return the stride in number of floats between vertices in in <code>renderedGPSMarker</code>.
		 */
		public int getVertexStride() {
			return this.vertexStride;
		}

		/**
		 * Specifies the stride in number of floats between the first element of consecutive vertices in <code>renderedGPSMarker</code>.
		 * 
		 * @param stride
		 *            the stride in number of floats between vertices in in <code>renderedGPSMarker</code>.
		 */
		public void setVertexStride(int stride) {
			this.vertexStride = stride;
		}

		/**
		 * Indicates the number of vertices in <code>renderedGPSMarker</code>.
		 * 
		 * @return the the number of verices in <code>renderedGPSMarker</code>.
		 */
		public int getVertexCount() {
			return this.vertexCount;
		}

		/**
		 * Specifies the number of vertices in <code>renderedGPSMarker</code>. Specify 0 if <code>renderedGPSMarker</code> contains no vertices.
		 * 
		 * @param count
		 *            the the number of verices in <code>renderedGPSMarker</code>.
		 */
		public void setVertexCount(int count) {
			this.vertexCount = count;
		}
	}

	@Override
	protected AbstractShapeData createCacheEntry(DrawContext dc) {
		return new GPSMarkerData(dc, this);
	}

	protected GPSMarkerData getCurrentGPSMarkerData() {
		return (GPSMarkerData) this.getCurrentData();
	}

	protected Iterable<? extends Position> positions; // the positions as provided by the application
	protected Position referencePosition;
	protected int numPositions; // the number of positions in the positions field.
	protected PositionColors positionColors; // defines a color at each application-provided position.
	// protected static ByteBuffer pickPositionColors; // defines the colors used to resolve position point picking.

	protected String GPSMarkerType = DEFAULT_GPSMarker_TYPE;
	protected boolean followTerrain; // true if altitude mode indicates terrain following
	protected boolean extrude;
	protected double terrainConformance = DEFAULT_TERRAIN_CONFORMANCE;
	protected int numSubsegments = DEFAULT_NUM_SUBSEGMENTS;
	protected boolean drawVerticals = true;
	protected boolean showPositions = false;
	protected double showPositionsThreshold = DEFAULT_DRAW_POSITIONS_THRESHOLD;
	protected double showPositionsScale = DEFAULT_DRAW_POSITIONS_SCALE;
	protected double side;
	protected double height;
	private boolean first=true;

	/** Creates a GPSMarker with no positions. */
	public GPSMarker() {
	}

	/**
	 * Creates a GPSMarker with specified positions.
	 * 
	 * @param position
	 *            the GPSMarker position. 
	 * @param side
	 *            side of the square base of the pyramid
	 * @param height
	 *            height of the pyramid
	 */
	public GPSMarker(Position position,double side,double height) {
		this.side=side;
		this.height=height;
		this.setPositions(position);
	}
	
	

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Overridden to assign this GPSMarker's pickSupport property to a new GPSMarkerPickSupport instance.
	 */
	@Override
	protected void initialize() {
		 //this.pickSupport = new PickSupport();
	}

	@Override
	protected void reset() {
		for (ShapeDataCache.ShapeDataCacheEntry entry : this.shapeDataCache) {
			((GPSMarkerData) entry).tessellatedPositions = null;
			((GPSMarkerData) entry).tessellatedColors = null;
		}

		super.reset();
	}


	/**
	 * Set the position 
	 * 
	 * @return this GPSMarker's positions. Will be null if no positions have been specified.
	 */
	public void setPositions(Position position) {
		if (position == null) {
			String msg = Logging.getMessage("nullValue.PositionsListIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.positions = Arrays.asList(
    	        Position.fromDegrees(position.latitude.degrees, position.longitude.degrees,position.elevation),
    	        Position.fromDegrees(position.latitude.degrees+side/2, position.longitude.degrees+side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees+side/2, position.longitude.degrees-side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees-side/2, position.longitude.degrees-side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees, position.longitude.degrees,position.elevation),
    	        Position.fromDegrees(position.latitude.degrees-side/2, position.longitude.degrees+side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees+side/2, position.longitude.degrees+side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees-side/2, position.longitude.degrees-side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees, position.longitude.degrees,position.elevation),
    	        Position.fromDegrees(position.latitude.degrees+side/2, position.longitude.degrees-side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees-side/2, position.longitude.degrees-side/2,position.elevation+height),
    	        Position.fromDegrees(position.latitude.degrees-side/2, position.longitude.degrees+side/2,position.elevation+height));
		this.computePositionCount();
		this.referencePosition = this.numPositions < 1 ? null : this.positions.iterator().next(); // use first position
		this.reset();
	}
	
	
	public Position getPosition(){
		Position pos = null;
		if(null != this.positions){
			if(this.positions.iterator().hasNext()){
				pos = this.positions.iterator().next();
			}
		}
		return pos;
	}

	

	/**
	 * Indicates whether to extrude this GPSMarker. Extruding the GPSMarker extends a filled interior from the GPSMarker to the
	 * terrain.
	 * 
	 * @return true to extrude this GPSMarker, otherwise false.
	 * @see #setExtrude(boolean)
	 */
	public boolean isExtrude() {
		return extrude;
	}

	/**
	 * Specifies whether to extrude this GPSMarker. Extruding the GPSMarker extends a filled interior from the GPSMarker to the
	 * terrain.
	 * 
	 * @param extrude
	 *            true to extrude this GPSMarker, otherwise false. The default value is false.
	 */
	public void setExtrude(boolean extrude) {
		this.extrude = extrude;
		this.reset();
	}

	/**
	 * Indicates whether this GPSMarker is terrain following.
	 * 
	 * @return true if terrain following, otherwise false.
	 * @see #setFollowTerrain(boolean)
	 */
	public boolean isFollowTerrain() {
		return this.followTerrain;
	}

	/**
	 * Specifies whether this GPSMarker is terrain following.
	 * 
	 * @param followTerrain
	 *            true if terrain following, otherwise false. The default value is false.
	 */
	public void setFollowTerrain(boolean followTerrain) {
		if (this.followTerrain == followTerrain) return;

		this.followTerrain = followTerrain;
		this.reset();
	}

	

	/**
	 * Indicates the terrain conformance target when this GPSMarker follows the terrain. The value indicates the maximum
	 * number of pixels between which intermediate positions of a GPSMarker segment -- the GPSMarker portion between two specified
	 * positions -- are computed.
	 * 
	 * @return the terrain conformance, in pixels.
	 * @see #setTerrainConformance(double)
	 */
	public double getTerrainConformance() {
		return terrainConformance;
	}

	/**
	 * Specifies how accurately this GPSMarker must adhere to the terrain when the GPSMarker is terrain following. The value
	 * specifies the maximum number of pixels between tessellation points. Lower values increase accuracy but decrease
	 * performance.
	 * 
	 * @param terrainConformance
	 *            the number of pixels between tessellation points.
	 */
	public void setTerrainConformance(double terrainConformance) {
		this.terrainConformance = terrainConformance;
		this.reset();
	}

	/**
	 * Indicates this GPSMarkers GPSMarker type.
	 * 
	 * @return the GPSMarker type.
	 * @see #setGPSMarkerType(String)
	 */
	public String getGPSMarkerType() {
		return GPSMarkerType;
	}

	/**
	 * Specifies this GPSMarker's GPSMarker type. Recognized values are {@link AVKey#GREAT_CIRCLE}, {@link AVKey#RHUMB_LINE} and {@link AVKey#LINEAR}.
	 * 
	 * @param GPSMarkerType
	 *            the current GPSMarker type. The default value is {@link AVKey#LINEAR}.
	 */
	public void setGPSMarkerType(String GPSMarkerType) {
		this.GPSMarkerType = GPSMarkerType;
		this.reset();
	}

	/**
	 * Indicates whether to draw at each specified GPSMarker position when this GPSMarker is extruded.
	 * 
	 * @return true to draw the lines, otherwise false.
	 * @see #setDrawVerticals(boolean)
	 */
	public boolean isDrawVerticals() {
		return drawVerticals;
	}

	/**
	 * Specifies whether to draw vertical lines at each specified GPSMarker position when this GPSMarker is extruded.
	 * 
	 * @param drawVerticals
	 *            true to draw the lines, otherwise false. The default value is true.
	 */
	public void setDrawVerticals(boolean drawVerticals) {
		this.drawVerticals = drawVerticals;
		this.reset();
	}

	/**
	 * Indicates whether dots are drawn at the GPSMarker's original positions.
	 * 
	 * @return true if dots are drawn, otherwise false.
	 */
	public boolean isShowPositions() {
		return showPositions;
	}

	/**
	 * Specifies whether to draw dots at the original positions of the GPSMarker. The dot color and size are controlled by
	 * the GPSMarker's outline material and scale attributes.
	 * 
	 * @param showPositions
	 *            true if dots are drawn at each original (not tessellated) position, otherwise false.
	 */
	public void setShowPositions(boolean showPositions) {
		this.showPositions = showPositions;
	}

	/**
	 * Indicates the scale factor controlling the size of dots drawn at this GPSMarker's specified positions. The scale is
	 * multiplied by the outline width given in this GPSMarker's {@link ShapeAttributes} to determine the actual size of the
	 * dots, in pixels. See {@link ShapeAttributes#setOutlineWidth(double)}.
	 * 
	 * @return the shape's draw-position scale. The default scale is 10.
	 */
	public double getShowPositionsScale() {
		return showPositionsScale;
	}

	/**
	 * Specifies the scale factor controlling the size of dots drawn at this GPSMarker's specified positions. The scale is
	 * multiplied by the outline width given in this GPSMarker's {@link ShapeAttributes} to determine the actual size of the
	 * dots, in pixels. See {@link ShapeAttributes#setOutlineWidth(double)}.
	 * 
	 * @param showPositionsScale
	 *            the new draw-position scale.
	 */
	public void setShowPositionsScale(double showPositionsScale) {
		this.showPositionsScale = showPositionsScale;
	}

	/**
	 * Indicates the eye distance from this shape's center beyond which position dots are not drawn.
	 * 
	 * @return the eye distance at which to enable or disable position dot drawing. The default is 1e6 meters, which
	 *         typically causes the dots to always be drawn.
	 */
	public double getShowPositionsThreshold() {
		return showPositionsThreshold;
	}

	/**
	 * Specifies the eye distance from this shape's center beyond which position dots are not drawn.
	 * 
	 * @param showPositionsThreshold
	 *            the eye distance at which to enable or disable position dot drawing.
	 */
	public void setShowPositionsThreshold(double showPositionsThreshold) {
		this.showPositionsThreshold = showPositionsThreshold;
	}

	public Sector getSector() {
		if (this.sector == null && this.positions != null) this.sector = Sector.fromBoundingSector(this.positions);

		return this.sector;
	}

	@Override
	protected boolean mustDrawInterior() {
		return super.mustDrawInterior() && this.getCurrentGPSMarkerData().hasExtrusionPoints;
	}

	@Override
	protected boolean mustApplyLighting(DrawContext dc) {
		return false; // TODO: Lighting; need to compute normals
	}

	protected boolean mustRegenerateGeometry(DrawContext dc) {
		if (this.getCurrentGPSMarkerData() == null || this.getCurrentGPSMarkerData().renderedGPSMarker == null) return true;

		if (this.getCurrentGPSMarkerData().tessellatedPositions == null) return true;

		if (dc.getVerticalExaggeration() != this.getCurrentGPSMarkerData().getVerticalExaggeration()) return true;

		// if ((this.getAltitudeMode() == null || AVKey.ABSOLUTE.equals(this.getAltitudeMode()))
		// && this.getCurrentGPSMarkerData().getGlobeStateKey() != null
		// && this.getCurrentGPSMarkerData().getGlobeStateKey().equals(dc.getGlobe().getGlobeStateKey(dc)))
		// return false;

		return super.mustRegenerateGeometry(dc);
	}

	/**
	 * Indicates whether this GPSMarker's defining positions and the positions in between are located on the underlying
	 * terrain. This returns <code>true</code> if this GPSMarker's altitude mode is <code>WorldWind.CLAMP_TO_GROUND</code> and the follow-terrain property is
	 * <code>true</code>. Otherwise this returns <code>false</code>.
	 * 
	 * @return <code>true</code> if this GPSMarker's positions and the positions in between are located on the underlying
	 *         terrain, and <code>false</code> otherwise.
	 */
	protected boolean isSurfaceGPSMarker() {
		return AVKey.CLAMP_TO_GROUND.equals(this.getAltitudeMode()) && this.isFollowTerrain();
	}

	@Override
	protected void determineActiveAttributes() {
		// When the interior is drawn the vertex buffer has a different layout, so it may need to be rebuilt.
		boolean isDrawInterior = this.activeAttributes.isEnableInterior();

		super.determineActiveAttributes();

		if (this.activeAttributes != null && this.activeAttributes.isEnableInterior() != isDrawInterior) this.getCurrentData().setExpired(true);
	}

	/** Counts the number of positions in this GPSMarker's specified positions. */
	protected void computePositionCount() {

		this.numPositions=12;
	}

	@Override
	protected boolean doMakeOrderedRenderable(DrawContext dc) {
		// currentData must be set prior to calling this method
		GPSMarkerData GPSMarkerData = this.getCurrentGPSMarkerData();

		this.computeReferenceCenter(dc);
		if (GPSMarkerData.getReferencePoint() == null) return false;

		GPSMarkerData.setTransformMatrixFromReferencePosition();

		// Recompute tessellated positions because the geometry or view may have changed.
		this.makeTessellatedPositions(dc, GPSMarkerData);
		if (GPSMarkerData.tessellatedPositions == null || GPSMarkerData.tessellatedPositions.size() < 2) return false;

		// Create the rendered Cartesian points.
		int previousSize = GPSMarkerData.renderedGPSMarker != null ? GPSMarkerData.renderedGPSMarker.limit() : 0;
		this.computeGPSMarker(dc, GPSMarkerData.tessellatedPositions, GPSMarkerData);
		if (GPSMarkerData.renderedGPSMarker == null || GPSMarkerData.renderedGPSMarker.limit() < 6) return false;

		if (GPSMarkerData.renderedGPSMarker.limit() > previousSize) this.clearCachedVbos(dc);

		GPSMarkerData.setExtent(this.computeExtent(GPSMarkerData));

		// If the shape is less that a pixel in size, don't render it.
		if (this.getExtent() == null || dc.isSmall(this.getExtent(), 1)) return false;

		if (!this.intersectsFrustum(dc)) return false;

		GPSMarkerData.setEyeDistance(this.computeEyeDistance(dc, GPSMarkerData));
		GPSMarkerData.setGlobeStateKey(dc.getGlobe().getGlobeStateKey(dc));
		GPSMarkerData.setVerticalExaggeration(dc.getVerticalExaggeration());

		return true;
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * Overridden to place this GPSMarker behind other ordered renderables when this GPSMarker is entirely located on the underlying terrain. In this case this GPSMarker must
	 * be drawn first to ensure that other ordered renderables are correctly drawn on top of it and are not affected by this GPSMarker's depth offset. If two GPSMarkers
	 * are both located on the terrain, they are drawn with respect to their layer ordering.
	 */
	@Override
	protected void addOrderedRenderable(DrawContext dc) {
		if (this.isSurfaceGPSMarker()) {
			dc.addOrderedRenderableToBack(this); // Specify that this GPSMarker should appear behind other ordered objects.
		} else {
			super.addOrderedRenderable(dc);
		}
	}

	@Override
	protected boolean isOrderedRenderableValid(DrawContext dc) {
		return this.getCurrentGPSMarkerData().renderedGPSMarker != null && this.getCurrentGPSMarkerData().vertexCount >= 2;
	}

	@Override
	protected void applyModelviewProjectionMatrix(DrawContext dc) {
		if (this.isSurfaceGPSMarker()) {
			// Modify the standard modelview-projection matrix by applying a depth offset to the perspective matrix.
			// This depth offset pulls the line forward just a bit to ensure it shows over the terrain.
			this.currentMatrix.set(dc.getView().getProjectionMatrix());
			this.currentMatrix.offsetPerspectiveDepth(SURFACE_GPSMarker_DEPTH_OFFSET);
			this.currentMatrix.multiplyAndSet(dc.getView().getModelviewMatrix());
			this.currentMatrix.multiplyAndSet(this.getCurrentGPSMarkerData().getTransformMatrix());
			dc.getCurrentProgram().loadUniformMatrix("mvpMatrix", this.currentMatrix);
		} else {
			super.applyModelviewProjectionMatrix(dc);
		}
	}

	/**
	 * {@inheritDoc}
	 * <p/>
	 * If this GPSMarker is entirely located on the terrain, this applies an offset to the GPSMarker's depth values to to ensure it shows over the terrain. This does not
	 * apply a depth offset in any other case to avoid incorrectly drawing the GPSMarker over objects it should be behind, including the terrain. In addition to
	 * applying a depth offset, this disables writing to the depth buffer to avoid causing subsequently drawn ordered renderables to incorrectly fail the depth
	 * test. Since this GPSMarker is located on the terrain, the terrain already provides the necessary depth values and we can be certain that other ordered
	 * renderables should appear on top of it.
	 */
	@Override
	protected void doDrawOutline(DrawContext dc) {
		boolean isSurfaceGPSMarker = this.isSurfaceGPSMarker(); // Keep track for OpenGL state recovery.

		try {
			if (isSurfaceGPSMarker) GLES20.glDepthMask(false);

			int[] vboIds = this.getVboIds(dc);
			if (vboIds != null) this.doDrawOutlineVBO(dc, vboIds, this.getCurrentGPSMarkerData());
			else {
				String msg = Logging.getMessage("GL.VertexBufferObjectNotInCache", this);
				Logging.warning(msg);
			}
		} finally {
			if (isSurfaceGPSMarker) GLES20.glDepthMask(true); // Restore the default depth mask.
		}
	}

	protected void doDrawOutlineVBO(DrawContext dc, int[] vboIds, GPSMarkerData GPSMarkerData) {
		int attribLocation = dc.getCurrentProgram().getAttribLocation("vertexPoint");
		if (attribLocation < 0) {
			String msg = Logging.getMessage("GL.VertexAttributeIsMissing", "vertexPoint");
			Logging.warning(msg);
		}

		
		int stride = GPSMarkerData.vertexStride;
		int count = GPSMarkerData.vertexCount;

		// Specify the data for the program's vertexPoint attribute, if one exists. This attribute is enabled in
		// beginRendering. Convert stride from number of elements to number of bytes.
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
		GLES20.glVertexAttribPointer(attribLocation, 3, GLES20.GL_FLOAT, false, 4 * stride, 0);
		
		//draw pyramid	
		GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0,count-4);
		
		Color color = new Color(54/255d,54/255d,134/255d);//color blue

		this.currentColor.set(color).premultiply();
		dc.getCurrentProgram().loadUniformColor("color", this.currentColor);
		
		//draw borders
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, count-5);
		GLES20.glDrawArrays(GLES20.GL_LINES, count-4, 2);
		GLES20.glDrawArrays(GLES20.GL_LINES, count-2, 2);
		
	}


	/**
	 * Draws this GPSMarker's interior when the GPSMarker is extruded.
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void doDrawInterior(DrawContext dc) {
		int[] vboIds = this.getVboIds(dc);
		if (vboIds != null) this.doDrawInteriorVBO(dc, vboIds, this.getCurrentGPSMarkerData());
		else {
			String msg = Logging.getMessage("GL.VertexBufferObjectNotInCache", this);
			Logging.warning(msg);
		}
		
		
		//this.doDrawInteriorVBO(dc, this.getCurrentGPSMarkerData());
	}

	
	/**
	 * Draws this GPSMarker's interior
	 * 
	 * @param dc
	 *            the current draw context.
	 */
	protected void doDrawInteriorVBO(DrawContext dc, int[] vboIds, GPSMarkerData GPSMarkerData) {
		int attribLocation = dc.getCurrentProgram().getAttribLocation("vertexPoint");
		if (attribLocation < 0) {
			String msg = Logging.getMessage("GL.VertexAttributeIsMissing", "vertexPoint");
			Logging.warning(msg);
		}

		int stride = GPSMarkerData.vertexStride;
		//int count = GPSMarkerData.vertexCount;

		// Specify the data for the program's vertexPoint attribute, if one exists. This attribute is enabled in
		// beginRendering. Convert stride from number of elements to number of bytes.
		GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
		GLES20.glVertexAttribPointer(attribLocation, 3, GLES20.GL_FLOAT, false, 4 * stride, 0);
		
		GLES20.glDrawArrays(GLES20.GL_LINE_STRIP, 0, GPSMarkerData.vertexCount);
		
	}

	/**
	 * Computes the shape's model-coordinate GPSMarker from a list of positions. Applies the GPSMarker's terrain-conformance
	 * settings. Adds extrusion points -- those on the ground -- when the GPSMarker is extruded.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param positions
	 *            the positions to create a GPSMarker for.
	 * @param GPSMarkerData
	 *            the current globe-specific GPSMarker data.
	 */
	protected void computeGPSMarker(DrawContext dc, List<Position> positions, GPSMarkerData GPSMarkerData) {
		GPSMarkerData.hasExtrusionPoints = false;

		FloatBuffer GPSMarker = GPSMarkerData.renderedGPSMarker;

		if (AVKey.CLAMP_TO_GROUND.equals(this.getAltitudeMode())) GPSMarker = this.computePointsRelativeToTerrain(dc, positions, 0d, GPSMarker, GPSMarkerData);
		else if (AVKey.RELATIVE_TO_GROUND.equals(this.getAltitudeMode())) GPSMarker = this.computePointsRelativeToTerrain(dc, positions, null, GPSMarker, GPSMarkerData);
		else GPSMarker = this.computeAbsolutePoints(dc, positions, GPSMarker, GPSMarkerData);

		GPSMarker.flip(); // since the GPSMarker is reused the limit might not be the same as the previous usage

		GPSMarkerData.renderedGPSMarker = GPSMarker;
		GPSMarkerData.vertexCount = GPSMarker.limit() / GPSMarkerData.vertexStride;
	}

	/**
	 * Computes a terrain-conforming, model-coordinate GPSMarker from a list of positions, using either a specified altitude
	 * or the altitudes in the specified positions. Adds extrusion points -- those on the ground -- when the GPSMarker is
	 * extruded and the specified single altitude is not 0.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param positions
	 *            the positions to create a GPSMarker for.
	 * @param altitude
	 *            if non-null, the height above the terrain to use for all positions. If null, each position's
	 *            altitude is used as the height above the terrain.
	 * @param GPSMarker
	 *            a buffer in which to store the computed points. May be null. The buffer is not used if it is
	 *            null or tool small for the required number of points. A new buffer is created in that case and
	 *            returned by this method. This method modifies the buffer,s position and limit fields.
	 * @param GPSMarkerData
	 *            the current globe-specific GPSMarker data.
	 * @return the buffer in which to place the computed points.
	 */
	protected FloatBuffer computePointsRelativeToTerrain(DrawContext dc, List<Position> positions, Double altitude, FloatBuffer GPSMarker, GPSMarkerData GPSMarkerData) {
		int numPoints = 12;
		int elemsPerPoint = 3;
		Iterator<Color> colorIter = (GPSMarkerData.tessellatedColors != null ? GPSMarkerData.tessellatedColors.iterator() : null);
		float[] color = (GPSMarkerData.tessellatedColors != null ? new float[4] : null);

		if (GPSMarker == null || GPSMarker.capacity() < elemsPerPoint * numPoints) GPSMarker = BufferUtil.newFloatBuffer(elemsPerPoint * numPoints);

		GPSMarker.clear();

		Terrain terrain = dc.getVisibleTerrain();
		Vec4 referencePoint = GPSMarkerData.getReferencePoint();
		
		
		Globe globe = dc.getGlobe();
		
		Position firstPos = positions.get(0);
			//Vec4 first = terrain.getSurfacePoint(firstPos.latitude, firstPos.longitude, altitude != null ? altitude : firstPos.elevation);
			double elev = terrain.getElevation(firstPos.latitude, firstPos.longitude);
		

		for (Position pos : positions) {
			if((first)&&elev< Double.MAX_VALUE){
				pos.elevation= pos.elevation+elev;
			}
			Vec4 pt = globe.computePointFromPosition(pos);
			GPSMarker.put((float) (pt.x - referencePoint.x));
			GPSMarker.put((float) (pt.y - referencePoint.y));
			GPSMarker.put((float) (pt.z - referencePoint.z));
			
			
			if (colorIter != null && colorIter.hasNext()) {
				colorIter.next().toArray4f(color, 0);
				GPSMarker.put(color);
			}
		}
		if(first){
			first=false;
		}
		
		
		
		

		GPSMarkerData.colorOffset = (GPSMarkerData.tessellatedColors != null ? 3 : 0);
		GPSMarkerData.vertexStride = elemsPerPoint;

		return GPSMarker;
	}
	

	/**
	 * Computes a model-coordinate GPSMarker from a list of positions, using the altitudes in the specified positions. Adds
	 * extrusion points -- those on the ground -- when the GPSMarker is extruded and the specified single altitude is not 0.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param positions
	 *            the positions to create a GPSMarker for.
	 * @param GPSMarker
	 *            a buffer in which to store the computed points. May be null. The buffer is not used if it is
	 *            null or tool small for the required number of points. A new buffer is created in that case and
	 *            returned by this method. This method modifies the buffer,s position and limit fields.
	 * @param GPSMarkerData
	 *            the current globe-specific GPSMarker data.
	 * @return the buffer in which to place the computed points.
	 */
	protected FloatBuffer computeAbsolutePoints(DrawContext dc, List<Position> positions, FloatBuffer GPSMarker, GPSMarkerData GPSMarkerData) {
		//int numPoints = this.isExtrude() ? (2 * 360+2) : 362;
		int numPoints = 12;
		//int elemsPerPoint = (GPSMarkerData.tessellatedColors != null ? 7 : 3);
		int elemsPerPoint = 3;
		Iterator<Color> colorIter = (GPSMarkerData.tessellatedColors != null ? GPSMarkerData.tessellatedColors.iterator() : null);
		float[] color = (GPSMarkerData.tessellatedColors != null ? new float[4] : null);

		if (GPSMarker == null || GPSMarker.capacity() < numPoints*elemsPerPoint) GPSMarker = BufferUtil.newFloatBuffer(numPoints*elemsPerPoint);

		GPSMarker.clear();

		Globe globe = dc.getGlobe();
		Vec4 referencePoint = GPSMarkerData.getReferencePoint();
			for (Position pos : positions) {
				Vec4 pt = globe.computePointFromPosition(pos);
				GPSMarker.put((float) (pt.x - referencePoint.x));
				GPSMarker.put((float) (pt.y - referencePoint.y));
				GPSMarker.put((float) (pt.z - referencePoint.z));

				if (colorIter != null && colorIter.hasNext()) {
					colorIter.next().toArray4f(color, 0);
					GPSMarker.put(color);
				}
			}
			
			

		GPSMarkerData.colorOffset = (GPSMarkerData.tessellatedColors != null ? 3 : 0);
		GPSMarkerData.vertexStride = elemsPerPoint;

		return GPSMarker;
	}

	/**
	 * Computes a point on a GPSMarker and adds it to the renderable geometry. Used to generate extrusion vertices.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param position
	 *            the GPSMarker position.
	 * @param color
	 *            an array of length 4 containing the position's corresponding color as RGBA values in the range
	 *            [0, 1], or <code>null</code> if the position has no associated color.
	 * @param GPSMarker
	 *            the GPSMarker to append to. Assumes that the GPSMarker has adequate capacity.
	 * @param GPSMarkerData
	 *            the current globe-specific GPSMarker data.
	 */
	protected void appendTerrainPoint(DrawContext dc, Position position, float[] color, FloatBuffer GPSMarker, GPSMarkerData GPSMarkerData) {
		Vec4 referencePoint = GPSMarkerData.getReferencePoint();
		Vec4 pt = dc.getVisibleTerrain().getSurfacePoint(position.latitude, position.longitude, 1000d);
		GPSMarker.put((float) (pt.x - referencePoint.x));
		GPSMarker.put((float) (pt.y - referencePoint.y));
		GPSMarker.put((float) (pt.z - referencePoint.z));

		if (color != null) GPSMarker.put(color);

		GPSMarkerData.hasExtrusionPoints = true;
	}
	
	
	protected void appendTerrainPoint(DrawContext dc, Vec4 position, float[] color, FloatBuffer GPSMarker, GPSMarkerData GPSMarkerData) {
		Vec4 referencePoint = GPSMarkerData.getReferencePoint();
		GPSMarker.put((float) (position.x - referencePoint.x));
		GPSMarker.put((float) (position.y - referencePoint.y));
		GPSMarker.put((float) (position.z - referencePoint.z));

		if (color != null) GPSMarker.put(color);

		GPSMarkerData.hasExtrusionPoints = true;
	}


	/**
	 * Generates positions defining this GPSMarker with GPSMarker type and terrain-conforming properties applied. Builds the
	 * GPSMarker's <code>tessellatedPositions</code> and <code>polePositions</code> fields.
	 * 
	 * @param GPSMarkerData
	 *            the current globe-specific GPSMarker data.
	 * @param dc
	 *            the current draw context.
	 */
	protected void makeTessellatedPositions(DrawContext dc, GPSMarkerData GPSMarkerData) {
		if (this.numPositions < 2) return;

		if (GPSMarkerData.tessellatedPositions == null || GPSMarkerData.tessellatedPositions.size() < this.numPositions) {
			int size = (this.numSubsegments * (this.numPositions - 1) + 1) * (this.isExtrude() ? 2 : 1);
			GPSMarkerData.tessellatedPositions = new ArrayList<Position>(size);
			GPSMarkerData.tessellatedColors = (this.positionColors != null) ? new ArrayList<Color>(size) : null;
		} else {
			GPSMarkerData.tessellatedPositions.clear();

			if (GPSMarkerData.tessellatedColors != null) GPSMarkerData.tessellatedColors.clear();
		}

		// if (GPSMarkerData.polePositions == null || GPSMarkerData.polePositions.capacity() < this.numPositions * 2)
		// GPSMarkerData.polePositions = BufferUtil.newIntBuffer(this.numPositions * 2);
		// else
		// GPSMarkerData.polePositions.clear();
		//
		// if (GPSMarkerData.positionPoints == null || GPSMarkerData.positionPoints.capacity() < this.numPositions)
		// GPSMarkerData.positionPoints = BufferUtil.newIntBuffer(this.numPositions);
		// else
		// GPSMarkerData.positionPoints.clear();

		this.makePositions(dc, GPSMarkerData);

		GPSMarkerData.tessellatedPositions.trimToSize();
		// GPSMarkerData.polePositions.flip();
		// GPSMarkerData.positionPoints.flip();

		if (GPSMarkerData.tessellatedColors != null) GPSMarkerData.tessellatedColors.trimToSize();
	}

	// /**
	// * Computes this GPSMarker's distance from the eye point, for use in determining when to show positions points. The value
	// * returned is only an approximation because the eye distance varies along the GPSMarker.
	// *
	// * @param dc the current draw context.
	// * @param GPSMarkerData this GPSMarker's current shape data.
	// *
	// * @return the distance of the shape from the eye point. If the eye distance cannot be computed, the eye position's
	// * elevation is returned instead.
	// */
	// protected double getDistanceMetric(DrawContext dc, GPSMarkerData GPSMarkerData)
	// {
	// if (GPSMarkerData.getExtent() != null)
	// return GPSMarkerData.getExtent().distanceTo(dc.getView().getEyePoint());
	//
	// return dc.getView().getEyePosition(dc.getGlobe()).elevation;
	// }

	protected void makePositions(DrawContext dc, GPSMarkerData GPSMarkerData) {
		Iterator<? extends Position> iter = this.positions.iterator();
		Position posA = iter.next();
		int ordinalA = 0;
		Color colorA = this.getColor(posA, ordinalA);

		this.addTessellatedPosition(posA, colorA, ordinalA, GPSMarkerData); // add the first position of the GPSMarker

		// Tessellate each segment of the GPSMarker.
		Vec4 ptA = this.computePoint(dc.getVisibleTerrain(), posA);

		while (iter.hasNext()) {
			Position posB = iter.next();
			int ordinalB = ordinalA + 1;
			Color colorB = this.getColor(posB, ordinalB);
			Vec4 ptB = this.computePoint(dc.getVisibleTerrain(), posB);

			// If the segment is very small or not visible, don't tessellate, just add the segment's end position.
			if (this.isSmall(dc, ptA, ptB, 8) || !this.isSegmentVisible(dc, posA, posB, ptA, ptB)) this.addTessellatedPosition(posB, colorB, ordinalB, GPSMarkerData);
			else this.makeSegment(dc, posA, posB, ptA, ptB, colorA, colorB, ordinalA, ordinalB, GPSMarkerData);

			posA = posB;
			ptA = ptB;
			ordinalA = ordinalB;
			colorA = colorB;
		}
	}

	/**
	 * Adds a position to this GPSMarker's <code>tessellatedPositions</code> list. If the specified color is not <code>null</code>, this adds the color to this
	 * GPSMarker's <code>tessellatedColors</code> list. If the specified
	 * ordinal is not <code>null</code>, this adds the position's index to the <code>polePositions</code> and <code>positionPoints</code> index buffers.
	 * 
	 * @param pos
	 *            the position to add.
	 * @param color
	 *            the color corresponding to the position. May be <code>null</code> to indicate that the position
	 *            has no associated color.
	 * @param ordinal
	 *            the ordinal number corresponding to the position's location in the original position list. May be <code>null</code> to indicate that the
	 *            position is not one of the originally specified
	 *            positions.
	 * @param GPSMarkerData
	 *            the current globe-specific GPSMarker data.
	 */
	protected void addTessellatedPosition(Position pos, Color color, Integer ordinal, GPSMarkerData GPSMarkerData) {
		// if (ordinal != null)
		// {
		// // NOTE: Assign these indices before adding the new position to the tessellatedPositions list.
		// int index = GPSMarkerData.tessellatedPositions.size() * 2;
		// GPSMarkerData.polePositions.put(index).put(index + 1);
		//
		// if (GPSMarkerData.hasExtrusionPoints)
		// GPSMarkerData.positionPoints.put(index);
		// else
		// GPSMarkerData.positionPoints.put(GPSMarkerData.tessellatedPositions.size());
		// }

		GPSMarkerData.tessellatedPositions.add(pos); // be sure to do the add after the pole position is set

		if (color != null) GPSMarkerData.tessellatedColors.add(color);
	}

	// /**
	// * Returns the GPSMarker position corresponding index. This returns null if the index does not correspond to an original
	// * position.
	// *
	// * @param positionIndex the position's index.
	// *
	// * @return the Position corresponding to the specified index.
	// */
	// protected Position getPosition(int positionIndex)
	// {
	// GPSMarkerData GPSMarkerData = this.getCurrentGPSMarkerData();
	// // Get an index into the tessellatedPositions list.
	// int index = GPSMarkerData.positionPoints.get(positionIndex);
	// // Return the originally specified position, which is stored in the tessellatedPositions list.
	// return (index >= 0 && index < GPSMarkerData.tessellatedPositions.size()) ?
	// GPSMarkerData.tessellatedPositions.get(index) : null;
	// }
	//
	// /**
	// * Returns the ordinal number corresponding to the position. This returns null if the position index does not
	// * correspond to an original position.
	// *
	// * @param positionIndex the position's index.
	// *
	// * @return the ordinal number corresponding to the specified position index.
	// */
	// protected Integer getOrdinal(int positionIndex)
	// {
	// return positionIndex;
	// }

	/**
	 * Returns an RGBA color corresponding to the specified position from the original position list and its
	 * corresponding ordinal number by delegating the call to this GPSMarker's positionColors. This returns <code>null</code> if this GPSMarker's positionColors property
	 * is <code>null</code>. This returns white if a color cannot be determined
	 * for the specified position and ordinal.
	 * 
	 * @param pos
	 *            the GPSMarker position the color corresponds to.
	 * @param ordinal
	 *            the ordinal number of the specified position.
	 * @return an RGBA color corresponding to the position and ordinal, or <code>null</code> if this GPSMarker's
	 *         positionColors property is <code>null</code>.
	 */
	protected Color getColor(Position pos, Integer ordinal) {
		if (this.positionColors == null) return null;

		Color color = this.positionColors.getColor(pos, ordinal);
		return color != null ? color : DEFAULT_POSITION_COLOR;
	}

	protected boolean isSmall(DrawContext dc, Vec4 ptA, Vec4 ptB, int numPixels) {
		return ptA.distanceTo3(ptB) <= numPixels * dc.getView().computePixelSizeAtDistance(dc.getView().getEyePoint().distanceTo3(ptA));
	}

	/**
	 * Determines whether the segment between two GPSMarker positions is visible.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param posA
	 *            the segment's first position.
	 * @param posB
	 *            the segment's second position.
	 * @param ptA
	 *            the model-coordinate point corresponding to the segment's first position.
	 * @param ptB
	 *            the model-coordinate point corresponding to the segment's second position.
	 * @return true if the segment is visible relative to the current view frustum, otherwise false.
	 */
	protected boolean isSegmentVisible(DrawContext dc, Position posA, Position posB, Vec4 ptA, Vec4 ptB) {
		Frustum f = dc.getView().getFrustumInModelCoordinates();

		if (f.contains(ptA)) return true;

		if (f.contains(ptB)) return true;

		if (ptA.equals(ptB)) return false;

		Position posC = Position.interpolateRhumb(0.5, posA, posB);

		Vec4 ptC = this.computePoint(dc.getVisibleTerrain(), posC);
		if (f.contains(ptC)) return true;

		double r = Line.distanceToSegment(ptA, ptB, ptC);
		Cylinder cyl = new Cylinder(ptA, ptB, r == 0 ? 1 : r);
		return cyl.intersects(dc.getView().getFrustumInModelCoordinates());
	}

	/**
	 * Creates the interior segment positions to adhere to the current GPSMarker type and terrain-following settings.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param posA
	 *            the segment's first position.
	 * @param posB
	 *            the segment's second position.
	 * @param ptA
	 *            the model-coordinate point corresponding to the segment's first position.
	 * @param ptB
	 *            the model-coordinate point corresponding to the segment's second position.
	 * @param colorA
	 *            the color corresponding to the segment's first position, or <code>null</code> if the first
	 *            position has no associated color.
	 * @param colorB
	 *            the color corresponding to the segment's second position, or <code>null</code> if the first
	 *            position has no associated color.
	 * @param ordinalA
	 *            the ordinal number corresponding to the segment's first position in the original position list.
	 * @param ordinalB
	 *            the ordinal number corresponding to the segment's second position in the original position list.
	 * @param GPSMarkerData
	 *            the current globe-specific GPSMarker data.
	 */
	protected void makeSegment(DrawContext dc, Position posA, Position posB, Vec4 ptA, Vec4 ptB, Color colorA, Color colorB, int ordinalA, int ordinalB, GPSMarkerData GPSMarkerData) {
		// This method does not add the first position of the segment to the position list. It adds only the
		// subsequent positions, including the segment's last position.

		double arcLength = this.getGPSMarkerType() == AVKey.LINEAR ? ptA.distanceTo3(ptB) : this.computeSegmentLength(dc, posA, posB);
		if (arcLength <= 0 || (this.getGPSMarkerType() == AVKey.LINEAR && !this.isFollowTerrain())) {
			if (!ptA.equals(ptB)) this.addTessellatedPosition(posB, colorB, ordinalB, GPSMarkerData);
			return;
		}

		// Variables for great GPSMarker and rhumb computation.
		Angle segmentAzimuth = null;
		Angle segmentDistance = null;
		Color interpolatedColor = (colorA != null && colorB != null) ? new Color() : null;

		for (double s = 0, p = 0; s < 1;) {
			if (this.isFollowTerrain()) p += this.terrainConformance * dc.getView().computePixelSizeAtDistance(ptA.distanceTo3(dc.getView().getEyePoint()));
			else p += arcLength / this.numSubsegments;

			Position pos;

			s = p / arcLength;
			if (s >= 1) {
				pos = posB;

				if (interpolatedColor != null) interpolatedColor.set(colorB);
			} else if (this.GPSMarkerType == AVKey.RHUMB_LINE || this.GPSMarkerType == AVKey.LINEAR) {
				if (segmentAzimuth == null) {
					segmentAzimuth = LatLon.rhumbAzimuth(posA, posB);
					segmentDistance = LatLon.rhumbDistance(posA, posB);
				}
				Angle distance = Angle.fromRadians(s * segmentDistance.radians);
				LatLon latLon = LatLon.rhumbEndPosition(posA, segmentAzimuth, distance);
				pos = new Position(latLon, (1 - s) * posA.elevation + s * posB.elevation);

				if (interpolatedColor != null) Color.interpolate(s, colorA, colorB, interpolatedColor);
			} else // GREAT_CIRCLE
			{
				if (segmentAzimuth == null) {
					segmentAzimuth = LatLon.greatCircleAzimuth(posA, posB);
					segmentDistance = LatLon.greatCircleDistance(posA, posB);
				}
				Angle distance = Angle.fromRadians(s * segmentDistance.radians);
				LatLon latLon = LatLon.greatCircleEndPosition(posA, segmentAzimuth, distance);
				pos = new Position(latLon, (1 - s) * posA.elevation + s * posB.elevation);

				if (interpolatedColor != null) Color.interpolate(s, colorA, colorB, interpolatedColor);
			}

			this.addTessellatedPosition(pos, interpolatedColor, s >= 1 ? ordinalB : null, GPSMarkerData);

			ptA = ptB;
		}
	}

	/**
	 * Computes the approximate model-coordinate, great-GPSMarker length between two positions.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @param posA
	 *            the first position.
	 * @param posB
	 *            the second position.
	 * @return the distance between the positions.
	 */
	protected double computeSegmentLength(DrawContext dc, Position posA, Position posB) {
		LatLon llA = new LatLon(posA.latitude, posA.longitude);
		LatLon llB = new LatLon(posB.latitude, posB.longitude);

		Angle ang = LatLon.greatCircleDistance(llA, llB);

		if (AVKey.CLAMP_TO_GROUND.equals(this.getAltitudeMode())) return ang.radians * (dc.getGlobe().getRadius());

		double height = 0.5 * (posA.elevation + posB.elevation);
		return ang.radians * (dc.getGlobe().getRadius() + height * dc.getVerticalExaggeration());
	}

	/**
	 * Computes this GPSMarker's reference center.
	 * 
	 * @param dc
	 *            the current draw context.
	 * @return the computed reference center, or null if it cannot be computed.
	 */
	protected void computeReferenceCenter(DrawContext dc) {
		if (this.positions == null) return;

		Position pos = this.getReferencePosition();
		if (pos == null) return;

		dc.getGlobe().computePointFromPosition(pos.latitude, pos.longitude, dc.getVerticalExaggeration() * pos.elevation, this.getCurrentGPSMarkerData().referencePoint);
	}

	/**
	 * Computes the minimum distance between this GPSMarker and the eye point.
	 * <p/>
	 * A {@link gov.nasa.worldwind.render.AbstractShape.AbstractShapeData} must be current when this method is called.
	 * 
	 * @param dc
	 *            the draw context.
	 * @param GPSMarkerData
	 *            the current shape data for this shape.
	 * @return the minimum distance from the shape to the eye point.
	 */
	protected double computeEyeDistance(DrawContext dc, GPSMarkerData GPSMarkerData) {
		double minDistanceSquared = Double.MAX_VALUE;
		Vec4 eyePoint = dc.getView().getEyePoint();
		Vec4 refPt = GPSMarkerData.getReferencePoint();

		GPSMarkerData.renderedGPSMarker.rewind();
		while (GPSMarkerData.renderedGPSMarker.hasRemaining()) {
			double x = eyePoint.x - (GPSMarkerData.renderedGPSMarker.get() + refPt.x);
			double y = eyePoint.y - (GPSMarkerData.renderedGPSMarker.get() + refPt.y);
			double z = eyePoint.z - (GPSMarkerData.renderedGPSMarker.get() + refPt.z);

			double d = x * x + y * y + z * z;
			if (d < minDistanceSquared) minDistanceSquared = d;

			// If the renderedGPSMarker contains RGBA color tuples in between each XYZ coordinate tuple, advance the
			// renderedGPSMarker's position to the next XYZ coordinate tuple.
			if (GPSMarkerData.vertexStride > 3) GPSMarkerData.renderedGPSMarker.position(GPSMarkerData.renderedGPSMarker.position() + GPSMarkerData.vertexStride - 3);
		}

		return Math.sqrt(minDistanceSquared);
	}

	/**
	 * Computes the GPSMarker's bounding box from the current rendering GPSMarker. Assumes the rendering GPSMarker is up-to-date.
	 * 
	 * @param current
	 *            the current data for this shape.
	 * @return the computed extent.
	 */
	protected Extent computeExtent(GPSMarkerData current) {
		if (current.renderedGPSMarker == null) return null;

		current.renderedGPSMarker.rewind();
		Box box = Box.computeBoundingBox(current.renderedGPSMarker, current.vertexStride);

		// The GPSMarker points are relative to the reference center, so translate the extent to the reference center.
		box = box.translate(current.getReferencePoint());

		return box;
	}


	/**
	 * Computes the GPSMarker's reference position. The position returned is the center-most ordinal position in the GPSMarker's
	 * specified positions.
	 * 
	 * @return the computed reference position.
	 */
	public Position getReferencePosition() {
		return this.referencePosition;
	}

	protected void fillVBO(DrawContext dc) {
		GPSMarkerData GPSMarkerData = this.getCurrentGPSMarkerData();
		int numIds = this.isShowPositions() ? 3 : GPSMarkerData.hasExtrusionPoints && this.isDrawVerticals() ? 2 : 1;

		int[] vboIds = (int[]) dc.getGpuResourceCache().get(GPSMarkerData.getVboCacheKey());
		if (vboIds != null && vboIds.length != numIds) {
			this.clearCachedVbos(dc);
			vboIds = null;
		}

		int vSize = GPSMarkerData.renderedGPSMarker.limit() * 4;
		int iSize = GPSMarkerData.hasExtrusionPoints && this.isDrawVerticals() ? GPSMarkerData.tessellatedPositions.size() * 2 * 4 : 0;
		if (this.isShowPositions()) iSize += GPSMarkerData.tessellatedPositions.size();

		if (vboIds == null) {
			vboIds = new int[numIds];
			GLES20.glGenBuffers(vboIds.length, vboIds, 0);
			dc.getGpuResourceCache().put(GPSMarkerData.getVboCacheKey(), vboIds, GpuResourceCache.VBO_BUFFERS, vSize + iSize);
		}

		try {
			FloatBuffer vb = GPSMarkerData.renderedGPSMarker;
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboIds[0]);
			GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, vb.limit() * 4, vb.rewind(), GLES20.GL_STATIC_DRAW);

		} finally {
			GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);
			GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0);
		}
	}

	@Override
	public List<Intersection> intersect(Line line, Terrain terrain) throws InterruptedException // TODO
	{
		return null;
	}

	public void move(Position delta) {
		if (delta == null) {
			String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Position refPos = this.getReferencePosition();

		// The reference position is null if this GPSMarker has no positions. In this case moving the GPSMarker by a
		// relative delta is meaningless because the GPSMarker has no geographic location. Therefore we fail softly by
		// exiting and doing nothing.
		if (refPos == null) return;

		this.moveTo(refPos.add(delta));
	}
	
	public void moveTo(Position position) {
		if (position == null) {
			String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (this.numPositions == 0) return;

		Position oldPosition = this.getReferencePosition();

		// The reference position is null if this GPSMarker has no positions. In this case moving the GPSMarker to a new
		// reference position is meaningless because the GPSMarker has no geographic location. Therefore we fail softly
		// by exiting and doing nothing.
		if (oldPosition == null) return;

		this.setPositions(position);
	}
}