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
package org.sinekarta.alfresco.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.security.PermissionService;

/**
 * utility class to work with h:datatable for listing nodes for signing process
 * 
 * @author andrea.tessaro
 *
 */
public class NodeRefWrapper implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private NodeRef nodeRef;
	
	private String path;

	private String relativeFileName;

	public static List<NodeRefWrapper> createList(NodeService nodeService, ResultSet rs, String path) {
		List<NodeRefWrapper> ret = new ArrayList<NodeRefWrapper>();
		for (int i=0;i<rs.length();i++) {
			ret.add(new NodeRefWrapper(nodeService, rs.getNodeRef(i), path));
		}
		return ret;
	}

	public static List<NodeRefWrapper> createList(PermissionService permissionService, NodeService nodeService, NodeRef nodeRef) {
		List<NodeRefWrapper> ret = new ArrayList<NodeRefWrapper>();
		ret.add(new NodeRefWrapper(nodeService, nodeRef, nodeService.getPath(nodeRef).toDisplayPath(nodeService, permissionService)));
		return ret;
	}

	public NodeRefWrapper(NodeService nodeService, NodeRef nodeRef, String path) {
		super();
		this.nodeRef = nodeRef;
		this.path = path;
		relativeFileName = NodeTools.translatePath(nodeService, nodeService.getPath(nodeRef)).substring(path.length());
	}
	
	public NodeRef getNodeRef() {
		return nodeRef;
	}

	public void setNodeRef(NodeRef nodeRef) {
		this.nodeRef = nodeRef;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getRelativeFileName() {
		return relativeFileName;
	}

	public void setRelativeFileName(String relativeFileName) {
		this.relativeFileName = relativeFileName;
	}

}
