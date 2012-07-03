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
package org.sinekarta.alfresco.web.converter;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;

import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.web.backing.SinekartaUtility;

/**
 * jsf converter to applay to a h:selectonemenu jsf component to have
 * the selected document type as object
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentTypeConverter implements Converter {

	private static Logger tracer = Logger.getLogger(DocumentTypeConverter.class);
	
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String stringValue) throws ConverterException {
		if (stringValue.equals("_")) return null;
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		try {
			return su.getSinekartaDao().getDocumentType(Integer.parseInt(stringValue));
		} catch (Exception e) {
			tracer.error("Unable to retrieve documentType",e);
			throw new ConverterException("Unable to retrieve documentType", e);
		}
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object objectValue) throws ConverterException {
		if (objectValue==null) return "_";
		if (objectValue instanceof String && ((String)objectValue).equals("_")) return "_";
		if (!(objectValue instanceof DocumentType)) throw new ConverterException("Invalid object received for DocumentTypeConverter");
		return Integer.toString(((DocumentType)objectValue).getId());
	}

}
