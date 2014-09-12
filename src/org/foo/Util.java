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

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import java.util.StringTokenizer;

public class Util {
	
	private static final String _CONF_FILE = "fpff.properties";

	private static Properties p = null;

	static {
		try {
			p = loadProperties();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static double _getDouble(String property) {
		return Double.parseDouble(p.getProperty(property).trim());
	}

	public static double[] _getDoubleArray(String property) {
		StringTokenizer st = _getStringTokenizer(property);
		double[] barks = new double[st.countTokens()];
		for (int i = 0; i < barks.length; i++) {
			barks[i] = Double.parseDouble(st.nextToken().trim());
		}
		return barks;
	}

	public static int _getInt(String property) {
		return Integer.parseInt(p.getProperty(property).trim());
	}

	public static int[] _getIntArray(String property) {
		StringTokenizer st = _getStringTokenizer(property);
		int[] barks = new int[st.countTokens()];
		for (int i = 0; i < barks.length; i++) {
			barks[i] = Integer.parseInt(st.nextToken().trim());
		}
		return barks;
	}

	private static Properties loadProperties() throws IOException {
		String configFile = System.getProperty(_CONF_FILE) == null ? _CONF_FILE
				: System.getProperty(_CONF_FILE);
		Properties p = new Properties();
		p.load(new FileReader(configFile));
		return p;
	}

	public static double[] toDoubleArr(short[] arr) {
		double[] dArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			dArr[i] = (double) arr[i];
		}
		return dArr;
	}

	private static StringTokenizer _getStringTokenizer(String property) {
		String string = p.getProperty(property).trim();
		return new StringTokenizer(string);
	}

	public static double[][] _getDoubleMatrix(String property, int rows,
			int columns) {
		StringTokenizer st = _getStringTokenizer(property);
		double[][] matrix = new double[rows][columns];
		for (int i = 0; i < rows; i++) {
			for (int j = 0; j < columns; j++) {
				matrix[i][j] = Double.parseDouble(st.nextToken().trim());
			}
		}
		return matrix;
	}

}
