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

import java.util.Iterator;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.impl.lucene.QueryParameterisationException;
import org.alfresco.service.cmr.model.FileExistsException;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.Path.ChildAssocElement;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.util.ISO9075;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.exception.InvalidPathException;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;

/**
 * utility class for working with nodes
 * 
 * @author andrea.tessaro
 *
 */
public class NodeTools {
	
	private static Logger tracer = Logger.getLogger(NodeTools.class);
	
	/**
	 * convert a path into a lucene compliant String
	 * all namespace are converted to corresponding prefix
	 * 
	 * @param namespaceService the namespace service used to translate namespace into prefix 
	 * @param path the path to translate
	 * @return a string representation of the path with prefixes 
	 */
	public static String translateNamespacePath(NamespaceService namespaceService, Path path) {
		StringBuffer ret = new StringBuffer();
		Iterator<Path.Element> iter = path.iterator();
		iter.next(); // skipping first "/" path
		while (iter.hasNext()) {
			ret.append("/");
			Path.Element el = iter.next();
			ret.append(el.getPrefixedString(namespaceService));
		}
		if (tracer.isDebugEnabled()) tracer.debug("translateNamespacePath : " + path + " = " + ret);
		return ret.toString();
	}
	
	/**
	 * convert a path into a human readable String
	 * 
	 * @param nodeService used to ask to each node his readable name
	 * @param path the path to translate
	 * @return a human readable string representation of the path
	 */
	public static String translatePath(NodeService nodeService, Path path) {
		StringBuffer ret = new StringBuffer();
		Iterator<Path.Element> iter = path.iterator();
		iter.next(); // skipping first "/" path
		while (iter.hasNext()) {
			ret.append("/");
			Path.Element el = iter.next();
			ChildAssociationRef elementRef = ((ChildAssocElement)el).getRef();
			ret.append(nodeService.getProperty(elementRef.getChildRef(), ContentModel.PROP_NAME));
		}
		if (tracer.isDebugEnabled()) tracer.debug("translatePath : " + path + " = " + ret);
		return ret.toString();
	}

	/**
	 * get or create the given (sinekarta archive) lucene path
	 * 
	 * @param searchService needed to search for the lucene path
	 * @param fileFolderService needed to create (if necessary) the path
	 * @param storeRef needed to know the soreRef into where create the path
	 * @param lucenePath the path to find and, if necessary, create
	 * @return the nodeRef of the requested lucene path
	 */
	@SuppressWarnings("static-access")
	public static NodeRef deepCreateArchiveFolder(NodeService nodeService, SearchService searchService, FileFolderService fileFolderService, OwnableService ownableService, StoreRef storeRef, String lucenePath) {
		if (tracer.isDebugEnabled()) tracer.debug("deepCreateArchiveFolder, path to create : " + lucenePath);
		// get node of path requested
		NodeRef node = getNodeRefByPath(searchService, nodeService, storeRef, lucenePath);
		// if exist, ok, return it
		if (node!=null) return node;
		// if not exist, get deep folder
		int i = lucenePath.lastIndexOf('/');
		// find (or create) parent of deep foler
		node = deepCreateArchiveFolder(nodeService, searchService, fileFolderService, ownableService, storeRef, lucenePath.substring(0,i));
		// calculate name of deep folder
		String spaceName = lucenePath.substring(i+1);
		spaceName = spaceName.substring(spaceName.indexOf(':')+1);
		// create deep folder and return it
		NodeRef ret = null;
		try {
			ret = fileFolderService.create(node,spaceName,SinekartaModel.TYPE_QNAME_ARCHIVE).getNodeRef();
		} catch (FileExistsException e) {
			int c=0;
			while (ret==null) {
				ret = getNodeRefByPath(searchService, nodeService, storeRef, lucenePath);
				try {
					Thread.currentThread().sleep(100);
				} catch (InterruptedException e1) {
				}
				c++;
				if (c>100) break;
			}
		}

		if (tracer.isDebugEnabled()) tracer.debug("deepCreateArchiveFolder, setting properties and ownership of " + lucenePath);
		// setting space attribute and ownership
		String rcsUserId = (String)nodeService.getProperty(node, SinekartaModel.PROP_QNAME_RCS_USER_ID);
		String sinekartaAdminUserId = (String)nodeService.getProperty(node, SinekartaModel.PROP_QNAME_SINEKARTA_ADMIN_USER_ID);
		nodeService.setProperty(ret, SinekartaModel.PROP_QNAME_ICON, "sinekarta-archive-icon");
		nodeService.setProperty(ret, SinekartaModel.PROP_QNAME_RCS_USER_ID, rcsUserId);
		nodeService.setProperty(ret, SinekartaModel.PROP_QNAME_SINEKARTA_ADMIN_USER_ID, sinekartaAdminUserId);
		nodeService.setProperty(ret, ContentModel.PROP_CREATOR, sinekartaAdminUserId);
		nodeService.setProperty(ret, ContentModel.PROP_MODIFIER, sinekartaAdminUserId);
		ownableService.setOwner(ret, sinekartaAdminUserId);
		if (tracer.isDebugEnabled()) tracer.debug("deepCreateArchiveFolder, path " + lucenePath + " created.");
		return ret;
	}

