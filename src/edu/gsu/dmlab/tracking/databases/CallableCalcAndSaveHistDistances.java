/**
 * 
 */
package edu.gsu.dmlab.tracking.databases;

import java.sql.SQLException;
import java.util.ArrayList;

import java.util.Random;
import java.util.concurrent.Callable;

import org.apache.commons.math3.stat.inference.OneWayAnova;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgproc.*;

import edu.gsu.dmlab.datatypes.interfaces.IEvent;
import edu.gsu.dmlab.datatypes.interfaces.ITrack;
import edu.gsu.dmlab.imageproc.interfaces.IHistogramProducer;
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

	// IImageDBConnection imageDB;
	ITrackingDBConnection trackingDB;
	IHistogramProducer histoProducer;
	ITrack[] tracks;
	int[] wavelenghts;
	int[] paramCombos;
	String type;

	public CallableCalcAndSaveHistDistances(ITrackingDBConnection trackingDB,
			IHistogramProducer histoProducer, String type, ITrack[] tracks,
			int[] paramCombos, int[] wavelengths) {
		if (histoProducer == null)
			throw new IllegalArgumentException(
					"IHistogramProducer cannot be null in CallableCalcAndSaveHistDistances constructor.");
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
		this.histoProducer = histoProducer;
		this.trackingDB = trackingDB;
		this.tracks = tracks;
		this.paramCombos = paramCombos;
		this.wavelenghts = wavelengths;
		this.type = type;

	}

	@Override
	public void finalize() {
		try {
			super.finalize();
		} catch (Throwable e) {
			e.printStackTrace();
		}
		this.histoProducer = null;
		this.trackingDB = null;
		this.tracks = null;
		this.paramCombos = null;
		this.wavelenghts = null;
		this.type = null;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.concurrent.Callable#call()
	 */
	@Override
	public Boolean call() throws Exception {

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
					int diffStartIdx = rnd.nextInt(tmpDiffEvents.length - 2);

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
					Mat sameHist1 = new Mat();
					this.histoProducer.getHist(sameHist1, sameEvents[0], dims,
							true);
					Mat sameHist2 = new Mat();
					this.histoProducer.getHist(sameHist2, sameEvents[1], dims,
							true);
					Mat sameHist3 = new Mat();
					this.histoProducer.getHist(sameHist3, sameEvents[2], dims,
							false);
					Mat sameHist4 = new Mat();
					this.histoProducer.getHist(sameHist4, sameEvents[3], dims,
							false);
					sameEvents = null;

					// Different events will have 2 events in it this is
					// guaranteed by the constructor previous checks
					Mat diffHist1 = new Mat();
					this.histoProducer.getHist(diffHist1, diffEvents[0], dims,
							false);
					Mat diffHist2 = new Mat();
					this.histoProducer.getHist(diffHist2, diffEvents[1], dims,
							false);
					diffEvents = null;

					double sameVal1, sameVal2, difVal1, difVal2;
					int compareMethod = this.paramCombos[1];
					sameVal1 = this.compareHistogram(sameHist2, sameHist3,
							compareMethod);
					difVal1 = this.compareHistogram(sameHist1, diffHist1,
							compareMethod);

					Mat sameSubHist1 = new Mat();
					Core.subtract(sameHist1, sameHist2, sameSubHist1);
					// release and cleanup
					sameHist1.release();
					sameHist1 = null;
					sameHist2.release();
					sameHist2 = null;

					Mat sameSubHist2 = new Mat();
					Core.subtract(sameHist3, sameHist4, sameSubHist2);
					sameVal2 = this.compareHistogram(sameSubHist1,
							sameSubHist2, compareMethod);

					// release and cleanup
					sameHist3.release();
					// sameHist3 = null;
					sameHist4.release();
					// sameHist4 = null;
					sameSubHist2.release();
					// sameSubHist2 = null;

					Mat diffSubHist1 = new Mat();
					Core.subtract(diffHist1, diffHist2, diffSubHist1);
					difVal2 = this.compareHistogram(sameSubHist1, diffSubHist1,
							compareMethod);

					// release and cleanup
					diffHist1.release();
					// diffHist1 = null;
					diffHist2.release();
					// diffHist2 = null;
					sameSubHist1.release();
					// sameSubHist1 = null;
					diffSubHist1.release();
					// diffSubHist1 = null;

					double[] tmpCompVals = new double[4];
					tmpCompVals[0] = sameVal1;
					tmpCompVals[1] = difVal1;
					tmpCompVals[2] = sameVal2;
					tmpCompVals[3] = difVal2;
					comparValues.add(tmpCompVals);
					tmpCompVals = null;
				}
			}

			tmpSameEvents = null;
			if (i % (this.tracks.length / 4) == 0)
				System.gc();
		}

		this.calcFStatAndSave(comparValues);
		comparValues.clear();
		comparValues = null;
		rnd = null;

		System.gc();
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
		cat1List.clear();
		cat1List = null;
		sameCat1 = null;
		diffCat1 = null;

		double fVal2 = ow.anovaFValue(cat2List);
		cat2List.clear();
		cat2List = null;
		sameCat2 = null;
		diffCat2 = null;

		ow = null;

		if (Double.isNaN(fVal1)) {
			fVal1 = 0;
		}

		if (Double.isNaN(fVal2)) {
			fVal2 = 0;
		}

		try {
			// paramCombos[0] is the id in the database for the combonation
			// being inserted
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

}
