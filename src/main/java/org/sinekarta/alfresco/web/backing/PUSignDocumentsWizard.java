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
import org.sinekarta.alfresco.action.DocumentPUSignApply;
import org.sinekarta.alfresco.action.DocumentPUSignPrepare;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeRefWrapper;
import org.sinekarta.alfresco.util.NodeTools;

public class PUSignDocumentsWizard extends BaseWizardBean {
	
	protected static final String BUNDLE_SUMMARY = "summary";
	protected static final String BUNDLE_PU_SIGN = "bundle.sinekarta-puSign";
	protected static final String STEP_NAME_CERTIFICATE_CHOICE = "certificateChoice";
	protected static final String STEP_NAME_SIGN = "sign";
	protected static final long serialVersionUID = 1L;
	protected static Logger tracer = Logger.getLogger(PUSignDocumentsWizard.class);

	protected transient ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_PU_SIGN, org.jenia.faces.util.Util.getFacesContext().getViewRoot().getLocale(), Util.class.getClassLoader());

	private DocumentType documentType;
	private Date archiveDateFrom;
	private Date archiveDateTo;
	private Date documentDateFrom;
	private Date documentDateTo;
	private String documentName;
	private boolean subspace;
	private boolean error;
	
	private List<NodeRefWrapper> documents;
	private List<NodeRefWrapper> selectedDocuments;
	
	private void init() {
		error=false;
		subspace=true;
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		selectedDocuments=new ArrayList<NodeRefWrapper>();
		documents=null;
		documentType=null;
		archiveDateFrom=null;
		archiveDateTo=null;
		documentName=null;
		su.setDataToAppletCertificateChoice(null);
		su.setDataFromAppletCertificateChoice(null);
		su.setDataToAppletSign(null);
		su.setDataFromAppletSign(null);
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

		try {
			
			// invoking pu sign apply action
			Action documentPUSignApply = su.getActionService().createAction(DocumentPUSignApply.ACTION_NAME_DOCUMENT_PU_SIGN_APPLY);
			documentPUSignApply.setParameterValue(DocumentPUSignApply.PARAM_CLIENT_AREA, su.getDataFromAppletSign());
			try {
				su.getActionService().executeAction(documentPUSignApply, browseBean.getActionSpace().getNodeRef(), false, false);
			} catch(Throwable t) {
				tracer.error("Unable to apply sign to document : " + t.getMessage(),t);
				throw new SignFailedException("Unable to apply sign to document : " + t.getMessage(),t);
			}

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
			su.setDataToAppletSign(su.getDataFromAppletCertificateChoice());
			// for each selected document
			for (NodeRefWrapper doc : selectedDocuments) {
				// invoking pu prepare sign action
				Action documentPUSignPrepare = su.getActionService().createAction(DocumentPUSignPrepare.ACTION_NAME_DOCUMENT_PU_SIGN_PREPARE);
				documentPUSignPrepare.setParameterValue(DocumentPUSignPrepare.PARAM_CLIENT_AREA, su.getDataToAppletSign());
				try {
					su.getActionService().executeAction(documentPUSignPrepare, doc.getNodeRef(), false, false);
				} catch(Throwable t) {
					tracer.error("Unable to prepare data for document sign : " + t.getMessage(),t);
					throw new SignFailedException("Unable to prepare data for document sign : " + t.getMessage(),t);
				}
				su.setDataToAppletSign((String)documentPUSignPrepare.getParameterValue(DocumentPUSignPrepare.PARAM_RESULT));
			}
		} catch (SignFailedException e) {
			tracer.error(e.getMessage(),e);
			throw e;
		} catch (Exception e) {
			tracer.error("Unable to calculate document fingerprint : " + e.getMessage(), e);
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
			su.getActionService().executeAction(digitalSignatureInit, browseBean.getActionSpace().getNodeRef(), false, false);
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
		if (selectedDocuments.isEmpty()) {
			Util.addFatalMessage(BUNDLE_PU_SIGN, "wrongSelection", "wrongSelectionDesc");
			Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() + 1);
			return null;
		}
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
		if (selectedDocuments.isEmpty()) {
			Util.addFatalMessage(BUNDLE_PU_SIGN, "wrongSelection", "wrongSelectionDesc");
			Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
			return null;
		}
		if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			prepareDataToAppletCertificateChoice();

			su.setDataFromAppletCertificateChoice(null);

		} else if (stepName.equals(STEP_NAME_SIGN)) {
			
			prepareDataToAppletSign();

			su.setDataFromAppletSign(null);
			
		}

		return null;
	}

	public String getSummary() {
		Object[] args = new Object[] {selectedDocuments.size()};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_SUMMARY), args);
	}
	
	/**
	 * search method (jsf backing bean method) to search for document that can be signed
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
							" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_PU_SIGN_REQUIRED+":true" +
							" AND ASPECT:\""+SinekartaModel.ASPECT_QNAME_RCS_SIGNATURE+"\"" +
							" AND -ASPECT:\""+SinekartaModel.ASPECT_QNAME_PU_SIGNATURE+"\"");
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

}
