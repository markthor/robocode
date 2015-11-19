package com.itu.mma.robocode.controller;

import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.RobotSpecification;
import robocode.control.events.IBattleListener;

public class BattleController implements RobocodeController {

	@Override
	public BattleListener runGame(String testBot, String enemy) {
		BattleListener bl = new BattleListener();
		
		RobocodeEngine engine = new RobocodeEngine();
		engine.addBattleListener(bl);
		
		RobotSpecification[] bots = engine.getLocalRepository(testBot + ", " + enemy);
		
		//Correct battle spec?
		BattlefieldSpecification battleSpec = new BattlefieldSpecification();
		BattleSpecification bs = new BattleSpecification(10, battleSpec, bots);
		
		engine.runBattle(bs);
		
		return bl;
	}
}
