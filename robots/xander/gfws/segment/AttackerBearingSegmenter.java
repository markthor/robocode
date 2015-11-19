package xander.gfws.segment;

import xander.core.math.RCMath;
import xander.core.track.Wave;
import xander.core.log.Logger;

/**
 * Segments on bearing of attacker from the back-as-front heading of the defender.
 * 
 * @author Scott Arnold
 */
public class AttackerBearingSegmenter extends AbstractSegmenter {

	public AttackerBearingSegmenter() {
		super(-180, 180);
	}

	@Override
	public String getName() {
		return "Defender Heading Segmenter";
	}

	@Override
	public double getSegmentValue(Wave wave) {
		double attackerAtAngle = RCMath.normalizeDegrees(180 + wave.getInitialDefenderBearing());
		double defenderHeading = wave.getInitialDefenderSnapshot().getHeadingRoboDegrees();
		if (wave.getInitialDefenderSnapshot().getVelocity() < 0) {
			defenderHeading = RCMath.normalizeDegrees(180 + defenderHeading);
		}
		return RCMath.getTurnAngle(defenderHeading, attackerAtAngle);
	}

	@Override
	public String getSegmentValueDescription(double segmentValue) {
		return "Attacker bearing " + Logger.format(segmentValue);
	}
}
