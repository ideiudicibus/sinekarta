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

/**
 * utility class defining some constants
 * 
 * @author andrea.tessaro
 *
 */
public class Constants {

	public static final String SINEKARTA_CONTENT_MODEL_PREFIX_EXTENDED = "\\{http\\://www.sinekarta.org/alfresco/model/content/1.0\\}";
	public static final String SINEKARTA_CONTENT_MODEL_PREFIX = "sinekarta";
	public static final String STANDARD_CONTENT_MODEL_PREFIX_EXTENDED = "\\{http\\://www.alfresco.org/model/content/1.0\\}";
	public static final String STANDARD_CONTENT_MODEL_PREFIX = "cm";
	public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.sssZ";
	public static final String SINEKARTA_UTILITY_BACKING_BEAN = "SinekartaUtility";
	public static final String FUNZIONE_APPLET_SIGN = "S";
	public static final String FUNZIONE_APPLET_CERTIFICATE = "C";
	public static final String ENCODING = "UTF-8";

	public static final String RCS_SIGN_REASON_PROCEDURA_DI_FIRMA_DIGITALE_PER_MARCA_TEMPORALE = "Firma digitale del Responsabile della Conservazione Sostitutiva propedeutica all'apposizione della marca temporale";
	public static final String RCS_SIGN_REASON_PROCEDURA_DI_FIRMA_DIGITALE_PROPEDEUTICA_ALLA_CONSERVAZIONE_SOSTITUTIVA = "Firma digitale del Responsabile della Conservazione Sostitutiva propedeutica alla conservazione sostitutiva";
	public static final String PU_SIGN_REASON_PROCEDURA_DI_FIRMA_DIGITALE_PROPEDEUTICA_ALLA_CONSERVAZIONE_SOSTITUTIVA = "Firma digitale del Pubblico Ufficiale propedeutica alla conservazione sostitutiva";
	public static final String SIGN_LOCATION_ITALY = "Italy";
	public static final String SIGN_PROVIDER_SINEKARTA_OPENSIGNPDF = "sinekarta/opensignpdf";
	
	public static final String JS = "js:";
	public static final String SDF = "sdf:";

	public static final String SHA1 = "SHA1";
	public static final String SHA256 = "SHA256";
	public static final String ALGORITHM = "algorithm";
	public static final String HASH = "sinekarta:HASH";
	public static final String TSR = "sinekarta:TSR";
	public static final String IMPRONTA = "sinekarta:impronta";
	public static final String NOME_DOCUMENTO = "nomeDocumento";
	public static final String RIFERIMENTO_TEMPORALE_FIRMA_PU = "riferimentoTemporaleFirmaPU";
	public static final String RIFERIMENTO_TEMPORALE_FIRMA_RCS = "riferimentoTemporaleFirmaRCS";
	public static final String ID_RIFERIMENTO = "idRiferimento";
	public static final String TIPO_DOCUMENTO = "tipoDocumento";
	public static final String TIPI_DOCUMENTO_MARCATI = "tipiDocumentoMarcati";
	public static final String CDATA = "CDATA";
	public static final String DESCRIZIONE = "sinekarta:descrizione";
	public static final String DATA_APPOSIZIONE_MARCA_TEMPORALE = "dataApposizioneMarcaTemporale";
	public static final String IMPRONTE = "sinekarta:impronteMarcaTemporale";
	public static final String IMPRONTE_AE = "sinekarta:impronteComunicazioneAgenziaEntrate";

	public static final String EXTENSION_PDF = ".pdf";
	public static final String EXTENSION_XML = ".xml";

	public static final String IMAGE_GIF = "image/gif";
	public static final String IMAGE_PNG = "image/png";
	public static final String IMAGE_JPG = "image/jpeg";
	public static final String IMAGE_TIF = "image/tiff";
	public static final String APPLICATION_PDF = "application/pdf";
	public static final String APPLICATION_XML = "application/xml";
}
