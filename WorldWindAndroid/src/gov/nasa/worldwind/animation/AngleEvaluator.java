/*
 * Copyright (C) 2014 United States Government as represented by the Administrator of the
 * National Aeronautics and Space Administration.
 * All Rights Reserved.
 */

package gov.nasa.worldwind.animation;

import android.animation.TypeEvaluator;
import gov.nasa.worldwind.geom.Angle;

/**
 * {@link android.animation.TypeEvaluator} for {@link gov.nasa.worldwind.geom.Angle}
 *
 * Created by kedzie on 3/28/14.
 * @author Mark Kedzierski
 */
public class AngleEvaluator implements TypeEvaluator<Angle>{

	@Override
	public Angle evaluate(float fraction, Angle startValue, Angle endValue) {
		return Angle.mix(fraction, startValue, endValue);
	}
}
