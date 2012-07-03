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
package org.sinekarta.alfresco.action;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.NodeTools;
import org.sinekarta.sign.area.DigitalSignatureArea;
import org.sinekarta.sign.area.DigitalSignatureDocument;
import org.springframework.beans.factory.InitializingBean;

/**
 * digital signature init action.
 * this action will prepare area to be passed to client for digital signature certificate choice
 * 
 * - no input parameters needed
 * 
 * - return a string to be passed to SinekartaDigitalSignatureClient constructor
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentDigitalSignatureInit extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentDigitalSignatureInit.class);
	public static final String ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_INIT = "sinekartaDocumentDigitalSignatureInit";

	private NodeService nodeService;
	private AuthenticationService authenticationService;
	private PersonService personService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentDigitalSignatureInit action, execution init");
		
		// work area for digital signature initialization
        String userName = authenticationService.getCurrentUserName();
        NodeRef personRef = personService.getPerson(userName);
		// get the user default driver
		String driver = (String)nodeService.getProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_SMARTCARD_DLL);
		if (driver==null) 
			driver = Configuration.getInstance().getDefaultSmartcardDriver();

		DigitalSignatureArea sca = new DigitalSignatureArea();
		// set the first function for the client : certificate choice
		sca.setFunction(DigitalSignatureArea.FUCTION_CODE_CERTIFICATE_CHOICE);
		// set default user driver
		sca.setDriver(driver);
		String documentPath = NodeTools.translatePath(nodeService, nodeService.getPath(actionedUponNodeRef));
		// set the documents path 
		sca.setDocumentPath(documentPath);
		// empty list, documents will be added later
		sca.setDocuments(new ArrayList<DigitalSignatureDocument>());
		// save path attributes
		sca.setDocumentPathNodeRefId(actionedUponNodeRef.getId());
		sca.setDocumentPathStoreRefProtocol(actionedUponNodeRef.getStoreRef().getProtocol());
		sca.setDocumentPathStoreRefId(actionedUponNodeRef.getStoreRef().getIdentifier());
		
		// serialization of area
		String ret = sca.toBase64String();

		action.setParameterValue(PARAM_RESULT, ret);

		if (tracer.isDebugEnabled()) tracer.debug("DocumentDigitalSignatureInit action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

}
