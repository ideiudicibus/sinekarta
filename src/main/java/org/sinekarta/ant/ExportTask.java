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
package org.sinekarta.ant;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.alfresco.webservice.content.Content;
import org.alfresco.webservice.content.ContentServiceSoapBindingStub;
import org.alfresco.webservice.repository.QueryResult;
import org.alfresco.webservice.repository.RepositoryServiceSoapBindingStub;
import org.alfresco.webservice.types.ClassDefinition;
import org.alfresco.webservice.types.NamedValue;
import org.alfresco.webservice.types.NodeDefinition;
import org.alfresco.webservice.types.Predicate;
import org.alfresco.webservice.types.Query;
import org.alfresco.webservice.types.Reference;
import org.alfresco.webservice.types.ResultSet;
import org.alfresco.webservice.types.Store;
import org.alfresco.webservice.util.Constants;
import org.alfresco.webservice.util.WebServiceException;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Task;
import org.sinekarta.ant.util.AuthenticationUtils;
import org.sinekarta.ant.util.Configuration;
import org.sinekarta.ant.util.ResultSetRowWrapper;
import org.sinekarta.ant.util.WebServiceFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class ExportTask extends Task {
	
	private static final String METADATI_XML = "_metadati.xml";
	private static final String ALFRESCO_MODEL_CONTENT_CONTENT = "{http://www.alfresco.org/model/content/1.0}content";
	private static final String SINEKARTA_MODEL_CONTENT_ARCHIVE = "{http://www.sinekarta.org/alfresco/model/content/1.0}archive";
	private static final String ALFRESCO_MODEL_CONTENT_FOLDER = "{http://www.alfresco.org/model/content/1.0}folder";
	public static String NAME_COLUMN_NAME = "{http://www.alfresco.org/model/content/1.0}name";
	public static String DESCRIPTION_COLUMN_NAME = "{http://www.alfresco.org/model/content/1.0}description";
	public static String UUID_COLUMN_NAME = "{http://www.alfresco.org/model/system/1.0}node-uuid";

	private String host; 
	
	private int port; 
	
	private int timeoutMilliseconds; 
	
	private String userId; 
	
	private String password; 
	
	private String alfrescoPath;
	
	private String localPath;

    // The method executing the task
    public void execute() throws BuildException {
        log("+++ Export start");
        try {
        	// creo la configurazione di Alfresco
	        Configuration config = new Configuration(host, port, timeoutMilliseconds);
        	// effettuo la connessione e la login
			AuthenticationUtils.startSession(config, userId, password);
        	// devo fare delle query sul repository, ottengo il servizio corrispondente
			RepositoryServiceSoapBindingStub repositoryService = WebServiceFactory.getRepositoryService();
			// stor alfresco su cui vengono fatte le query
			Store theStore = new Store(Constants.WORKSPACE_STORE, "SpacesStore");
			// preparo la query, sempre figlia di company home
	        Query query = new Query(Constants.QUERY_LANG_LUCENE, "PATH:\""+"/app:company_home"+alfrescoPath+"\"");
			// la eseguo
	        QueryResult queryResult = repositoryService.query(theStore, query, false);
	        ResultSet resultSet = queryResult.getResultSet();
	        // ottengo il nodo root da scaricare e lo wrappo
	        ResultSetRowWrapper root = new ResultSetRowWrapper(resultSet.getRows(0));
	        // stampa nodo ramo
	        log("esplorazione repository alfresco a partire da : " + root);
			// creazione directory su local file system
			File localRoot = createLocalDir(localPath);			
			// esplorazione ricorsiva di tutto l'albero
			explore(theStore, root, localRoot);
        	// fine del lavoro, logout
	    	AuthenticationUtils.endSession();
	    	log("alfresco path successefull exported");
        } catch (Throwable t) {
        	t.printStackTrace();
	    	log("alfresco path export failed ",t,Project.MSG_ERR);
        } 
        log("+++ Export finished.");
    }
    
	private void explore(Store theStore, ResultSetRowWrapper branch, File currentDir) throws Exception {
		// preparo la query per l'elenco dei figli del ramo che ho ricevuto
		String q = "PRIMARYPARENT:\"" + theStore.getScheme() + "://" + theStore.getAddress() + "/" + branch.getUuid() + "\" AND (TYPE:\"cm:folder\" OR TYPE:\"cm:content\")";
        Query query = new Query(Constants.QUERY_LANG_LUCENE, q);
    	// devo fare delle query sul repository, ottengo il servizio corrispondente
		RepositoryServiceSoapBindingStub repositoryService = WebServiceFactory.getRepositoryService();
        QueryResult queryResult = repositoryService.query(theStore, query, false);
        // eseguo la query
        ResultSet resultSet = queryResult.getResultSet();
        // wrappo ciascun figlio
        List<ResultSetRowWrapper> childs = ResultSetRowWrapper.convert(resultSet.getRows(),branch);
        // elenco dei figli
        for (ResultSetRowWrapper child : childs) {
        	// se il figlio e' una directory la esploro
			if (child.getRow().getNode().getType().equals(ALFRESCO_MODEL_CONTENT_FOLDER) ||
				child.getRow().getNode().getType().equals(SINEKARTA_MODEL_CONTENT_ARCHIVE)) {
		        log("directory : " + child);
		        File childDir = createLocalDir(currentDir + File.separator + child.getName());
				explore(theStore, child,childDir);
			} else {
	        	// se e' un file lo scarico
		        log("file : " + child);
		    	// devo ottenere il contenuto del documento, mi serve il content service
				ContentServiceSoapBindingStub contentService = WebServiceFactory.getContentService();
				Reference r = new Reference(theStore,child.getUuid(),null);
				Predicate predicate = new Predicate(new Reference[]{r}, null, null);
				Content[] contents = contentService.read(predicate, ALFRESCO_MODEL_CONTENT_CONTENT);
				// creo il file su file system locale
		        File childFile = new File(currentDir + File.separator + child.getName());
				// ottengo l'input stream del file da scaricare su file system locale
				download(getContentAsInputStream(contents[0]),new FileOutputStream(childFile));
				// creo il file dei metadati su file system locale
		        File childFileXML = new File(currentDir + File.separator + child.getName() + METADATI_XML);
				// ottengo l'input stream del file da scaricare su file system locale
		        createXMLFile(theStore,new FileOutputStream(childFileXML),child);
			}
        }
	}
	
	private File createLocalDir(String path) {
        // creazione root directory su filesystem locale
        File pathFile = new File(path);
        if (pathFile.exists()) {
        	if (pathFile.list().length>0) {
	        	throw new BuildException("if localPath exists, must be an empty dir : " + path);
        	}
        } else {
	    	log("localPath does not exists, trying to create : " + path);
	    	if (!pathFile.mkdirs()) {
	        	throw new BuildException("failed to create localPath : " + path);
	    	}
        }
        return pathFile;
	}

	/**
	 * metodo copiato dalla contentutils di alfresco
	 * e' necessario utilizzare la nostra classe AuthenticationUtils
	 * @param content
	 * @return
	 */
	public InputStream getContentAsInputStream(Content content) {
		// Get the url and the ticket
		String ticket = AuthenticationUtils.getTicket();
		String strUrl = content.getUrl() + "?ticket=" + ticket;
		try {
			// Create the url connection to the download servlet
			URL url = new URL(strUrl);
			URLConnection conn = url.openConnection();
			// Set the cookie information
			conn.setRequestProperty("Cookie", "JSESSIONID=" + AuthenticationUtils.getAuthenticationDetails().getSessionId() + ";");
			// Return the input stream
			return conn.getInputStream();
		} catch (Exception exception) {
			throw new WebServiceException("Unable to get content as inputStream.", exception);
		}
	}
    
	private void download(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[8192];
		int len = is.read(buf);
		// scarico il file, byte per byte
		while (len!=-1) {
			os.write(buf,0,len);
			len = is.read(buf);
		}
		os.flush();
		os.close();
		is.close();
	}
	
	/**
     * Metodo per la creazione del file xml da accompagnare col file di backup
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
     */
	private static void createXMLFile(Store theStore, OutputStream os, ResultSetRowWrapper row) throws Exception {
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

		// Elemento radice
		Document doc = docBuilder.newDocument();
		Element rootElement = doc.createElement("metadata");
		doc.appendChild(rootElement);

		// mi creo il predicate del documento corrente
		Reference r = new Reference(theStore,row.getUuid(),null);
		Predicate predicate = new Predicate(new Reference[]{r}, null, null);
		
		RepositoryServiceSoapBindingStub repositoryService = WebServiceFactory.getRepositoryService();
    	// devo ottenere le proprieta' del documento, uso il dictionaryservice
		NodeDefinition[] definitions = repositoryService.describe(predicate);
		for (NodeDefinition definition : definitions) {
			if (definition.getAspects()!=null) {
				for (ClassDefinition aspect : definition.getAspects()) {
					// Elemento proprieta' singola
					Element property = doc.createElement("aspect");
					property.setAttribute("id", aspect.getName());
					rootElement.appendChild(property);
				}
			}
		}
		for (NamedValue nv:row.getRow().getColumns()) {
			// Elemento proprieta' singola
			Element property = doc.createElement("property");
			property.setAttribute("id", nv.getName());
			property.appendChild(doc.createTextNode(nv.getValue()));
			rootElement.appendChild(property);
		}
		// Scrive il contenuto nel file xml
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(os);
		transformer.transform(source, result);
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUserId(String user) {
		this.userId = user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setAlfrescoPath(String alfrescoPath) {
		this.alfrescoPath = alfrescoPath;
	}

	public void setLocalPath(String localPath) {
		this.localPath = localPath;
	}

	public void setTimeoutMilliseconds(int timeoutMilliseconds) {
		this.timeoutMilliseconds = timeoutMilliseconds;
	}
}
