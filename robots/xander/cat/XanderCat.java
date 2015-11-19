package xander.cat;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Set;

import robocode.WinEvent;

import xander.cat.drive.GreenBeeSurfSelector;
import xander.cat.drive.HighPerformanceDriveSelector;
import xander.cat.drive.IdealPositionDrive;
import xander.cat.drive.SmartDistancingEquation;
import xander.cat.gfws.AutoVaryWeightByAge;
import xander.cat.gfws.segment.DefenderAccelerationAndSpeedSegmenter;
import xander.cat.group.mirror.MirrorFactory;
import xander.cat.group.ram.RamFactory;
import xander.cat.group.rem.REMFactory;
import xander.cat.group.shield.BulletShieldingAutoFireCondition;
import xander.cat.group.shield.BulletShieldingController;
import xander.cat.group.shield.BulletShieldingFactory;
import xander.cat.gun.BSProtectedGun;
import xander.cat.gun.power.SteppedHitRatioPowerSelector;
import xander.cat.gun.targeter.JPCircularTargeter;
import xander.cat.processor.TargetingDetectorFactorArrayProcessor;
import xander.cat.radar.BasicRadar;
import xander.cat.scenario.CircularDriveScenario;
import xander.cat.scenario.NoOpponentWavesScenario;
import xander.core.AbstractXanderRobot;
import xander.core.ComponentChain;
import xander.core.Configuration;
import xander.core.Resources;
import xander.core.RobotStyle;
import xander.core.Scenario;
import xander.core.drive.Drive;
import xander.core.drive.DriveArray;
import xander.core.drive.DriveBoundsFactory;
import xander.core.drive.DriveSelector;
import xander.core.gun.Gun;
import xander.core.gun.GunArray;
import xander.core.gun.VirtualHitRatioGunSelector;
import xander.core.gun.XanderGun;
import xander.core.gun.detect.HeadOnDetector;
import xander.core.gun.detect.LinearDetector;
import xander.core.gun.detect.TargeterTargetingDetector;
import xander.core.gun.power.PowerSelector;
import xander.core.gun.targeter.CircularTargeter;
import xander.core.io.BattleStats;
import xander.core.math.RCMath;
import xander.core.math.RCPhysics;
import xander.core.track.DriveStats;
import xander.core.track.GunStats;
import xander.gfws.data.AgeRollLinearFunction;
import xander.gfws.data.DataPointFactorArrayProcessor;
import xander.gfws.data.FactorArrayParms;
import xander.gfws.data.KDTreeWaveLogger;
import xander.gfws.data.KNNLogReader;
import xander.gfws.data.KNNParms;
import xander.gfws.data.red.RedKDTreeWaveLogger;
import xander.gfws.drive.DirectWaveSurfingDrive;
import xander.gfws.gun.targeter.GuessFactorTargeter;
import xander.gfws.segment.AttackerBearingSegmenter;
import xander.gfws.segment.BulletTravelTimeSegmenter;
import xander.gfws.segment.DefenderAccelerationSegmenter;
import xander.gfws.segment.LateralVelocitySegmenter;
import xander.gfws.segment.WallStickSegmenter;

/**
 * Robocode advanced robot and flagship of the Xander robot fleet.
 * XanderCat is a multi-mode robot built on the Xander 2.0 framework.
 * 
 * @author Scott Arnold
 */
public class XanderCat extends AbstractXanderRobot {

	private static DataPointFactorArrayProcessor driveFAP;
	private static CircularDriveScenario circularDriverScenario;
	private static SteppedHitRatioPowerSelector steppedPowerSelector;
	private static PowerSelector mainPowerSelector;
	private static Path2D.Double driveBounds;
	private static boolean[] wins;
	private static BulletShieldingController bsc;
	
	@Override
	protected void style(RobotStyle robotStyle) {
		robotStyle.setColors(Color.RED, Color.BLACK, Color.RED);  // radar color changed to red at Alexander's request :)
		robotStyle.setBulletColor(Color.PINK);
		robotStyle.setScanArcColor(Color.GREEN);
	}

	@Override
	protected void configure(Configuration configuration) {
		configuration.setAutoFireOnDisabledOpponents(true);  
		configuration.addAutoFireCondition(new BulletShieldingAutoFireCondition());
		configuration.setLogComponentRunTimes(true);
		configuration.setLogDriveTimes(true);
		configuration.setSnapshotHistorySize(120);
		Rectangle2D.Double dbRec = new Rectangle2D.Double(60, 10, getBattleFieldSize().width-120, getBattleFieldSize().height-20); // being lazy here and assuming 800 x 600
		driveBounds = DriveBoundsFactory.getSmoothedRectangleBounds(dbRec, -0.105, 0.4);
		Path2D.Double oppDriveBounds = DriveBoundsFactory.getRectangularBounds(getBattleFieldSize());
		configuration.setUsePreciseMEAForMyWaves(true, oppDriveBounds);
		PowerSelector powerSelector = configuration.getDisabledOpponentPowerSelector();
		powerSelector = REMFactory.getX5PowerSelector(powerSelector);
		configuration.setDisabledOpponentPowerSelector(powerSelector);
		steppedPowerSelector = new SteppedHitRatioPowerSelector(
				new double[] {0.17}, new double[] {1.85, 1.99});
		steppedPowerSelector.setPowerDrop(0.135, 15, 0);
		mainPowerSelector = REMFactory.getX5PowerSelector(steppedPowerSelector);
		wins = new boolean[getNumRounds()]; 
	}
	
