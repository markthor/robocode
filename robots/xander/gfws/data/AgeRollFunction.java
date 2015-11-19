package xander.gfws.data;

public interface AgeRollFunction {

	/**
	 * Returns a multiplier that a data point weight will be multiplied by.  
	 * Generally this should return a value in range 0 to 1.
	 * 
	 * @param age    age of data point in ticks
	 * 
	 * @return       multiplier for data point weight based on age
	 */
	public double getMultiplier(long age);
}
