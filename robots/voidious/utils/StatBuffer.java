package voidious.utils;

import java.awt.geom.Point2D;

import robocode.AdvancedRobot;
import robocode.Bullet;

abstract public class StatBuffer implements WaveRegister, BulletHitRegister {
	protected static final int ACCEL_SLICES = 3;
	protected double[] LATERAL_VELOCITY_SLICES = {};
	protected double[] ADVANCING_VELOCITY_SLICES = {};
	protected double[] DISTANCE_SLICES = {};
	protected double[] BULLET_TIME_SLICES = {};
	protected double[] WALL_DISTANCE_SLICES = {};
	protected double[] WALL_REVERSE_SLICES = {};
	protected double[] VCHANGE_TIME_SLICES = {};
	protected double[] SINCEMAX_TIME_SLICES = {};
	protected double[] SINCEZERO_TIME_SLICES = {};
	protected double[] DLET_SLICES = {};
	protected double[] DLFT_SLICES = {};
	protected double[] DLTFT_SLICES = {};
	protected int _bins = 59;

	public double _rollingDepth = 100;
	public double _firingWaveWeight = 1;
	public double _nonFiringWaveWeight = .2;
	public double _bulletHitWeight = 1;

	abstract public double[] getStatArray(WaveIndexSet s);

	public StatBuffer(int bins) {
		_bins = bins;
	}
	
	public void registerWaveHit(BotScan scan, BotScan enemyScan,
		double power, double factor, int gfIndex, int gfLowIndex,
		int gfHighIndex, boolean isFiringWave, long fireTime,
		int orientation, double escapeAngleRange) {
		
		WaveIndexSet indexSet = getBufferIndexSet(scan, enemyScan, power);
		
		logHit(indexSet, gfIndex, gfLowIndex, gfHighIndex, 1.0, 
			(isFiringWave? _firingWaveWeight : _nonFiringWaveWeight));
	}
	
	public void registerBulletHit(Bullet bullet, Wave closestWave,
		long hitTime) {
		
		Point2D.Double bulletLocation = 
			new Point2D.Double(bullet.getX(), bullet.getY());
		
		if (DUtils.round(bullet.getPower(), 1) != 
				DUtils.round(closestWave.bulletPower, 1)) {
			return;
		} else {
			GuessFactorWindowSet gfWindow = 
				closestWave.guessFactorWindow(bulletLocation);
			int gfBin = closestWave.guessFactorIndex(gfWindow.guessFactor);
			int gfBinLow = closestWave.guessFactorIndex(gfWindow.guessFactorLow);
			int gfBinHigh = 
				closestWave.guessFactorIndex(gfWindow.guessFactorHigh);

			WaveIndexSet indexSet = getBufferIndexSet(closestWave.targetScan, 
				closestWave.targetScan.getInverseScan(), 
				closestWave.bulletPower);
			
			logHit(indexSet, gfBin, gfBinLow, gfBinHigh, 
					DUtils.sign(_bulletHitWeight), Math.abs(_bulletHitWeight));
		}
	}
	
