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

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.security.AccessStatus;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.web.action.evaluator.BaseActionEvaluator;
import org.alfresco.web.bean.repository.Node;
import org.apache.log4j.Logger;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.web.backing.SinekartaUtility;

/**
 * verifying document or folder that can have the sign action wizard enabled
 * 
 * @author andrea.tessaro
 *
 */
public class SinekartaRcsSignPermission extends BaseActionEvaluator {

	private static final long serialVersionUID = 1L;

	// constants
	private static Logger tracer = Logger.getLogger(SinekartaRcsSignPermission.class);

	@Override
	public boolean evaluate(Node node) {
		try {
			// enabled if is a sinekarta archive or if the document has documentAcquiring aspect and does not have rcssignature aspect
			if (node.getType().equals(org.sinekarta.alfresco.model.SinekartaModel.TYPE_QNAME_ARCHIVE) ||
				(node.hasAspect(SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING) && 
				 !node.hasAspect(SinekartaModel.ASPECT_QNAME_RCS_SIGNATURE))) {
				SinekartaUtility su = SinekartaUtility.getCurrentInstance();
				PermissionService permissionService = su.getPermissionService();
				// is the given node a folder?
				if (node.getType().equals(SinekartaModel.TYPE_QNAME_ARCHIVE)) {
					// then check permission of the given node
					if (permissionService.hasPermission(node.getNodeRef(), SinekartaModel.PERMISSION_GROUP_SINEKARTA_RCS).compareTo(AccessStatus.ALLOWED)==0) {
						return true;
					} else {
						return false;
					}
				} else {
					// otherwise check permission for parent (folder) of the given node
					NodeService nodeService = su.getNodeService();
					NodeRef folder = nodeService.getPrimaryParent(node.getNodeRef()).getParentRef();
					if (permissionService.hasPermission(folder, SinekartaModel.PERMISSION_GROUP_SINEKARTA_RCS).compareTo(AccessStatus.ALLOWED)==0) {
						return true;
					} else {
						return false;
					}
				}
			}
			else return false;
		} catch (Throwable t) {
			tracer.warn("Unable calculate SinekartaSignPermission, have you added faces-config-sinekarta.xml in web.xml?",t);
			return false;
		}
	}

}
