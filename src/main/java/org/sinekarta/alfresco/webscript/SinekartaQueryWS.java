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
package org.sinekarta.alfresco.webscript;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.util.NodeTools;
import org.sinekarta.alfresco.webscript.request.SinekartaQueryRequest;
import org.sinekarta.alfresco.webscript.response.Document;
import org.sinekarta.alfresco.webscript.response.SinekartaQueryResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class SinekartaQueryWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";

	private static Logger tracer = Logger.getLogger(SinekartaQueryWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private SearchService searchService;
	private NodeService nodeService;
	@SuppressWarnings("unused")
	private FileFolderService fileFolderService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript SinekartaQueryWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		SinekartaQueryResponse resp = new SinekartaQueryResponse();
		List<Document> docs = new ArrayList<Document>();
		resp.setDocument(docs);
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			SinekartaQueryRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = SinekartaQueryRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = SinekartaQueryRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			// verifica parametro lucenePath
			if (r.getLuceneQuery()==null || r.getLuceneQuery().equals("")) { 
				tracer.error("No lucene path specified for sinekartaQuery action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No lucene path specified for sinekartaQuery action");
			}
			
			// verifica parametro path
			if (r.getPath()==null ) { 
				tracer.error("No path specified for sinekartaQuery action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No path specified for sinekartaQuery action");
			}
			
	        SearchParameters sp = new SearchParameters();
			StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
	        sp.addStore(storeRef);
	        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
	        sp.setQuery(r.getLuceneQuery());
	        sp.addSort(SearchParameters.SORT_IN_DOCUMENT_ORDER_ASCENDING);
	        ResultSet rs = null;
	        try {
				rs = searchService.query(sp);
				for (int i=0;i<rs.length();i++) {
					NodeRef noderef = rs.getNodeRef(i);
					String name = NodeTools.translatePath(nodeService, nodeService.getPath(noderef)).substring(r.getPath().length());
					docs.add(new Document(name, r.getPath(), noderef.toString())); // TODO verificare il tostring del noderef
				}
	        } finally {
	        	if (rs!=null) rs.close();
	        }
			
			resp.setResult("success");
			
			String res = null;
			if (req.getFormat().equalsIgnoreCase("xml")) {
				res = resp.toXML();
			} else if (req.getFormat().equalsIgnoreCase("json")) {
				res = resp.toJSON();
			}
			
			model.put("results", res);
			
		} catch (WebScriptException e) {
			throw e;
		} catch (Exception e) {
			tracer.error("Generic error on SinekartaQueryWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on SinekartaQueryWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript SinekartaQueryWS finished");

		return model;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

}
