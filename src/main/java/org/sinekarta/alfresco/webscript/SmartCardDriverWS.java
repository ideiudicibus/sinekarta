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
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.action.DocumentAcquiring;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.webscript.response.SmartCardDriver;
import org.sinekarta.alfresco.webscript.response.SmartCardDriverResponse;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

public class SmartCardDriverWS extends DeclarativeWebScript {

	private static final String DRIVER_CODE = "driver";

	private static Logger tracer = Logger.getLogger(DocumentAcquiring.class);

	@SuppressWarnings("unused")
	private Repository repository;
	private PersonService personService;
	private NodeService nodeService;
	private AuthenticationService authenticationService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
		if (tracer.isDebugEnabled()) tracer.debug("webscript SmartCardDriverWS starting");
		
		Map<String, Object> model = new HashMap<String, Object>();
		SmartCardDriverResponse resp = new SmartCardDriverResponse();
		List<SmartCardDriver> drvs = new ArrayList<SmartCardDriver>();
		resp.setSmartCardDrivers(drvs);
		
		try {
			// acquisizione parametro languageCode
			String driver = req.getParameter(DRIVER_CODE);
			// che tipo di lista e' stato chiesto?
			if (driver!=null && !driver.equals("")) {
				String description = Configuration.getInstance().getMappaSmartcardDrivers().get(driver);
				SmartCardDriver el = new SmartCardDriver(driver, description);
				drvs.add(el);
			} else {
				for (String driverLoop : Configuration.getInstance().getMappaSmartcardDrivers().keySet()) {
					String description = Configuration.getInstance().getMappaSmartcardDrivers().get(driverLoop);
					SmartCardDriver el = new SmartCardDriver(driverLoop, description);
					drvs.add(el);
				}
			}

			
			String userId = authenticationService.getCurrentUserName();
			NodeRef person = personService.getPerson(userId);
		
			String def = (String)nodeService.getProperty(person, SinekartaModel.PROP_QNAME_SINEKARTA_SMARTCARD_DLL);
			if (def==null || def.equals("")) {
				resp.setUserDefaultDriver(Configuration.getInstance().getDefaultSmartcardDriver());
			} else {
				resp.setUserDefaultDriver(def);
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
			tracer.error("Generic error on SmartCardDriverWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
			throw new WebScriptException(Status.STATUS_NOT_ACCEPTABLE, "Generic error on SmartCardDriverWS web script : " + e.getClass().getName() + " : " + e.getMessage(),e);
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("webscript SmartCardDriverWS finished");

		return model;
	}

	public void setRepository(Repository repository) {
		this.repository = repository;
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

}
