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
package org.sinekarta.alfresco.web.backing;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.util.NodeRefWrapper;

/**
 * signing a single document is a particular type of multi-document signing
 * 
 * @author andrea.tessaro
 *
 */
public class PUSingleSignDocumentsWizard extends PUSignDocumentsWizard {
	
	private static final long serialVersionUID = 1L;

	@SuppressWarnings("unused")
	private static Logger tracer = Logger.getLogger(PUSingleSignDocumentsWizard.class);

	protected static final String BUNDLE_SINGLE_SIGN_TITLE = "singleSign_title";

	private String documentName;

	/** 
	 * on init i create the selectedDocument list containing only selected document
	 */
	private void init() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();

		documentName = (String) su.getNodeService().getProperty(browseBean.getActionSpace().getNodeRef(), ContentModel.PROP_NAME);

		List<NodeRefWrapper> l = NodeRefWrapper.createList(su.getPermissionService(), su.getNodeService(), browseBean.getActionSpace().getNodeRef());
		setSelectedDocuments(l);
		setDocuments(l);
		
		// preparazione area di input per applet

		prepareDataToAppletCertificateChoice();

		su.setDataFromAppletCertificateChoice(null);

	}
	
	@Override
	public void init(Map<String, String> parameters) {
		super.init(parameters);
		init();
	}

	public String reset() {
		super.reset();
		init();
		return null;
	}
	
	@Override
	public String getContainerTitle() {
		Object[] args = new Object[] {documentName};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_SINGLE_SIGN_TITLE), args);
	}
	
}
