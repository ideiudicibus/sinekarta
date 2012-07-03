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
package org.sinekarta.ant.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.alfresco.webservice.types.NamedValue;
import org.alfresco.webservice.types.ResultSetRow;

public class ResultSetRowWrapper {

	public static String NAME_COLUMN_NAME = "{http://www.alfresco.org/model/content/1.0}name";
	public static String DESCRIPTION_COLUMN_NAME = "{http://www.alfresco.org/model/content/1.0}description";
	public static String UUID_COLUMN_NAME = "{http://www.alfresco.org/model/system/1.0}node-uuid";
	
	private ResultSetRowWrapper parent;
	private ResultSetRow row;
	private String pathContainer;
	private String name;
	private String description;
	private String uuid;
	
	public ResultSetRowWrapper(ResultSetRow row) {
		super();
		this.row = row;
		for (NamedValue nv:row.getColumns()) {
			if (nv.getName().equals(NAME_COLUMN_NAME)) name=nv.getValue();
			if (nv.getName().equals(DESCRIPTION_COLUMN_NAME)) description=nv.getValue();
			if (nv.getName().equals(UUID_COLUMN_NAME)) uuid=nv.getValue();
		}
		pathContainer = "/";
	}

	public static List<ResultSetRowWrapper> convert(ResultSetRow[] rs, ResultSetRowWrapper parent) {
		ArrayList<ResultSetRowWrapper> ret = new ArrayList<ResultSetRowWrapper>();
		if (rs!=null) {
			for (ResultSetRow row : rs) {
				if (!row.getNode().getType().equals("{http://www.alfresco.org/model/content/1.0}systemfolder")) {
					ResultSetRowWrapper a = new ResultSetRowWrapper(row);
					a.pathContainer = parent.pathContainer + parent.name + "/";
					a.parent=parent;
					ret.add(a);
				}
			}
		}
		Collections.sort(ret, new Comparator<ResultSetRowWrapper>() {
			@Override
			public int compare(ResultSetRowWrapper o1, ResultSetRowWrapper o2) {
				return o1.getName().compareToIgnoreCase(o2.getName());
			}
		});
		return ret;
	}
	
	public ResultSetRow getRow() {
		return row;
	}


	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getUuid() {
		return uuid;
	}

	public ResultSetRowWrapper getParent() {
		return parent;
	}

	public String getPathContainer() {
		return pathContainer;
	}

	@Override
	public String toString() {
		return ("name = " + name + "; description = " + description + "; uuid = " + uuid);
	}

}
