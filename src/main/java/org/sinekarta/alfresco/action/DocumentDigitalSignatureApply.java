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
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.DigitalSignatureException;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.sign.area.DigitalSignatureArea;
import org.sinekarta.sign.area.DigitalSignatureDocument;
import org.springframework.beans.factory.InitializingBean;

/**
 * document digital signature apply action.
 * this action will apply digital signature to all document prepared for
 * 
 * - parameter clientArea : returned from SinekartaDigitalSignatureClient in executeDigitalSignature
 * 
 * - no output parameter returned
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentDigitalSignatureApply extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentDigitalSignatureApply.class);
	public static final String ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_APPLY = "sinekartaDocumentDigitalSignatureApply";
	public static final String PARAM_CLIENT_AREA = "clientArea";


	private ContentService contentService;

	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentDigitalSignatureApply action, execution init");
		
		String clientArea = (String)action.getParameterValue(PARAM_CLIENT_AREA);
		if (clientArea==null) {
			tracer.error("no clientArea specified for DocumentDigitalSignatureApply.");
			throw new DigitalSignatureException("no clientArea specified for DocumentDigitalSignatureApply.");
		}
		
		// DigitalSignatureArea deserialization
		DigitalSignatureArea sca = DigitalSignatureArea.fromBase64String(clientArea);
		
		// verification of noderef received : is the same of the init phase?
		if (!actionedUponNodeRef.getStoreRef().getProtocol().equals(sca.getDocumentPathStoreRefProtocol()) ||
			!actionedUponNodeRef.getStoreRef().getIdentifier().equals(sca.getDocumentPathStoreRefId()) ||
			!actionedUponNodeRef.getId().equals(sca.getDocumentPathNodeRefId())) {
			tracer.error("document provided is not the same provided ini init phase.");
			throw new DigitalSignatureException("document provided is not the same provided ini init phase.");
		}

		// no document selected, how is it possible?
		List<DigitalSignatureDocument> docs = sca.getDocuments();
		if (docs==null || docs.size()==0) {
			tracer.error("no documento was selected for signing.");
			throw new DigitalSignatureException("no documento was selected for signing.");
		}
		
		StoreRef storeRef = new StoreRef(sca.getDocumentPathStoreRefProtocol(), sca.getDocumentPathStoreRefId());

		// loop on each document of the list
		for (DigitalSignatureDocument dsd : docs) {
			NodeRef nodeRef = new NodeRef(storeRef, dsd.getNodeRefId());
			// applying the digital signature using itext
			ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			InputStream is = contentReader.getContentInputStream();
			 
			ContentWriter contentWriter = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
			OutputStream os = contentWriter.getContentOutputStream();
			contentWriter.setMimetype(contentReader.getMimetype());
			contentWriter.setEncoding("UTF-8");
			contentWriter.setLocale(contentReader.getLocale());

			try {
				PDFTools.sign(dsd, sca.hexCertificateToX509Certificate(), is, os);
			} catch (CertificateException e) {
				tracer.error("failed to apply digital signature, certificate invalid.",e);
				throw new DigitalSignatureException("failed to apply digital signature, certificate invalid.",e);
			}
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("DocumentDigitalSignatureApply action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_CLIENT_AREA,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_CLIENT_AREA))); 
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

}
