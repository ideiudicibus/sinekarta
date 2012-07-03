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

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.sinekarta.alfresco.webscript.GenericArea;

@XmlRootElement(name = "sinekarta")
@XmlAccessorType(XmlAccessType.FIELD)
public class DocumentToPDFARequest extends GenericArea{

	private String nodeRefs;

	public static DocumentToPDFARequest fromXML(InputStream is) throws JAXBException {
		return (DocumentToPDFARequest)GenericArea.fromXML(is, DocumentToPDFARequest.class);
	}

	public static DocumentToPDFARequest fromJSON(InputStream is) throws IOException {
		return (DocumentToPDFARequest)GenericArea.fromJSON(is, DocumentToPDFARequest.class);
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
