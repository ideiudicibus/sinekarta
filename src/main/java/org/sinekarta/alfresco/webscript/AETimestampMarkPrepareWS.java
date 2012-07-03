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
import org.sinekarta.alfresco.action.DocumentTimestampAEMarkPrepare;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.MarkDocumentAlreadyExistsException;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.webscript.request.AETimestampMarkPrepareRequest;
import org.sinekarta.alfresco.webscript.response.AETimestampMarkPrepareResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class AETimestampMarkPrepareWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";

	private static Logger tracer = Logger.getLogger(AETimestampMarkPrepareWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private ActionService actionService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript AETimestampMarkPrepareWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			AETimestampMarkPrepareRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = AETimestampMarkPrepareRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = AETimestampMarkPrepareRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			// verifica parametro MarkDocumentArea
			if (r.getMarkDocumentArea()==null || r.getMarkDocumentArea().equals("")) { 
				tracer.error("No MarkDocumentArea specified for MarkDocumentAddWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No MarkDocumentArea specified for MarkDocumentAddWS action");
			}
			// verifica parametro DigitalSignatureArea
			if (r.getDigitalSignatureArea()==null || r.getDigitalSignatureArea().equals("")) { 
				tracer.error("No DigitalSignatureArea specified for AETimestampMarkPrepareWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No DigitalSignatureArea specified for AETimestampMarkPrepareWS action");
			}
			// verifica parametro nodeRefs
			if (r.getNodeRef()==null || r.getNodeRef().equals("")) { 
				tracer.error("No nodeRefs specified for action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No nodeRefs specified for action");
			}

			String digitalSignatureArea = r.getDigitalSignatureArea();
			
			NodeRef selectedDocumentNode = new NodeRef(r.getNodeRef());
			// conversione documento in PDF/A
			Action documentTimestampMarkPrepare = actionService.createAction(DocumentTimestampAEMarkPrepare.ACTION_NAME_DOCUMENT_TIMESTAMP_AEMARK_PREPARE);
			documentTimestampMarkPrepare.setParameterValue(DocumentTimestampAEMarkPrepare.PARAM_MARK_AREA, r.getMarkDocumentArea());
			documentTimestampMarkPrepare.setParameterValue(DocumentTimestampAEMarkPrepare.PARAM_CLIENT_AREA, r.getDigitalSignatureArea());
			try {
				actionService.executeAction(documentTimestampMarkPrepare, selectedDocumentNode, false, false);
			} catch(MarkDocumentAlreadyExistsException e) {
				throw e;
			} catch(Throwable t) {
				tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
				throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			}
			digitalSignatureArea = (String)documentTimestampMarkPrepare.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT);
			
			AETimestampMarkPrepareResponse resp = new AETimestampMarkPrepareResponse();
			resp.setDigitalSignatureArea(digitalSignatureArea);
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
			tracer.error("Generic error on AETimestampMarkPrepareWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on AETimestampMarkPrepareWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript AETimestampMarkPrepareWS finished");

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
