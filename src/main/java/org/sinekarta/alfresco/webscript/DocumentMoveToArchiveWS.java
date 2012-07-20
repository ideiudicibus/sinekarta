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

import java.util.HashMap;
import java.util.Map;

import org.alfresco.repo.model.Repository;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.action.DocumentAcquiring;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.webscript.request.DocumentMoveToArchiveRequest;
import org.sinekarta.alfresco.webscript.response.DocumentMoveToArchiveResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class DocumentMoveToArchiveWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";

	private static Logger tracer = Logger.getLogger(DocumentMoveToArchiveWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private ActionService actionService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript DocumentMoveToArchiveWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		DocumentMoveToArchiveResponse resp = new DocumentMoveToArchiveResponse();
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			DocumentMoveToArchiveRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = DocumentMoveToArchiveRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = DocumentMoveToArchiveRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			// verifica parametro documentTypeId
			if (r.getDocumentTypeId()==null || r.getDocumentTypeId().equals("")) { 
				tracer.error("No Document type id specified for moveToArchive action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No Document type id specified for moveToArchive action");
			}
			// verifica parametro documentDate
			if (r.getDocumentDate()==null || r.getDocumentDate().equals("")) { 
				tracer.error("No Document date specified for moveToArchive action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No Document date specified for moveToArchive action");
			}
			// verifica parametro documentLanguage
			if (r.getDocumentLanguage()==null || r.getDocumentLanguage().equals("")) { 
				tracer.error("No Document language specified for moveToArchive action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No Document language specified for moveToArchive action");
			}
			// verifica parametro nodeRefs
			if (r.getNodeRefs()==null || r.getNodeRefs().equals("")) { 
				tracer.error("No nodeRefs specified for action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No nodeRefs specified for action");
			}

			for (String nodeRefStr : r._getNodeRefs()) {
				NodeRef selectedDocumentNode = new NodeRef(nodeRefStr);
				Action documentAcquiring = actionService.createAction(DocumentAcquiring.ACTION_NAME_DOCUMENT_ACQUIRING);
				documentAcquiring.setParameterValue(DocumentAcquiring.PARAM_DOCUMENT_DATE, r._getDocumentDate());
				documentAcquiring.setParameterValue(DocumentAcquiring.PARAM_DOCUMENT_LANGUAGE, r.getDocumentLanguage());
				documentAcquiring.setParameterValue(DocumentAcquiring.PARAM_DOCUMENT_TYPE, r._getDocumentTypeId());
				try {
					actionService.executeAction(documentAcquiring, selectedDocumentNode, false, false);
				} catch(Throwable t) {
					tracer.error("Unable to move document",t);
					throw new WebScriptException("Unable to move document",t);
				}
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
			tracer.error("Generic error on DocumentMoveToArchiveWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on DocumentMoveToArchiveWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript DocumentMoveToArchiveWS finished");

		return model;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

}
