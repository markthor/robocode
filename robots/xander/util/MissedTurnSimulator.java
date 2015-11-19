package xander.util;

import xander.core.Resources;
import xander.core.RobotProxy;

public class MissedTurnSimulator {

	private RobotProxy robotProxy;
	private long[][] missedTurnRanges;
	
	public MissedTurnSimulator(long[][] ranges) {
		this.robotProxy = Resources.getRobotProxy();
		this.missedTurnRanges = ranges;
	}
	
	public void setMissedTurnRanges(long[][] ranges) {
		this.missedTurnRanges = ranges;
	}
	
	public boolean isTurnToBeMissed() {
		long time = robotProxy.getTime();
		for (int i=0; i<missedTurnRanges.length; i++) {
			if (time >= missedTurnRanges[i][0] && time <= missedTurnRanges[i][1]) {
				return true;
			}
		}
		return false;
	}
}
