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
 * requested document type is invalid
 * 
 * @author andrea.tessaro
 *
 */
public class InvalidDocumentTypeIdException extends Exception {

	private static final long serialVersionUID = -7861420716588819443L;

	public InvalidDocumentTypeIdException() {
		super();
	}

	public InvalidDocumentTypeIdException(String message, Throwable cause) {
		super(message, cause);
	}

	public InvalidDocumentTypeIdException(String message) {
		super(message);
	}

	public InvalidDocumentTypeIdException(Throwable cause) {
		super(cause);
	}

}
