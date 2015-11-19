package xander.gfws.segment;

import xander.core.log.Logger;
import xander.core.math.RCMath;
import xander.core.math.RCPhysics;
import xander.core.track.Wave;

public class LateralVelocitySegmenter extends AbstractSegmenter {

	public LateralVelocitySegmenter() {
		super(0, RCPhysics.MAX_SPEED);
	}

	@Override
	public String getName() {
		return "Lateral Velocity Segmenter";
	}

	@Override
	public double getSegmentValue(Wave wave) {
		double defenderToWaveAngle = RCMath.normalizeDegrees(wave.getInitialDefenderBearing()+180);
		double defenderHeading = RCMath.getBackAsFrontHeading(wave.getInitialDefenderSnapshot());
		double pheta = RCMath.getTurnAngle(defenderToWaveAngle, defenderHeading);
		double lv = wave.getInitialDefenderSnapshot().getVelocity() * Math.sin(Math.toRadians(pheta));
		return Math.abs(lv);
	}

	@Override
	public String getSegmentValueDescription(double segmentValue) {
		return "Lateral Velocity " + Logger.format(segmentValue);
	}
}
