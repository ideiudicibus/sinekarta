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
import org.sinekarta.alfresco.action.DocumentDigitalSignatureInit;
import org.sinekarta.alfresco.action.AEMarkDocumentPrepare;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.MarkDocumentAlreadyExistsException;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.webscript.request.AEMarkDocumentPrepareRequest;
import org.sinekarta.alfresco.webscript.response.AEMarkDocumentPrepareResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class AEMarkDocumentPrepareWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";

	private static Logger tracer = Logger.getLogger(AEMarkDocumentPrepareWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private ActionService actionService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript AEMarkDocumentInitWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			AEMarkDocumentPrepareRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = AEMarkDocumentPrepareRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = AEMarkDocumentPrepareRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			// verifica parametro MarkDocumentArea
			if (r.getMarkDocumentArea()==null || r.getMarkDocumentArea().equals("")) { 
				tracer.error("No MarkDocumentArea specified for MarkDocumentAddWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No MarkDocumentArea specified for MarkDocumentAddWS action");
			}
			// verifica parametro documentTypeId
			if (r.getMarkDescription()==null || r.getMarkDescription().equals("")) { 
				tracer.error("No mark description id specified for moveToArchive action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No mark description id specified for moveToArchive action");
			}
			// verifica parametro documentDate
			if (r.getMarkFileName()==null || r.getMarkFileName().equals("")) { 
				tracer.error("No mark filename specified for moveToArchive action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No mark filename  specified for moveToArchive action");
			}
			// verifica parametro nodeRefs
			if (r.getNodeRef()==null || r.getNodeRef().equals("")) { 
				tracer.error("No nodeRef specified for action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No nodeRef specified for action");
			}

			// preparazione documento di marca temporale
			Action markDocumentPrepare = actionService.createAction(AEMarkDocumentPrepare.ACTION_NAME_AEMARK_DOCUMENT_PREPARE);
			markDocumentPrepare.setParameterValue(AEMarkDocumentPrepare.PARAM_MARK_AREA, r.getMarkDocumentArea());
			markDocumentPrepare.setParameterValue(AEMarkDocumentPrepare.PARAM_MARK_DESCRIPTIONR, r.getMarkDescription());
			markDocumentPrepare.setParameterValue(AEMarkDocumentPrepare.PARAM_MARK_FILENAME, r.getMarkFileName());
			NodeRef selectedDocumentNode = new NodeRef(r.getNodeRef());
			try {
				actionService.executeAction(markDocumentPrepare, selectedDocumentNode, false, false);
			} catch(MarkDocumentAlreadyExistsException e) {
				throw e;
			} catch(Throwable t) {
				tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
				throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			}
			String markDocumentArea = (String)markDocumentPrepare.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT);

			AEMarkDocumentPrepareResponse resp = new AEMarkDocumentPrepareResponse();
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
			tracer.error("Generic error on AEMarkDocumentInitWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on AEMarkDocumentInitWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript AEMarkDocumentInitWS finished");

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
