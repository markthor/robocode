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

package cs.utils;

import java.awt.Color;
import cs.geom.Rectangle;
import cs.geom.Vector;
import robocode.util.Utils;
import static cs.utils.FastMath.*;

/**
 * A sub section of geom tools, modified to take advantage of vector class
 */
public final class Tools {
	private static final Color[] alphabet;
	static {
		final int[][] cA = new int[][]{{0, 0, 180},{175, 13, 102},{146,248,70},
				{255, 200, 47},{255,118,0},{185,185,185},{235,235,222},{100,100,100},
				{255,255,0},{55,19,112},{255,255,150},{202,62,94},{205,145,63},{12,75,100},
				{255,0,0},{175,155,50},{0,0,0},{37,70,25},{121,33,135},{83,140,208},{0,154,37},
				{178,220,205},{255,152,213},{0,0,74},{175,200,74},{63,25,12}};

		alphabet = new Color[26];
		for(int i=0;i<26;++i) {
			alphabet[i] = new Color(cA[i][0],cA[i][1],cA[i][2]);
		}
	}

	public static final java.awt.Color getColorForLetter(char c) {
		c = (""+c).toLowerCase().charAt(0);
		if(c >= 97 && c <=122) {
			int index = c-97;
			return alphabet[index];
		}
		return Color.BLACK;
	}

	/**
	 * Returns an array of vectors where the rectangle intersect a circle
	 * at c with radius r.
	 */
	public static final Vector[] intersectRectCircle(Rectangle rect, Vector c, double r) {
		double[] pnts = intersectRectCircle(rect.getMinX(),rect.getMinY(),
				rect.getWidth(),rect.getHeight(), c.x, c.y, r);
		Vector[] output = new Vector[pnts.length/2];
		for(int i = 0; i<output.length; ++i) {
			output[i] = new Vector(pnts[i*2],pnts[i*2+1]);
		}
		return output;
	}

	public static final double[][] intersectRectCircleD(Rectangle rect, Vector c, double r) {
		double[] pnts = intersectRectCircle(rect.getMinX(),rect.getMinY(),
				rect.getWidth(),rect.getHeight(), c.x, c.y, r);
		double[][] output = new double[pnts.length/2][2];
		for(int i = 0; i<output.length; ++i) {
			output[i] = new double[]{pnts[i*2],pnts[i*2+1]};
		}
		return output;
	}

