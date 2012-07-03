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
package org.sinekarta.alfresco.web.action.evaluator;

import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.model.SinekartaModel;

/**
 * verifying document or folder that can have the configuration action enabled
 * 
 * @author andrea.tessaro
 *
 */
public class SinekartaSearchPermission extends BaseActionEvaluator {

	private static final long serialVersionUID = 1L;

	// constants
	private static Logger tracer = Logger.getLogger(SinekartaSearchPermission.class);

	@Override
	public boolean evaluate(Node node) {
		try {
			// is the given node a sinekarta archive?
			if (node.getType().equals(SinekartaModel.TYPE_QNAME_ARCHIVE)) {
				return true;
			} else { 
				return false;
			}
		} catch (Throwable t) {
			tracer.warn("Unable calculate SinekartaConfigurationPermission, have you added faces-config-sinekarta.xml in web.xml?",t);
			return false;
		}
	}

}
