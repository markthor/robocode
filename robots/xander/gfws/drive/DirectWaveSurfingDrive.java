package xander.gfws.drive;

import java.awt.geom.Path2D;
import java.util.Arrays;

import robocode.Bullet;
import robocode.HitByBulletEvent;
import xander.core.Resources;
import xander.core.RobotProxy;
import xander.core.drive.DirectDrivePredictor;
import xander.core.drive.Drive;
import xander.core.drive.DriveController;
import xander.core.drive.DriveState;
import xander.core.event.OpponentWaveListener;
import xander.core.math.RCMath;
import xander.core.math.RCPhysics;
import xander.core.track.Snapshot;
import xander.core.track.SnapshotHistory;
import xander.core.track.Wave;
import xander.paint.Paintable;
import xander.paint.Paintables;

public class DirectWaveSurfingDrive implements Drive, OpponentWaveListener, Paintable {
	
//	private static final Log log = Logger.getLog(DirectWaveSurfingDrive.class);
	
	private String driveName;
	private RobotProxy robotProxy;
	private SnapshotHistory snapshotHistory;
	private DirectDrivePredictor predictor;
	private boolean surfMultipleWaves;
	private int wave1SurfOptions = 4;  // for when surfing multiple waves
	private Wave surfWave;
	private Wave lastSurfWave;
	private boolean surfWaveUpdated;
	private double oppSurfX;
	private double oppSurfY;
	private double[] oppSurfXYShift;
	private DirectSurfSelection[] wave1SurfSelections = new DirectSurfSelection[1];
	private DirectSurfSelection[] wave2SurfSelections = null;
	private DirectSurfSelector surfSelector;
	private double cTargetHeading;
	private double cTargetFactorAngle;
	private int[] optionCounts;  // debug counters
	private String painterName;
	
//	private RunTimeLogger firstWaveRTL;
//	private RunTimeLogger secondWaveRTL;
	
	public DirectWaveSurfingDrive(String driveName, DirectSurfSelector surfSelector, Path2D.Double driveBounds) {
//		firstWaveRTL = RunTimeLogger.getLoggerFor("1W SS");
//		secondWaveRTL = RunTimeLogger.getLoggerFor("2W SS");
		this.driveName = driveName;
		this.robotProxy = Resources.getRobotProxy();
		this.snapshotHistory = Resources.getSnapshotHistory();
		this.surfSelector = surfSelector;
		this.predictor = new DirectDrivePredictor(robotProxy.getBattleFieldSize(), driveBounds);
		this.wave1SurfSelections[0] = new DirectSurfSelection();
		Resources.getWaveHistory().addOpponentWaveListener(this);
	}
	
	public double getCTargetHeading() {
		return cTargetHeading;
	}
	
	public double getCTargetFactorAngle() {
		return cTargetFactorAngle;
	}
	
	public Wave getCSurfWave() {
		return surfWave;
	}
	
	public DirectDrivePredictor getCDirectDrivePredictor() {
		return predictor;
	}
	
	@Override
	public String getName() {
		return driveName;
	}

	@Override
	public String getPainterName() {
		return painterName;
	}

	public void setPainterName(String painterName) {
		this.painterName = painterName;
		Paintables.addPaintable(this);
	}
	
	public boolean isSurfMultipleWaves() {
		return surfMultipleWaves;
	}

	public void setSurfMultipleWaves(boolean surfMultipleWaves, int wave1SurfOptions) {
		this.surfMultipleWaves = surfMultipleWaves;
		this.wave1SurfOptions = wave1SurfOptions;
		if (surfMultipleWaves) {
			this.wave1SurfSelections = new DirectSurfSelection[wave1SurfOptions];
			for (int i=0; i<wave1SurfOptions; i++) {
				this.wave1SurfSelections[i] = new DirectSurfSelection();
			}
			this.wave2SurfSelections = new DirectSurfSelection[1];
			this.wave2SurfSelections[0] = new DirectSurfSelection();
			this.optionCounts = new int[wave1SurfOptions];
		} else {
			this.wave1SurfSelections = new DirectSurfSelection[1];
			this.wave1SurfSelections[0] = new DirectSurfSelection();
			this.wave2SurfSelections = null;
			this.optionCounts = null;
		}
	}

	public void setSurfMultipleWaves(boolean surfMultipleWaves) {
		setSurfMultipleWaves(surfMultipleWaves, wave1SurfOptions);
	}
	
	@Override
	public void onRoundBegin() {
		this.surfWave = null;
		this.lastSurfWave = null;
	}
	
	@Override
	public void drive(DriveController driveController) {
		if (robotProxy.getOthers() <= 0 && Resources.getWaveHistory().getOpponentActiveWaveCount() > 0 && surfWave != null) {
			// opponent is dead but opponent bullets are still in play
			// fake an opponent snapshot and continue to surf
			Snapshot rs = snapshotHistory.getSnapshot(surfWave.getInitialAttackerSnapshot().getName()); // only valid for 1 vs 1
			double distance = RCMath.getDistanceBetweenPoints(
					rs.getX(), rs.getY(), robotProxy.getX(), robotProxy.getY());
			Snapshot fakeRS = new Snapshot(
					rs.getName(), rs.getX(), rs.getY(), 
					rs.getHeadingRoboRadians(), 0, distance, 
					rs.getEnergy(), robotProxy.getTime());
			driveTo(fakeRS, driveController);
		}
	}
	
