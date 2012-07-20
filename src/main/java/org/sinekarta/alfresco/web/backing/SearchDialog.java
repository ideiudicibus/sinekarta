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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import javax.faces.context.FacesContext;

import org.alfresco.web.bean.dialog.BaseDialogBean;
import org.alfresco.web.bean.search.SearchContext;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;

public class SearchDialog extends BaseDialogBean {

	private static final long serialVersionUID = 1L;

	private static final String OUTCOME_BROWSE = "browse";

	private String testo;
	private DocumentType documentType;
	private Date archiveDateFrom;
	private Date archiveDateTo;
	private Date documentDateFrom;
	private Date documentDateTo;
	private boolean documentTypeNotSet;

	private boolean error;

	@Override
	public void init(Map<String, String> parameters) {
		super.init(parameters);
		error = false;
	}

	@Override
	protected String finishImpl(FacesContext context, String outcome)
			throws Throwable {
		return null;
	}

	public String search() {
		SearchContext search = new SearchContext() {
			private static final long serialVersionUID = 1L;

			@Override
			public String buildQuery(int minimum) {
				// get the searcher object to build the query
				SinekartaUtility su = SinekartaUtility.getCurrentInstance();
				StringBuffer lucenePath=new StringBuffer(NodeTools.translateNamespacePath(su.getNamespaceService(), browseBean.getActionSpace().getNodePath()));
				lucenePath.append("//*");
				StringBuffer querySb = new StringBuffer("PATH:\""+lucenePath.toString()+"\"");
				querySb.append(" AND TYPE:\""+Constants.STANDARD_CONTENT_MODEL_PREFIX+":content\"");
				querySb.append(" AND ASPECT:\""+SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING+"\"");
				if (documentTypeNotSet) {
					querySb.append(" AND ISUNSET:\""+SinekartaModel.SINEKARTA_PREFIX+":"+SinekartaModel.PROP_DOCUMENT_TYPE+"\"");
				}
				if (testo!=null && !testo.trim().equals("")) {
					String[] words = testo.split(" ");
					querySb.append(" AND ( ");
					for (int i=0;i<words.length;i++) {
						String word = words[i];
						querySb.append(" @"+Constants.STANDARD_CONTENT_MODEL_PREFIX_EXTENDED+"name:\""+word+"\"");

						querySb.append(" @"+Constants.STANDARD_CONTENT_MODEL_PREFIX_EXTENDED+"description:\""+word+"\"");

						querySb.append(" @"+Constants.STANDARD_CONTENT_MODEL_PREFIX_EXTENDED+"title:\""+word+"\"");
						
						querySb.append(" @"+Constants.STANDARD_CONTENT_MODEL_PREFIX_EXTENDED+"author:\""+word+"\"");
						
						querySb.append(" @"+Constants.SINEKARTA_CONTENT_MODEL_PREFIX_EXTENDED+SinekartaModel.PROP_OCR_RESULT+":\""+word+"\"");
						
						querySb.append(" TEXT:\""+word+"\"");
						
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
				return querySb.toString();
			}
		};
		// set the Search Context onto the top-level navigator bean
		// this causes the browse screen to switch into search results view
		this.navigator.setSearchContext(search);
		
		return OUTCOME_BROWSE;
	}

	public boolean isError() {
		return error;
	}

	public void setError(boolean error) {
		this.error = error;
	}

	public String getTesto() {
		return testo;
	}

	public void setTesto(String testo) {
		this.testo = testo;
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

	public boolean isDocumentTypeNotSet() {
		return documentTypeNotSet;
	}

	public void setDocumentTypeNotSet(boolean documentTypeNotSet) {
		this.documentTypeNotSet = documentTypeNotSet;
	}

}
