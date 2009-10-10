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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

import org.opensourcephysics.numerics.FFTReal;

public class Fpff {

	private static final String B64CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

	private static final int MODE_FINGERPRINT = 3;

	private static final int MODE_RAW_SPECTRUM = 0;

	private static final int MODE_BARKS = 1;

	private static final int MODE_LOG_BARKS = 2;

	/**
	 * Constant: e - 1
	 */
	private static final double E1m = Math.E - 1.0;

	/**
	 * Multiply arr with '(e - 1) / scale' and take natural log of it. 
	 * @param arr
	 * @param scale
	 * @return
	 */
	private static double[] log(double[] arr, double scale) {
		double[] dArr = new double[arr.length];
		for (int i = 0; i < arr.length; i++) {
			// log(x * ((e - 1) / scale) + 1)
			dArr[i] = Math.log1p(arr[i] * (E1m / scale));
		}
		return dArr;
	}

	public static void main(String[] args) {

		int mode = MODE_FINGERPRINT;

		if (args.length > 0 && args[0].equals("--help")) {
			System.out
					.println("Audio fingerprinting utility version 0.0.2, Copyright (C) 2007 Juha Hejoranta");
			System.out.println("Usage: " + Fpff.class.getName()
					+ " [mode] [options]");
			System.out.println("    Mode:");
			System.out.println("        --help            Show this message");
			System.out
					.println("        --raw             Print raw spectrum data");
			System.out
					.println("        --barks           Print spectrum data in barks");
			System.out
					.println("        --logarithmic     Print barks in adjusted logarithmic scale");
			System.out
					.println("        --symbol          Print symbol data (default)");
			System.out.println("    Options:");
			System.out.println("        --skip-silence    Skip silence");
			System.exit(0);
		} else if (args.length > 0 && args[0].equals("--raw")) {
			mode = MODE_RAW_SPECTRUM;
		} else if (args.length > 0 && args[0].equals("--barks")) {
			mode = MODE_BARKS;
		} else if (args.length > 0 && args[0].equals("--logarithmic")) {
			mode = MODE_LOG_BARKS;
		} else if (args.length > 0 && args[0].equals("--symbol")) {
			mode = MODE_FINGERPRINT;
		}

		boolean skipSilence;
		if (args.length > 1 && args[1].equals("--skip-silence")) {
			skipSilence = true;
		} else {
			skipSilence = false;
		}

		Fpff fpff = new Fpff(mode, skipSilence);
		fpff.go2();

	}

	private final boolean skipSilence;

	/**
	 * Mean of the BARK_BAND_SCALE. Used to scale barked power spectrum.
	 */
	private final double AVG_SCALE;

	// private final String BARK_FORMAT;

	/**
	 * Bark band scaling factors for data whitening
	 */
	private final double[] BARK_BAND_SCALE;

	/**
	 * Selected bark scale band stop frequencies.
	 */
	private final int[] BARK_BAND_STOP;

	/**
	 * Array of bark arrays. Each bark array contains indexes of the fft() data
	 * to construct a single bark.
	 */
	private final int[][] BARK_FRQ_IDX;

	/**
	 * Contains the codebook for symbol lookup
	 */
	private final double[][] codebook;

	private final FFTReal fft;

	private final int fingerPrintMode;

	private final double[] HANN_WINDOW;

	private final int SAMPLEINTERVAL;

	private final int SAMPLERATE;

	private final int WINDOW;

	/**
	 * Array of zeroes presenting spectrum with zero spectrum energy.
	 */
	private final double[] ZERO_SPECT;

	private final double ZERO_SPECT_THRESHOLD;

	public Fpff(int mode, boolean skipSilence) {

		this.skipSilence = skipSilence;

		fingerPrintMode = mode;

		WINDOW = Util._getInt("sample.window");

		HANN_WINDOW = Mth.getHannWindow(WINDOW);

		AVG_SCALE = Util._getDouble("bark.band.scales.avg");

		BARK_BAND_SCALE = Util._getDoubleArray("bark.band.scales");

		SAMPLEINTERVAL = Util._getInt("sample.interval");

		BARK_BAND_STOP = Util._getIntArray("bark.band.stops");

		SAMPLERATE = Util._getInt("sample.rate");

		BARK_FRQ_IDX = getBarkFrequencyIdxArr();

		codebook = Util._getDoubleMatrix("codebook", 64, 16);

		fft = new FFTReal();

		ZERO_SPECT = new double[WINDOW / 2];

		ZERO_SPECT_THRESHOLD = Util._getDouble("zero.spectrum.treshold");

		// BARK_FORMAT = sss(BARK_BAND_STOP.length);

	}

