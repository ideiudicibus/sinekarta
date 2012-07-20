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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

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
import org.alfresco.service.cmr.repository.AssociationRef;
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
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.model.agenziaEntrate.Comunicazione;
import org.sinekarta.alfresco.model.agenziaEntrate.DataFineVal;
import org.sinekarta.alfresco.model.agenziaEntrate.DataInizioVal;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiArchivioInformatico;
import org.sinekarta.alfresco.model.agenziaEntrate.Documento;
import org.sinekarta.alfresco.model.agenziaEntrate.IndiceDocumentiInArchivio;
import org.sinekarta.alfresco.model.agenziaEntrate.ObjectFactory;
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
public class DocumentTimestampAEMarkApply extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentTimestampAEMarkApply.class);
	public static final String ACTION_NAME_DOCUMENT_RCS_AEMARK_SIGN_APPLY = "sinekartaDocumentTimestampAEMarkApply";
	public static final String PARAM_CLIENT_AREA = "clientArea";
	public static final String PARAM_MARK_AREA = "markArea";
	public static final String PARAM_XML_AREA = "xmlArea";

	private String companyHomePath;
	private ContentService contentService;
	private NodeService nodeService;
	private ActionService actionService;
	private SearchService searchService;
	private FileFolderService fileFolderService;
	private OwnableService ownableService;
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkApply action, execution init");
		
		String clientArea = (String)action.getParameterValue(PARAM_CLIENT_AREA);
		if (clientArea==null) {
			tracer.error("no clientArea specified for aemark sign apply.");
			throw new MarkFailedException("no clientArea specified for aemark sign apply.");
		}
		
		String markArea = (String)action.getParameterValue(PARAM_MARK_AREA);
		if (markArea==null) {
			tracer.error("no markArea specified for aemark sign apply.");
			throw new MarkFailedException("no markArea specified for aemark sign apply.");
		}
		
		String xmlArea = (String)action.getParameterValue(PARAM_XML_AREA);
		if (xmlArea==null) {
			tracer.error("no xmlArea specified for aemark sign apply.");
			throw new MarkFailedException("no xmlArea specified for aemark sign apply.");
		}
		
		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		DocumentTimestampAEMarkApplyWorker execAsSinekartaAdmin = new DocumentTimestampAEMarkApplyWorker(nodeService, contentService, 
				actionService, fileFolderService, ownableService, sinekartaDao, clientArea, markArea, xmlArea);
		
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on timestamp aemark apply : " + execAsSinekartaAdmin.getKoReason());
			throw new MarkFailedException("error on timestamp aemark apply : " + execAsSinekartaAdmin.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkApply action, execution end");
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
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_XML_AREA,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_XML_AREA))); 
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
class DocumentTimestampAEMarkApplyWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(DocumentTimestampAEMarkApply.class);

	private NodeService nodeService;
	private ContentService contentService;
	private ActionService actionService;
	private String clientArea;
	private String markArea;
	private String xmlArea;
	private FileFolderService fileFolderService;
	private OwnableService ownableService;
	private SinekartaDao sinekartaDao;
	private String koReason;

	public DocumentTimestampAEMarkApplyWorker(NodeService nodeService, ContentService contentService,
			ActionService actionService, FileFolderService fileFolderService, OwnableService ownableService, 
			SinekartaDao sinekartaDao, String clientArea, String markArea, String xmlArea) {
		super();
		this.nodeService = nodeService;
		this.contentService = contentService;
		this.clientArea = clientArea;
		this.markArea = markArea;
		this.xmlArea = xmlArea;
		this.actionService = actionService;
		this.fileFolderService = fileFolderService;
		this.ownableService = ownableService;
		this.sinekartaDao = sinekartaDao;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkApply action.doWork, execution init");

		try {
			
			MarkDocumentArea mda = MarkDocumentArea.fromBase64String(markArea);
			
			DigitalSignatureArea sca = DigitalSignatureArea.fromBase64String(clientArea);
			
			JAXBContext jc = JAXBContext.newInstance(Comunicazione.class);
			Unmarshaller u = jc.createUnmarshaller();
			ByteArrayInputStream bais = new ByteArrayInputStream(xmlArea.getBytes());
		    Comunicazione com = (Comunicazione)u.unmarshal(bais);
		    
		    // TODO aggiungere un po' di controlli al bean comunicazione
		    if (com.getDatiFornitura().getProtocolloTelematicoDaSostituire()!=null && com.getDatiFornitura().getProtocolloTelematicoDaSostituire().trim().equals("")) {
		    	com.getDatiFornitura().setProtocolloTelematicoDaSostituire(null);
		    }
	
			// l'area di firma deve contenere il solo documento di marca temporale
			if (sca.getDocuments().size() > 1) {
				tracer.error("wrong area received for timestamp aemark.");
				throw new MarkFailedException("wrong area received for timestamp aemark.");
			}
			
			// verifica coerenza aree ricevute
			DigitalSignatureDocument dsd = sca.getDocuments().get(0);
			if (!mda.getTmpMarkDocumentNodeRefId().equals(dsd.getNodeRefId()) ||
				!mda.getTmpMarkFolderPathNodeRefId().equals(sca.getDocumentPathNodeRefId()) ||
				!mda.getTmpMarkFolderPathStoreRefId().equals(sca.getDocumentPathStoreRefId()) ||
				!mda.getTmpMarkFolderPathStoreRefProtocol().equals(sca.getDocumentPathStoreRefProtocol())) {
				tracer.error("Wrond temp aemark document.");
				throw new MarkFailedException("Wrond temp aemark document.");
			}
	
			// storeref file temporaneo
			StoreRef storeRefTmp = new StoreRef(mda.getTmpMarkFolderPathStoreRefProtocol(), mda.getTmpMarkFolderPathStoreRefId());
			// storeref file di marca buono
			StoreRef storeRef = new StoreRef(mda.getMarkFolderPathStoreRefProtocol(), mda.getMarkFolderPathStoreRefId());
	
			// aggiungo al file per l'AE i dati calcolabili in automatico
			ObjectFactory of = new ObjectFactory();
			DatiArchivioInformatico dai = com.getDatiArchivioInformatico();
			dai.setImpronta(dsd.getFingerPrint());
			
	        IndiceDocumentiInArchivio idia = new IndiceDocumentiInArchivio();
	        
	        Map<DocumentType, Documento> indice = new HashMap<DocumentType, Documento>();
	        
			for (MarkDocument md:mda.getDocuments()) {
				NodeRef originalDocument = new NodeRef(storeRef,md.getNodeRefId());
				int documentTypeId = Integer.parseInt((String)nodeService.getProperty(originalDocument, SinekartaModel.PROP_QNAME_MARK_DOCUMENT_TYPE));
				DocumentType documentType = sinekartaDao.getDocumentType(documentTypeId);
				Documento doc = indice.get(documentType);
				if (doc==null) {
					if (documentType.getExternalCode()==null || documentType.getExternalCode().trim().equals("")) {
						tracer.error("No external code found on documentType for type : " + documentType.getDescription() + ", can not create xml for AE.");
						throw new MarkFailedException("No external code found on documentType for type : " + documentType.getDescription() + ", can not create xml for AE.");
					}
					doc = of.createDocumento();
			        DataInizioVal dini = new DataInizioVal();
			        dini.setTime(Long.MAX_VALUE);
			        doc.setDataInizioVal(dini);
			        DataFineVal dfin = new DataFineVal();
			        dfin.setTime(Long.MIN_VALUE);
			        doc.setDataFineVal(dfin);
					indice.put(documentType, doc);
				}
		        doc.setTipoDocumento(documentType.getExternalCode());
		        List<AssociationRef> arefs = nodeService.getTargetAssocs(originalDocument, SinekartaModel.ASSOCIATION_QNAME_MARKED_DOCUMENT_LIST);
		        for (AssociationRef aref : arefs) {
		        	NodeRef ref = aref.getTargetRef();
		        	Date data = (Date)nodeService.getProperty(ref, SinekartaModel.PROP_QNAME_DOCUMENT_DATE);
		        	if (data.getTime()<doc.getDataInizioVal().getTime()) {
		        		doc.getDataInizioVal().setTime(data.getTime());
		        	}
		        	if (data.getTime()>doc.getDataFineVal().getTime()) {
		        		doc.getDataFineVal().setTime(data.getTime());
		        	}
			        doc.setNumero(doc.getNumero()+1); 
		        }
			}	        
	        idia.setNumElemIndice(indice.size());
	        List<Documento> documenti = idia.getDocumento();
	        for (Documento doc : indice.values()) {
		        documenti.add(doc);
	        }
	        dai.setIndiceDocumentiInArchivio(idia);
			com.setDatiArchivioInformatico(dai);
			
			// noderef della dir temporanea necessario per completare il processo di firma
			NodeRef nodeRefFolderTmp = new NodeRef(storeRefTmp, mda.getTmpMarkFolderPathNodeRefId());
			
			// chiamata a action di firma e marca (sta applicando la firma e la marca al temporaneo....)
			Action digitalSignatureApply = actionService.createAction(DocumentDigitalSignatureTimestampMarkApply.ACTION_NAME_DOCUMENT_DIGITAL_SIGNATURE_TIMESTAMP_MARK_APPLY);
			digitalSignatureApply.setParameterValue(DocumentDigitalSignatureTimestampMarkApply.PARAM_CLIENT_AREA, clientArea);
			try {
				actionService.executeAction(digitalSignatureApply, nodeRefFolderTmp, false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare data for document sign",t);
				throw new MarkFailedException("Unable to prepare data for document sign",t);
			}
			
			// calcolo il noderef del documento marca temporale temporaneo
			NodeRef tmpMarkNodeRef = new NodeRef(storeRefTmp, dsd.getNodeRefId());
			
			// calcolo il noderef della directory delle marce temporali
			NodeRef markFolder = new NodeRef(storeRef,mda.getMarkFolderPathNodeRefId());
	
			// copio la marca temporale temporanea nella directory marca temporale
			NodeRef mark = null;
			// create the timestamp mark document on archive, to write into it the doc
			try {
				mark = fileFolderService.create(markFolder,dsd.getFileName(),ContentModel.TYPE_CONTENT).getNodeRef();
			} catch (Exception e) {
				tracer.error("File already exists on timestamp aemark folder, how is it possible? : " + e.getClass().getName() + " : " + e.getMessage(),e);
				throw new MarkFailedException("File already exists on timestamp aemark folder, how is it possible? : " + e.getClass().getName() + " : " + e.getMessage(),e);
			}
			
			{
				// Il timestamp non viene tornato (e cmq non sarebbe leggibile), lo estraggo dal PDF
				ContentReader contentReader = contentService.getReader(tmpMarkNodeRef, ContentModel.PROP_CONTENT);
				InputStream is = contentReader.getContentInputStream();
				dsd.setEncodedTimeStampToken(PDFTools.getPrintableTimestampToken(is));
				dai.setMarcaTemporale(dsd.getEncodedTimeStampToken());
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
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkApply action.doWork, firma digitale e marca temporale applicate, calcolo metadati documento");
	
			String rcsUserId = (String)nodeService.getProperty(markFolder, SinekartaModel.PROP_QNAME_RCS_USER_ID);
			String sinekartaAdminUserId = (String)nodeService.getProperty(markFolder, SinekartaModel.PROP_QNAME_SINEKARTA_ADMIN_USER_ID);
			nodeService.setProperty(mark, ContentModel.PROP_CREATOR, rcsUserId);
			nodeService.setProperty(mark, ContentModel.PROP_MODIFIER, rcsUserId);
			ownableService.setOwner(mark, sinekartaAdminUserId);
	
			// calculating TIMESTAMP_AEMARK properties values
			Map<QName,Serializable> properties = new HashMap<QName,Serializable>();
			properties.put(SinekartaModel.PROP_QNAME_MARK_DOCUMENT_DESCRIPTION, mda.getMarkDescription());
			properties.put(SinekartaModel.PROP_QNAME_MARK_DOCUMENT_REFERENCE_ID,  (String) nodeService.getProperty(mark, ContentModel.PROP_NODE_UUID));
			properties.put(SinekartaModel.PROP_QNAME_MARK_TIMESTAMP_RCS_SIGNATURE,  new Date(System.currentTimeMillis()));
			properties.put(SinekartaModel.PROP_QNAME_MARK_FINGER_PRINT,  dsd.getFingerPrint());
			properties.put(SinekartaModel.PROP_QNAME_MARK_TIMESTAMP_TOKEN, dsd.getEncodedTimeStampToken());
	
			// adding TIMESTAMP_AEMARK aspect to document
			nodeService.addAspect(mark, SinekartaModel.ASPECT_QNAME_TIMESTAMP_AEMARK, properties);		
	
			// empty properties for ASPECT_QNAME_AEMARK_CREATED aspect (to be added to referenced document)
			Map<QName,Serializable> emptyProperties = new HashMap<QName,Serializable>();
			for (MarkDocument m:mda.getDocuments()) {
				NodeRef originalDocument = new NodeRef(storeRef,m.getNodeRefId());
				
				nodeService.createAssociation(mark, originalDocument, SinekartaModel.ASSOCIATION_QNAME_MARKED_DOCUMENT_LIST);
	
				// adding ASPECT_QNAME_AEMARK_CREATED aspect to referenced document
				nodeService.addAspect(originalDocument, SinekartaModel.ASPECT_QNAME_AEMARK_CREATED, emptyProperties);		
				nodeService.createAssociation(originalDocument, mark, SinekartaModel.ASSOCIATION_QNAME_AEPDFFILE_DOCUMENT);
			}
			
			fileFolderService.delete(tmpMarkNodeRef);
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkApply action.doWork, inizio scrittura documento XML per agenzia entrate");

			// creo il documento XML per AE
			NodeRef markXML = null;
			// create the AE xml document on archive, to write into it the XML
			try {
				markXML = fileFolderService.create(markFolder,mda.getMarkFileName() + Constants.EXTENSION_XML,ContentModel.TYPE_CONTENT).getNodeRef();
			} catch (Exception e) {
				tracer.error("File already exists on timestamp aemark folder, how is it possible? : " + e.getClass().getName() + " : " + e.getMessage(),e);
				throw new MarkFailedException("File already exists on timestamp aemark folder, how is it possible? : " + e.getClass().getName() + " : " + e.getMessage(),e);
			}
						
			ContentWriter contentWriterXML = contentService.getWriter(markXML, ContentModel.PROP_CONTENT, true);
			OutputStream osXML = contentWriterXML.getContentOutputStream();
			contentWriterXML.setMimetype(Constants.APPLICATION_XML);
			contentWriterXML.setEncoding("UTF-8");
			contentWriterXML.setLocale(Locale.getDefault());
			
	        Marshaller m = jc.createMarshaller();
	        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
			m.marshal(com, osXML);
			 
			osXML.flush();
			osXML.close();
			
			nodeService.setProperty(markXML, ContentModel.PROP_CREATOR, rcsUserId);
			nodeService.setProperty(markXML, ContentModel.PROP_MODIFIER, rcsUserId);
			ownableService.setOwner(markXML, sinekartaAdminUserId);
	
			// calculating TIMESTAMP_AEMARK properties values
			Map<QName,Serializable> propertiesXML = new HashMap<QName,Serializable>();
			propertiesXML.put(SinekartaModel.PROP_QNAME_MARK_DOCUMENT_DESCRIPTION, mda.getMarkDescription());
			propertiesXML.put(SinekartaModel.PROP_QNAME_MARK_DOCUMENT_REFERENCE_ID,  (String) nodeService.getProperty(markXML, ContentModel.PROP_NODE_UUID));
			propertiesXML.put(SinekartaModel.PROP_QNAME_MARK_TIMESTAMP_RCS_SIGNATURE,  new Date(System.currentTimeMillis()));
			propertiesXML.put(SinekartaModel.PROP_QNAME_MARK_FINGER_PRINT,  dsd.getFingerPrint());
			propertiesXML.put(SinekartaModel.PROP_QNAME_MARK_TIMESTAMP_TOKEN, dsd.getEncodedTimeStampToken());
	
			// adding TIMESTAMP_AEMARK aspect to document
			nodeService.addAspect(markXML, SinekartaModel.ASPECT_QNAME_TIMESTAMP_AEMARK, propertiesXML);		
	
			// empty properties for ASPECT_QNAME_AEMARK_CREATED aspect (to be added to referenced document)
			for (MarkDocument md:mda.getDocuments()) {
				NodeRef originalDocument = new NodeRef(storeRef,md.getNodeRefId());
				
				nodeService.createAssociation(markXML, originalDocument, SinekartaModel.ASSOCIATION_QNAME_MARKED_DOCUMENT_LIST);
	
				// adding association to referenced document
				nodeService.createAssociation(originalDocument, markXML, SinekartaModel.ASSOCIATION_QNAME_AEXMLFILE_DOCUMENT);
			}
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkApply action.doWork, documento XML per agenzia entrate preparato correttamente");

		} catch (Throwable e) {
			tracer.error("DocumentTimestampAEMarkApply action.doWork, generic error", e);
			koReason="DocumentTimestampAEMarkApply action.doWork, generic error : " + e.getMessage();
			return false;
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentTimestampAEMarkApply action.doWork, finished OK");

		return true;
	}

	public String getKoReason() {
		return koReason;
	}
	
}
