/*
 * Copyright (c) 2011 Andrew Wells. All rights reserved.
 *
 *	Redistribution and use in source and binary forms, with or without modification, are
 *	permitted provided that the following conditions are met:
 *
 *		1. Redistributions of source code must retain the above copyright notice, this list of
 *     conditions and the following disclaimer.
 *
 *		2. Redistributions in binary form must reproduce the above copyright notice, this list
 *    of conditions and the following disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 *	THIS SOFTWARE IS PROVIDED BY Andrew Wells ''AS IS'' AND ANY EXPRESS OR IMPLIED
 *	WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 *	FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL Andrew Wells OR
 *	CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 *	CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *	SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 *	ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 *	NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 *	ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * 	The views and conclusions contained in the software and documentation are those of the
 *	authors and should not be interpreted as representing official policies, either expressed
 *	or implied, of Andrew Wells.
 */

package aw.utils;

public final class misc {

	public static int[] getOrderAscendingDangers(double[] dangers) {
		int[] order = new int[dangers.length];
		for (int i = 0; i < dangers.length; i++) {
			order[i] = i;
		}
		return quicksortDangers(dangers, order, 0, dangers.length - 1);
	}

	private static int[] quicksortDangers(double[] dangers, int[] order,
			int left, int right) {
		if (right > left) {
			int pivot = (left + right) / 2;
			int partitionIndex = partition(dangers, order, left, right, pivot);
			quicksortDangers(dangers, order, left, partitionIndex - 1);
			quicksortDangers(dangers, order, partitionIndex + 1, right);

		}

		return order;
	}

	private static int partition(double[] dangers, int[] order, int left,
			int right, int pivot) {
		int storeIndex = left;

		int tmp = order[right];
		order[right] = order[pivot];
		order[pivot] = tmp;
		double dangerOfPivot = dangers[order[right]];

		for (int i = left; i < right; i++) {
			if (dangers[order[i]] < dangerOfPivot) {
				tmp = order[storeIndex];
				order[storeIndex++] = order[i];
				order[i] = tmp;
			}
		}
		tmp = order[storeIndex];
		order[storeIndex] = order[right];
		order[right] = tmp;

		return storeIndex;
	}
	

	public static double kernelIntegralFunction(double z) {
		return Math.atan(z);
	}
	
	

	/*
	 * 
	 * This algorithm comes from
	 * 
	 * UNITED STATES DEPARTMENT OF COMMERCE
	 * 
	 * Luther H. Hodges, Secretary
	 * 
	 * NATIONAL BUREAU OF STANDARDS
	 * 
	 * A. V. Astin, Director
	 * 
	 * Handbook of Mathematical Functions With Formulas, Graphs, and
	 * Mathematical Tables
	 * 
	 * Edited by Milton Abramowitz and Irene A. Stegun
	 * 
	 * http://people.math.sfu.ca/~cbm/aands/frameindex.htm
	 */

	public static double erfPrecise(double z) {

		double x = Math.abs(z);

		final double p = 0.32759;
		final double a1 = 0.254829592;
		final double a2 = -0.284496736;
		final double a3 = 1.421413741;
		final double a4 = -1.453152027;
		final double a5 = 1.061405429;

		double t = 1.0 / (1.0 + p * x);

		double ans = 1 - t * (a1 + t * (a2 + t * (a3 + t * (a4 + t * a5))))
				* Math.exp(-x * x);
		if (z < 0)
			return -ans;
		else
			return ans;
	}

	// Max error 2.5 * 10 ^ -5.
	// Algorithm from Abromowitz and Stegun, Handbook of Mathematical.
	public static double erf(double z) {
		final double p = 0.47047;
		final double a1 = 0.3480242;
		final double a2 = -0.0958798;
		final double a3 = 0.7478556;

		double t = 1.0 / (1.0 + p * Math.abs(z));
		double ans = 1.0 - t * (a1 + t * (a2 + t * a3)) * Math.exp(-z * z);
		if (z < 0)
			return -ans;
		else
			return ans;
	}

}
