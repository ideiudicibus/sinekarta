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
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.wizard.BaseWizardBean;
import org.apache.log4j.Logger;
import org.jenia.faces.util.Util;
import org.sinekarta.alfresco.action.DocumentDigitalSignatureApply;
import org.sinekarta.alfresco.action.DocumentDigitalSignatureInit;
import org.sinekarta.alfresco.action.DocumentDigitalSignaturePrepareAndAddDocument;
import org.sinekarta.alfresco.action.DocumentToPDFA;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;

/**
 * signing a single document is a particular type of multi-document signing
 * 
 * @author andrea.tessaro
 *
 */
public class GenericSingleSignDocumentsWizard extends BaseWizardBean {
	
	private static final long serialVersionUID = 1L;

	private static Logger tracer = Logger.getLogger(GenericSingleSignDocumentsWizard.class);

	protected static final String BUNDLE_SINGLE_SIGN_TITLE = "singleSign_title";

	protected static final String BUNDLE_SUMMARY = "summary";
	protected static final String BUNDLE_GENERIC_SIGN = "bundle.sinekarta-genericSign";
	protected static final String STEP_NAME_CERTIFICATE_CHOICE = "certificateChoice";
	protected static final String STEP_NAME_SIGN = "sign";

	protected transient ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_GENERIC_SIGN, org.jenia.faces.util.Util.getFacesContext().getViewRoot().getLocale(), Util.class.getClassLoader());

	private boolean error;
	private boolean fileAlreadyExists;
	
	private String description;

	private String documentName;
	private String documentNamePdf;
	
	private String documentPath;

	private NodeRef selectedDocumentNodeParent;

	private NodeRef selectedDocumentNode;

	private NodeRef selectedDocumentNodePdf;

	@Override
	public String getContainerTitle() {
		Object[] args = new Object[] {documentName};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_SINGLE_SIGN_TITLE), args);
	}
	
	private void init() {
		error=false;
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		
		su.setDataToAppletCertificateChoice(null);
		su.setDataFromAppletCertificateChoice(null);
		su.setDataToAppletSign(null);
		su.setDataFromAppletSign(null);
		description=null;
		selectedDocumentNodePdf=null;

		selectedDocumentNode = browseBean.getActionSpace().getNodeRef();

		documentName = (String) su.getNodeService().getProperty(selectedDocumentNode, ContentModel.PROP_NAME);
		
		documentPath = NodeTools.translatePath(su.getNodeService(), su.getNodeService().getPath(selectedDocumentNode));
		documentPath = documentPath.substring(0, documentPath.indexOf(documentName));
		
		// verify if a signed document version of this document already exists
		selectedDocumentNodeParent = su.getNodeService().getPrimaryParent(selectedDocumentNode).getParentRef();
		
		NodeRef exist = null;
		int i=0;
		do {
			// calculating new filename
			documentNamePdf = (String) su.getNodeService().getProperty(selectedDocumentNode, ContentModel.PROP_NAME);
			int dotIdx = documentNamePdf.lastIndexOf('.');
			if (i==0) {
				documentNamePdf = documentNamePdf.substring(0, dotIdx) + Configuration.getInstance().getSignedSuffix() + documentNamePdf.substring(dotIdx);
			} else {
				documentNamePdf = documentNamePdf.substring(0, dotIdx) + " ("+i+")" + Configuration.getInstance().getSignedSuffix() + documentNamePdf.substring(dotIdx);
			}
			
			// adjust documentName
			documentName = PDFTools.calculatePdfName(documentNamePdf);
	
			exist = su.getFileFolderService().searchSimple(selectedDocumentNodeParent, documentName);
			i++;
		} while(exist!=null);
		
		// preparazione area di input per applet

		prepareDataToAppletCertificateChoice();

		su.setDataFromAppletCertificateChoice(null);

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
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();

		// applicao firma ai documenti selezionati

		try {
			
			// invoking generic sign apply action
			Action documentDigitalSignatureApply = su.getActionService().createAction(DocumentDigitalSignatureApply.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_APPLY);
			documentDigitalSignatureApply.setParameterValue(DocumentDigitalSignatureApply.PARAM_CLIENT_AREA, su.getDataFromAppletSign());
			try {
				su.getActionService().executeAction(documentDigitalSignatureApply, selectedDocumentNodeParent, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to apply sign to document : " + t.getMessage(),t);
				throw new SignFailedException("Unable to apply sign to document : " + t.getMessage(),t);
			}
			
			su.getNodeService().setProperty(selectedDocumentNodePdf, ContentModel.PROP_NAME, documentName);
			
		} catch (SignFailedException e) {
			tracer.error(e.getMessage(),e);
			throw e;
		} catch (Exception e) {
			tracer.error("Unable to apply sign to document : " + e.getMessage(), e);
			throw new SignFailedException("Unable to apply sign to document : " + e.getMessage(), e);
		}

		return outcome;
	}
	
	/**
	 * preparing data to send to applet for sign process
	 * @param dataFromApplet encoded64 data received from applet certificate choice
	 * @return the data to passa to sign applet
	 */
	protected void prepareDataToAppletSign() {

		// calcolo impronte documenti selezionati
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();

		try {

			// copio il file per la conversione
			NodeRef folder = su.getNodeService().getPrimaryParent(selectedDocumentNode).getParentRef();
			
			FileInfo newFile = su.getFileFolderService().copy(selectedDocumentNode, folder, documentNamePdf);
			
			selectedDocumentNodePdf = newFile.getNodeRef();
			
			// conversione documento in PDF/A
			Action documentToPDFA = su.getActionService().createAction(DocumentToPDFA.ACTION_NAME_DOCUMENT_TO_PDFA);
			try {
				su.getActionService().executeAction(documentToPDFA, selectedDocumentNodePdf, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to execute PDF/A conversion : " + t.getMessage(),t);
				throw new SignFailedException("Unable to execute PDF/A conversion : " + t.getMessage(),t);
			}
			
			Action digitalSignaturePrepareAndAddDocument = su.getActionService().createAction(DocumentDigitalSignaturePrepareAndAddDocument.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_PREPARE_AND_ADD_DOCUMENT);
			digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_SIGN_DESCRIPTION, description);
			digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_SIGN_LOCATION, Constants.SIGN_LOCATION_ITALY);
			digitalSignaturePrepareAndAddDocument.setParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_CLIENT_AREA, su.getDataFromAppletCertificateChoice());
			try {
				su.getActionService().executeAction(digitalSignaturePrepareAndAddDocument, selectedDocumentNodePdf, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare data for document sign : " + t.getMessage(),t);
				throw new SignFailedException("Unable to prepare data for document sign : " + t.getMessage(),t);
			}
			su.setDataToAppletSign((String)digitalSignaturePrepareAndAddDocument.getParameterValue(DocumentDigitalSignaturePrepareAndAddDocument.PARAM_RESULT));
		} catch (SignFailedException e) {
			tracer.error(e.getMessage(),e);
			throw e;
		} catch (Exception e) {
			tracer.error("Unable to calculate document fingerprint.", e);
			throw new SignFailedException("Unable to calculate document fingerprint : " + e.getMessage(), e);
		}

	}
	
	/**
	 * prepare data to pass to applet for certificate choice
	 * @return encoded64 string to pass to applet
	 */
	protected void prepareDataToAppletCertificateChoice() {
		// calcolo impronte documenti selezionati
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		// conversione documento in PDF/A
		Action digitalSignatureInit = su.getActionService().createAction(DocumentDigitalSignatureInit.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_INIT);
		try {
			su.getActionService().executeAction(digitalSignatureInit, selectedDocumentNodeParent, false, false);
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		su.setDataToAppletCertificateChoice((String)digitalSignatureInit.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT));
	}
	
	@Override
	public String back() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			prepareDataToAppletCertificateChoice();

			su.setDataFromAppletCertificateChoice(null);

		} else if (stepName.equals(STEP_NAME_SIGN)) {
			
			prepareDataToAppletSign();

			su.setDataFromAppletSign(null);
			
		}

		return null;
	}

	public String next() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();

		if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			prepareDataToAppletCertificateChoice();

			su.setDataFromAppletCertificateChoice(null);

		} else if (stepName.equals(STEP_NAME_SIGN)) {
			
			// adjust documentName
			documentName = PDFTools.calculatePdfName(documentNamePdf);
	
			// verify if a signed document version of this document already exists
			NodeRef folder = su.getNodeService().getPrimaryParent(selectedDocumentNode).getParentRef();
			
			if (su.getFileFolderService().searchSimple(folder, documentName)!=null) { 
				Util.addFatalMessage(BUNDLE_GENERIC_SIGN, "fileAlreadyExists", "fileAlreadyExistsDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}

			prepareDataToAppletSign();

			su.setDataFromAppletSign(null);
			
		}

		return null;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getDocumentNamePdf() {
		return documentNamePdf;
	}

	public void setDocumentNamePdf(String documentNamePdf) {
		this.documentNamePdf = documentNamePdf;
	}

	public NodeRef getSelectedDocumentNodePdf() {
		return selectedDocumentNodePdf;
	}

	public void setSelectedDocumentNodePdf(NodeRef selectedDocumentNodePdf) {
		this.selectedDocumentNodePdf = selectedDocumentNodePdf;
	}

	public boolean isFileAlreadyExists() {
		return fileAlreadyExists;
	}

}
