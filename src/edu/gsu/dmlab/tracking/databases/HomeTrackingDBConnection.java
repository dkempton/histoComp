/**
 * 
 * File: TrackingDBConnection.java is the class to access the tracking database 
 * for the Data Mining Lab at Georgia State University.  
 * 
 * @author Dustin Kempton
 * @version 05/12/2015 
 * @Owner Data Mining Lab, Georgia State University
 */
package edu.gsu.dmlab.tracking.databases;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.sql.DataSource;

import edu.gsu.dmlab.tracking.databases.interfaces.ITrackingDBConnection;

public class HomeTrackingDBConnection implements ITrackingDBConnection {
	DataSource dsourc = null;

	public HomeTrackingDBConnection(DataSource dsourc) {
		if (dsourc == null)
			throw new IllegalArgumentException(
					"DataSource cannot be null in HomeTrackingDBConnection constructor.");
		this.dsourc = dsourc;
	}

	public void populateParamCombos() throws SQLException {

		ArrayList<Future<?>> futures = new ArrayList<Future<?>>();
		ExecutorService exService = Executors.newFixedThreadPool(6);
		futures.add(exService.submit(new Runnable() {
			@Override
			public void run() {
				Connection con = null;
				try {
					con = dsourc.getConnection();
					con.setAutoCommit(false);
					String insString1 = "INSERT INTO "
							+ " param_combos (measure, wave1, param1 ) VALUES (?,?,?);";

					PreparedStatement saveParamsStmt1 = con
							.prepareStatement(insString1);
					for (int measureNum = 1; measureNum < 5; measureNum++) {
						for (int wave1 = 1; wave1 < 10; wave1++) {
							for (int param1 = 1; param1 < 11; param1++) {
								saveParamsStmt1.setInt(1, measureNum);
								saveParamsStmt1.setInt(2, wave1);
								saveParamsStmt1.setInt(3, param1);
								saveParamsStmt1.addBatch();
							}
						}
					}
					saveParamsStmt1.executeBatch();
					con.commit();
					System.out.println("Single Done.");
				} catch (SQLException e) {
					e.printStackTrace();
				} finally {
					if (con != null) {
						try {
							con.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}));

		// submit two param/wave as runnable
		futures.add(exService.submit(new Runnable() {

			@Override
			public void run() {

				Connection con = null;
				try {
					con = dsourc.getConnection();
					con.setAutoCommit(false);
					String insString2 = "INSERT INTO "
							+ " param_combos (measure, wave1, param1, wave2, param2) VALUES (?,?,?,?,?);";

					PreparedStatement saveParamsStmt2 = con
							.prepareStatement(insString2);
					for (int measureNum = 1; measureNum < 5; measureNum++) {
						for (int wave1 = 1; wave1 < 10; wave1++) {
							for (int param1 = 1; param1 < 11; param1++) {

								// next set of wave/param
								for (int wave2 = wave1 + 1; wave2 < 10; wave2++) {
									for (int param2 = 1; param2 < 11; param2++) {
										saveParamsStmt2.setInt(1, measureNum);
										saveParamsStmt2.setInt(2, wave1);
										saveParamsStmt2.setInt(3, param1);
										saveParamsStmt2.setInt(4, wave2);
										saveParamsStmt2.setInt(5, param2);
										saveParamsStmt2.addBatch();

									}
								}
							}
						}
					}
					saveParamsStmt2.executeBatch();
					con.commit();
					System.out.println("Double Done.");
				} catch (SQLException e) {
					e.printStackTrace();
				} finally {
					if (con != null) {
						try {
							con.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}));

		for (int i = 1; i < 5; i++) {
			final int measureNum = i;
			futures.add(exService.submit(new Runnable() {
				@Override
				public void run() {
					Connection con = null;
					try {
						con = dsourc.getConnection();
						con.setAutoCommit(false);
						String insString3 = "INSERT INTO "
								+ " param_combos (measure, wave1, param1, wave2, param2, wave3, param3) VALUES (?,?,?,?,?,?,?);";

						PreparedStatement saveParamsStmt3 = con
								.prepareStatement(insString3);
						for (int wave1 = 1; wave1 < 10; wave1++) {
							for (int param1 = 1; param1 < 11; param1++) {

								// next set of wave/param
								for (int wave2 = wave1 + 1; wave2 < 10; wave2++) {
									for (int param2 = 1; param2 < 11; param2++) {

										// next set of wave/param
										for (int wave3 = wave2 + 1; wave3 < 10; wave3++) {
											for (int param3 = 1; param3 < 11; param3++) {

												saveParamsStmt3.setInt(1,
														measureNum);
												saveParamsStmt3
														.setInt(2, wave1);
												saveParamsStmt3.setInt(3,
														param1);
												saveParamsStmt3
														.setInt(4, wave2);
												saveParamsStmt3.setInt(5,
														param2);
												saveParamsStmt3
														.setInt(6, wave3);
												saveParamsStmt3.setInt(7,
														param3);
												saveParamsStmt3.addBatch();

											}
										}
									}
								}
							}
						}
						saveParamsStmt3.executeBatch();
						con.commit();
						System.out.println("Tripple Done.");
					} catch (SQLException e) {
						e.printStackTrace();
					} finally {
						if (con != null) {
							try {
								con.close();
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}));
		}

		for (int i = 1; i < 5; i++) {

			// create a new runnable task for each measurement
			final int measureNum = i;
			futures.add(exService.submit(new Runnable() {
				@Override
				public void run() {
					Connection con = null;
					try {
						con = dsourc.getConnection();
						con.setAutoCommit(false);
						String insString4 = "INSERT INTO "
								+ " param_combos (measure, wave1, param1, wave2, param2, wave3, param3, wave4, param4)"
								+ " VALUES (?,?,?,?,?,?,?,?,?);";

						PreparedStatement saveParamsStmt4 = con
								.prepareStatement(insString4);

						for (int wave1 = 1; wave1 < 10; wave1++) {
							for (int param1 = 1; param1 < 11; param1++) {

								// next set of wave/param
								for (int wave2 = wave1 + 1; wave2 < 10; wave2++) {
									for (int param2 = 1; param2 < 11; param2++) {

										// next set of wave/param
										for (int wave3 = wave2 + 1; wave3 < 10; wave3++) {
											for (int param3 = 1; param3 < 11; param3++) {

												// next set of wave/param
												for (int wave4 = wave3 + 1; wave4 < 10; wave4++) {
													for (int param4 = 1; param4 < 11; param4++) {

														saveParamsStmt4.setInt(
																1, measureNum);
														saveParamsStmt4.setInt(
																2, wave1);
														saveParamsStmt4.setInt(
																3, param1);
														saveParamsStmt4.setInt(
																4, wave2);
														saveParamsStmt4.setInt(
																5, param2);
														saveParamsStmt4.setInt(
																6, wave3);
														saveParamsStmt4.setInt(
																7, param3);
														saveParamsStmt4.setInt(
																8, wave4);
														saveParamsStmt4.setInt(
																9, param4);

														saveParamsStmt4
																.addBatch();

													}
												}
											}
										}
									}
								}
							}
						}
						saveParamsStmt4.executeBatch();
						con.commit();
						System.out.println("Quad Done." + measureNum);
					} catch (SQLException e) {
						e.printStackTrace();
					} finally {
						if (con != null) {
							try {
								con.close();
							} catch (SQLException e) {
								e.printStackTrace();
							}
						}
					}
				}
			}));
		}

		while (!futures.isEmpty()) {
			Future<?> ft = futures.get(0);
			if (!ft.isDone()) {
				try {
					ft.get(300, TimeUnit.SECONDS);
					System.out.println("Future signaled.");
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TimeoutException e) {
					System.out.println("Timeout on wait.");
				}
			} else {
				futures.remove(0);
			}
		}

		exService.shutdown();
		try {
			System.out.println("Waiting for termination.");
			exService.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		System.out.println("All is done.");
	}

	public int[][] getParamCombos(int offset, int limit) throws SQLException {
		Connection con = null;
		int[][] returnVals = null;
		try {
			con = this.dsourc.getConnection();
			con.setAutoCommit(true);

			String selString = "SELECT * FROM param_combos limit ?,?;";

			PreparedStatement selParamsStmt = con.prepareStatement(selString);
			selParamsStmt.setInt(1, offset);
			selParamsStmt.setInt(2, limit);

			ResultSet rs = selParamsStmt.executeQuery();
			ArrayList<int[]> paramAndIdList = new ArrayList<int[]>();
			while (rs.next()) {
				int count = 0;
				int[] tmpVals = new int[9];
				for (int i = 1; i < tmpVals.length + 1; i++) {
					int tmpVal = rs.getInt(i);
					if (tmpVal != 0) {
						tmpVals[i - 1] = tmpVal;
						count++;
					}
				}

				int[] actualVals = new int[count];
				for (int j = 0; j < actualVals.length; j++) {
					actualVals[j] = tmpVals[j];
				}
				paramAndIdList.add(actualVals);
			}

			returnVals = new int[paramAndIdList.size()][];
			paramAndIdList.toArray(returnVals);

		} finally {
			if (con != null) {
				con.close();
			}
		}
		return returnVals;
	}

	public int[] getWavelengths() throws SQLException {
		Connection con = null;
		int[] returnVals = null;
		try {
			con = this.dsourc.getConnection();
			con.setAutoCommit(true);

			String selString = "SELECT * FROM wavelength;";

			PreparedStatement selWaveStmt = con.prepareStatement(selString);

			ResultSet rs = selWaveStmt.executeQuery();
			ArrayList<Integer> waveAndIdList = new ArrayList<Integer>();
			while (rs.next()) {
				int val = rs.getInt(2);
				waveAndIdList.add(val);
			}

			returnVals = new int[waveAndIdList.size()];
			for (int i = 0; i < returnVals.length; i++) {
				returnVals[i] = waveAndIdList.get(i);
			}

		} finally {
			if (con != null) {
				con.close();
			}
		}
		return returnVals;
	}

	public void insertParamHistVals(String type, int id, double fVal1,
			double fVal2) throws SQLException {
		Connection con = null;
		try {
			con = this.dsourc.getConnection();
			con.setAutoCommit(true);

			String insString1 = "INSERT INTO "
					+ type.toLowerCase()
					+ "_hist_vals (param_combo_id, f_val1, f_val2) VALUES (?,?,?);";
			PreparedStatement saveValsStmt1 = con.prepareStatement(insString1);

			saveValsStmt1.setInt(1, id);
			saveValsStmt1.setDouble(2, fVal1);
			saveValsStmt1.setDouble(3, fVal2);

			saveValsStmt1.executeUpdate();
			
		} finally {
			if (con != null) {
				con.close();
			}
		}
	}
}
