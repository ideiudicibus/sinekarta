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
 * Part of this code come from 
 * FirmaPdf version 0.0.x Copyright (C) 2006 Antonino Iacono (ant_iacono@tin.it)
 * and Roberto Resoli
 * See method description for more details
 * 
 */
package org.sinekarta.alfresco.pdf.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERConstructedSet;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERNull;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.nist.NISTObjectIdentifiers;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.tsp.TimeStampToken;
import org.sinekarta.alfresco.exception.PDFException;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.TSRGenerator;
import org.sinekarta.alfresco.util.Util;
import org.sinekarta.sign.area.DigitalSignatureDocument;
import org.sinekarta.sign.area.TextUtil;

import com.itextpdf.text.pdf.AcroFields;
import com.itextpdf.text.pdf.PdfArray;
import com.itextpdf.text.pdf.PdfDate;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfPKCS7;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignature;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfString;
import com.itextpdf.text.pdf.codec.Base64;

@SuppressWarnings("deprecation")
public class PDFTools {

	private static Logger tracer = Logger.getLogger(PDFTools.class);

	public static final String PDF = ".pdf";

	private static final int CSIZE = 0x5090 / 2;

	public static void sign(DigitalSignatureDocument doc,
			X509Certificate certificate, InputStream is, OutputStream os) {
		signAndMark(doc, certificate, is, os, null, null, null);
	}

