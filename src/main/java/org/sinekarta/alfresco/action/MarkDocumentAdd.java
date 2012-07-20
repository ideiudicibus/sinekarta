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
package org.sinekarta.alfresco.action;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.repo.action.ParameterDefinitionImpl;
import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.sign.area.MarkDocument;
import org.sinekarta.sign.area.MarkDocumentArea;
import org.springframework.beans.factory.InitializingBean;

/**
 * document containing finger print of other documents preparation action.
 * document produced is timestamp mark for document provided in MarkDocumentArea
 * 
 * - parameter markArea : the markDocumentArea returned from MarkFolderprepare
 * 
 * - return the serialize and encoded markDocumentArea with references to timestamp mark document
 * 
 * @author andrea.tessaro
 *
 */
public class MarkDocumentAdd extends ActionExecuterAbstractBase implements InitializingBean {

	private static Logger tracer = Logger.getLogger(MarkDocumentAdd.class);
	public static final String ACTION_NAME_MARK_DOCUMENT_ADD = "sinekartaMarkDocumentAdd";
	public static final String PARAM_MARK_AREA = "markArea";

	@SuppressWarnings("unused")
	private SinekartaDao sinekartaDao;

	@Override
	protected void executeImpl(Action action, NodeRef actionedUponNodeRef) {
		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentAdd action, execution init");
		
		String markArea = (String)action.getParameterValue(PARAM_MARK_AREA);
		if (markArea==null) {
			tracer.error("no markFileName specified for RCS mark prepare.");
			throw new MarkFailedException("no markFileName specified for RCS mark prepare.");
		}
		
		MarkDocumentArea markDocumentArea = MarkDocumentArea.fromBase64String(markArea);
		
		List<MarkDocument> docs = markDocumentArea.getDocuments();
		if (docs==null) {
			docs = new ArrayList<MarkDocument>();
			markDocumentArea.setDocuments(docs);
		}

		MarkDocument doc = new MarkDocument();
		doc.setNodeRefId(actionedUponNodeRef.getId());
		if (!docs.contains(doc)) docs.add(doc);

		action.setParameterValue(PARAM_RESULT, markDocumentArea.toBase64String());

		if (tracer.isDebugEnabled()) tracer.debug("DocumentMarkDocumentAdd action, execution end");
	}
	
	@Override
	protected void addParameterDefinitions(List<ParameterDefinition> paramList) {
		paramList.add(
				new ParameterDefinitionImpl(
				PARAM_MARK_AREA,
				DataTypeDefinition.TEXT, 
				true, // Indicates whether the parameter is mandatory
				getParamDisplayLabel(PARAM_MARK_AREA))); 
	}

    public void afterPropertiesSet() throws Exception {
	}

	public void setSinekartaDao(SinekartaDao sinekartaDao) {
		this.sinekartaDao = sinekartaDao;
	}

}
