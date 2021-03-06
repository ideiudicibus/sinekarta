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

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.NodeTools;
import org.sinekarta.sign.area.DigitalSignatureArea;
import org.sinekarta.sign.area.DigitalSignatureDocument;
import org.springframework.beans.factory.InitializingBean;

/**
 * document digital signature of PU action.
 * this action will apply the PU digital signature to the document
 * 
 * - parameter clientArea : returned from SinekartaDigitalSignatureClient in executeDigitalSignature
 * 
 * - no output parameter returned
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentPUSignApply extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentPUSignApply.class);
	public static final String ACTION_NAME_DOCUMENT_PU_SIGN_APPLY = "sinekartaDocumentPUSignApply";
	public static final String PARAM_CLIENT_AREA = "clientArea";

	private String companyHomePath;
	private ContentService contentService;
	private NodeService nodeService;
	private ActionService actionService;
	private SearchService searchService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentPUSignApply action, execution init");
		
		String clientArea = (String)action.getParameterValue(PARAM_CLIENT_AREA);
		if (clientArea==null) {
			tracer.error("no clientArea specified for generic sign.");
			throw new SignFailedException("no clientArea specified for generic sign.");
		}
		
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		DocumentPUSignApplyWorker execAsSinekartaAdmin = new DocumentPUSignApplyWorker(nodeService, contentService, 
				actionService, actionedUponNodeRef, clientArea);
		
		// running core of the action as the sinekarta admin user
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on pu sign : " + execAsSinekartaAdmin.getKoReason());
			throw new SignFailedException("error on pu sign : " + execAsSinekartaAdmin.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentPUSignApply action, execution end");
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

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

}
class DocumentPUSignApplyWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(DocumentPUSignApply.class);

	private NodeService nodeService;
	private ContentService contentService;
	private ActionService actionService;
	private String clientArea;
	private NodeRef actionedUponNodeRef;
	private String koReason;

	public DocumentPUSignApplyWorker(NodeService nodeService, ContentService contentService,
			ActionService actionService, NodeRef actionedUponNodeRef, String clientArea) {
		super();
		this.nodeService = nodeService;
		this.contentService = contentService;
		this.clientArea = clientArea;
		this.actionService = actionService;
		this.actionedUponNodeRef = actionedUponNodeRef;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentPUSignApply action.doWork, execution init");

		try {
			// deserialization of client area
			DigitalSignatureArea sca = DigitalSignatureArea.fromBase64String(clientArea);
			StoreRef storeRef = new StoreRef(sca.getDocumentPathStoreRefProtocol(), sca.getDocumentPathStoreRefId());
			// for each document we will verify if that the PU signature is not yet applied
			for (DigitalSignatureDocument dsd : sca.getDocuments()) {
				NodeRef docNodeRef = new NodeRef(storeRef, dsd.getNodeRefId());
				if (nodeService.hasAspect(docNodeRef, SinekartaModel.ASPECT_QNAME_PU_SIGNATURE)) {
					tracer.error("Document already PU signed, why you want to sign another time?");
					throw new SignFailedException("Document already PU signed, why you want to sign another time?");
				}
			}
	
			// invoking PU digital signature apply action
			Action digitalSignatureApply = actionService.createAction(DocumentDigitalSignatureApply.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_APPLY);
			digitalSignatureApply.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_CLIENT_AREA, clientArea);
			try {
				actionService.executeAction(digitalSignatureApply, actionedUponNodeRef, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare data for document sign", t);
				throw new SignFailedException("Unable to prepare data for document sign",t);
			}
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentPUSignApply action.doWork, firma digitale applicata, calcolo metadati documento");
	
			// setting metadata for each document
			for (DigitalSignatureDocument dsd : sca.getDocuments()) {
				NodeRef docNodeRef = new NodeRef(storeRef, dsd.getNodeRefId());
				// calculating PU_SIGNATURE properties values
				Map<QName,Serializable> properties = new HashMap<QName,Serializable>();
				properties.put(SinekartaModel.PROP_QNAME_TIMESTAMP_PU_SIGNATURE, new Date(System.currentTimeMillis()));
				ContentReader contentReader = contentService.getReader(docNodeRef, ContentModel.PROP_CONTENT);
				properties.put(SinekartaModel.PROP_QNAME_DOCUMENT_PU_SIGNED_FINGERPRINT, org.sinekarta.alfresco.util.Util.byteToHex(org.sinekarta.alfresco.util.Util.digest256(contentReader.getContentInputStream())));
		
				// adding PU_SIGNATURE aspect to document
				nodeService.addAspect(docNodeRef, SinekartaModel.ASPECT_QNAME_PU_SIGNATURE, properties);		
			}
		} catch (Throwable e) {
			tracer.error("DocumentPUSignApply action.doWork, generic error", e);
			koReason="DocumentPUSignApply action.doWork, generic error : " + e.getMessage();
			return false;
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("DocumentPUSignApply action.doWork, finished OK");

		return true;
	}
	
	public String getKoReason() {
		return koReason;
	}
}
