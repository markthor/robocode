package xander.gfws.data;

public class AgeRollLinearFunction implements AgeRollFunction {

	private double decayPerTick;
	
	public AgeRollLinearFunction(double decayPerTick) {
		this.decayPerTick = decayPerTick;
	}
	
	public void setDecayPerTick(double decayPerTick) {
		this.decayPerTick = decayPerTick;
	}
	
	@Override
	public double getMultiplier(long age) {
		return Math.max(0, 1 - decayPerTick * age);
	}
}
