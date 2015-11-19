package voidious.utils;

import robocode.*;
import robocode.Bullet;
import java.util.ArrayList;

public class StatBufferSet implements WaveRegister, BulletHitRegister {
	private ArrayList _statBuffers;

	public static final boolean BEST_BIN = true;
	public static final boolean WORST_BIN = false;
	
	public StatBufferSet() {
		_statBuffers = new ArrayList();
	}
	
	public void addStatBuffer(StatBuffer sb, double weight) {
		_statBuffers.add(new BufferEntry(sb, weight));
	}
	
	public void registerWaveHit(BotScan scan, BotScan enemyScan,
		double power, double factor, int gfIndex, int gfLowIndex,
		int gfHighIndex, boolean isFiringWave, long fireTime,
		int orientation, double escapeAngleRange) {

		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer.registerWaveHit(scan, enemyScan, power, factor, gfIndex,
				gfLowIndex, gfHighIndex, isFiringWave, fireTime,
				orientation, escapeAngleRange);
		}
	}

	public void registerBulletHit(Bullet bullet, Wave closestWave,
		long hitTime) {
		
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			((BulletHitRegister)be.buffer).registerBulletHit(bullet, 
				closestWave, hitTime);
		}
	}

	public double getBinScore(BotScan scan, BotScan enemyScan,
		double power, int gfIndex) {

		double binScore = 0;
		
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			binScore += be.weight * be.buffer.getBinScore(
				be.buffer.getBufferIndexSet(scan, enemyScan, power), gfIndex);
		}
		
		return binScore;
	}
	
	public double getWindowScore(BotScan scan, BotScan enemyScan,
		double power, int numBins, int lowBin, int highBin) {

		double windowScore = 0;
		lowBin = Math.max(0, lowBin);
		highBin = Math.min(numBins - 1, highBin);
		
		for (int x = lowBin; x <= highBin; x++) {
			windowScore += getBinScore(scan, enemyScan, power, x);
		}
		
		return windowScore / (highBin - lowBin + 1);
	}
	
	public int getBestBin(BotScan scan, BotScan enemyScan, double power,
		int numBins) {

		return getBestBin(scan, enemyScan, power, numBins, 0, numBins - 1);
	}

	public int getWorstBin(BotScan scan, BotScan enemyScan, double power,
		int numBins) {

		return getWorstBin(scan, enemyScan, power, numBins, 0, numBins - 1);
	}

	public int getBestPositiveBin(BotScan scan, BotScan enemyScan, double power,
		int numBins) {

		return getBestBin(scan, enemyScan, power, numBins, (numBins - 1) / 2, 
			numBins - 1);
	}

	public int getBestNegativeBin(BotScan scan, BotScan enemyScan, double power,
		int numBins) {

		return getBestBin(scan, enemyScan, power, numBins, 0, 
			(numBins - 1) / 2);
	}

	public int getBestBin(BotScan scan, BotScan enemyScan, double power,
			int numBins, int lowBin, int highBin) {

		return getBestOrWorstBin(scan, enemyScan, power, numBins,
			lowBin, highBin, BEST_BIN);
	}

	public int getWorstBin(BotScan scan, BotScan enemyScan, double power,
			int numBins, int lowBin, int highBin) {

		return getBestOrWorstBin(scan, enemyScan, power, numBins,
			lowBin, highBin, WORST_BIN);
	}

	public int getBestOrWorstBin(BotScan scan, BotScan enemyScan, double power,
		int numBins, int lowBin, int highBin, boolean lookForBestBin) {

		WaveIndexSet[] bufferSegments = new WaveIndexSet[_statBuffers.size()];
		BufferEntry[] weightedBuffers = new BufferEntry[_statBuffers.size()];
		
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);

			weightedBuffers[x] = be;
			bufferSegments[x] = 
				be.buffer.getBufferIndexSet(scan, enemyScan, power); 
		}

		lowBin = Math.max(0, lowBin);
		highBin = Math.min(numBins - 1, highBin);
		double bestScore = (lookForBestBin ? 
			Double.NEGATIVE_INFINITY : Double.POSITIVE_INFINITY);
		int bestIndex = (numBins - 1) / 2;

		for (int x = lowBin; x <= highBin; x++) {
			double binScore = 0;

			for (int y = 0; y < _statBuffers.size(); y++) {
				binScore += weightedBuffers[y].weight * 
					weightedBuffers[y].buffer.getBinScore(bufferSegments[y], x);
			}
			
			if ((lookForBestBin && binScore > bestScore) ||
				(!lookForBestBin && binScore < bestScore)) {
				bestScore = binScore;
				bestIndex = x;
			}
		}
		
		return bestIndex;
	}
	
	public void save(AdvancedRobot robot, String enemyName) {
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer.save(robot, enemyName);
		}
	}
	
	public void restore(AdvancedRobot robot, String enemyName) {
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer.restore(robot, enemyName);
		}
	}
	
	public void setBulletHitWeight(double d) {
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer._bulletHitWeight = d;
		}
	}
	
	public void setFiringWaveWeight(double d) {
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer._firingWaveWeight = d;
		}
	}
	
	public void setNonFiringWaveWeight(double d) {
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer._nonFiringWaveWeight = d;
		}
	}

/*
	public void setFiringWaveHitWeight(double d) {
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer._firingWaveWeight = d;
		}
	}

	public void setNonFiringWaveWeight(double d) {
		for (int x = 0; x < _statBuffers.size(); x++) {
			BufferEntry be = (BufferEntry)_statBuffers.get(x);
			
			be.buffer._nonFiringWaveWeight = d;
		}
	}
*/
}

class BufferEntry {
	public StatBuffer buffer;
	public double weight;
	
	public BufferEntry(StatBuffer b, double w) {
		buffer = b;
		weight = w;
	}
}
