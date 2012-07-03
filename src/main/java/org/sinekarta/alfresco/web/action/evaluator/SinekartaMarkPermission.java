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
 * verifying document or folder that can have the mark action wizard enabled
 * 
 * @author andrea.tessaro
 *
 */
public class SinekartaMarkPermission extends BaseActionEvaluator {

	private static final long serialVersionUID = 1L;

	// constants
	private static Logger tracer = Logger.getLogger(SinekartaMarkPermission.class);

	@Override
	public boolean evaluate(Node node) {
		try {
			// passano le folder archivio oppure i documenti : 
			// che hanno l'aspect document_acquiring (quindi e' stato trasformato in pdf/a
			// che hanno l'aspect RCS_signature (quindi e' stato firmato dall'RCS)
			// che NON hanno l'aspect substitutive_preservation (non gli e' ancora stata applicata la marca temporale)
			// che NON hanno la proprieta' PU_sign_required (NON e' un originale unico, NON necessita della firma del PU)
			// oppure che hanno la proprieta' PU_sign_required (e' un originale unico, necessita della firma del PU)
			// e la firma del PU e' stata applicata
			if (node.getType().equals(org.sinekarta.alfresco.model.SinekartaModel.TYPE_QNAME_ARCHIVE) ||
				(node.hasAspect(SinekartaModel.ASPECT_QNAME_DOCUMENT_ACQUIRING) && 
				 node.hasAspect(SinekartaModel.ASPECT_QNAME_RCS_SIGNATURE) &&
				 !node.hasAspect(SinekartaModel.ASPECT_QNAME_SUBSTITUTIVE_PRESERVATION) &&
				 (
				  !(Boolean)node.getProperties().get(SinekartaModel.PROP_QNAME_PU_SIGN_REQUIRED) ||
				  (
					(Boolean)node.getProperties().get(SinekartaModel.PROP_QNAME_PU_SIGN_REQUIRED) &&	 
					node.hasAspect(SinekartaModel.ASPECT_QNAME_PU_SIGNATURE)
				  )
				 )
				)) {
				// in questo caso si puo' applicare la marca temporale
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
			} else { 
				return false;
			}
		} catch (Throwable t) {
			tracer.warn("Unable calculate SinekartaMarkPermission, have you added faces-config-sinekarta.xml in web.xml?",t);
			return false;
		}
	}

}
