package xander.gfws.data;

import java.text.NumberFormat;

public class AgeRollSquaredFunction implements AgeRollFunction {

	private long dropOffAge;
	
	public static void main(String[] args) {
		int[] t = new int[] {0, 1000, 2000, 3000, 4000, 5000, 6000};
		NumberFormat nf = NumberFormat.getNumberInstance();
		nf.setMaximumFractionDigits(5);
		for (int i=0; i<t.length; i++) {
			double y = 1 / Math.pow((2.162d * t[i]) / 3000d + 1, 2);
			System.out.println(t[i] + " -> " + nf.format(y));
		}
	}
	
	public AgeRollSquaredFunction(long dropOffAge) {
		this.dropOffAge = dropOffAge;
	}
	
	public void setDropOffAge(long dropOffAge) {
		this.dropOffAge = dropOffAge;
	}
	
	@Override
	public double getMultiplier(long age) {
		// try dropping off to 10% by the dropOffAge -- the equation rougly approximates this
		return 1 / Math.pow((2.162d * age) / (double)dropOffAge + 1, 2);
//		if (age >= dropOffAge) {
//			return 0;
//		} else {
//			return Math.pow((double)(dropOffAge-age) / (double)dropOffAge, 2);
//		}
	}
}
