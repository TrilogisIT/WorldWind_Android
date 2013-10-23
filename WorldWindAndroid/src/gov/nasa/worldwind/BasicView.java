/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Frustum;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Matrix;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Rect;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.Globe;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWMath;
import android.graphics.Point;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author ccrick
 * @version $Id: BasicView.java 851 2012-10-12 19:04:20Z dcollins $
 */
public class BasicView extends WWObjectImpl implements View {
	// TODO: add documentation to all public methods

	// TODO: make configurable
	protected static final double MINIMUM_NEAR_DISTANCE = 2;
	protected static final double MINIMUM_FAR_DISTANCE = 100;

	// View representation
	protected Matrix modelview = Matrix.fromIdentity();
	protected Matrix modelviewInv = Matrix.fromIdentity();
	protected Matrix modelviewTranspose = Matrix.fromIdentity();
	protected Matrix projection = Matrix.fromIdentity();
	protected Matrix modelviewProjection = Matrix.fromIdentity();
	protected Rect viewport = new Rect();
	protected Frustum frustum = new Frustum();
	protected Frustum frustumInModelCoords = new Frustum();
	/** The field of view in degrees. */
	protected Angle fieldOfView = Angle.fromDegrees(45);
	protected double nearClipDistance = MINIMUM_NEAR_DISTANCE;
	protected double farClipDistance = MINIMUM_FAR_DISTANCE;

	protected Vec4 eyePoint = new Vec4();
	protected Position eyePosition = new Position();
	protected Position lookAtPosition = new Position();
	protected double range;
	protected Angle heading = new Angle();
	protected Angle tilt = new Angle();
	protected Angle roll = new Angle();

	// Temporary property used to avoid constant allocation of Line objects during repeated calls to
	// computePositionFromScreenPoint.
	protected Line line = new Line();

	public BasicView() {
		this.lookAtPosition.setDegrees(Configuration.getDoubleValue(AVKey.INITIAL_LATITUDE, 0.0), Configuration.getDoubleValue(AVKey.INITIAL_LONGITUDE, 0.0), 0.0);
		this.range = Configuration.getDoubleValue(AVKey.INITIAL_ALTITUDE, 0.0);
	}

	/** {@inheritDoc} */
	public Matrix getModelviewMatrix() {
		return this.modelview;
	}

	/** {@inheritDoc} */
	public Matrix getProjectionMatrix() {
		return this.projection;
	}

	/** {@inheritDoc} */
	public Matrix getModelviewProjectionMatrix() {
		return this.modelviewProjection;
	}

	/** {@inheritDoc} */
	public Rect getViewport() {
		return this.viewport;
	}

	/** {@inheritDoc} */
	public Frustum getFrustum() {
		return this.frustum;
	}

	/** {@inheritDoc} */
	public Frustum getFrustumInModelCoordinates() {
		return this.frustumInModelCoords;
	}

	/** {@inheritDoc} */
	public Angle getFieldOfView() {
		return this.fieldOfView;
	}

