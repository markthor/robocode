package voidious.utils;

/**
 * WaveIndexSet
 *
 * A set of indexes for a given scan for a given stat buffer. Will allow
 * us to only pass this object around instead of a dozen segmentation
 * indices, and also make it easier to cache the indices.
 */

public class WaveIndexSet {
    public int latVelIndex;
    public int advVelIndex;
    public int distanceIndex;
    public int bulletTimeIndex;
    public int accelIndex;
    public int wallDistanceIndex;
    public int wallReverseIndex;
    public int vChangeIndex;
    public int sinceMaxIndex;
    public int sinceZeroIndex;
    public int dletIndex;
    public int dlftIndex;
    public int dltftIndex;
/*
    public int ehIndex;
    public int sIndex;
    public int asIndex;
    public int trIndex;
    public int shtIndex;
    public int smstIndex;
    public int enIndex;
    public int hletIndex;
*/
    public WaveIndexSet() { }
}

