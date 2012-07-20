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
import java.util.Collections;
import java.util.List;

import org.alfresco.repo.dictionary.constraint.ListOfValuesConstraint;
import org.alfresco.service.cmr.dictionary.ConstraintException;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.TypeConversionException;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.util.Configuration;

/**
 * constraint for OCR language
 * This constraint will prepare a list of languages reading the sinekarta-repository.properties
 * 
 * @author andrea.tessaro
 *
 */
public class LanguageConstraint extends ListOfValuesConstraint {
	
	private static Logger tracer = Logger.getLogger(LanguageConstraint.class);

    private static final String ERR_NON_STRING = "d_dictionary.constraint.string_length.non_string";
    private static final String ERR_INVALID_VALUE = "d_dictionary.constraint.list_of_values.invalid_value";

    @Override
	public List<String> getAllowedValues() {
    	// allowed values are the values read in configuration file
		ArrayList<String> ret = new ArrayList<String>();
		Collections.addAll(ret, Configuration.getInstance().getElencoLingueOcr());
		return ret;
	}

	@Override
	public boolean isCaseSensitive() {
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
		if (!Configuration.getInstance().getMappaLingueOcr().containsKey(valueStr)) {
			tracer.error(ERR_INVALID_VALUE + value);
			throw new ConstraintException(ERR_INVALID_VALUE, value);
		}
    }
}
