package com.itu.mma.robocode.controller;

import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.RobotSpecification;

public class BattleController implements RobocodeController {

	private static RobocodeEngine _engine;
	
	@Override
	public BattleListener runGame(String testBot, String enemy) {
		BattleListener bl = new BattleListener();
		RobocodeEngine engine = getEngine();
		
		engine.addBattleListener(bl);
		RobotSpecification[] allBots = engine.getLocalRepository();
		RobotSpecification[] bots = engine.getLocalRepository(testBot + ", " + enemy);
		
		//Correct battle spec?
		BattlefieldSpecification battleSpec = new BattlefieldSpecification();
		BattleSpecification bs = new BattleSpecification(10, battleSpec, bots);
		
		engine.runBattle(bs);
		
		return bl;
	}

	private static RobocodeEngine getEngine() {
		if(_engine == null){
			_engine = new RobocodeEngine();
		}
		
		return _engine;
	}
}
