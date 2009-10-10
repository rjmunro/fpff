/*
 Audio fingerprinting utility
 Copyright (C) 2007 Juha Heljoranta

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation; either version 2
 of the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.foo;

/**
 * Simple math and
 * 
 * @author Juha Heljoranta
 * 
 */
public class Mth {

	public static double[] abs(double[] arr) {
		double[] dArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			dArr[i] = Math.abs(arr[i]);
		}
		return dArr;
	}

	/**
	 * Return absolute values of complex double[] arr. Complex data is in format
	 * Re arr[n] = a, Im arr[n+1] = b.
	 * 
	 * @param arr
	 * @return
	 */
	public static double[] complexAbs(double[] arr) {
		double[] dArr = new double[arr.length / 2];
		for (int i = 0; i < arr.length / 2; i++) {
			dArr[i] = Math.sqrt(Math.pow(Math.abs(arr[i * 2]), 2.0)
					+ Math.pow(Math.abs(arr[i * 2 + 1]), 2.0));
		}
		return dArr;
	}

	public static double[] div(double[] arr, double value) {
		double[] dArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			dArr[i] = arr[i] / value;
		}
		return dArr;
	}

	public static double mean(double[] arr) {
		double val = 0.0;
		for (int i = 0; i < arr.length; i++) {
			val += arr[i];
		}
		return val / arr.length;
	}

	public static int[] mul(int[] arr, int value) {
		int[] dArr = new int[arr.length];
		for (int i = 0; i < arr.length; i++) {
			dArr[i] = arr[i] * value;
		}
		return dArr;
	}

	public static double[] mult(double[] arr, double[] arr2) {
		double[] dArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			dArr[i] = arr[i] * arr2[i];
		}
		return dArr;

	}

	public static double[] subst(double[] arr, double value) {
		double[] dArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			dArr[i] = arr[i] - value;
		}
		return dArr;
	}

	/**
	 * Calculate sum of selected elements in arr. The argument selector contains
	 * indexes to select elements from arr. The selected elements are summed.
	 * 
	 * @param arr
	 *            array which components are summed
	 * @param selectors
	 *            array of indexes of elements of arr which will be summed
	 * @return
	 */
	public static double sumSelected(double[] arr, int[] selectors) {
		double sum = 0.0;
		for (int i : selectors) {
			sum += arr[i];
		}
		return sum;
	}

	public static double distance(double[] arrA, double[] arrB) {
		double sum = 0.0;
		for (int i = 0; i < arrA.length; i++) {
			sum += Math.pow(Math.abs(arrA[i]) - Math.abs(arrB[i]), 2.0);
		}
		return Math.sqrt(sum);
	}

	public static int getClosetRowIndex(double[] referenceRow, double[][] matrix) {
		int idx = -1;
		double neareset = Double.MAX_VALUE;
		for (int i = 0; i < matrix.length; i++) {
			double dist = Mth.distance(referenceRow, matrix[i]);
			if (dist < neareset) {
				idx = i;
				neareset = dist;
			}
		}
		return idx;
	}
	
	public static double[] getHannWindow(int length) {
		double[] v = new double[length];
		for (int i = 0; i < length; i++) {
			v[i] = 0.5 * (1.0 - Math.cos((2.0 * Math.PI * i) / (length - 1)));
		}
		return v;
	}


}
