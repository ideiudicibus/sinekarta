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
package org.sinekarta.alfresco.webscript;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import net.sf.json.JSONObject;

public abstract class GenericArea {
	
	public void toXML(OutputStream os) throws JAXBException {
		JAXBContext jcupr = JAXBContext.newInstance(this.getClass());
		Marshaller m = jcupr.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
		m.marshal(this, os);
	}
	
	public String toXML() throws JAXBException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		toXML(baos);
		try {
			baos.flush();
			baos.close();
		} catch (IOException e) {
			// how can be possible???
		}
		return new String(baos.toByteArray());
	}
	
	public static GenericArea fromXML(InputStream is, Class<? extends GenericArea> clazz) throws JAXBException {
		JAXBContext jcupr = JAXBContext.newInstance(clazz);
		Unmarshaller u = jcupr.createUnmarshaller();
		return (GenericArea) u.unmarshal(is);
	}

	public void toJSON(OutputStream os) throws IOException {
		JSONObject jsonobj = JSONObject.fromObject(this);
		os.write(jsonobj.toString().getBytes());
		os.flush();
	}

	public String toJSON() throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		toJSON(baos);
		try {
			baos.flush();
			baos.close();
		} catch (IOException e) {
			// how can be possible???
		}
		return new String(baos.toByteArray());
	}

	public static GenericArea fromJSON(InputStream is, Class<? extends GenericArea> clazz) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len = is.read(buf);
		while (len!=-1) {
			baos.write(buf,0,len);
			len = is.read(buf);
		}
		is.close();
		baos.flush();
		baos.close();
		String data = new String(baos.toByteArray());
		JSONObject inputJson=JSONObject.fromObject(data);
		return (GenericArea)JSONObject.toBean(inputJson, clazz);
	}

}
