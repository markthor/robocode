package xander.gfws.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import xander.gfws.processor.FactorArrayProcessor;

public abstract class CachingFactorArrayProcessor implements FactorArrayProcessor, RoundBeginListener, MyWaveListener, OpponentWaveListener {
	
	private Map<Wave, double[]> factorArrayCache = new HashMap<Wave, double[]>();
	private List<double[]> availableFactorArrays = new ArrayList<double[]>();
	protected int factors;
	
	public CachingFactorArrayProcessor(int factors) {
		this.factors = factors;
		Resources.getRobotEvents().addRoundBeginListener(this);
		Resources.getWaveHistory().addMyWaveListener(this);
		Resources.getWaveHistory().addOpponentWaveListener(this);
	}
	
	protected abstract void fillFactorArray(double[] factorArray, Wave surfWave, double currentMinFactorAngle, double currentMaxFactorAngle);
	
	@Override
	public int getFactors() {
		return factors;
	}
	
	@Override
	public double[] getFactorArray(Wave surfWave, double currentMinFactorAngle, double currentMaxFactorAngle) {
		double[] factorArray = factorArrayCache.get(surfWave);
		if (factorArray == null) {
			if (availableFactorArrays.size() > 0) {
				factorArray = availableFactorArrays.remove(availableFactorArrays.size()-1);
			} else {
				factorArray = new double[factors];
			}
			fillFactorArray(factorArray, surfWave, currentMinFactorAngle, currentMaxFactorAngle);
			factorArrayCache.put(surfWave, factorArray);
		}
		return factorArray;
	}
	
	private void recycleFactorArray(Wave wave) {
		double[] factorArray = factorArrayCache.remove(wave);
		if (factorArray != null) {
			Arrays.fill(factorArray, 0d);
			availableFactorArrays.add(factorArray);
		}
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
		recycleFactorArray(wave);
	}


	@Override
	public void oppWaveDestroyed(Wave wave) {
		recycleFactorArray(wave);
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
		recycleFactorArray(wave);		
	}

	@Override
	public void onRoundBegin() {
		Set<Wave> leftOvers = new HashSet<Wave>();
		leftOvers.addAll(factorArrayCache.keySet());
		for (Wave wave : leftOvers) {
			recycleFactorArray(wave);
		}
	}
}
