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
package org.sinekarta.alfresco.exception;

/**
 * exception thrown when something goes wrong in sing process
 * 
 * @author andrea.tessaro
 *
 */
public class PDFException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public PDFException() {
		super();
	}

	public PDFException(String message, Throwable cause) {
		super(message, cause);
	}

	public PDFException(String message) {
		super(message);
	}

	public PDFException(Throwable cause) {
		super(cause);
	}

}
