/**
 * Copyright (c) 2011 Chase
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *    1. The origin of this software must not be misrepresented; you must not
 *    claim that you wrote the original software. If you use this software
 *    in a product, an acknowledgment in the product documentation would be
 *    appreciated but is not required.
 * 
 *    2. Altered source versions must be plainly marked as such, and must not be
 *    misrepresented as being the original software.
 * 
 *    3. This notice may not be removed or altered from any source
 *    distribution.
 */

package cs.move.extra;

import static cs.utils.FastMath.*;
import java.util.ArrayList;
import java.util.Iterator;

import robocode.Bullet;
import robocode.util.Utils;
import cs.geom.Line;
import cs.geom.Rectangle;
import cs.geom.Vector;
import cs.utils.FastMath;
import cs.utils.Tools;


public class Wave<T> extends Vector {
	private static final long serialVersionUID = -6209515502860778281L;
	private static final double MAX_ESCAPE_FACTOR = 1.1;

	public long fireTime;
	public double escapeAngle;
	public double directAngle;
	public double speed;
	public double power;
	public boolean imaginary = false;

	public T data;

	public double getRadius(long time) {
		return speed*(time - fireTime);
	}

	public int intersected = 0;
	
	/**
	 * Used to determine if we can safely remove this wave
	 */
	public final boolean didIntersect(Vector target, long time) {
		hitbox.setFrame(target, 36, 36);

		double radius = getRadius(time);
		double nextRadius = getRadius(time+1);
		Vector[] current = Tools.intersectRectCircle(hitbox, this, radius);
		Vector[] next = Tools.intersectRectCircle(hitbox, this, nextRadius);

		if(current.length != 0 || next.length != 0) {
			++intersected;
		} else {
			if(intersected > 0) {
				return true;
			}
		}

		return false;
	}

	public final boolean intersects(Vector target, long time) {
		hitbox.setFrame(target, 36, 36);

		double radius = getRadius(time);
		double nextRadius = getRadius(time+1);
		Vector[] current = Tools.intersectRectCircle(hitbox, this, radius);
		Vector[] next = Tools.intersectRectCircle(hitbox, this, nextRadius);

		if(current.length != 0 || next.length != 0) {
			return true;
		}

		return false;
	}

	public static Rectangle hitbox = new Rectangle();
	/**
	 * Used for calculating where a bullet will intersect a target
	 */
	public final boolean doesIntersect(Vector target, long time) {
		hitbox.setFrame(target, 36, 36);

		double radius = getRadius(time);
		double nextRadius = getRadius(time+1);
		double[][] current = Tools.intersectRectCircleD(hitbox, this, radius);
		double[][] next = Tools.intersectRectCircleD(hitbox, this, nextRadius);

		if(current.length != 0 || next.length != 0) {
			for(double[] v : current)
				expandFactors(v);

			for(double[] v : next)
				expandFactors(v);

			double[][] corners = hitbox.getCorners();
			for(double[] c : corners) {
				double dist = distanceSq(c[0],c[1]);
				if(dist < nextRadius*nextRadius
						&& dist > radius*radius) {
					expandFactors(c);
				}
			}

			return true;
		}

		return false;
	}

	public ArrayList<double[]> bulletShadows = new ArrayList<double[]>();
	public ArrayList<BulletShadow> unmergedShadows = new ArrayList<BulletShadow>();

	public final void shadowBullet(Bullet b, Line line, long time) {
		double minFactor = java.lang.Double.POSITIVE_INFINITY;
		double maxFactor = java.lang.Double.NEGATIVE_INFINITY;

		boolean intersect = false;

		double radius = getRadius(time);
		double nextRadius = getRadius(time+1);

		double[] current = Tools.intersectSegCircle(x, y, radius, line.x1, line.y1, line.x2, line.y2);
		double[] next = Tools.intersectSegCircle(x, y, nextRadius, line.x1, line.y1, line.x2, line.y2);

		for(int i=0; i<current.length; i+=2) {
			double angle = Utils.normalRelativeAngle(angleTo(current[i],current[i+1]) - directAngle) / escapeAngle;
			if(angle < minFactor) minFactor = angle;
			if(angle > maxFactor) maxFactor = angle;

			intersect = true;
		}

		for(int i=0; i<next.length; i+=2) {
			double angle = Utils.normalRelativeAngle(angleTo(next[i],next[i+1]) - directAngle) / escapeAngle;
			if(angle < minFactor) minFactor = angle;
			if(angle > maxFactor) maxFactor = angle;
			intersect = true;
		}

		//if()
		double distA = this.distanceSq(line.x1, line.y1);
		if(distA < nextRadius*nextRadius && distA > radius*radius) {
			double angle = Utils.normalRelativeAngle(angleTo(line.x1,line.y1) - directAngle) / escapeAngle;
			if(angle < minFactor) minFactor = angle;
			if(angle > maxFactor) maxFactor = angle;
			intersect = true;
		}

		double distB = this.distanceSq(line.x2, line.y2);
		if(distB < nextRadius*nextRadius && distB > radius*radius) {
			double angle = Utils.normalRelativeAngle(angleTo(line.x2,line.y2) - directAngle) / escapeAngle;
			if(angle < minFactor) minFactor = angle;
			if(angle > maxFactor) maxFactor = angle;
			intersect = true;
		}

		if(intersect) {
			BulletShadow shadow = new BulletShadow();
			shadow.b = b;
			shadow.shadow = new double[] {
					minFactor, maxFactor
			};
			
			//if shadow is outside of the escape angles, don't add it
			if((minFactor > MAX_ESCAPE_FACTOR && maxFactor > MAX_ESCAPE_FACTOR)
			|| (minFactor < -MAX_ESCAPE_FACTOR && maxFactor < -MAX_ESCAPE_FACTOR)) {
				return;
			}
			
			//if one of the factors is outside of the escape angle, clamp it
			minFactor = FastMath.limit(-MAX_ESCAPE_FACTOR, minFactor, MAX_ESCAPE_FACTOR);
			maxFactor = FastMath.limit(-MAX_ESCAPE_FACTOR, maxFactor, MAX_ESCAPE_FACTOR);
				
			unmergedShadows.add(shadow);
			mergeShadow(shadow);
		}
	}

