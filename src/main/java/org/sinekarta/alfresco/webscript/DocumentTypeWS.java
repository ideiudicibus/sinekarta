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
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.webscript.response.DocumentTypeResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class DocumentTypeWS extends DeclarativeWebScript {

	private static final String FULL = "full";
	private static final String DOCUMENT_TYPE_DESCRIPTION = "documentTypeDescription";
	private static final String DOCUMENT_TYPE_ID = "documentTypeId";

	private static Logger tracer = Logger.getLogger(DocumentTypeWS.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript DocumentTypeWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		DocumentTypeResponse resp = new DocumentTypeResponse();
		List<org.sinekarta.alfresco.webscript.response.DocumentType> ddtt = new ArrayList<org.sinekarta.alfresco.webscript.response.DocumentType>();
		resp.setDocumentTypes(ddtt);
		
		List<DocumentType> ret = null;
		
		try {
			// acquisizione parametro documentTypeId
			String documentTypeIdStr = req.getParameter(DOCUMENT_TYPE_ID);
			int documentTypeId = 0;
			if (documentTypeIdStr!=null && !documentTypeIdStr.equals("")) { 
				documentTypeId = Integer.parseInt(documentTypeIdStr);
			}
			// acquisizione parametro documentDescription
			String documentTypeDescription = req.getParameter(DOCUMENT_TYPE_DESCRIPTION);
			String isFullStr = req.getParameter(FULL);
			if (isFullStr==null || isFullStr.equals("")) { 
				isFullStr = "false";
			}
			// acquisizione parametro short
			boolean isFull = Boolean.parseBoolean(isFullStr);
			// che tipo di lista e' stato chiesto?
			if (documentTypeId!=0) {
				DocumentType dt = sinekartaDao.getDocumentType(documentTypeId);
				if (dt==null) {
					tracer.error("Requested document type ("+documentTypeId+") was not found.");
					throw new WebScriptException(Status.STATUS_BAD_REQUEST, "Requested document type ("+documentTypeId+") was not found.");
				}
				ret = new ArrayList<DocumentType>();
				ret.add(dt);
			} else if(documentTypeDescription!=null && !documentTypeDescription.equals("")) {
				ret = sinekartaDao.getDocumentTypesByDescription(documentTypeDescription);
			} else {
				ret = sinekartaDao.getDocumentTypes();
			}
			
			for (DocumentType dt:ret) {
				org.sinekarta.alfresco.webscript.response.DocumentType d = new org.sinekarta.alfresco.webscript.response.DocumentType();
				d.setId(dt.getId());
				d.setDescription(dt.getDescription());
				if (isFull) {
					d.setDefaultLanguage(dt.getDefaultLanguage());
					d.setExternalCode(dt.getExternalCode());
					d.setOcrRequired(dt.isOcrRequired());
					d.setPdfaConvertNeeded(dt.isPdfaConvertNeeded());
					d.setPdfaAlreadySigned(dt.isPdfaAlreadySigned());
					d.setTimestampUpdate(dt.getTimestampUpdate());
					d.setUniqueOriginal(dt.isUniqueOriginal());
					d.setMarkLocationRule(dt.getMarkLocationRule());
					d.setDocLocationRule(dt.getDocLocationRule());
				}
				ddtt.add(d);
			}
			
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
			tracer.error("Generic error on DocumentType web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on DocumentType web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
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
