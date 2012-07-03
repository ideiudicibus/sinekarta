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
package org.sinekarta.alfresco.configuration.dao;

import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.sinekarta.alfresco.configuration.SinekartaEditionService;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.exception.ConfigurationException;
import org.sinekarta.alfresco.exception.EntityNotFoundException;
import org.sinekarta.alfresco.exception.InvalidDocumentTypeIdException;
import org.springframework.orm.hibernate3.support.HibernateDaoSupport;

/**
 * sinekarta has a low number of tables, so we have only one DAO for all persisted classes
 * This is the sinekarta DAO
 * This is a spring bean
 * 
 * @author andrea.tessaro
 *
 */
public class SinekartaDao extends HibernateDaoSupport {

	private static Logger tracer = Logger.getLogger(SinekartaDao.class);
	
	private SinekartaEditionService sinekartaEditionService;
	
	private boolean initialized=false;
	
	/**
	 * getting a documentType by it's id
	 * 
	 * @param id the id of the documentType
	 * @return the documentType (if it exists)
	 * @throws InvalidDocumentTypeIdException if the passed id is null or an empty string
	 * @throws EntityNotFoundException if the documentType was not found
	 */
	public DocumentType getDocumentType(Integer id) throws InvalidDocumentTypeIdException, EntityNotFoundException {
		if (!initialized) { 
			throw new ConfigurationException("Invalid configuration");
		}
		if (id == null) {
			tracer.error("SinekartaDao.getDocumentType : id can not be null.");
			throw new InvalidDocumentTypeIdException("SinekartaDao.getDocumentType : id can not be null.");
		}
		try {
			return (DocumentType) getHibernateTemplate().load(DocumentType.class, id);
		} catch (Throwable t) {
			tracer.error("Document type not found, or problems on read it", t);
			throw new EntityNotFoundException("Document type not found, or problems on read it", t);
		}
	}

	/**
	 * getting a list of documentTypes by it's id
	 * 
	 * @param id the partial id of the documentType
	 * @return the list of documentTypes (if it exists)
	 */
	@SuppressWarnings("unchecked")
	public List<DocumentType> getDocumentTypesByDescription(String description) {
		if (!initialized) { 
			throw new ConfigurationException("Invalid configuration");
		}
		String filter = description==null?"%":"%"+description+"%";
		return (List<DocumentType>) getHibernateTemplate().findByNamedQueryAndNamedParam("sinekarta.documentType.FindByDescription","description",filter);
	}
	
	/**
	 * method to have a  list of all document types 
	 * 
	 * @return a list of all document types
	 */
	@SuppressWarnings("unchecked")
	public List<DocumentType> getDocumentTypes() {
		if (!initialized) { 
			throw new ConfigurationException("Invalid configuration");
		}
		return (List<DocumentType>) getHibernateTemplate().findByNamedQuery("sinekarta.documentType.FindAll");
	}

	public void save(Object o) {
		Session s = getSession();
		Transaction t = s.getTransaction();
		try {
			t.begin();
			s.save(o);
			s.flush();
			t.commit();
		} catch (RuntimeException e) {
			if (t!=null) t.rollback();
			throw e;
		}
	}

	public void update(Object o) {
		Session s = getSession();
		Transaction t = s.getTransaction();
		try {
			t.begin();
			s.update(o);
			s.flush();
			t.commit();
		} catch (RuntimeException e) {
			if (t!=null) t.rollback();
			throw e;
		}
	}

	public void delete(Object o) {
		Session s = getSession();
		Transaction t = s.getTransaction();
		try {
			t.begin();
			s.delete(o);
			s.flush();
			t.commit();
		} catch (RuntimeException e) {
			if (t!=null) t.rollback();
			throw e;
		}
	}

	public void verify() {
		if (sinekartaEditionService==null) {
			throw new ConfigurationException("Invalid configuration.");
		}
		initialized=true;
	}
	
	public void setSinekartaEditionService(
			SinekartaEditionService sinekartaEditionService) {
		this.sinekartaEditionService = sinekartaEditionService;
	}

}
