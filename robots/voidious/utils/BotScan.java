package voidious.utils;

import java.awt.geom.*;

/**
 * BotScan
 *
 * This class represents a single scan.
 *
 * This code is open source, released under the RoboWiki Public Code License:
 * http://robowiki.net/cgi-bin/robowiki?RWPCL
 */

public class BotScan
{
	private BotScan _inverseScan;
    private double _absBearingRadians;
    private double _bearingRadians;
    private double _headingRadians;
    private double _distance;
    private double _velocity;
    private double _energy;
    private double _turnRateRadians;
    private int _accel;
    private Point2D.Double _location;
    private long _travelTime; // time since their velocity almost hit 0
    private long _vchangeTime; // time since their velocity changed
    private long _sinceHitTime;
    private long _sinceMaxSpeedTime;
    private double _distLastEightTicks;
    private double _distLastFifteenTicks;
    private double _distLastTwentyFiveTicks;
    private long _time;
    private int _orientation = 1;

    private double _forwardRatio = -1.0;
    private double _reverseRatio = -1.0;

    public BotScan(double ab, double b, double h, double d, double v, 
    	double e, double tr, int ac, Point2D.Double p, long tt, long vct, 
    	long sht, long smst, double dlet, double dlft, double dltft, long t) {
    	
    	_absBearingRadians = ab;
    	_bearingRadians = b;
    	_headingRadians = h;
        _distance = d;
        _velocity = v;
        _energy = e;
        _turnRateRadians= tr;
        _accel = ac;
        _location = p;
        _travelTime = tt;
        _vchangeTime = vct;
        _sinceHitTime = sht;
        _sinceMaxSpeedTime = smst;
        _distLastEightTicks = dlet;
        _distLastFifteenTicks = dlft;
        _distLastTwentyFiveTicks = dltft;
        _time = t;
    }

    public BotScan() {    }

    public void setAbsBearingRadians(double b) {
        _absBearingRadians = b;
    }

    public double getAbsBearingRadians() {
        return _absBearingRadians;
    }

    public void setBearingRadians(double b) {
        _bearingRadians = b;
    }

    public double getBearingRadians() {
        return _bearingRadians;
    }

    public void setHeadingRadians(double h) {
        _headingRadians = h;
    }

    public double getHeadingRadians() {
        return _headingRadians;
    }

    public void setDistance(double d) {
        _distance = d;
    }

    public double getDistance() {
        return _distance;
    }

    public void setVelocity(double v) {
        _velocity = v;
    }

    public double getVelocity() {
        return _velocity;
    }

    public void setEnergy(double e) {
        _energy = e;
    }

    public double getEnergy() {
        return _energy;
    }

    public void setTurnRateRadians(double tr) {
        _turnRateRadians = tr;
    }

    public double getTurnRateRadians() {
        return _turnRateRadians;
    }

    public void setAccel(int ac) {
        _accel = ac;
    }

    public int getAccel() {
        return _accel;
    }

    public void setTravelTime(long tt) {
        _travelTime = tt;
    }

    public long getTravelTime() {
        return _travelTime;
    }

    public void setVchangeTime(long vct) {
        _vchangeTime = vct;
    }

    public long getVchangeTime() {
        return _vchangeTime;
    }

    public void setSinceHitTime(long sht) {
        _sinceHitTime = sht;
    }

    public long getSinceHitTime() {
        return _sinceHitTime;
    }

    public void setSinceMaxSpeedTime(long smst) {
        _sinceMaxSpeedTime = smst;
    }

    public long getSinceMaxSpeedTime() {
        return _sinceMaxSpeedTime;
    }

    public void setDistanceLastEightTicks(double dlet) {
        _distLastEightTicks = dlet;
    }

    public double getDistanceLastEightTicks() {
        return _distLastEightTicks;
    }

    public void setDistanceLastFifteenTicks(double dlft) {
        _distLastFifteenTicks = dlft;
    }

    public double getDistanceLastFifteenTicks() {
        return _distLastFifteenTicks;
    }

    public void setDistanceLastTwentyFiveTicks(double dltft) {
        _distLastTwentyFiveTicks = dltft;
    }

    public double getDistanceLastTwentyFiveTicks() {
        return _distLastTwentyFiveTicks;
    }

    public void setTime(long t) {
        _time = t;
    }

    public long getTime() {
        return _time;
    }

    public void setLocation(Point2D.Double l) {
    	_location.setLocation(l);
    }
    
    public Point2D.Double getLocation() {
    	return _location;
    }
    
    public void setOrientation(int i) {
        _orientation = i;
    }

    public int getOrientation() {
        return _orientation;
    }
    
    public void setInverseScan(BotScan es) {
    	_inverseScan =  es;
    }
    
    public BotScan getInverseScan() {
    	return _inverseScan;
    }

    public void printData() {
        System.out.println("Bearing: " + _bearingRadians);
        System.out.println("Heading: " + _headingRadians);
        System.out.println("Distance: " + _distance);
        System.out.println("Velocity: " + _velocity);
        System.out.println("Energy: " + _energy);
        System.out.println("Turn Rate: " + _turnRateRadians);
        System.out.println("Accel: " + _accel);
        System.out.println("Travel Time: " + _travelTime);
        System.out.println("Vchange Time: " + _vchangeTime);
        System.out.println("Since Hit Time: " + _sinceHitTime);
        System.out.println("Since Max Speed Time: " + _sinceMaxSpeedTime);
        System.out.println("Dist last 8 ticks: " + _distLastEightTicks);
        System.out.println("X/Y: (" + _location.x + ", " + _location.y + ")");
        System.out.println("Time: " + _time);
        System.out.println("**********************");
    }

    public double getForwardPositionRatio(Point2D.Double myLocation,
        double power, Rectangle2D.Double fieldRect) {

    	if (_forwardRatio >= 0.0) { return _forwardRatio; }

        double orientation = _orientation;
        double maxAngleRadians = Math.asin(8.0/(20-3.0*power));

        // 1.0 means the max range of orbital movement exactly reaches bounds
        // of battle field
        double outRatio = 2.0;
        for (int x = 0; x < 200; x++) {
            if (!fieldRect.contains(
                myLocation.x + (Math.sin(_absBearingRadians +
                    (orientation*(x/100.0)*maxAngleRadians))*_distance),
                myLocation.y + (Math.cos(_absBearingRadians +
                    (orientation*(x/100.0)*maxAngleRadians))*_distance))) {
                outRatio = x/100.0;
                break;
            }
        }

        _forwardRatio = outRatio;

        return _forwardRatio;
    }

    public double getReversePositionRatio(Point2D.Double myLocation,
        double power, Rectangle2D.Double fieldRect) {
        if (_reverseRatio >= 0.0) { return _reverseRatio; }

        double orientation = _orientation;
        double maxAngleRadians = Math.asin(8.0/(20-3.0*power));

        // 1.0 means the max range of orbital movement exactly reaches bounds
        // of battle field
        double outRatio = 2.0;
        for (int x = 0; x < 200; x++) {
            if (!fieldRect.contains(
                myLocation.x + (Math.sin(_absBearingRadians +
                    (-1*orientation*(x/100.0)*maxAngleRadians))*_distance),
                myLocation.y + (Math.cos(_absBearingRadians +
                    (-1*orientation*(x/100.0)*maxAngleRadians))*_distance))) {
                outRatio = x/100.0;
                break;
            }
        }

        _reverseRatio = outRatio;

        return _reverseRatio;
    }
}
