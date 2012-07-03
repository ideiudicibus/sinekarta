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



public class DigitalSignatureDocument implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	private String fileName;
	
	private String nodeRefId;
	
	private String fingerPrint; 
	
	private String digitalSignature; 

	private String description; 

	private String location; 

	private String signDate;

	private String modDateUnicodeString;

	private String fileID_0_byteContent;

	private String fileID_1_byteContent;
	
	private String encodedTimeStampToken;
	
	private String signedAttributesEncoded;
	
	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getFingerPrint() {
		return fingerPrint;
	}

	public void setFingerPrint(String fingerPrint) {
		this.fingerPrint = fingerPrint;
	}

	public  byte[] fingerPrintToByteArray() {
		return TextUtil.hexTobyte(fingerPrint);
	}

	public void fingerPrintFromByteArray(byte[] fingerPrint) {
		this.fingerPrint = TextUtil.byteToHex(fingerPrint);
	}

	public String getDigitalSignature() {
		return digitalSignature;
	}

	public void setDigitalSignature(String digitalSignature) {
		this.digitalSignature = digitalSignature;
	}

	public byte[] digitalSignatureToByteArray() {
		return TextUtil.hexTobyte(digitalSignature);
	}

	public void digitalSignatureFromByteArray(byte[] digitalSignature) {
		this.digitalSignature = TextUtil.byteToHex(digitalSignature);
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSignDate() {
		return signDate;
	}

	public void setSignDate(String signDate) {
		this.signDate = signDate;
	}

	public String getModDateUnicodeString() {
		return modDateUnicodeString;
	}

	public void setModDateUnicodeString(String modDateUnicodeString) {
		this.modDateUnicodeString = modDateUnicodeString;
	}

	public String getFileID_0_byteContent() {
		return fileID_0_byteContent;
	}

	public void setFileID_0_byteContent(String fileID_0_byteContent) {
		this.fileID_0_byteContent = fileID_0_byteContent;
	}

	public String getFileID_1_byteContent() {
		return fileID_1_byteContent;
	}

	public void setFileID_1_byteContent(String fileID_1_byteContent) {
		this.fileID_1_byteContent = fileID_1_byteContent;
	}

	public String getEncodedTimeStampToken() {
		return encodedTimeStampToken;
	}

	public void setEncodedTimeStampToken(String encodedTimeStampToken) {
		this.encodedTimeStampToken = encodedTimeStampToken;
	}

	public String getSignedAttributesEncoded() {
		return signedAttributesEncoded;
	}

	public void setSignedAttributesEncoded(String signedAttributesEncoded) {
		this.signedAttributesEncoded = signedAttributesEncoded;
	}

	public String getNodeRefId() {
		return nodeRefId;
	}

	public void setNodeRefId(String nodeRefId) {
		this.nodeRefId = nodeRefId;
	}

}
