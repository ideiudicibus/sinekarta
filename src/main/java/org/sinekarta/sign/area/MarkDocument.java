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
package org.sinekarta.sign.area;

import java.io.Serializable;

public class MarkDocument implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String nodeRefId;
	
	public String getNodeRefId() {
		return nodeRefId;
	}

	public void setNodeRefId(String nodeRefId) {
		this.nodeRefId = nodeRefId;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof MarkDocument)
			return nodeRefId.equals(((MarkDocument)obj).nodeRefId);
		else
			return false;
	}

}
