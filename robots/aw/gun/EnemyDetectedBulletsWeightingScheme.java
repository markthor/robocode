/*
 * Copyright (c) 2011 Andrew Wells. All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or without modification, are
 *	permitted provided that the following conditions are met:
 *
 *		1. Redistributions of source code must retain the above copyright notice, this list of
 *     conditions and the following disclaimer.
 *
 *		2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 *	THIS SOFTWARE IS PROVIDED BY Andrew Wells ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *	WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *	FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Andrew Wells OR
 *	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *	SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *	ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *	ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * 	The views and conclusions contained in the software and documentation are those of the
 *	authors and should not be interpreted as representing official policies, either expressed
 *	or implied, of Andrew Wells.
 */

package aw.gun;

import robocode.Rules;
import aw.utils.ClassificationWeightingScheme;
import aw.waves.DataWave;
import aw.waves.GunDataWave;

public class EnemyDetectedBulletsWeightingScheme extends
		ClassificationWeightingScheme {

	public EnemyDetectedBulletsWeightingScheme() {

		_weights = new double[] { 1, 1, 1, 1};

	}

	@Override
	public double[] getPointCoordinates(DataWave wave) {
		GunDataWave gunWave = (GunDataWave) wave;

		double[] dataPointCoordinates = new double[_weights.length];

		dataPointCoordinates[0] = _weights[0] * gunWave.getBulletTravelTime()
				* 0.009166667;
		dataPointCoordinates[1] = _weights[1]
				* (Math.cos(gunWave.getRelHeading()) + 1) / 2;
		dataPointCoordinates[2] = _weights[2]
				* Math.abs(gunWave.getTargetVelocity()) / 8.0;
		dataPointCoordinates[3] = _weights[3]
				* (gunWave.getVChange() + Rules.DECELERATION)
				/ (Rules.ACCELERATION + Rules.DECELERATION);


		return dataPointCoordinates;
	}
}
