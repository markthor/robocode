package mikkels;

import java.io.File;
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
		
		/*File currentDirFile = new File(".");
		String helper = currentDirFile.getAbsolutePath();
		String dir = helper.substring(0,helper.length() - 1) + "bin\\";
		File robotPath = new File(dir);
		
		RobocodeEngine engine = new RobocodeEngine(robotPath);*/
		RobocodeEngine engine = new RobocodeEngine();
		
		
		
		String s = System.getProperty("user.home");
		File f = RobocodeEngine.getRobotsDir();
		RobotSpecification[] allBots = engine.getLocalRepository();
		RobotSpecification[] bots = engine.getLocalRepository("sample.SittingDuck, sample.TrackFire");
		BattlefieldSpecification battleSpec = new BattlefieldSpecification();
		
		BattleSpecification bs = new BattleSpecification(10, battleSpec, bots);
		
		engine.runBattle(bs);
	}
}