	/**
	 * Return indexes of frequency array which presents the selected bark band.
	 * There might be error of one slot. The R implementation
	 * 
	 * @param frqArr
	 * @param barkStart
	 * @param barkStop
	 * @return
	 */
	private static int[] getBarkFreqIdx(int[] frqArr, int barkStart,
			int barkStop) {
		ArrayList<Integer> iArr = new ArrayList<Integer>();
		for (int i = 0; i < frqArr.length; i++) {
			if (frqArr[i] >= barkStart && frqArr[i] < barkStop) {
				iArr.add(i);
			}
		}
		int[] arr = new int[iArr.size()];
		for (int i = 0; i < iArr.size(); i++) {
			arr[i] = iArr.get(i);
		}
		return arr;
	}

	/**
	 * Get array of bark frequency arrays. Each bark frq entry contains indexes
	 * of the {@link #BARK_BAND_STOP} for that particular entry for grouping.
	 * 
	 * @return
	 */
	private int[][] getBarkFrequencyIdxArr() {
		ArrayList<int[]> iArr = new ArrayList<int[]>();
		int[] freqArr = getFrqArr();
		for (int i = 0; i < BARK_BAND_STOP.length - 1; i++) {
			int[] freqIdx = getBarkFreqIdx(freqArr, BARK_BAND_STOP[i],
					BARK_BAND_STOP[i + 1]);
			iArr.add(freqIdx);
		}
		int[][] arr = new int[iArr.size()][];
		for (int i = 0; i < iArr.size(); i++) {
			arr[i] = iArr.get(i);
		}
		return arr;
	}

