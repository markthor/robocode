package xander.gfws.distributer;

import xander.core.drive.Direction;

public class WaveDistributer implements WeightDistributer {

	@Override
	public double addWeight(double[] array, double preciseIndex, double weight,
			double distance, double bulletVelocity, Direction surfDirection) {
		double waveHeight = weight / 3d;
		double sum = 0;
		for (int i=0; i<array.length; i++) {
			double weightAtIndex = waveHeight / (Math.pow(preciseIndex - i - 0.5, 2) + 1);
			array[i] += weightAtIndex;
			sum += weightAtIndex;
		}
		return sum;
	}

}
