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

import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;

public class ConfigurationDialog extends BaseDialogBean {

	private static final long serialVersionUID = 1L;

	protected static Logger tracer = Logger.getLogger(PUSignDocumentsWizard.class);
	
	private String tipoDocumentoFiltro;
	
	private List<DocumentType> documentTypes;
	
	private DocumentType selectedDocumentType;
	
	private boolean error;

	private boolean editing;

	@Override
	public void init(Map<String, String> parameters) {
		super.init(parameters);
		error=false;
		editing=false;
	}
	
	@Override
	protected String finishImpl(FacesContext context, String outcome) throws Throwable {
		return null;
	}
	
	public String save() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		SinekartaDao dao = su.getSinekartaDao();
		if (selectedDocumentType.getTimestampUpdate()==null) {
			// new type, i will persist
			dao.save(selectedDocumentType);
		} else {
			dao.update(selectedDocumentType);
		}
		if (documentTypes!=null && documentTypes.contains(selectedDocumentType)) {
			int idx = documentTypes.indexOf(selectedDocumentType);
			documentTypes.remove(idx);
			documentTypes.add(idx, selectedDocumentType);
		}
		return search();
	}
	
	public String back() {
		editing=false;
		return "dialog:configuration";
	}
	
	public String create() {
		editing=true;
		selectedDocumentType = new DocumentType();
		selectedDocumentType.setMarkLocationRule("sdf:'/cm:Anno 'yyyy'/cm:Marche temporali'");
		selectedDocumentType.setDocLocationRule("sdf:'/cm:Anno 'yyyy'/cm:documenti'");
		selectedDocumentType.setDefaultLanguage("ita");
		selectedDocumentType.setPdfaConvertNeeded(true);
		return "dialog:configuration";
	}
	
	public String edit() {
		editing=true;
		selectedDocumentType=(DocumentType)selectedDocumentType.clone();
		return "dialog:configuration";
	}
	
	public String search() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		SinekartaDao dao = su.getSinekartaDao();
		documentTypes = dao.getDocumentTypesByDescription(tipoDocumentoFiltro);
		editing=false;
		return "dialog:configuration";
	}

	public String getTipoDocumentoFiltro() {
		return tipoDocumentoFiltro;
	}

	public void setTipoDocumentoFiltro(String tipoDocumentoFiltro) {
		this.tipoDocumentoFiltro = tipoDocumentoFiltro;
	}

	public List<DocumentType> getDocumentTypes() {
		return documentTypes;
	}

	public void setDocumentTypes(List<DocumentType> documentTypes) {
		this.documentTypes = documentTypes;
	}

	public DocumentType getSelectedDocumentType() {
		return selectedDocumentType;
	}

	public void setSelectedDocumentType(DocumentType selectedDocumentType) {
		this.selectedDocumentType = selectedDocumentType;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public boolean isEditing() {
		return editing;
	}

	public void setEditing(boolean editing) {
		this.editing = editing;
	}

}
