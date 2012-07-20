/*
 * Copyright (C) 2010 - 2012 Jenia Software.
 *
 * This file is part of Sinekarta
 *
 * Sinekarta is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Sinekarta is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 */
package org.sinekarta.alfresco.util;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.sinekarta.alfresco.exception.ConfigurationException;

/**
 * this class is a singleton! use Configuration.getInstance() to get current instance
 * Configuration is read from alfresco/extension/sinekarta-repository.properties in classloader
 * The file found in classes folder is the last read
 * 
 * 
 * @author andrea.tessaro
 *
 */
public class Configuration {

	private static Logger tracer = Logger.getLogger(Configuration.class);

	private static final String CONFIGURATION = "alfresco/extension/sinekarta-repository.properties";
	
	public static final String SINEKARTA_DIR = "SINEKARTA_DIR";
	public static final String PDFA_SUFFIX = "PDFA_SUFFIX";
	public static final String ACTION_NAME_DOCUMENT_TO_PDFA = "ACTION_NAME_DOCUMENT_TO_PDFA";
	public static final String SIGNED_SUFFIX = "SIGNED_SUFFIX";
	public static final String MARKED_SUFFIX = "MARKED_SUFFIX";
	public static final String USER_SPACE_TEMPORARY_FOLDER = "USER_SPACE_TEMPORARY_FOLDER";
	public static final String LUCENE_PATH_ARCHIVIO = "LUCENE_PATH_ARCHIVIO";
	public static final String LUCENE_PATH_AE = "LUCENE_PATH_AE";
	public static final String ELENCO_LINGUE_OCR = "ELENCO_LINGUE_OCR";
	public static final String LINGUA_DEFAULT_OCR = "LINGUA_DEFAULT_OCR";
	public static final String PROCESSO_OCR = "PROCESSO_OCR";
	public static final String DEFAULT_SMARTCARD_DRIVER = "DEFAULT_SMARTCARD_DRIVER";
	public static final String SMARTCARD_DRIVERS = "SMARTCARD_DRIVERS";
	public static final String TSA_URL = "TSA_URL";
	public static final String TSA_USER = "TSA_USER";
	public static final String TSA_PASSWORD = "TSA_PASSWORD";
	public static final String OCR_METADATA_LENGHT = "OCR_METADATA_LENGHT";
	
	private static Configuration mySelf = new Configuration();
	
	private String sinekartaDir;
	private String luceneArchivePath;
	private String luceneAEPath;
	private String[] elencoLingueOcr;
	private Map<String,String> mappaLingueOcr;
	private Map<String,String> mappaInversaLingueOcr;
	private String linguaDefaultOcr;
	private String processoOcr;
	private int ocrMetadataLenght;
	private String defaultSmartcardDriver;
	private Map<String,String> mappaSmartcardDrivers;
	private String tsaUrl;
	private String tsaUser;
	private String tsaPassword;
	private String pdfaSuffix;
	private String documentToPDFAActionName;
	private String signedSuffix;
	private String markedSuffix;
	private String userSpaceTemporaryFolder;
	
