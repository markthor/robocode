package xander.gfws.modifier;

import java.util.HashMap;
import java.util.Map;

import xander.core.Resources;
import xander.core.RobotProxy;
import xander.core.math.RCMath;
import xander.core.track.Snapshot;
import xander.core.track.SnapshotHistory;
import xander.core.track.Wave;
import xander.gfws.FactorIndexer;
import xander.gfws.distributer.WeightDistributer;

public abstract class AbstractFactorArrayModifier implements FactorArrayModifier {

	protected RobotProxy robotProxy;
	private WeightDistributer weightDistributer;
	private SnapshotHistory snapshotHistory;
	private double weight;
	private Map<Wave, Double> indicies = new HashMap<Wave, Double>();
	int arraySize;
	
	public AbstractFactorArrayModifier(WeightDistributer weightDistributer, double weight) {
		this.weightDistributer = weightDistributer;
		this.weight = weight;
		this.robotProxy = Resources.getRobotProxy();
		this.snapshotHistory = Resources.getSnapshotHistory();
	}

	public void setWeightDistributer(WeightDistributer weightDistributer) {
		this.weightDistributer = weightDistributer;
	}
	
	/**
	 * Returns precise index of where to add weight.  If weight should not be added,
	 * a negative value should be returned.
	 * 
	 * Returned array should have a second dimension of size 2, with the first index
	 * being the precise modify index, and the second index being a weighted value 
	 * from 0 to 1 that will be multiplied by the base weight when applied to the factor array. 
	 * 
	 * @param array       array
	 * @param wave        bullet wave
	 * 
	 * @return            precise index of where to add weight in the array (negative value if no weight should be added)
	 */
	protected abstract double getModifyFactorAngle(double[] array, Wave wave);
	
	@Override
	public void modify(double[] array, Wave wave) {
		arraySize = array.length;
		FactorIndexer factorIndexer = wave.getFactorIndexer();
		double factorAngle = getModifyFactorAngle(array, wave);
		double preciseIndex = factorIndexer.getPreciseFactorIndex(
					factorAngle, array.length, wave.getSurfDirection(), wave.getInitialMEA());
		if (preciseIndex >= 0) {
			indicies.put(wave, Double.valueOf(preciseIndex));
			Snapshot defender = snapshotHistory.getSnapshot(wave.getInitialDefenderSnapshot().getName());
			double distance = RCMath.getDistanceBetweenPoints(
					wave.getOriginX(), wave.getOriginY(), 
					defender.getX(), defender.getY());
			weightDistributer.addWeight(array, preciseIndex, weight, distance, wave.getBulletVelocity(), wave.getSurfDirection());
		}
	}
}
