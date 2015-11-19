package xander.gfws.gun.targeter;

import java.awt.geom.Path2D;

import xander.core.Resources;
import xander.core.drive.Direction;
import xander.core.drive.DistancingEquation;
import xander.core.drive.DriveBoundsFactory;
import xander.core.drive.OrbitalDrivePredictor;
import xander.core.gun.targeter.Targeter;
import xander.core.math.RCMath;
import xander.core.track.Snapshot;
import xander.core.track.Wave;
import xander.gfws.BasicFactorArrays;
import xander.gfws.FactorRange;
import xander.gfws.OrbitalFactorArrays;
import xander.gfws.processor.FactorArrayProcessor;

public class GuessFactorTargeter implements Targeter {

	private FactorArrayProcessor fap;
	private OrbitalDrivePredictor orbitalDriver;
	private AssumptionEngine assumptionEngine;
	
	public GuessFactorTargeter(FactorArrayProcessor fap) {
		this.fap = fap;
		Path2D.Double driveBounds = DriveBoundsFactory.getRectangularBounds(Resources.getRobotProxy().getBattleFieldSize());
		this.orbitalDriver = new OrbitalDrivePredictor(driveBounds);
	}
	
	public void setAssumptionEngine(AssumptionEngine assumptionEngine) {
		this.assumptionEngine = assumptionEngine;
	}
	
	@Override
	public String getTargetingType() {
		return "Guess Factor";
	}

	@Override
	public boolean canAimAt(Snapshot target) {
		return true;
	}

	@Override
	public double getAim(Snapshot target, Snapshot myself,
			Wave wave) {
		FactorRange rfr = OrbitalFactorArrays.getReachableFactorRange(
				wave, myself, target, myself.getTime(), fap.getFactors(), false, 
				DistancingEquation.NO_ADJUST, orbitalDriver, null);
		double[] factorArray = fap.getFactorArray(wave, rfr.getMaxCounterClockwiseFactorAngle(), rfr.getMaxClockwiseFactorAngle());
		if (assumptionEngine != null) {
			assumptionEngine.applyToFactorArray(wave, rfr, factorArray, myself, target, orbitalDriver);
		}
		int factorIndex = BasicFactorArrays.getMostWeightedFactorIndex(
				factorArray, rfr.getBeginIndex(), rfr.getEndIndex());
		Direction surfDirection = OrbitalDrivePredictor.getOribitalDirection(
				myself.getX(), myself.getY(), target.getX(), target.getY(), target.getVelocity(), target.getHeadingRoboDegrees());
		double factorAngle = Resources.getOpponentFactorIndexer().getFactorAngle(factorIndex, factorArray.length, surfDirection, wave.getInitialMEA());
		double zeroAngle = RCMath.getRobocodeAngle(myself.getX(), myself.getY(), target.getX(), target.getY());
		double bestTurnAngle = RCMath.normalizeDegrees(zeroAngle + factorAngle);
		return bestTurnAngle;
	}
}