	public WaveIndexSet getBufferIndexSet(BotScan scan,	BotScan enemyScan, 
		double power) {
		
		WaveIndexSet indexSet = new WaveIndexSet();

		indexSet.latVelIndex = DUtils.index(LATERAL_VELOCITY_SLICES, Math.abs( 
			scan.getVelocity() * Math.sin(enemyScan.getBearingRadians())));
		indexSet.advVelIndex = DUtils.index(ADVANCING_VELOCITY_SLICES, 
			scan.getVelocity() * Math.cos(enemyScan.getBearingRadians()));
		indexSet.distanceIndex = DUtils.index(DISTANCE_SLICES, 
			scan.getDistance());
		indexSet.bulletTimeIndex = DUtils.index(BULLET_TIME_SLICES, 
			DUtils.bulletTicks(scan.getDistance(), power));
		indexSet.accelIndex = scan.getAccel();
		indexSet.wallDistanceIndex = DUtils.index(WALL_DISTANCE_SLICES,
			scan.getForwardPositionRatio(enemyScan.getLocation(), 
			power, DUtils.battleField));
		indexSet.wallReverseIndex = DUtils.index(WALL_REVERSE_SLICES,
			scan.getReversePositionRatio(enemyScan.getLocation(), 
			power, DUtils.battleField));
		indexSet.vChangeIndex = DUtils.index(VCHANGE_TIME_SLICES, 
			((double)scan.getVchangeTime()) / 
			DUtils.bulletTicks(scan.getDistance(), power));
		indexSet.sinceMaxIndex = DUtils.index(SINCEMAX_TIME_SLICES, 
			((double)scan.getSinceMaxSpeedTime()) / 
			DUtils.bulletTicks(scan.getDistance(), power));
		indexSet.sinceZeroIndex = DUtils.index(SINCEZERO_TIME_SLICES, 
			((double)scan.getTravelTime()) / 
			DUtils.bulletTicks(scan.getDistance(), power));
		indexSet.dletIndex = DUtils.index(DLET_SLICES, 
			scan.getDistanceLastEightTicks());
		indexSet.dlftIndex = DUtils.index(DLFT_SLICES, 
			scan.getDistanceLastFifteenTicks());
		indexSet.dltftIndex = DUtils.index(DLTFT_SLICES, 
			scan.getDistanceLastTwentyFiveTicks());

		return indexSet;
	}
		
	public void logHit(WaveIndexSet s, int gfIndex, int gfLowIndex, 
		int gfHighIndex, double score, double weight) {
		
		if (weight == 0) { return; }
		
		double[] thisBuffer = getStatArray(s);
		
		for (int x = 0; x < _bins; x++) {
			double divisor;
			if (x < gfLowIndex || x > gfHighIndex) {
				divisor = DUtils.square(x - gfIndex);
			} else if (x != gfIndex) {
				divisor = Math.sqrt(Math.abs(x - gfIndex));
			} else {
				divisor = 1;
			}
			thisBuffer[x+1] = rollingAverage(thisBuffer[x+1],
				Math.min(thisBuffer[0], _rollingDepth), score / divisor, 
				weight);
		}
		
		thisBuffer[0] += weight;
	}
	
	public double getBinScore(WaveIndexSet s, int gfIndex) {
		return getStatArray(s)[gfIndex+1];
	}
	
	public double rollingAverage(double oldValue, double rollingDepth,
		double newValue, double weight, int distFromHitIndex) {
		
		return rollingAverage(oldValue, rollingDepth, 
			newValue / (DUtils.square(distFromHitIndex) + 1), weight);
	}
	
	public double rollingAverage(double oldValue, double rollingDepth,
			double newValue, double weight) {

		return ((oldValue * rollingDepth) +	(newValue * weight))
				/ (rollingDepth + weight);
	}
	
	public void save(AdvancedRobot robot, String enemyName) { }
	public void restore(AdvancedRobot robot, String enemyName) { }
	
    protected void convertBinDataToByteArray(byte[] ba, int segmentIndex,
        int highBin) {
        String tempString = Integer.toBinaryString(segmentIndex);
        while (tempString.length() < 16) {
            tempString = "0" + tempString;
        }
        ba[0] = (byte)(Integer.parseInt(tempString.substring(0, 8), 2) - 128);
        ba[1] = (byte)(Integer.parseInt(tempString.substring(8, 16), 2) - 128);
        ba[2] = (byte)highBin;
    }

    protected void convertByteArrayToBinData(int[] binData, byte[] ba) {
        String tempString = Integer.toBinaryString(ba[1] + 128);
        while (tempString.length() < 8) {
            tempString = "0" + tempString;
        }
        binData[0] = Integer.parseInt(Integer.toBinaryString(ba[0] + 128)
            + tempString, 2);
        binData[1] = ba[2];
    }
}
