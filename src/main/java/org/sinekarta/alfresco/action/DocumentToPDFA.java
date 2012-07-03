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

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.alfresco.error.AlfrescoRuntimeException;
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
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.DocumentToPDFAException;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.DefaultResourceLoader;

import com.artofsolving.jodconverter.DocumentFamily;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.DocumentFormatRegistry;
import com.artofsolving.jodconverter.XmlDocumentFormatRegistry;
import com.artofsolving.jodconverter.openoffice.connection.AbstractOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeException;
import com.artofsolving.jodconverter.openoffice.converter.StreamOpenOfficeDocumentConverter;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.RectangleReadOnly;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.TiffImage;
import com.sun.star.beans.PropertyValue;

/**
 * document conversion to PDF/A action.
 * this action will convert a document added to archive into a PDF/A document
 * 
 * - no input parameter needed
 * - no output parameter returned
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentToPDFA extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(DocumentToPDFA.class);
	public static final String ACTION_NAME_DOCUMENT_TO_PDFA = "sinekartaDocumentToPDFA";
	
	private String companyHomePath;
	private SearchService searchService;
	private NodeService nodeService;
	private ContentService contentService;
	private NamespaceService namespaceService;
	private AuthenticationService authenticationService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;
	private AbstractOpenOfficeConnection connection;
	private StreamOpenOfficeDocumentConverter converter;
	private String documentFormatsConfiguration;
	private DocumentFormatRegistry formatRegistry;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentToPDFA action, execution init");
		
		// first of all : converting document to PDF/A using jodconverter
		if (!isAvailable()) {
			tracer.error("Openoffice connection not available");
			throw new DocumentToPDFAException("Openoffice connection not available");
		}

		String userId;
		if (NodeTools.isArchived(nodeService, namespaceService, actionedUponNodeRef, companyHomePath)) {
			// getting sinekarta admin user
			userId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		} else {
			// getting current user
	        userId = authenticationService.getCurrentUserName();
		}
		
		// TODO, determinare dpi!!!
		// per ora assumiamo fissi 300 dpi, minimo indispensabile per fare un OCR decente
			
		DocumentToPDFAWorker execAsUserId = new DocumentToPDFAWorker(nodeService, contentService, actionedUponNodeRef, converter, formatRegistry, 300);
		
		Boolean result = AuthenticationUtil.runAs(execAsUserId, userId);
		if (!result) {
			tracer.error("error on document to pdfa conversion : " + execAsUserId.getKoReason());
			throw new DocumentToPDFAException("error on document to pdfa conversion : " + execAsUserId.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentAcquiring action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
	}

    public void afterPropertiesSet() throws Exception {

		DefaultResourceLoader resourceLoader = new DefaultResourceLoader();
		try {
			InputStream is = resourceLoader.getResource(documentFormatsConfiguration).getInputStream();
			formatRegistry = new XmlDocumentFormatRegistry(is);
		} catch (IOException e) {
			tracer.error("Unable to load document formats configuration file: "+ documentFormatsConfiguration, e);
			throw new AlfrescoRuntimeException("Unable to load document formats configuration file: " + documentFormatsConfiguration,e);
		}

		// set up the converter
		if (converter == null) {
			converter = new StreamOpenOfficeDocumentConverter(connection);
		}
	}

	public boolean isAvailable() {
		if (!connection.isConnected()) {
			try {
				connection.connect();
			} catch (ConnectException e) {
				return false;
			}
			return connection.isConnected();
		} else 
			return true;
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

	public void setConnection(AbstractOpenOfficeConnection connection) {
		this.connection = connection;
	}

	public void setDocumentFormatsConfiguration(String path) {
		this.documentFormatsConfiguration = path;
	}

	public void setNamespaceService(NamespaceService namespaceService) {
		this.namespaceService = namespaceService;
	}

	public void setCompanyHomePath(String companyHomePath) {
		this.companyHomePath = companyHomePath;
	}

	public void setSearchService(SearchService searchService) {
		this.searchService = searchService;
	}

	public void setAuthenticationService(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

}
class DocumentToPDFAWorker implements RunAsWork<Boolean> {

	private static Logger tracer = Logger.getLogger(DocumentToPDFA.class);

	private NodeService nodeService;
	private ContentService contentService;
	private NodeRef actionedUponNodeRef;
	private StreamOpenOfficeDocumentConverter  converter;
	private DocumentFormatRegistry formatRegistry;
	private int resolution;
	private String koReason;

	public DocumentToPDFAWorker(NodeService nodeService,
			ContentService contentService, NodeRef actionedUponNodeRef,
			StreamOpenOfficeDocumentConverter converter,
			DocumentFormatRegistry formatRegistry, int resolution) {
		super();
		this.nodeService = nodeService;
		this.contentService = contentService;
		this.actionedUponNodeRef = actionedUponNodeRef;
		this.converter = converter;
		this.formatRegistry = formatRegistry;
		this.resolution = resolution;
	}

	@Override
	public Boolean doWork() {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentToPDFA action.doWork, execution init");

		try {
			ContentReader contentReader = contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
	
			String documentMimetype = (String) contentReader.getMimetype();
			
			if (documentMimetype.equals(Constants.APPLICATION_PDF)) {
				if (PDFTools.isPdfa(contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT).getContentInputStream()) || PDFTools.isPdfSigned(contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT).getContentInputStream())) {
					// is a PDF/A, this will not be converted
					if (tracer.isDebugEnabled()) tracer.debug("DocumentToPDFA action.doWork, the input document is PDF/A, no conversion needed");
				} else {
					// preparing the new document content, the pdf/a content 
					ContentWriter contentWriter = contentService.getWriter(actionedUponNodeRef, ContentModel.PROP_CONTENT, true);
					contentWriter.setMimetype(Constants.APPLICATION_PDF);
					contentWriter.setEncoding("UTF-8");
					contentWriter.setLocale(contentReader.getLocale());

					// is not a PDF/A, the document need to be converted
					if (tracer.isDebugEnabled()) tracer.debug("DocumentToPDFA action.doWork, converting document using openoffice (pdf-import plugin)");
					// need to reload contetReader, the contetReader stream was read to understand pdf type
					contentReader = contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
					convertUsingOpenOffice(contentReader, contentWriter);
				}
			} else {
				// preparing the new document content, the pdf/a content 
				ContentWriter contentWriter = contentService.getWriter(actionedUponNodeRef, ContentModel.PROP_CONTENT, true);
				contentWriter.setMimetype(Constants.APPLICATION_PDF);
				contentWriter.setEncoding("UTF-8");
				contentWriter.setLocale(contentReader.getLocale());

				if (documentMimetype.equals(Constants.IMAGE_GIF) ||
					documentMimetype.equals(Constants.IMAGE_PNG) ||
					documentMimetype.equals(Constants.IMAGE_JPG) ||
					documentMimetype.equals(Constants.IMAGE_TIF)) {
					if (tracer.isDebugEnabled()) tracer.debug("DocumentToPDFA action.doWork, converting image(s) using Itext");
					// if the document it's an image, i will create a PDF/A using itext
					convertUsingItext(contentReader, contentWriter, resolution);
				} else {
					if (tracer.isDebugEnabled()) tracer.debug("DocumentToPDFA action.doWork, unknown mimetype converting document using openoffice");
					// in all other formats i will try with openoffice
					convertUsingOpenOffice(contentReader, contentWriter);
				}
			}
			
			// renaming the file, after conversion the file is a .pdf
			String fileName = PDFTools.calculatePdfName((String) nodeService.getProperty(actionedUponNodeRef, ContentModel.PROP_NAME));
			nodeService.setProperty(actionedUponNodeRef, ContentModel.PROP_NAME, fileName);
		} catch (Throwable e) {
			tracer.error("DocumentToPDFA action.doWork, unable to execute work", e);
			koReason="DocumentToPDFA action.doWork, unable to execute work : " + e.getMessage();
			return false;
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentToPDFA action.doWork, finished OK");

		return true;
	}
	
	public String getKoReason() {
		return koReason;
	}

	/**
	 * 
	 * metodo di utilita' per convertire immagini in pdf/A
	 * 
	 * @param actionedUponNodeRef
	 * @param contentReader
	 * @param contentWriter
	 */
	private void convertUsingItext(ContentReader contentReader, ContentWriter contentWriter, int resolution) {
		String sourceExtension = (String) contentReader.getMimetype();
		// download the content from the source reader
		InputStream is = contentReader.getContentInputStream();
		OutputStream os = contentWriter.getContentOutputStream();

		Document document=null;
		PdfWriter writer=null;
		try {
			if (sourceExtension.equals(Constants.IMAGE_TIF)) {
				RandomAccessFileOrArray ra = new RandomAccessFileOrArray(is);
				int comps = TiffImage.getNumberOfPages(ra);
				PdfContentByte cb = null;
				for (int c = 0; c < comps; ++c) {
					Image img = TiffImage.getTiffImage(ra, c + 1);
					if (img != null) {
			  			img.setAbsolutePosition(0, 0);
						// calcolo la dimensione della pagina 
//						e' necessario riadattare la dimensione della pagina in base alla risoluzione dell''immagine
//						se l''immagine e' 100x100 px e la risoluzione dell''immagine e' 300x300 dpi
//						contando che il pdf e' 72x72 dpi, e' necessario riadattare width e height
//						ribaltare questi ragionamenti su tutte le creazioni dei pdf a partire dall''immagine
//						
//						esempio pratico : 
//							ho scannerizzato un A4 in bassa risoluzione (da verificare i dpi)
//							mi ha creato un immagine con una certa dimensione (da verificare i pixel)
//							l''immagine e' stata riportata nel PDF creando una pagina da 580,7 x 822,3 mm che non corrisponde a nessun formato carta
//							Stampando successivamente questo documento scannerizzato la spampante si incarta perche' non sa che carta usare.
//						
//						Sapendo che il PDF lavora su 72 dpi e che la misura del foglio derivata e' di 580,7 x 822,3 mm capiamo quanto era grande l''immagine in pixel : 
//						1 inch = 25.4 mm
//						72 dpi = 72 * 25.4 = 1828,8 dpmm
//						dimensione in px dell''immagine = (580,7 * 1828,8) x  (822,3 * 1828,8) = 1061984 x 1503822 px
//						la risoluzione era (essendo il foglio un A4 = 210 x 297 mm): (72 * 580,7)/210 x (72 * 822,3)/297 = 200 x 200 risoluzione
	//
//						
//						La dimensione della pagina del pdf (da calcolare) deve essere rapportata a 72 DPI, quindi sapendo la risoluzione originaria : 
//						
//							dpiw/dimw = 72/x
//							dimw/dpiw = x/72
//							(dimw * 72)/dpiw = 72
			  			
						float width = (img.getWidth() * 72)/resolution;
						float height = (img.getHeight() * 72)/resolution;						
						
//	 					Eseguo resize dell'immagine per adeguarla alla dimensione del pdf					
						img.scaleToFit(width, height);
						
						RectangleReadOnly pageSize = new RectangleReadOnly(width, height);
						if ( document==null ){
							// creo il PDF/A che andra' a contenere il documento scannerizzato
							document = new Document(pageSize,0,0,0,0);
							try {
								writer = PdfWriter.getInstance(document, os);
							} catch (Exception e) {
								tracer.error("Unable to convert from "
										+ sourceExtension
										+ " to PDF/A. Prolem creating PDF/A document.",e);
								throw new DocumentToPDFAException("Unable to convert from "
										+ sourceExtension
										+ " to PDF/A. Prolem creating PDF/A document.",e);
							}
							writer.setPDFXConformance(PdfWriter.PDFA1A);
							PdfDictionary outi = new PdfDictionary(PdfName.OUTPUTINTENT);
							outi.put(PdfName.S, PdfName.GTS_PDFA1);
							writer.getExtraCatalog().put(PdfName.OUTPUTINTENTS, new PdfArray(outi));
							document.open();
							cb = writer.getDirectContent();
						}else{
							document.setPageSize(pageSize);
						}
						cb.addImage(img);
						document.newPage();
					}
				}
				ra.close();
			} else {
				try {
					writer = PdfWriter.getInstance(document, os);
				} catch (Exception e) {
					tracer.error("Unable to convert from "
							+ sourceExtension
							+ " to PDF/A. Prolem creating PDF/A document.",e);
					throw new DocumentToPDFAException("Unable to convert from "
							+ sourceExtension
							+ " to PDF/A. Prolem creating PDF/A document.",e);
				}
				writer.setPDFXConformance(PdfWriter.PDFA1A);
				BufferedImage image = ImageIO.read(is);
				Image imagePdf = Image.getInstance(writer, image, 0.95f);
				imagePdf.setAbsolutePosition(0, 0);
				imagePdf.setCompressionLevel(9);
				// calcolo la dimensione della pagina 
//				e' necessario riadattare la dimensione della pagina in base alla risoluzione dell''immagine
//				se l''immagine e' 100x100 px e la risoluzione dell''immagine e' 300x300 dpi
//				contando che il pdf e' 72x72 dpi, e' necessario riadattare width e height
//				ribaltare questi ragionamenti su tutte le creazioni dei pdf a partire dall''immagine
//				
//				esempio pratico : 
//					ho scannerizzato un A4 in bassa risoluzione (da verificare i dpi)
//					mi ha creato un immagine con una certa dimensione (da verificare i pixel)
//					l''immagine e' stata riportata nel PDF creando una pagina da 580,7 x 822,3 mm che non corrisponde a nessun formato carta
//					Stampando successivamente questo documento scannerizzato la spampante si incarta perche' non sa che carta usare.
//				
//				Sapendo che il PDF lavora su 72 dpi e che la misura del foglio derivata e' di 580,7 x 822,3 mm capiamo quanto era grande l''immagine in pixel : 
//				1 inch = 25.4 mm
//				72 dpi = 72 * 25.4 = 1828,8 dpmm
//				dimensione in px dell''immagine = (580,7 * 1828,8) x  (822,3 * 1828,8) = 1061984 x 1503822 px
//				la risoluzione era (essendo il foglio un A4 = 210 x 297 mm): (72 * 580,7)/210 x (72 * 822,3)/297 = 200 x 200 risoluzione
//
//				
//				La dimensione della pagina del pdf (da calcolare) deve essere rapportata a 72 DPI, quindi sapendo la risoluzione originaria : 
//				
//					dpiw/dimw = 72/x
//					dimw/dpiw = x/72
//					(dimw * 72)/dpiw = 72
	  			
				float width = (imagePdf.getWidth() * 72)/resolution;
				float height = (imagePdf.getHeight() * 72)/resolution;
				
//					Eseguo resize dell'immagine per adeguarla alla dimensione del pdf					
				imagePdf.scaleToFit(width, height);
				
				RectangleReadOnly pageSize = new RectangleReadOnly(width, height);
				document = new Document(pageSize,0,0,0,0);
				PdfDictionary outi = new PdfDictionary(PdfName.OUTPUTINTENT);
				outi.put(PdfName.S, PdfName.GTS_PDFA1);
				writer.getExtraCatalog().put(PdfName.OUTPUTINTENTS, new PdfArray(outi));
				document.open();
				document.add(imagePdf); 	
			}
		} catch (Exception e) {
			tracer.error("Unable to convert from "
					+ sourceExtension
					+ " to PDF/A. Prolem reading/writing image.",e);
			throw new DocumentToPDFAException("Unable to convert from "
					+ sourceExtension
					+ " to PDF/A. Prolem reading/writing image.",e);
		}
		writer.createXmpMetadata();
		document.close();
		// this should be already done, but ...
		// closing streams
		try {
			is.close();
		} catch (IOException e) {
			tracer.error("error on input stream", e);
		}
	    try {
	    	os.flush();
		} catch (IOException e) {
			tracer.error("error on output stream",e);
		}
	    try {
	    	os.close();
		} catch (IOException e) {
			tracer.error("error on output stream",e);
		}

	}
	
	/**
	 * 
	 * metodo di utilita' per convertire documenti in pdf/A
	 * 
	 * @param actionedUponNodeRef
	 * @param contentReader
	 * @param contentWriter
	 */
	private void convertUsingOpenOffice(ContentReader contentReader, ContentWriter contentWriter) {
		String sourceExtension = (String) contentReader.getMimetype();

		// download the content from the source reader
		InputStream is = contentReader.getContentInputStream();
		OutputStream os = contentWriter.getContentOutputStream();
      
		// query the registry for the source format
		DocumentFormat sourceFormat = formatRegistry.getFormatByMimeType(sourceExtension);
		if (sourceFormat == null) {
			tracer.error("Unable to convert from "
					+ sourceExtension
					+ " to PDF/A. Input document format unknow");
			throw new DocumentToPDFAException("Unable to convert from "
					+ sourceExtension
					+ " to PDF/A. Input document format unknow");
		}

		// setting mediatype (mimetype) of source format
		sourceFormat.setImportOption("MediaType", sourceExtension);
		
		// query the registry for the target format
		DocumentFormat targetFormat = formatRegistry.getFormatByMimeType(Constants.APPLICATION_PDF);
		if (targetFormat == null) {
			tracer.error("Unable to convert from "
					+ sourceExtension
					+ " to PDF/A. OpenOffice does not support PDF??");
			throw new DocumentToPDFAException("Unable to convert from "
					+ sourceExtension
					+ " to PDF/A. OpenOffice does not support PDF??");
		}
        List<PropertyValue> pdfFilterDataList = new ArrayList<PropertyValue>();
        
		// setting parameters to produce a good pdf/a

        
        // Filter data comments origin:
        // http://www.openoffice.org/nonav/issues/showattachment.cgi/37895/draft-doc-pdf-security.odt
        // http://specs.openoffice.org/appwide/pdf_export/PDFExportDialog.odt

        // Set the password that a user will need to change the permissions
        // of the exported PDF. The password should be in clear text.
        // Must be used with the "RestrictPermissions" property
        PropertyValue pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "PermissionPassword";
        pdfFilterDataElement.Value = "nopermission";
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specify that PDF related permissions of this file must be
        // restricted. It is meaningfull only if the "PermissionPassword"
        // property is not empty
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "RestrictPermissions";
        pdfFilterDataElement.Value = Boolean.FALSE;
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies that the PDF document should be encrypted while
        // exporting it, meanifull only if the "DocumentOpenPassword"
        // property is not empty
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "EncryptFile";
        pdfFilterDataElement.Value = Boolean.FALSE;
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies printing of the document:
        //   0: PDF document cannot be printed
        //   1: PDF document can be printed at low resolution only
        //   2: PDF document can be printed at maximum resolution.
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "Printing";
        pdfFilterDataElement.Value = new Integer(2);
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies the changes allowed to the document:
        //   0: PDF document cannot be changed
        //   1: Inserting, deleting and rotating pages is allowed
        //   2: Filling of form field is allowed
        //   3: Filling of form field and commenting is allowed
        //   4: All the changes of the previous selections are permitted,
        //      with the only exclusion of page extraction
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "Changes";
        pdfFilterDataElement.Value = new Integer(0);
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies that the pages and the PDF document content can be
        // extracted to be used in other documents: Copy from the PDF
        // document and paste eleswhere
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "EnableCopyingOfContent";
        pdfFilterDataElement.Value = Boolean.TRUE;
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies that the PDF document content can be extracted to
        // be used in accessibility applications
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "EnableTextAccessForAccessibilityTools";
        pdfFilterDataElement.Value = Boolean.TRUE;
        pdfFilterDataList.add(pdfFilterDataElement);
        
        // Specifies which pages are exported to the PDF document.
        // To export a range of pages, use the format 3-6.
        // To export single pages, use the format 7;9;11.
        // Specify a combination of page ranges and single pages
        // by using a format like 2-4;6.
        // If the document has less pages than defined in the range,
        // the result might be the exception
        // "com.sun.star.task.ErrorCodeIOException".
        // This exception occured for example by using an ODT file with
        // only one page and a page range of "2-4;6;8-10". Changing the
        // page range to "1" prevented this exception.
        // For no apparent reason the exception didn't occure by using
        // an ODT file with two pages and a page range of "2-4;6;8-10".
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "Pages";
        pdfFilterDataElement.Value = "All";
        pdfFilterDataList.add(pdfFilterDataElement);
        
        // Specifies if graphics are exported to PDF using a
        // lossless compression. If this property is set to true,
        // it overwrites the "Quality" property
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "UseLosslessCompression";
        pdfFilterDataElement.Value = Boolean.TRUE;
        pdfFilterDataList.add(pdfFilterDataElement);
        
        // Specifies the quality of the JPG export in a range from 0 to 100.
        // A higher value results in higher quality and file size.
        // This property affects the PDF document only, if the property
        // "UseLosslessCompression" is false
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "Quality";
        pdfFilterDataElement.Value = new Integer(80);
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies if the resolution of each image is reduced to the
        // resolution specified by the property "MaxImageResolution".
        // If the property "ReduceImageResolution" is set to true and
        // the property "MaxImageResolution" is set to a DPI value, the
        // exported PDF document is affected by this settings even if
        // the property "UseLosslessCompression" is set to true, too
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "ReduceImageResolution";
        pdfFilterDataElement.Value = Boolean.FALSE;
        pdfFilterDataList.add(pdfFilterDataElement);
        
//        // If the property "ReduceImageResolution" is set to true
//        // all images will be reduced to the given value in DPI
//        pdfFilterData[12] = new PropertyValue();
//        pdfFilterData[12].Name = "MaxImageResolution";
//        pdfFilterData[12].Value = new Integer(100);

        // Specifies whether form fields are exported as widgets or
        // only their fixed print representation is exported
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "ExportFormFields";
        pdfFilterDataElement.Value = Boolean.FALSE;
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies that the PDF viewer window is centered to the
        // screen when the PDF document is opened
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "CenterWindow";
        pdfFilterDataElement.Value = Boolean.FALSE;
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies the action to be performed when the PDF document
        // is opened:
        //   0: Opens with default zoom magnification
        //   1: Opens magnified to fit the entire page within the window
        //   2: Opens magnified to fit the entire page width within
        //      the window
        //   3: Opens magnified to fit the entire width of its boundig
        //      box within the window (cuts out margins)
        //   4: Opens with a zoom level given in the "Zoom" property
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "Magnification";
        pdfFilterDataElement.Value = new Integer(0);
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies that automatically inserted empty pages are
        // suppressed. This option only applies for storing Writer
        // documents.
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "IsSkipEmptyPages";
        pdfFilterDataElement.Value = Boolean.FALSE;
        pdfFilterDataList.add(pdfFilterDataElement);

        // Specifies the PDF version that should be generated:
        //   0: PDF 1.4 (default selection)
        //   1: PDF/A-1 (ISO 19005-1:2005)
        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "SelectPdfVersion";
        pdfFilterDataElement.Value = new Integer(1);
        pdfFilterDataList.add(pdfFilterDataElement);
        
        // Specifies the change allowed to the document. 
        // Possible values: 
        //	0 = The document cannot be changed. 
        //	1 = Inserting deleting and rotating pages is allowed. 
        //	2 = Filling of form field is allowed. 
        //	3 = Both filling of form field and commenting is allowed. 
        //	4 = All the changes of the previous selections are permitted, with the only exclusion of page extraction (copy). 

        pdfFilterDataElement = new PropertyValue();
        pdfFilterDataElement.Name = "Changes";
        pdfFilterDataElement.Value = new Integer(0);
        pdfFilterDataList.add(pdfFilterDataElement);

        PropertyValue pdfFilterDataArray[] = new PropertyValue[pdfFilterDataList.size()];
        pdfFilterDataList.toArray(pdfFilterDataArray);
        
        targetFormat.setExportOption(sourceFormat.getFamily(), "FilterData", pdfFilterDataArray);

		// get the family of the target document
		DocumentFamily sourceFamily = sourceFormat.getFamily();
		
		// does the format support the conversion ?
		if (!targetFormat.isExportableFrom(sourceFamily)) {
			tracer.error("Unable to convert from "
					+ sourceExtension
					+ " to PDF/A. OpenOffice does not support this type of transformation.");
			throw new DocumentToPDFAException(
					"Unable to convert from "
							+ sourceExtension
							+ " to PDF/A. OpenOffice does not support this type of transformation.");
		}

		// this is the real conversion!!! call to jodconverter 
		try {
			converter.convert(is, sourceFormat, os, targetFormat);
			// conversion success
		} catch (OpenOfficeException e) {
			tracer.error("OpenOffice server conversion failed: \n" 
					+ "   reader: " + contentReader + "\n" 
					+ "   writer: " + contentWriter + "\n", e);
			throw new DocumentToPDFAException(
					"OpenOffice server conversion failed: \n" 
					+ "   reader: " + contentReader + "\n" 
					+ "   writer: " + contentWriter + "\n", e);
		}

		// this should be already done, but ...
		// closing streams
		try {
			is.close();
		} catch (IOException e) {
			tracer.error("error on input stream", e);
		}
	    try {
	    	os.flush();
		} catch (IOException e) {
			tracer.error("error on output stream",e);
		}
	    try {
	    	os.close();
		} catch (IOException e) {
			tracer.error("error on output stream",e);
		}

	}

}
