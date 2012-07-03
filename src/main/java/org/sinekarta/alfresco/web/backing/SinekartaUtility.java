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
package org.sinekarta.alfresco.web.backing;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.alfresco.service.cmr.action.ActionService;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.model.FileFolderService;
import org.alfresco.service.cmr.repository.ContentService;
import org.alfresco.service.cmr.repository.CopyService;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.NodeService;
import org.alfresco.service.cmr.repository.Path;
import org.alfresco.service.cmr.repository.ScriptService;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.cmr.security.AuthenticationService;
import org.alfresco.service.cmr.security.OwnableService;
import org.alfresco.service.cmr.security.PermissionService;
import org.alfresco.service.cmr.security.PersonService;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.web.bean.BrowseBean;
import org.alfresco.web.bean.NavigationBean;
import org.alfresco.web.bean.repository.Node;
import org.alfresco.web.bean.repository.Repository;
import org.jenia.faces.util.Util;
import org.sinekarta.alfresco.configuration.dao.SinekartaDao;
import org.sinekarta.alfresco.configuration.hibernate.DocumentType;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.util.Configuration;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeTools;

/**
 * utility backing bean for retrieving alfresco services and sinekarta dao
 * this bean has also the selectitem list of documenttype
 * 
 * @author andrea.tessaro
 *
 */
public class SinekartaUtility {

	private static final long serialVersionUID = 1L;

	transient private AuthenticationService authenticationService;
	transient private ActionService actionService;
	transient private ScriptService scriptService;
	transient private TransactionService transactionService;
	transient private NodeService nodeService;
	transient private FileFolderService fileFolderService;
	transient private SearchService searchService;
	transient private DictionaryService dictionaryService;
	transient private NamespaceService namespaceService;
	transient private PermissionService permissionService;
	transient private CopyService copyService;
	transient private OwnableService ownableService;
	transient private PersonService personService;
	transient private ContentService contentService;
	transient private SinekartaDao sinekartaDao;
	private BrowseBean browseBean;
	private NavigationBean navigator;
	
	private List<SelectItem> tipiDocumentoForAlfresco;
	private List<SelectItem> tipiDocumento;
	private List<SelectItem> tipiDocumentoAll;

	private List<SelectItem> lingue;
	private List<SelectItem> lingueAll;

	private List<SelectItem> smartCards;

	private String dataToAppletCertificateChoice;
	private String dataFromAppletCertificateChoice;
	private String dataToAppletSign;
	private String dataFromAppletSign;
	
	public static SinekartaUtility getCurrentInstance() {
		return (SinekartaUtility)Util.getFacesBean(Constants.SINEKARTA_UTILITY_BACKING_BEAN);
	}

	public SinekartaUtility() {
		super();
		List<DocumentType> tt = getSinekartaDao().getDocumentTypes();
		tipiDocumento=new ArrayList<SelectItem>();
		tipiDocumentoForAlfresco=new ArrayList<SelectItem>();
		for (DocumentType t: tt) {
			SelectItem si = new SelectItem(t,t.getDescription());
			tipiDocumento.add(si);
			SelectItem sifa = new SelectItem(Integer.toString(t.getId()),t.getDescription());
			tipiDocumentoForAlfresco.add(sifa);
		}
		SelectItem si = new SelectItem("_"," --- ");
		tipiDocumentoAll=new ArrayList<SelectItem>();
		tipiDocumentoAll.add(si);
		tipiDocumentoAll.addAll(tipiDocumento);

		Map<String,String> ll = Configuration.getInstance().getMappaLingueOcr();
		lingue=new ArrayList<SelectItem>();
		for (String l : ll.keySet()) {
			SelectItem so = new SelectItem(l,ll.get(l));
			lingue.add(so);
		}
		SelectItem so = new SelectItem("_"," --- ");
		lingueAll=new ArrayList<SelectItem>();
		lingueAll.add(so);
		lingueAll.addAll(lingue);

		Map<String,String> lls = Configuration.getInstance().getMappaSmartcardDrivers();
		smartCards=new ArrayList<SelectItem>();
		String[] llsa = lls.keySet().toArray(new String[0]);
		Arrays.sort(llsa);
		for (String l : llsa) {
			SelectItem sos = new SelectItem(l,lls.get(l));
			smartCards.add(sos);
		}
	}
	
	public String getDefaultSmartcardDriver() {
		String userId = getNavigator().getCurrentUser().getUserName();
		NodeRef person = getPersonService().getPerson(userId);
	
		String def = (String)getNodeService().getProperty(person, SinekartaModel.PROP_QNAME_SINEKARTA_SMARTCARD_DLL);
		if (def!=null) 
			return def;
		else 
			return Configuration.getInstance().getDefaultSmartcardDriver();
	}

	public void setDefaultSmartcardDriver(String defaultSmartcardDriver) {
	}

