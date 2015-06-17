/*
 * 
 * File: ITrackingDBConnection.java is the public interface for tracking database connectons for any 
 * project that depends on the tracking database created for the Data Mining Lab at
 * Georgia State University
 * 
 * @author Dustin Kempton
 * @version 05/15/2015 
 * @Owner Data Mining Lab, Georgia State University
 * 
 */

package edu.gsu.dmlab.tracking.databases.interfaces;

import java.sql.SQLException;

public interface ITrackingDBConnection {

	public int[][] getParamCombos(int offset, int limit) throws SQLException;

	public int[] getWavelengths() throws SQLException;

	public void insertParamHistVals(String type, int id, double fVal1,
			double fVal2) throws SQLException;
}
