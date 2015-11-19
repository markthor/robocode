package mikkels;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;

import robocode.*;
import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.RobotSpecification;

public class runner {
	public static void main(String[] args) {
		runBattle();
	}
	
	public static void runBattle(){
		File currentDirFile = new File(".");
		String helper = currentDirFile.getAbsolutePath();
		String dir = helper.substring(0,helper.length() - 1) + "robots\\";
		File robotPath = new File(dir);
		
		String[] ss = robotPath.list();
		File[] s1 = robotPath.listFiles();
		String[] s2 = robotPath.list(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return true;
			}
		});
		File[] s3 = robotPath.listFiles(new FileFilter() {
			
			@Override
			public boolean accept(File pathname) {
				return true;
			}
		});
		File[] s5 = robotPath.listFiles(new FilenameFilter() {
			
			@Override
			public boolean accept(File dir, String name) {
				return true;
			}
		});
		
		/*File currentDirFile = new File(".");
		String helper = currentDirFile.getAbsolutePath();
		String dir = helper.substring(0,helper.length() - 1) + "bin\\";
		File robotPath = new File(dir);
		
		RobocodeEngine engine = new RobocodeEngine(robotPath);*/
		RobocodeEngine engine = new RobocodeEngine();
		
		
		
		String s = System.getProperty("user.home");
		File f = RobocodeEngine.getRobotsDir();
		RobotSpecification[] allBots = engine.getLocalRepository();
		RobotSpecification[] bots = engine.getLocalRepository("kms.LightningStrike, sample.TrackFire");
		BattlefieldSpecification battleSpec = new BattlefieldSpecification();
		
		BattleSpecification bs = new BattleSpecification(10, battleSpec, bots);
		
		engine.runBattle(bs);
	}
}
