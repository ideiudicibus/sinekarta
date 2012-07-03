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

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;
import org.springframework.beans.factory.InitializingBean;

/**
 * document RCS digital signature prepare action.
 * this action will add the given document to list of signing documents, calculating finger print and digital signature attributes
 * 
 * - parameter clientArea : returned from SinekartaDigitalSignatureClient in executeDigitalSignature
 * 
 * - return a string to be passed to SinekartaDigitalSignatureClient constructor
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentRCSSignPrepare extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentRCSSignPrepare.class);
	public static final String ACTION_NAME_DOCUMENT_RCS_SIGN_PREPARE = "sinekartaDocumentRCSSignPrepare";
	public static final String PARAM_CLIENT_AREA = "clientArea";

	private String companyHomePath;
	private ActionService actionService;
	private NodeService nodeService;
	private SearchService searchService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSSignPrepare action, execution init");
		
		String clientArea = (String)action.getParameterValue(PARAM_CLIENT_AREA);
		if (clientArea==null) {
			tracer.error("no clientArea specified for generic sign.");
			throw new SignFailedException("no clientArea specified for generic sign.");
		}
		
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		DocumentRCSSignPrepareWorker execAsSinekartaAdmin = new DocumentRCSSignPrepareWorker(actionService, clientArea, actionedUponNodeRef);
		
		// running core of the action as sinekarta admin user
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on rcs sign prepare : " + execAsSinekartaAdmin.getKoReason());
			throw new SignFailedException("error on rcs sign prepare : " + execAsSinekartaAdmin.getKoReason());
		}

		action.setParameterValue(PARAM_RESULT, execAsSinekartaAdmin.getRet());

		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSSignPrepare action, execution end");
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

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

}
class DocumentRCSSignPrepareWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(DocumentRCSSignPrepare.class);

	private ActionService actionService;
	private NodeRef actionedUponNodeRef;
	private String clientArea;
	private String ret;
	private String koReason;

	public DocumentRCSSignPrepareWorker(ActionService actionService, String clientArea, NodeRef actionedUponNodeRef) {
		super();
		this.actionService = actionService;
		this.clientArea = clientArea;
		this.actionedUponNodeRef = actionedUponNodeRef;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSSignPrepare action.doWork, execution init");

		try {
			// invoking rcs prepare sign action
			Action digitalSignaturePrepareAndAddDocument = actionService.createAction(DocumentDigitalSignaturePrepareAndAddDocument.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_PREPARE_AND_ADD_DOCUMENT);
			digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_SIGN_DESCRIPTION, Constants.RCS_SIGN_REASON_PROCEDURA_DI_FIRMA_DIGITALE_PROPEDEUTICA_ALLA_CONSERVAZIONE_SOSTITUTIVA);
			digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_SIGN_LOCATION, Constants.SIGN_LOCATION_ITALY);
			digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_CLIENT_AREA, clientArea);
			try {
				actionService.executeAction(digitalSignaturePrepareAndAddDocument, actionedUponNodeRef, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare data for document sign",t);
				throw new SignFailedException("Unable to prepare data for document sign",t);
			}
			ret = (String)digitalSignaturePrepareAndAddDocument.getParameterValue(DocumentRCSSignPrepare.PARAM_RESULT);
		} catch (Throwable e) {
			tracer.error("DocumentRCSSignPrepare action.doWork, generic error", e);
			koReason="DocumentRCSSignPrepare action.doWork, generic error : " + e.getMessage();
			return false;
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSSignPrepare action.doWork, finished OK");

		return true;
	}

	public String getRet() {
		return ret;
	}
	
	public String getKoReason() {
		return koReason;
	}
}
