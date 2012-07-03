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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PersonService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.MarkFolderPrepareException;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;
import org.sinekarta.sign.area.MarkDocument;
import org.sinekarta.sign.area.MarkDocumentArea;
import org.springframework.beans.factory.InitializingBean;

/**
 * mark folder create action.
 * this action will verify and (if needed) create the mark folder
 * 
 * - parameter documentType : the documentType (id) of document that will be marked
 * 
 * - return the serialize and encoded markDocumentArea
 * 
 * @author andrea.tessaro
 *
 */
public class MarkDocumentInit extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(MarkDocumentInit.class);

	public static final String ACTION_NAME_MARK_DOCUMENT_INIT = "sinekartaMarkDocumentInit";
	public static final String PARAM_DOCUMENT_TYPE = "documentType";
	public static final String PARAM_DOCUMENT_DATE = "documentDate";

	private String companyHomePath;
	private NodeService nodeService;
	private OwnableService ownableService;
	private FileFolderService fileFolderService;
	private ScriptService scriptService;
	private PersonService personService;
	private AuthenticationService authenticationService;
	private SearchService searchService;
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentInit action, execution init");
		
		Integer documentTypeId = (Integer) action.getParameterValue(PARAM_DOCUMENT_TYPE);
		if (documentTypeId==null) {
			tracer.error("no documentType for mark folder prepare.");
			throw new MarkFolderPrepareException("no documentType for mark folder prepare.");
		}
		DocumentType documentType;
		try {
			documentType = sinekartaDao.getDocumentType(documentTypeId);
		} catch (Exception e) {
			tracer.error("wrong documentType for mark folder prepare.", e);
			throw new MarkFolderPrepareException("wrong documentType for mark folder prepare.", e);
		}

		Date documentDate = (Date) action.getParameterValue(PARAM_DOCUMENT_DATE);
		if (documentDate==null) {
			tracer.error("no documentDate for mark folder prepare.");
			throw new MarkFolderPrepareException("no documentDate for mark folder prepare.");
		}

		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		MarkFolderPrepareWorker execAsSinekartaAdmin = new MarkFolderPrepareWorker(nodeService, searchService, 
				authenticationService, personService, scriptService,
				fileFolderService, ownableService, documentType, 
				actionedUponNodeRef, companyHomePath, documentDate);
		
		// run core of the action with sinekarta admin user
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on mark folder prepare : " + execAsSinekartaAdmin.getKoReason());
			throw new MarkFolderPrepareException("error on mark folder prepare : " + execAsSinekartaAdmin.getKoReason());
		}
		
		action.setParameterValue(PARAM_RESULT, execAsSinekartaAdmin.getMarkDocumentArea().toBase64String());

		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentInit action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_DOCUMENT_TYPE,
				DataTypeDefinition.INT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_DOCUMENT_TYPE))); 
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_DOCUMENT_DATE,
				DataTypeDefinition.DATE, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_DOCUMENT_DATE))); 
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setOwnableService(OwnableService ownableService) {
		this.ownableService = ownableService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public void setScriptService(ScriptService scriptService) {
		this.scriptService = scriptService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

}
class MarkFolderPrepareWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(MarkFolderPrepareWorker.class);

	private NodeService nodeService;
	private SearchService searchService;
	private AuthenticationService authenticationService;
	private PersonService personService;
	private ScriptService scriptService;
	private FileFolderService fileFolderService;
	private OwnableService ownableService;
	private DocumentType documentType;
	private NodeRef relativeTo;
	private String prefixedCompanyHome;
	private MarkDocumentArea markDocumentArea;
	private Date referenceDate;
	private String koReason;

	public MarkFolderPrepareWorker(NodeService nodeService, SearchService searchService, 
			AuthenticationService authenticationService, PersonService personService, ScriptService scriptService,
			FileFolderService fileFolderService, OwnableService ownableService, DocumentType documentType, 
			NodeRef relativeTo, String prefixedCompanyHome, Date referenceDate) {
		super();
		this.nodeService = nodeService;
		this.searchService = searchService;
		this.authenticationService = authenticationService;
		this.personService = personService;
		this.scriptService = scriptService;
		this.fileFolderService = fileFolderService;
		this.ownableService = ownableService;
		this.documentType = documentType;
		this.relativeTo = relativeTo;
		this.prefixedCompanyHome = prefixedCompanyHome;
		this.referenceDate = referenceDate;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentInit action.doWork, execution init");

		try {
			// preparing for query 
			StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
	
			// interpreting rule for target position of mark file
			String docLocationRule = documentType.getMarkLocationRule();
			if (docLocationRule.startsWith(Constants.SDF)) {
				// simple rule, based on simpledateformat
				SimpleDateFormat sdf = new SimpleDateFormat(docLocationRule.substring(Constants.SDF.length()));
				docLocationRule = sdf.format(referenceDate);
			} else if (docLocationRule.startsWith(Constants.JS)) {
				// getting the script node
				NodeRef scriptNodeRef = NodeTools.getNodeRefByPath(searchService, nodeService, storeRef, prefixedCompanyHome + docLocationRule.substring(Constants.JS.length()));
				if (scriptNodeRef == null) {
					tracer.error("Script does not exists : " + docLocationRule);
					throw new MarkFolderPrepareException("Script does not exists : " + docLocationRule);
				}
				// get the references we need to build the default scripting data-model (to run script)
	            String userName = authenticationService.getCurrentUserName();
	            NodeRef personRef = personService.getPerson(userName);
	            NodeRef homeSpaceRef = (NodeRef)nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);
	    		NodeRef companyHome = NodeTools.getNodeRefByPath(searchService, nodeService, storeRef, prefixedCompanyHome);
				if (companyHome == null) {
					tracer.error("companyHome does not exists : " + prefixedCompanyHome);
					throw new MarkFolderPrepareException("companyHome does not exists : " + prefixedCompanyHome);
				}
	            // the default scripting model provides access to well known objects and searching
	            // facilities - it also provides basic create/update/delete/copy/move services
	            Map<String, Object> model = scriptService.buildDefaultModel(
	                    personRef,
	                    companyHome,
	                    homeSpaceRef,
	                    scriptNodeRef,
	                    null,
	                    relativeTo);
	            // variabile aggiuntiva di sinekarta che indica la data di riferimento della marca temporale
	            // puo' essere utilizzata nello script per determinare la directory in cui inserire la marca temporale
	            model.put("referenceDate", referenceDate);
	            // running the script, The script must return a string, the lucene path to the target document folder
	            // path returned must be relative to archive folder (not start with a /
	            docLocationRule = (String) scriptService.executeScript(scriptNodeRef, null, model);
			}
			// getting noderef of archivio
			String archivioPath = Configuration.getInstance().getLuceneArchivePath();
			NodeRef archivio = NodeTools.getNodeRefByPath(searchService, nodeService, storeRef, prefixedCompanyHome + archivioPath);
			if (archivio == null) {
				tracer.error("archivio does not exists : " + archivioPath);
				throw new MarkFolderPrepareException("archivio does not exists : " + prefixedCompanyHome + archivioPath);
			}
			// get (or create) target folder
			NodeRef markFolder = NodeTools.deepCreateArchiveFolder(nodeService, searchService, fileFolderService, ownableService, storeRef, prefixedCompanyHome + archivioPath + docLocationRule);
	
			markDocumentArea = new MarkDocumentArea();
			markDocumentArea.setDocuments(new ArrayList<MarkDocument>());
			markDocumentArea.setMarkFolderPathNodeRefId(markFolder.getId());
			markDocumentArea.setMarkFolderPathStoreRefId(markFolder.getStoreRef().getIdentifier());
			markDocumentArea.setMarkFolderPathStoreRefProtocol(markFolder.getStoreRef().getProtocol());
			String path = NodeTools.translatePath(nodeService, nodeService.getPath(markFolder));
			markDocumentArea.setMarkFolderPath(path);
			markDocumentArea.setMarkDocumentType(Integer.toString(documentType.getId()));
		} catch (Throwable e) {
			tracer.error("DocumentMarkFolderPrepare action.doWork, generic error", e);
			koReason="DocumentMarkFolderPrepare action.doWork, generic error : " + e.getMessage();
			return false;
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentInit action.doWork, finished ok");

		return true;
	}

	public MarkDocumentArea getMarkDocumentArea() {
		return markDocumentArea;
	}

	public String getKoReason() {
		return koReason;
	}

}