	private Configuration() {
		Properties properties = new Properties();
		// find all CONFIGURATION files (in all classpath)
		Enumeration<URL> enumerationURLconfiguration = null;
		try {
			enumerationURLconfiguration = this.getClass().getClassLoader().getResources(CONFIGURATION);
		} catch (Exception e) {
			String msg = "unable to load configuration urls ("+CONFIGURATION+")";
			tracer.error(msg,e);
			throw new ConfigurationException(msg,e);
		}
		// invert the list of url, so i can load files stored in WEB-INF/classes after contained in jar
		List<URL> all = new ArrayList<URL>();
		URL urlConfiguration = null;
		while (enumerationURLconfiguration.hasMoreElements()) {
			urlConfiguration = (URL)enumerationURLconfiguration.nextElement();
			all.add(0,urlConfiguration);
		}
		// load inverted list
		for (URL url : all) {
			try {
				properties.load(url.openStream());
			} catch (IOException e) {
				String msg = "unable to load configuration urls ("+urlConfiguration+")";
				tracer.error(msg,e);
				throw new ConfigurationException(msg,e);
			}
		}
		
		// read each property and convert in the corresponding configuration attribute
		if (properties.getProperty(SINEKARTA_DIR)==null) throw new ConfigurationException("property " + SINEKARTA_DIR + " not found.");
		if (properties.getProperty(PDFA_SUFFIX)==null) throw new ConfigurationException("property " + PDFA_SUFFIX + " not found.");
		if (properties.getProperty(ACTION_NAME_DOCUMENT_TO_PDFA)==null) throw new ConfigurationException("property " + ACTION_NAME_DOCUMENT_TO_PDFA + " not found.");
		if (properties.getProperty(SIGNED_SUFFIX)==null) throw new ConfigurationException("property " + SIGNED_SUFFIX + " not found.");
		if (properties.getProperty(MARKED_SUFFIX)==null) throw new ConfigurationException("property " + MARKED_SUFFIX + " not found.");
		if (properties.getProperty(USER_SPACE_TEMPORARY_FOLDER)==null) throw new ConfigurationException("property " + USER_SPACE_TEMPORARY_FOLDER + " not found.");
		if (properties.getProperty(LUCENE_PATH_ARCHIVIO)==null) throw new ConfigurationException("property " + LUCENE_PATH_ARCHIVIO + " not found.");
		if (properties.getProperty(LUCENE_PATH_AE)==null) throw new ConfigurationException("property " + LUCENE_PATH_AE + " not found.");
		if (properties.getProperty(ELENCO_LINGUE_OCR)==null) throw new ConfigurationException("property " + ELENCO_LINGUE_OCR + " not found.");
		if (properties.getProperty(LINGUA_DEFAULT_OCR)==null) throw new ConfigurationException("property " + LINGUA_DEFAULT_OCR + " not found.");
		if (properties.getProperty(DEFAULT_SMARTCARD_DRIVER)==null) throw new ConfigurationException("property " + DEFAULT_SMARTCARD_DRIVER + " not found.");
		if (properties.getProperty(SMARTCARD_DRIVERS)==null) throw new ConfigurationException("property " + SMARTCARD_DRIVERS + " not found.");
		if (properties.getProperty(PROCESSO_OCR)==null) throw new ConfigurationException("property " + PROCESSO_OCR + " not found.");
		if (properties.getProperty(OCR_METADATA_LENGHT)==null) throw new ConfigurationException("property " + OCR_METADATA_LENGHT + " not found.");
		if (properties.getProperty(TSA_URL)==null) throw new ConfigurationException("property " + TSA_URL + " not found.");
		if (properties.getProperty(TSA_USER)==null) throw new ConfigurationException("property " + TSA_USER + " not found.");
		if (properties.getProperty(TSA_PASSWORD)==null) throw new ConfigurationException("property " + TSA_PASSWORD + " not found.");
		try {
			sinekartaDir = properties.getProperty(SINEKARTA_DIR);
			pdfaSuffix = properties.getProperty(PDFA_SUFFIX);
			documentToPDFAActionName = properties.getProperty(ACTION_NAME_DOCUMENT_TO_PDFA);
			signedSuffix = properties.getProperty(SIGNED_SUFFIX);
			markedSuffix = properties.getProperty(MARKED_SUFFIX);
			userSpaceTemporaryFolder = properties.getProperty(USER_SPACE_TEMPORARY_FOLDER);
			luceneArchivePath = properties.getProperty(LUCENE_PATH_ARCHIVIO);
			luceneAEPath = properties.getProperty(LUCENE_PATH_AE);
			elencoLingueOcr = properties.getProperty(ELENCO_LINGUE_OCR).split(",");
			mappaLingueOcr=new HashMap<String, String>();
			mappaInversaLingueOcr=new HashMap<String, String>();
			linguaDefaultOcr = properties.getProperty(LINGUA_DEFAULT_OCR);
			boolean found=false;
			for (int i=0;i<elencoLingueOcr.length;i++) {
				String[] c = elencoLingueOcr[i].split(":");
				elencoLingueOcr[i]=c[1];
				mappaLingueOcr.put(c[1], c[0]);
				mappaInversaLingueOcr.put(c[0], c[1]);
				if (linguaDefaultOcr.equals(c[0])) {
					linguaDefaultOcr = c[1];
					found=true;
				}
				if (linguaDefaultOcr.equals(c[1])) {
					found=true;
				}
			}
			if (!found) {
				throw new ConfigurationException("Invalid field on " + CONFIGURATION + ", " + LINGUA_DEFAULT_OCR + "("+linguaDefaultOcr+") not found on " + ELENCO_LINGUE_OCR);
			}
			processoOcr = properties.getProperty(PROCESSO_OCR);
			try {
				ocrMetadataLenght = Integer.parseInt(properties.getProperty(OCR_METADATA_LENGHT));
			} catch (Exception e) {
				throw new ConfigurationException("Invalid field on " + CONFIGURATION + ", field " + OCR_METADATA_LENGHT + " = " + properties.getProperty(OCR_METADATA_LENGHT) + " : invalid value");
			}
			defaultSmartcardDriver = properties.getProperty(DEFAULT_SMARTCARD_DRIVER);
			mappaSmartcardDrivers=new HashMap<String, String>();
			String[] smartcardDrivers = properties.getProperty(SMARTCARD_DRIVERS).split(",");
			found=false;
			for (int i=0;i<smartcardDrivers.length;i++) {
				String[] c = smartcardDrivers[i].split(":");
				mappaSmartcardDrivers.put(c[0], c[1]);
				if (defaultSmartcardDriver.equals(c[0])) {
					found=true;
				}
			}
			if (!found) {
				throw new ConfigurationException("Invalid field on " + CONFIGURATION + ", " + DEFAULT_SMARTCARD_DRIVER + "("+defaultSmartcardDriver+") not found on " + SMARTCARD_DRIVERS);
			}
			tsaUrl = properties.getProperty(TSA_URL);
			tsaUser = properties.getProperty(TSA_USER);
			tsaPassword = properties.getProperty(TSA_PASSWORD);
		} catch (Exception e) {
			String msg = "unable to parse sinekarta-repository.properties (" + e.getClass().getName() + " : " + e.getMessage() + ")";
			tracer.error(msg,e);
			throw new ConfigurationException(msg,e);
		}

	}
	
