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
import org.sinekarta.alfresco.action.DocumentDigitalSignaturePrepareAndAddDocument;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.webscript.request.DigitalSignaturePrepareAndAddDocumentRequest;
import org.sinekarta.alfresco.webscript.response.DigitalSignaturePrepareAndAddDocumentResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class DigitalSignaturePrepareAndAddDocumentWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";

	private static Logger tracer = Logger.getLogger(DigitalSignaturePrepareAndAddDocumentWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private ActionService actionService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript DigitalSignaturePrepareAndAddDocumentWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			DigitalSignaturePrepareAndAddDocumentRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = DigitalSignaturePrepareAndAddDocumentRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = DigitalSignaturePrepareAndAddDocumentRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			// verifica parametro DigitalSignatureArea
			if (r.getDigitalSignatureArea()==null || r.getDigitalSignatureArea().equals("")) { 
				tracer.error("No DigitalSignatureArea specified for DigitalSignaturePrepareAndAddDocumentWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No DigitalSignatureArea specified for DigitalSignaturePrepareAndAddDocumentWS action");
			}
			// verifica parametro SignDescription
			if (r.getSignDescription()==null || r.getSignDescription().equals("")) { 
				tracer.error("No SignDescription specified for DigitalSignaturePrepareAndAddDocumentWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No SignDescription specified for DigitalSignaturePrepareAndAddDocumentWS action");
			}
			// verifica parametro SignLocation
			if (r.getSignLocation()==null || r.getSignLocation().equals("")) { 
				tracer.error("No SignLocation specified for DigitalSignaturePrepareAndAddDocumentWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No SignLocation specified for DigitalSignaturePrepareAndAddDocumentWS action");
			}
			// verifica parametro nodeRefs
			if (r.getNodeRefs()==null || r.getNodeRefs().equals("")) { 
				tracer.error("No nodeRefs specified for action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No nodeRefs specified for action");
			}

			String digitalSignatureArea = r.getDigitalSignatureArea();
			
			for (String nodeRefStr : r._getNodeRefs()) {
				NodeRef selectedDocumentNode = new NodeRef(nodeRefStr);
				Action digitalSignaturePrepareAndAddDocument = actionService.createAction(DocumentDigitalSignaturePrepareAndAddDocument.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_PREPARE_AND_ADD_DOCUMENT);
				digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_SIGN_DESCRIPTION, r.getSignDescription());
				digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_SIGN_LOCATION, r.getSignLocation());
				digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_CLIENT_AREA, digitalSignatureArea);
				try {
					actionService.executeAction(digitalSignaturePrepareAndAddDocument, selectedDocumentNode, false, false);
				} catch(Throwable t) {
					tracer.error("Unable to prepare data for document sign",t);
					throw new WebScriptException("Unable to prepare data for document sign",t);
				}
				digitalSignatureArea = (String)digitalSignaturePrepareAndAddDocument.getParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_RESULT);
			}
			
			DigitalSignaturePrepareAndAddDocumentResponse resp = new DigitalSignaturePrepareAndAddDocumentResponse();
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
			tracer.error("Generic error on DigitalSignaturePrepareAndAddDocumentWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on DigitalSignaturePrepareAndAddDocumentWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript DigitalSignaturePrepareAndAddDocumentWS finished");

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
