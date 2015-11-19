package cs.move.extra;

import static cs.utils.FastMath.abs;
import static cs.utils.FastMath.limit;
import static cs.utils.FastMath.min;

public class Data {
	public long globalTime;

	public double distance;
	public double lateralVelocity;
	public double advancingVelcity;
	public double bulletFlightTime;
	public double wallForward;
	public double wallReverse;
	public double distLast10;
	public long timeSinceDirectionChange;

	public double guessfactor;

	public double weight = 1.0;

	public double[] getDataArray() {
		return new double[] {
				abs(lateralVelocity)/8.0,
				(advancingVelcity+8.0)/16.0,
				distLast10/(8*10),
				limit(0,abs(wallForward)/Math.PI*0.5,1),
				limit(0,abs(wallReverse)/Math.PI,1),
				min(distance/800,1),
				min(timeSinceDirectionChange/400.0,1)
				//1.0/(1.0+timeSinceDirectionChange/bulletFlightTime),
		};
	}

	public double[] getDataWeights() {
		return new double[] {
				6,
				1,
				3.2,
				4,
				2,
				1,
				2.2,
		};
	}

	public double[] getWeightedDataArray() {
		double[] data = getDataArray();
		double[] weights = getDataWeights();
		for(int i=0;i<data.length;++i) {
			data[i] *= weights[i];
		}
		return data;
	}
}