	public ScriptService getScriptService() {
		if (this.scriptService == null) {
			this.scriptService = Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getScriptService();
		}
		return this.scriptService;
	}

	public PersonService getPersonService() {
		if (this.personService == null) {
			this.personService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getPersonService();
		}
		return this.personService;
	}

	public AuthenticationService getAuthenticationService() {
		if (this.authenticationService == null) {
			this.authenticationService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getAuthenticationService();
		}
		return this.authenticationService;
	}

	public TransactionService getTransactionService() {
		if (this.transactionService == null) {
			this.transactionService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getTransactionService();
		}
		return this.transactionService;
	}

	public NodeService getNodeService() {
		if (this.nodeService == null) {
			this.nodeService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getNodeService();
		}
		return this.nodeService;
	}

	public FileFolderService getFileFolderService() {
		if (this.fileFolderService == null) {
			this.fileFolderService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getFileFolderService();
		}
		return this.fileFolderService;
	}

	public SearchService getSearchService() {
		if (this.searchService == null) {
			this.searchService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getSearchService();
		}
		return this.searchService;
	}

	public DictionaryService getDictionaryService() {
		if (this.dictionaryService == null) {
			this.dictionaryService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getDictionaryService();
		}
		return this.dictionaryService;
	}

	public NamespaceService getNamespaceService() {
		if (this.namespaceService == null) {
			this.namespaceService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getNamespaceService();
		}
		return this.namespaceService;
	}

	public PermissionService getPermissionService() {
		if (this.permissionService == null) {
			this.permissionService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getPermissionService();
		}
		return this.permissionService;
	}

	public CopyService getCopyService() {
		if (this.copyService == null) {
			this.copyService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getCopyService();
		}
		return this.copyService;
	}

	public OwnableService getOwnableService() {
		if (this.ownableService == null) {
			this.ownableService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getOwnableService();
		}
		return this.ownableService;
	}

	public ContentService getContentService() {
		if (this.contentService == null) {
			this.contentService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getContentService();
		}
		return this.contentService;
	}

	public ActionService getActionService() {
		if (this.actionService == null) {
			this.actionService = Repository.getServiceRegistry(
					FacesContext.getCurrentInstance()).getActionService();
		}
		return this.actionService;
	}

	public SinekartaDao getSinekartaDao() {
		if (this.sinekartaDao == null) {
			this.sinekartaDao = (SinekartaDao)Repository.getServiceRegistry(FacesContext.getCurrentInstance()).getService(SinekartaModel.QNAME_SINEKARTA_DAO);
		}
		return sinekartaDao;
	}

	public List<SelectItem> getTipiDocumento() {
		return tipiDocumento;
	}

	public List<SelectItem> getTipiDocumentoAll() {
		return tipiDocumentoAll;
	}

	public Node getCompanyHome() {
		return navigator.getCompanyHomeNode();
	}

	public Path getCompanyHomePath() {
		return navigator.getCompanyHomeNode().getNodePath();
	}
	
	public String getPrefixedCompanyHome() {
		return NodeTools.translateNamespacePath(getNamespaceService(), navigator.getCompanyHomeNode().getNodePath());
	}

	public String getCompanyHomePathForDisplay() {
		return NodeTools.translatePath(getNodeService(), navigator.getCompanyHomeNode().getNodePath());
	}

	public BrowseBean getBrowseBean() {
		return browseBean;
	}

	public void setBrowseBean(BrowseBean browseBean) {
		this.browseBean = browseBean;
	}

	public NavigationBean getNavigator() {
		return navigator;
	}

	public void setNavigator(NavigationBean navigator) {
		this.navigator = navigator;
	}

	public List<SelectItem> getLingue() {
		return lingue;
	}

	public List<SelectItem> getLingueAll() {
		return lingueAll;
	}

	public List<SelectItem> getTipiDocumentoForAlfresco() {
		return tipiDocumentoForAlfresco;
	}

	public List<SelectItem> getSmartCards() {
		return smartCards;
	}

	public String getDataToAppletCertificateChoice() {
		return dataToAppletCertificateChoice;
	}

	public void setDataToAppletCertificateChoice(
			String dataToAppletCertificateChoice) {
		this.dataToAppletCertificateChoice = dataToAppletCertificateChoice;
	}

	public String getDataFromAppletCertificateChoice() {
		return dataFromAppletCertificateChoice;
	}

	public void setDataFromAppletCertificateChoice(
			String dataFromAppletCertificateChoice) {
		this.dataFromAppletCertificateChoice = dataFromAppletCertificateChoice;
	}

	public String getDataToAppletSign() {
		return dataToAppletSign;
	}

	public void setDataToAppletSign(String dataToAppletSign) {
		this.dataToAppletSign = dataToAppletSign;
	}

	public String getDataFromAppletSign() {
		return dataFromAppletSign;
	}

	public void setDataFromAppletSign(String dataFromAppletSign) {
		this.dataFromAppletSign = dataFromAppletSign;
	}

}
