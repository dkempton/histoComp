/**
 * 
 * File: TrackingFactory.java is the class factory class for 
 * many different works in the tracking domain 
 * for the Data Mining Lab at Georgia State University.  
 * 
 * @author Dustin Kempton
 * @version 05/15/2015 
 * @Owner Data Mining Lab, Georgia State University
 */
package edu.gsu.dmlab.tracking;

import java.io.File;
import java.sql.SQLException;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;

import snaq.db.DBPoolDataSource;
import edu.gsu.dmlab.ObjectFactory;
import edu.gsu.dmlab.databases.interfaces.IImageDBConnection;
import edu.gsu.dmlab.datatypes.interfaces.ITrack;
import edu.gsu.dmlab.exceptions.InvalidConfigException;
import edu.gsu.dmlab.tracking.databases.CallableCalcAndSaveHistDistances;
import edu.gsu.dmlab.tracking.databases.HomeTrackingDBConnection;
import edu.gsu.dmlab.tracking.databases.interfaces.ITrackingDBConnection;
import edu.gsu.dmlab.tracking.interfaces.ITrackingFactory;

public class TrackingFactory implements ITrackingFactory {

	DBPoolDataSource imageDBPoolSourc = null;
	DBPoolDataSource trackingDBPoolSourc = null;
	IImageDBConnection imageDB = null;
	ITrackingDBConnection trackingDB = null;
	ListeningExecutorService executor = null;

	int imageCacheSize;
	int maxThreads;
	int pagSize;

	int[] wavelengths = null;

