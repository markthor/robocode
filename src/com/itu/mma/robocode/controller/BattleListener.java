package com.itu.mma.robocode.controller;

import control.Runner;
import robocode.control.events.BattleCompletedEvent;
import robocode.control.events.BattleErrorEvent;
import robocode.control.events.BattleFinishedEvent;
import robocode.control.events.BattleMessageEvent;
import robocode.control.events.BattlePausedEvent;
import robocode.control.events.BattleResumedEvent;
import robocode.control.events.BattleStartedEvent;
import robocode.control.events.IBattleListener;
import robocode.control.events.RoundEndedEvent;
import robocode.control.events.RoundStartedEvent;
import robocode.control.events.TurnEndedEvent;
import robocode.control.events.TurnStartedEvent;

public class BattleListener implements IBattleListener {
	private int fitness;
	private boolean finished = false;
	
	
	@Override
	public void onBattleCompleted(BattleCompletedEvent arg0) {
		//Save results
		finished  = true;
		fitness = arg0.getIndexedResults()[0].getScore();
		notifyAll();
	}

	@Override
	public void onBattleError(BattleErrorEvent e) {
		System.out.println(e.getError());
	}
	
	public int getFitness() {
		return fitness;
	}
	
	public boolean isFinished() {
		return finished;
	}

	@Override
	public void onBattleFinished(BattleFinishedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBattleMessage(BattleMessageEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBattlePaused(BattlePausedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBattleResumed(BattleResumedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onBattleStarted(BattleStartedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRoundEnded(RoundEndedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onRoundStarted(RoundStartedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTurnEnded(TurnEndedEvent arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onTurnStarted(TurnStartedEvent arg0) {
		// TODO Auto-generated method stub

	}




}
