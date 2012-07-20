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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.sinekarta.alfresco.webscript.GenericArea;

@XmlRootElement(name = "sinekarta")
@XmlAccessorType(XmlAccessType.FIELD)
public class MarkDocumentInitRequest extends GenericArea{

	private String documentTypeId;
	
	private String documentDate;

	private String nodeRef;

	public static MarkDocumentInitRequest fromXML(InputStream is) throws JAXBException {
		return (MarkDocumentInitRequest)GenericArea.fromXML(is, MarkDocumentInitRequest.class);
	}

	public static MarkDocumentInitRequest fromJSON(InputStream is) throws IOException {
		return (MarkDocumentInitRequest)GenericArea.fromJSON(is, MarkDocumentInitRequest.class);
	}

	public String getDocumentTypeId() {
		return documentTypeId;
	}

	public void setDocumentTypeId(String documentTypeId) {
		this.documentTypeId = documentTypeId;
	}

	public int _getDocumentTypeId() {
		return Integer.parseInt(documentTypeId);
	}

	public void _setDocumentTypeId(int documentTypeId) {
		this.documentTypeId = Integer.toString(documentTypeId);
	}

	public String getDocumentDate() {
		return documentDate;
	}

	public void setDocumentDate(String documentDate) {
		this.documentDate = documentDate;
	}

	public Date _getDocumentDate() throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		return sdf.parse(documentDate);
	}

	public void _setDocumentDate(Date documentDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		this.documentDate = sdf.format(documentDate);
	}

	public String getNodeRef() {
		return nodeRef;
	}

	public void setNodeRef(String nodeRef) {
		this.nodeRef = nodeRef;
	}

}
