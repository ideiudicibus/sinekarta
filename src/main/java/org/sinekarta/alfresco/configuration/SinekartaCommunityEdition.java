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
package org.sinekarta.alfresco.configuration;

import org.apache.log4j.Logger;

public class SinekartaCommunityEdition extends SinekartaEditionService {

	private static Logger tracer = Logger.getLogger(SinekartaCommunityEdition.class);
	
	@Override
	public void verify() {
		if (tracer.isDebugEnabled()) tracer.debug("Verifing sinekarta version");
		if (!isRegular()) {
			tracer.error("sinekarta is an open source software!");
			tracer.error("You are using sinekarta community edition on an Alfresco Enterprise edition. If you want to have sinekarta project mantained for many years, and you want to use it on the Alfresco Enterprise edition,");
			tracer.fatal("PLEASE BUY THE SINEKARTA ENTERPRISE LICENSE!");
			tracer.error("You can find more details on how to buy the license on www.sinekarta.org site.");
			tracer.error("sinekarta will work perfectly in Alfresco Enterprise edition, only a printstacktrace will be notified in next lines.");
			tracer.error("The printstacktrace is only a warning, there are no problems on sinekarta.");
			
			tracer.error("sinekarta e' un software open source !");
			tracer.error("Stai usando la versione community di sinekarta su una versione Enterprise di Alfresco. Se desideri che il progetto sinekarta venga mantenuto per diversi anni, e vuoi utilizzarlo con l'edizione Enterprise di Alfresco,");
			tracer.fatal("PERFAVORE COMPRA LA LICENZA ENTERPRISE DI SINEKARTA!");
			tracer.error("Puoi trovare maggiori dettagli su come comperare la licenza sul sito www.sinekarta.org.");
			tracer.error("sinekarta funzionera' perfettamente sull'edizione Enterprise di Alfresco, soltanto un printstacktrace verra' visualizzato nelle prossime linee.");
			tracer.error("Il printstacktrace e' soltanto un warning, non ci sono problemi su sinekarta.");

			try {
				throw new NullPointerException("PLEASE BUY THE SINEKARTA ENTERPRISE LICENSE! - PERFAVORE COMPRA LA LICENZA ENTERPRISE DI SINEKARTA!");
			} catch (Throwable t) {
				tracer.error(t.getMessage(),t);
				t.printStackTrace();
			}
		}
		if (tracer.isDebugEnabled()) tracer.debug("sinekarta version verification complete");
	}
	
}
