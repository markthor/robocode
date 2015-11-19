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

package cs.geom;

import java.awt.geom.Point2D;
import static cs.utils.FastMath.*;

public class Vector extends Point2D.Double implements Cloneable {
	private static final long serialVersionUID = 8157436870284626412L;

	/**
	 * Initializes it to 0,0
	 */
	public Vector() {}

	/**
	 * Initializes this vector to x,y
	 * @param x
	 * @param y
	 */
	public Vector(double x, double y) {
		super(x,y);
	}

	/**
	 * Calculates the angle from this point to point p.
	 */
	public final double angleTo(Vector p) {
		return atan2(p.x - x, p.y - y);
	}

	public final double angleTo(double x, double y) {
		return atan2(x - this.x, y - this.y);
	}

	/**
	 * Calculates the angle from point p to this point.
	 */
	public final double angleFrom(Vector p) {
		return atan2(x - p.x, y - p.y);
	}

	/**
	 * Sets this vector to be equal to the given projection. Equivalent to calling set then project.
	 */
	public final void setLocationAndProject(Vector vec, double angle, double distance) {
		x = vec.x + sin(angle) * distance;
		y = vec.y + cos(angle) * distance;
	}

	/**
	 * Projects from this point, by an angle and distance
	 */
	public final Vector projectNew(double angle, double distance) {
		Vector out = clone();
		out.x += sin(angle) * distance;
		out.y += cos(angle) * distance;
		return out;
	}

	/**
	 * Projects this point, by an angle and distance
	 */
	public final void project(double angle, double distance) {
		x += sin(angle) * distance;
		y += cos(angle) * distance;
	}

	@Override
	public final Vector clone() {
		return (Vector)super.clone();
	}
}