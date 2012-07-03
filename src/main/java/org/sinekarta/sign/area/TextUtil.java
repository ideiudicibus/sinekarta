package org.sinekarta.sign.area;
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


public class TextUtil {

	public static String byteToHex (byte[] buf) {
		if (buf==null) return null;
		return byteToHex(buf, 0, buf.length);
	}

	public static String byteToHex (byte[] buf, int offset, int len) {
		if (buf==null) return null;
		StringBuffer ret = new StringBuffer();
		long tmpL=0;
		String tmp;
		for (int i=0;i<len/8;i++) {
			for (int k=0;k<8;k++) {
				tmpL=tmpL<<8;
				tmpL=tmpL|(0xff&buf[(i*8)+k+offset]);
			}
			tmp = Long.toHexString(tmpL);
			for (int j=0;j<16-tmp.length();j++) {
				ret.append('0');
			}
			ret.append(tmp);
			tmpL=0;
		}
		int mod = len%8;
		if (mod!=0) {
			for (int k=len-mod;k<len;k++) {
				tmpL=tmpL<<8;
				tmpL=tmpL|(0xff&buf[k+offset]);
			}
			tmp = Long.toHexString(tmpL);
			for (int j=0;j<(mod*2)-tmp.length();j++) {
				ret.append('0');
			}
			ret.append(tmp);
		}
		return ret.toString().toUpperCase();
	}

	public static byte[] hexTobyte (String buf) {
		if (buf==null) return null;
		byte[] ret = new byte[buf.length()/2];
		for (int i=0; i<buf.length(); i=i+2) {
			ret[i/2] = (byte)Integer.parseInt(buf.substring(i, i+2), 16);
		}
		return ret;
	}

}
