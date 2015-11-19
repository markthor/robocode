package xander.gfws.segment;

import java.awt.geom.Rectangle2D;

import xander.core.log.Logger;
import xander.core.math.RCPhysics;
import xander.core.track.Wave;

/**
 * Segmenter that segments by approximate bullet travel time to defender.
 * 
 * @author Scott Arnold
 */
public class BulletTravelTimeSegmenter extends AbstractSegmenter {
	
	public BulletTravelTimeSegmenter(Rectangle2D.Double battlefieldBounds) {
		super(1, Math.sqrt(battlefieldBounds.getWidth()*battlefieldBounds.getWidth() + battlefieldBounds.getHeight()*battlefieldBounds.getHeight()) / RCPhysics.MIN_BULLET_VELOCITY);
	}
	
	public String getName() {
		return "Bullet Travel Time Segmenter";
	}

	@Override
	public double getSegmentValue(Wave wave) {
		// snapshots for self don't have distance values, so take whichever snapshot has a non-zero value
		double distance = Math.max(wave.getInitialAttackerSnapshot().getDistance(), wave.getInitialDefenderSnapshot().getDistance());
		return distance / wave.getBulletVelocity();
	}

	@Override
	public String getSegmentValueDescription(double segmentValue) {
		return "Bullet Travel Time " + Logger.format(segmentValue, 0);
	}
}
