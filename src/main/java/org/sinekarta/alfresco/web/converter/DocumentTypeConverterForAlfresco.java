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

import java.util.List;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.ConverterException;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.sinekarta.alfresco.web.backing.SinekartaUtility;

/**
 * jsf converter to applay to a h:selectonemenu jsf component to have
 * the selected document type as object
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentTypeConverterForAlfresco implements Converter {

	private static Logger tracer = Logger.getLogger(DocumentTypeConverterForAlfresco.class);
	
	public static final String CONVERTER_ID = "org.sinekarta.alfresco.DocumentTypeConverterForAlfresco";
	
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String stringValue) throws ConverterException {
		if (stringValue==null || stringValue.equals("")) return null;
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		List<SelectItem> ll = su.getTipiDocumentoForAlfresco();
		for (SelectItem si : ll) {
			if (si.getLabel().equals(stringValue)) return si.getValue();
		}
		return null;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object objectValue) throws ConverterException {
		if (objectValue==null || objectValue.equals("")) return "";
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		try {
			return su.getSinekartaDao().getDocumentType(Integer.parseInt((String)objectValue)).getDescription();
		} catch (Exception e) {
			tracer.error("Unable to retrieve documentType",e);
			throw new ConverterException("Unable to retrieve documentType", e);
		}
	}

}
