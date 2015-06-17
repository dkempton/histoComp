/**
 * 
 */
package edu.gsu.dmlab.tracking.databases;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfFloat;
import org.opencv.core.MatOfInt;
import org.opencv.imgproc.*;

import edu.gsu.dmlab.databases.interfaces.IImageDBConnection;
import edu.gsu.dmlab.datatypes.interfaces.IEvent;
import edu.gsu.dmlab.datatypes.interfaces.ITrack;
import edu.gsu.dmlab.tracking.databases.interfaces.ITrackingDBConnection;

/**
 * @author Dustin
 *
 */
public class CallableCalcAndSaveHistDistances implements Callable<Boolean> {

	static final float ranges[][] = { { (float) 0.75, (float) 8.3 },
			{ (float) 0, (float) 256 }, { (float) 0, (float) 52 },
			{ (float) 0.75, (float) 2 }, { (float) 0, (float) 14 },
			{ (float) 0, (float) 150 }, { (float) -0.05, (float) 0.175 },
			{ (float) -0.0001, (float) 0.005 }, { (float) 0, (float) 10 },
			{ (float) -0.0001, (float) 0.05 } };
	static final int histSize = 15;

	IImageDBConnection imageDB;
	ITrackingDBConnection trackingDB;
	ITrack[] tracks;
	int[] wavelenghts;
	int[] paramCombos;
	String type;

	public CallableCalcAndSaveHistDistances(IImageDBConnection imageDB,
			ITrackingDBConnection trackingDB, String type, ITrack[] tracks,
			int[] paramCombos, int[] wavelengths) {
		if (imageDB == null)
			throw new IllegalArgumentException(
					"IImageDBConnection cannot be null in CallableCalcAndSaveHistDistances constructor.");
		if (trackingDB == null)
			throw new IllegalArgumentException(
					"ITrackingDBConnection cannot be null in CallableCalcAndSaveHistDistances constructor.");
		if (tracks == null)
			throw new IllegalArgumentException(
					"sameEvents cannot be null in CallableCalcAndSaveHistDistances constructor.");
		if (paramCombos == null)
			throw new IllegalArgumentException(
					"paramCombos cannot be null in CallableCalcAndSaveHistDistances constructor.");
		if (wavelengths == null)
			throw new IllegalArgumentException(
					"wavelengths cannot be null in CallableCalcAndSaveHistDistances constructor.");
		this.imageDB = imageDB;
		this.trackingDB = trackingDB;
		this.tracks = tracks;
		this.paramCombos = paramCombos;
		this.wavelenghts = wavelengths;
		this.type = type;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Boolean call() throws Exception {
		{
			ArrayList<double[]> comparValues = new ArrayList<double[]>();
			Random rnd = new Random(10);

			// loop over all the tracks and process for this paramCombo
			for (int i = 0; i < this.tracks.length; i++) {

				// process one track
				ITrack trk = tracks[i];
				IEvent[] tmpSameEvents = trk.getEvents();

				// if we have enough IEvents in the track to calculate the same
				// and
				// difference historams then we can proceed
				if (tmpSameEvents.length >= 4) {

					// loop through each of the IEvents in the track
					for (int sameStartIdx = 0; sameStartIdx < tmpSameEvents.length - 4; sameStartIdx++) {

						// find some random track to compare this set of IEvents
						// to
						int idx = rnd.nextInt(tracks.length);
						IEvent[] tmpDiffEvents = tracks[idx].getEvents();

						// make sure there are enough IEvents in the different
						// track
						// to do the processing
						while (tmpDiffEvents.length < 3) {
							idx = rnd.nextInt(tracks.length);
							tmpDiffEvents = tracks[idx].getEvents();
						}

						// get the same and different IEvents for processing
						IEvent[] sameEvents = new IEvent[4];
						IEvent[] diffEvents = new IEvent[2];
						int diffStartIdx = rnd
								.nextInt(tmpDiffEvents.length - 2);

						for (int k = 0; k < 4; k++) {
							sameEvents[k] = tmpSameEvents[sameStartIdx + k];
						}
						for (int j = 0; j < 2; j++) {
							diffEvents[j] = tmpDiffEvents[diffStartIdx + j];
						}

						// subtract two for the id in the first column and the
						// measurement
						// id
						// in the second column. This leaves only the number of
						// wavelengths
						// and parameters.
						int paramSize = this.paramCombos.length - 2;

						// the wavelengths and parameters are split into two
						// columns
						// so
						// divide by two
						int[][] dims = new int[paramSize / 2][];
						for (int j = 0; j < dims.length; j++) {
							int[] dimPair = new int[2];
							// add two to offset for the id and measurement id
							dimPair[0] = this.paramCombos[2 + (j * 2)];
							dimPair[1] = this.paramCombos[2 + (j * 2) + 1];
							dims[j] = dimPair;
						}

						// Same events will have 4 events in it this is
						// guaranteed
						// by the previous checks
						Mat sameHist1 = this.getHist(sameEvents[0], dims, true);
						Mat sameHist2 = this.getHist(sameEvents[1], dims, true);
						Mat sameHist3 = this
								.getHist(sameEvents[2], dims, false);
						Mat sameHist4 = this
								.getHist(sameEvents[3], dims, false);

						// Different events will have 2 events in it this is
						// guaranteed by the constructor previous checks
						Mat diffHist1 = this
								.getHist(diffEvents[0], dims, false);
						Mat diffHist2 = this
								.getHist(diffEvents[1], dims, false);

						double sameVal1, sameVal2, difVal1, difVal2;
						int compareMethod = this.paramCombos[1];
						sameVal1 = this.compareHistogram(sameHist2, sameHist3,
								compareMethod);
						difVal1 = this.compareHistogram(sameHist1, diffHist1,
								compareMethod);

						Mat sameSubHist1 = new Mat();
						Core.subtract(sameHist1, sameHist2, sameSubHist1);

						Mat sameSubHist2 = new Mat();
						Core.subtract(sameHist3, sameHist4, sameSubHist2);
						sameVal2 = this.compareHistogram(sameSubHist1,
								sameSubHist2, compareMethod);

						Mat diffSubHist1 = new Mat();
						Core.subtract(diffHist1, diffHist2, diffSubHist1);
						difVal2 = this.compareHistogram(sameSubHist1,
								diffSubHist1, compareMethod);

						double[] tmpCompVals = new double[4];
						tmpCompVals[0] = sameVal1;
						tmpCompVals[1] = difVal1;
						tmpCompVals[2] = sameVal2;
						tmpCompVals[3] = difVal2;
						comparValues.add(tmpCompVals);
					}
				}
			}
			this.calcFStatAndSave(comparValues);
		}
		System.runFinalization();
		return true;
	}

	private void calcFStatAndSave(ArrayList<double[]> values) {
		OneWayAnova ow = new OneWayAnova();
		double[] sameCat1 = new double[values.size()];
		double[] diffCat1 = new double[values.size()];
		double[] sameCat2 = new double[values.size()];
		double[] diffCat2 = new double[values.size()];

		for (int i = 0; i < values.size(); i++) {
			double[] tmp = values.get(i);
			sameCat1[i] = tmp[0];
			diffCat1[i] = tmp[1];
			sameCat2[i] = tmp[2];
			diffCat2[i] = tmp[3];
		}
		ArrayList<double[]> cat1List = new ArrayList<double[]>();
		cat1List.add(sameCat1);
		cat1List.add(diffCat1);

		ArrayList<double[]> cat2List = new ArrayList<double[]>();
		cat2List.add(sameCat2);
		cat2List.add(diffCat2);

		double fVal1 = ow.anovaFValue(cat1List);
		double fVal2 = ow.anovaFValue(cat2List);
		if (Double.isNaN(fVal1)) {
			fVal1 = 0;
		}

		if (Double.isNaN(fVal2)) {
			fVal2 = 0;
		}

		try {
			//paramCombos[0] is the id in the database for the combonation being inserted
			this.trackingDB.insertParamHistVals(this.type, this.paramCombos[0],
					fVal1, fVal2);
		} catch (SQLException e) {
			e.printStackTrace();
		}

	}

	private double compareHistogram(Mat hist1, Mat hist2, int method) {
		double value = 0;
		switch (method) {
		case 1:
			value = Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CORREL);
			break;
		case 2:
			value = Imgproc.compareHist(hist1, hist2, Imgproc.CV_COMP_CHISQR);
			break;
		case 3:
			value = Imgproc
					.compareHist(hist1, hist2, Imgproc.CV_COMP_INTERSECT);
			break;
		default:
			value = Imgproc.compareHist(hist1, hist2,
					Imgproc.CV_COMP_BHATTACHARYYA);
			break;
		}

		if (Double.isNaN(value)) {
			return 0.0;
		}
		return value;
	}

