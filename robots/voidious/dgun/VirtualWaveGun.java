package voidious.dgun;

import robocode.*;
import robocode.util.Utils;
import voidious.utils.BotScan;
import voidious.utils.StatBufferSet;
import voidious.utils.Wave;

/**
 * VirtualGun
 *
 * A gun that makes it easy to use in a VirtualGuns array.
 *
 * This code is open source, released under the RoboWiki Public Code License:
 * http://robowiki.net/cgi-bin/robowiki?RWPCL
 */

public class VirtualWaveGun {
	protected String _label;
	
    protected double _lastAimAngle = 0;
    protected int _lastAimBin = 0;
    protected double _lastFireTime = -1;
    protected double _rollingDepth = 2500;
    protected double _shots = 0;
    protected double _rating = 0;
    protected double _ratingMultiplier = 1;
    protected int _lowBin = 0;
    protected int _highBin = 58;
    
	private StatBufferSet _statBuffers;
	private int _bins;

	public VirtualWaveGun(String label, StatBufferSet bufferSet, int bins) {
		_label = label;
		_statBuffers = bufferSet;
		_bins = bins;
		_highBin = _bins - 1;
	}
	
	public double getAbsoluteAimAngle(Wave aimWave, BotScan scan, 
		BotScan enemyScan, double bulletPower) {
		
		if (_lastFireTime == aimWave.fireTime) {
			return _lastAimAngle;
		}
		
		_lastAimAngle = Utils.normalAbsoluteAngle(aimWave.absBearingRadians + 
			aimWave.guessAngleFromIndex(
					getAimBin(aimWave, scan, enemyScan, bulletPower)));

		_lastFireTime = aimWave.fireTime;

		return _lastAimAngle;
	}
	
	public int getAimBin(Wave aimWave, BotScan scan, BotScan enemyScan, 
		double bulletPower) {
		
		if (_lastFireTime == aimWave.fireTime) {
			return _lastAimBin;
		}
		
		_lastAimBin = getBestBin(scan, enemyScan, bulletPower, _bins);
		
		return _lastAimBin;
	}
		
	public int getBestBin(BotScan scan, BotScan enemyScan, 
		double bulletPower,	int bins) {
		return _statBuffers.getBestBin(scan, enemyScan, bulletPower, bins,
			_lowBin, _highBin);
	}

    public double getLastAimAngle() {
        return _lastAimAngle;
    }

    public int getLastAimBin() {
        return _lastAimBin;
    }

    public void logHit(double weight) {
    	logScore(1.0, weight);
    }

    public void logMiss(double weight) {
    	logScore(0.0, weight);
    }

    public void logHit() {
        logScore(1.0);
    }

    public void logMiss() {
        logScore(0.0);
    }

    public void logScore(double score) {
    	logScore(score, 1);
    }

    public void logScore(double score, double weight) {
        _rating = (double)((_rating*Math.min(_shots, _rollingDepth) + (score * weight))
                    / (Math.min(_shots, _rollingDepth) + weight));
        _shots += weight;
    }

    public double getRating() {
        return _rating * _ratingMultiplier;
    }

    public void setRating(double r) {
        _rating = r;
    }

    public double getRatingMultiplier() {
        return _ratingMultiplier;
    }

    public void setRatingMultiplier(double r) {
        _ratingMultiplier = r;
    }

    public double getShots() {
        return _shots;
    }

    public void setShots(double s) {
        _shots = s;
    }
    
    public String getLabel() {
    	return _label;
    }
    
    public void save(AdvancedRobot robot, String enemyName) {
    	_statBuffers.save(robot, enemyName);
    }

    public void restore(AdvancedRobot robot, String enemyName) {
    	_statBuffers.restore(robot, enemyName);
    }

    public void setToNegativeGuessFactors() {
    	_lowBin = 0;
    	_highBin = (_bins - 1) / 2;
    }
    
    public void setToPositiveGuessFactors() {
    	_lowBin = (_bins - 1) / 2;
    	_highBin = _bins - 1;
    }
    
    public void setToAllGuessFactors() {
    	_lowBin = 0;
    	_highBin = _bins - 1;
    }
}