	/** {@inheritDoc} */
	public void setFieldOfView(Angle fieldOfView) {
		if (fieldOfView == null) {
			String msg = Logging.getMessage("nullValue.FieldOfViewIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.fieldOfView.set(fieldOfView);
	}

	/** {@inheritDoc} */
	public double getNearClipDistance() {
		return this.nearClipDistance;
	}

	/** {@inheritDoc} */
	public double getFarClipDistance() {
		return this.farClipDistance;
	}

	/** {@inheritDoc} */
	public boolean project(Vec4 modelPoint, Vec4 result) {
		if (modelPoint == null) {
			String msg = Logging.getMessage("nullValue.ModelPointIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (result == null) {
			String msg = Logging.getMessage("nullValue.ResultIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return WWMath.project(modelPoint.x, modelPoint.y, modelPoint.z, this.modelviewProjection, this.viewport, result);
	}

	/** {@inheritDoc} */
	public boolean unProject(Vec4 screenPoint, Vec4 result) {
		if (screenPoint == null) {
			String msg = Logging.getMessage("nullValue.ScreenPointIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (result == null) {
			String msg = Logging.getMessage("nullValue.ResultIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		return WWMath.unProject(screenPoint.z, screenPoint.y, screenPoint.z, this.modelviewProjection, this.viewport, result);
	}

	/** {@inheritDoc} */
	public boolean computeRayFromScreenPoint(Point point, Line result) {
		if (point == null) {
			String msg = Logging.getMessage("nullValue.PointIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (result == null) {
			String msg = Logging.getMessage("nullValue.ResultIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		double yInGLCoords = this.viewport.y + (this.viewport.height - point.y);
		return WWMath.computeRayFromScreenPoint(point.x, yInGLCoords, this.modelview, this.projection, this.viewport, result);
	}

	/** {@inheritDoc} */
	public boolean computePositionFromScreenPoint(Globe globe, Point point, Position result) {
		if (globe == null) {
			String msg = Logging.getMessage("nullValue.GlobeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (point == null) {
			String msg = Logging.getMessage("nullValue.PointIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (result == null) {
			String msg = Logging.getMessage("nullValue.ResultIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// noinspection SimplifiableIfStatement
		if (!this.computeRayFromScreenPoint(point, this.line)) return false;

		return globe.getIntersectionPosition(this.line, result);
	}

	/** {@inheritDoc} */
	public double computePixelSizeAtDistance(double distance) {
		if (distance < 0) {
			String msg = Logging.getMessage("generic.DistanceIsInvalid", distance);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Replace a zero viewport width with 1. This effectively ignores the viewport width.
		double viewportWidth = this.viewport.width > 0 ? this.viewport.width : 1;
		double pixelSizeScale = 2 * this.fieldOfView.tanHalfAngle() / viewportWidth;

		return distance * pixelSizeScale;
	}

	/** {@inheritDoc} */
	public void apply(DrawContext dc) {
		if (dc == null) {
			String msg = Logging.getMessage("nullValue.DrawContextIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		if (dc.getGlobe() == null) {
			String msg = Logging.getMessage("nullValue.DrawingContextGlobeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// Compute and apply the current modelview matrix, its inverse, and its transpose.
		this.applyModelviewMatrix(dc);
		this.modelviewInv.invertTransformMatrix(this.modelview);
		this.modelviewTranspose.transpose(this.modelview);

		// Compute current eye position in both cartesian coordinates and geographic coordinates. This must be done
		// before computing the clip distances, since they depend on eye position. The eye point is computed by
		// transforming the origin (0.0, 0.0, 0.0, 1.0) by the inverse of the modelview matrix. We have pre-computed the
		// result and stored it inline here to avoid an unnecessary matrix multiplication. This is equivalent to the
		// following: this.eyePoint.set(0, 0, 0).transformBy4AndSet(this.modelviewInv)
		this.eyePoint.set(this.modelviewInv.m[3], this.modelviewInv.m[7], this.modelviewInv.m[11]);
		dc.getGlobe().computePositionFromPoint(this.eyePoint, this.eyePosition);

		// Compute and apply the current modelview and projection matrices, and the combined modelview-projection
		// matrix.
		this.applyProjectionMatrix(dc);
		this.modelviewProjection.multiplyAndSet(this.projection, this.modelview);

		// Compute and apply the current viewport rectangle.
		this.applyViewport(dc);

		// Compute and apply the current frustum.
		this.applyFrustum(dc);
		this.frustumInModelCoords.transformBy(this.frustum, this.modelviewTranspose);
	}

	protected void applyModelviewMatrix(DrawContext dc) {
		this.modelview.setLookAt(dc.getVisibleTerrain(), this.lookAtPosition.latitude, this.lookAtPosition.longitude, this.lookAtPosition.elevation, AVKey.ABSOLUTE, this.range,
				this.heading, this.tilt, this.roll);
	}

	protected void applyProjectionMatrix(DrawContext dc) {
		this.projection.setPerspective(this.fieldOfView, dc.getViewportWidth(), dc.getViewportHeight(), this.nearClipDistance, this.farClipDistance);
	}

	protected void applyViewport(DrawContext dc) {
		this.viewport.set(0, 0, dc.getViewportWidth(), dc.getViewportHeight());
	}

	protected void applyFrustum(DrawContext dc) {
		double tanHalfFov = this.fieldOfView.tanHalfAngle();
		this.nearClipDistance = this.eyePosition.elevation / (2 * Math.sqrt(2 * tanHalfFov * tanHalfFov + 1));
		this.farClipDistance = WWMath.computeHorizonDistance(dc.getGlobe(), this.eyePosition.elevation);

		if (this.nearClipDistance < MINIMUM_NEAR_DISTANCE) this.nearClipDistance = MINIMUM_NEAR_DISTANCE;

		if (this.farClipDistance < MINIMUM_FAR_DISTANCE) this.farClipDistance = MINIMUM_FAR_DISTANCE;

		this.frustum.setPerspective(this.fieldOfView, this.viewport.width, this.viewport.height, this.nearClipDistance, this.farClipDistance);
	}

	/** {@inheritDoc} */
	public Vec4 getEyePoint() {
		return this.eyePoint;
	}

	/** {@inheritDoc} */
	public Position getEyePosition(Globe globe) {
		// TODO: Remove the globe parameter from this method.
		return this.eyePosition;
	}

	public Position getLookAtPosition() {
		return this.lookAtPosition;
	}

	public void setLookAtPosition(Position position) {
		if (position == null) {
			String msg = Logging.getMessage("nullValue.PositionIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.lookAtPosition.set(position);
	}

	/**
	 * Gets the geographic position of the current lookAt point on the globe.
	 * 
	 * @param globe
	 *            the current globe
	 * @return the position of the LookAt
	 * @throws IllegalArgumentException
	 *             if <code>globe</code> is null.
	 */
	public Position getLookAtPosition(Globe globe) {
		if (globe == null) {
			String msg = Logging.getMessage("nullValue.GlobeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Vec4 front = new Vec4(-this.modelview.m[8], -this.modelview.m[9], -this.modelview.m[10]);
		Vec4 eyePoint = getEyePoint();
		Line viewRay = new Line(eyePoint, front);

		// Compute the forward vector's intersection with the specified globe, and return the intersection position.
		// Return null if the forward vector does not intersect the globe.
		Position result = new Position();
		return globe.getIntersectionPosition(viewRay, result) ? result : null;
	}

	/**
	 * Gets the heading rotation around the current lookAt point on the globe, as measured in clockwise rotation from
	 * North.
	 * 
	 * @param globe
	 *            the current globe
	 * @return the heading rotation around the LookAt
	 * @throws IllegalArgumentException
	 *             if <code>globe</code> is null.
	 */
	public Angle getLookAtHeading(Globe globe) {
		if (globe == null) {
			String msg = Logging.getMessage("nullValue.GlobeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		// TODO: what to do when look at at the poles?
		Vec4 right = new Vec4(this.modelview.m[0], this.modelview.m[1], this.modelview.m[2]);

		Position lookAtPosition = getLookAtPosition(globe);
		if (lookAtPosition == null) return null;

		// get north pointing vector at lookAt position (not eye position)
		Vec4 northTangent = globe.computeNorthPointingTangentAtLocation(lookAtPosition);
		Vec4 normal = globe.computeSurfaceNormalAtLocation(lookAtPosition.latitude, lookAtPosition.longitude);
		Vec4 forward = normal.cross3(right).normalize3();

		Angle delta = northTangent.angleBetween3(forward);
		// determine if newDelta is positive or negative
		if (forward.cross3(northTangent).dot3(normal) <= 0) delta.multiplyAndSet(-1);

		return delta.multiplyAndSet(-1); // negate for positive CW heading rotations
	}

	/**
	 * Gets the current tilt around the lookAt point. The tilt value may be between zero and 90 degrees, with zero
	 * indicating that the eye looks directly down at the lookAt, and 90 meaning that the eye looks toward the horizon
	 * at the lookAt point.
	 * 
	 * @param globe
	 *            the current globe
	 * @return tilt around the lookAt point
	 * @throws IllegalArgumentException
	 *             if <code>globe</code> is null.
	 */
	public Angle getLookAtTilt(Globe globe) {
		if (globe == null) {
			String msg = Logging.getMessage("nullValue.GlobeIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		Position lookAt = getLookAtPosition(globe);
		if (lookAt == null) return null; // ray did not intersect the globe, so cannot pivot around LookAt

		Vec4 back = new Vec4(this.modelview.m[8], this.modelview.m[9], this.modelview.m[10]);

		Vec4 normal = new Vec4();
		globe.computeSurfaceNormalAtLocation(lookAt.latitude, lookAt.longitude, normal);

		Angle delta = normal.angleBetween3(back);

		// determine if delta is positive or negative
		Vec4 right = new Vec4(this.modelview.m[0], this.modelview.m[1], this.modelview.m[2]);
		if (normal.cross3(back).dot3(right) < 0) delta.multiplyAndSet(-1);

		return delta;
	}

	public double getRange() {
		return this.range;
	}

	public void setRange(double distance) {
		if (distance < 0) {
			String msg = Logging.getMessage("generic.DistanceIsInvalid", distance);
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.range = distance;
	}

	public Angle getHeading() {
		return this.heading;
	}

	public void setHeading(Angle angle) {
		if (angle == null) {
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.heading.set(angle);
	}

	public Angle getTilt() {
		return this.tilt;
	}

	public void setTilt(Angle angle) {
		if (angle == null) {
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.tilt.set(angle);
	}

	public Angle getRoll() {
		return this.roll;
	}

	public void setRoll(Angle angle) {
		if (angle == null) {
			String msg = Logging.getMessage("nullValue.AngleIsNull");
			Logging.error(msg);
			throw new IllegalArgumentException(msg);
		}

		this.roll.set(angle);
	}
}