	// make sure to pass dims in as wavelength/parameter pairs
	private Mat getHist(IEvent event, int[][] dims, boolean left) {

		// Get the image parameters for each wavelength in the set of dimensions
		ArrayList<float[][][]> paramsList = new ArrayList<float[][][]>();
		for (int i = 0; i < dims.length; i++) {
			// waveIdx is 1 to 9 but array idx is 0 to 8 so make sure to
			// subtract 1
			int waveIdx = dims[i][0];

			float[][][] params = this.imageDB.getImageParam(event,
					this.wavelenghts[waveIdx - 1], left);
			paramsList.add(params);
		}

		// Place the values into an Matrix for processing by the OpenCV
		// functions
		int depth = dims.length;
		Mat m = new Mat(paramsList.get(0)[0].length, paramsList.get(0).length,
				org.opencv.core.CvType.CV_32FC(depth));
		for (int y = 0; y < m.rows(); y++) {
			for (int x = 0; x < m.cols(); x++) {
				float[] vals = new float[depth];
				for (int i = 0; i < depth; i++) {
					// get the param indicated by the dims array at depth i
					// the param value is from 1 to 10 whare array index is 0-9
					// hence the -1
					vals[i] = paramsList.get(i)[x][y][dims[i][1] - 1];
				}
				m.put(y, x, vals);
			}
		}

		// for some reason the calcHist function wants a list of mat for the
		// input
		List<Mat> matsForHistFunction = new ArrayList<Mat>();
		matsForHistFunction.add(m);

		// create the channel matrix, histSizes matrix, and histogram ranges
		// matrix for input
		int[] channelsArr = new int[depth];
		int[] histSizeArr = new int[depth];
		MatOfFloat rangesMat = new MatOfFloat();
		for (int i = 0; i < depth; i++) {
			channelsArr[i] = i;
			histSizeArr[i] = histSize;
			// the params in dims go from 1 to 10 but the array index
			// of ranges goes from 0 to 9 so subtract 1.
			rangesMat.push_back(new MatOfFloat(ranges[dims[i][1] - 1]));
		}
		MatOfInt channels = new MatOfInt(channelsArr);
		MatOfInt histSizes = new MatOfInt(histSizeArr);

		// create mask and histogram mat and calculate the histogram
		Mat mask = new Mat();
		Mat currHist = new Mat();
		Imgproc.calcHist(matsForHistFunction, channels, mask, currHist,
				histSizes, rangesMat);
		return currHist;
	}
}
