package xander.gfws.drive;

import xander.core.drive.DirectDrivePredictor;
import xander.core.drive.DriveState;
import xander.core.track.Snapshot;
import xander.core.track.Wave;

public interface DirectSurfSelector {
	
	public void updateSurfSelection(DirectSurfSelection[] surfSelections, 
			Wave surfWave, DirectDrivePredictor predictor, DriveState myDriveState, 
			Snapshot currentOpponentSnapshot, long fromTime);
}
