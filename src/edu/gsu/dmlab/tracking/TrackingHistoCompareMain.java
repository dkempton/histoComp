package edu.gsu.dmlab.tracking;

import java.sql.SQLException;

import org.opencv.core.Core;

import edu.gsu.dmlab.datatypes.interfaces.ITrack;
import edu.gsu.dmlab.exceptions.InvalidConfigException;

public class TrackingHistoCompareMain {

	public static void main(String[] args) throws SQLException,
			InvalidConfigException {

		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		String type1 = "CH";
		String type2 = "AR";
		String fileLocation = "F:\\files\\tmp1\\1Mo";
		int span = 600;

		TrackingFactory factory = new TrackingFactory();
		// HomeTrackingDBConnection tdb = (HomeTrackingDBConnection) factory
		// .getTrackingDBConnection();
		// tdb.populateParamCombos();
		{
			HistogramComparison hc = factory.getHistoComp(type1);
			ITrack[] tracks = factory.getTrackedResults(type1, fileLocation,
					span);
			hc.run(tracks);
		}
		{
			HistogramComparison hc = factory.getHistoComp(type2);
			ITrack[] tracks = factory.getTrackedResults(type2, fileLocation,
					span);
			hc.run(tracks);
		}

	}

}
