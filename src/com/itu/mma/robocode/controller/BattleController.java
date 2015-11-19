package com.itu.mma.robocode.controller;

import java.io.File;

import mikkels.BattleListener;
import robocode.control.BattleSpecification;
import robocode.control.BattlefieldSpecification;
import robocode.control.RobocodeEngine;
import robocode.control.RobotSpecification;
import robocode.control.events.IBattleListener;

public class BattleController implements RobocodeController {

	@Override
	public IBattleListener runGame(String testBot, String enemy) {
		BattleListener bl = new BattleListener();
		
		RobocodeEngine engine = new RobocodeEngine();
		engine.addBattleListener(bl);
		
		RobotSpecification[] bots = engine.getLocalRepository(testBot + ", " + enemy);
		BattlefieldSpecification battleSpec = new BattlefieldSpecification();
		
		BattleSpecification bs = new BattleSpecification(10, battleSpec, bots);
		
		engine.runBattle(bs);
		
		return bl;
	}
}
