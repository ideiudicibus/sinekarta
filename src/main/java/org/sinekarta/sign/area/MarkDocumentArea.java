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
package org.sinekarta.sign.area;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.bouncycastle.util.encoders.Base64;

/**
 * This class is the communication area between sinekarta alfresco action for mark document and client
 * side sinekarta mark document creation
 * 
 * @author andrea.tessaro
 *
 */
public class MarkDocumentArea implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private static final String ENCODING = "UTF-8";

	private String markDocumentType;

	private String markFolderPath;
	
	private String markFolderPathNodeRefId;
	
	private String markFolderPathStoreRefProtocol;

	private String markFolderPathStoreRefId;
	
	private String tmpMarkFolderPath;
	
	private String tmpMarkFolderPathNodeRefId;
	
	private String tmpMarkFolderPathStoreRefProtocol;

	private String tmpMarkFolderPathStoreRefId;
	
	private String markFileName;
	
	private String markDescription;
	
	private String markDocumentNodeRefId;

	private String tmpMarkDocumentNodeRefId;

	private List<MarkDocument> documents;

	public String getMarkFolderPath() {
		return markFolderPath;
	}

	public void setMarkFolderPath(String markFolderPath) {
		this.markFolderPath = markFolderPath;
	}

	public List<MarkDocument> getDocuments() {
		return documents;
	}

	public void setDocuments(List<MarkDocument> documents) {
		this.documents = documents;
	}

	public String getMarkFolderPathNodeRefId() {
		return markFolderPathNodeRefId;
	}

	public void setMarkFolderPathNodeRefId(String markFolderPathNodeRefId) {
		this.markFolderPathNodeRefId = markFolderPathNodeRefId;
	}

	public String getMarkFolderPathStoreRefProtocol() {
		return markFolderPathStoreRefProtocol;
	}

	public void setMarkFolderPathStoreRefProtocol(String markFolderPathStoreRefProtocol) {
		this.markFolderPathStoreRefProtocol = markFolderPathStoreRefProtocol;
	}

	public String getMarkFolderPathStoreRefId() {
		return markFolderPathStoreRefId;
	}

	public void setMarkFolderPathStoreRefId(String markFolderPathStoreRefId) {
		this.markFolderPathStoreRefId = markFolderPathStoreRefId;
	}

	public String getMarkFileName() {
		return markFileName;
	}

	public void setMarkFileName(String markFileName) {
		this.markFileName = markFileName;
	}

	public String getMarkDescription() {
		return markDescription;
	}

	public void setMarkDescription(String markDescription) {
		this.markDescription = markDescription;
	}

	public String getMarkDocumentNodeRefId() {
		return markDocumentNodeRefId;
	}

	public void setMarkDocumentNodeRefId(String markDocumentNodeRefId) {
		this.markDocumentNodeRefId = markDocumentNodeRefId;
	}

	public String getTmpMarkFolderPath() {
		return tmpMarkFolderPath;
	}

	public void setTmpMarkFolderPath(String tmpMarkFolderPath) {
		this.tmpMarkFolderPath = tmpMarkFolderPath;
	}

	public String getTmpMarkFolderPathNodeRefId() {
		return tmpMarkFolderPathNodeRefId;
	}

	public void setTmpMarkFolderPathNodeRefId(String tmpMarkFolderPathNodeRefId) {
		this.tmpMarkFolderPathNodeRefId = tmpMarkFolderPathNodeRefId;
	}

	public String getTmpMarkFolderPathStoreRefProtocol() {
		return tmpMarkFolderPathStoreRefProtocol;
	}

	public void setTmpMarkFolderPathStoreRefProtocol(
			String tmpMarkFolderPathStoreRefProtocol) {
		this.tmpMarkFolderPathStoreRefProtocol = tmpMarkFolderPathStoreRefProtocol;
	}

	public String getTmpMarkFolderPathStoreRefId() {
		return tmpMarkFolderPathStoreRefId;
	}

	public void setTmpMarkFolderPathStoreRefId(String tmpMarkFolderPathStoreRefId) {
		this.tmpMarkFolderPathStoreRefId = tmpMarkFolderPathStoreRefId;
	}

	public String getTmpMarkDocumentNodeRefId() {
		return tmpMarkDocumentNodeRefId;
	}

	public void setTmpMarkDocumentNodeRefId(String tmpMarkDocumentNodeRefId) {
		this.tmpMarkDocumentNodeRefId = tmpMarkDocumentNodeRefId;
	}

	/**
	 * serialization of the class using json and base64 encoder
	 * @return
	 */
	public String toBase64String() {
		JSONObject jsonobj = JSONObject.fromObject(this);
		byte[] outputBuffer=null;
		try {
			outputBuffer = jsonobj.toString().getBytes(ENCODING);
		} catch (UnsupportedEncodingException e) {
			// not possible
		}
		return new String(Base64.encode(outputBuffer));
	}

	/**
	 * deserialization of the class using json and base64 decoder
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static MarkDocumentArea fromBase64String(String inputString) {
		JSONObject inputJson=null;
		try {
			inputJson = JSONObject.fromObject(new String(Base64.decode(inputString), ENCODING));
		} catch (UnsupportedEncodingException e) {
			// not possible
		}
		Map<String, Class> classMap = new HashMap<String, Class>();
        classMap.put("documents", MarkDocument.class);
		return (MarkDocumentArea)JSONObject.toBean(inputJson, MarkDocumentArea.class, classMap);
	}

	public String getMarkDocumentType() {
		return markDocumentType;
	}

	public void setMarkDocumentType(String markDocumentType) {
		this.markDocumentType = markDocumentType;
	}

}