	/**
	 * get or create the given (simple) lucene path
	 * 
	 * @param searchService needed to search for the lucene path
	 * @param fileFolderService needed to create (if necessary) the path
	 * @param storeRef needed to know the soreRef into where create the path
	 * @param lucenePath the path to find and, if necessary, create
	 * @return the nodeRef of the requested lucene path
	 */
	@SuppressWarnings("static-access")
	public static NodeRef deepCreateFolder(NodeService nodeService, SearchService searchService, FileFolderService fileFolderService, StoreRef storeRef, String lucenePath) {
		if (tracer.isDebugEnabled()) tracer.debug("deepCreateFolder, path to create : " + lucenePath);
		// get node of path requested
		NodeRef node = getNodeRefByPath(searchService, nodeService, storeRef, lucenePath);
		// if exist, ok, return it
		if (node!=null) return node;
		// if not exist, get deep folder
		int i = lucenePath.lastIndexOf('/');
		// find (or create) parent of deep foler
		node = deepCreateFolder(nodeService, searchService, fileFolderService, storeRef, lucenePath.substring(0,i));
		// calculate name of deep folder
		String spaceName = lucenePath.substring(i+1);
		spaceName = spaceName.substring(spaceName.indexOf(':')+1);
		// create deep folder and return it
		NodeRef ret = null;
		try {
			ret = fileFolderService.create(node,spaceName,ContentModel.TYPE_FOLDER).getNodeRef();
		} catch (FileExistsException e) { // for concurrency reasson, in case of fileExists we need to reread
			int c=0;
			while (ret==null) {
				ret = getNodeRefByPath(searchService, nodeService, storeRef, lucenePath);
				try {
					Thread.currentThread().sleep(100);
				} catch (InterruptedException e1) {
				}
				c++;
				if (c>100) break;
			}
		}
		if (tracer.isDebugEnabled()) tracer.debug("deepCreateFolder, path " + lucenePath + " created.");
		return ret;
	}

