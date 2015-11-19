package cs.mint.gun;

import robocode.util.Utils;
import cs.mint.MintState;
import cs.mint.util.Simulate;

public class GuaranteedHitTargeting {
	public static double getPerfectAim(CloverWave wave, MintState state) {
		double distance = wave.distanceSq(state.targetPosition);
		if(distance > wave.speed*wave.speed*100) /* 121 might be better */
			return Double.NaN;
		
		double[] r0 = getRangeMinMax(wave,state,1);
		double[] r1 = getRangeMinMax(wave,state,-1);
		
		if(r1[0] > r0[1] || r0[0] > r1[1])
			return Double.NaN;
		
		return 0.5*(Math.max(r0[0], r1[0])+Math.min(r0[1], r1[1]));
	}
	
	public static double[] getRangeMinMax(CloverWave wave, MintState state, int direction) {
		CloverWave daWave = (CloverWave) wave.clone();
		
		Simulate sim = new Simulate();
		sim.position = state.targetPosition.clone();
		sim.heading = state.targetHeading;
		sim.velocity = state.targetVelocity;
		sim.direction = direction;
		
		long time = daWave.fireTime-1;
		while(true) {
			daWave.update(time++, sim.position);
			
			if(daWave.isCompleted())
				break;
			
			double goalAngle = wave.angleTo(sim.position) + Math.PI/2.0;
			
			sim.angleToTurn = -Utils.normalRelativeAngle(sim.heading - goalAngle);
			if(Math.abs(sim.angleToTurn) > Math.PI/2.0)
				sim.angleToTurn = Utils.normalRelativeAngle(sim.angleToTurn+Math.PI);
			
			sim.step();
		}
		
		return new double[] {daWave.minFactor,daWave.maxFactor};
	}
}
