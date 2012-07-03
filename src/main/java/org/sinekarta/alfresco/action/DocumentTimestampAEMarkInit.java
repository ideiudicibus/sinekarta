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

import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.NodeTools;
import org.springframework.beans.factory.InitializingBean;

/**
 * document RCS digital signature for timestamp mark init action.
 * this action will initialize client area for timestamp mark done by the client
 * 
 * - parameter markArea : returned from MarkDocumentPrepare action
 * 
 * - return a string to be passed to SinekartaDigitalSignatureClient constructor
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentTimestampAEMarkInit extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentTimestampAEMarkInit.class);
	public static final String ACTION_NAME_DOCUMENT_TIMESTAMP_AEMARK_INIT = "sinekartaDocumentTimestampAEMarkInit";
	public static final String PARAM_MARK_AREA = "markArea";

	private String companyHomePath;
	private ActionService actionService;
	private NodeService nodeService;
	private SearchService searchService;
	private PersonService personService;
	private FileFolderService fileFolderService;
	private NamespaceService namespaceService;
	private AuthenticationService authenticationService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkInit action, execution init");
		
		String markArea = (String)action.getParameterValue(PARAM_MARK_AREA);
		if (markArea==null) {
			tracer.error("no clientArea specified for aemark sign.");
			throw new MarkFailedException("no clientArea specified for timestamp aemark init.");
		}
				
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		String userName = authenticationService.getCurrentUserName();

		// piglio la user home
		NodeRef person = personService.getPerson(userName);
		NodeRef homeFolder = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, nodeService.getProperty(person, ContentModel.PROP_HOMEFOLDER));
		// creo la directory sinekarta temp dentro la user home
		NodeRef tmpMarkFolder = NodeTools.deepCreateFolder(nodeService, searchService, fileFolderService, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 
				NodeTools.translateNamespacePath(namespaceService, nodeService.getPath(homeFolder)) + Configuration.getInstance().getUserSpaceTemporaryFolder());

		DocumentTimestampAEMarkInitWorker execAsSinekartaAdmin = new DocumentTimestampAEMarkInitWorker(actionService, markArea, personService, 
				nodeService, searchService, fileFolderService, namespaceService, sinekartaAdminUserId, tmpMarkFolder);
		
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on timestamp aemark init : " + execAsSinekartaAdmin.getKoReason());
			throw new MarkFailedException("error on timestamp aemark init : " + execAsSinekartaAdmin.getKoReason());
		}

		action.setParameterValue(PARAM_RESULT, execAsSinekartaAdmin.getRet());

		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkInit action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_MARK_AREA,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_MARK_AREA))); 
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

}
class DocumentTimestampAEMarkInitWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(DocumentTimestampAEMarkInit.class);

	private ActionService actionService;
	@SuppressWarnings("unused")
	private String markArea;
	private String ret;
	private NodeRef tmpMarkFolder;
	private String koReason;

	public DocumentTimestampAEMarkInitWorker(ActionService actionService, String markArea, 
			PersonService personService, NodeService nodeService, SearchService searchService, 
			FileFolderService fileFolderService, NamespaceService namespaceService, String userId, NodeRef tmpMarkFolder) {
		super();
		this.actionService = actionService;
		this.markArea = markArea;
		this.tmpMarkFolder = tmpMarkFolder;

	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSAEMarkSignPrepare action.doWork, execution init");

		try {
			// invoking prepare mar init
			Action documentDigitalSignatureInit = actionService.createAction(DocumentDigitalSignatureInit.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_INIT);
			try {
				actionService.executeAction(documentDigitalSignatureInit, tmpMarkFolder, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare data for document sign",t);
				throw new MarkFailedException("Unable to prepare data for document sign",t);
			}
			ret = (String)documentDigitalSignatureInit.getParameterValue(DocumentRCSSignPrepare.PARAM_RESULT);
		} catch (Throwable e) {
			tracer.error("DocumentRCSAEMarkSignPrepare action.doWork, generic error", e);
			koReason="DocumentRCSAEMarkSignPrepare action.doWork, generic error : " + e.getMessage();
			return false;
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSAEMarkSignPrepare action.doWork, finished OK");

		return true;
	}
	
	public String getRet() {
		return ret;
	}
	
	public String getKoReason() {
		return koReason;
	}
}