	/**
	 * 	calculate the NodeRef of a giving lucenePath (the path should return only one result)
	 *  lucenePath must be a path: query format (without PATH:)
	 *  
	 * @param searchService needed to search for the lucene path
	 * @param storeRef needed to know the soreRef into where search for the path
	 * @param lucenePath the path to find and 
	 * @return the nodeRef of the requested lucene path
	 */
	public static NodeRef getNodeRefByPath(SearchService searchService, NodeService nodeService, StoreRef storeRef, String lucenePath) {
		if (tracer.isDebugEnabled()) tracer.debug("getNodeRefByPath, searching noderef of this path : " + lucenePath);
		// execute the lucene query (MUST BE a PATH: query format)
		ResultSet rs = null; 
		try {
			try {
				rs = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, "PATH:\""+encodeLucenePath(lucenePath)+"\"");
			} catch (Exception ex) {
				rs = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, "PATH:\""+encodeLucenePath(lucenePath)+"\"");
			}
			// if no nodes found, retur null
			if (rs.length()==0)
				return null;
			else if (rs.length()==1) {
				NodeRef ret =  rs.getNodeRef(0);
				if (!nodeService.exists(ret)) return null;
				// if 1 node found, it's good!
				else return ret;
			} else {
				// if more than 1, throw exception
				tracer.error("Lucene query returns more than 1 noderef.");
				throw new InvalidPathException("Lucene query returns more than 1 noderef.");
			}
		} finally {
			if (rs!=null) rs.close();
		}
	}
	
	/**
	 * 	calculate the NodeRef of a given noderef id
	 *  
	 * @param searchService needed to search for the lucene path
	 * @param storeRef needed to know the soreRef into where search for the path
	 * @param lucenePath the path to find and 
	 * @return the nodeRef of the requested lucene path
	 */
	public static NodeRef getNodeByID(SearchService searchService, NodeService nodeService, StoreRef storeRef, String id) {
		if (tracer.isDebugEnabled()) tracer.debug("getNodeByID, searching noderef of this id : " + id);
		// execute the lucene query (MUST BE a PATH: query format)
		ResultSet rs = null;
		try {
			rs = searchService.query(storeRef, SearchService.LANGUAGE_LUCENE, "ID:\"" + storeRef.getProtocol() + "://" + storeRef.getIdentifier()+"/"+id+"\"");
			// if no nodes found, retur null
			if (rs.length()==0)
				return null;
			else {
				NodeRef ret =  rs.getNodeRef(0);
				if (!nodeService.exists(ret)) return null;
				// if 1 node found, it's good!
				else return ret;
			}
		} finally {
			if (rs!=null) rs.close();
		}
	}
	
	/**
	 * 	ISO9075 encoding of a lucene path
	 *  lucenePath must be a path: query format (without PATH:)
	 *  
	 * @param lucenePath the path to endoce 
	 * @return the lucenePath encoded
	 */
	public static String encodeLucenePath(String lucenePath) {
		if (tracer.isDebugEnabled()) tracer.debug("encodeLucenePath, encoding path : " + lucenePath);
		// if lucenePath is null or empty, nothing to do
		if (lucenePath==null || lucenePath.equals("")) return lucenePath;
		// find first / for deep recursive encoding
		int i = lucenePath.lastIndexOf('/');
		// calculate last spacePrefix and spaceName
		String completeSpaceName = lucenePath.substring(i+1);
		String spaceName = completeSpaceName.substring(completeSpaceName.indexOf(':')+1);
		String spacePrefix = completeSpaceName.substring(0,completeSpaceName.indexOf(':'));
		// deep recursive encoding
		String spaceParent = encodeLucenePath(lucenePath.substring(0,i));
		if (tracer.isDebugEnabled()) tracer.debug("encodeLucenePath, path encoded : " + spaceParent + "/" + spacePrefix + ":" + ISO9075.encode(spaceName));
		return spaceParent + "/" + spacePrefix + ":" + ISO9075.encode(spaceName);
	}

	public static boolean isArchived(NodeService nodeService, NamespaceService namespaceService, NodeRef nodeRef, String companyHomePath) {
		String givenPath = NodeTools.translateNamespacePath(namespaceService, nodeService.getPath(nodeRef));
		String archivioPath = Configuration.getInstance().getLuceneArchivePath();
		String fullArchivioPath = companyHomePath + archivioPath;
		if (givenPath.startsWith(fullArchivioPath)) return true;
		else return false;
	}
	
	public static NodeRef getArchivio(NodeService nodeService, SearchService searchService, String companyHomePath) {
		// preparing for query 
		StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
		// getting noderef of archivio
		String archivioPath = Configuration.getInstance().getLuceneArchivePath();
		NodeRef archivio = NodeTools.getNodeRefByPath(searchService, nodeService, storeRef, companyHomePath + archivioPath);
		if (archivio == null) {
			throw new SignFailedException("archivio does not exists : " + companyHomePath + archivioPath);
		}
		return archivio;
	}

	public static String getSinekartaAdminUserId(NodeService nodeService, SearchService searchService, String companyHomePath) {
		NodeRef archivio = getArchivio(nodeService, searchService, companyHomePath);
		// obtaining sinekartaAdmin userid of specified archivio
		return (String)nodeService.getProperty(archivio, SinekartaModel.PROP_QNAME_SINEKARTA_ADMIN_USER_ID);

	}
}
