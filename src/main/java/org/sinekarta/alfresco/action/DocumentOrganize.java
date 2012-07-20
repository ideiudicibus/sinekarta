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

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.model.FileInfo;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.behavior.ArchiveOrganizator;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.ArchiveOrganizatorException;
import org.sinekarta.alfresco.exception.DocumentAcquiringException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;
import org.springframework.beans.factory.InitializingBean;

/**
 * document organize archive action.
 * this action will organize archive using document type configuration
 * 
 * - non input parameter required
 * 
 * - non output parameter provided
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentOrganize extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentOrganize.class);
	public static final String ACTION_NAME_DOCUMENT_ORGANIZE = "sinekartaDocumentOrganize";

	private String companyHomePath;
	private NodeService nodeService;
	private FileFolderService fileFolderService;
	private SearchService searchService;
	private ScriptService scriptService;
	private PersonService personService;
	private ActionService actionService;
	private ContentService contentService;
	private AuthenticationService authenticationService;
	private OwnableService ownableService;
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganize action, execution init");
		
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		DocumentOrganizeWorker execAsSinekartaAdmin = new DocumentOrganizeWorker(actionedUponNodeRef, nodeService,
				fileFolderService,
				searchService, scriptService,
				personService,
				authenticationService,
				ownableService, 
				actionService, 
				contentService,
				companyHomePath, sinekartaDao);
		
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error organize archive : " + execAsSinekartaAdmin.getKoReason());
			throw new ArchiveOrganizatorException("error organize archive : " + execAsSinekartaAdmin.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganize action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
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

	public void setScriptService(ScriptService scriptService) {
		this.scriptService = scriptService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	public void setOwnableService(OwnableService ownableService) {
		this.ownableService = ownableService;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

}
class DocumentOrganizeWorker implements RunAsWork<Boolean> {
	// constants
	private static Logger tracer = Logger.getLogger(ArchiveOrganizator.class);

	private NodeRef nodeRef;
	private NodeService nodeService;
	private FileFolderService fileFolderService;
	private SearchService searchService;
	private ScriptService scriptService;
	private PersonService personService;
	private ActionService actionService;
	private ContentService contentService;
	private AuthenticationService authenticationService;
	private OwnableService ownableService;
	private String companyHomePath;
	private SinekartaDao sinekartaDao;
	private String koReason;
	
	public DocumentOrganizeWorker(NodeRef nodeRef, NodeService nodeService,
			FileFolderService fileFolderService,
			SearchService searchService, ScriptService scriptService,
			PersonService personService,
			AuthenticationService authenticationService,
			OwnableService ownableService, 
			ActionService actionService,
			ContentService contentService, 
			String companyHomePath, SinekartaDao sinekartaDao) {
		super();
		this.nodeRef = nodeRef;
		this.nodeService = nodeService;
		this.fileFolderService = fileFolderService;
		this.searchService = searchService;
		this.scriptService = scriptService;
		this.personService = personService;
		this.authenticationService = authenticationService;
		this.ownableService = ownableService;
		this.actionService = actionService;
		this.contentService = contentService;
		this.companyHomePath = companyHomePath;
		this.sinekartaDao = sinekartaDao;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : execution init");

		try {
			
			// preparing for query 
			StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
	
			// getting noderef of archivio
			String archivioPath = Configuration.getInstance().getLuceneArchivePath();
	
			// calculating Document Type
			int documentTypeId = Integer.parseInt((String)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_DOCUMENT_TYPE));
			DocumentType documentType = sinekartaDao.getDocumentType(documentTypeId);
			
			Map<QName, Serializable> properties = nodeService.getProperties(nodeRef);
			properties.put(SinekartaModel.PROP_QNAME_PU_SIGN_REQUIRED, documentType.isUniqueOriginal());
			nodeService.setProperties(nodeRef, properties);
			
			ContentReader contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			String documentMimetype = (String) contentReader.getMimetype();
			
			// if the document is already signed, we do not need to convert it.
			// how to undestand if the document is signed?
			// 1. document type attribute (could be, not so bad)
			// 4. the document has a special aspect (could be, added to 1)
			
			boolean signedDocument = false;
			if (documentType.isPdfaAlreadySigned()) signedDocument=true;
			if (nodeService.hasAspect(nodeRef, SinekartaModel.ASPECT_QNAME_SIGNED_DOCUMENT)) signedDocument=true;
			if (documentMimetype.equals(Constants.APPLICATION_PDF)) {
				signedDocument=PDFTools.isPdfSigned(contentReader.getContentInputStream());
				contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			}
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : converting documento to PDF/A");

			if (documentType.isPdfaConvertNeeded() && !signedDocument) {
				// first of all we will convert documento to PDF/A (if needed)
				Action documentToPDFA = actionService.createAction(Configuration.getInstance().getDocumentToPDFAActionName());
				try {
					actionService.executeAction(documentToPDFA, nodeRef, false, false);
				} catch(Throwable t) {
					tracer.error("Unable to execute PDF/A conversion",t);
					throw new DocumentAcquiringException("Unable to execute PDF/A conversion",t);
				}
			}

			contentReader = contentService.getReader(nodeRef, ContentModel.PROP_CONTENT);
			documentMimetype = (String) contentReader.getMimetype();
			
			if (documentMimetype.equals(Constants.APPLICATION_PDF)){
				// do only if the document is a pdf and is it not signed
				if (!signedDocument) {
					if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : we have to update PDF/A properties, launching action (asynchronous)");
					
					Action documentUpdateProperties = actionService.createAction(DocumentUpdatePDFAProperties.ACTION_NAME_DOCUMENT_UPDATE_PDFA_PROPERTIES);
					try {
						actionService.executeAction(documentUpdateProperties, nodeRef, false, false);
					} catch(Throwable t) {
						tracer.error("Unable to execute update properties...",t);
						throw new ArchiveOrganizatorException("Unable to execute update properties...",t);
					}
					
					// if OCR is required, call (asynchronously) the action
					if (documentType.isOcrRequired()) {
			
						if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : we have to execute OCR for this kind of document, launching action (asynchronous)");
			
						Action documentOCR = actionService.createAction(DocumentOCR.ACTION_NAME_DOCUMENT_OCR);
						documentOCR.setParameterValue(DocumentOCR.PARAM_LANGUAGE, (String) nodeService.getProperty(nodeRef,SinekartaModel.PROP_QNAME_LANGUAGE));
						try {
							actionService.executeAction(documentOCR, nodeRef, false, true);
						} catch(Throwable t) {
							tracer.error("Unable to execute OCR",t);
							// why abort if ocr fails?
						}
					}
				} else {
					// nothing to do, waiting for RCS signature...
				}
			} else {
				if (signedDocument) {
					// calculating RCS_SIGNATURE properties values
					properties = new HashMap<QName,Serializable>();
					properties.put(SinekartaModel.PROP_QNAME_TIMESTAMP_RCS_SIGNATURE, new Date(System.currentTimeMillis()));
					properties.put(SinekartaModel.PROP_QNAME_DOCUMENT_RCS_SIGNED_FINGERPRINT, org.sinekarta.alfresco.util.Util.byteToHex(org.sinekarta.alfresco.util.Util.digest256(contentReader.getContentInputStream())));
			
					// adding RCS_SIGNATURE aspect to document
					nodeService.addAspect(nodeRef, SinekartaModel.ASPECT_QNAME_RCS_SIGNATURE, properties);		
				} else {
					tracer.error("You are trying to apply sinekarta to a document not signed and not converted to PDF/A, unsupported feature in this edition.");
					throw new ArchiveOrganizatorException("You are trying to apply sinekarta to a document not signed and not converted to PDF/A, unsupported feature in this edition.");
				}
			}
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : interpreting rule for document move");
	
			// interpreting rule for target position of document
			String docLocationRule = documentType.getDocLocationRule();
			if (docLocationRule.startsWith(Constants.SDF)) {
	
				if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : this is an SDF rule : " + docLocationRule);
	
				// simple rule, based on simpledateformat
				SimpleDateFormat sdf = new SimpleDateFormat(docLocationRule.substring(Constants.SDF.length()));
				// la regola va applicata alla data del documento
				Date documentDate = (Date)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_DOCUMENT_DATE);
				docLocationRule = sdf.format(documentDate);
				
			} else if (docLocationRule.startsWith(Constants.JS)) {
	
				if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : this is JS rule");
	
				// getting the script node
				NodeRef scriptNodeRef = NodeTools.getNodeRefByPath(searchService, nodeService, storeRef, companyHomePath + docLocationRule.substring(Constants.JS.length()));
				if (scriptNodeRef == null) {
					tracer.error("Script does not exists : " + docLocationRule);
					throw new ArchiveOrganizatorException("Script does not exists : " + docLocationRule);
				}
				// get the references we need to build the default scripting data-model (to run script)
	            String userName = authenticationService.getCurrentUserName();
	            NodeRef personRef = personService.getPerson(userName);
	            NodeRef homeSpaceRef = (NodeRef)nodeService.getProperty(personRef, ContentModel.PROP_HOMEFOLDER);
	    		ChildAssociationRef parent = nodeService.getPrimaryParent(nodeRef);
	    		NodeRef spaceRef = parent.getParentRef();
	    		NodeRef companyHome = NodeTools.getNodeRefByPath(searchService, nodeService, storeRef, companyHomePath);
				if (companyHome == null) {
					tracer.error("companyHome does not exists : " + companyHomePath);
					throw new ArchiveOrganizatorException("companyHome does not exists : " + companyHomePath);
				}
	            // the default scripting model provides access to well known objects and searching
	            // facilities - it also provides basic create/update/delete/copy/move services
	            Map<String, Object> model = scriptService.buildDefaultModel(
	                    personRef,
	                    companyHome,
	                    homeSpaceRef,
	                    scriptNodeRef,
	                    nodeRef,
	                    spaceRef);
	
	            if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : executing script : " + docLocationRule);
	
	            // running the script, The script must return a string, the lucene path to the target document folder
	            // path returned must be relative to archive folder (not start with a /
	            docLocationRule = (String) scriptService.executeScript(scriptNodeRef, null, model);
			}
	
			if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : folder to move document : " + companyHomePath + archivioPath + docLocationRule);
	
			// get (or create) target folder
			NodeRef targetFolder = NodeTools.deepCreateArchiveFolder(nodeService, searchService, fileFolderService, ownableService, storeRef, companyHomePath + archivioPath + docLocationRule);
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : moving document...");
			
			FileInfo newFile = null;
			
			try {
				// move the archived document to the target folder
				newFile = fileFolderService.move(nodeRef, targetFolder, null);
			} catch (Exception e) {
				tracer.error("Unable to move file, surce file or destination folder does not exist!",e);
				throw new ArchiveOrganizatorException("Unable to move file, surce file or destination folder does not exist!",e);
			}
	
			if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : document successefully moved");
	
			// the owner of the document must be the RCS!
			// getting owner of the archive space
			String rcsUserId = (String)nodeService.getProperty(targetFolder, SinekartaModel.PROP_QNAME_RCS_USER_ID);
			String sinekartaAdminUserId = (String)nodeService.getProperty(targetFolder, SinekartaModel.PROP_QNAME_SINEKARTA_ADMIN_USER_ID);
			nodeService.setProperty(newFile.getNodeRef(), ContentModel.PROP_CREATOR, rcsUserId);
			nodeService.setProperty(newFile.getNodeRef(), ContentModel.PROP_MODIFIER, rcsUserId);
			ownableService.setOwner(newFile.getNodeRef(), sinekartaAdminUserId);
	
		} catch (Throwable e) {
			tracer.error("DocumentOrganizeWorker action.doWork, unable to organize archive", e);
			koReason="DocumentOrganizeWorker action.doWork, unable to organize archive : " + e.getMessage();
			return false;
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentOrganizeWorker doWork : finished OK");

		return true;
	}

	public String getKoReason() {
		return koReason;
	}
}
