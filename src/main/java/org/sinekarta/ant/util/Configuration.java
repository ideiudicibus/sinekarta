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
package org.sinekarta.ant.util;


/**
 * this class is a singleton! use Configuration.getInstance() to get current instance
 * Configuration is read from alfresco/extension/sinekarta-repository.properties in classloader
 * The file found in classes folder is the last read
 * 
 * 
 * @author andrea.tessaro
 *
 */
public class Configuration {

	private String authenticationService;
	private String repositoryService;
    private String contentService;
    private String authoringService;
    private String classificationService;
    private String actionService;
    private String accessControlService;
    private String administrationService;
    private String dictionaryService;
    
    private int timeoutMilliseconds = 60000;

	
	public Configuration(String host, int port, int timeoutMilliseconds) throws Exception {
		
		try {
			authenticationService = "http://"+host+":"+port+"/alfresco/api/AuthenticationService";
			repositoryService = "http://"+host+":"+port+"/alfresco/api/RepositoryService";
		    contentService = "http://"+host+":"+port+"/alfresco/api/ContentService";
		    authoringService = "http://"+host+":"+port+"/alfresco/api/AuthoringService";
		    classificationService = "http://"+host+":"+port+"/alfresco/api/ClassificationService";
		    actionService = "http://"+host+":"+port+"/alfresco/api/ActionService";
		    accessControlService = "http://"+host+":"+port+"/alfresco/api/AccessControlService";
		    administrationService = "http://"+host+":"+port+"/alfresco/api/AdministrationService";
		    dictionaryService = "http://"+host+":"+port+"/alfresco/api/DictionaryService";
		    this.timeoutMilliseconds = timeoutMilliseconds;
		} catch (Exception e) {
			String msg = "unable to parse sinekarta-repository.properties (" + e.getClass().getName() + " : " + e.getMessage() + ")";
			throw new Exception(msg,e);
		}

	}
	
	public String getAuthenticationService() {
		return authenticationService;
	}

	public String getRepositoryService() {
		return repositoryService;
	}

	public String getContentService() {
		return contentService;
	}

	public String getAuthoringService() {
		return authoringService;
	}

	public String getClassificationService() {
		return classificationService;
	}

	public String getActionService() {
		return actionService;
	}

	public String getAccessControlService() {
		return accessControlService;
	}

	public String getAdministrationService() {
		return administrationService;
	}

	public String getDictionaryService() {
		return dictionaryService;
	}

	public int getTimeoutMilliseconds() {
		return timeoutMilliseconds;
	}

}
