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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.behavior.ArchiveOrganizator;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.DocumentUpdatePDFAPropertiesException;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;
import org.springframework.beans.factory.InitializingBean;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

/**
 * document update properties action
 * this action will update document PDF/A properties for modifying author, description and document name
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentUpdatePDFAProperties extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentUpdatePDFAProperties.class);
	public static final String ACTION_NAME_DOCUMENT_UPDATE_PDFA_PROPERTIES = "sinekartaDocumentUpdatePDFAProperties";

	private String companyHomePath;
	private NodeService nodeService;
	private SearchService searchService;
	private ContentService contentService;
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentUpdateProperties action, execution init");
		
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		DocumentUpdatePropertiesWorker execAsSinekartaAdmin = new DocumentUpdatePropertiesWorker(actionedUponNodeRef, nodeService,
				contentService, sinekartaDao);
		
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on document update prepare : " + execAsSinekartaAdmin.getKoReason());
			throw new DocumentUpdatePDFAPropertiesException("error on document update prepare : " + execAsSinekartaAdmin.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentUpdateProperties action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

}
class DocumentUpdatePropertiesWorker implements RunAsWork<Boolean> {
	// constants
	private static Logger tracer = Logger.getLogger(ArchiveOrganizator.class);

	private NodeRef nodeRef;
	private NodeService nodeService;
	private ContentService contentService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;
	private String koReason;
	
	public DocumentUpdatePropertiesWorker(NodeRef nodeRef, NodeService nodeService,
			ContentService contentService, SinekartaDao sinekartaDao) {
		super();
		this.nodeRef = nodeRef;
		this.nodeService = nodeService;
		this.contentService = contentService;
		this.sinekartaDao = sinekartaDao;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentUpdatePropertiesWorker doWork : execution init");

		try {
			updateAttributes(nodeRef);
		} catch (Throwable e) {
			tracer.error("DocumentUpdatePropertiesWorker action.doWork, generic error", e);
			koReason="DocumentUpdatePropertiesWorker action.doWork, generic error : " + e.getMessage();
			return false;
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentUpdatePropertiesWorker doWork : finished OK");

		return true;
	}

	public String getKoReason() {
		return koReason;
	}

	/**
	 * 
	 * metodo di utilita' per aggiornare subject e keywords
	 * 
	 * @param actionedUponNodeRef
	 * @param contentReader
	 * @param contentWriter
	 */
	private void updateAttributes(NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentUpdatePropertiesWorker doWork : updating pdf attributes with document base metadata");

		// aggiornamento documento con proprieta' impostate da utente
		ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);

		String fileName = (String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME);
		String description = (String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_DESCRIPTION);
		String title = (String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_TITLE);
		// download the content from the source reader
		InputStream is = contentReader.getContentInputStream();
		
		OutputStream os = null;

		try {
			PdfReader reader = new PdfReader(is);
			if (PDFTools.isPdfSigned(reader)) {
				reader.close();
				return;
			}
			// preparing the new document content, the pdf/a content 
			ContentWriter contentWriter = contentService.getWriter(nodeRef, ContentModel.PROP_CONTENT, true);
			contentWriter.setMimetype(Constants.APPLICATION_PDF);
			contentWriter.setEncoding("UTF-8");
			contentWriter.setLocale(contentReader.getLocale());
			
			os = contentWriter.getContentOutputStream();

			PdfStamper stamper = new PdfStamper(reader, os);
			HashMap<String, String> infoDict = reader.getInfo();
			if (infoDict.containsKey("Title")) {
				infoDict.put("Title", "[SineKarta : "+fileName+"] "+infoDict.get("Title"));
			} else {
				infoDict.put("Title", "[SineKarta : "+fileName+"] ");
			}
			if (infoDict.containsKey("Subject")) {
				infoDict.put("Subject", "[SineKarta : "+title+"] "+infoDict.get("Subject"));
			} else {
				infoDict.put("Subject", "[SineKarta : "+title+"] ");
			}
			if (infoDict.containsKey("Keywords")) {
				infoDict.put("Keywords", infoDict.get("Keywords") + " [SineKarta : "+description+"]");
			} else {
				infoDict.put("Keywords", "[SineKarta : "+description+"]");
			}
			stamper.setMoreInfo(infoDict);
			stamper.close();
			reader.close();
		} catch (Exception e) {
			tracer.error("Unable to update PDF/A with attributes",e);
			throw new DocumentUpdatePDFAPropertiesException("Unable to update PDF/A with attributes",e);
		} finally {
			try {
				if (os!=null) os.flush();
			} catch (IOException e) {
			}
			try {
				if (os!=null) os.close();
			} catch (IOException e) {
			}
			try {
				is.close();
			} catch (IOException e) {
			}
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentUpdatePropertiesWorker doWork : document attributes updated");

	}
	
}
