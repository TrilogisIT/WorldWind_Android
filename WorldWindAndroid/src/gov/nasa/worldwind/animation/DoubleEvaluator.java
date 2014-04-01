/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.animation;

import android.animation.TypeEvaluator;
import gov.nasa.worldwind.geom.Position;

/**
 * {@link android.animation.TypeEvaluator} for {@link java.lang.Double}
 *
 * Created by kedzie on 3/28/14.
 * @author Mark Kedzierski
 */
public class DoubleEvaluator implements TypeEvaluator<Double> {

	@Override
	public Double evaluate(float fraction, Double startValue, Double endValue) {
		return startValue + (endValue-startValue)*fraction;
	}
}
