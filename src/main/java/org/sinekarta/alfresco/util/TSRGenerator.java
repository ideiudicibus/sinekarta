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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.Security;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.util.encoders.Base64;

/**
 * @author Tessaro Porta Andrea
 */
public class TSRGenerator {
	
	private static final Logger tracer = Logger.getLogger(TSRGenerator.class);

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	private String url;
	private String user;
	private String password;

	public TSRGenerator(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
	}

	public TSRGenerator(String url) {
		this.url = url;
	}

	public byte[] doRequest(TimeStampRequest request) throws Exception {

		if (tracer.isDebugEnabled()) tracer.debug("TSRGenerator.doRequest ");

		byte[] requestBytes = request.getEncoded();

		if (tracer.isDebugEnabled()) tracer.debug("TSRGenerator.doRequest request : " + new String(requestBytes));

		byte[] respBytes = getTSAResponse(requestBytes);

		if (tracer.isDebugEnabled()) tracer.debug("TSRGenerator.doRequest response : " + new String(respBytes));

		return respBytes;

	}

	/**
	 * Get timestamp token - communications layer
	 * 
	 * @return - byte[] - TSA response, raw bytes (RFC 3161 encoded)
	 */
	@SuppressWarnings("deprecation")
	private byte[] getTSAResponse(byte[] requestBytes) throws Exception {
		PostMethod method = new PostMethod(url);
		ByteArrayInputStream bais = new ByteArrayInputStream(requestBytes);
		method.setRequestBody(bais);
		method.setRequestContentLength(requestBytes.length);
		method.setRequestHeader("Content-type", "application/timestamp-query");
		if ((user != null) && !user.equals("")) {
			String userPassword = user + ":" + password;
			method.setRequestHeader("Authorization", "Basic " + new String(Base64.encode(userPassword.getBytes())));
		}

		HttpClient http_client = new HttpClient();
		http_client.executeMethod(method);
		InputStream in = method.getResponseBodyAsStream();
		
		byte[] respBuf = Util.inputStreamToByteArray(in);
		tracer.debug(new String(respBuf));

		return respBuf;
	}
}
