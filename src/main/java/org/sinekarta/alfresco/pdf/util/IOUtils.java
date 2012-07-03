/*
 * FirmaPdf version 0.0.x Copyright (C) 2006 Antonino Iacono (ant_iacono@tin.it)
 * and Roberto Resoli
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.sinekarta.alfresco.pdf.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1OutputStream;
import org.bouncycastle.asn1.ASN1StreamParser;
import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERSequence;

/**
 * Utility class with useful conversions and methods.
 * 
 * @author <a href="mailto:japaricio@accv.es">Javier Aparicio</a>
 * 
 */
public class IOUtils {

	/**
	 * Class Logger
	 */
	@SuppressWarnings("unused")
	private static Logger logger = Logger.getLogger(IOUtils.class);

	/**
	 * Dumps the content of the in memory Object to a byte[].
	 * 
	 * @param derEncObject
	 * @return
	 * @throws IOException
	 */
	public static byte[] toByteArray(ASN1EncodableVector v) throws IOException {

		return new DERSequence(v).getEncoded();

	}

	/**
	 * Dumps the content of the in memory Object to a byte[].
	 * 
	 * @param derEncObject
	 * @return
	 * @throws IOException
	 */
	public static byte[] toByteArray(DEREncodable derEncObject)
			throws IOException {

		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		BufferedOutputStream bos = new BufferedOutputStream(bOut);

		ASN1OutputStream dout = new ASN1OutputStream(bos);
		dout.writeObject(derEncObject);
		dout.close();

		return bOut.toByteArray();

	}

	/**
	 * Reads an DERObject from a byte[].
	 * 
	 * @param ab
	 * @return
	 * @throws IOException
	 */
	public static DERObject readDERObject(byte[] ab) throws IOException {

		ASN1InputStream in = IOUtils.getASN1InputStream(ab);
		DERObject obj = in.readObject();

		return obj;
	}

	/**
	 * Reads an DERObject from a byte[].
	 * 
	 * @param ab
	 * @return
	 * @throws IOException
	 */
	public static ASN1EncodableVector readASN1EncodableVector(byte[] ab)
			throws IOException {

		ASN1EncodableVector v = new ASN1EncodableVector();

		ASN1StreamParser p = new ASN1StreamParser(ab);
		DEREncodable s = (DEREncodable) p.readObject();
		@SuppressWarnings("rawtypes")
		Enumeration e = ((DERSequence)s.getDERObject()).getObjects();
		while (e.hasMoreElements()) {
			DERObject de = (DERObject)e.nextElement();
			v.add(de);
		}

		return v;
	}

	/**
	 * Gets an ASN1Stream from a byte[].
	 * 
	 * @param ab
	 * @return
	 */
	private static ASN1InputStream getASN1InputStream(byte[] ab) {

		ByteArrayInputStream bais = new ByteArrayInputStream(ab);
		BufferedInputStream bis = new BufferedInputStream(bais);

		ASN1InputStream asn1is = new ASN1InputStream(bis);

		return asn1is;

	}

}
