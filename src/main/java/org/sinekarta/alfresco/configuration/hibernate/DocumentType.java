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
package org.sinekarta.alfresco.configuration.hibernate;

import java.io.Serializable;
import java.sql.Timestamp;

import org.apache.log4j.Logger;

/**
 * java class to manage documentType table
 * this is a pojo managed via hibernate
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentType implements Cloneable, Serializable {

	private static final long serialVersionUID = 1L;

	private static Logger tracer = Logger.getLogger(DocumentType.class);
	
	private int id; 
	private String description; 
	private Timestamp timestampUpdate;
	private String externalCode;
	private boolean ocrRequired;
	private boolean pdfaAlreadySigned;
	private boolean pdfaConvertNeeded;
	private boolean uniqueOriginal;
	private String defaultLanguage;
	/**
	 * la regola definita deve restituire un percorso assoluto che sara' accodato al percorso dell'archivio in formato lucene, sara' quindi qualcosa tipo :
	 * Ricordarsi che in alfresco gli space non possono iniziare con un numero 
	 * '/cm:Anno 'yyyy'/cm:fatture/cm:settimana_'ww
	 * che il giorno 11/08/2010 restituira' 
	 * /cm:Anno 2010/cm:fatture/cm:settimana_32
	 */
	private String docLocationRule;
	private String markLocationRule;
	
	public Object clone() {
		
		DocumentType object;
		try {
			object = (DocumentType)super.clone();
			return object;
		} catch (Exception e) {
			tracer.error("unable to clone DocumentType : " + e.getClass().getName() + " : " + e.getMessage());
			throw new RuntimeException("unable to clone DocumentType : " + e.getClass().getName() + " : " + e.getMessage(), e);
		}
	}
	
	public boolean equals(Object object){
		if (object instanceof DocumentType) {
			return ((DocumentType) object).getId()==this.id;
		}
		return false;
	}

	public Timestamp getTimestampUpdate() {
		return timestampUpdate;
	}

	public void setTimestampUpdate(Timestamp timestampUpdate) {
		this.timestampUpdate = timestampUpdate;
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

	public String getExternalCode() {
		return externalCode;
	}

	public void setExternalCode(String externalCode) {
		this.externalCode = externalCode;
	}

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

	public boolean isPdfaConvertNeeded() {
		return pdfaConvertNeeded;
	}

	public void setPdfaConvertNeeded(boolean pdfaConvertNeeded) {
		this.pdfaConvertNeeded = pdfaConvertNeeded;
	}

}
