package xander.gfws.data;


import xander.core.track.Wave;

public class KNNLogReader extends CachingLogReader {

	private AbstractWaveLogger<KNNParms> waveLogger;
	private KNNParms parms;
	
	public KNNLogReader(AbstractWaveLogger<KNNParms> waveLogger, KNNParms parms) {
		this.waveLogger = waveLogger;
		this.parms = parms;
	}
	
	@Override
	protected WaveSearchIterable<DataPoint> readHitData(Wave wave) {
		if (parms.isSelectByHitsWithinKNNVisits()) {
			WaveSearchIterable<DataPoint> visits = waveLogger.getVisitData(wave, parms);
			parms.visitDistance = visits.getMaxDistance();
		}
		return waveLogger.getHitData(wave, parms);
	}

	@Override
	protected WaveSearchIterable<DataPoint> readVisitData(Wave wave) {
		return waveLogger.getVisitData(wave, parms);
	}
}
