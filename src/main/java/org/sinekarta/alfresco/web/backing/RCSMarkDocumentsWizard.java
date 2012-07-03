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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.wizard.BaseWizardBean;
import org.apache.log4j.Logger;
import org.jenia.faces.util.Util;
import org.sinekarta.alfresco.action.DocumentDigitalSignatureInit;
import org.sinekarta.alfresco.action.DocumentTimestampMarkApply;
import org.sinekarta.alfresco.action.DocumentTimestampMarkInit;
import org.sinekarta.alfresco.action.DocumentTimestampMarkPrepare;
import org.sinekarta.alfresco.action.MarkDocumentAdd;
import org.sinekarta.alfresco.action.MarkDocumentInit;
import org.sinekarta.alfresco.action.MarkDocumentPrepare;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.MarkDocumentAlreadyExistsException;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeRefWrapper;
import org.sinekarta.alfresco.util.NodeTools;

/**
 * timestamp mark wizard
 * 
 * @author andrea.tessaro
 *
 */
public class RCSMarkDocumentsWizard extends BaseWizardBean {
	
	protected static final String BUNDLE_SUMMARY = "summary";
	protected static final String BUNDLE_RCS_MARK = "bundle.sinekarta-rcsMark";
	
	protected static final String STEP_NAME_DOCUMENT_ATTRIBUTES = "documentAttributes";
	protected static final String STEP_NAME_DOCUMENT_SELECTION = "documentSelection";
	protected static final String STEP_NAME_CERTIFICATE_CHOICE = "certificateChoice";
	protected static final String STEP_NAME_SIGN = "sign";
	protected static final long serialVersionUID = 1L;
	protected static Logger tracer = Logger.getLogger(RCSMarkDocumentsWizard.class);

