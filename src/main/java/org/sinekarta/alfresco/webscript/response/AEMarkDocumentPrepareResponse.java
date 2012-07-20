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
package org.sinekarta.alfresco.webscript.response;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.sinekarta.alfresco.webscript.GenericArea;

@XmlRootElement(name = "sinekarta")
@XmlAccessorType(XmlAccessType.FIELD)
public class AEMarkDocumentPrepareResponse extends GenericArea{

	private String markDocumentArea;
	
	private String result;

	public static AEMarkDocumentPrepareResponse fromXML(InputStream is) throws JAXBException {
		return (AEMarkDocumentPrepareResponse)GenericArea.fromXML(is, AEMarkDocumentPrepareResponse.class);
	}

	public static AEMarkDocumentPrepareResponse fromJSON(InputStream is) throws IOException {
		return (AEMarkDocumentPrepareResponse)GenericArea.fromJSON(is, AEMarkDocumentPrepareResponse.class);
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getMarkDocumentArea() {
		return markDocumentArea;
	}

	public void setMarkDocumentArea(String markDocumentArea) {
		this.markDocumentArea = markDocumentArea;
	}


}
