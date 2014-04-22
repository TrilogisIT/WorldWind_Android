/* Copyright (C) 2001, 2012 United States Government as represented by
the Administrator of the National Aeronautics and Space Administration.
All Rights Reserved.
 */
package gov.nasa.worldwind;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.util.Property;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Interpolator;
import gov.nasa.worldwind.animation.AngleEvaluator;
import gov.nasa.worldwind.animation.DoubleEvaluator;
import gov.nasa.worldwind.animation.PositionEvaluator;
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
import gov.nasa.worldwind.util.PerformanceStatistic;
import gov.nasa.worldwind.util.WWMath;
import android.graphics.Point;
import gov.nasa.worldwind.view.BasicOrbitViewLimits;
import gov.nasa.worldwind.view.OrbitViewCollisionSupport;
import gov.nasa.worldwind.view.OrbitViewLimits;
import gov.nasa.worldwind.view.ViewPropertyLimits;

import java.util.HashMap;
import java.util.Map;

/**
 * Edited By: Nicola Dorigatti, Trilogis
 * 
 * @author ccrick
 * @version $Id: BasicView.java 851 2012-10-12 19:04:20Z dcollins $
 */
public class BasicView extends WWObjectImpl implements View {
	// TODO: add documentation to all public methods

	// TODO: make configurable
	protected static final double MINIMUM_NEAR_DISTANCE = 1;
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

	protected DrawContext dc;
	protected Globe globe;
	protected OrbitViewCollisionSupport collisionSupport = new OrbitViewCollisionSupport();
	protected boolean detectCollisions = true;
	protected boolean hadCollisions;
	protected float farDistanceMultiplier = 1f;

	// Temporary property used to avoid constant allocation of Line objects during repeated calls to
	// computePositionFromScreenPoint.
	protected Line line = new Line();
	private ViewPropertyLimits viewLimits = new BasicOrbitViewLimits();

	public BasicView() {
		this.lookAtPosition.setDegrees(Configuration.getDoubleValue(AVKey.INITIAL_LATITUDE, 0.0), Configuration.getDoubleValue(AVKey.INITIAL_LONGITUDE, 0.0), 0.0);
		this.range = Configuration.getDoubleValue(AVKey.INITIAL_ALTITUDE, 0.0);
	}

	public float getFarDistanceMultiplier() {
		return this.farDistanceMultiplier;
	}

	public void setFarDistanceMultiplier(float multiplier) {
		this.farDistanceMultiplier = multiplier;
	}

	public boolean isDetectCollisions()
	{
		return this.detectCollisions;
	}

	public void setDetectCollisions(boolean detectCollisions)
	{
		this.detectCollisions = detectCollisions;
	}

	public boolean hadCollisions()
	{
		boolean result = this.hadCollisions;
		this.hadCollisions = false;
		return result;
	}

	protected void flagHadCollisions()
	{
		this.hadCollisions = true;
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

		this.dc = dc;
		this.globe = this.dc.getGlobe();

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
		calculateOrbitModelview(dc, this.lookAtPosition, this.heading, this.tilt, this.roll, this.range, this.modelview);
	}

	protected void applyProjectionMatrix(DrawContext dc) {
		this.projection.setPerspective(this.fieldOfView, dc.getViewportWidth(), dc.getViewportHeight(), this.nearClipDistance, this.farClipDistance);
	}

	protected void applyViewport(DrawContext dc) {
		this.viewport.set(0, 0, dc.getViewportWidth(), dc.getViewportHeight());
	}

