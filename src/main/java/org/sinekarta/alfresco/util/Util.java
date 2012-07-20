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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Various utility method
 * 
 * @author Tessaro Porta Andrea
 *
 */
public class Util {
	
	private static final String BC = "BC";
	private static final String SHA_1 = "SHA-1";
	private static final String SHA_256 = "SHA-256";

	static {
		Security.addProvider(new BouncyCastleProvider());
	}

	public static final char LEFT_FILLER = 'L';
	public static final char RIGHT_FILLER = 'R';

	/**
	 * filling a string to a length with a specified char
	 * if input string is longer than the required len, result will be trunked
	 * @param in
	 * @param leftRight
	 * @param filler
	 * @param len
	 * @return filled (or trunked string
	 */
	public static String FILL(String in, char leftRight, char filler, int len) {
		StringBuffer out = new StringBuffer();
		int lenout = (in!=null?in.length():0);
		if (lenout > len) lenout=len;
		if (leftRight == RIGHT_FILLER) {
			if (in!=null && in.length()>len)
				in=in.substring(0, len);
			if (in != null)
				out.append(in);
			for (int i = 0; i < len - lenout; i++) 
				out.append(filler);
		} else if (leftRight == LEFT_FILLER) { 
			for (int i = 0; i < len - lenout; i++) 
				out.append(filler);
			if (in!=null && in.length()>len)
				in=in.substring(in.length() - len);
			if (in != null)
				out.append(in);
		}
		return out.toString();
	}

	/**
	 * converting a byte[] to it's hexadecimal format String
	 * 
	 * @param buf the byte to convert
	 * @return the hex String corresponding to buf
	 */
	public static String byteToHex(byte[] buf) {
		if (buf == null)
			return null;
		return byteToHex(buf, 0, buf.length);
	}

	/**
	 * converting a byte[] to it's hexadecimal format String
	 * 
	 * @param buf the byte to convert
	 * @offset the offset to star with conversion
	 * @len how many byte to convert
	 * @return the hex String corresponding to buf
	 */
	public static String byteToHex(byte[] buf, int offset, int len) {
		if (buf == null)
			return null;
		StringBuffer ret = new StringBuffer();
		long tmpL = 0;
		String tmp;
		for (int i = 0; i < len / 8; i++) {
			for (int k = 0; k < 8; k++) {
				tmpL = tmpL << 8;
				tmpL = tmpL | (0xff & buf[(i * 8) + k + offset]);
			}
			tmp = Long.toHexString(tmpL);
			for (int j = 0; j < 16 - tmp.length(); j++) {
				ret.append('0');
			}
			ret.append(tmp);
			tmpL = 0;
		}
		int mod = len % 8;
		if (mod != 0) {
			for (int k = len - mod; k < len; k++) {
				tmpL = tmpL << 8;
				tmpL = tmpL | (0xff & buf[k + offset]);
			}
			tmp = Long.toHexString(tmpL);
			for (int j = 0; j < (mod * 2) - tmp.length(); j++) {
				ret.append('0');
			}
			ret.append(tmp);
		}
		return ret.toString().toUpperCase();
	}

	/**
	 * converting a hexadecimal format String to corresponding byte[]
	 * @param buf the hexadecimal format String
	 * @return corresponding byte[]
	 */
	public static byte[] hexTobyte(String buf) {
		if (buf == null)
			return null;
		byte[] ret = new byte[buf.length() / 2];
		for (int i = 0; i < buf.length(); i = i + 2) {
			ret[i / 2] = (byte) Integer.parseInt(buf.substring(i, i + 2), 16);
		}
		return ret;
	}

	/**
	 * calculate fingerprint of a given byte[] using SHA-1 agorithm
	 * @param buffer the buffer on which calculate the fingerprint
	 * @return finger print
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	@Deprecated
	public static byte[] digest(byte[] buffer) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		return digest(SHA_1, buffer);
	}

	/**
	 * calculate fingerprint of a given byte[] using SHA-256 agorithm
	 * @param buffer the buffer on which calculate the fingerprint
	 * @return finger print
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	public static byte[] digest256(byte[] buffer) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		return digest(SHA_256, buffer);
	}

	/**
	 * calculate fingerprint of a given byte[] using SHA-256 agorithm
	 * @param buffer the buffer on which calculate the fingerprint
	 * @return finger print
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	public static byte[] digest(String algorithm, byte[] buffer) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		MessageDigest md =  MessageDigest.getInstance(algorithm, BC);
		md.update(buffer, 0, buffer.length);
		return md.digest();
	}

	/**
	 * calculate fingerprint of a given InputStream using SHA-1 agorithm
	 * 
	 * @param is the inputStream on which calculate the fingerprint
	 * @return finger print
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	@Deprecated
	public static byte[] digest(InputStream is) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		return digest(SHA_1, is);
	}
	
	/**
	 * calculate fingerprint of a given InputStream using SHA-256 agorithm
	 * 
	 * @param is the inputStream on which calculate the fingerprint
	 * @return finger print
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	public static byte[] digest256(InputStream is) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		return digest(SHA_256, is);
	}
	
	/**
	 * calculate fingerprint of a given InputStream using given agorithm
	 * 
	 * @param algorithm the algorithm to use
	 * @param is the inputStream on which calculate the fingerprint
	 * @return finger print
	 * @throws NoSuchAlgorithmException
	 * @throws NoSuchProviderException
	 * @throws IOException
	 */
	private static byte[] digest(String alghoritm, InputStream is) throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		byte[] buffer = new byte[1024];
		MessageDigest md =  MessageDigest.getInstance(alghoritm, BC);
		int read = is.read(buffer);
		while (read!=-1) {
//			System.out.println(byteToHex(buffer,0,read));
			md.update(buffer, 0, read);
			read = is.read(buffer);
		}
		is.close();
		return md.digest();
	}
	
	/**
	 * conversion of a inputstream to corresponding byte[]
	 * be carefull on using this method
	 * 
	 * @param is the input stream to read
	 * @return the byte[] of the inputstream
	 * @throws IOException
	 */
	public static byte[] inputStreamToByteArray(InputStream is) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int len = is.read(buf);
		while (len >= 0) {
			baos.write(buf, 0, len);
			len = is.read(buf);
		}
		is.close();
		baos.flush();
		byte[] ret = baos.toByteArray();
		baos.close();
		return ret;
	}

}
