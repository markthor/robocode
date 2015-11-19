package control;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.anji.integration.Activator;

import robocode.Robot;
import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.RobotSpecification;

public class Runner {
	List<Activator> networks = new ArrayList<Activator>();
	
	/**
	 * Runs a battle with all the bots in the directory
	 */
	public static void runBattle(Activator network, String testBot, String enemy){
		File robotsDir = new File("");
		RobocodeEngine engine = new RobocodeEngine(robotsDir);
		
		//File f = RobocodeEngine.getRobotsDir();
		RobotSpecification[] bots = engine.getLocalRepository();
		BattlefieldSpecification battleSpec = new BattlefieldSpecification();
		
		BattleSpecification bs = new BattleSpecification(10, battleSpec, bots);
		
		engine.runBattle(bs);
	}
	
	public static void setResult(){
		
	}
}
