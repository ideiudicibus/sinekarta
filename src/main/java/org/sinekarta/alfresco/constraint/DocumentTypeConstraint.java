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
package org.sinekarta.alfresco.constraint;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;

/**
 * constraint for document type
 * This constraint will prepare a list of document types reading the document type table
 * 
 * @author andrea.tessaro
 *
 */
public class DocumentTypeConstraint extends ListOfValuesConstraint {
	
	private static Logger tracer = Logger.getLogger(DocumentTypeConstraint.class);

    private static final String ERR_NON_STRING = "d_dictionary.constraint.string_length.non_string";
    private static final String ERR_INVALID_VALUE = "d_dictionary.constraint.list_of_values.invalid_value";

    // questa e' una porcata (l'attributo static), ma anche quelli di alfresco dicono di fare cosi'...
	// http://blogs.alfresco.com/wp/jbarmash/2008/08/08/dynamic-data-driven-drop-downs-for-list-properties/
	private static SinekartaDao sinekartaDao;

	@Override
	public List<String> getAllowedValues() {
		// allowed values are types read from documenttype table
		List <String> ret = new ArrayList<String>();
		if (sinekartaDao==null) {
			tracer.error("############################### DocumentTypeConstraint DAO IS NULL!!!.");
			return ret;
		}
		List<DocumentType> l = sinekartaDao.getDocumentTypes();
		for (DocumentType t : l) {
			ret.add(Integer.toString(t.getId()));
		}
		return ret;
	}

	@Override
	public boolean isCaseSensitive() {
		// allways case sensitive
		return true;
	}

	@Override
	public void initialize() {
		// eliminata chiamata a padre, verificava la presenza dell'attributo allowed values
	}

    protected void evaluateSingleValue(Object value)
    {
		// convert the value to a String
		String valueStr = null;
		try {
			valueStr = DefaultTypeConverter.INSTANCE.convert(String.class, value);
		} catch (TypeConversionException e) {
			tracer.error(ERR_NON_STRING + value,e);
			throw new ConstraintException(ERR_NON_STRING, value,e);
		}
		try {
			if (sinekartaDao.getDocumentType(Integer.parseInt(valueStr)) == null) {
				tracer.error(ERR_INVALID_VALUE + value);
				throw new ConstraintException(ERR_INVALID_VALUE, value);
			}
		} catch (Exception e) {
			tracer.error(ERR_INVALID_VALUE + value);
			throw new ConstraintException(ERR_INVALID_VALUE, value,e);
		}
    }
    
	@SuppressWarnings("static-access")
	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

}
