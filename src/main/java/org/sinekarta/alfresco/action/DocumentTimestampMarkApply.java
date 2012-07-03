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

import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;
import org.sinekarta.sign.area.DigitalSignatureArea;
import org.sinekarta.sign.area.DigitalSignatureDocument;
import org.sinekarta.sign.area.MarkDocument;
import org.sinekarta.sign.area.MarkDocumentArea;
import org.springframework.beans.factory.InitializingBean;

/**
 * document RCS digital signature for timestamp mark apply action.
 * this action will apply digital signature and timestamp mark to all documents
 * 
 * - parameter clientArea : returned from SinekartaDigitalSignatureClient in executeDigitalSignature
 * - parameter markArea : returned from markMocumentPrepare action
 * 
 * - no return provided
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentTimestampMarkApply extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentTimestampMarkApply.class);
	public static final String ACTION_NAME_DOCUMENT_RCS_MARK_SIGN_APPLY = "sinekartaDocumentTimestampMarkApply";
	public static final String PARAM_CLIENT_AREA = "clientArea";
	public static final String PARAM_MARK_AREA = "markArea";

	private String companyHomePath;
	private ContentService contentService;
	private NodeService nodeService;
	private ActionService actionService;
	private SearchService searchService;
	private FileFolderService fileFolderService;
	private OwnableService ownableService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampMarkApply action, execution init");
		
		String clientArea = (String)action.getParameterValue(PARAM_CLIENT_AREA);
		if (clientArea==null) {
			tracer.error("no clientArea specified for mark sign apply.");
			throw new MarkFailedException("no clientArea specified for mark sign apply.");
		}
		
		String markArea = (String)action.getParameterValue(PARAM_MARK_AREA);
		if (markArea==null) {
			tracer.error("no markArea specified for mark sign apply.");
			throw new MarkFailedException("no markArea specified for mark sign apply.");
		}
		
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		DocumentTimestampMarkApplyWorker execAsSinekartaAdmin = new DocumentTimestampMarkApplyWorker(nodeService, contentService, 
				actionService, fileFolderService, ownableService, clientArea, markArea);
		
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on timestamp mark apply : " + execAsSinekartaAdmin.getKoReason());
			throw new MarkFailedException("error on timestamp mark apply : " + execAsSinekartaAdmin.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampMarkApply action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_CLIENT_AREA,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_CLIENT_AREA))); 
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

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public void setOwnableService(OwnableService ownableService) {
		this.ownableService = ownableService;
	}

}
class DocumentTimestampMarkApplyWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(DocumentTimestampMarkApply.class);

	private NodeService nodeService;
	private ContentService contentService;
	private ActionService actionService;
	private String clientArea;
	private String markArea;
	private FileFolderService fileFolderService;
	private OwnableService ownableService;
	private String koReason;

	public DocumentTimestampMarkApplyWorker(NodeService nodeService, ContentService contentService,
			ActionService actionService, FileFolderService fileFolderService, OwnableService ownableService, String clientArea, String markArea) {
		super();
		this.nodeService = nodeService;
		this.contentService = contentService;
		this.clientArea = clientArea;
		this.markArea = markArea;
		this.actionService = actionService;
		this.fileFolderService = fileFolderService;
		this.ownableService = ownableService;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampMarkApply action.doWork, execution init");

		try {
			
			MarkDocumentArea mda = MarkDocumentArea.fromBase64String(markArea);
			
			DigitalSignatureArea sca = DigitalSignatureArea.fromBase64String(clientArea);
	
			// l'area di firma deve contenere il solo documento di marca temporale
			if (sca.getDocuments().size() > 1) {
				tracer.error("wrong area received for timestamp mark.");
				throw new MarkFailedException("wrong area received for timestamp mark.");
			}
			
			// verifica coerenza aree ricevute
			DigitalSignatureDocument dsd = sca.getDocuments().get(0);
			if (!mda.getTmpMarkDocumentNodeRefId().equals(dsd.getNodeRefId()) ||
				!mda.getTmpMarkFolderPathNodeRefId().equals(sca.getDocumentPathNodeRefId()) ||
				!mda.getTmpMarkFolderPathStoreRefId().equals(sca.getDocumentPathStoreRefId()) ||
				!mda.getTmpMarkFolderPathStoreRefProtocol().equals(sca.getDocumentPathStoreRefProtocol())) {
				tracer.error("Wrond temp mark document.");
				throw new MarkFailedException("Wrond temp mark document.");
			}
	
			// storeref file temporaneo
			StoreRef storeRefTmp = new StoreRef(mda.getTmpMarkFolderPathStoreRefProtocol(), mda.getTmpMarkFolderPathStoreRefId());
			// storeref file di marca buono
			StoreRef storeRef = new StoreRef(mda.getMarkFolderPathStoreRefProtocol(), mda.getMarkFolderPathStoreRefId());
	
			// per ciascun file presente in mda, verifica che nessuno sia ancora stato inserito in un file di marca temporale
			for (MarkDocument markDocument : mda.getDocuments()) {
				NodeRef nodeRef = new NodeRef(storeRef, markDocument.getNodeRefId());
				if (nodeService.hasAspect(nodeRef, SinekartaModel.ASPECT_QNAME_TIMESTAMP_MARK)) {
					String fileName = NodeTools.translatePath(nodeService, nodeService.getPath(nodeRef)); 
					tracer.error("Document " + fileName + " already marked.");
					throw new MarkFailedException("Document " + fileName + " already marked.");
				}
			}
			
			// noderef della dir temporanea necessario per completare il processo di firma
			NodeRef nodeRefFolderTmp = new NodeRef(storeRefTmp, mda.getTmpMarkFolderPathNodeRefId());
			
			// chiamata a action di firma e marca (sta applicando la firma e la marca al temporaneo....)
			Action digitalSignatureApply = actionService.createAction(DocumentDigitalSignatureTimestampMarkApply.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_TIMESTAMP_MARK_APPLY);
			digitalSignatureApply.setParameterValue(DocumentDigitalSignatureTimestampMarkApply.PARAM_CLIENT_AREA, clientArea);
			try {
				actionService.executeAction(digitalSignatureApply, nodeRefFolderTmp, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to apply timestamp mark",t);
				throw new MarkFailedException("Unable to apply timestamp mark",t);
			}
			
			// calcolo il noderef del documento marca temporale temporaneo
			NodeRef tmpMarkNodeRef = new NodeRef(storeRefTmp, dsd.getNodeRefId());
			
			// calcolo il noderef della directory delle marce temporali
			NodeRef markFolder = new NodeRef(storeRef,mda.getMarkFolderPathNodeRefId());
	
			// copio la marca temporale temporanea nella directory marca temporale
			NodeRef mark = null;
			// create the timestamp mark document on archive, to write into it the m7m doc
			try {
				mark = fileFolderService.create(markFolder,dsd.getFileName(),ContentModel.TYPE_CONTENT).getNodeRef();
			} catch (Exception e) {
				tracer.error("File already exists on timestamp mark folder, how is it possible? : " + e.getClass().getName() + " : " + e.getMessage(),e);
				throw new MarkFailedException("File already exists on timestamp mark folder, how is it possible? : " + e.getClass().getName() + " : " + e.getMessage(),e);
			}

			{
				// Il timestamp non viene tornato (e cmq non sarebbe leggibile), lo estraggo dal PDF
				ContentReader contentReader = contentService.getReader(tmpMarkNodeRef, ContentModel.PROP_CONTENT);
				InputStream is = contentReader.getContentInputStream();
				dsd.setEncodedTimeStampToken(PDFTools.getPrintableTimestampToken(is));
			}
			
			// copia del documento temporaneo (firmato e marcato) nel doc buono
			ContentReader contentReader = contentService.getReader(tmpMarkNodeRef, ContentModel.PROP_CONTENT);
			InputStream is = contentReader.getContentInputStream();
			ContentWriter contentWriter = contentService.getWriter(mark, ContentModel.PROP_CONTENT, true);
			OutputStream os = contentWriter.getContentOutputStream();
			contentWriter.setMimetype(Constants.APPLICATION_PDF);
			contentWriter.setEncoding("UTF-8");
			contentWriter.setLocale(Locale.getDefault());
			
			byte[] buf = new byte[8192];
			int len = is.read(buf);
			while (len!=-1) {
				os.flush();
				os.write(buf,0,len);
				len = is.read(buf);
			}
			os.flush();
			os.close();
			is.close();
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampMarkApply action.doWork, firma digitale e marca temporale applicate, calcolo metadati documento");
	
			String rcsUserId = (String)nodeService.getProperty(markFolder, SinekartaModel.PROP_QNAME_RCS_USER_ID);
			String sinekartaAdminUserId = (String)nodeService.getProperty(markFolder, SinekartaModel.PROP_QNAME_SINEKARTA_ADMIN_USER_ID);
			nodeService.setProperty(mark, ContentModel.PROP_CREATOR, rcsUserId);
			nodeService.setProperty(mark, ContentModel.PROP_MODIFIER, rcsUserId);
			ownableService.setOwner(mark, sinekartaAdminUserId);
	
			// calculating TIMESTAMP_MARK properties values
			Map<QName,Serializable> properties = new HashMap<QName,Serializable>();
			properties.put(SinekartaModel.PROP_QNAME_MARK_DOCUMENT_DESCRIPTION, mda.getMarkDescription());
			properties.put(SinekartaModel.PROP_QNAME_MARK_DOCUMENT_REFERENCE_ID,  (String) nodeService.getProperty(mark, ContentModel.PROP_NODE_UUID));
			properties.put(SinekartaModel.PROP_QNAME_MARK_TIMESTAMP_RCS_SIGNATURE,  new Date(System.currentTimeMillis()));
			properties.put(SinekartaModel.PROP_QNAME_MARK_FINGER_PRINT,  dsd.getFingerPrint());
			properties.put(SinekartaModel.PROP_QNAME_MARK_TIMESTAMP_TOKEN, dsd.getEncodedTimeStampToken());
			properties.put(SinekartaModel.PROP_QNAME_MARK_DOCUMENT_TYPE, mda.getMarkDocumentType());
	
			// adding TIMESTAMP_MARK aspect to document
			nodeService.addAspect(mark, SinekartaModel.ASPECT_QNAME_TIMESTAMP_MARK, properties);		
	
			// empty properties for SUBSTITUTIVE_PRESERVATION aspect (to be added to referenced document)
			Map<QName,Serializable> emptyProperties = new HashMap<QName,Serializable>();
			for (MarkDocument m:mda.getDocuments()) {
				NodeRef originalDocument = new NodeRef(storeRef,m.getNodeRefId());
				
				nodeService.createAssociation(mark, originalDocument, SinekartaModel.ASSOCIATION_QNAME_MARKED_DOCUMENT_LIST);
	
				// adding SUBSTITUTIVE_PRESERVATION aspect to referenced document
				nodeService.addAspect(originalDocument, SinekartaModel.ASPECT_QNAME_SUBSTITUTIVE_PRESERVATION, emptyProperties);		
				nodeService.createAssociation(originalDocument, mark, SinekartaModel.ASSOCIATION_QNAME_MARKS_DOCUMENT);
			}
			
			fileFolderService.delete(tmpMarkNodeRef);
		} catch (Throwable e) {
			tracer.error("DocumentTimestampMarkApply action.doWork, generic error", e);
			koReason="DocumentTimestampMarkApply action.doWork, generic error : " + e.getMessage();
			return false;
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampMarkApply action.doWork, finished OK");

		return true;
	}
	
	public String getKoReason() {
		return koReason;
	}
	
}
