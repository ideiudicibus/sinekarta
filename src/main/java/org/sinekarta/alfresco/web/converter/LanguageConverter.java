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
import org.sinekarta.alfresco.util.Configuration;

/**
 * jsf converter to applay to a h:selectonemenu jsf component to have
 * the selected document type as object
 * 
 * @author andrea.tessaro
 *
 */
public class LanguageConverter implements Converter {

	public static final String CONVERTER_ID = "org.sinekarta.alfresco.LanguageConverter";
	
	@SuppressWarnings("unused")
	private static Logger tracer = Logger.getLogger(LanguageConverter.class);
	
	@Override
	public Object getAsObject(FacesContext context, UIComponent component, String stringValue) throws ConverterException {
		if (Configuration.getInstance().getMappaInversaLingueOcr().get(stringValue)!=null) return Configuration.getInstance().getMappaInversaLingueOcr().get(stringValue);
		else return stringValue;
	}

	@Override
	public String getAsString(FacesContext context, UIComponent component, Object objectValue) throws ConverterException {
		return Configuration.getInstance().getMappaLingueOcr().get((String)objectValue);
	}

}
