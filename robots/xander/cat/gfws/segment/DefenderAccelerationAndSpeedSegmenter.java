package xander.cat.gfws.segment;

import xander.core.Resources;
import xander.core.log.Logger;
import xander.core.math.RCPhysics;
import xander.core.track.Snapshot;
import xander.core.track.SnapshotHistory;
import xander.core.track.Wave;
import xander.gfws.segment.AbstractSegmenter;

public class DefenderAccelerationAndSpeedSegmenter extends AbstractSegmenter {

	private SnapshotHistory snapshotHistory;
	
	public DefenderAccelerationAndSpeedSegmenter() {
		super(0, RCPhysics.MAX_SPEED*2 + 2);
		this.snapshotHistory = Resources.getSnapshotHistory();
	}

	@Override
	public String getName() {
		return "Defender Accel and Speed Segmenter";
	}

	@Override
	public double getSegmentValue(Wave wave) {
		Snapshot snap = wave.getInitialDefenderSnapshot();
		Snapshot prevSnap = snapshotHistory.getPreviousSnapshot(snap);
		double segmentValue = RCPhysics.MAX_SPEED + 1;
		if (prevSnap != null) {
			double v1 = snap.getVelocity();
			double v0 = prevSnap.getVelocity();
			if (RCPhysics.isAcceleration(v0, v1)) {
				segmentValue = Math.abs(v1);
			} else if (RCPhysics.isDeceleration(v0, v1)) {
				segmentValue = RCPhysics.MAX_SPEED*2 + 2 - Math.abs(v1);
			}
		}
		return segmentValue;
	}

	@Override
	public String getSegmentValueDescription(double segmentValue) {
		if (segmentValue <= RCPhysics.MAX_SPEED) {
			return "Accel / Speed " + Logger.format(segmentValue, 1);
		} else if (segmentValue >= RCPhysics.MAX_SPEED + 2) {
			return "Decel / Speed " + Logger.format(RCPhysics.MAX_SPEED*2 + 2 - segmentValue, 1);
		} else {
			return "Constant Speed";
		}
	}
}