	/**
	 * this is a singleton!!!
	 * this method will return the instance of the utility
	 * 
	 * @return
	 */
	public static Configuration getInstance() {
		return mySelf;
	}

	public String getLuceneArchivePath() {
		return luceneArchivePath;
	}

	public String getLuceneAEPath() {
		return luceneAEPath;
	}

	public String getLinguaDefaultOcr() {
		return linguaDefaultOcr;
	}

	public String getTsaUser() {
		return tsaUser;
	}

	public String getTsaPassword() {
		return tsaPassword;
	}

	public String getTsaUrl() {
		return tsaUrl;
	}

	public String[] getElencoLingueOcr() {
		return elencoLingueOcr;
	}

	public Map<String, String> getMappaLingueOcr() {
		return mappaLingueOcr;
	}

	public String getDefaultSmartcardDriver() {
		return defaultSmartcardDriver;
	}

	public Map<String, String> getMappaSmartcardDrivers() {
		return mappaSmartcardDrivers;
	}

	public String getProcessoOcr() {
		return processoOcr;
	}

	public String getSinekartaDir() {
		return sinekartaDir;
	}

	public void setLuceneArchivePath(String luceneArchivePath) {
		this.luceneArchivePath = luceneArchivePath;
	}

	public void setLuceneAEPath(String luceneAEPath) {
		this.luceneAEPath = luceneAEPath;
	}

	public Map<String, String> getMappaInversaLingueOcr() {
		return mappaInversaLingueOcr;
	}

	public String getPdfaSuffix() {
		return pdfaSuffix;
	}

	public String getUserSpaceTemporaryFolder() {
		return userSpaceTemporaryFolder;
	}

	public String getSignedSuffix() {
		return signedSuffix;
	}

	public int getOcrMetadataLenght() {
		return ocrMetadataLenght;
	}

	public String getMarkedSuffix() {
		return markedSuffix;
	}

	public String getDocumentToPDFAActionName() {
		return documentToPDFAActionName;
	}

}