	public static void signAndMark(DigitalSignatureDocument doc,
			X509Certificate certificate, InputStream is, OutputStream os,
			String tsaUrl, String tsaUser, String tsaPassword) {
		try {

			// creo il reader del pdf
			PdfReader reader = new PdfReader(is);

			// creo lo stamper (se il pdf e' gia' firmato, controfirma,
			// altrimenti firma
			PdfStamper stamper = null;
			if (isPdfSigned(reader)) {
				if (tracer.isDebugEnabled()) tracer.debug("document already signed, i will apply another sign");
				stamper = PdfStamper.createSignature(reader, os, '\0', null, true);
			} else {
				if (tracer.isDebugEnabled()) tracer.debug("document never signed before, this is first");
				stamper = PdfStamper.createSignature(reader, os, '\0');
			}

			// creo la signature apparence
			PdfSignatureAppearance signatureApparence = stamper.getSignatureAppearance();
			// ripassandogli i campi passati nel giro di calcolo dell'impronta
			signatureApparence.setReason(doc.getDescription());
			signatureApparence.setLocation(doc.getLocation());
			signatureApparence.setExternalDigest(new byte[128], new byte[32], "RSA");
			signatureApparence.setProvider(Constants.SIGN_PROVIDER_SINEKARTA_OPENSIGNPDF);
			signatureApparence.setSignDate(_getSignDate(doc));
			signatureApparence.getStamper().setModDate(_getModDate(doc));
			signatureApparence.getStamper().setFileID(_getFileID(doc));

			// questo e' il certificato su cui lavorare
			X509Certificate[] certList = new X509Certificate[1];
			certList[0] = certificate;

			signatureApparence.setCrypto(null, certList, null, null);

			// firma detached, con sha256 questo e' il modo
			PdfSignature signature = new PdfSignature(PdfName.ADOBE_PPKLITE, PdfName.ADBE_PKCS7_DETACHED);
			// imposto (nel PDF) i dati di firma
			signature.setDate(new PdfDate(signatureApparence.getSignDate()));
			signature.setName(PdfPKCS7.getSubjectFields((X509Certificate) certList[0]).getField("CN"));
			signature.setReason(signatureApparence.getReason());
			signature.setLocation(signatureApparence.getLocation());
			signatureApparence.setCryptoDictionary(signature);

			// mi appresto a firmare
			HashMap<PdfName, Integer> extraContent = new HashMap<PdfName, Integer>();
			extraContent.put(PdfName.CONTENTS, new Integer((CSIZE * 2) + 2));
			signatureApparence.preClose(extraContent);

			// Create the set of Hash algorithms
			DERConstructedSet digestAlgorithms = new DERConstructedSet();

			// Creo manualmente la sequenza di digest algos
			ASN1EncodableVector digestAlgorithm = new ASN1EncodableVector();
			digestAlgorithm.add(new DERObjectIdentifier(NISTObjectIdentifiers.id_sha256.getId())); // SHA256
			digestAlgorithm.add(new DERNull());

			digestAlgorithms.addObject(new DERSequence(digestAlgorithm));

			// Get all the certificates
			ASN1EncodableVector certificateVector = new ASN1EncodableVector();
			for (int c = 0; c < certList.length; c++) {
				ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(certList[c].getEncoded()));
				certificateVector.add(tempstream.readObject());
			}

			DERSet dercertificates = new DERSet(certificateVector);

			// Create signerinfo structure.
			ASN1EncodableVector signerinfo = new ASN1EncodableVector();

			// Add the signerInfo version
			signerinfo.add(new DERInteger(1));

			certificateVector = new ASN1EncodableVector();
			certificateVector.add(CertUtil.getIssuer(certList[0]));
			certificateVector.add(new DERInteger(certList[0].getSerialNumber()));
			signerinfo.add(new DERSequence(certificateVector));

			// Add the digestAlgorithm
			certificateVector = new ASN1EncodableVector();
			certificateVector.add(new DERObjectIdentifier(NISTObjectIdentifiers.id_sha256.getId())); // SHA256
			certificateVector.add(new DERNull());
			signerinfo.add(new DERSequence(certificateVector));

			// ripesco (dai dati salvati) l'impronta
			byte[] signatureBytes = doc.digitalSignatureToByteArray();

			// ed i signed attributes
			ASN1EncodableVector signedAttributes = _getSignedAttributes(doc);

			// add the authenticated attribute if present
			signerinfo.add(new DERTaggedObject(false, 0, new DERSet(signedAttributes)));

			// Add the digestEncryptionAlgorithm
			certificateVector = new ASN1EncodableVector();
			certificateVector.add(new DERObjectIdentifier("1.2.840.113549.1.1.1"));// RSA
			certificateVector.add(new DERNull());
			signerinfo.add(new DERSequence(certificateVector));

			// Add the encrypted digest
			signerinfo.add(new DEROctetString(signatureBytes));

			// Add unsigned attributes (timestamp)
			if (tsaUrl != null && !"".equals(tsaUrl.toString())) {
				if (tracer.isDebugEnabled()) tracer.debug("applying timestamp mark other than signature");
				byte[] timestampHash = Util.digest256(signatureBytes);
				ASN1EncodableVector unsignedAttributes = buildUnsignedAttributes(doc, timestampHash, tsaUrl, tsaUser, tsaPassword);
				if (unsignedAttributes != null) {
					signerinfo.add(new DERTaggedObject(false, 1, new DERSet(unsignedAttributes)));
				}
			}

			// Create the contentInfo.
			ASN1EncodableVector extraDataVector = new ASN1EncodableVector();
			extraDataVector.add(new DERObjectIdentifier("1.2.840.113549.1.7.1")); // PKCS7SignedData

			DERSequence contentinfo = new DERSequence(extraDataVector);

			// Finally build the body out of all the components above
			ASN1EncodableVector body = new ASN1EncodableVector();
			body.add(new DERInteger(1)); // pkcs7 version, always 1
			body.add(digestAlgorithms);
			body.add(contentinfo);
			body.add(new DERTaggedObject(false, 0, dercertificates));
//			body.add(contentinfoEncoding);

			// Only allow one signerInfo
			body.add(new DERSet(new DERSequence(signerinfo)));

			// Now we have the body, wrap it in it's PKCS7Signed shell
			// and return it
			ASN1EncodableVector completeStructure = new ASN1EncodableVector();
			completeStructure.add(new DERObjectIdentifier("1.2.840.113549.1.7.2"));// PKCS7_SIGNED_DATA
			completeStructure.add(new DERTaggedObject(0, new DERSequence(body)));

			ASN1EncodableVector extraDataVectorEncoding = new ASN1EncodableVector();
			// Attention!!! for actual digital signature specification this field is optional but for new specs this is pretty important
			extraDataVectorEncoding.add(new DERObjectIdentifier("1.2.840.114283")); // encoding attribute 
			extraDataVectorEncoding.add(new DEROctetString("115.105.110.101.107.97.114.116.97".getBytes()));

			// applico la firma al PDF
			byte[] extraDataVectorEncodingBytes = IOUtils.toByteArray(new DERSequence(extraDataVectorEncoding));

			// applico la firma al PDF
			byte[] completeStructureBytes = IOUtils.toByteArray(new DERSequence(completeStructure));

			byte[] outputCompleteStructureBytes = new byte[CSIZE];
			PdfDictionary signatureApparenceDictionary = new PdfDictionary();
			System.arraycopy(completeStructureBytes, 0,outputCompleteStructureBytes, 0,completeStructureBytes.length);
			System.arraycopy(extraDataVectorEncodingBytes, 0,outputCompleteStructureBytes, completeStructureBytes.length,extraDataVectorEncodingBytes.length); // encoding attribute
			signatureApparenceDictionary.put(PdfName.CONTENTS, new PdfString(outputCompleteStructureBytes).setHexWriting(true));
			signatureApparence.close(signatureApparenceDictionary);

			// this should be already done, but ...
			// closing streams
			try {
				is.close();
			} catch (IOException e) {
				tracer.error("error on input stream", e);
			}
			try {
				os.flush();
			} catch (IOException e) {
				tracer.error("error on output stream", e);
			}
			try {
				os.close();
			} catch (IOException e) {
				tracer.error("error on output stream", e);
			}
		} catch (Exception e) {
			tracer.error("Unable to sign PDF.", e);
			throw new PDFException("Unable to sign PDF.", e);
		}
	}

	public static void calculateFingerPrint(DigitalSignatureDocument doc,
			X509Certificate certificate, InputStream is) {
		try {

			// creo il reader del pdf
			PdfReader reader = new PdfReader(is);

			// creo lo stamper (se il pdf e' gia' firmato, controfirma,
			// altrimenti firma
			PdfStamper stamper = null;
			if (isPdfSigned(reader)) {
				if (tracer.isDebugEnabled()) tracer.debug("calculating finger print for document already signed");
				stamper = PdfStamper.createSignature(reader, null, '\0', null,
						true);
			} else {
				if (tracer.isDebugEnabled()) tracer.debug("calculating finger print for document never signed before");
				stamper = PdfStamper.createSignature(reader, null, '\0');
			}

			// questo e' il certificato su cui lavorare
			Certificate[] chain = new Certificate[1];
			chain[0] = certificate;

			// creo la signature apparence
			PdfSignatureAppearance sap = stamper.getSignatureAppearance();
			// passandogli i dati di firma che verranno salvati per il giro di
			// firma vera e propria
			sap.setReason(doc.getDescription());
			sap.setLocation(doc.getLocation());
			sap.setExternalDigest(new byte[128], new byte[32], "RSA");
			sap.setProvider(Constants.SIGN_PROVIDER_SINEKARTA_OPENSIGNPDF);
			Calendar now = Calendar.getInstance();
			sap.setSignDate(now);

			sap.setCrypto(null, chain, null, null);

			// firma detached, con sha256 questo e' il modo
			PdfSignature dic = new PdfSignature(PdfName.ADOBE_PPKLITE,PdfName.ADBE_PKCS7_DETACHED);
			// imposto (nel PDF) i dati di firma
			dic.setDate(new PdfDate(sap.getSignDate()));
			dic.setName(PdfPKCS7.getSubjectFields((X509Certificate) chain[0]).getField("CN"));
			dic.setReason(sap.getReason());
			dic.setLocation(sap.getLocation());
			sap.setCryptoDictionary(dic);

			// mi appresto a firmare
			HashMap<PdfName, Integer> exc = new HashMap<PdfName, Integer>();
			exc.put(PdfName.CONTENTS, new Integer(CSIZE * 2 + 2));
			sap.preClose(exc);

			// calcolo l'impronta del PDF
			byte[] firstHash = Util.digest256(sap.getRangeStream()); // USO
																		// DIGEST
																		// A 256

			// creo i signed attribute necessari per il calcolo dell'impronta e
			// li salvo
			ASN1EncodableVector signedAttributes = buildSignedAttributes(firstHash, sap.getSignDate());
			byte[] bytesForSecondHash = IOUtils.toByteArray(new DERSet(signedAttributes));

			// calcolo dell'impronta
			byte[] fingerPrint = Util.digest256(bytesForSecondHash);

			// salvo i dati di firma
			_setSignedAttributes(doc, signedAttributes);
			_setFileID(doc, (PdfArray) sap.getStamper().getFileID());
			doc.fingerPrintFromByteArray(fingerPrint);
			_setModDate(doc, sap.getStamper().getModDate());
			_setSignDate(doc, now);

			// this should be already done, but ...
			// closing streams
			try {
				is.close();
			} catch (IOException e) {
				tracer.error("error on input stream", e);
			}
		} catch (Exception e) {
			tracer.error("Unable to calculate finger print of PDF.", e);
			throw new PDFException("Unable calculate finger print of PDF.", e);
		}
	}

	/**
	 * metodo preso da opensignpdf copiato perche' l'originale non supporta
	 * l'sha256 e' da sostituire con la chiamata a opensignpdf
	 */
	private static ASN1EncodableVector buildSignedAttributes(byte[] hash,
			Calendar cal) {

		ASN1EncodableVector signedAttributes = new ASN1EncodableVector();

		// Content type
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERObjectIdentifier("1.2.840.113549.1.9.3"));// CONTENT_TYPE
		v.add(new DERSet(new DERObjectIdentifier("1.2.840.113549.1.7.1")));// PKCS7_DATA
		signedAttributes.add(new DERSequence(v));

		if (cal != null) {
			// signing time
			v = new ASN1EncodableVector();
			v.add(new DERObjectIdentifier("1.2.840.113549.1.9.5")); // SIGNING_TIME
			v.add(new DERSet(new DERUTCTime(cal.getTime())));
			signedAttributes.add(new DERSequence(v));
		}

		// message digest
		v = new ASN1EncodableVector();
		v.add(new DERObjectIdentifier("1.2.840.113549.1.9.4"));// MESSAGE_DIGEST
		v.add(new DERSet(new DEROctetString(hash)));
		signedAttributes.add(new DERSequence(v));

		return signedAttributes;

	}

	/**
	 * metodo preso da opensignpdf copiato perche' l'originale non supporta
	 * l'sha256 e' da sostituire con la chiamata a opensignpdf
	 */
	private static ASN1EncodableVector buildUnsignedAttributes(
			DigitalSignatureDocument doc, byte[] hash, String serverTimestamp,
			String tsUser, String tsPassword) throws Exception {

		TSRGenerator tsrGen = new TSRGenerator(serverTimestamp, tsUser,tsPassword);

		TimeStampRequestGenerator tsrGenerator = new TimeStampRequestGenerator();
		tsrGenerator.setCertReq(true);

		BigInteger nonce = BigInteger.valueOf(0);
		TimeStampRequest request = tsrGenerator.generate(TSPAlgorithms.SHA256,hash, nonce);

		byte[] respBytes = tsrGen.doRequest(request);

		TimeStampResponse bcResp = new TimeStampResponse(respBytes);

		bcResp.validate(request);

		TimeStampToken tsToken = bcResp.getTimeStampToken();

		doc.setEncodedTimeStampToken(Base64.encodeBytes(tsToken.getEncoded()));

		ASN1InputStream tempstream = new ASN1InputStream(new ByteArrayInputStream(respBytes));
		ASN1EncodableVector unsignedAttributes = new ASN1EncodableVector();

		// time Stamp token : id-aa-timeStampToken da RFC3161, alias old
		// id-smime-aa-timeStampToken
		ASN1EncodableVector v = new ASN1EncodableVector();
		v.add(new DERObjectIdentifier("1.2.840.113549.1.9.16.2.14")); // id-aa-timeStampToken

		ASN1Sequence seq = (ASN1Sequence) tempstream.readObject();
		DERObject timeStampToken = (DERObject) seq.getObjectAt(1);
		v.add(new DERSet(timeStampToken));

		unsignedAttributes.add(new DERSequence(v));

		return unsignedAttributes;

	}

	/**
	 * metodo di utilita' che verifica se il pdf in input e' gia' firmato
	 * 
	 * @param reader
	 * @return
	 */
	public static boolean isPdfSigned(InputStream is) {
		if (tracer.isDebugEnabled())
			tracer.debug("chacking if PDF/A is signed");
		try {
			PdfReader reader = new PdfReader(is);
			boolean ret = false;
			if (PDFTools.isPdfSigned(reader)) {
				ret = true;
			}
			reader.close();
			return ret;
		} catch (Exception e) {
			tracer.error("Unable to read PDF. Unable to check if the pdf is signed.",e);
			throw new PDFException("Unable to read PDF. Unable to check if the pdf is signed.",e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
		}
	}

	/**
	 * metodo di utilita' che verifica se il pdf in input e' gia' firmato
	 * 
	 * @param reader
	 * @return
	 */
	public static boolean isPdfSigned(PdfReader reader) {
		if (tracer.isDebugEnabled())
			tracer.debug("chacking if PDF/A is signed");
		try {
			AcroFields af = reader.getAcroFields();

			// Search of the whole signature
			ArrayList<String> names = af.getSignatureNames();

			// For every signature :
			if (names.size() > 0) {
				if (tracer.isDebugEnabled())tracer.debug("yes, it is");
				return true;
			} else {
				if (tracer.isDebugEnabled())tracer.debug("no, it isn't");
				return false;
			}
		} catch (Exception e) {
			tracer.error("Unable to read PDF. Unable to check if the pdf is signed.",e);
			throw new PDFException("Unable to read PDF. Unable to check if the pdf is signed.",e);
		}
	}

	/**
	 * metodo di utilita' che verifica se il pdf in input e' un PDF/A
	 * 
	 * @param reader
	 * @return
	 */
	public static boolean isPdfa(InputStream is) {
		if (tracer.isDebugEnabled()) tracer.debug("checking if PDF is PDF/A");
		PdfReader reader = null;
		ByteArrayInputStream bais = null;
		XMLStreamReader sr = null;
		try {
			reader = new PdfReader(is);
			byte[] metadata = reader.getMetadata();
			if (metadata == null || metadata.length == 0)
				return false;
			bais = new ByteArrayInputStream(metadata);
			sr = XMLInputFactory.newInstance().createXMLStreamReader(bais);
			boolean isConformanceTag = false;
			int eventCode;
			while (sr.hasNext()) {
				eventCode = sr.next();
				String val = null;
				switch (eventCode) {
				case 1:
					val = sr.getLocalName();
					if (val.equals("conformance") && sr.getNamespaceURI().equals("http://www.aiim.org/pdfa/ns/id/"))
						isConformanceTag = true;
					break;
				case 4:
					val = sr.getText();
					if (isConformanceTag) {
						if (val.equals("A") || val.equals("B")) {
							if (tracer.isDebugEnabled()) tracer.debug("yes, it is");
							return true;
						} else {
							if (tracer.isDebugEnabled()) tracer.debug("no, it isn't");
							return false;
						}
					}
					break;
				}
			}
		} catch (Exception e) {
			tracer.error("Unable to read PDF. Unable to check if the pdf is a pdf/a.",e);
			throw new PDFException("Unable to read PDF. Unable to check if the pdf is a pdf/a.",e);
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) {
				tracer.error("error on pdf reader", e);
			}
			try {
				if (sr != null)
					sr.close();
			} catch (Exception e) {
				tracer.error("error on stax reader", e);
			}
			try {
				if (bais != null)
					bais.close();
			} catch (Exception e) {
				tracer.error("error on input stream", e);
			}
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
				tracer.error("error on input stream", e);
			}
		}
		if (tracer.isDebugEnabled())
			tracer.debug("no, it isn't");
		return false;
	}

	/**
	 * metodo di utilita' che verifica se il pdf in input e' un PDF/A
	 * 
	 * @param reader
	 * @return
	 */
	public static String getPrintableTimestampToken(InputStream is) {
		if (tracer.isDebugEnabled()) tracer.debug("getting timestamp token");
		String ret = null;
		PdfReader reader = null;
		try {
			reader = new PdfReader(is);
			AcroFields af = reader.getAcroFields();
			for (String name : af.getSignatureNames()) {
				PdfPKCS7 pk = af.verifySignature(name);
				TimeStampToken tst = pk.getTimeStampToken();
				if (tst != null) {
					ret = Base64.encodeBytes(tst.getEncoded());
				}
			}
			if (tracer.isDebugEnabled()) tracer.debug("timestamp token returned : " + ret);
			return ret;
		} catch (Exception e) {
			tracer.error("Unable to read PDF. Unable to get timestamp token.", e);
			throw new PDFException("Unable to read PDF. Unable to get timestamp token.", e);
		} finally {
			try {
				if (reader != null)
					reader.close();
			} catch (Exception e) {
				tracer.error("error on pdf reader", e);
			}
			try {
				if (is != null)
					is.close();
			} catch (Exception e) {
				tracer.error("error on input stream", e);
			}
		}
	}

	public static PdfDate _getModDate(DigitalSignatureDocument doc) {
		return new PdfDate(PdfDate.decode(doc.getModDateUnicodeString()));
	}

	public static void _setModDate(DigitalSignatureDocument doc, PdfDate modDate) {
		doc.setModDateUnicodeString(modDate.toUnicodeString());
	}

	public static PdfArray _getFileID(DigitalSignatureDocument doc) {
		PdfString ps1 = new PdfString(TextUtil.hexTobyte(doc.getFileID_0_byteContent()));
		ps1.setHexWriting(true);
		PdfString ps2 = new PdfString(TextUtil.hexTobyte(doc.getFileID_1_byteContent()));
		ps2.setHexWriting(true);
		PdfArray ar = new PdfArray();
		ar.add(ps1);
		ar.add(ps2);
		return ar;
	}

	public static void _setFileID(DigitalSignatureDocument doc, PdfArray fileID) {
		doc.setFileID_0_byteContent(TextUtil.byteToHex(((PdfString) fileID.getPdfObject(0)).getBytes()));
		doc.setFileID_1_byteContent(TextUtil.byteToHex(((PdfString) fileID.getPdfObject(1)).getBytes()));
	}

	public static Calendar _getSignDate(DigitalSignatureDocument doc) {
		if (doc.getSignDate() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
			Date d = null;
			try {
				d = sdf.parse(doc.getSignDate());
			} catch (ParseException e) {
				// not possible
			}
			Calendar c = Calendar.getInstance();
			c.setTime(d);
			return c;
		}
		return null;
	}

	public static void _setSignDate(DigitalSignatureDocument doc,
			Calendar signDate) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSSZ");
		doc.setSignDate(sdf.format(signDate.getTime()));
	}

	public static ASN1EncodableVector _getSignedAttributes(
			DigitalSignatureDocument doc) throws IOException {
		return IOUtils.readASN1EncodableVector(TextUtil.hexTobyte(doc
				.getSignedAttributesEncoded()));
	}

	public static void _setSignedAttributes(DigitalSignatureDocument doc,
			ASN1EncodableVector signedAttributes) throws IOException {
		doc.setSignedAttributesEncoded(TextUtil.byteToHex(IOUtils
				.toByteArray(signedAttributes)));
	}

	public static String calculatePdfName(String fileName) {
		int dotIdx = fileName.lastIndexOf('.');
		if (dotIdx >= 0) {
			return fileName.substring(0, dotIdx)
					+ Configuration.getInstance().getPdfaSuffix()
					+ PDFTools.PDF;
		} else {
			return fileName + Configuration.getInstance().getPdfaSuffix()
					+ PDFTools.PDF;
		}
	}

}
