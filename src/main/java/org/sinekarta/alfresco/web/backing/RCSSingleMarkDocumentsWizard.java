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
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.web.app.Application;
import org.apache.log4j.Logger;
import org.jenia.faces.util.Util;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.NodeRefWrapper;

/**
 * marking a single document is a particular type of multi-document marking
 * 
 * @author andrea.tessaro
 *
 */
public class RCSSingleMarkDocumentsWizard extends RCSMarkDocumentsWizard {
	
	private static final long serialVersionUID = 1L;

	private static Logger tracer = Logger.getLogger(RCSSingleMarkDocumentsWizard.class);
	
	protected static final String BUNDLE_SINGLE_MARK_TITLE = "singleMark_title";

	private String documentName;

	/** 
	 * on init i create the selectedDocument list containing only selected document
	 */
	private void init() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();

		documentName = (String) su.getNodeService().getProperty(browseBean.getActionSpace().getNodeRef(), ContentModel.PROP_NAME);

		List<NodeRefWrapper> l = NodeRefWrapper.createList(su.getPermissionService(), su.getNodeService(), browseBean.getActionSpace().getNodeRef());
		setSelectedDocuments(l);
		setDocuments(l);
		
		// preparazione area di input per applet
		String dt = (String) su.getNodeService().getProperty(browseBean.getActionSpace().getNodeRef(), SinekartaModel.PROP_QNAME_DOCUMENT_TYPE);
		DocumentType documentType=null;
		try {
			documentType = su.getSinekartaDao().getDocumentType(Integer.parseInt(dt));
		} catch (Exception e) {
			tracer.error("Unable to find document type of document selected : " + e.getMessage(), e);
			throw new MarkFailedException("Unable to find document type of document selected : " + e.getMessage(), e);
		}
		setDocumentType(documentType);

//		initMarkDocument();
//
//		prepareDataToAppletCertificateChoice();
//		
//		setDataFromAppletCertificateChoice(null);

	}
	
	@Override
	public void init(Map<String, String> parameters) {
		super.init(parameters);
		init();
	}

	public String reset() {
		super.reset();
		init();
		return null;
	}

	@Override
	public String back() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			if (getDocumentDate()==null) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidDocumentDate", "invalidDocumentDateDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (getMarkFileName()==null || getMarkFileName().trim().equals("")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			// controllo se il nome del file contiene caratteri invalidi
			setMarkFileName(getMarkFileName().trim());
			if (getMarkFileName().indexOf('"')!=-1 ||
				getMarkFileName().indexOf('*')!=-1 ||
				getMarkFileName().indexOf('\\')!=-1 ||
				getMarkFileName().indexOf('>')!=-1 ||
				getMarkFileName().indexOf('<')!=-1 ||
				getMarkFileName().indexOf('?')!=-1 ||
				getMarkFileName().indexOf('/')!=-1 ||
				getMarkFileName().indexOf(':')!=-1 ||
				getMarkFileName().indexOf('|')!=-1 ||
				getMarkFileName().endsWith(".")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (getMarkDescription()==null || getMarkDescription().trim().equals("")) {
				setMarkDescription(getMarkFileName());
			}
			
		}

		return super.back();
	}

	@Override
	public String next() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			if (getDocumentDate()==null) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidDocumentDate", "invalidDocumentDateDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (getMarkFileName()==null || getMarkFileName().trim().equals("")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			// controllo se il nome del file contiene caratteri invalidi
			setMarkFileName(getMarkFileName().trim());
			if (getMarkFileName().indexOf('"')!=-1 ||
				getMarkFileName().indexOf('*')!=-1 ||
				getMarkFileName().indexOf('\\')!=-1 ||
				getMarkFileName().indexOf('>')!=-1 ||
				getMarkFileName().indexOf('<')!=-1 ||
				getMarkFileName().indexOf('?')!=-1 ||
				getMarkFileName().indexOf('/')!=-1 ||
				getMarkFileName().indexOf(':')!=-1 ||
				getMarkFileName().indexOf('|')!=-1 ||
				getMarkFileName().endsWith(".")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (getMarkDescription()==null || getMarkDescription().trim().equals("")) {
				setMarkDescription(getMarkFileName());
			}
			
		}

		return super.next();
	}

	@Override
	public String getContainerTitle() {
		Object[] args = new Object[] {documentName};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_SINGLE_MARK_TITLE), args);
	}
	
}
