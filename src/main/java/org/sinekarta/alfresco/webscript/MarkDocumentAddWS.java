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
import org.sinekarta.alfresco.action.MarkDocumentAdd;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.webscript.request.MarkDocumentAddRequest;
import org.sinekarta.alfresco.webscript.response.MarkDocumentAddResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class MarkDocumentAddWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";

	private static Logger tracer = Logger.getLogger(MarkDocumentAddWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private ActionService actionService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript MarkDocumentAddWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			MarkDocumentAddRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = MarkDocumentAddRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = MarkDocumentAddRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			// verifica parametro MarkDocumentArea
			if (r.getMarkDocumentArea()==null || r.getMarkDocumentArea().equals("")) { 
				tracer.error("No MarkDocumentArea specified for MarkDocumentAddWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No MarkDocumentArea specified for MarkDocumentAddWS action");
			}
			// verifica parametro nodeRefs
			if (r.getNodeRefs()==null || r.getNodeRefs().equals("")) { 
				tracer.error("No nodeRefs specified for action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No nodeRefs specified for action");
			}

			String markDocumentArea = r.getMarkDocumentArea();
			
			for (String nodeRefStr : r._getNodeRefs()) {
				NodeRef selectedDocumentNode = new NodeRef(nodeRefStr);
				// invoking action for producing timestamp mark file
				Action markDocumentAdd = actionService.createAction(MarkDocumentAdd.ACTION_NAME_MARK_DOCUMENT_ADD);
				markDocumentAdd.setParameterValue(MarkDocumentAdd.PARAM_MARK_AREA, markDocumentArea);
				try {
					actionService.executeAction(markDocumentAdd, selectedDocumentNode, false, false);
				} catch(Throwable t) {
					tracer.error("Unable to prepare mark documenr : " + t.getMessage(),t);
					throw new MarkFailedException("Unable to prepare mark documenr : " + t.getMessage(),t);
				}
				markDocumentArea = (String)markDocumentAdd.getParameterValue(MarkDocumentAdd.PARAM_RESULT);
			}
			
			MarkDocumentAddResponse resp = new MarkDocumentAddResponse();
			resp.setMarkDocumentArea(markDocumentArea);
			resp.setResult("success");
			
			String res = null;
			if (requestType.equalsIgnoreCase("xml")) {
				res = resp.toXML();
			} else if (requestType.equalsIgnoreCase("json")) {
				res = resp.toJSON();
			}
			
			model.put("results", res);
			
		} catch (WebScriptException e) {
			throw e;
		} catch (Exception e) {
			tracer.error("Generic error on MarkDocumentAddWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on MarkDocumentAddWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript MarkDocumentAddWS finished");

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
