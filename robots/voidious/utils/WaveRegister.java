package voidious.utils;

/**
 * WaveRegister
 *
 * This code is open source, released under the RoboWiki Public Code License:
 * http://robowiki.net/cgi-bin/robowiki?RWPCL
 */

public interface WaveRegister {
    public void registerWaveHit(BotScan scan, BotScan enemyScan,
        double power, double factor, int factorIndex, int windowIndexLow,
        int windowIndexHigh, boolean isFiringWave, long fireTime,
        int orientation, double escapeAngleRange);
}
