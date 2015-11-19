package xander.cat.gfws;

import robocode.Bullet;
import robocode.BulletHitEvent;
import robocode.HitByBulletEvent;
import xander.core.Resources;
import xander.core.event.MyWaveListener;
import xander.core.event.OpponentWaveListener;
import xander.core.math.LinearEquation;
import xander.core.track.GunStats;
import xander.core.track.Snapshot;
import xander.core.track.Wave;
import xander.core.track.XBulletWave;
import xander.gfws.data.AgeRollFunction;
import xander.gfws.data.AgeRollLinearFunction;
import xander.gfws.data.AgeRollSquaredFunction;
import xander.gfws.data.AgeRollVariableDecaySquaredFunction;
import xander.gfws.data.FactorArrayParms;

public class AutoVaryWeightByAge implements OpponentWaveListener, MyWaveListener {

	public static enum RollType { LINEAR, SQUARED, VD_SQUARED }
	
	private FactorArrayParms parms;
	private double noRollHitRatio;
	private LinearEquation rollEquation;
	private LinearEquation minWeightEquation;
	private GunStats gunStats;
	private int activationRound = 0;
	private RollType rollType;
	private AgeRollFunction ageRollFunction;
	
	public AutoVaryWeightByAge(FactorArrayParms parms, 
			double noRollHitRatio, 
			double lowRollHitRatio, double lowRollRate, 
			double highRollHitRatio, double highRollRate,
			RollType rollType) {
		this.parms = parms;
		this.rollType = rollType;
		this.noRollHitRatio = noRollHitRatio;
		this.rollEquation = new LinearEquation(lowRollHitRatio, lowRollRate, highRollHitRatio, highRollRate, lowRollRate, highRollRate);
		this.gunStats = Resources.getGunStats();
		if (rollType == RollType.LINEAR) {
			this.ageRollFunction = new AgeRollLinearFunction(0);
		} else if (rollType == RollType.SQUARED) {
			this.ageRollFunction = new AgeRollSquaredFunction(1000);  // initial value doesn't really matter
		} else if (rollType == RollType.VD_SQUARED) {
			this.ageRollFunction = new AgeRollVariableDecaySquaredFunction();
		}
		if (parms.isOffensive()) {
			Resources.getWaveHistory().addMyWaveListener(this);
		} else {
			Resources.getWaveHistory().addOpponentWaveListener(this);
		}
	}

	/**
	 * Sets equation for varying minAgeWeightPercent based on hit ratio difference.  For defensive use only.
	 * 
	 * @param lowHRDiff
	 * @param lowHRDiffMinWeight
	 * @param highHRDiff
	 * @param highHRDiffMinWeight
	 */
	public void setVaryMinWeight(double lowHRDiff, double lowHRDiffMinWeight, double highHRDiff, double highHRDiffMinWeight) {
		double minY = Math.min(lowHRDiffMinWeight, highHRDiffMinWeight);
		double maxY = Math.max(lowHRDiffMinWeight, highHRDiffMinWeight);
		this.minWeightEquation = new LinearEquation(lowHRDiff, lowHRDiffMinWeight, highHRDiff, highHRDiffMinWeight, minY, maxY);
	}
	
	public void setActivationRound(int activationRound) {
		this.activationRound = activationRound;
	}
	
	private void setAgeRoll(double rollValue) {
		if (rollValue <= 0) {
			parms.setVaryWeightByAgeFunction(null);
		} else {
			if (parms.getVaryWeightByAgeFunction() == null) {
				parms.setVaryWeightByAgeFunction(ageRollFunction);
			}
			if (rollType == RollType.LINEAR) {
				((AgeRollLinearFunction)ageRollFunction).setDecayPerTick(rollValue);
			} else if (rollType == RollType.SQUARED) {
				((AgeRollSquaredFunction)ageRollFunction).setDropOffAge(Math.round(rollValue));
			} else if (rollType == RollType.VD_SQUARED) {
				((AgeRollVariableDecaySquaredFunction)ageRollFunction).setTimeMultiplier(rollValue);
			}
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
		if (Resources.getRobotProxy().getRoundNum() >= activationRound) {
			double oppHitRatio = gunStats.getOverallOpponentHitRatio();
			if (oppHitRatio <= noRollHitRatio) {
				setAgeRoll(0);
			} else {
				setAgeRoll(rollEquation.getY(oppHitRatio));
				if (minWeightEquation != null) {
					double myHitRatio = gunStats.getOverallHitRatio();
					double hrDiff = myHitRatio - oppHitRatio;
					parms.setMinAgeWeightPercent(minWeightEquation.getY(hrDiff));
				}
			}
		}
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
		if (Resources.getRobotProxy().getRoundNum() >= activationRound) {
			double myHitRatio = gunStats.getOverallHitRatio();
			if (myHitRatio >= noRollHitRatio) {
				setAgeRoll(0);
			} else {
				setAgeRoll(rollEquation.getY(myHitRatio));
			}
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