	public TrackingFactory() throws InvalidConfigException, SQLException {

		this.config();
		this.imageDB = ObjectFactory.getImageDBConnection(
				this.imageDBPoolSourc, this.imageCacheSize);
		this.trackingDB = new HomeTrackingDBConnection(this.trackingDBPoolSourc);

		this.wavelengths = this.trackingDB.getWavelengths();
		this.executor = MoreExecutors.listeningDecorator(Executors
				.newFixedThreadPool(this.maxThreads));
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see edu.gsu.dmlab.tracking.interfaces.ITrackingFactory#
	 * getCallableCalcAndSaveHistDistances(java.lang.String,
	 * edu.gsu.dmlab.datatypes.interfaces.IEvent[],
	 * edu.gsu.dmlab.datatypes.interfaces.IEvent[])
	 */
	@Override
	public Callable<Boolean> getCallableCalcAndSaveHistDistances(String type,
			ITrack[] tracks, int[] paramCombos) {
		return new CallableCalcAndSaveHistDistances(this.imageDB,
				this.trackingDB, type, tracks, paramCombos, this.wavelengths);
	}

	@Override
	public int[][] getParamCombos(int offset, int limit) {
		try {
			return this.trackingDB.getParamCombos(offset, limit);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public HistogramComparison getHistoComp(String type) {
		return new HistogramComparison(this, this.executor, type,
				this.maxThreads, this.pagSize);
	}

	@Override
	public ITrack[] getTrackedResults(String type, String fileLocation, int span) {
		return ObjectFactory.getTrackedResults(type, fileLocation, span);
	}

	@Override
	public ITrackingDBConnection getTrackingDBConnection() {
		return this.trackingDB;
	}

	// /////////////////////////////////////////////////////////////////////////////////
	// Start of private methods
	// ////////////////////////////////////////////////////////////////////////////////
	private void config() throws InvalidConfigException {
		try {
			DocumentBuilderFactory fctry = DocumentBuilderFactory.newInstance();
			Document doc;
			String fileLoc = System.getProperty("user.dir") + File.separator
					+ "config" + File.separator + "trackingHistoComp.cfg.xml";
			DocumentBuilder bldr = fctry.newDocumentBuilder();
			doc = bldr.parse(new File(fileLoc));
			doc.getDocumentElement().normalize();

			Element root = doc.getDocumentElement();
			NodeList ndLst = root.getChildNodes();
			for (int i = 0; i < ndLst.getLength(); i++) {
				Node nde = ndLst.item(i);
				if (nde.getNodeType() == Node.ELEMENT_NODE) {
					String ndName = nde.getNodeName();
					if (ndName.compareTo("imagepool") == 0) {
						this.imageDBPoolSourc = this.getPoolSourc(nde
								.getChildNodes());
					} else if (ndName.compareTo("trackingpool") == 0) {
						this.trackingDBPoolSourc = this.getPoolSourc(nde
								.getChildNodes());
					}
				}
			}

			this.getRestConfig(ndLst);

		} catch (Exception e) {
			throw new InvalidConfigException(e.getMessage());
		}

	}

	private void getRestConfig(NodeList ndLst) {

		for (int i = 0; i < ndLst.getLength(); i++) {
			Node nde = ndLst.item(i);
			if (nde.getNodeType() == Node.ELEMENT_NODE) {
				String ndName = nde.getNodeName();
				switch (ndName) {
				case "imagedbcache":
					this.imageCacheSize = Integer.parseInt(this.getAttrib(nde,
							"max"));
					break;
				case "threadcount":
					this.maxThreads = Integer.parseInt(this.getAttrib(nde,
							"max"));
					break;
				case "pagesize":
					this.pagSize = Integer.parseInt(this
							.getAttrib(nde, "value"));
				}
			}
		}
	}

	private DBPoolDataSource getPoolSourc(NodeList ndLst) {
		DBPoolDataSource dbPoolSourc = null;
		dbPoolSourc = new DBPoolDataSource();
		for (int i = 0; i < ndLst.getLength(); i++) {
			Node nde = ndLst.item(i);
			if (nde.getNodeType() == Node.ELEMENT_NODE) {
				String ndName = nde.getNodeName();
				switch (ndName) {
				case "poolname":
					dbPoolSourc.setName(this.getAttrib(nde, "value"));
					break;
				case "description":
					dbPoolSourc.setDescription(this.getAttrib(nde, "value"));
					break;
				case "ideltimeout":
					String idlStr = this.getAttrib(nde, "value");
					dbPoolSourc.setIdleTimeout(Integer.parseInt(idlStr));
					break;
				case "minpool":
					String minStr = this.getAttrib(nde, "value");
					dbPoolSourc.setMinPool(Integer.parseInt(minStr));
					break;
				case "maxpool":
					String maxStr = this.getAttrib(nde, "value");
					dbPoolSourc.setMaxPool(Integer.parseInt(maxStr));
					break;
				case "maxsize":
					String maxszStr = this.getAttrib(nde, "value");
					dbPoolSourc.setMaxSize(Integer.parseInt(maxszStr));
					break;
				case "username":
					dbPoolSourc.setUser(this.getAttrib(nde, "value"));
					break;
				case "password":
					dbPoolSourc.setPassword(this.getAttrib(nde, "value"));
					break;
				case "validationquery":
					dbPoolSourc
							.setValidationQuery(this.getAttrib(nde, "value"));
					break;
				case "driverclass":
					dbPoolSourc
							.setDriverClassName(this.getAttrib(nde, "value"));
					break;
				case "url":
					dbPoolSourc.setUrl(this.getAttrib(nde, "value"));
					break;
				default:
					System.out.print("Unknown Element: ");
					System.out.println(ndName);
				}
			}
		}
		return dbPoolSourc;
	}

	private String getAttrib(Node prntNde, String attName) {
		StringBuffer buf = new StringBuffer("");
		boolean isSet = false;
		if (prntNde.hasAttributes()) {
			NamedNodeMap ndeMp = prntNde.getAttributes();
			for (int i = 0; i < ndeMp.getLength(); i++) {
				Node nde = ndeMp.item(i);
				if (nde.getNodeName().compareTo(attName) == 0) {
					buf.append(nde.getNodeValue());
					isSet = true;
					break;
				}
			}
		}

		if (!isSet) {
			return "";
		} else {
			return buf.toString();
		}
	}

}