	@Override
	protected boolean recordBattleStats(Map<String, String> oppStats, int numBattles) {
		GunStats gunStats = Resources.getGunStats();
		DriveStats driveStats = Resources.getDriveStats();
		double oHR = gunStats.getOverallOpponentHitRatio();
		double mHR = gunStats.getOverallHitRatio();
		double fop = driveFAP.getFlattenerUsePercentage();	
		double cda = circularDriverScenario.getAppliesPercentage();
		double dpc = steppedPowerSelector.getDropPowerCount();
		BattleStats.updateAveragedStatValue(oppStats, "OppHitRatio", oHR, 3, numBattles);
		BattleStats.updateAveragedStatValue(oppStats, "MyHitRatio", mHR, 3, numBattles);
		BattleStats.updateAveragedStatValue(oppStats, "FlattenerPercent", fop, 3, numBattles);
		BattleStats.updateAveragedStatValue(oppStats, "CircularApplies", cda, 3, numBattles);
		BattleStats.updateAveragedStatValue(oppStats, "DropPowerCount", dpc, 1, numBattles);		
		for (int i=0; i<2; i++) {
			int bf = gunStats.getActionedBulletsFired("GF Gun " + i);
			// for the guns, we are only showing numbers for the last battle instead of an average
			oppStats.put("GF"+i, String.valueOf(bf));
		}
		Set<String> driveNames = driveStats.getDriveNames();
		for (String driveName : driveNames) {
			double dup = driveStats.getDriveUsagePercent(driveName);
			BattleStats.updateAveragedStatValue(oppStats, "D:"+driveName, dup, 3, numBattles);
		}
//		String[] loggers = new String[] {"Config", "Construct", "LoadStats", "Radar", "Drive", "Gun"};
//		for (String loggerName : loggers) {
//			RunTimeLogger rtl = RunTimeLogger.getLoggerFor(loggerName);
//			rtl.saveTo(statsmap, totalbattles);
//		}
		return true;
	}

	@Override
	protected boolean recordCommonBattleStats(Map<String, String> commonStats, int totalBattles) {
		for (int i=0; i<wins.length; i++) {
			String roundKey = (i < 10)? "R 0" + i + " Wins" : "R " + i + " Wins";
			int roundWins = RCMath.parseInt(commonStats.get(roundKey), 0);
			if (wins[i]) {
				roundWins++;
			}
			commonStats.put(roundKey, String.valueOf(roundWins));
		}
		double wallHits = Resources.getDriveStats().getWallHits();
		BattleStats.updateAveragedStatValue(commonStats, "Avg Wall Hits", wallHits, 2, totalBattles);
		double avgWallHitDmg = Resources.getDriveStats().getAverageWallHitDamage();
		BattleStats.updateAveragedStatValue(commonStats, "Avg Wall Hit Dmg", avgWallHitDmg, 2, totalBattles);
		double skippedTurns = getSkippedTurns();
		BattleStats.updateAveragedStatValue(commonStats, "Skipped Turns", skippedTurns, 2, totalBattles);
		String fc = bsc.getFireCheckString();
		if (fc != null) {
			commonStats.put("Fire Check", fc);
		}
		return true;
	}

