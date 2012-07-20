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
package org.sinekarta.alfresco.webscript.response;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.sinekarta.alfresco.webscript.GenericArea;

@XmlRootElement(name = "sinekarta")
@XmlAccessorType(XmlAccessType.FIELD)
public class SmartCardDriverResponse extends GenericArea {
	
	private String userDefaultDriver;
	
	private List<SmartCardDriver> smartCardDrivers;

	public String getUserDefaultDriver() {
		return userDefaultDriver;
	}

	public void setUserDefaultDriver(String userDefaultDriver) {
		this.userDefaultDriver = userDefaultDriver;
	}

	public List<SmartCardDriver> getSmartCardDrivers() {
		return smartCardDrivers;
	}

	public void setSmartCardDrivers(List<SmartCardDriver> smartCardDrivers) {
		this.smartCardDrivers = smartCardDrivers;
	}

}
