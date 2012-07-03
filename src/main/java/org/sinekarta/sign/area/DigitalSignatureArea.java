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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.json.JSONObject;

import org.bouncycastle.util.encoders.Base64;

/**
 * This class is the communication area between sinekarta alfresco action for digital signature and client
 * side sinekarta digital signature
 * smart card access is done in client side using iaik
 * client can also be implemented with an applet 
 * 
 * @author andrea.tessaro
 *
 */
public class DigitalSignatureArea implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private static final String ENCODING = "UTF-8";

	public static final String	FUCTION_CODE_CERTIFICATE_CHOICE= "C";
	public static final String	FUCTION_CODE_SIGNATURE_DIGESTED_CONENT= "S";

	private String function;
	
	private String driver;
	
	private String pin;
	
	private String hexCertificate; 
	
	private String documentPath;
	
	private String documentPathNodeRefId;
	
	private String documentPathStoreRefProtocol;

	private String documentPathStoreRefId;
	
	private List<DigitalSignatureDocument> documents;

	public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}

	public String getPin() {
		return pin;
	}

	public void setPin(String pin) {
		this.pin = pin;
	}

	public String getHexCertificate() {
		return hexCertificate;
	}

	public void setHexCertificate(String hexCertificate) {
		this.hexCertificate = hexCertificate;
	}

	public byte[] hexCertificateToByteArray() {
		return TextUtil.hexTobyte(hexCertificate);
	}

	public void hexCertificateFromByteArray(byte[] certificate) {
		this.hexCertificate = TextUtil.byteToHex(certificate);
	}

	public X509Certificate hexCertificateToX509Certificate() throws CertificateException {
		CertificateFactory cf = CertificateFactory.getInstance("X.509");
		X509Certificate certificate = (X509Certificate) cf.generateCertificate(new ByteArrayInputStream(hexCertificateToByteArray()));
		return certificate;
	}

	public void hexCertificateFromX509Certificate(X509Certificate certificate) throws CertificateEncodingException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		baos.write(certificate.getEncoded());
		baos.close();
		hexCertificateFromByteArray(baos.toByteArray());
	}

	public String getDocumentPath() {
		return documentPath;
	}

	public void setDocumentPath(String documentPath) {
		this.documentPath = documentPath;
	}

	public List<DigitalSignatureDocument> getDocuments() {
		return documents;
	}

	public void setDocuments(List<DigitalSignatureDocument> documents) {
		this.documents = documents;
	}

	public String getDriver() {
		return driver;
	}

	public void setDriver(String driver) {
		this.driver = driver;
	}
	
	/**
	 * serialization of the class using json and base64 encoder
	 * @return
	 */
	public String toBase64String() {
		JSONObject jsonobj = JSONObject.fromObject(this);
		byte[] outputBuffer=null;
		try {
			outputBuffer = jsonobj.toString().getBytes(ENCODING);
		} catch (UnsupportedEncodingException e) {
			// not possible
		}
		return new String(Base64.encode(outputBuffer));
	}

	/**
	 * deserialization of the class using json and base64 decoder
	 * @return
	 */
	@SuppressWarnings("rawtypes")
	public static DigitalSignatureArea fromBase64String(String inputString) {
		JSONObject inputJson=null;
		try {
			inputJson = JSONObject.fromObject(new String(Base64.decode(inputString), ENCODING));
		} catch (UnsupportedEncodingException e) {
			// not possible
		}
		Map<String, Class> classMap = new HashMap<String, Class>();
        classMap.put("documents", DigitalSignatureDocument.class);
		return (DigitalSignatureArea)JSONObject.toBean(inputJson, DigitalSignatureArea.class, classMap);
	}

	public String getDocumentPathNodeRefId() {
		return documentPathNodeRefId;
	}

	public void setDocumentPathNodeRefId(String documentPathNodeRefId) {
		this.documentPathNodeRefId = documentPathNodeRefId;
	}

	public String getDocumentPathStoreRefProtocol() {
		return documentPathStoreRefProtocol;
	}

	public void setDocumentPathStoreRefProtocol(String documentPathStoreRefProtocol) {
		this.documentPathStoreRefProtocol = documentPathStoreRefProtocol;
	}

	public String getDocumentPathStoreRefId() {
		return documentPathStoreRefId;
	}

	public void setDocumentPathStoreRefId(String documentPathStoreRefId) {
		this.documentPathStoreRefId = documentPathStoreRefId;
	}

}
