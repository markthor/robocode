package xander.gfws.segment;

import xander.core.Resources;
import xander.core.track.Snapshot;
import xander.core.track.SnapshotHistory;
import xander.core.track.Wave;

public class DefenderAccelerationSegmenter implements Segmenter {

	private String myRobotName;
	private enum Accel {
		STEADY_SPEED, ACCELERATING, DECELERATING;
	}
	
	public DefenderAccelerationSegmenter() {
		this.myRobotName = Resources.getRobotProxy().getName();
	}
	
	@Override
	public String getName() {
		return "Defender Acceleration Segmenter";
	}

	private Accel getAccel(Wave wave) {
		SnapshotHistory history = Resources.getSnapshotHistory();
		Snapshot defenderSnapshot = wave.getInitialDefenderSnapshot();
		Snapshot previousDefenderSnapshot = null;
		if (defenderSnapshot.getName().equals(myRobotName)) {
			previousDefenderSnapshot = history.getMySnapshot(defenderSnapshot.getTime()-1, false);
		} else {
			previousDefenderSnapshot = history.getSnapshot(
					defenderSnapshot.getName(), defenderSnapshot.getTime()-1, false);
		}
		double s = defenderSnapshot.getVelocity();
		double ps = (previousDefenderSnapshot == null)? s : previousDefenderSnapshot.getVelocity();
		if (s == ps) {
			// steady speed
			return Accel.STEADY_SPEED;			
		} else if ((s > 0 && ps < 0) || (s < 0 && ps > 0) || (Math.abs(s) > Math.abs(ps))) {
			// accelerating
			return Accel.ACCELERATING;
		} else {
			// decelerating
			return Accel.DECELERATING;
		}
	}

	@Override
	public double getSegmentValue(Wave wave) {
		return getAccel(wave).ordinal();
	}

	@Override
	public double getSegmentValuePercent(Wave wave) {
		Accel accel = getAccel(wave);
		switch (accel) {
		case STEADY_SPEED:
			return 0.5;
		case DECELERATING:
			return 0;
		default:
			return 1;
		}
	}

	@Override
	public String getSegmentValueDescription(double segmentValue) {
		int ord = (int)Math.round(segmentValue);
		return Accel.values()[ord].toString();
	}

}
