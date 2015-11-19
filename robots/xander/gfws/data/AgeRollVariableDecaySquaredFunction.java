package xander.gfws.data;

import xander.core.Resources;

public class AgeRollVariableDecaySquaredFunction implements AgeRollFunction {

	private double timeMultiplier = 1;
	
	public AgeRollVariableDecaySquaredFunction() {
	}
	
	public AgeRollVariableDecaySquaredFunction(double timeMultiplier) {
		this.timeMultiplier = timeMultiplier;
	}
	
	public void setTimeMultiplier(double timeMultiplier) {
		this.timeMultiplier = timeMultiplier;
	}
	
	@Override
	public double getMultiplier(long age) {
		double dropOffAge = Resources.getCumulativeTime() * timeMultiplier;
		if (age >= dropOffAge) {
			return 0;
		} else {
			return Math.pow((double)(dropOffAge-age) / (double)dropOffAge, 2);
		}
	}

}
