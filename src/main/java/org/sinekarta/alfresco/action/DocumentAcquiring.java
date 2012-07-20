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
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.DocumentAcquiringException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.NodeTools;
import org.springframework.beans.factory.InitializingBean;

/**
 * document acquiring action.
 * this action will convert a document added to archive into a PDF/A document
 * and add documentAcquiring aspect
 * 
 * - no input parameter needed
 * - no output parameter returned
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentAcquiring extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentAcquiring.class);

	public static final String ACTION_NAME_DOCUMENT_ACQUIRING = "sinekartaDocumentAcquiring";
	public static final String PARAM_DOCUMENT_TYPE = "documentType";
	public static final String PARAM_DOCUMENT_DATE = "documentDate";
	public static final String PARAM_DOCUMENT_LANGUAGE = "documentLanguage";

	private String companyHomePath;
	private NodeService nodeService;
	private ContentService contentService;
	private SearchService searchService;
	private ActionService actionService;
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentAcquiring action, execution init");
		
		// the document has already the documentAcquiring aspect? if yes, nothing to do
		if (nodeService.hasAspect(actionedUponNodeRef, SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING)) return;

		// the document has the timestamp mark aspect? if yes, nothing to do
		if (nodeService.hasAspect(actionedUponNodeRef, SinekartaModel.ASPECT_QNAME_TIMESTAMP_MARK)) return;

		// the node to add is an archive, we don't need any acquire processing
		if (nodeService.getType(actionedUponNodeRef).equals(SinekartaModel.TYPE_QNAME_ARCHIVE)) return;
		
		// Someone is rendering the document (by ie in share), the document must not be acquired
		// if the document is a child of an acquired document and is not to render, it's a working copy for displaying
		if (nodeService.hasAspect(nodeService.getPrimaryParent(actionedUponNodeRef).getParentRef(), SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING)) return; //&& 
			//nodeService.hasAspect(actionedUponNodeRef, org.alfresco.model.RenditionModel.ASPECT_HIDDEN_RENDITION))
				 

		Integer documentTypeId = (Integer) action.getParameterValue(PARAM_DOCUMENT_TYPE);
		DocumentType documentType = null;
		if (documentTypeId!=null) {
			try {
				documentType = sinekartaDao.getDocumentType(documentTypeId);
			} catch (Exception e) {
				tracer.error("wrong documentType for mark folder prepare.", e);
				throw new DocumentAcquiringException("wrong documentType for mark folder prepare.", e);
			}
		}
		
		Date documentDate = (Date)action.getParameterValue(PARAM_DOCUMENT_DATE);
		
		String documentLanguage = (String)action.getParameterValue(PARAM_DOCUMENT_LANGUAGE);
		
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		DocumentAcquiringWorker execAsSinekartaAdmin = new DocumentAcquiringWorker(nodeService,  
				contentService, actionedUponNodeRef, actionService, documentType, documentDate, documentLanguage);
		
		// running core of action as sinekarta admin
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("document acquiring failed, please verify if the document already exists on archive : " + execAsSinekartaAdmin.getKoReason());
			throw new DocumentAcquiringException("document acquiring failed, please verify if the document already exists on archive : " + execAsSinekartaAdmin.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentAcquiring action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_DOCUMENT_TYPE,
				DataTypeDefinition.INT, 
				false, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_DOCUMENT_TYPE))); 
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_DOCUMENT_DATE,
				DataTypeDefinition.DATE, 
				false, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_DOCUMENT_DATE))); 
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_DOCUMENT_LANGUAGE,
				DataTypeDefinition.TEXT, 
				false, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_DOCUMENT_LANGUAGE))); 
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

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

}
class DocumentAcquiringWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(DocumentAcquiring.class);

	private NodeService nodeService;
	private ActionService actionService;
	private NodeRef actionedUponNodeRef;
	private DocumentType documentType;
	private Date documentDate;
	private String documentLanguage;
	private String koReason;

	public DocumentAcquiringWorker(NodeService nodeService, 
			ContentService contentService, NodeRef actionedUponNodeRef, 
			ActionService actionService, DocumentType documentType, Date documentDate, String documentLanguage) {
		super();
		this.nodeService = nodeService;
		this.actionedUponNodeRef = actionedUponNodeRef;
		this.actionService = actionService;
		this.documentType = documentType;
		this.documentDate = documentDate;
		this.documentLanguage = documentLanguage;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentAcquiring action.doWork, execution init");

		try {
			// calculating DOCUMENT_ACQUIRING properties values
			Map<QName,Serializable> properties = new HashMap<QName,Serializable>();
			properties.put(SinekartaModel.PROP_QNAME_TIMESTAMP_PROCESS_START, new Date(System.currentTimeMillis()));
			properties.put(SinekartaModel.PROP_QNAME_REFERENCE_ID, (String) this.nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NODE_UUID));
			
			// have we received info about the document type?
			if (documentType!=null) {
				// if yes the document is moving from another space
				properties.put(SinekartaModel.PROP_QNAME_DOCUMENT_MOVED, true);
				properties.put(SinekartaModel.PROP_QNAME_DOCUMENT_DATE, documentDate);
				properties.put(SinekartaModel.PROP_QNAME_DOCUMENT_TYPE, ""+documentType.getId());
				properties.put(SinekartaModel.PROP_QNAME_LANGUAGE, documentLanguage);
			}
			
			// adding DOCUMENT_ACQUIRING aspect to document
			nodeService.addAspect(actionedUponNodeRef, SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING, properties);		

			if (documentType!=null) {
				
				if (tracer.isDebugEnabled()) tracer.debug("DocumentAcquiring doWork : let's go to reorganize the archive");

				// invoking archive organization action
				Action documentOrganize = actionService.createAction(DocumentOrganize.ACTION_NAME_DOCUMENT_ORGANIZE);
				try {
					actionService.executeAction(documentOrganize, actionedUponNodeRef, false, false);
				} catch(Throwable t) {
					tracer.error("Unable to reorganize archive",t);
					throw new DocumentAcquiringException("Unable to reorganize archive",t);
				}

			}
		} catch (Throwable e) {
			tracer.error("DocumentAcquiring action.doWork, unable to acquire document", e);
			koReason="DocumentAcquiring action.doWork, unable to acquire document : " + e.getMessage();
			return false;
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("DocumentAcquiring action.doWork, finished ok");

		return true;
	}
	
	public String getKoReason() {
		return koReason;
	}
}
