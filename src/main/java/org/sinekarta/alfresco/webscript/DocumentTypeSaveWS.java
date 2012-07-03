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
import org.alfresco.service.cmr.action.ActionService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.webscript.request.DocumentTypeRequest;
import org.sinekarta.alfresco.webscript.response.DocumentTypeResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class DocumentTypeSaveWS extends DeclarativeWebScript {

	private static final String REQUEST_TYPE = "requestType";
	
	private static final String DOCUMENT_TYPE_DESCRIPTION = "documentTypeDescription";
	private static final String DOCUMENT_TYPE_EXTERNALCODE = "documentTypeExternalCode";
	private static final String DOCUMENT_TYPE_OCRREQUIRED = "documentTypeOcrRequired";
	private static final String DOCUMENT_TYPE_PDFASIGNED = "documentTypePdfAAlreadySigned";
	private static final String DOCUMENT_TYPE_UNIQUEORIGINAL = "documentTypeUniqueOriginal";
	private static final String DOCUMENT_TYPE_DEFAULTLANGUAGE = "documentTypeDefaultLanguage";
	private static final String DOCUMENT_TYPE_DOCLOCATIONRULE = "documentTypeDocLocatioRule";
	private static final String DOCUMENT_TYPE_MARKLOCATIONRULE = "documentTypeMarkLocationRule";
	
	private static Logger tracer = Logger.getLogger(DocumentTypeSaveWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private ActionService actionService;
	public ActionService getActionService() {
		return actionService;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public Repository getRepository() {
		return repository;
	}

	public SinekartaDao getSinekartaDao() {
		return sinekartaDao;
	}

	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript DocumentTypeWS starting");
		
//		Map<String, Object> model = new HashMap<String, Object>();
//		DocumentTypeSaveResponse resp = new DocumentTypeSaveResponse();
//		List<org.sinekarta.alfresco.webscript.response.DocumentType> ddttList = new ArrayList<org.sinekarta.alfresco.webscript.response.DocumentType>();
//		DocumentType ddtt = new DocumentType();
//		resp.setDocumentTypes(ddttList);
//		
//		try {		
//			// acquisizione parametri attributi
//			ddtt.setDescription(req.getParameter(DOCUMENT_TYPE_DESCRIPTION));
//			ddtt.setExternalCode(req.getParameter(DOCUMENT_TYPE_EXTERNALCODE));
//			ddtt.setOcrRequired(Boolean.parseBoolean(req.getParameter(DOCUMENT_TYPE_OCRREQUIRED)));
//			ddtt.setPdfaAlreadySigned(Boolean.parseBoolean(req.getParameter(DOCUMENT_TYPE_PDFASIGNED)));
//			ddtt.setUniqueOriginal(Boolean.parseBoolean(req.getParameter(DOCUMENT_TYPE_UNIQUEORIGINAL)));
//			ddtt.setDefaultLanguage(req.getParameter(DOCUMENT_TYPE_DEFAULTLANGUAGE));
//			ddtt.setDocLocationRule(req.getParameter(DOCUMENT_TYPE_DOCLOCATIONRULE));
//			ddtt.setMarkLocationRule(req.getParameter(DOCUMENT_TYPE_MARKLOCATIONRULE));
//			sinekartaDao.save(ddtt);
//
//			String res = null;
//			if (req.getFormat().equalsIgnoreCase("xml")) {
//				res = resp.toXML();
//			} else if (req.getFormat().equalsIgnoreCase("json")) {
//				res = resp.toJSON();
//			}
//			
//			model.put("results", res);
//			
//		} catch (WebScriptException e) {
//			throw e;
//		} catch (Exception e) {
//			tracer.error("Generic error on DocumentType web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
//			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on DocumentTypeSAve web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
//		}
		
		
		Map<String, Object> model = new HashMap<String, Object>();
		DocumentTypeResponse resp = new DocumentTypeResponse();
		
		try {
			String requestType = req.getParameter(REQUEST_TYPE);
			DocumentTypeRequest r = null;
			if (requestType.equalsIgnoreCase("xml")) {
				r = DocumentTypeRequest.fromXML(req.getContent().getInputStream());
			} else if (requestType.equalsIgnoreCase("json")) {
				r = DocumentTypeRequest.fromJSON(req.getContent().getInputStream());
			} else {
				tracer.error("Invalid request type : " + requestType);
				throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Invalid request type : " + requestType);
			}
			
			DocumentType ddtt;
			if(r.getId() > 0) {
				ddtt = sinekartaDao.getDocumentType(r.getId());
			} else {
				ddtt = new DocumentType();
			}
			
			ddtt.setId(r.getId());
			ddtt.setDescription(r.getDescription());
			ddtt.setExternalCode(r.getExternalCode());
			ddtt.setOcrRequired(r.isOcrRequired());
			ddtt.setPdfaAlreadySigned(r.isPdfaAlreadySigned());
			ddtt.setPdfaConvertNeeded(r.isPdfaConvertNeeded());
			ddtt.setUniqueOriginal(r.isUniqueOriginal());
			ddtt.setDefaultLanguage(r.getDefaultLanguage());
			ddtt.setDocLocationRule(r.getDocLocationRule());
			ddtt.setMarkLocationRule(r.getMarkLocationRule());
			
			if(r.getId() > 0) {
				sinekartaDao.update(ddtt);	
			} else {
				sinekartaDao.save(ddtt);
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
			tracer.error("Generic error on DocumentTypeSaveWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR, "Generic error on DocumentTypeSaveWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript DocumentTypeWS finished");

		return model;
	}
	
	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

}