	@Override
	public void driveTo(Snapshot opponentSnapshot,
			DriveController driveController) {
		long time = robotProxy.getTime();
		Snapshot mySnapshot = snapshotHistory.getMySnapshot(time, true);
		DriveState myDriveState = new DriveState(mySnapshot);
		double oppDistDiff = 0;
		if (oppSurfXYShift != null && surfWave != null) {
			oppSurfX += oppSurfXYShift[0];
			oppSurfY += oppSurfXYShift[1];
			oppDistDiff = RCMath.getDistanceBetweenPoints(oppSurfX, oppSurfY, opponentSnapshot.getX(), opponentSnapshot.getY());
		}
		if (surfWave != null && (surfWave != lastSurfWave || oppDistDiff > 40 || surfWaveUpdated)) {
			// update the surf target factor angle and target heading
			// first initialize all surf selection danger values
			// this is done as when surfing multiple waves, the surf selector is
			// not required to fill every surf selection.
			for (int i=0; i<wave1SurfSelections.length; i++) {
				wave1SurfSelections[i].clear();
			}
//			firstWaveRTL.start();
			surfSelector.updateSurfSelection(wave1SurfSelections, surfWave, predictor, myDriveState, opponentSnapshot, time);
//			firstWaveRTL.stop();
			cTargetFactorAngle = wave1SurfSelections[0].getFactorAngle();
			cTargetHeading = wave1SurfSelections[0].getHeading();
			// surf second wave
			if (surfMultipleWaves) {
				Wave secondWave = Resources.getWaveHistory().getOpponentWaveAfter(surfWave, robotProxy.getX(), robotProxy.getY());
				if (secondWave != null) {
//					secondWaveRTL.start();
					Arrays.sort(wave1SurfSelections);
					int option = 0;
					double combinedDanger = 0;
					int bestOption = 0;
					double bestCombinedDanger = Double.MAX_VALUE;
					do {
						if (wave1SurfSelections[option].isSet()) {
							long tuh = surfWave.getTimeUntilHit(
									wave1SurfSelections[option].getFinalDriveState().getX(), 
									wave1SurfSelections[option].getFinalDriveState().getY(), time);
							wave2SurfSelections[0].clear();
							surfSelector.updateSurfSelection(wave2SurfSelections, secondWave, 
									predictor, wave1SurfSelections[option].getFinalDriveState(), 
									opponentSnapshot, time+tuh);
							if (wave2SurfSelections[0].isSet()) {
								long wave2tuh = secondWave.getTimeUntilHit(
										wave2SurfSelections[0].getFinalDriveState().getX(), 
										wave2SurfSelections[0].getFinalDriveState().getY(), time);
								double wave1BulletDamage = RCPhysics.getBulletDamage(surfWave.getBulletPower());
								double wave1Danger = wave1SurfSelections[option].getDanger() * (wave1BulletDamage / tuh);
								double wave2BulletDamage = RCPhysics.getBulletDamage(secondWave.getBulletPower());
								double wave2Danger = wave2SurfSelections[0].getDanger() * (wave2BulletDamage / wave2tuh);
								combinedDanger = wave1Danger + wave2Danger;
								if (combinedDanger < bestCombinedDanger) {
									bestCombinedDanger = combinedDanger;
									bestOption = option;
								}
							}
						}
						option++;
					} while (option < wave1SurfSelections.length 
							&& bestCombinedDanger > wave1SurfSelections[option].getDanger());
					cTargetFactorAngle = wave1SurfSelections[bestOption].getFactorAngle();
					cTargetHeading = wave1SurfSelections[bestOption].getHeading();
					optionCounts[bestOption]++;
//					secondWaveRTL.stop();
//					log.debug("Wave option choices: " + Logger.format(optionCounts));
				}
			}
			// setup distance checking variables
			this.oppSurfX = opponentSnapshot.getX();
			this.oppSurfY = opponentSnapshot.getY();
			this.oppSurfXYShift = opponentSnapshot.getXYShift();
			this.oppSurfXYShift[0] /= 2d;
			this.oppSurfXYShift[1] /= 2d;
			// update surf wave variables
			lastSurfWave = surfWave;
			surfWaveUpdated = false;
		}
		if (surfWave == null) {
			// TODO: figure out what to do when there is no wave to surf
			driveController.drive(robotProxy.getBackAsFrontHeadingDegrees(), 0);
		} else {
			double targetSpeed = RCPhysics.MAX_SPEED;
			if (predictor.shouldStop(surfWave, cTargetFactorAngle, myDriveState, cTargetHeading, targetSpeed, time)) {
				targetSpeed = 0;
			}
			driveController.drive(cTargetHeading, targetSpeed);
		}
	}
		
	@Override
	public void oppWaveUpdated(Wave wave) {
		if (wave == surfWave) {
			this.surfWaveUpdated = true;
		}
	}

	@Override
	public void oppWaveCreated(Wave wave) {
		// no action required
	}

	@Override
	public void oppWaveHitBullet(Wave wave, Bullet oppBullet) {
		// surf wave will get handled by oppNextWaveToHit event
		// however, if surfing multiple waves, the second wave
		// may have been eliminated, so we need to force an update.
		if (surfMultipleWaves) {
			this.surfWaveUpdated = true;
		}
	}

	@Override
	public void oppWaveHit(Wave wave) {
		// no action required
	}

	@Override
	public void oppNextWaveToHit(Wave wave) {
		this.surfWave = wave;	
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
	public void oppWaveDestroyed(Wave wave) {
		// no action required
	}	
}
