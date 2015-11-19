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

public class AntiSurferWeightingScheme extends ClassificationWeightingScheme {
	public AntiSurferWeightingScheme() {
//		 _weights = new double[] {
//		 4.0, //BulletTravelTime
//		 4.0, //AheadWallDist
//		 2.0, //ReverseWallDist
//		 3.0, //latRelHeading
//		 4.0, //approachingRelHeading
//		 2.0, //velocity
//		 4.0, //VChange
//		 4.0, //ticksSinceDirChange
//		 1.0, //virtuality
//		 500.0 //time
//		 };
		
		
		_weights = new double[] { 1, 1, 1, 1, 1 };

	}

	@Override
	public double[] getPointCoordinates(DataWave wave) {
		GunDataWave gunWave = (GunDataWave) wave;

//		 double[] dataPointCoordinates = new double[_weights.length];
//		 dataPointCoordinates[0] = _weights[0] *
//		 gunWave.getBulletTravelTime();
//		 dataPointCoordinates[1] = _weights[1] * wave.getAheadWallDist();
//		 dataPointCoordinates[2] = _weights[2] * wave.getReverseWallDist();
//		
//		 dataPointCoordinates[3] = _weights[3]
//		 * Math.sin(gunWave.getRelHeading());
//		 dataPointCoordinates[4] = _weights[4]
//		 * (Math.cos(gunWave.getRelHeading()) + 1) / 2;
//		 dataPointCoordinates[5] = _weights[5]
//		 * Math.abs(gunWave.getTargetVelocity()) / 8.0;
//		 dataPointCoordinates[6] = _weights[6]
//		 * (gunWave.getVChange() + Rules.DECELERATION)
//		 / (Rules.ACCELERATION + Rules.DECELERATION);
//		 dataPointCoordinates[7] = _weights[7]
//		 * Math.min(1.0, (gunWave.getTicksSinceDirChange() / gunWave
//		 .getBulletTravelTime()));
//		 dataPointCoordinates[8] = _weights[8] * gunWave.getVirtuality();
//		 dataPointCoordinates[9] = _weights[9] * wave.getAbsFireTime() * 4E-5;
		
		
		double[] dataPointCoordinates = new double[_weights.length];
		dataPointCoordinates[0] = _weights[0] * gunWave.getAheadWallDist();
		dataPointCoordinates[1] = _weights[1] * wave.getReverseWallDist();
		dataPointCoordinates[2] = _weights[2] * wave.getAheadWallSeverity();
		dataPointCoordinates[3] = _weights[3]
				* gunWave.getReverseWallSeverity();
		dataPointCoordinates[4] = _weights[4] * gunWave.getVirtuality();

		return dataPointCoordinates;

	}
}