	protected void applyFrustum(DrawContext dc) {
		nearClipDistance = computeNearDistance(eyePoint);
		farClipDistance = computeFarDistance(eyePosition);

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
		this.lookAtPosition.set(BasicOrbitViewLimits.limitLookAtPosition(this.lookAtPosition, this.getOrbitViewLimits()));
		resolveCollisionsWithCenterPosition();
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
		boolean isZoomingIn = distance<this.range;
		this.range = distance;
		this.range = BasicOrbitViewLimits.limitZoom(this.range, this.getOrbitViewLimits());
		if(isZoomingIn) {
			resolveCollisionsWithCenterPosition();
		}
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
		this.heading.set(BasicOrbitViewLimits.limitHeading(this.heading, this.getOrbitViewLimits()));
		resolveCollisionsWithPitch();
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
		this.tilt.set(BasicOrbitViewLimits.limitPitch(this.tilt, this.getOrbitViewLimits()));
		resolveCollisionsWithPitch();
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

	/**
	 * Returns the <code>OrbitViewLimits</code> that apply to this <code>OrbitView</code>. Incoming parameters to the
	 * methods setCenterPosition, setHeading, setPitch, or setZoom are be limited by the parameters defined in this
	 * <code>OrbitViewLimits</code>.
	 *
	 * @return the <code>OrbitViewLimits</code> that apply to this <code>OrbitView</code>
	 */
	public OrbitViewLimits getOrbitViewLimits()
	{
		return (OrbitViewLimits) viewLimits;
	}

	/**
	 * Sets the <code>OrbitViewLimits</code> that will apply to this <code>OrbitView</code>. Incoming parameters to the
	 * methods setCenterPosition, setHeading, setPitch, or setZoom will be limited by the parameters defined in
	 * <code>viewLimits</code>.
	 *
	 * @param viewLimits the <code>OrbitViewLimits</code> that will apply to this <code>OrbitView</code>.
	 *
	 * @throws IllegalArgumentException if <code>viewLimits</code> is null.
	 */
	public void setOrbitViewLimits(OrbitViewLimits viewLimits)
	{
		if (viewLimits == null)
		{
			String message = Logging.getMessage("nullValue.ViewLimitsIsNull");
			Logging.error(message);
			throw new IllegalArgumentException(message);
		}

		this.viewLimits = viewLimits;
	}

	public Vec4 getCurrentEyePoint()
	{
		if (this.globe != null)
		{
			Matrix modelview = Matrix.fromIdentity();
			calculateOrbitModelview(dc, this.lookAtPosition, this.heading,
					this.tilt, this.roll, this.range, modelview);
			if (modelview != null) {
				Matrix modelviewInv = modelview.invert();
				if (modelviewInv != null) {
					return Vec4.UNIT_W.transformBy4(modelviewInv);
				}
			}
		}

		return Vec4.ZERO;
	}

	public Position getCurrentEyePosition()
	{
		if (this.globe != null)
		{
			return this.globe.computePositionFromPoint(getCurrentEyePoint());
		}

		return Position.ZERO;
	}

	public static void calculateOrbitModelview(DrawContext dc, Position lookAtPosition, Angle heading, Angle tilt, Angle roll, double range, Matrix matrix) {
		matrix.setLookAt(dc.getVisibleTerrain(),
				lookAtPosition.latitude, lookAtPosition.longitude, lookAtPosition.elevation,
				AVKey.CLAMP_TO_GROUND, range, heading, tilt, roll);
	}

	protected double computeNearDistance(Vec4 eyePoint)
	{
		double near = 0;
		if (eyePoint != null && this.dc != null)
		{
			double tanHalfFov = this.fieldOfView.tanHalfAngle();
			Vec4 lookAtPoint = dc.getVisibleTerrain().getSurfacePoint(lookAtPosition.latitude, lookAtPosition.longitude, 0);
			double eyeDistance = new Vec4().subtract3AndSet(eyePoint, lookAtPoint).getLength3();
			near = eyeDistance / (2 * Math.sqrt(2 * tanHalfFov * tanHalfFov + 1));
		}
		return near < MINIMUM_NEAR_DISTANCE ? MINIMUM_NEAR_DISTANCE : near;
	}

	protected double computeFarDistance(Position eyePosition)
	{
		double far = 0;
		if (eyePosition != null)
		{
			double height = OrbitViewCollisionSupport.computeViewHeightAboveSurface(dc, modelviewInv, fieldOfView, viewport, nearClipDistance);
			far = WWMath.computeHorizonDistance(dc.getGlobe(), height);
			if(WorldWindow.DEBUG) {
				Logging.verbose(String.format("Sea level horizon: %.2f",
						WWMath.computeHorizonDistance(dc.getGlobe(), eyePosition.elevation)));
				Logging.verbose(String.format("Surface horizon: %.2f",
						far));
			}
			far *= farDistanceMultiplier;
		}

		return far < MINIMUM_FAR_DISTANCE ? MINIMUM_FAR_DISTANCE : far;
	}

	protected void resolveCollisionsWithCenterPosition()
	{
		if (this.dc == null)
			return;

		if (!isDetectCollisions())
			return;

		if(dc.getVisibleTerrain().getGlobe()==null) {
			//TODO figure out why this is null
			Logging.warning("getVisibleTerrain().getGlobe()==null.\tBasicView.dc.getGlobe(): " + dc.getGlobe());
			return;
		}
		// If there is no collision, 'newCenterPosition' will be null. Otherwise it will contain a value
		// that will resolve the collision.
		double nearDistance = this.computeNearDistance(this.getCurrentEyePoint());
		Position newCenter = this.collisionSupport.computeCenterPositionToResolveCollision(this, nearDistance, this.dc);
		if (newCenter != null && newCenter.getLatitude().degrees >= -90 && newCenter.getLongitude().degrees <= 90)
		{
			this.lookAtPosition = newCenter;
			flagHadCollisions();
		}
	}

	protected void resolveCollisionsWithPitch()
	{
		if (this.dc == null)
			return;

		if (!isDetectCollisions())
			return;

		if(dc.getVisibleTerrain().getGlobe()==null) {
			//TODO figure out why this is null
			Logging.warning("getVisibleTerrain().getGlobe()==null.\tBasicView.dc.getGlobe(): " + dc.getGlobe());
			return;
		}

		// Compute the near distance corresponding to the current set of values.
		// If there is no collision, 'newPitch' will be null. Otherwise it will contain a value
		// that will resolve the collision.
		double nearDistance = this.computeNearDistance(this.getCurrentEyePoint());
		Angle newPitch = this.collisionSupport.computePitchToResolveCollision(this, nearDistance, this.dc);
		if (newPitch != null && newPitch.degrees <= 90 && newPitch.degrees >= 0)
		{
			this.tilt = newPitch;
			flagHadCollisions();
		}
	}

	protected Map<String, Animator> goToAnimations = new HashMap<String, Animator>();

	public void stopAnimations() {
		for(Animator a : goToAnimations.values()) {
			a.cancel();
		}
		goToAnimations.clear();
	}

	public ValueAnimator createTiltAnimator(final WorldWindowGLTextureView wwd, Angle tilt) {
		final ValueAnimator tiltAnimator = ValueAnimator
				.ofObject(new AngleEvaluator(), this.tilt, tilt);
		tiltAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(final ValueAnimator animation) {
				wwd.invokeInRenderingThread(new Runnable() {
					@Override
					public void run() {
						setTilt((Angle)animation.getAnimatedValue());
					}
				});
				BasicView.this.firePropertyChange(AVKey.VIEW, null, BasicView.this);
			}
		});
		return tiltAnimator;
	}

	public ValueAnimator createHeadingAnimator(final WorldWindowGLTextureView wwd, Angle heading) {
		final ValueAnimator headingAnimator = ValueAnimator
				.ofObject(new AngleEvaluator(), this.heading, heading);
		headingAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(final ValueAnimator animation) {
				wwd.invokeInRenderingThread(new Runnable() {
					@Override
					public void run() {
						setHeading((Angle)animation.getAnimatedValue());
					}
				});
				firePropertyChange(AVKey.VIEW, null, BasicView.this);
			}
		});
		return headingAnimator;
	}

	public ValueAnimator createRangeAnimator(final WorldWindowGLTextureView wwd, double range) {
		final ValueAnimator rangeAnimator = ValueAnimator
				.ofObject(new DoubleEvaluator(), this.range, range);
		rangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(final ValueAnimator animation) {
				wwd.invokeInRenderingThread(new Runnable() {
					@Override
					public void run() {
						setRange((Double)animation.getAnimatedValue());
					}
				});
				firePropertyChange(AVKey.VIEW, null, BasicView.this);
			}
		});
		return rangeAnimator;
	}

	private ValueAnimator createLookAtAnimator(final WorldWindowGLTextureView wwd, Position position) {
		final ValueAnimator lookAtAnimator = ValueAnimator.ofObject(
						new PositionEvaluator(), this.lookAtPosition, position);
		lookAtAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
			@Override
			public void onAnimationUpdate(final ValueAnimator animation) {
				wwd.invokeInRenderingThread(new Runnable() {
					@Override
					public void run() {
						setLookAtPosition((Position)animation.getAnimatedValue());
					}
				});
				firePropertyChange(AVKey.VIEW, null, BasicView.this);
			}
		});
		return lookAtAnimator;
	}

	public void animate(Animator animator) {
		stopAnimations();
		goToAnimations.put("Custom", animator);
		animator.start();
	}
}