	public final void mergeShadow(BulletShadow shadow) {
		double minFactor = shadow.shadow[0];
		double maxFactor = shadow.shadow[1];

		boolean merged = false;
		for(double[] d : bulletShadows) {
			if(!(minFactor > d[1] || maxFactor < d[0])) {
				//intersection
				if(minFactor < d[0] && maxFactor > d[1]) {
					d[0] = minFactor;
					d[1] = maxFactor;
				}

				if(maxFactor > d[0] && maxFactor < d[1]) {
					if(minFactor < d[0]) {
						d[0] = minFactor;
					}
				}
				if(minFactor < d[1] && minFactor > d[0]) {
					if(maxFactor > d[1]) {
						d[1] = maxFactor;
					}
				}
				merged = true;
				break;
			}
		}

		if(!merged) {
			bulletShadows.add(shadow.shadow);
		}
	}

	public final void removeShadow(Bullet b) {
		boolean removed = false;
		Iterator<BulletShadow> it = unmergedShadows.iterator();
		while(it.hasNext()) {
			BulletShadow bs = it.next();
			if(bs.equals(b)) {
				it.remove();
				removed = true;
			}
			//we may have more then one shadow for each bullet
			//weird I know
		}

		if(removed) {
			bulletShadows.clear();

			//remerge all still existing shadows
			for(BulletShadow bs : unmergedShadows) {
				mergeShadow(bs);
			}
		}
	}

	public final void standingIntersection(Vector target) {
		/*
		 * hitbox.set(target, 36, 36);
		 * Vector[] corners = hitbox.getFasterCorners();
		 * for(Vector v : corners) {
		 *     expandFactors(v);
		 * }
		 * 
		 * Equivalent, the below is just faster and uses less memory
		 */
		double angle = Utils.normalRelativeAngle(atan2((target.x - 18) - x, (target.y - 18) - y)
				- directAngle) / escapeAngle;
		if(angle < minFactor) minFactor = angle;
		if(angle > maxFactor) maxFactor = angle;

		angle = Utils.normalRelativeAngle(atan2((target.x + 18) - x, (target.y - 18) - y)
				- directAngle) / escapeAngle;
		if(angle < minFactor) minFactor = angle;
		if(angle > maxFactor) maxFactor = angle;

		angle = Utils.normalRelativeAngle(atan2((target.x - 18) - x, (target.y + 18) - y)
				- directAngle) / escapeAngle;
		if(angle < minFactor) minFactor = angle;
		if(angle > maxFactor) maxFactor = angle;

		angle = Utils.normalRelativeAngle(atan2((target.x + 18) - x, (target.y + 18) - y)
				- directAngle) / escapeAngle;
		if(angle < minFactor) minFactor = angle;
		if(angle > maxFactor) maxFactor = angle;
		
		lastTarget.setLocation(target);
	}

	public Vector lastTarget = new Vector();
	public double minFactor = java.lang.Double.POSITIVE_INFINITY;
	public double maxFactor = java.lang.Double.NEGATIVE_INFINITY;

	/**
	 * Expand the guessfactors based on target location
	 */
	private void expandFactors(double[] pos) {
		double angle = Utils.normalRelativeAngle(angleTo(pos[0],pos[1]) - directAngle) / escapeAngle;
		if(angle < minFactor) minFactor = angle;
		if(angle > maxFactor) maxFactor = angle;
	}

	/**
	 * Reset our factors for calculating position (important!)
	 */
	public void resetFactors() {
		minFactor = java.lang.Double.POSITIVE_INFINITY;
		maxFactor = java.lang.Double.NEGATIVE_INFINITY;
		lastTarget.setLocation(0, 0);
	}
}

class BulletShadow {
	public Bullet b;
	public double[] shadow;

	public final boolean equals(Bullet q) {
		if(abs(b.getHeadingRadians() - q.getHeadingRadians()) < 0.001 && abs(b.getPower() - q.getPower()) < 0.001
				&& abs(b.getX()-q.getX()) < 0.001 && abs(b.getY()-q.getY()) < 0.001) {
			return true;
		}
		return false;
	}
}
