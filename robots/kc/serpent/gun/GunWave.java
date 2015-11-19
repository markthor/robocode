package kc.serpent.gun;
import kc.serpent.utils.*;
import java.awt.geom.*;
import java.util.*;

public class GunWave extends Wave {
	HashMap virtualBulletAngles = new HashMap();
	public boolean hasPassed;
	
	Point2D.Double enemyLocation;
	public double enemyVelocity;
	public double advancingVelocity;
	public double enemyHeading;
	public int deltaHSign;
	public int velocitySign;
	public int latVelocitySign;
}