	public static final double[] intersectRectCircle(
			double rx, double ry, double rw, double rh,
			double cx, double cy, double r) {
		double mx = rx+rw;
		double my = ry+rh;

		//every line can intersect twice, meaning 4 points at most per line
		double[] intersect = new double[16];
		int n = 0;

		double[] in = intersectSegCircle(cx,cy,r,rx,ry,mx,ry); //top
		/*
		 * for(int i=0;i!=in.length;++i)
		 *     intersect[n++] = in[i];
		 * 
		 * Equivalent to below, just the hardcoded ifs are faster
		 */
		if(in.length == 2) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
		} else if(in.length == 4) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
			intersect[n++] = in[2];
			intersect[n++] = in[3];
		}

		in = intersectSegCircle(cx,cy,r,rx,my,mx,my); //bottom
		if(in.length == 2) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
		} else if(in.length == 4) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
			intersect[n++] = in[2];
			intersect[n++] = in[3];
		}

		in = intersectSegCircle(cx,cy,r,rx,ry,rx,my); //left
		if(in.length == 2) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
		} else if(in.length == 4) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
			intersect[n++] = in[2];
			intersect[n++] = in[3];
		}

		in = intersectSegCircle(cx,cy,r,mx,ry,mx,my); //right
		if(in.length == 2) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
		} else if(in.length == 4) {
			intersect[n++] = in[0];
			intersect[n++] = in[1];
			intersect[n++] = in[2];
			intersect[n++] = in[3];
		}

		double[] output = new double[n];
		for(int i=0;i!=n;++i)
			output[i] = intersect[i];

		return output;
	}

	public static final double[] intersectSegCircle(double cx, double cy, double r,
			double lax, double lay, double lbx, double lby) {

		double diffx = cx - lax;
		double diffy = cy - lay;

		double dirx = lbx-lax;
		double diry = lby-lay;
		double l = sqrt(dirx*dirx + diry*diry);

		dirx /= l;
		diry /= l;

		double a0 = diffx*diffx+diffy*diffy - r*r;
		double a1 = diffx*dirx+diffy*diry;

		double discr = a1 * a1 - a0;

		if (discr > 0) {
			/* The circle and line meet at two places */
			double lengthSq = (lbx-lax)*(lbx-lax)+(lby-lay)*(lby-lay);

			discr = sqrt(discr);
			double m1 = a1 - discr;
			double m2 = a1 + discr;

			if(m1 > 0 && m1*m1 < lengthSq && m2 > 0 && m2*m2 < lengthSq) {
				return new double[] {
						lax + m1 * dirx, lay + m1 * diry,
						lax + m2 * dirx, lay + m2 * diry
				};
			} else if (m1 > 0 && m1*m1 < lengthSq) {
				return new double[] {
						lax + m1 * dirx, lay + m1 * diry
				};
			} else if (m2 > 0 && m2*m2 < lengthSq) {
				return new double[] {
						lax + m2 * dirx, lay + m2 * diry
				};
			}
		} else if (discr == 0) {
			double lengthSq = (lbx-lax)*(lbx-lax)+(lby-lay)*(lby-lay);
			/* We have ourselves a tangent */
			if (a1 > 0 && a1*a1 < lengthSq) {
				return new double[] {
						lax+a1*dirx, lay+a1*diry
				};
			}
		}

		return new double[0];
	}

	/**
	 * Calculates the wall distance to a certain enemy.
	 */
	public static final double getWallDistance(Vector center, Rectangle field,
			double eDist, double eAngle, int oDir) {
		final double WALL_MARGIN = 18.0;

		return min(min(min(
				distanceWest(field.getMaxY() - WALL_MARGIN - center.y, eDist, eAngle - (PI / 2.0), oDir),
				distanceWest(field.getMaxX() - WALL_MARGIN - center.x, eDist, eAngle + PI, oDir)),
				distanceWest(center.y - WALL_MARGIN, eDist, eAngle + (PI / 2.0), oDir)),
				distanceWest(center.x - WALL_MARGIN, eDist, eAngle, oDir));
	}

	private static final double distanceWest(double toWall, double eDist,
			double eAngle, int oDir) {
		if (eDist <= toWall)
			return Double.POSITIVE_INFINITY;
		final double wallAngle = acos(-oDir * toWall / eDist) + oDir * (PI / 2.0);
		return Utils.normalAbsoluteAngle(oDir * (wallAngle - eAngle));
	}

	public static final double fastSmooth(Vector pos, double angle, int direction, double dist) {
		return fastSmooth(pos.x,pos.y,angle,direction,800,600,dist);
	}

	public static final double fastSmooth(Vector pos, double angle, int direction, double dist,
			double fw, double fh) {
		return fastSmooth(pos.x,pos.y,angle,direction,fw,fh,dist);
	}

	//no object creation or method calling if we can help it, need this to stay fast and rather memory unintensive
	public static final double fastSmooth(double px, double py, double angle, int direction,
			double fw, double fh, double c2pd) {
		final double margin = 18;

		double stick = 140;
		if(c2pd < stick) stick = c2pd;

		double stickSq = stick*stick;

		double nx = px + stick*sin(angle);
		double ny = py + stick*cos(angle);

		if(nx >= margin && nx <= fw - margin && ny >= margin && ny <= fh - margin)
			return angle;

		/* TOP */
		if(ny > fh - margin || py > fh - stick - margin) {
			//System.out.println("top");
			/* RIGHT */
			if(nx > fw - margin || px > fw - stick - margin) {
				//System.out.println("right");
				if(direction > 0) {
					//smooth right
					stick = fw - margin - px;
					nx = fw - margin;
					ny = py - direction * sqrt(stickSq - stick*stick);
					return atan2(nx-px, ny-py);
				} else {
					//smooth top
					stick = fh - margin - py;
					nx = px + direction * sqrt(stickSq - stick*stick);
					ny = fh - margin;
					return atan2(nx-px, ny-py);
				}
			} else /* LEFT */ if(nx < margin || px < stick + margin) {
				//System.out.println("left");
				if(direction > 0) {
					//smooth top
					stick = fh - margin - py;
					nx = px + direction * sqrt(stickSq - stick*stick);
					ny = fh - margin;
					return atan2(nx-px, ny-py);
				} else {
					//smooth left
					stick = px - margin;
					nx = margin;
					ny = py + direction * sqrt(stickSq - stick*stick);
					return atan2(nx-px, ny-py);
				}
			}
			//smooth top
			stick = fh - margin - py;
			nx = px + direction * sqrt(stickSq - stick*stick);
			ny = fh - margin;
			return atan2(nx-px, ny-py);
		} else /* BOTTOM */ if(ny < margin || py < stick + margin) {
			/* RIGHT */
			if(nx > fw - margin || px > fw - stick - margin) {
				if(direction > 0) {
					//smooth bottom
					stick = py - margin;
					nx = px - direction * sqrt(stickSq - stick*stick);
					ny = margin;
					return atan2(nx-px, ny-py);
				} else {
					//smooth right
					stick = fw - margin - px;
					nx = fw - margin;
					ny = py - direction * sqrt(stickSq - stick*stick);
					return atan2(nx-px, ny-py);
				}
			} else /* LEFT */ if(nx < margin || px < stick + margin) {
				if(direction > 0) {
					//smooth left
					stick = px - margin;
					nx = margin;
					ny = py + direction * sqrt(stickSq - stick*stick);
					return atan2(nx-px, ny-py);
				} else {
					//smooth bottom
					stick = py - margin;
					nx = px - direction * sqrt(stickSq - stick*stick);
					ny = margin;
					return atan2(nx-px, ny-py);
				}
			}
			//smooth bottom
			stick = py - margin;
			nx = px - direction * sqrt(stickSq - stick*stick);
			ny = margin;
			return atan2(nx-px, ny-py);
		}

		/* RIGHT */
		if(nx > fw - margin || px > fw - stick - margin) {
			stick = fw - margin - px;
			nx = fw-margin;
			ny = py - direction * sqrt(stickSq - stick*stick);
			return atan2(nx-px, ny-py);
		} else /* LEFT */ if(nx < margin || px < stick + margin) {
			stick = px - margin;
			nx = margin;
			ny = py+direction * sqrt(stickSq - stick*stick);
			return atan2(nx-px, ny-py);
		}
		System.err.println("Something is really messed up here... (check your wall smoothing code!)");
		return angle;
	}
}