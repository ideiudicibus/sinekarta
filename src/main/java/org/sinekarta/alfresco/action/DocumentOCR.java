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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.repo.security.authentication.AuthenticationUtil.RunAsWork;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.DocumentOCRException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.pdf.util.PDFTools;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.NodeTools;
import org.springframework.beans.factory.InitializingBean;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.itextpdf.text.pdf.BaseFont;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;

/**
 * document OCR action. this action will execute a OCR on a PDF/A document the
 * OCR result will be added as an aspect : SinekartaModel.ASPECT_OCR = "OCR"
 * 
 * - parameter language : the language to use for OCR
 * - parameter user : the userid to use for calculating OCR - in some case we need admin user, in other simple user; it will depend on who is the owner of the document
 * 
 * - no output parameter returned
 * 
 * @author andrea.tessaro
 * 
 */
public class DocumentOCR extends ActionExecuterAbstractBase implements
		InitializingBean {

	public static final String ACTION_NAME_DOCUMENT_OCR = "sinekartaDocumentOCR";
	public static final String PARAM_LANGUAGE = "language";

	private static Logger tracer = Logger.getLogger(DocumentOCR.class);

	private String companyHomePath;
	private SearchService searchService;
	private NodeService nodeService;
	private ContentService contentService;
	private NamespaceService namespaceService;
	private AuthenticationService authenticationService;
	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action, execution init");

		// the document has already the documentOCR aspect? if yes, nothing to
		// do
		if (nodeService.hasAspect(actionedUponNodeRef,SinekartaModel.ASPECT_QNAME_OCR))
			return;

		String lingua = (String)action.getParameterValue(PARAM_LANGUAGE);
		if (lingua==null) {
			lingua=Configuration.getInstance().getLinguaDefaultOcr();
//			tracer.error("no language specified for OCR.");
//			throw new DocumentOCRException("no language specified for OCR.");
		}
		
		String userId;
		if (NodeTools.isArchived(nodeService, namespaceService, actionedUponNodeRef, companyHomePath)) {

			// getting sinekarta admin user
			userId = NodeTools.getSinekartaAdminUserId(nodeService, searchService, companyHomePath);

		} else {
			// getting current user
	        userId = authenticationService.getCurrentUserName();
		}
		
		DocumentOCRWorker execAsUserId = new DocumentOCRWorker(actionedUponNodeRef, lingua, nodeService, contentService);
		
		// run core of action with given user
		Boolean result = AuthenticationUtil.runAs(execAsUserId, userId);
		if (!result) {
			tracer.error("error OCRing document : " + execAsUserId.getKoReason());
			throw new DocumentOCRException("error OCRing document : " + execAsUserId.getKoReason());
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action, execution end");
	}

	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_LANGUAGE,
				DataTypeDefinition.TEXT, 
				false, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_LANGUAGE))); 
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
class MyFilenameFilter implements FilenameFilter {
	private String name;

	public MyFilenameFilter(String name) {
		super();
		this.name = name;
	}

	@Override
	public boolean accept(File dir, String name) {
		return name.contains(this.name);
	}

}
class DocumentOCRWorker implements RunAsWork<Boolean> {

	private static final int LENTEXT = 300; // number of letters for each line of OCR result

	private static Logger tracer = Logger.getLogger(DocumentOCRWorker.class);

	private NodeRef actionedUponNodeRef;
	private NodeService nodeService;
	private ContentService contentService;
	private String lingua;
	private String koReason;
	
	public DocumentOCRWorker(NodeRef actionedUponNodeRef, String  lingua, 
			NodeService nodeService, ContentService contentService) {
		super();
		this.actionedUponNodeRef = actionedUponNodeRef;
		this.nodeService = nodeService;
		this.contentService = contentService;
		this.lingua = lingua;
	}