	protected transient ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_RCS_MARK, org.jenia.faces.util.Util.getFacesContext().getViewRoot().getLocale(), Util.class.getClassLoader());

	private DocumentType documentType;
	private Date documentDate;
	private Date archiveDateFrom;
	private Date archiveDateTo;
	private Date documentDateFrom;
	private Date documentDateTo;
	private String documentName;
	private boolean subspace;
	private boolean error;
	
	private String markFileName;
	private String markDescription;
	
	private List<NodeRefWrapper> documents;
	private List<NodeRefWrapper> selectedDocuments;
	
	private String markDocumentArea;

	private void init() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		error=false;
		subspace=true;
		selectedDocuments=new ArrayList<NodeRefWrapper>();
		documents=null;
		documentType=null;
		documentName=null;
		documentDate=null;
		documentDateFrom=null;
		documentDateTo=null;
		archiveDateFrom=null;
		archiveDateTo=null;
		su.setDataToAppletCertificateChoice(null);
		su.setDataFromAppletCertificateChoice(null);
		su.setDataToAppletSign(null);
		su.setDataFromAppletSign(null);
		markFileName=null;
		markDescription=null;
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
	 * called an finish button is pressed
	 * this method apply the sign, create the p7m and (calling the TSA) create the m7m
	 * the mark document is saved into archive
	 * 
	 */
	protected String finishImpl(FacesContext context, String outcome)
			throws Exception {

		// interpretazione area ricevuta dall'applet
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();

		// conversione documento in PDF/A
		Action documentTimestampMarkApply = su.getActionService().createAction(DocumentTimestampMarkApply.ACTION_NAME_DOCUMENT_RCS_MARK_SIGN_APPLY);
		documentTimestampMarkApply.setParameterValue(DocumentTimestampMarkApply.PARAM_MARK_AREA, markDocumentArea);
		documentTimestampMarkApply.setParameterValue(DocumentTimestampMarkApply.PARAM_CLIENT_AREA, su.getDataFromAppletSign());
		try {
			su.getActionService().executeAction(documentTimestampMarkApply, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		
		return outcome;
	}
	
	/**
	 * using documentType calculate the folder into which save the timestamp mark file
	 */
	protected void initMarkDocument() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		
		// invoking action for calculating mark folder
		Action markFolderPrepare = su.getActionService().createAction(MarkDocumentInit.ACTION_NAME_MARK_DOCUMENT_INIT);
		markFolderPrepare.setParameterValue(MarkDocumentInit.PARAM_DOCUMENT_TYPE, documentType.getId());
		markFolderPrepare.setParameterValue(MarkDocumentInit.PARAM_DOCUMENT_DATE, documentDate);
		try {
			su.getActionService().executeAction(markFolderPrepare, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(Throwable t) {
			tracer.error("Unable to calculate mark folder : " + t.getMessage(),t);
			throw new MarkFailedException("Unable to calculate mark folder : " + t.getMessage(),t);
		}
		markDocumentArea = (String)markFolderPrepare.getParameterValue(MarkDocumentInit.PARAM_RESULT);
		
	}
	
	/**
	 * prepare xml document to be signed and marked
	 * see specifications for more details
	 */
	protected void prepareMarkDocument() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		
		for (NodeRefWrapper node : selectedDocuments) {
			// invoking action for producing timestamp mark file
			Action markDocumentAdd = su.getActionService().createAction(MarkDocumentAdd.ACTION_NAME_MARK_DOCUMENT_ADD);
			markDocumentAdd.setParameterValue(MarkDocumentAdd.PARAM_MARK_AREA, markDocumentArea);
			try {
				su.getActionService().executeAction(markDocumentAdd, node.getNodeRef(), false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare mark documenr : " + t.getMessage(),t);
				throw new MarkFailedException("Unable to prepare mark documenr : " + t.getMessage(),t);
			}
			markDocumentArea = (String)markDocumentAdd.getParameterValue(MarkDocumentAdd.PARAM_RESULT);
		}

		
		// preparazione documento di marca temporale
		Action markDocumentPrepare = su.getActionService().createAction(MarkDocumentPrepare.ACTION_NAME_MARK_DOCUMENT_PREPARE);
		markDocumentPrepare.setParameterValue(MarkDocumentPrepare.PARAM_MARK_AREA, markDocumentArea);
		markDocumentPrepare.setParameterValue(MarkDocumentPrepare.PARAM_MARK_DESCRIPTIONR, markDescription);
		markDocumentPrepare.setParameterValue(MarkDocumentPrepare.PARAM_MARK_FILENAME, markFileName);
		try {
			su.getActionService().executeAction(markDocumentPrepare, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(MarkDocumentAlreadyExistsException e) {
			throw e;
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		markDocumentArea = (String)markDocumentPrepare.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT);

	}
	
	/**
	 * read data received from applet (certificate choice) and prepare data
	 * to send to applet for sign
	 * @param dataFromApplet applet data of certificate choice
	 * @return data to send to applet for signing
	 */
	protected void prepareDataToAppletSign() {

		// calcolo impronte documenti selezionati
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		// conversione documento in PDF/A
		Action documentTimestampMarkPrepare = su.getActionService().createAction(DocumentTimestampMarkPrepare.ACTION_NAME_DOCUMENT_TIMESTAMP_MARK_PREPARE);
		documentTimestampMarkPrepare.setParameterValue(DocumentTimestampMarkPrepare.PARAM_MARK_AREA, markDocumentArea);
		documentTimestampMarkPrepare.setParameterValue(DocumentTimestampMarkPrepare.PARAM_CLIENT_AREA, su.getDataFromAppletCertificateChoice());
		try {
			su.getActionService().executeAction(documentTimestampMarkPrepare, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(MarkDocumentAlreadyExistsException e) {
			throw e;
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		su.setDataToAppletSign((String)documentTimestampMarkPrepare.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT));

	}
	
	/**
	 * prepare the area to pass to appet for certificate choice
	 * 
	 * @return encoded64 string to pass to applet
	 */
	protected void prepareDataToAppletCertificateChoice() {
		// calcolo impronte documenti selezionati
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		// conversione documento in PDF/A
		Action documentTimestampMarkInit = su.getActionService().createAction(DocumentTimestampMarkInit.ACTION_NAME_DOCUMENT_TIMESTAMP_MARK_INIT);
		documentTimestampMarkInit.setParameterValue(DocumentTimestampMarkInit.PARAM_MARK_AREA, markDocumentArea);
		try {
			su.getActionService().executeAction(documentTimestampMarkInit, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		su.setDataToAppletCertificateChoice((String)documentTimestampMarkInit.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT));
	}
	
	@Override
	public String back() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		if (stepName.equals(STEP_NAME_DOCUMENT_ATTRIBUTES)) {

			// nothing to do...
			
		} else if (stepName.equals(STEP_NAME_DOCUMENT_SELECTION)) {
			
			if (documentDate==null) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidDocumentDate", "invalidDocumentDateDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markFileName==null || markFileName.trim().equals("")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			// controllo se il nome del file contiene caratteri invalidi
			markFileName = markFileName.trim();
			if (markFileName.indexOf('"')!=-1 ||
				markFileName.indexOf('*')!=-1 ||
				markFileName.indexOf('\\')!=-1 ||
				markFileName.indexOf('>')!=-1 ||
				markFileName.indexOf('<')!=-1 ||
				markFileName.indexOf('?')!=-1 ||
				markFileName.indexOf('/')!=-1 ||
				markFileName.indexOf(':')!=-1 ||
				markFileName.indexOf('|')!=-1 ||
				markFileName.endsWith(".")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markDescription==null || markDescription.trim().equals("")) {
				markDescription = markFileName;
			}
			
		} else if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			if (selectedDocuments.isEmpty()) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "wrongSelection", "wrongSelectionDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() + 1);
				return null;
			}
			
			initMarkDocument();
			
			prepareDataToAppletCertificateChoice();

			su.setDataFromAppletCertificateChoice(null);

		} else if (stepName.equals(STEP_NAME_SIGN)) {
			
			prepareMarkDocument();
			
			try {
				prepareDataToAppletSign();
			} catch (MarkDocumentAlreadyExistsException e) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "markFileNameAlreadyExists", "markFileNameAlreadyExistsDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}

			su.setDataFromAppletSign(null);
			
		}

		return null;
	}

	public String next() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		if (stepName.equals(STEP_NAME_DOCUMENT_ATTRIBUTES)) {
			
			// nothing to do...
			
		} else if (stepName.equals(STEP_NAME_DOCUMENT_SELECTION)) {
			
			if (documentDate==null) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidDocumentDate", "invalidDocumentDateDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markFileName==null || markFileName.trim().equals("")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			// controllo se il nome del file contiene caratteri invalidi
			markFileName = markFileName.trim();
			if (markFileName.indexOf('"')!=-1 ||
				markFileName.indexOf('*')!=-1 ||
				markFileName.indexOf('\\')!=-1 ||
				markFileName.indexOf('>')!=-1 ||
				markFileName.indexOf('<')!=-1 ||
				markFileName.indexOf('?')!=-1 ||
				markFileName.indexOf('/')!=-1 ||
				markFileName.indexOf(':')!=-1 ||
				markFileName.indexOf('|')!=-1 ||
				markFileName.endsWith(".")) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markDescription==null || markDescription.trim().equals("")) {
				markDescription = markFileName;
			}
			
		} else if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			if (selectedDocuments.isEmpty()) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "wrongSelection", "wrongSelectionDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			initMarkDocument();
			
			prepareDataToAppletCertificateChoice();

			su.setDataFromAppletCertificateChoice(null);

		} else if (stepName.equals(STEP_NAME_SIGN)) {
			
			prepareMarkDocument();
			
			try {
				prepareDataToAppletSign();
			} catch (MarkDocumentAlreadyExistsException e) {
				Util.addFatalMessage(BUNDLE_RCS_MARK, "markFileNameAlreadyExists", "markFileNameAlreadyExistsDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}

			su.setDataFromAppletSign(null);
			
		}

		return null;
	}

	public String getSummary() {
		Object[] args = new Object[] {selectedDocuments.size()};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_SUMMARY), args);
	}
	
	/**
	 * search method (jsf backing bean method) to search for document that can be marked
	 * 
	 * @return jsf outcome action
	 */
	public String search() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
		StringBuffer lucenePath=new StringBuffer(NodeTools.translateNamespacePath(su.getNamespaceService(), browseBean.getActionSpace().getNodePath()));
		if (subspace){
			lucenePath.append("//*");
		} else {
			lucenePath.append("/*");
		}
		StringBuffer querySb = new StringBuffer("PATH:\""+lucenePath.toString()+"\" AND TYPE:\""+Constants.STANDARD_CONTENT_MODEL_PREFIX+":content\"" +
							" AND ASPECT:\""+SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING+"\"" +
							" AND ASPECT:\""+SinekartaModel.ASPECT_QNAME_RCS_SIGNATURE+"\"" +
							" AND -ASPECT:\""+SinekartaModel.ASPECT_QNAME_SUBSTITUTIVE_PRESERVATION+"\"" +
							" AND ( " +
									"@"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_PU_SIGN_REQUIRED+":false " +
									"OR (@"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_PU_SIGN_REQUIRED+":true " +
									"AND ASPECT:\""+SinekartaModel.ASPECT_QNAME_PU_SIGNATURE+"\")" +
							")" 
							);
		if (documentName!=null && !documentName.trim().equals("")){
			String[] words = documentName.split(" ");
			querySb.append(" AND ( ");
			for (int i=0;i<words.length;i++) {
				String word = words[i];
				querySb.append(" @"+Constants.STANDARD_CONTENT_MODEL_PREFIX_EXTENDED+"name:\""+word+"\"");
			}
			querySb.append(" ) ");
		} 
		if (documentType!=null) {
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_DOCUMENT_TYPE+":\""+Integer.toString(documentType.getId())+"\"");
		}
		if (documentDateFrom!=null && documentDateTo==null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_DOCUMENT_DATE+":["+sdf.format(documentDateFrom)+" TO MAX]");
		}
		if (documentDateTo!=null && documentDateFrom==null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_DOCUMENT_DATE+":[MIN TO "+sdf.format(documentDateTo)+"]");
		}
		if (documentDateFrom!=null && documentDateTo!=null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_DOCUMENT_DATE+":["+sdf.format(documentDateFrom)+" TO "+sdf.format(documentDateTo)+"]");
		}
		if (archiveDateFrom!=null && archiveDateTo==null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_TIMESTAMP_PROCESS_START+":["+sdf.format(archiveDateFrom)+" TO MAX]");
		}
		if (archiveDateTo!=null && archiveDateFrom==null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_TIMESTAMP_PROCESS_START+":[MIN TO "+sdf.format(archiveDateTo)+"]");
		}
		if (archiveDateFrom!=null && archiveDateTo!=null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_TIMESTAMP_PROCESS_START+":["+sdf.format(archiveDateFrom)+" TO "+sdf.format(archiveDateTo)+"]");
		}
        SearchParameters sp = new SearchParameters();
        sp.addStore(storeRef);
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(querySb.toString());
        sp.addSort(SearchParameters.SORT_IN_DOCUMENT_ORDER_ASCENDING);
        ResultSet rs = null;
        try {
			rs = su.getSearchService().query(sp);
			documents = NodeRefWrapper.createList(su.getNodeService(), rs,getDocumentPath());
        } finally {
        	if (rs!=null) rs.close();
        }
		return null;
	}
	
	public String selectAll() {
		selectedDocuments.addAll(documents);
		return null;
	}
	
	public String selectNone() {
		selectedDocuments.clear();
		return null;
	}
	
	public DocumentType getDocumentType() {
		return documentType;
	}

	public void setDocumentType(DocumentType documentType) {
		this.documentType = documentType;
	}

	public Date getArchiveDateFrom() {
		return archiveDateFrom;
	}

	public void setArchiveDateFrom(Date archiveDateFrom) {
		this.archiveDateFrom = archiveDateFrom;
	}

	public Date getArchiveDateTo() {
		return archiveDateTo;
	}

	public void setArchiveDateTo(Date archiveDateTo) {
		this.archiveDateTo = archiveDateTo;
	}

	public String getDocumentPath() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		return NodeTools.translatePath(su.getNodeService(), browseBean.getActionSpace().getNodePath());
	}

	public String getMarkFolderPath() {
		// TODO this value has to be calculated, how???
		return "";
//		SinekartaUtility su = (SinekartaUtility)Util.getFacesBean(Constants.SINEKARTA_UTILITY_BACKING_BEAN);
//		StoreRef storeRef = new StoreRef(markDocumentArea.getMarkFolderPathStoreRefProtocol(), markDocumentArea.getMarkFolderPathStoreRefId());
//		NodeRef markFolderNodeRef = new NodeRef(storeRef, markDocumentArea.getMarkFolderPathNodeRefId());
//		return NodeTools.translatePath(su.getNodeService(), su.getNodeService().getPath(markFolderNodeRef));
	}

	public boolean isSubspace() {
		return subspace;
	}

	public void setSubspace(boolean subspace) {
		this.subspace = subspace;
	}

	public List<NodeRefWrapper> getDocuments() {
		return documents;
	}

	public void setDocuments(List<NodeRefWrapper> documents) {
		this.documents = documents;
	}

	public List<NodeRefWrapper> getSelectedDocuments() {
		return selectedDocuments;
	}

	public void setSelectedDocuments(List<NodeRefWrapper> selectedDocuments) {
		this.selectedDocuments = selectedDocuments;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getMarkFileName() {
		return markFileName;
	}

	public void setMarkFileName(String markFileName) {
		this.markFileName = markFileName;
	}

	public String getMarkDescription() {
		return markDescription;
	}

	public void setMarkDescription(String markDescription) {
		this.markDescription = markDescription;
	}

	public boolean isError() {
		return error;
	}

	public Date getDocumentDateFrom() {
		return documentDateFrom;
	}

	public void setDocumentDateFrom(Date documentDateFrom) {
		this.documentDateFrom = documentDateFrom;
	}

	public Date getDocumentDateTo() {
		return documentDateTo;
	}

	public void setDocumentDateTo(Date documentDateTo) {
		this.documentDateTo = documentDateTo;
	}

	public Date getDocumentDate() {
		return documentDate;
	}

	public void setDocumentDate(Date documentDate) {
		this.documentDate = documentDate;
	}

}
