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
import org.sinekarta.alfresco.action.DocumentAcquiring;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.webscript.response.DocumentTypeLanguage;
import org.sinekarta.alfresco.webscript.response.DocumentTypeLanguagesResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class DocumentTypeLanguageWS extends DeclarativeWebScript {

	private static final String LANGUAGE_CODE = "languageCode";

	private static Logger tracer = Logger.getLogger(DocumentAcquiring.class);

	@SuppressWarnings("unused")
	private Repository repository;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript DocumentTypeLanguageWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		DocumentTypeLanguagesResponse resp = new DocumentTypeLanguagesResponse();
		List<DocumentTypeLanguage> dtl = new ArrayList<DocumentTypeLanguage>();
		resp.setDocumentTypeLanguages(dtl);
		
		try {
			// acquisizione parametro languageCode
			String languageCode = req.getParameter(LANGUAGE_CODE);
			// che tipo di lista e' stato chiesto?
			if (languageCode!=null && !languageCode.equals("")) {
				String languageDescription = Configuration.getInstance().getMappaLingueOcr().get(languageCode);
				DocumentTypeLanguage el = new DocumentTypeLanguage(languageCode, languageDescription);
				dtl.add(el);
			} else {
				for (String languageCodeLoop : Configuration.getInstance().getMappaLingueOcr().keySet()) {
					String languageDescription = Configuration.getInstance().getMappaLingueOcr().get(languageCodeLoop);
					DocumentTypeLanguage el = new DocumentTypeLanguage(languageCodeLoop, languageDescription);
					dtl.add(el);
				}
			}
			
			resp.setUserDefaultLanguage(Configuration.getInstance().getLinguaDefaultOcr());
			
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
			tracer.error("Generic error on DocumentTypeLanguageWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on DocumentTypeLanguageWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript DocumentTypeLanguageWS finished");

		return model;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

}
