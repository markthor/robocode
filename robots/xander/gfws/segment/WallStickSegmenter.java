package xander.gfws.segment;

import java.awt.geom.Rectangle2D;

import xander.core.Resources;
import xander.core.log.Logger;
import xander.core.math.RCMath;
import xander.core.track.Snapshot;
import xander.core.track.Wave;

public class WallStickSegmenter extends AbstractSegmenter {

	private boolean forwardDirection;
	private Rectangle2D.Double battleFieldSize;
	
	public WallStickSegmenter(double minDistanceToWall,
			double maxDistanceToWall, boolean forwardDirection) {
		super(minDistanceToWall, maxDistanceToWall);
		this.forwardDirection = forwardDirection;
		this.battleFieldSize = Resources.getRobotProxy().getBattleFieldSize();
	}

	@Override
	public String getName() {
		return "Wall Stick Segmenter";
	}

	@Override
	public double getSegmentValue(Wave wave) {
		Snapshot snapshot = wave.getInitialDefenderSnapshot();
		double heading = snapshot.getHeadingRoboDegrees();
		if (snapshot.getVelocity() < 0) {
			heading = RCMath.normalizeDegrees(heading + 180);
		}
		double stickHeading = forwardDirection? heading : RCMath.normalizeDegrees(heading + 180);
		double distanceToWall = RCMath.getDistanceToIntersect(snapshot.getX(), snapshot.getY(), stickHeading, battleFieldSize);
		return distanceToWall;
	}

	@Override
	public String getSegmentValueDescription(double segmentValue) {
		return "Distance to wall " + Logger.format(segmentValue);
	}



}
