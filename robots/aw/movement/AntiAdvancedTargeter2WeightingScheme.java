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

package aw.movement;

import robocode.Rules;
import aw.utils.ClassificationWeightingScheme;
import aw.waves.DataWave;
import aw.waves.MovementDataWave;

public class AntiAdvancedTargeter2WeightingScheme extends
		ClassificationWeightingScheme {
	
	public AntiAdvancedTargeter2WeightingScheme() {
//		_weights = new double[] { 2.0, 8.0, 5.0, 7.0, 3.0, 6.0, 4.0, 5.0, 3.0, 3.0, 3.0, 10.0, 5000.0};
		_weights = new double[] { 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0, 1.0};
	}

	@Override
	public double[] getPointCoordinates(DataWave wave) {
		MovementDataWave movementWave = (MovementDataWave) wave;

		double[] dataPointCoordinates = new double[_weights.length];
		dataPointCoordinates[0] = _weights[0] * movementWave.getBulletTravelTime()
				* 0.009166667;
		dataPointCoordinates[1] = _weights[1]
				* Math.min(Math.PI / 2, wave.getAheadWallDist()) / (Math.PI / 2);
		dataPointCoordinates[2] = _weights[2]
				* Math.min(Math.PI / 2, wave.getReverseWallDist()) / (Math.PI / 2);
		dataPointCoordinates[3] = _weights[3]
				* (wave.getVChange() + Rules.DECELERATION)
				/ (Rules.ACCELERATION + Rules.DECELERATION);
		dataPointCoordinates[4] = _weights[4] 
				* Math.sin(movementWave.getRelHeading());
		
		dataPointCoordinates[5] = _weights[5]
				* (Math.cos(movementWave.getRelHeading()) + 1) / 2;
		dataPointCoordinates[6] = _weights[6]
				* Math.abs(wave.getTargetVelocity()) / 8.0;
		dataPointCoordinates[7] = _weights[7] * Math.min(1.0, wave.getTicksSinceDirChange() / movementWave.getBulletTravelTime());
		dataPointCoordinates[8] = _weights[8] * movementWave.getRobotDistLast5Ticks() * 0.025;
		dataPointCoordinates[9] = _weights[9] * movementWave.getRobotDistLast10Ticks() * 0.0125;
		dataPointCoordinates[10] = _weights[10] * movementWave.getRobotDistLast20Ticks() * 0.00625;
		dataPointCoordinates[11] = _weights[11] * movementWave.getVirtuality();
		dataPointCoordinates[12] = _weights[12] * wave.getAbsFireTime() * 4E-5;

		
		return dataPointCoordinates;
	}
}