	@Override
	public Boolean doWork() throws Exception {
		
		if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action.doWork, execution init");

		InputStream is = null; 
		
		ContentReader contentReader = null;
			
		try {
			// get document input stream
			contentReader = contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
			boolean ret = PDFTools.isPdfSigned(contentReader.getContentInputStream());
			if (ret) return ret;
		} catch (Throwable e) {
			tracer.error("DocumentOCR action.doWork, unable to execute OCR on document", e);
			koReason="DocumentOCR action.doWork, unable to execute OCR on document : " + e.getMessage();
			return false;
		} finally {
			try {
				if (is!=null) is.close();
			} catch (IOException e) {
			}
		}
		
		try {
			// get document input stream
			contentReader = contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
			
			String ocrResult = null;
	
			// creo il file temporaneo
			File tmpPdfFile = null;
			OutputStream os = null;
	
			String uuid = (String) this.nodeService.getProperty(actionedUponNodeRef,ContentModel.PROP_NODE_UUID);
			
			try {
				StringBuffer ret = new StringBuffer();
	
				if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action.doWork, writing pdf on temporary dir");
	
				// writing document on temporary directory
				tmpPdfFile = File.createTempFile(uuid, ".pdf");
	
				// scrivo sul file temporaneo tmpFile il pdf ottenuto da alfresco
				os = new FileOutputStream(tmpPdfFile);
				is = contentReader.getContentInputStream();
	
				byte[] buf = new byte[8192];
				int len = is.read(buf);
				while (len!=-1) {
					os.write(buf,0,len);
					os.flush();
					len = is.read(buf);
				}
				os.close();
				is.close();
	
	//			rem %1 the absolute name of the file pdf on which execute OCR (with extension)
	//			rem %2 the language (3 byte) to use for OCR
	//			rem %3 sinekarta directory
				
				// preparing script shell for executing OCR (in a separate process)
				String[] cmd = new String[]{Configuration.getInstance().getSinekartaDir()+"/"+Configuration.getInstance().getProcessoOcr(), tmpPdfFile.getAbsolutePath(), lingua, Configuration.getInstance().getSinekartaDir()};
	
				if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action.doWork, executing OCR process");
	
				Process p = Runtime.getRuntime().exec(cmd);
				BufferedReader input = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String l = input.readLine();
				while (l != null) {
					if (tracer.isDebugEnabled()) {
						tracer.debug(l);
					}
					l = input.readLine();
				}
				input.close();
				
				// parsing resulting text files and deleting created temporary images
	
				if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action.doWork, OCR process finished, reading output");
	
				File[] ff = (new java.io.File(tmpPdfFile.toURI())).getParentFile().listFiles(new MyFilenameFilter(uuid));
				
				for (File tf : ff) {
					if (tf.getAbsolutePath().endsWith(".txt")) {
						// number of bytes saved as metadata as configured on sinekarta-repository.properties
						StringBuffer subret = new StringBuffer();
						FileInputStream fis = new FileInputStream(tf);
						byte[] bufRet = new byte[1024];
						int lenRet = fis.read(bufRet);
						while (lenRet != -1) {
							try {
								subret.append(new String(bufRet,0,lenRet,"UTF-8"));
							} catch (Exception e) { }
							lenRet = fis.read(bufRet);
						}
						fis.close();
						ret.append(fixResult(subret.toString().trim()));
						ret.append(" ");
					}
					tf.delete();
				}
				
				ocrResult = ret.toString();
				
			} catch (Exception e) {
				tracer.error("Unable execute OCR on : " + tmpPdfFile.getAbsolutePath(), e);
				throw new DocumentOCRException("Unable to execute OCR on : " + tmpPdfFile.getAbsolutePath(), e);
			} finally {
				if (is!=null) {
					try {
						is.close();
					} catch (IOException e) {
						// nothing to do
					}
				}
				if (os!=null) {
					try {
						os.close();
					} catch (IOException e) {
						// nothing to do
					}
				}
				// se c'e' stata un'eccezione, i file temporanei rimangono li', ripulisco comunque....
				try {
					File[] ff = (new java.io.File(tmpPdfFile.toURI())).getParentFile().listFiles(new MyFilenameFilter(uuid));
					for (File tf : ff) {
						tf.delete();
					}
				} catch (Exception e) {
					// nothing to do
				}
			}
						
			// adding metadata as XMP metadata on PDF
			contentReader = contentService.getReader(actionedUponNodeRef, ContentModel.PROP_CONTENT);
			
			is = contentReader.getContentInputStream();
			 
			// verifica se il documento contiene gia' l'XML di sinekarta riguardante l'OCR
			// parso il PDF/A per ottenere l'XMP (xml)
			PdfReader reader = new PdfReader(is);
			
			if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action.doWork, updating PDF adding XMP metadata of OCR");
	
			// verifica se il documento contiene gia' l'XML di sinekarta riguardante l'OCR
			byte[] md = reader.getMetadata();
			
			String metas;
			if (md==null || md.length==0) {
				metas = "";
			} else {
				metas = new String(md,"UTF-8");
			}
			
			if (metas.indexOf("<sinekartaOCR:ocrContainer>")==-1) {
	
				// adding ocr result as xmp metadata (for further use)
				Document docXmp =null;
				DocumentBuilderFactory dbf =DocumentBuilderFactory.newInstance();
				dbf.setNamespaceAware(true);
				DocumentBuilder db = dbf.newDocumentBuilder();
				
				try {
					try {
						ByteArrayInputStream bais = new ByteArrayInputStream(reader.getMetadata());
						// parse del XMP per aggiungere i nuovi metadati
						docXmp = db.parse(bais);
					} catch (Exception e) {
						try {
							docXmp = db.parse(new String(reader.getMetadata(),"UTF-8"));
						} catch (Exception e1) {
							docXmp = db.parse(new String(reader.getMetadata(),"ISO-8859-2"));
						}
					}
				} catch (Throwable t) {
					docXmp = db.newDocument();
					// preparo l'XMP description per lo schema della description OCR
					ByteArrayInputStream baisToAddx = new ByteArrayInputStream(
							("<?xpacket begin=\"\uFEFF\" id=\"W5M0MpCehiHzreSzNTczkc9d\"?>\n" +
							 "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\">\n" +
							 "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\">\n" +
							 "</rdf:RDF>\n"+
							 "</x:xmpmeta>\n" +
							 "<?xpacket end=\"w\"?>").getBytes());
			
					Document docXmpToAddx = db.parse(baisToAddx);
			
					docXmp.appendChild(docXmp.importNode(docXmpToAddx.getDocumentElement(),true));
				}
		
				// preparo l'XMP description per lo schema della description OCR
				ByteArrayInputStream baisToAdd1 = new ByteArrayInputStream(
						("<rdf:Description rdf:about=\"\" "+
							"xmlns:pdfaExtension=\"http://www.aiim.org/pdfa/ns/extension/\" " +
							"xmlns:pdfaSchema=\"http://www.aiim.org/pdfa/ns/schema#\" " +
							"xmlns:pdfaProperty=\"http://www.aiim.org/pdfa/ns/property#\" " +
							"xmlns:pdfaType=\"http://www.aiim.org/pdfa/ns/type#\" " +
							"xmlns:pdfaField=\"http://www.aiim.org/pdfa/ns/field#\" " +
							"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"" +
							">"+
							"<pdfaExtension:schemas>" +
								"<rdf:Bag>" +
									"<rdf:li rdf:parseType=\"Resource\">" +
										"<pdfaSchema:schema>Sinekarta OCR</pdfaSchema:schema>" +
										"<pdfaSchema:namespaceURI>http://www.sinekarta.org/ocr</pdfaSchema:namespaceURI>" +
										"<pdfaSchema:prefix>sinekartaOCR</pdfaSchema:prefix>" +
										"<pdfaSchema:property>" +
											"<rdf:Seq>" +
												"<rdf:li rdf:parseType=\"Resource\">" +
													"<pdfaProperty:name>ocrContainer</pdfaProperty:name>" +
													"<pdfaProperty:valueType>Text</pdfaProperty:valueType>" +
													"<pdfaProperty:category>external</pdfaProperty:category>" +
													"<pdfaProperty:description>testo risultato dell'OCR</pdfaProperty:description>" +
												"</rdf:li>" +
											"</rdf:Seq>" +
										"</pdfaSchema:property>" +
									"</rdf:li>" +
								"</rdf:Bag>" +
							"</pdfaExtension:schemas>" + 
						"</rdf:Description>").getBytes());
		
				Document docXmpToAdd1 = db.parse(baisToAdd1);
		
				Node rdf = docXmp.getElementsByTagNameNS("http://www.w3.org/1999/02/22-rdf-syntax-ns#","RDF").item(0);
				
				rdf.appendChild(docXmp.importNode(docXmpToAdd1.getDocumentElement(),true));
		
				// preparo l'XMP description OCR
				ByteArrayInputStream baisToAdd2 = new ByteArrayInputStream(
						("<rdf:Description rdf:about=\"\" "+
							"xmlns:sinekartaOCR=\"http://www.sinekarta.org/ocr\" " +
							"xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"" +
							">"+
							"<sinekartaOCR:ocrContainer><![CDATA[" + ocrResult + "]]></sinekartaOCR:ocrContainer>" +
						"</rdf:Description>").getBytes("UTF-8"));
				
				Document docXmpToAdd2 = db.parse(baisToAdd2);
				
				rdf.appendChild(docXmp.importNode(docXmpToAdd2.getDocumentElement(),true));
		
				// ottengo il nuovo XMP (esistente + nuovi dati OCR)
				TransformerFactory tFactory = TransformerFactory.newInstance();
				Transformer transformer = tFactory.newTransformer();
		
				DOMSource source = new DOMSource(docXmp);
		
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				StreamResult result = new StreamResult(baos);
				transformer.transform(source, result); 
		
				// ottengo lo stream di update del documento
				ContentWriter contentWriter = contentService.getWriter(actionedUponNodeRef, ContentModel.PROP_CONTENT, true);
				os = contentWriter.getContentOutputStream();
				contentWriter.setMimetype(contentReader.getMimetype());
				contentWriter.setEncoding("UTF-8");
				contentWriter.setLocale(contentReader.getLocale());
		
				// creo lo stamper per aggiungere il nuovo XMP al documento
				PdfStamper stamper = new PdfStamper(reader, os);
		        
		        stamper.setXmpMetadata(baos.toByteArray());
		        
		        // now we also add ocr result as hidden text
		        // Alfresco will index (full text) the ocred content of the document
		        PdfContentByte cont = stamper.getUnderContent(1);
		        BaseFont bf = BaseFont.createFont(BaseFont.HELVETICA,
	                    BaseFont.WINANSI,
	                    BaseFont.EMBEDDED);
		        
		        cont.saveState();
		        cont.beginText();
		        cont.setTextRenderingMode(PdfContentByte.TEXT_RENDER_MODE_INVISIBLE);
		        // the font is very little, to have more words in a line
		        cont.setFontAndSize(bf, 0.1f);
		        
		        int i = 0;
		        int k = LENTEXT;
		        // searching for space near the length of the line
		        if (k<=ocrResult.length()) {
		        	while (ocrResult.charAt(k)!=' ') {
		        		k--;
		        		if (k<i) {
		        			k = i+LENTEXT;
		        			break;
		        		}
		        	}
		        }
		        while (k<=ocrResult.length()) {
			        // write ocred text
			        cont.newlineShowText(ocrResult.substring(i,k));
		        	i=k;
		        	k=k+LENTEXT;
		        	if (k<=ocrResult.length()) {
			        	while (ocrResult.charAt(k)!=' ') {
			        		k--;
			        		if (k<i) {
			        			k = i+LENTEXT;
			        			break;
			        		}
			        	}
		        	}
		        }
		        cont.newlineShowText(ocrResult.substring(i));
		        cont.endText();
		        cont.restoreState();
		        stamper.close();
			}
			reader.close();
			
			// calculating OCR properties values
			Map<QName, Serializable> properties = new HashMap<QName, Serializable>();
			if (ocrResult.length()>Configuration.getInstance().getOcrMetadataLenght()) {
				ocrResult = ocrResult.substring(0,Configuration.getInstance().getOcrMetadataLenght());
			}
			properties.put(SinekartaModel.PROP_QNAME_OCR_RESULT, ocrResult);
	
			// adding OCR aspect to document
			nodeService.addAspect(actionedUponNodeRef, SinekartaModel.ASPECT_QNAME_OCR, properties);
		} catch (Throwable e) {
			tracer.error("DocumentOCR action.doWork, unable to execute OCR on document", e);
			koReason="DocumentOCR action.doWork, unable to execute OCR on document : " + e.getMessage();
			return false;
		} finally {
			if (is!=null) {
				try {
					is.close();
				} catch (IOException e) {
					// nothing to do
				}
			}
		}

		if (tracer.isDebugEnabled()) tracer.debug("DocumentOCR action.doWork, finished ok");

		return true;
	}
	
	public String getKoReason() {
		return koReason;
	}

	private String fixResult(String res) {
		StringBuilder sb = new StringBuilder(res.length());
		boolean blank=false;
		for (int i=0;i<res.length();i++) {
			char cur = res.charAt(i);
			if (cur!=' ') {
				if (Character.isLetterOrDigit(cur)) {
					sb.append(cur);
					blank=false;
				}
			} else {
				if (!blank) {
					sb.append(cur);
				}
				blank=true;
			}
		}
		return sb.toString();
	}
}