package com.itu.mma.robocode.controller;


public interface RobocodeController {
	
	/**
	 * Run a game with the given network against the given enemy
	 * String format "classname(s).botName" case-sensitive
	 * @param String The bot to test
	 * @param String The robot to battle against.
	 * @return A battle listener to get the results
	 */
	public BattleListener runGame(String testBot, String enemy);
	
}