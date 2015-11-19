package xander.gfws.modifier;

import xander.core.log.Log;
import xander.core.log.Logger;
import xander.core.math.Linear;
import xander.core.math.LinearIntercept;
import xander.core.math.RCMath;
import xander.core.track.Wave;
import xander.gfws.distributer.WaveDistributer;
import xander.gfws.distributer.WeightDistributer;

public class LinearFactorArrayModifier extends AbstractFactorArrayModifier {

	private static final Log log = Logger.getLog(LinearFactorArrayModifier.class);
	
	public LinearFactorArrayModifier(double weight) {
		this(new WaveDistributer(), weight);
	}
	
	public LinearFactorArrayModifier(WeightDistributer weightDistributer, double weight) {
		super(weightDistributer, weight);
	}

	@Override
	protected double getModifyFactorAngle(double[] array, Wave wave) {
		LinearIntercept vv = Linear.calculateTrajectory(
				wave.getInitialDefenderSnapshot(), 
				wave.getOriginX(), wave.getOriginY(), wave.getBulletVelocity(), 
				robotProxy.getBattleFieldSize(), wave.getOriginTime());
		if (vv == null) {
			log.warn("Unable to calculate linear trajectory for bullet.");
			// fall back to modifying at zero
			return 0;
		} else {
			double factorAngle = RCMath.getTurnAngle(wave.getInitialDefenderBearing(), vv.getVelocityVector().getRoboAngle());
			return factorAngle;
		}
	}
}