	/**
	 * Returns array of spectrum components presenting real life frequencies. In
	 * other words, array contains true frequency of each spectrum component
	 * returned by fft().
	 */
	private int[] getFrqArr() {
		int[] arr = new int[WINDOW / 2];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = i;
		}
		return Mth.mul(arr, SAMPLERATE / WINDOW);
	}

	private double[] getPowerSpectrum(double[] sample) {

		double[] normSample = Mth.subst(sample, Mth.mean(sample));
		double meanAbs = Mth.mean(Mth.abs(normSample));
		if (meanAbs < ZERO_SPECT_THRESHOLD) {
			return ZERO_SPECT;
		} else {
			double[] scaledSample = Mth.div(normSample, meanAbs);
			double[] windowedSample = Mth.mult(scaledSample, HANN_WINDOW);

			/*
			 * It seems that the FFTReal handles dropping of the "mirrored".
			 * Other wise it should return array twice the sample array length.
			 * half. TODO double check this.
			 */
			double[] spectrum = fft.transform(windowedSample);

			return Mth.complexAbs(spectrum);
		}
	}

	/**
	 * 
	 * @param powSpect
	 * @return
	 */
	private double[] getScaledBarks(double[] powSpect) {
		double[] result = new double[BARK_FRQ_IDX.length];
		for (int i = 0; i < BARK_FRQ_IDX.length; i++) {
			result[i] = Mth.sumSelected(powSpect, BARK_FRQ_IDX[i])
					/ BARK_FRQ_IDX[i].length * BARK_BAND_SCALE[i];
		}
		return result;
	}

	private static byte[] buf;

	private static short[] sample;

	private void go2() {

		FileOutputStream fdout = new FileOutputStream(FileDescriptor.out);
		BufferedOutputStream bos = new BufferedOutputStream(fdout, 256 * 1024);
		PrintStream psout = new PrintStream(bos, false);

		FileInputStream fdin = new FileInputStream(FileDescriptor.in);
		BufferedInputStream bis = new BufferedInputStream(fdin, 1024 * 1024);

		buf = new byte[WINDOW * 2];
		ByteBuffer bb = ByteBuffer.wrap(buf);
		sample = new short[WINDOW];

		// sox (should) emit bytes in little endian
		bb.order(ByteOrder.LITTLE_ENDIAN);
		ShortBuffer sb = bb.asShortBuffer();

		try {
			boolean go = true;
			int pos = 0;
			while (go) {

				if (bis.available() < buf.length - pos) {
					// input stream would block.
					if (psout.checkError()) {
						// exit if error
						go = false;
						break;
					}
				}
				while (pos != buf.length) {
					int bcnt = bis.read(buf, pos, buf.length - pos);
					if (bcnt == -1) {
						go = false;
						break;
					}
					pos += bcnt;
				}

				if (go) {
					sb.clear();
					sb.get(sample);

					printFingerprint(sample, fingerPrintMode, psout);

					bb.clear();
					bb.position(SAMPLEINTERVAL * 2);
					bb.compact();
					pos = bb.position();
				}
			}
			if (MODE_FINGERPRINT == fingerPrintMode) {
				psout.println();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		psout.flush();

	}

	private void printFingerprint(short[] sample, int mode, PrintStream psout) {

		double[] powSpect = getPowerSpectrum(Util.toDoubleArr(sample));
		if (skipSilence && ZERO_SPECT == powSpect) {
			return;
		}

		if (MODE_RAW_SPECTRUM == mode) {
			int i = 0;
			for (; i < powSpect.length - 1; i++) {
				psout.format("%e ", powSpect[i]);
			}
			psout.format("%e", powSpect[i]);
			psout.println();
			return;
		}

		double[] barks = getScaledBarks(powSpect);
		if (MODE_BARKS == mode) {
			int i = 0;
			for (; i < barks.length - 1; i++) {
				psout.format("%e ", barks[i]);
			}
			psout.format("%e", barks[i]);
			psout.println();
			return;
		}

		double[] logBarks = log(barks, AVG_SCALE);
		if (MODE_LOG_BARKS == mode) {
			int i = 0;
			for (; i < logBarks.length - 1; i++) {
				psout.format("%e ", logBarks[i]);
			}
			psout.format("%e", logBarks[i]);
			psout.println();
			return;
		}

		if (MODE_FINGERPRINT == mode) {
			int cbIdx = Mth.getClosetRowIndex(logBarks, codebook);
			psout.print(B64CHARS.charAt(cbIdx));
			return;
		}

	}
	// private void go() {
	//
	// FileOutputStream fdout = new FileOutputStream(FileDescriptor.out);
	// BufferedOutputStream bos = new BufferedOutputStream(fdout, 256 * 1024);
	// PrintStream ps = new PrintStream(bos, false);
	// System.setOut(ps);
	//
	// FileInputStream fdin = new FileInputStream(FileDescriptor.in);
	// BufferedInputStream bis = new BufferedInputStream(fdin, 1024 * 1024);
	// System.setIn(bis);
	//
	// ReadableByteChannel rbc = Channels.newChannel(System.in);
	// ByteBuffer bb = ByteBuffer.allocateDirect(WINDOW * 2);
	// short[] sample = new short[WINDOW];
	//
	// // sox (should) emit bytes in little endian
	// bb.order(ByteOrder.LITTLE_ENDIAN);
	// ShortBuffer sb = bb.asShortBuffer();
	//
	// try {
	// boolean go = true;
	// while (go) {
	// while (bb.hasRemaining()) {
	// if (rbc.read(bb) == -1) {
	// go = false;
	// break;
	// }
	// }
	// if (go) {
	// sb.clear();
	// sb.get(sample);
	//
	// printFingerprint(sample, fingerPrintMode, System.out);
	// System.out.flush();
	// if (System.out.checkError()) {
	// go = false;
	// break;
	// }
	//
	// bb.position(SAMPLEINTERVAL * 2);
	// bb.compact();
	// }
	// }
	// if (MODE_FINGERPRINT == fingerPrintMode) {
	// System.out.println();
	// }
	// } catch (IOException e) {
	// e.printStackTrace();
	// }
	// System.out.flush();
	//
	// }

}
