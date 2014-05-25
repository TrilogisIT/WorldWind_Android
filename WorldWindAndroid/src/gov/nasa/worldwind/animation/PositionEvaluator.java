/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.animation;

import android.animation.TypeEvaluator;
import gov.nasa.worldwind.geom.Position;

/**
 * {@link android.animation.TypeEvaluator} for {@link gov.nasa.worldwind.geom.Position}
 *
 * Created by kedzie on 3/28/14.
 * @author Mark Kedzierski
 */
public class PositionEvaluator implements TypeEvaluator<Position> {

	@Override
	public Position evaluate(float fraction, Position startValue, Position endValue) {
		return Position.interpolateGreatCircle(fraction, startValue, endValue);
	}
}
