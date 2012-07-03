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
package org.sinekarta.alfresco.webscript.request;

import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.sinekarta.alfresco.webscript.GenericArea;

@XmlRootElement(name = "sinekarta")
@XmlAccessorType(XmlAccessType.FIELD)
public class DocumentTypeRequest extends GenericArea {

	private int id;
	
	private String description;
	
	private Date timestampUpdate;
	
	private String externalCode;
	
	private boolean ocrRequired;
	
	private boolean pdfaAlreadySigned;
	
	private boolean pdfaConvertNeeded;
	
	private boolean uniqueOriginal;
	
	private String defaultLanguage;
	
	private String docLocationRule;
	
	private String markLocationRule;
	
	private String nodeRefs;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Date getTimestampUpdate() {
		return timestampUpdate;
	}

	public void setTimestampUpdate(Date timestampUpdate) {
		this.timestampUpdate = timestampUpdate;
	}

	public String getExternalCode() {
		return externalCode;
	}

	public void setExternalCode(String externalCode) {
		this.externalCode = externalCode;
	}

	public boolean isOcrRequired() {
		return ocrRequired;
	}

	public void setOcrRequired(boolean ocrRequired) {
		this.ocrRequired = ocrRequired;
	}

	public boolean isPdfaAlreadySigned() {
		return pdfaAlreadySigned;
	}

	public void setPdfaAlreadySigned(boolean pdfaAlreadySigned) {
		this.pdfaAlreadySigned = pdfaAlreadySigned;
	}

	public boolean isPdfaConvertNeeded() {
		return pdfaConvertNeeded;
	}

	public void setPdfaConvertNeeded(boolean pdfaConvertNeeded) {
		this.pdfaConvertNeeded = pdfaConvertNeeded;
	}

	public boolean isUniqueOriginal() {
		return uniqueOriginal;
	}

	public void setUniqueOriginal(boolean uniqueOriginal) {
		this.uniqueOriginal = uniqueOriginal;
	}

	public String getDefaultLanguage() {
		return defaultLanguage;
	}

	public void setDefaultLanguage(String defaultLanguage) {
		this.defaultLanguage = defaultLanguage;
	}

	public String getDocLocationRule() {
		return docLocationRule;
	}

	public void setDocLocationRule(String docLocationRule) {
		this.docLocationRule = docLocationRule;
	}

	public String getMarkLocationRule() {
		return markLocationRule;
	}

	public void setMarkLocationRule(String markLocationRule) {
		this.markLocationRule = markLocationRule;
	}

	public static DocumentTypeRequest fromXML(InputStream is) throws JAXBException {
		return (DocumentTypeRequest)GenericArea.fromXML(is, DocumentTypeRequest.class);
	}

	public static DocumentTypeRequest fromJSON(InputStream is) throws IOException {
		return (DocumentTypeRequest)GenericArea.fromJSON(is, DocumentTypeRequest.class);
	}

	public String getNodeRefs() {
		return nodeRefs;
	}

	public void setNodeRefs(String nodeRefs) {
		this.nodeRefs = nodeRefs;
	}

	public String[] _getNodeRefs() {
		return nodeRefs.split(",");
	}

	public void _setNodeRefs(String[] nodeRefs) {
		StringBuffer sb = new StringBuffer();
		for (String s : nodeRefs) {
			sb.append(s).append(",");
		}
		this.nodeRefs = sb.toString();
		this.nodeRefs = this.nodeRefs.substring(0,this.nodeRefs.length()-1);
	}
	
}
