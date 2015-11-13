package com.itu.mma.robocode.controller;

import robocode.Robot;

import com.anji.integration.Activator;

public interface RobocodeController {
	
	/**
	 * Run a game with the given network against the given enemy
	 * @param network The network to use
	 * @param enemy The robot to battle against
	 * @return The score achieved against the given enemy
	 */
	public int runGame(Activator network, Robot enemy);
	
}