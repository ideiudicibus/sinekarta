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
package org.sinekarta.alfresco.model;

import org.alfresco.service.namespace.QName;

/**
 * usitlity class defining sinekarta model 
 * 
 * @author andrea.tessaro
 *
 */
public class SinekartaModel {

	// commons
	public static final String NAMESPACE_SINEKARTA_CONTENT_MODEL = "http://www.sinekarta.org/alfresco/model/content/1.0";
	public static final String SINEKARTA_PREFIX = "sinekarta";

	public static final String SINEKARTA_DAO = "SinekartaDao";
	public static final QName QNAME_SINEKARTA_DAO = QName.createQName(SINEKARTA_DAO);
	
	public static final String PERMISSION_GROUP_SINEKARTA_RCS = "SinekartaRCS";
	
	// sinekarta archive folder
	public static final String TYPE_ARCHIVE = "archive";
	public static final QName TYPE_QNAME_ARCHIVE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, TYPE_ARCHIVE);
	
	public static final QName PROP_QNAME_ICON = QName.createQName(
			"http://www.alfresco.org/model/application/1.0", "icon");

	// Aspects
	// each aspect has the String value and the relative QName
	// each aspect (both string and QNames) starts with ASPECT_
	
	public static final String ASPECT_SIGNED_DOCUMENT = "signedDocument";
	public static final QName ASPECT_QNAME_SIGNED_DOCUMENT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_SIGNED_DOCUMENT);

	public static final String ASPECT_DOCUMENT_ACQUIRING = "documentAcquiring";
	public static final QName ASPECT_QNAME_DOCUMENT_ACQUIRING = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_DOCUMENT_ACQUIRING);

	public static final String ASPECT_RCS_SIGNATURE = "RCSSignature";
	public static final QName ASPECT_QNAME_RCS_SIGNATURE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_RCS_SIGNATURE);
	
	public static final String ASPECT_TIMESTAMP_MARK = "timestampMark";
	public static final QName ASPECT_QNAME_TIMESTAMP_MARK = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_TIMESTAMP_MARK);

	public static final String ASPECT_SUBSTITUTIVE_PRESERVATION = "substitutivePreservation";
	public static final QName ASPECT_QNAME_SUBSTITUTIVE_PRESERVATION = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_SUBSTITUTIVE_PRESERVATION);

	public static final String ASPECT_OCR = "OCR";
	public static final QName ASPECT_QNAME_OCR = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_OCR);

	public static final String ASPECT_PU_SIGNATURE = "PUSignature";
	public static final QName ASPECT_QNAME_PU_SIGNATURE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_PU_SIGNATURE);

	public static final String ASPECT_TIMESTAMP_AEMARK = "timestampAEMark";
	public static final QName ASPECT_QNAME_TIMESTAMP_AEMARK = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_TIMESTAMP_AEMARK);

	public static final String ASPECT_AEMARK_CREATED = "aemarkCreated";
	public static final QName ASPECT_QNAME_AEMARK_CREATED = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASPECT_AEMARK_CREATED);

	// Properties
	// each property has the String value and the relative QName
	// each property (both string and QNames) starts with PROP_
	
	// type archive

	public static final String PROP_RCS_USER_ID = "rcsUserId";
	public static final QName PROP_QNAME_RCS_USER_ID = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_RCS_USER_ID);
	
	public static final String PROP_SINEKARTA_ADMIN_USER_ID = "sinekartaAdminUserId";
	public static final QName PROP_QNAME_SINEKARTA_ADMIN_USER_ID = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SINEKARTA_ADMIN_USER_ID);
	
	// aspect signedDocument
	
	public static final String PROP_SIGNATURE_FORMAT = "signatureFormat";
	public static final QName PROP_QNAME_SIGNATURE_FORMAT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SIGNATURE_FORMAT);
	
	// aspect documentAcquiring
	
	public static final String PROP_DOCUMENT_TYPE = "documentType";
	public static final QName PROP_QNAME_DOCUMENT_TYPE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_DOCUMENT_TYPE);
	
	public static final String PROP_LANGUAGE = "language";
	public static final QName PROP_QNAME_LANGUAGE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_LANGUAGE);

	public static final String PROP_DOCUMENT_DATE = "documentDate";
	public static final QName PROP_QNAME_DOCUMENT_DATE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_DOCUMENT_DATE);

	public static final String PROP_DOCUMENT_MOVED = "documentMoved";
	public static final QName PROP_QNAME_DOCUMENT_MOVED = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_DOCUMENT_MOVED);

	public static final String PROP_TIMESTAMP_PROCESS_START = "timestampProcessStart";
	public static final QName PROP_QNAME_TIMESTAMP_PROCESS_START = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_TIMESTAMP_PROCESS_START);

	public static final String PROP_REFERENCE_ID = "referenceId";
	public static final QName PROP_QNAME_REFERENCE_ID = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_REFERENCE_ID);
	
	public static final String PROP_PU_SIGN_REQUIRED = "PUSignRequired";
	public static final QName PROP_QNAME_PU_SIGN_REQUIRED = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_PU_SIGN_REQUIRED);
	
	// aspect RCSSignature

	public static final String PROP_RCS_SIGNED_DOCUMENT_FINGERPRINT = "RCSSignedDocumentFingerprint";
	public static final QName PROP_QNAME_DOCUMENT_RCS_SIGNED_FINGERPRINT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_RCS_SIGNED_DOCUMENT_FINGERPRINT);
	
	public static final String PROP_TIMESTAMP_RCS_SIGNATURE = "timestampRCSSignature";
	public static final QName PROP_QNAME_TIMESTAMP_RCS_SIGNATURE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_TIMESTAMP_RCS_SIGNATURE);
	
	// aspect timestampMark

	public static final String PROP_MARK_DOCUMENT_DESCRIPTION = "markDocumentDescription";
	public static final QName PROP_QNAME_MARK_DOCUMENT_DESCRIPTION = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_MARK_DOCUMENT_DESCRIPTION);

	public static final String PROP_MARK_DOCUMENT_REFERENCE_ID = "markDocumentReferenceId";
	public static final QName PROP_QNAME_MARK_DOCUMENT_REFERENCE_ID = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_MARK_DOCUMENT_REFERENCE_ID);
	
	public static final String PROP_MARK_TIMESTAMP_RCS_SIGNATURE = "markTimestampRCSSignature";
	public static final QName PROP_QNAME_MARK_TIMESTAMP_RCS_SIGNATURE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_MARK_TIMESTAMP_RCS_SIGNATURE);
	
	public static final String ASSOCIATION_MARKED_DOCUMENT_LIST = "markedDocumentList";
	public static final QName ASSOCIATION_QNAME_MARKED_DOCUMENT_LIST = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASSOCIATION_MARKED_DOCUMENT_LIST);
	
	public static final String PROP_MARK_FINGER_PRINT = "markFingerPrint";
	public static final QName PROP_QNAME_MARK_FINGER_PRINT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_MARK_FINGER_PRINT);

	public static final String PROP_MARK_TIMESTAMP_TOKEN = "markTimestampToken";
	public static final QName PROP_QNAME_MARK_TIMESTAMP_TOKEN = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_MARK_TIMESTAMP_TOKEN);
	
	public static final String PROP_MARK_DOCUMENT_TYPE = "markDocumentType";
	public static final QName PROP_QNAME_MARK_DOCUMENT_TYPE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_MARK_DOCUMENT_TYPE);
	
	// aspect substitutivePreservation
	
	public static final String ASSOCIATION_MARKS_DOCUMENT = "marksDocument";
	public static final QName ASSOCIATION_QNAME_MARKS_DOCUMENT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASSOCIATION_MARKS_DOCUMENT);

	// aspect OCR
	
	public static final String PROP_OCR_RESULT = "OCRResult";
	public static final QName PROP_QNAME_OCR_RESULT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_OCR_RESULT);

	// aspect PUSignature

	public static final String PROP_PU_SIGNED_DOCUMENT_FINGERPRINT = "PUSignedDocumentFingerprint";
	public static final QName PROP_QNAME_DOCUMENT_PU_SIGNED_FINGERPRINT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_PU_SIGNED_DOCUMENT_FINGERPRINT);
	
	public static final String PROP_TIMESTAMP_PU_SIGNATURE = "timestampPUSignature";
	public static final QName PROP_QNAME_TIMESTAMP_PU_SIGNATURE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_TIMESTAMP_PU_SIGNATURE);
	
	// aspect aemarkCreated
	
	public static final String ASSOCIATION_AEXMLFILE_DOCUMENT = "aeXMLFile";
	public static final QName ASSOCIATION_QNAME_AEXMLFILE_DOCUMENT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASSOCIATION_AEXMLFILE_DOCUMENT);

	public static final String ASSOCIATION_AEPDFFILE_DOCUMENT = "aePDFFile";
	public static final QName ASSOCIATION_QNAME_AEPDFFILE_DOCUMENT = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, ASSOCIATION_AEXMLFILE_DOCUMENT);

	// generic attributes
	
	// used for saving (jn user noderef) the last smart card dll used
	public static final String PROP_SINEKARTA_SMARTCARD_DLL = "sinekartaSmartcardDLL";
	public static final QName PROP_QNAME_SINEKARTA_SMARTCARD_DLL = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SINEKARTA_SMARTCARD_DLL);
	
	// used for saving (jn archive noderef) last datiTitolareContabilita used
	public static final String PROP_SINEKARTA_AE_DATITITOLARECONTABILITA = "datiTitolareContabilita";
	public static final QName PROP_QNAME_SINEKARTA_AE_DATITITOLARECONTABILITA = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SINEKARTA_AE_DATITITOLARECONTABILITA);
	
	// used for saving (jn user noderef) last DatiResponsabileConservazione used
	public static final String PROP_SINEKARTA_AE_DATIRESPONSABILECONSERVAZIONE = "datiResponsabileConservazione";
	public static final QName PROP_QNAME_SINEKARTA_AE_DATIRESPONSABILECONSERVAZIONE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SINEKARTA_AE_DATIRESPONSABILECONSERVAZIONE);
	
	// used for saving (jn user noderef) last DatiDelegatiConservazione used
	public static final String PROP_SINEKARTA_AE_DATIDELEGATICONSERVAZIONE = "datiDelegatiConservazione";
	public static final QName PROP_QNAME_SINEKARTA_AE_DATIDELEGATICONSERVAZIONE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SINEKARTA_AE_DATIDELEGATICONSERVAZIONE);

	// used for saving (jn archive noderef) last DatiTrasmissione used
	public static final String PROP_SINEKARTA_AE_DATITRASMISSIONE = "datiTrasmissione";
	public static final QName PROP_QNAME_SINEKARTA_AE_DATITRASMISSIONE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SINEKARTA_AE_DATITRASMISSIONE);
	
	// used for saving (jn user noderef) last LuogoConservazione used
	public static final String PROP_SINEKARTA_AE_LUOGOCONSERVAZIONE = "luogoConservazione";
	public static final QName PROP_QNAME_SINEKARTA_AE_LUOGOCONSERVAZIONE = QName.createQName(
			NAMESPACE_SINEKARTA_CONTENT_MODEL, PROP_SINEKARTA_AE_LUOGOCONSERVAZIONE);
	
}
