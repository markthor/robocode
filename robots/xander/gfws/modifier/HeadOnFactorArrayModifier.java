package xander.gfws.modifier;

import xander.core.track.Wave;
import xander.gfws.distributer.WaveDistributer;
import xander.gfws.distributer.WeightDistributer;

public class HeadOnFactorArrayModifier extends AbstractFactorArrayModifier {

	public HeadOnFactorArrayModifier(WeightDistributer weightDistributer,
			double weight) {
		super(weightDistributer, weight);
	}

	public HeadOnFactorArrayModifier(double weight) {
		this(new WaveDistributer(), weight);
	}
	
	@Override
	protected double getModifyFactorAngle(double[] array, Wave wave) {
		return 0;
	}
}
