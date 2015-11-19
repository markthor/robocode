package xander.gfws.data;

import java.awt.geom.Point2D;


import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import xander.core.Resources;
import xander.core.RobotProxy;
import xander.core.event.MyWaveListener;
import xander.core.event.OpponentWaveListener;
import xander.core.math.RCMath;
import xander.core.track.Snapshot;
import xander.core.track.Wave;
import xander.core.track.XBulletWave;
import xander.gfws.segment.Segmenter;

public abstract class AbstractWaveLogger<T> implements MyWaveListener, OpponentWaveListener {

	protected boolean offensive;
	protected Segmenter[] segmenters;
	protected RobotProxy robotProxy;
	protected int totalHits;
	protected int totalVisits;
	protected boolean addHitsForNonFiringWaves;
	
	public AbstractWaveLogger(boolean offensive, Segmenter... segmenters) {
		this.offensive = offensive;
		this.segmenters = segmenters;
		this.robotProxy = Resources.getRobotProxy();
		if (offensive) {
			Resources.getWaveHistory().addMyWaveListener(this);
		} else {
			Resources.getWaveHistory().addOpponentWaveListener(this);
		}		
	}
	
	protected abstract void addHit(Wave wave, Segmenter[] segmenters, DataPoint dataPoint);
	
	protected abstract void addVisit(Wave wave, Segmenter[] segmenters, DataPoint dataPoint);
	
	protected abstract WaveSearchIterable<DataPoint> getHitData(Wave wave, Segmenter[] segmenters, T parms);
	
	protected abstract WaveSearchIterable<DataPoint> getVisitData(Wave wave, Segmenter[] segmenters, T parms);
	
	public WaveSearchIterable<DataPoint> getHitData(Wave wave, T parms) {
		return getHitData(wave, segmenters, parms);
	}
	
	public WaveSearchIterable<DataPoint> getVisitData(Wave wave, T parms) {
		return getVisitData(wave, segmenters, parms);
	}
	
	public Segmenter[] getSegmenters() {
		return segmenters;
	}
	
	public int getTotalHits() {
		return totalHits;
	}

	public int getTotalVisits() {
		return totalVisits;
	}
	
	public boolean isAddHitsForNonFiringWaves() {
		return addHitsForNonFiringWaves;
	}

	public void setAddHitsForNonFiringWaves(boolean addHitsForNonFiringWaves) {
		this.addHitsForNonFiringWaves = addHitsForNonFiringWaves;
	}

	private void process(Wave bulletWave, double defenderX, double defenderY) {
//		log.info("Adding data point for wave.");
		double defenderOriginalHeadingFromOrigin = bulletWave.getInitialDefenderBearing();
		double defenderCurrentHeadingFromOrigin = RCMath.getRobocodeAngle(
				bulletWave.getOriginX(), bulletWave.getOriginY(), 
				defenderX, defenderY);
		double distance = RCMath.getDistanceBetweenPoints(
				bulletWave.getOriginX(), bulletWave.getOriginY(), 
				defenderX, defenderY);
		double angle = RCMath.getTurnAngle(defenderOriginalHeadingFromOrigin, defenderCurrentHeadingFromOrigin);
		DataPoint dataPoint = new DataPoint(angle, distance, bulletWave.getBulletVelocity(), bulletWave.getSurfDirection(), 
				bulletWave.getInitialMEA(), Resources.getCumulativeTime(), Resources.getTime(), bulletWave.isOpponentWave());
		addHit(bulletWave, segmenters, dataPoint);
		totalHits++;
//		log.info("totalPoints=" + totalPoints + "; k value updated to " + k);
	}
	
	private void processVisit(Wave wave, double defenderX, double defenderY) {
		double defenderOriginalHeadingFromOrigin = wave.getInitialDefenderBearing();
		double defenderCurrentHeadingFromOrigin = RCMath.getRobocodeAngle(
				wave.getOriginX(), wave.getOriginY(), 
				defenderX, defenderY);		
		double angle = RCMath.getTurnAngle(defenderOriginalHeadingFromOrigin, defenderCurrentHeadingFromOrigin);
		double distance = RCMath.getDistanceBetweenPoints(
				wave.getOriginX(), wave.getOriginY(), 
				defenderX, defenderY);
		DataPoint dataPoint = new DataPoint(angle, distance, wave.getBulletVelocity(), wave.getSurfDirection(), 
				wave.getInitialMEA(), Resources.getCumulativeTime(), Resources.getTime(), wave.isOpponentWave());
		addVisit(wave, segmenters, dataPoint);
		totalVisits++;
	}
	
	@Override
	public void oppWaveCreated(Wave wave) {
		// no action required		
	}

	@Override
	public void oppWaveHitBullet(Wave wave, Bullet oppBullet) {
		if (!offensive) {
			double hitAngle = RCMath.getRobocodeAngle(
					wave.getOriginX(), wave.getOriginY(), oppBullet.getX(), oppBullet.getY());
			double hitDistance = RCMath.getDistanceBetweenPoints(
					wave.getOriginX(), wave.getOriginY(), robotProxy.getX(), robotProxy.getY());
			Point2D.Double hitLocation = RCMath.getLocation(
					wave.getOriginX(), wave.getOriginY(), hitDistance, hitAngle);
			process(wave, hitLocation.x, hitLocation.y);
		}
	}

	@Override
	public void oppWaveHit(Wave wave) {
		if (!offensive) {
			processVisit(wave, robotProxy.getX(), robotProxy.getY());
		}
	}

	@Override
	public void oppNextWaveToHit(Wave wave) {
		// no action required
	}

	@Override
	public void oppBulletHit(Wave wave, HitByBulletEvent hitByBulletEvent) {
		if (!offensive) {
			process(wave, robotProxy.getX(), robotProxy.getY());
		}
	}

	@Override
	public void oppWavePassing(Wave wave) {
		// no action required	
	}

	@Override
	public void oppWavePassed(Wave wave) {
		// no action required		
	}

	@Override
	public void oppWaveUpdated(Wave wave) {
		// no action required
	}

	@Override
	public void oppWaveDestroyed(Wave wave) {
		// no action required		
	}

	@Override
	public void myWaveCreated(XBulletWave wave) {
		// no action required		
	}

	@Override
	public void myWaveHitBullet(XBulletWave wave, Bullet myBullet) {
		// no action required		
	}

	@Override
	public void myWaveHit(XBulletWave wave, Snapshot opponentSnapshot) {
		if (offensive) {
			process(wave, opponentSnapshot.getX(), opponentSnapshot.getY());
		}
	}

	@Override
	public void myBulletHit(XBulletWave wave, BulletHitEvent bulletHitEvent) {
		// no action required
	}

	@Override
	public void myWavePassing(XBulletWave wave, Snapshot opponentSnapshot) {
		// no action required
	}

	@Override
	public void myWavePassed(XBulletWave wave, Snapshot opponentSnapshot) {
		// no action required
	}

	@Override
	public void myWaveDestroyed(XBulletWave wave) {
		// no action required
	}
}
