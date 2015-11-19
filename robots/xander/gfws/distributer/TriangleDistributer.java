package xander.gfws.distributer;

import xander.core.drive.Direction;
import xander.gfws.BasicFactorArrays;

public class TriangleDistributer implements WeightDistributer {

	private double wp = 1d;
	
	public TriangleDistributer() {
	}
	
	/**
	 * Constructs a new triangle distributer using the provided width multiplier.
	 * Width multiplier changes the width of the base of the triangle. 
	 * 
	 * @param widthMultiplier
	 */
	public TriangleDistributer(double widthMultiplier) {
		this.wp = widthMultiplier;
	}
	
	@Override
	public double addWeight(double[] array, double preciseIndex, double weight,
			double distance, double bulletVelocity, Direction surfDirection) {
		double f1 = preciseIndex;
		double indexWidth = wp*BasicFactorArrays.getEstimatedFactorIndexRobotWidth(array.length, distance, bulletVelocity);
		double f0 = preciseIndex - (indexWidth/2d);
		double d = f1 - f0; // half width of sig triangle
		double f2 = f1 + d; // rightmost point of sig triangle
		double h = weight/d;  // height of sig triangle
		double m0 = h/d;  // leading linear equation slope
		double m2 = -h/d; // trailing linear equation slope
		int si = (int) Math.round(Math.floor(f0)); // index where sig triangle starts
		int ei = (int) Math.round(Math.floor(f2)); // index where sig triangle ends
		int mi = (int) Math.round(Math.floor(f1)); // index where sig triangle is at it's peak (primary division)
		double b0 = -m0*f0;  // leading linear equation b value
		double b2 = -m2*f2;  // trailing linear equation b value
		double rsum = 0;  // makes it a little easier to figure the middle segment
		double vsum = 0;  // need to return actual fill added after throwing away any edges
		for (int i=si; i<=ei; i++) {
			if (i != mi) {
				double x0 = Math.max(f0,i);
				double x1 = Math.min(i+1,f2);
				double m = (i<mi)? m0 : m2;
				double b = (i<mi)? b0 : b2;
				double y0 = m*x0 + b;
				double y1 = m*x1 + b;
				double a = ((y0+y1)/2d)*(x1-x0);  //area of sig segment to add to this division
				double r = a/weight; //ratio of total sig to add to this division
				if (i > 0 && i < array.length) {  // throw away anything off the edges
					array[i] += r*weight;
					vsum += r*weight;
				}
				rsum += r;
			}
		}
		if (mi >= 0 && mi < array.length) {
			array[mi] += (1-Math.abs(rsum))*weight; // abs for when we are removing v
			vsum += (1-Math.abs(rsum))*weight;
		}
		return vsum;
	}

}