	@Override
	protected void addComponents(ComponentChain chain) {
		
//		FactorArrayPainter wsDriveFAPainter = new FactorArrayPainter("WS Drive FA", "WS Drive", false);
//		FactorArrayPainter tdDriveFAPainter = new FactorArrayPainter("TD Drive FA", "TD Drive", false);
//		tdDriveFAPainter.setPegColor(Color.PINK);
//		XanderPaintManager.getInstance(this).enable(getBattleFieldHeight(), wsDriveFAPainter, tdDriveFAPainter);
//		PaintSource<Path2D.Double> dbSource = new PaintSource<Path2D.Double>("Drive Bounds", driveBounds);
//		XanderPaintManager.getInstance(this).enable(getBattleFieldHeight(), new BulletShieldingPainter());
		
		// RADAR
		
		chain.addDefaultComponents(new BasicRadar(45, 5));
		
		// DRIVES
		
		RamFactory.addRamComponents(chain, 2d, 0.1d, 30);
		
		RamFactory.addAntiRamComponents(chain);
		
		bsc = BulletShieldingFactory.addBulletShieldingComponents(chain, 0, 0d,  true, true);
		
		MirrorFactory.addAntiMirrorComponents(chain, 20, 4, 25);
				
		Scenario ipScenario = new NoOpponentWavesScenario();
		Drive ipDrive = new IdealPositionDrive();
		chain.addComponents(ipScenario, ipDrive);
		
		SmartDistancingEquation de = new SmartDistancingEquation(80, 400, 600, 45, 20);
		de.setMyHitRatioThresholds(0, 0);
		KNNParms driveKNNParms = new KNNParms(10, 40, 0.4);	
		KDTreeWaveLogger driveWaveLogger = new RedKDTreeWaveLogger(false,
				new LateralVelocitySegmenter(),
				new DefenderAccelerationSegmenter(),
				new AttackerBearingSegmenter(),
				new WallStickSegmenter(60, 260, true));	
		FactorArrayParms driveFactorArrayParms = new FactorArrayParms(false);
		driveFactorArrayParms.setVaryWeightBySearchDistance(true, 0.2);
		driveFactorArrayParms.setUseFlattener(true, 0.125, 0.15, 5);
		driveFactorArrayParms.setMinAgeWeightPercent(0.06);
		// note: old values for 11.11 would be 0.09, 0.11, 0.0005, 0.1275, 0.005, RollType.LINEAR
		new AutoVaryWeightByAge(driveFactorArrayParms, 0.115, 0.115, 15000, 0.1275, 2500, 
				AutoVaryWeightByAge.RollType.SQUARED);
		KNNLogReader driveLogReader = new KNNLogReader(driveWaveLogger, driveKNNParms);			
		driveFAP = new DataPointFactorArrayProcessor("WS Drive FA", driveLogReader, 67, driveFactorArrayParms);
		GreenBeeSurfSelector surfSelector = new GreenBeeSurfSelector(driveFAP, de);
		DirectWaveSurfingDrive mainDrive = new DirectWaveSurfingDrive("WS Drive", surfSelector, driveBounds);
		mainDrive.setSurfMultipleWaves(true);	
		
		TargetingDetectorFactorArrayProcessor tdFAP = new TargetingDetectorFactorArrayProcessor("TD Drive FA", 67,
				new HeadOnDetector(false),
				new LinearDetector(false),
				new TargeterTargetingDetector(false, new JPCircularTargeter(1), RCPhysics.ROBOT_WIDTH),
				new TargeterTargetingDetector(false, new JPCircularTargeter(2), RCPhysics.ROBOT_WIDTH));
		GreenBeeSurfSelector tdSurfSelector = new GreenBeeSurfSelector(tdFAP, de);
		DirectWaveSurfingDrive tdDrive = new DirectWaveSurfingDrive("TD Drive", tdSurfSelector, driveBounds);
		tdDrive.setSurfMultipleWaves(true);
		
		DriveSelector driveSelector = new HighPerformanceDriveSelector(mainDrive, 50, 0.08);
		DriveArray driveArray = new DriveArray(driveSelector, mainDrive, tdDrive);		
						
		chain.addDefaultComponents(driveArray);
		
		// GUNS  
		
		// a special scenario just for our circular drivers out there!
		XanderGun circularGun = new XanderGun(new CircularTargeter(), mainPowerSelector);
		circularDriverScenario = new CircularDriveScenario(circularGun);
		chain.addComponents(circularDriverScenario, circularGun);
		
		// main guess factor guns
		KDTreeWaveLogger gunWaveLogger = new RedKDTreeWaveLogger(true,
				new LateralVelocitySegmenter(),
				new BulletTravelTimeSegmenter(getBattleFieldSize()),
				new DefenderAccelerationAndSpeedSegmenter(),
				new AttackerBearingSegmenter(),
				new WallStickSegmenter(60, 260, true),
				new WallStickSegmenter(60, 260, false));	
		KNNParms gunKNNParms = new KNNParms(20, 100, 0.05);
		gunKNNParms.setKnnWhenSelectionEmpty(0);
		KNNLogReader gunLogReader = new KNNLogReader(gunWaveLogger, gunKNNParms);
		double gunRoll = 0;
		Gun[] guns = new Gun[2];
		for (int i=0; i<guns.length; i++) {
			FactorArrayParms gunFactorArrayParms = new FactorArrayParms(true);
			gunFactorArrayParms.setVaryWeightByAgeFunction(new AgeRollLinearFunction(gunRoll));
			DataPointFactorArrayProcessor gunFAP = new DataPointFactorArrayProcessor("Gun FA", gunLogReader, 91, gunFactorArrayParms);
			GuessFactorTargeter targeter = new GuessFactorTargeter(gunFAP);
			XanderGun gun = new XanderGun("GF Gun " + i, targeter, mainPowerSelector);
			gun.setMinEnergyToFire(4.1d);
			gun.setLowEnergyConservationRate(1d/3d);
			guns[i] = new BSProtectedGun(gun);
			gunRoll += 0.015;
		}
		VirtualHitRatioGunSelector gunSelector = new VirtualHitRatioGunSelector();
		gunSelector.setRollingRatioWeight(0.225);
		GunArray gunArray = new GunArray(gunSelector, guns);
		
		chain.addDefaultComponents(gunArray);
	}

	@Override
	public void onWin(WinEvent event) {
		wins[getRoundNum()] = true;
		super.onWin(event);
	}		
}
