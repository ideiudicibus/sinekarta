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
package org.sinekarta.alfresco.behavior;

import java.io.Serializable;
import java.util.Map;

import org.alfresco.repo.content.ContentServicePolicies;
import org.alfresco.repo.node.NodeServicePolicies;
import org.alfresco.repo.policy.Behaviour;
import org.alfresco.repo.policy.Behaviour.NotificationFrequency;
import org.alfresco.repo.policy.JavaBehaviour;
import org.alfresco.repo.policy.PolicyComponent;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.action.DocumentOrganize;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;

/**
 * if someone changes the documentType, this behavior will move the archived document into 
 * the right archive subfolder
 * 
 * @author andrea.tessaro
 *
 */
public class ArchiveOrganizator implements 	NodeServicePolicies.OnUpdatePropertiesPolicy,
											NodeServicePolicies.OnMoveNodePolicy,
											NodeServicePolicies.BeforeDeleteNodePolicy,
											ContentServicePolicies.OnContentUpdatePolicy {

	// constants
	private static Logger tracer = Logger.getLogger(ArchiveOrganizator.class);

	// working properties
	private Behaviour onUpdateProperties;
	private Behaviour beforeDeleteNode;
	private Behaviour onContentUpdate;
	private Behaviour onMoveNode;
	private PolicyComponent policyComponent;
	
	
	// services, added via spring
	private ActionService actionService;

	public void init() {
		// registering this behaviour
		if (tracer.isDebugEnabled()) tracer.debug("Initializing sinekarta ArchiveOrganizator behaviour");
		this.onUpdateProperties = new JavaBehaviour(this, "onUpdateProperties", NotificationFrequency.EVERY_EVENT);
		this.policyComponent.bindClassBehaviour(QName.createQName( NamespaceService.ALFRESCO_URI, "onUpdateProperties"), SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING, this.onUpdateProperties);
		this.beforeDeleteNode = new JavaBehaviour(this, "beforeDeleteNode", NotificationFrequency.EVERY_EVENT);
		this.policyComponent.bindClassBehaviour(QName.createQName( NamespaceService.ALFRESCO_URI, "beforeDeleteNode"), SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING, this.beforeDeleteNode);
		this.onContentUpdate = new JavaBehaviour(this, "onContentUpdate", NotificationFrequency.EVERY_EVENT);
		this.policyComponent.bindClassBehaviour(QName.createQName( NamespaceService.ALFRESCO_URI, "onContentUpdate"), SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING, this.onContentUpdate);
		this.onMoveNode = new JavaBehaviour(this, "onMoveNode", NotificationFrequency.EVERY_EVENT);
		this.policyComponent.bindClassBehaviour(QName.createQName( NamespaceService.ALFRESCO_URI, "onMoveNode"), SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING, this.onMoveNode);
		if (tracer.isDebugEnabled()) tracer.debug("Initializing sinekarta ArchiveOrganizator behaviour OK");
	}

	@Override
	public void onMoveNode(ChildAssociationRef oldChildAssocRef, ChildAssociationRef newChildAssocRef) {
		// if the document is marked then can not be moved
		// if the document is a timestamp mark then can not be moved
		// 
		// this methods remain empty by now.
		// we should understand if it is the best choice to do this by software and not by user configuration
	}

	@Override
	public void onContentUpdate(NodeRef nodeRef, boolean newContent) {
		// if the document is signed can not be modified... not at all, can be signed by the PU???
		// 
		// this methods remain empty by now.
		// we should understand if it is the best choice to do this by software and not by user configuration
	}

	@Override
	public void beforeDeleteNode(NodeRef nodeRef) {
		// if the document is marked then can not be deleted 
		// if the document is a timestamp mark then can not be deleted
		// if the document is signed by the PU then can not be deleted
		// 
		// this methods remain empty by now.
		// we should understand if it is the best choice to do this by software and not by user configuration
	}

	@Override
	public void onUpdateProperties(NodeRef nodeRef, Map<QName, Serializable> before, Map<QName, Serializable> after) {
		if (tracer.isDebugEnabled()) tracer.debug("ArchiveOrganizator.behavior : aspect documentacquiring modified!");

		String beforeDocumentType = (String)before.get(SinekartaModel.PROP_QNAME_DOCUMENT_TYPE);
		String afterDocumentType = (String)after.get(SinekartaModel.PROP_QNAME_DOCUMENT_TYPE);
		Boolean afterDocumentMoved = (Boolean)after.get(SinekartaModel.PROP_QNAME_DOCUMENT_MOVED);
		
		// was the document moved from another space ? 
		// if moved from another space, organization is done manually
		if (afterDocumentMoved==null || !afterDocumentMoved) { 
			// documentType was modified?
			if ((beforeDocumentType==null  && afterDocumentType!=null) || (beforeDocumentType!=null && !beforeDocumentType.equals(afterDocumentType))) {
				if (tracer.isDebugEnabled()) tracer.debug("ArchiveOrganizator.behavior : property document type of aspect documentacquiring is modified!");
	
				// invoking archive organization action
				Action documentOrganize = actionService.createAction(DocumentOrganize.ACTION_NAME_DOCUMENT_ORGANIZE);
				try {
					actionService.executeAction(documentOrganize, nodeRef, false, false);
				} catch(Throwable t) {
					throw new SignFailedException("Unable to reorganize SineKarta archive",t);
				}
				if (tracer.isDebugEnabled()) tracer.debug("ArchiveOrganizator.behavior : reorganizig sinekarta archive ended!");
			}
		}
	}
	
	
	public void setPolicyComponent(PolicyComponent policyComponent) {
		this.policyComponent = policyComponent;
	}

	public void setActionService(ActionService actionService) {
		this.actionService = actionService;
	}

}