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

import java.io.InputStream;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.DigitalSignatureException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.sign.area.DigitalSignatureArea;
import org.sinekarta.sign.area.DigitalSignatureDocument;
import org.springframework.beans.factory.InitializingBean;

/**
 * document prepare for sign.
 * this action will add the given document to list of documents to be signed
 * 
 * - parameter clientArea : returned from SinekartaDigitalSignatureClient in certificate list
 * - parameter description : the description (reason) of signature of this document
 * - parameter location : the location of where this digital signature will be applyed
 * 
 * - return a string to be passed to SinekartaDigitalSignatureClient constructor
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentDigitalSignaturePrepareAndAddDocument extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentDigitalSignaturePrepareAndAddDocument.class);
	public static final String ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_PREPARE_AND_ADD_DOCUMENT = "sinekartaDocumentDigitalSignaturePrepareAndAddDocument";
	public static final String PARAM_CLIENT_AREA = "clientArea";
	public static final String PARAM_SIGN_DESCRIPTION = "description";
	public static final String PARAM_SIGN_LOCATION = "location";


	private NodeService nodeService;
	private ContentService contentService;
	private AuthenticationService authenticationService;
	private PersonService personService;

	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentDigitalSignaturePrepareAndAddDocument action, execution init");
		
		String clientArea = (String)action.getParameterValue(PARAM_CLIENT_AREA);
		if (clientArea==null) {
			tracer.error("no clientArea specified for DocumentDigitalSignaturePrepareAndAddDocument.");
			throw new DigitalSignatureException("no clientArea specified for DocumentDigitalSignaturePrepareAndAddDocument.");
		}
		
		String description = (String)action.getParameterValue(PARAM_SIGN_DESCRIPTION);
		if (description==null) {
			tracer.error("no description specified for DocumentDigitalSignaturePrepareAndAddDocument.");
			throw new DigitalSignatureException("no description specified for DocumentDigitalSignaturePrepareAndAddDocument.");
		}
		
		String location = (String)action.getParameterValue(PARAM_SIGN_LOCATION);
		if (location==null) {
			tracer.error("no location specified for DocumentDigitalSignaturePrepareAndAddDocument.");
			throw new DigitalSignatureException("no location specified for DocumentDigitalSignaturePrepareAndAddDocument.");
		}
		
		DigitalSignatureArea sca = DigitalSignatureArea.fromBase64String(clientArea);
		
		// driver specified from the user will be saved for further uses
        String userName = authenticationService.getCurrentUserName();
        NodeRef personRef = personService.getPerson(userName);
		nodeService.setProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_SMARTCARD_DLL, sca.getDriver());

		// verification of attributes of file and attributes of initialized path
		if (!actionedUponNodeRef.getStoreRef().getProtocol().equals(sca.getDocumentPathStoreRefProtocol()) ||
			!actionedUponNodeRef.getStoreRef().getIdentifier().equals(sca.getDocumentPathStoreRefId())) {
			tracer.error("store ref of given document is not compliant with given path in init.");
			throw new DigitalSignatureException("store ref of given document is not compliant with given path in init.");
		}

		// the next function to be done on client will be digital signature
		sca.setFunction(DigitalSignatureArea.FUCTION_CODE_SIGNATURE_DIGESTED_CONENT);

		// if the document list is null i will create an empty one 
		List<DigitalSignatureDocument> docs = sca.getDocuments();
		if (docs==null) {
			sca.setDocuments(new ArrayList<DigitalSignatureDocument>());
			docs = sca.getDocuments();
		}
		
		// given document is already present in the list of document?
		for (DigitalSignatureDocument doc : docs) {
			if (doc.getNodeRefId().equals(actionedUponNodeRef.getId())) {
				tracer.error("given document already prepared for signature.");
				throw new DigitalSignatureException("given document already prepared for signature.");
			}
		}
		
		DigitalSignatureDocument dsd = new DigitalSignatureDocument();
		// set id of document
		dsd.setNodeRefId(actionedUponNodeRef.getId());
		// set sign description (reason)
		dsd.setDescription(description);
		// set location of signature
		dsd.setLocation(location);
		String fileName = (String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME);
		// set filename (for display reason) 
		dsd.setFileName(fileName);
		
		docs.add(dsd);
		
		// calculate fingerprint
		ContentReader contentReader = contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
		InputStream is = contentReader.getContentInputStream();
		
		try {
			PDFTools.calculateFingerPrint(dsd,sca.hexCertificateToX509Certificate(),is);
		} catch (CertificateException e) {
			tracer.error("failed to calculate finger print, certificate invalid.",e);
			throw new DigitalSignatureException("failed to calculate finger print, certificate invalid.",e);
		}

		// serialize result 
		String ret = sca.toBase64String();

		action.setParameterValue(PARAM_RESULT, ret);

		if (tracer.isDebugEnabled()) tracer.debug("DocumentDigitalSignaturePrepareAndAddDocument action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_CLIENT_AREA,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_CLIENT_AREA))); 
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_SIGN_DESCRIPTION,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_SIGN_DESCRIPTION))); 
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_SIGN_LOCATION,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_SIGN_LOCATION))); 
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

}
