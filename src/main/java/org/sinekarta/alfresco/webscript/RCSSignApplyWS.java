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
import org.sinekarta.alfresco.action.DocumentRCSSignApply;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.webscript.request.RCSSignApplyRequest;
import org.sinekarta.alfresco.webscript.response.RCSSignApplyResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class RCSSignApplyWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";

	private static Logger tracer = Logger.getLogger(RCSSignApplyWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private ActionService actionService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript RCSSignApplyWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			RCSSignApplyRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = RCSSignApplyRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = RCSSignApplyRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			// verifica parametro DigitalSignatureArea
			if (r.getDigitalSignatureArea()==null || r.getDigitalSignatureArea().equals("")) { 
				tracer.error("No DigitalSignatureArea specified for RCSSignApplyWS action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No DigitalSignatureArea specified for RCSSignApplyWS action");
			}
			// verifica parametro nodeRefs
			if (r.getNodeRef()==null || r.getNodeRef().equals("")) { 
				tracer.error("No nodeRef specified for action");
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "No nodeRef specified for action");
			}

			String digitalSignatureArea = r.getDigitalSignatureArea();
			
			NodeRef selectedDocumentNode = new NodeRef(r.getNodeRef());
			Action rcsSignPrepare = actionService.createAction(DocumentRCSSignApply.ACTION_NAME_DOCUMENT_RCS_SIGN_APPLY);
			rcsSignPrepare.setParameterValue(DocumentRCSSignApply.PARAM_CLIENT_AREA, digitalSignatureArea);
			try {
				actionService.executeAction(rcsSignPrepare, selectedDocumentNode, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare data for document sign",t);
				throw new WebScriptException("Unable to prepare data for document sign",t);
			}
			digitalSignatureArea = (String)rcsSignPrepare.getParameterValue(DocumentRCSSignApply.PARAM_RESULT);
			
			RCSSignApplyResponse resp = new RCSSignApplyResponse();
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
			tracer.error("Generic error on RCSSignApplyWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on RCSSignApplyWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript RCSSignApplyWS finished");

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
