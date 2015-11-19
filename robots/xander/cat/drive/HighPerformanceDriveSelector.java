package xander.cat.drive;

import xander.core.Resources;
import xander.core.drive.Drive;
import xander.core.drive.DriveSelector;
import xander.core.track.GunStats;
import xander.core.track.Snapshot;

/**
 * Drive selector for a Drive Array that switches between drives based on their performance.
 * This drive selector can be set up with a default drive to use when none of the other drives
 * are performing better than a set margin.
 * 
 * @author Scott Arnold
 */
public class HighPerformanceDriveSelector implements DriveSelector {

	private Drive defaultDrive;   // drive used when none of the other drives are high performance
	private int defaultDriveIndex;
	private int shotsForAccuracy;
	private double highPerformanceHitRatio;
	private GunStats gunStats;
	
	public HighPerformanceDriveSelector(Drive defaultDrive, int shotsForAccuracy, double highPerformanceHitRatio) {
		this.defaultDrive = defaultDrive;
		this.shotsForAccuracy = shotsForAccuracy;
		this.highPerformanceHitRatio = highPerformanceHitRatio;
		this.gunStats = Resources.getGunStats();
	}
	
	public HighPerformanceDriveSelector(int shotsForAccuracy) {
		this(null, shotsForAccuracy, 1);
	}
	
	@Override
	public int selectDrive(Drive[] drives, Snapshot snapshot) {
		int bestIndex = -1;
		double bestScore = 1;
		for (int i=0; i<drives.length; i++) {
			Drive drive = drives[i];
			double hitRatioAgainstDrive = gunStats.getOpponentHitRatioAgainstDrive(drive.getName());
			int shotsAgainstDrive = gunStats.getShotsAgainstDrive(drive.getName());
			double confidenceMultiplier = Math.min(1, Math.pow((double)shotsAgainstDrive/(double)shotsForAccuracy, 2));
			double weightedScore = hitRatioAgainstDrive * confidenceMultiplier;
			if (weightedScore <= highPerformanceHitRatio && weightedScore < bestScore) {
				bestIndex = i;
				bestScore = weightedScore;
			}
			if (drive == defaultDrive) {
				defaultDriveIndex = i;
			}
		} 
		return (bestIndex < 0)? defaultDriveIndex : bestIndex;
	}

}
