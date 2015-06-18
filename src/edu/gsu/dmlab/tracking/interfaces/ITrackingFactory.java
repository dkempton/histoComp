package edu.gsu.dmlab.tracking.interfaces;

import java.util.concurrent.Callable;

import edu.gsu.dmlab.datatypes.interfaces.ITrack;
import edu.gsu.dmlab.tracking.HistogramComparison;
import edu.gsu.dmlab.tracking.databases.interfaces.ITrackingDBConnection;

public interface ITrackingFactory {
	public Callable<Boolean> getCallableCalcAndSaveHistDistances(String type,
			ITrack[] tracks, int[] paramCombos);

	public HistogramComparison getHistoComp(String type);

	public ITrack[] getTrackedResults(String type, String fileLocation, int span);

	public ITrackingDBConnection getTrackingDBConnection();

	public int[][] getParamCombos(String type, int limit);
}
