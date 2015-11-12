package mikkels;

import java.io.File;

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
		RobocodeEngine engine = new RobocodeEngine();
		
		File f = RobocodeEngine.getRobotsDir();
		RobotSpecification[] bots = engine.getLocalRepository();
		BattlefieldSpecification battleSpec = new BattlefieldSpecification();
		
		BattleSpecification bs = new BattleSpecification(10, battleSpec, bots);
		
		engine.runBattle(bs);
	}
}
