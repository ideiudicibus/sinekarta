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
package org.sinekarta.alfresco.web.backing;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.wizard.BaseWizardBean;
import org.apache.log4j.Logger;
import org.jenia.faces.util.Util;
import org.sinekarta.alfresco.action.DocumentAcquiring;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.ArchiveOrganizatorException;
import org.sinekarta.alfresco.util.NodeTools;

/**
 * signing a single document is a particular type of multi-document signing
 * 
 * @author andrea.tessaro
 *
 */
public class MoveToArchiveWizard extends BaseWizardBean {
	
	private static final long serialVersionUID = 1L;

	private static Logger tracer = Logger.getLogger(MoveToArchiveWizard.class);

	protected static final String BUNDLE_MOVE_TO_ARCHIVE_TITLE = "moveToArchive_title";

	protected static final String BUNDLE_SUMMARY = "summary";
	protected static final String BUNDLE_MOVE_TO_ARCHIVE = "bundle.sinekarta-moveToArchive";

	protected static final String STEP_NAME_PREPARE_TO_MOVE = "prepareToMove";
	protected static final String STEP_NAME_MOVE_SUMMARY = "moveSummary";

	protected transient ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_MOVE_TO_ARCHIVE, org.jenia.faces.util.Util.getFacesContext().getViewRoot().getLocale(), Util.class.getClassLoader());

	private boolean error;
	private boolean fileAlreadyExists;
	
	private DocumentType documentType;
	private Date dataDocumento;
	private String lingua;
	
	private String documentPath;
	private String documentName;
	private NodeRef selectedDocumentNode;
	
	@Override
	public String getContainerTitle() {
		Object[] args = new Object[] {documentName};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_MOVE_TO_ARCHIVE_TITLE), args);
	}
	
	private void init() {
		error=false;
		dataDocumento=new Date();
		lingua=null;
		documentType=null;

		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		
		selectedDocumentNode = browseBean.getActionSpace().getNodeRef();

		documentName = (String) su.getNodeService().getProperty(selectedDocumentNode, ContentModel.PROP_NAME);
		documentPath = NodeTools.translatePath(su.getNodeService(), su.getNodeService().getPath(selectedDocumentNode));
		documentPath = documentPath.substring(0, documentPath.indexOf(documentName));
		
	}
	
	@Override
	public String cancel() {
		return super.cancel();
	}

	@Override
	protected String getErrorOutcome(Throwable exception) {
		error=true;
		return super.getErrorOutcome(exception);
	}

	@Override
	public void init(Map<String, String> parameters) {
		super.init(parameters);
		init();
	}

	public String reset() {
		init();
		return null;
	}
	
	/**
	 * this method is called when finish button is pressed
	 * applying the sign returned from applet
	 */
	protected String finishImpl(FacesContext context, String outcome)
			throws Exception {
		if (dataDocumento==null) {
			context.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_ERROR, "E' necessario impostare una data del documento", "E' necessario impostare una data del documento"));
			Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
			return null;
		}
		
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		
		Action documentAcquiring = su.getActionService().createAction(DocumentAcquiring.ACTION_NAME_DOCUMENT_ACQUIRING);
		documentAcquiring.setParameterValue(DocumentAcquiring.PARAM_DOCUMENT_DATE, dataDocumento);
		documentAcquiring.setParameterValue(DocumentAcquiring.PARAM_DOCUMENT_LANGUAGE, lingua);
		documentAcquiring.setParameterValue(DocumentAcquiring.PARAM_DOCUMENT_TYPE, documentType.getId());
		try {
			su.getActionService().executeAction(documentAcquiring, selectedDocumentNode, false, false);
		} catch(Throwable t) {
			tracer.error("Unable to move document : " + t.getMessage(),t);
			throw new ArchiveOrganizatorException("Unable to move document : " + t.getMessage(),t);
		}
		

		return outcome;
	}
	
	public String getSummary() {
		Object[] args = new Object[] {1};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_SUMMARY), args);
	}
	
	public String getDocumentPath() {
		return documentPath;
	}

	public NodeRef getSelectedDocumentNode() {
		return selectedDocumentNode;
	}

	public void setSelectedDocumentNode(NodeRef selectedDocumentNode) {
		this.selectedDocumentNode = selectedDocumentNode;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public boolean isError() {
		return error;
	}

	public boolean isFileAlreadyExists() {
		return fileAlreadyExists;
	}

	public String getLingua() {
		return lingua;
	}

	public void setLingua(String lingua) {
		this.lingua = lingua;
	}

	public DocumentType getDocumentType() {
		return documentType;
	}

	public void setDocumentType(DocumentType documentType) {
		this.documentType = documentType;
	}

	public Date getDataDocumento() {
		return dataDocumento;
	}

	public void setDataDocumento(Date dataDocumento) {
		this.dataDocumento = dataDocumento;
	}

}
