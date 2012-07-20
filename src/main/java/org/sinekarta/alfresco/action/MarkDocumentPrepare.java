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

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.MarkDocumentAlreadyExistsException;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;
import org.sinekarta.sign.area.MarkDocument;
import org.sinekarta.sign.area.MarkDocumentArea;
import org.springframework.beans.factory.InitializingBean;
import org.xml.sax.helpers.AttributesImpl;

import com.itextpdf.text.Document;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.CMYKColor;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.xml.xmp.XmpWriter;

/**
 * document containing finger print of other documents preparation action.
 * document produced is timestamp mark for document provided in MarkDocumentArea
 * 
 * - parameter markArea : the markDocumentArea returned from MarkFolderprepare
 * 
 * - return the serialize and encoded markDocumentArea with references to timestamp mark document
 * 
 * @author andrea.tessaro
 *
 */
public class MarkDocumentPrepare extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(MarkDocumentPrepare.class);
	public static final String ACTION_NAME_MARK_DOCUMENT_PREPARE = "sinekartaMarkDocumentPrepare";
	public static final String PARAM_MARK_AREA = "markArea";
	public static final String PARAM_MARK_DESCRIPTIONR = "markDescription";
	public static final String PARAM_MARK_FILENAME = "markFileName";

	private String companyHomePath;
	private ContentService contentService;
	private PersonService personService;
	private NodeService nodeService;
	private SearchService searchService;
	private FileFolderService fileFolderService; 
	private NamespaceService namespaceService;
	private AuthenticationService authenticationService;
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentPrepare action, execution init");
		
		String markArea = (String)action.getParameterValue(PARAM_MARK_AREA);
		if (markArea==null) {
			tracer.error("no markFileName specified for RCS mark prepare.");
			throw new MarkFailedException("no markFileName specified for RCS mark prepare.");
		}
		
		MarkDocumentArea markDocumentArea = MarkDocumentArea.fromBase64String(markArea);
		
		String markDescription = (String)action.getParameterValue(PARAM_MARK_DESCRIPTIONR);
		if (markDescription==null) {
			tracer.error("no markDescription specified for mark prepare.");
			throw new MarkFailedException("no markDescription specified for mark prepare.");
		}
				
		String markFileName = (String)action.getParameterValue(PARAM_MARK_FILENAME);
		if (markFileName==null) {
			tracer.error("no markFileName specified for mark prepare.");
			throw new MarkFailedException("no markFileName specified for mark prepare.");
		}

		// getting sinekarta admin user
		String sinekartaAdminUserId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		// work area for digital signature initialization
        String userName = authenticationService.getCurrentUserName();
		// piglio la user home
		NodeRef person = personService.getPerson(userName);
		NodeRef homeFolder = DefaultTypeConverter.INSTANCE.convert(NodeRef.class, nodeService.getProperty(person, ContentModel.PROP_HOMEFOLDER));
		// creo la directory sinekarta temp dentro la user home
		NodeRef tmpMarkFolder = NodeTools.deepCreateFolder(nodeService, searchService, fileFolderService, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, 
				NodeTools.translateNamespacePath(namespaceService, nodeService.getPath(homeFolder)) + Configuration.getInstance().getUserSpaceTemporaryFolder());
		markDocumentArea.setTmpMarkFolderPathNodeRefId(tmpMarkFolder.getId());
		markDocumentArea.setTmpMarkFolderPathStoreRefId(tmpMarkFolder.getStoreRef().getIdentifier());
		markDocumentArea.setTmpMarkFolderPathStoreRefProtocol(tmpMarkFolder.getStoreRef().getProtocol());
		String path = NodeTools.translatePath(nodeService, nodeService.getPath(tmpMarkFolder));
		markDocumentArea.setTmpMarkFolderPath(path);
		markDocumentArea.setMarkFileName(markFileName);
		markDocumentArea.setMarkDescription(markDescription);

		StoreRef storeRef = new StoreRef(markDocumentArea.getMarkFolderPathStoreRefProtocol(), markDocumentArea.getMarkFolderPathStoreRefId());
		NodeRef markFolderNodeRef = new NodeRef(storeRef, markDocumentArea.getMarkFolderPathNodeRefId());

		// verifica esistenza file con lo stesso nome in subspace marche

		if (nodeService.getChildByName(markFolderNodeRef,ContentModel.ASSOC_CONTAINS, markFileName + Constants.EXTENSION_PDF)!=null) {
			tracer.error("A Mark file with the specified name already exists in mark folder directory");
			throw new MarkDocumentAlreadyExistsException("A Mark file with the specified name already exists in mark folder directory");
		}

		MarkDocumentPrepareWorker execAsSinekartaAdmin = new MarkDocumentPrepareWorker(contentService, 
				nodeService, fileFolderService, sinekartaDao,  markDocumentArea);
		
		Boolean result = AuthenticationUtil.runAs(execAsSinekartaAdmin, sinekartaAdminUserId);
		if (!result) {
			tracer.error("error on mark document prepare : " + execAsSinekartaAdmin.getKoReason());
			throw new MarkFailedException("error on document update prepare : " + execAsSinekartaAdmin.getKoReason());
		}

		action.setParameterValue(PARAM_RESULT, execAsSinekartaAdmin.getMarkDocumentArea().toBase64String());

		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentAdd action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_MARK_AREA,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_MARK_AREA))); 
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_MARK_DESCRIPTIONR,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_MARK_DESCRIPTIONR))); 
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_MARK_FILENAME,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_MARK_FILENAME))); 
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

	public void setContentService(ContentService contentService) {
		this.contentService = contentService;
	}

	public void setPersonService(PersonService personService) {
		this.personService = personService;
	}

	public void setNodeService(NodeService nodeService) {
		this.nodeService = nodeService;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setFileFolderService(FileFolderService fileFolderService) {
		this.fileFolderService = fileFolderService;
	}

	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

}
class MarkDocumentPrepareWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(MarkDocumentPrepare.class);

	private ContentService contentService;
	private NodeService nodeService;
	private FileFolderService fileFolderService;
	private SinekartaDao sinekartaDao;
	private MarkDocumentArea markDocumentArea;
	private String koReason;

	public MarkDocumentPrepareWorker(ContentService contentService, 
			NodeService nodeService, FileFolderService fileFolderService, 
			SinekartaDao sinekartaDao, MarkDocumentArea markDocumentArea) {
		super();
		this.contentService = contentService;
		this.nodeService = nodeService;
		this.fileFolderService = fileFolderService;
		this.sinekartaDao = sinekartaDao;
		this.markDocumentArea = markDocumentArea;
	}

	@Override
	public Boolean doWork() throws Exception {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSMarkPrepare action.doWork, execution init");

		try {
			StoreRef tmpStoreRef = new StoreRef(markDocumentArea.getTmpMarkFolderPathStoreRefProtocol(), markDocumentArea.getTmpMarkFolderPathStoreRefId());
			
			NodeRef tmpMarkFolder = new NodeRef(tmpStoreRef, markDocumentArea.getTmpMarkFolderPathNodeRefId());
	
			NodeRef tmpMarkDocument = nodeService.getChildByName(tmpMarkFolder,ContentModel.ASSOC_CONTAINS, markDocumentArea.getMarkFileName() + Constants.EXTENSION_PDF);
			if (tmpMarkDocument==null) {
				// creo il documento temp dentro la temp
				tmpMarkDocument = fileFolderService.create(tmpMarkFolder, markDocumentArea.getMarkFileName() + Constants.EXTENSION_PDF,ContentModel.TYPE_CONTENT).getNodeRef();
			}
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSMarkPrepare action.doWork, creazione file PDF/A di marca temporale in sinekarta temporary folder");
			
			ContentWriter contentWriter = contentService.getWriter(tmpMarkDocument, ContentModel.PROP_CONTENT, true);
			OutputStream pdfOut = contentWriter.getContentOutputStream();
			contentWriter.setMimetype(Constants.APPLICATION_PDF);
			contentWriter.setEncoding("UTF-8");
			contentWriter.setLocale(Locale.getDefault());
	
			Document document = new Document(PageSize.A4);
			PdfWriter writer = PdfWriter.getInstance(document, pdfOut);
			writer.setPDFXConformance(PdfWriter.PDFA1A);
	
			PdfDictionary outi = new PdfDictionary(PdfName.OUTPUTINTENT);
			outi.put(PdfName.S, PdfName.GTS_PDFA1);
			writer.getExtraCatalog().put(PdfName.OUTPUTINTENTS, new PdfArray(outi));
			document.open();
	
			@SuppressWarnings("deprecation")
			String font = URLDecoder.decode(this.getClass().getClassLoader().getResource("fonts/extension/sinekarta/Asana-Math.ttf").toString());
			Font titolo = FontFactory.getFont(font,
					BaseFont.CP1252, BaseFont.EMBEDDED, Font.UNDEFINED,
					Font.UNDEFINED, new CMYKColor(255, 255, 255, 0));
			titolo.setSize(20);
			Font sottoTitolo = new Font(titolo);
			sottoTitolo.setSize(16);
			Font capitolo = new Font(titolo);
			capitolo.setSize(12);
			capitolo.setStyle(Font.BOLD);
			Font testo = new Font(titolo);
			testo.setSize(10);
			Font testoEvidenziato = new Font(titolo);
			testoEvidenziato.setSize(10);
			testoEvidenziato.setStyle(Font.BOLD);
	
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			StreamResult streamResult = new StreamResult(baos);
			SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
			// SAX2.0 ContentHandler.
			TransformerHandler hd = tf.newTransformerHandler();
			Transformer serializer = hd.getTransformer();
			serializer.setOutputProperty(OutputKeys.ENCODING,"UTF-8");
			serializer.setOutputProperty(OutputKeys.INDENT,"yes");
			hd.setResult(streamResult);
			hd.startDocument();
			
			AttributesImpl atts = new AttributesImpl();
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			String data = sdf.format(new Date(System.currentTimeMillis()));
			atts.addAttribute("", "", Constants.DATA_APPOSIZIONE_MARCA_TEMPORALE, Constants.CDATA, data);
	
			document.add(new Paragraph(Constants.IMPRONTE, titolo));
			document.add(new Paragraph(Constants.DATA_APPOSIZIONE_MARCA_TEMPORALE + " : " + data, sottoTitolo));
			document.add(new Paragraph("\n",capitolo));
			
			// impronte tag.
			hd.startElement("","",Constants.IMPRONTE,atts);
			
			atts.clear();
			document.add(new Paragraph(Constants.DESCRIZIONE, capitolo));
			document.add(new Paragraph(markDocumentArea.getMarkDescription(), testo));
			document.add(new Paragraph("\n",capitolo));
	
			hd.startElement("","",Constants.DESCRIZIONE,atts);
			String validDescription = markDocumentArea.getMarkDescription();
			validDescription = validDescription.replaceAll("--", "??");
			validDescription = validDescription.replaceAll("]]", "??");
			validDescription = validDescription.replaceAll("<", "?");
			validDescription = validDescription.replaceAll(">", "?");
			writeToTransformerHandler(hd,validDescription);
			hd.endElement("","",Constants.DESCRIZIONE);
			
			String tipoDocumento = null;
			String idRiferimento = null;
			String riferimentoTemporaleFirmaRCS = null;
			String riferimentoTemporaleFirmaPU = null;
			for (MarkDocument n : markDocumentArea.getDocuments()) {
				atts.clear();
				
				StoreRef storeRef = new StoreRef(markDocumentArea.getMarkFolderPathStoreRefProtocol(), markDocumentArea.getMarkFolderPathStoreRefId());
				
				NodeRef nodeRef = new NodeRef(storeRef,n.getNodeRefId());
				
				tipoDocumento = (String)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_DOCUMENT_TYPE);
	
				DocumentType dt = sinekartaDao.getDocumentType(Integer.parseInt(tipoDocumento));
				tipoDocumento = tipoDocumento + " - " + dt.getDescription();
	
				// esecuzione script per identificare la directory di destinazione del file di marca
				atts.addAttribute("","",Constants.TIPO_DOCUMENTO,Constants.CDATA, tipoDocumento);
				idRiferimento = (String)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_REFERENCE_ID);
				atts.addAttribute("","",Constants.ID_RIFERIMENTO,Constants.CDATA, idRiferimento);
				riferimentoTemporaleFirmaRCS = sdf.format((Date)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_TIMESTAMP_RCS_SIGNATURE));
				atts.addAttribute("","",Constants.RIFERIMENTO_TEMPORALE_FIRMA_RCS,Constants.CDATA,riferimentoTemporaleFirmaRCS);
				if (nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_TIMESTAMP_PU_SIGNATURE)!=null) {
					riferimentoTemporaleFirmaPU = sdf.format((Date)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_TIMESTAMP_PU_SIGNATURE));
					atts.addAttribute("","",Constants.RIFERIMENTO_TEMPORALE_FIRMA_PU,Constants.CDATA,riferimentoTemporaleFirmaPU);
				}
				
				String path = NodeTools.translatePath(nodeService, nodeService.getPath(nodeRef)); 
				
				atts.addAttribute("","",Constants.NOME_DOCUMENTO,Constants.CDATA,path);
	
				document.add(new Paragraph(Constants.IMPRONTA + " : " + path, capitolo));
				document.add(new Paragraph(Constants.TIPO_DOCUMENTO + " : " + tipoDocumento, testo));
				document.add(new Paragraph(Constants.ID_RIFERIMENTO + " : " + idRiferimento, testo));
				document.add(new Paragraph(Constants.RIFERIMENTO_TEMPORALE_FIRMA_RCS + " : " + riferimentoTemporaleFirmaRCS, testo));
				if (riferimentoTemporaleFirmaPU!=null) {
					document.add(new Paragraph(Constants.RIFERIMENTO_TEMPORALE_FIRMA_PU + " : " + riferimentoTemporaleFirmaPU, testo));
				}
				
				hd.startElement("","",Constants.IMPRONTA,atts);
				atts.clear();
				atts.addAttribute("","",Constants.ALGORITHM,Constants.CDATA,Constants.SHA256);
				hd.startElement("","",Constants.HASH,atts);
				String impronta = null;
				if (nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_DOCUMENT_PU_SIGNED_FINGERPRINT)!=null) {
					impronta = (String)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_DOCUMENT_PU_SIGNED_FINGERPRINT);
					writeToTransformerHandler(hd,impronta);
				} else {
					impronta = (String)nodeService.getProperty(nodeRef, SinekartaModel.PROP_QNAME_DOCUMENT_RCS_SIGNED_FINGERPRINT);
					writeToTransformerHandler(hd,impronta);
				}
				
				document.add(new Paragraph(Constants.HASH + " [ SHA256 ] : " + impronta , testoEvidenziato));
				document.add(new Paragraph("\n",capitolo));
				
				hd.endElement("","",Constants.HASH);
				hd.endElement("","",Constants.IMPRONTA);
			}
			hd.endElement("","",Constants.IMPRONTE);
			hd.endDocument();
			baos.flush();
			baos.close();
			
			
			ByteArrayOutputStream baosXMP = new ByteArrayOutputStream();
	        XmpWriter xmp = new XmpWriter(baosXMP, writer.getInfo(), writer.getPDFXConformance());
	        String descNameSpaces = "xmlns:pdfaExtension=\"http://www.aiim.org/pdfa/ns/extension/\" " +
	        						"xmlns:pdfaSchema=\"http://www.aiim.org/pdfa/ns/schema#\" " +
	        						"xmlns:pdfaProperty=\"http://www.aiim.org/pdfa/ns/property#\" " +
	        						"xmlns:pdfaType=\"http://www.aiim.org/pdfa/ns/type#\" " +
	        						"xmlns:pdfaField=\"http://www.aiim.org/pdfa/ns/field#\"";
		    xmp.addRdfDescription(descNameSpaces, 
		    		"<pdfaExtension:schemas>" +
		    			"<rdf:Bag>" +
		    				"<rdf:li rdf:parseType=\"Resource\">" +
		    					"<pdfaSchema:schema>Sinekarta marca temporale</pdfaSchema:schema>" +
		    					"<pdfaSchema:namespaceURI>http://www.sinekarta.org/marcatemporale</pdfaSchema:namespaceURI>" +
		    					"<pdfaSchema:prefix>sinekarta</pdfaSchema:prefix>" +
		    					"<pdfaSchema:property>" +
		    						"<rdf:Seq>" +
		    							"<rdf:li rdf:parseType=\"Resource\">" +
		    								"<pdfaProperty:name>impronteContainer</pdfaProperty:name>" +
		    								"<pdfaProperty:valueType>Text</pdfaProperty:valueType>" +
		    								"<pdfaProperty:category>external</pdfaProperty:category>" +
		    								"<pdfaProperty:description>documento xml contenente le impronte oggetto della marca temporale</pdfaProperty:description>" +
		    							"</rdf:li>" +
		    						"</rdf:Seq>" +
		    					"</pdfaSchema:property>" +
		    				"</rdf:li>" +
		    			"</rdf:Bag>" +
		    		"</pdfaExtension:schemas>");
	        
	        
		    xmp.addRdfDescription("xmlns:sinekarta=\"http://www.sinekarta.org/marcatemporale\"", "<sinekarta:impronteContainer><![CDATA[" + new String(baos.toByteArray()) + "]]></sinekarta:impronteContainer>");
	        xmp.close();
		    writer.setXmpMetadata(baosXMP.toByteArray());
	
		    document.close();
			
			writer.close();
			
			markDocumentArea.setTmpMarkDocumentNodeRefId(tmpMarkDocument.getId());
		} catch (Throwable e) {
			tracer.error("DocumentRCSMarkPrepare action.doWork, generic error", e);
			koReason="DocumentRCSMarkPrepare action.doWork, generic error : " + e.getMessage();
			return false;
		}
		
		if (tracer.isDebugEnabled()) tracer.debug("DocumentRCSMarkPrepare action.doWork, finished OK");

		return true;
	}
	
	/**
	 * utility method to create xml doc
	 */
	private void writeToTransformerHandler(TransformerHandler hd, String s) throws Exception {
		char[] o = s.toCharArray();
		hd.characters(o,0,o.length);
	}

	public MarkDocumentArea getMarkDocumentArea() {
		return markDocumentArea;
	}
	
	public String getKoReason() {
		return koReason;
	}
}
