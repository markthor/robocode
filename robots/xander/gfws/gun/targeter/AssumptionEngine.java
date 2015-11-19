package xander.gfws.gun.targeter;

import xander.core.drive.OrbitalDrivePredictor;
import xander.core.track.Snapshot;
import xander.core.track.Wave;
import xander.gfws.FactorRange;

public interface AssumptionEngine {

	public void applyToFactorArray(Wave wave, FactorRange rfr, double[] factorArray, 
			Snapshot myself, Snapshot target, OrbitalDrivePredictor orbitalDriver);
}
