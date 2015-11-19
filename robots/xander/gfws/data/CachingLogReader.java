package xander.gfws.data;

import java.util.HashMap;
import java.util.Map;


import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;

import xander.core.Resources;
import xander.core.event.MyWaveListener;
import xander.core.event.OpponentWaveListener;
import xander.core.event.RoundBeginListener;
import xander.core.track.Snapshot;
import xander.core.track.Wave;
import xander.core.track.XBulletWave;

public abstract class CachingLogReader implements LogReader, RoundBeginListener, MyWaveListener, OpponentWaveListener {

	private Map<Wave, WaveSearchIterable<DataPoint>> logReaderHitCache = new HashMap<Wave, WaveSearchIterable<DataPoint>>();
	private Map<Wave, WaveSearchIterable<DataPoint>> logReaderVisitCache = new HashMap<Wave, WaveSearchIterable<DataPoint>>();
	
	public CachingLogReader() {
		Resources.getRobotEvents().addRoundBeginListener(this);
		Resources.getWaveHistory().addMyWaveListener(this);
		Resources.getWaveHistory().addOpponentWaveListener(this);		
	}
	
	protected abstract WaveSearchIterable<DataPoint> readHitData(Wave wave);
	
	protected abstract WaveSearchIterable<DataPoint> readVisitData(Wave wave);
	
	@Override
	public WaveSearchIterable<DataPoint> getHitData(Wave wave) {
		WaveSearchIterable<DataPoint> data = logReaderHitCache.get(wave);
		if (data == null) {
			data = readHitData(wave);
			logReaderHitCache.put(wave, data);
		}
		return data;
	}
	
	@Override
	public WaveSearchIterable<DataPoint> getVisitData(Wave wave) {
		WaveSearchIterable<DataPoint> data = logReaderVisitCache.get(wave);
		if (data == null) {
			data = readVisitData(wave);
			logReaderVisitCache.put(wave, data);
		}
		return data;
	}

	@Override
	public void oppWaveCreated(Wave wave) {
		// no action required
	}

	@Override
	public void oppWaveHitBullet(Wave wave, Bullet oppBullet) {
		// no action required
	}

	@Override
	public void oppWaveHit(Wave wave) {
		// no action required
	}

	@Override
	public void oppNextWaveToHit(Wave wave) {
		// no action required
	}

	@Override
	public void oppBulletHit(Wave wave, HitByBulletEvent hitByBulletEvent) {
		// no action required
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
		logReaderHitCache.remove(wave);
		logReaderVisitCache.remove(wave);
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
		// no action required
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
		logReaderHitCache.remove(wave);
		logReaderVisitCache.remove(wave);
	}

	@Override
	public void onRoundBegin() {
		logReaderHitCache.clear();
		logReaderVisitCache.clear();
	}
}
