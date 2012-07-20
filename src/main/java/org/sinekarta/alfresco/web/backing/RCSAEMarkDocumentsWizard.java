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

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.web.app.Application;
import org.alfresco.web.bean.wizard.BaseWizardBean;
import org.apache.log4j.Logger;
import org.jenia.faces.util.Util;
import org.sinekarta.alfresco.action.AEMarkDocumentAdd;
import org.sinekarta.alfresco.action.AEMarkDocumentInit;
import org.sinekarta.alfresco.action.AEMarkDocumentPrepare;
import org.sinekarta.alfresco.action.DocumentDigitalSignatureInit;
import org.sinekarta.alfresco.action.DocumentTimestampAEMarkApply;
import org.sinekarta.alfresco.action.DocumentTimestampAEMarkInit;
import org.sinekarta.alfresco.action.DocumentTimestampAEMarkPrepare;
import org.sinekarta.alfresco.exception.MarkDocumentAlreadyExistsException;
import org.sinekarta.alfresco.exception.MarkFailedException;
import org.sinekarta.alfresco.exception.SignFailedException;
import org.sinekarta.alfresco.model.SinekartaModel;
import org.sinekarta.alfresco.model.agenziaEntrate.Comunicazione;
import org.sinekarta.alfresco.model.agenziaEntrate.DataImpegno;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiAnagrType;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiArchivioInformatico;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiFornitura;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiIntermediarioTrasmissione;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiNascita;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiPersonaFisica;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiTitolareContabilita;
import org.sinekarta.alfresco.model.agenziaEntrate.DatiTrasmissione;
import org.sinekarta.alfresco.model.agenziaEntrate.Delegati;
import org.sinekarta.alfresco.model.agenziaEntrate.DomFiscaleSedeLegale;
import org.sinekarta.alfresco.model.agenziaEntrate.LuogoConservazione;
import org.sinekarta.alfresco.model.agenziaEntrate.ObjectFactory;
import org.sinekarta.alfresco.util.Constants;
import org.sinekarta.alfresco.util.NodeRefWrapper;
import org.sinekarta.alfresco.util.NodeTools;

/**
 * timestamp mark wizard
 * 
 * @author andrea.tessaro
 *
 */
public class RCSAEMarkDocumentsWizard extends BaseWizardBean {
	
	private static final String PERSONA = "P";
	private static final String DENOMINAZIONE = "D";
	protected static final String BUNDLE_SUMMARY = "summary";
	protected static final String BUNDLE_RCS_AEMARK = "bundle.sinekarta-rcsAEMark";
	
	protected static final String STEP_NAME_DOCUMENT_ATTRIBUTES = "documentAttributes";
	protected static final String STEP_NAME_DOCUMENT_SELECTION = "documentSelection";
	protected static final String STEP_NAME_CERTIFICATE_CHOICE = "certificateChoice";
	protected static final String STEP_NAME_SIGN = "sign";
	protected static final long serialVersionUID = 1L;
	protected static Logger tracer = Logger.getLogger(RCSAEMarkDocumentsWizard.class);

	protected transient ResourceBundle resourceBundle = ResourceBundle.getBundle(BUNDLE_RCS_AEMARK, org.jenia.faces.util.Util.getFacesContext().getViewRoot().getLocale(), Util.class.getClassLoader());

	private Date documentDate;
	private Date documentDateFrom;
	private Date documentDateTo;
	private String documentName;
	private boolean subspace;
	private boolean alreadySent;
	private boolean error;
	
	private String markFileName;
	private String markDescription;
	
	private List<NodeRefWrapper> documents;
	private List<NodeRefWrapper> selectedDocuments;
	
	private String markDocumentArea;
	
	private Comunicazione comunicazione;
	private List<SelectItem> tipiComunicazione;
	private List<SelectItem> tipiImpegno;
	
	private DatiAnagrType delegatoConservazioneSelected;
	private String intermediarioTrasmissione;

	private void init() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		error=false;
		subspace=true;
		alreadySent=false;
		selectedDocuments=new ArrayList<NodeRefWrapper>();
		documents=null;
		documentName=null;
		documentDate=null;
		documentDateFrom=null;
		documentDateTo=null;
		su.setDataToAppletCertificateChoice(null);
		su.setDataFromAppletCertificateChoice(null);
		su.setDataToAppletSign(null);
		su.setDataFromAppletSign(null);
		markFileName=null;
		markDescription=null;
		// creazione infrastruttura per diting comunicazione
		ObjectFactory of = new ObjectFactory();
		// creo la comunicazione vuota
		comunicazione = of.createComunicazione();
		String archivioPath = NodeTools.translateNamespacePath(su.getNamespaceService(),su.getCompanyHomePath());
		NodeRef archivio = NodeTools.getArchivio(su.getNodeService(), su.getSearchService(), archivioPath);
        // prendo i dati di trasmissione salvati a livello di archivio
		String datiTrasmissioneStr = (String)su.getNodeService().getProperty(archivio, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATITRASMISSIONE);
        // prendo i dati del titolare della contabilita' salvati a livello di archivio
        String datiTitolareContabilitaStr = (String)su.getNodeService().getProperty(archivio, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATITITOLARECONTABILITA);
        String userName = su.getAuthenticationService().getCurrentUserName();
        NodeRef personRef = su.getPersonService().getPerson(userName);
        // prendo i dati del RCS salvati a livello di utente
        String datiResponsabileConservazioneStr = (String)su.getNodeService().getProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATIRESPONSABILECONSERVAZIONE);
        // prendo i dati del RCS salvati a livello di utente
        String datiDelegatiConservazioneStr = (String)su.getNodeService().getProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATIDELEGATICONSERVAZIONE);
        // prendo i dati di conservazione salvati a livello di utente
        String luogoConservazioneStr = (String)su.getNodeService().getProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_AE_LUOGOCONSERVAZIONE);
        // ricostruisco gli oggetti partendo dagli XML recuperati
        DatiTrasmissione datiTrasmissione=null;
        DatiTitolareContabilita datiTitolareContabilita=null;
        DatiAnagrType datiResponsabileConservazione=null;
        List<DatiAnagrType> datiDelegatiConservazione=null;
        LuogoConservazione luogoConservazione=null;
        try {
			JAXBContext jc = JAXBContext.newInstance(DatiTrasmissione.class);
			Unmarshaller u = jc.createUnmarshaller();
			ByteArrayInputStream bais = new ByteArrayInputStream(datiTrasmissioneStr.getBytes());
			datiTrasmissione = (DatiTrasmissione)u.unmarshal(bais);
			if (datiTrasmissione.getDatiIntermediarioTrasmissione()!=null)
				intermediarioTrasmissione="S";
			else 
				intermediarioTrasmissione="N";
        } catch (Exception e) {
        	datiTrasmissione = of.createDatiTrasmissione();
			intermediarioTrasmissione="N";
        }
        try {
			JAXBContext jc = JAXBContext.newInstance(DatiTitolareContabilita.class);
			Unmarshaller u = jc.createUnmarshaller();
			ByteArrayInputStream bais = new ByteArrayInputStream(datiTitolareContabilitaStr.getBytes());
			datiTitolareContabilita = (DatiTitolareContabilita)u.unmarshal(bais);
			if (datiTitolareContabilita.getDatiAnagr().getDatiPersonaFisica()!=null)
				datiTitolareContabilita.getDatiAnagr().setPersonaODenominazione(PERSONA);
			else
				datiTitolareContabilita.getDatiAnagr().setPersonaODenominazione(DENOMINAZIONE);
        } catch (Exception e) {
        	datiTitolareContabilita = of.createDatiTitolareContabilita();
//          default titolare contabilita'
            DatiAnagrType da = of.createDatiAnagrType();
            da.setPersonaODenominazione(DENOMINAZIONE);
            datiTitolareContabilita.setDatiAnagr(da);
            DomFiscaleSedeLegale dfsl = of.createDomFiscaleSedeLegale();
            da.setDomFiscaleSedeLegale(dfsl);
            dfsl.setIndirizzo(of.createIndirizzo());
       }
        try {
			JAXBContext jc = JAXBContext.newInstance(DatiAnagrType.class);
			Unmarshaller u = jc.createUnmarshaller();
			ByteArrayInputStream bais = new ByteArrayInputStream(datiResponsabileConservazioneStr.getBytes());
			datiResponsabileConservazione = (DatiAnagrType)u.unmarshal(bais);
			if (datiResponsabileConservazione.getDatiPersonaFisica()!=null)
				datiResponsabileConservazione.setPersonaODenominazione(PERSONA);
			else
				datiResponsabileConservazione.setPersonaODenominazione(DENOMINAZIONE);
        } catch (Exception e) {
        	datiResponsabileConservazione = of.createDatiAnagrType();
//          default responsabile conservazione
            DomFiscaleSedeLegale dfslrc = of.createDomFiscaleSedeLegale();
            datiResponsabileConservazione.setDomFiscaleSedeLegale(dfslrc);
            datiResponsabileConservazione.setPersonaODenominazione(PERSONA);
    		DatiPersonaFisica dpf = of.createDatiPersonaFisica();
    		datiResponsabileConservazione.setDatiPersonaFisica(dpf);
            dfslrc.setIndirizzo(of.createIndirizzo());
    		DatiNascita dn = of.createDatiNascita();
    		dn.setData(of.createData());
            dpf.setDatiNascita(dn);
        }
        try {
			JAXBContext jc = JAXBContext.newInstance(Delegati.class);
			Unmarshaller u = jc.createUnmarshaller();
			ByteArrayInputStream bais = new ByteArrayInputStream(datiDelegatiConservazioneStr.getBytes());
			Delegati dd = (Delegati)u.unmarshal(bais);
			datiDelegatiConservazione = dd.getDatiDelegatoConservazione(); 
			if (datiDelegatiConservazione!=null) {
				for (DatiAnagrType delegato : datiDelegatiConservazione) {
					if (delegato.getDatiPersonaFisica()!=null)
						delegato.setPersonaODenominazione(PERSONA);
					else
						delegato.setPersonaODenominazione(DENOMINAZIONE);
				}
			}
        } catch (Exception e) {
        	// nothing to do...
        }
        DatiArchivioInformatico datiArchivioInformatico = of.createDatiArchivioInformatico();
        try {
			JAXBContext jc = JAXBContext.newInstance(LuogoConservazione.class);
			Unmarshaller u = jc.createUnmarshaller();
			ByteArrayInputStream bais = new ByteArrayInputStream(luogoConservazioneStr.getBytes());
			luogoConservazione = (LuogoConservazione)u.unmarshal(bais);
            datiArchivioInformatico.setLuogoConservazione(luogoConservazione);
        } catch (Exception e) {
        	luogoConservazione = of.createLuogoConservazione();
            datiArchivioInformatico.setLuogoConservazione(luogoConservazione);
            luogoConservazione.setIndirizzo(of.createIndirizzo());
        }
        comunicazione.setDatiTrasmissione(datiTrasmissione);
        comunicazione.setDatiTitolareContabilita(datiTitolareContabilita);
        comunicazione.setDatiResponsabileConservazione(datiResponsabileConservazione);
        if (datiDelegatiConservazione!=null) {
	        for (DatiAnagrType delegato : datiDelegatiConservazione) {
	        	comunicazione.getDatiDelegatoConservazione().add(delegato);
	        }
        }
        comunicazione.setDatiArchivioInformatico(datiArchivioInformatico);
        DatiFornitura datiFornitura = of.createDatiFornitura();
        datiFornitura.setCodiceFornitura("IMP00");
		Date now = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		datiFornitura.setPeriodoImposta(c.get(Calendar.YEAR)-1);
        comunicazione.setDatiFornitura(datiFornitura);
//		combo tipi comunicazione
        tipiComunicazione = new ArrayList<SelectItem>();
        tipiComunicazione.add(new SelectItem(1,resourceBundle.getString("tipoComunicazione_1")));
        tipiComunicazione.add(new SelectItem(2,resourceBundle.getString("tipoComunicazione_2")));
        tipiComunicazione.add(new SelectItem(3,resourceBundle.getString("tipoComunicazione_3")));
        tipiImpegno = new ArrayList<SelectItem>();
        tipiImpegno.add(new SelectItem(1,resourceBundle.getString("tipoImpegno_1")));
        tipiImpegno.add(new SelectItem(2,resourceBundle.getString("tipoImpegno_2")));
	}
	
	public String titolareContabilitaPersonaODenominazioneP() {
		ObjectFactory of = new ObjectFactory();
		comunicazione.getDatiTitolareContabilita().getDatiAnagr().setPersonaODenominazione(PERSONA);
		comunicazione.getDatiTitolareContabilita().getDatiAnagr().setDenominazione(null);
		DatiPersonaFisica dpf = of.createDatiPersonaFisica();
		comunicazione.getDatiTitolareContabilita().getDatiAnagr().setDatiPersonaFisica(dpf);
		DatiNascita dn = of.createDatiNascita();
		dpf.setDatiNascita(dn);
		dn.setData(of.createData());
		return null;
	}
	
	public String titolareContabilitaPersonaODenominazioneD() {
		comunicazione.getDatiTitolareContabilita().getDatiAnagr().setPersonaODenominazione(DENOMINAZIONE);
		comunicazione.getDatiTitolareContabilita().getDatiAnagr().setDatiPersonaFisica(null);
		comunicazione.getDatiTitolareContabilita().getDatiAnagr().setDenominazione(null);
		return null;
	}
	
	public String responsabileConservazionePersonaODenominazioneP() {
		ObjectFactory of = new ObjectFactory();
		comunicazione.getDatiResponsabileConservazione().setPersonaODenominazione(PERSONA);
		comunicazione.getDatiResponsabileConservazione().setDenominazione(null);
		DatiPersonaFisica dpf = of.createDatiPersonaFisica();
		comunicazione.getDatiResponsabileConservazione().setDatiPersonaFisica(dpf);
		DatiNascita dn = of.createDatiNascita();
		dpf.setDatiNascita(dn);
		dn.setData(of.createData());
		return null;
	}
	
	public String responsabileConservazionePersonaODenominazioneD() {
		comunicazione.getDatiResponsabileConservazione().setPersonaODenominazione(DENOMINAZIONE);
		comunicazione.getDatiResponsabileConservazione().setDatiPersonaFisica(null);
		comunicazione.getDatiResponsabileConservazione().setDenominazione(null);
		return null;
	}
	
	public String delegatoConservazionePersonaODenominazioneP() {
		ObjectFactory of = new ObjectFactory();
		delegatoConservazioneSelected.setPersonaODenominazione(PERSONA);
		delegatoConservazioneSelected.setDenominazione(null);
		DatiPersonaFisica dpf = of.createDatiPersonaFisica();
		delegatoConservazioneSelected.setDatiPersonaFisica(dpf);
		DatiNascita dn = of.createDatiNascita();
		dpf.setDatiNascita(dn);
		dn.setData(of.createData());
		return null;
	}
	
	public String delegatoConservazionePersonaODenominazioneD() {
		delegatoConservazioneSelected.setPersonaODenominazione(DENOMINAZIONE);
		delegatoConservazioneSelected.setDatiPersonaFisica(null);
		delegatoConservazioneSelected.setDenominazione(null);
		return null;
	}
	
	public String addDelegatoConservazione() {
		ObjectFactory of = new ObjectFactory();
//      default responsabile conservazione
		DatiAnagrType delegato = of.createDatiAnagrType();
        DomFiscaleSedeLegale dfslrc = of.createDomFiscaleSedeLegale();
        delegato.setDomFiscaleSedeLegale(dfslrc);
        dfslrc.setIndirizzo(of.createIndirizzo());
		DatiPersonaFisica dpf = of.createDatiPersonaFisica();
		DatiNascita dn = of.createDatiNascita();
		dn.setData(of.createData());
        dpf.setDatiNascita(dn);
		delegato.setDatiPersonaFisica(dpf);
        delegato.setPersonaODenominazione(PERSONA);
        comunicazione.getDatiDelegatoConservazione().add(delegato);
		return null;
	}
	
	public String removeDelegatoConservazione() {
		comunicazione.getDatiDelegatoConservazione().remove(delegatoConservazioneSelected);
		return null;
	}
	
	public String utilizzaIntermediarioNo() {
		comunicazione.getDatiTrasmissione().setDatiIntermediarioTrasmissione(null);
        intermediarioTrasmissione="N";
		return null;
	}
	
	public String utilizzaIntermediarioSi() {
		ObjectFactory of = new ObjectFactory();
		DatiIntermediarioTrasmissione di = of.createDatiIntermediarioTrasmissione();
		DataImpegno datai = of.createDataImpegno();
		Date now = new Date();
		Calendar c = Calendar.getInstance();
		c.setTime(now);
		datai.setMese(c.get(Calendar.MONTH)+1);
		datai.setAnno(c.get(Calendar.YEAR));
		datai.setGiorno(c.get(Calendar.DAY_OF_MONTH));
		di.setDataImpegno(datai);
		comunicazione.getDatiTrasmissione().setDatiIntermediarioTrasmissione(di);
        intermediarioTrasmissione="S";
		return null;
	}
	
	@Override
	protected String getErrorOutcome(Throwable exception) {
		error=true;
		return super.getErrorOutcome(exception);
	}

	@Override
	public void init(Map<String, String> parameters) {
		super.init(parameters);
		init();
	}

	public String reset() {
		init();
		return null;
	}
		
	/**
	 * called an finish button is pressed
	 * this method apply the sign, create the p7m and (calling the TSA) create the m7m
	 * the mark document is saved into archive
	 * 
	 */
	protected String finishImpl(FacesContext context, String outcome)
			throws Exception {

		// interpretazione area ricevuta dall'applet
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();

		// conversione documento in PDF/A
		Action documentTimestampAEMarkApply = su.getActionService().createAction(DocumentTimestampAEMarkApply.ACTION_NAME_DOCUMENT_RCS_AEMARK_SIGN_APPLY);
		documentTimestampAEMarkApply.setParameterValue(DocumentTimestampAEMarkApply.PARAM_MARK_AREA, markDocumentArea);
		documentTimestampAEMarkApply.setParameterValue(DocumentTimestampAEMarkApply.PARAM_CLIENT_AREA, su.getDataFromAppletSign());
		JAXBContext jc = JAXBContext.newInstance(Comunicazione.class);
        Marshaller m = jc.createMarshaller();
        m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
        StringWriter sw = new StringWriter();
        m.marshal(comunicazione, sw);
		documentTimestampAEMarkApply.setParameterValue(DocumentTimestampAEMarkApply.PARAM_XML_AREA, sw.toString());
		try {
			su.getActionService().executeAction(documentTimestampAEMarkApply, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		
		return outcome;
	}
	
	/**
	 * using documentType calculate the folder into which save the timestamp mark file
	 */
	protected void initMarkDocument() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		
		// invoking action for calculating mark folder
		Action aemarkFolderPrepare = su.getActionService().createAction(AEMarkDocumentInit.ACTION_NAME_AEMARK_DOCUMENT_INIT);
		aemarkFolderPrepare.setParameterValue(AEMarkDocumentInit.PARAM_DOCUMENT_DATE, documentDate);
		try {
			su.getActionService().executeAction(aemarkFolderPrepare, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(Throwable t) {
			tracer.error("Unable to calculate aemark folder : " + t.getMessage(),t);
			throw new MarkFailedException("Unable to calculate aemark folder : " + t.getMessage(),t);
		}
		markDocumentArea = (String)aemarkFolderPrepare.getParameterValue(AEMarkDocumentInit.PARAM_RESULT);
		
	}
	
	/**
	 * prepare xml document to be signed and marked
	 * see specifications for more details
	 */
	protected void prepareMarkDocument() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		
		for (NodeRefWrapper node : selectedDocuments) {
			// invoking action for producing timestamp mark file
			Action markDocumentAdd = su.getActionService().createAction(AEMarkDocumentAdd.ACTION_NAME_AEMARK_DOCUMENT_ADD);
			markDocumentAdd.setParameterValue(AEMarkDocumentAdd.PARAM_MARK_AREA, markDocumentArea);
			try {
				su.getActionService().executeAction(markDocumentAdd, node.getNodeRef(), false, false);
			} catch(Throwable t) {
				tracer.error("Unable to prepare mark documenr : " + t.getMessage(),t);
				throw new MarkFailedException("Unable to prepare mark documenr : " + t.getMessage(),t);
			}
			markDocumentArea = (String)markDocumentAdd.getParameterValue(AEMarkDocumentAdd.PARAM_RESULT);
		}

		
		// preparazione documento di marca temporale
		Action markDocumentPrepare = su.getActionService().createAction(AEMarkDocumentPrepare.ACTION_NAME_AEMARK_DOCUMENT_PREPARE);
		markDocumentPrepare.setParameterValue(AEMarkDocumentPrepare.PARAM_MARK_AREA, markDocumentArea);
		markDocumentPrepare.setParameterValue(AEMarkDocumentPrepare.PARAM_MARK_DESCRIPTIONR, markDescription);
		markDocumentPrepare.setParameterValue(AEMarkDocumentPrepare.PARAM_MARK_FILENAME, markFileName);
		try {
			su.getActionService().executeAction(markDocumentPrepare, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(MarkDocumentAlreadyExistsException e) {
			throw e;
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		markDocumentArea = (String)markDocumentPrepare.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT);

	}
	
	/**
	 * read data received from applet (certificate choice) and prepare data
	 * to send to applet for sign
	 * @param dataFromApplet applet data of certificate choice
	 * @return data to send to applet for signing
	 */
	protected void prepareDataToAppletSign() {

		// calcolo impronte documenti selezionati
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		// conversione documento in PDF/A
		Action documentTimestampMarkPrepare = su.getActionService().createAction(DocumentTimestampAEMarkPrepare.ACTION_NAME_DOCUMENT_TIMESTAMP_AEMARK_PREPARE);
		documentTimestampMarkPrepare.setParameterValue(DocumentTimestampAEMarkPrepare.PARAM_MARK_AREA, markDocumentArea);
		documentTimestampMarkPrepare.setParameterValue(DocumentTimestampAEMarkPrepare.PARAM_CLIENT_AREA, su.getDataFromAppletCertificateChoice());
		try {
			su.getActionService().executeAction(documentTimestampMarkPrepare, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(MarkDocumentAlreadyExistsException e) {
			throw e;
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		su.setDataToAppletSign((String)documentTimestampMarkPrepare.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT));

	}
	
	/**
	 * prepare the area to pass to appet for certificate choice
	 * 
	 * @return encoded64 string to pass to applet
	 */
	protected void prepareDataToAppletCertificateChoice() {
		// calcolo impronte documenti selezionati
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		// conversione documento in PDF/A
		Action documentTimestampMarkPrepare = su.getActionService().createAction(DocumentTimestampAEMarkInit.ACTION_NAME_DOCUMENT_TIMESTAMP_AEMARK_INIT);
		documentTimestampMarkPrepare.setParameterValue(DocumentTimestampAEMarkInit.PARAM_MARK_AREA, markDocumentArea);
		try {
			su.getActionService().executeAction(documentTimestampMarkPrepare, browseBean.getActionSpace().getNodeRef(), false, false);
		} catch(Throwable t) {
			tracer.error("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
			throw new SignFailedException("Unable to initialize digital signature alfresco action service : " + t.getMessage(),t);
		}
		su.setDataToAppletCertificateChoice( (String)documentTimestampMarkPrepare.getParameterValue(DocumentDigitalSignatureInit.PARAM_RESULT));
	}
	
	@Override
	public String back() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		if (stepName.equals(STEP_NAME_DOCUMENT_ATTRIBUTES)) {

			// nothing to do...
			
		} else if (stepName.equals(STEP_NAME_DOCUMENT_SELECTION)) {
			
			if (documentDate==null) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "invalidDocumentDate", "invalidDocumentDateDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markFileName==null || markFileName.trim().equals("")) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			// controllo se il nome del file contiene caratteri invalidi
			markFileName = markFileName.trim();
			if (markFileName.indexOf('"')!=-1 ||
				markFileName.indexOf('*')!=-1 ||
				markFileName.indexOf('\\')!=-1 ||
				markFileName.indexOf('>')!=-1 ||
				markFileName.indexOf('<')!=-1 ||
				markFileName.indexOf('?')!=-1 ||
				markFileName.indexOf('/')!=-1 ||
				markFileName.indexOf(':')!=-1 ||
				markFileName.indexOf('|')!=-1 ||
				markFileName.endsWith(".")) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markDescription==null || markDescription.trim().equals("")) {
				markDescription = markFileName;
			}
			
		} else if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			if (selectedDocuments.isEmpty()) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "wrongSelection", "wrongSelectionDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() + 1);
				return null;
			}

			initMarkDocument();
			
			prepareDataToAppletCertificateChoice();

			su.setDataFromAppletCertificateChoice(null);

		} else if (stepName.equals(STEP_NAME_SIGN)) {
			
			try {
				prepareMarkDocument();
			} catch (Exception e) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "markFilePrepareError", "markFilePrepareErrorDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			try {
				prepareDataToAppletSign();
			} catch (MarkDocumentAlreadyExistsException e) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "markFileNameAlreadyExists", "markFileNameAlreadyExistsDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}

			su.setDataFromAppletSign(null);
			
		}

		return null;
	}

	public String next() {
		String stepName = Application.getWizardManager().getCurrentStepName();
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		if (stepName.equals(STEP_NAME_DOCUMENT_ATTRIBUTES)) {

			// nothing to do...
			
		} else if (stepName.equals(STEP_NAME_DOCUMENT_SELECTION)) {
			
			String datiTrasmissioneStr = null;
	        String datiTitolareContabilitaStr = null;
	        String datiResponsabileConservazioneStr = null;
	        String datiDelegatiConservazioneStr = null;
	        String luogoConservazioneStr = null;
	        try {
				JAXBContext jc = JAXBContext.newInstance(DatiTrasmissione.class);
				Marshaller m = jc.createMarshaller();
				StringWriter w = new StringWriter();
				m.marshal(comunicazione.getDatiTrasmissione(), w);
				datiTrasmissioneStr=w.toString();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	// nothing to do...
	        }
	        try {
				JAXBContext jc = JAXBContext.newInstance(DatiTitolareContabilita.class);
				Marshaller m = jc.createMarshaller();
				StringWriter w = new StringWriter();
				m.marshal(comunicazione.getDatiTitolareContabilita(), w);
				datiTitolareContabilitaStr=w.toString();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	// nothing to do...
	        }
	        try {
				JAXBContext jc = JAXBContext.newInstance(DatiAnagrType.class);
				Marshaller m = jc.createMarshaller();
				StringWriter w = new StringWriter();
				m.marshal(comunicazione.getDatiResponsabileConservazione(), w);
				datiResponsabileConservazioneStr=w.toString();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	// nothing to do...
	        }
	        try {
				JAXBContext jc = JAXBContext.newInstance(Delegati.class);
				Marshaller m = jc.createMarshaller();
				StringWriter w = new StringWriter();
				ObjectFactory of = new ObjectFactory();
				Delegati dd = of.createDelegati();
				dd.setDatiDelegatoConservazione(comunicazione.getDatiDelegatoConservazione());
				m.marshal(dd, w);
				datiDelegatiConservazioneStr=w.toString();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	// nothing to do...
	        }
	        try {
				JAXBContext jc = JAXBContext.newInstance(LuogoConservazione.class);
				Marshaller m = jc.createMarshaller();
				StringWriter w = new StringWriter();
				m.marshal(comunicazione.getDatiArchivioInformatico().getLuogoConservazione(), w);
				luogoConservazioneStr=w.toString();
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	// nothing to do...
	        }
	        // salvo i dati di trasmissione a livello di archivio
			String archivioPath = NodeTools.translateNamespacePath(su.getNamespaceService(),su.getCompanyHomePath());
			NodeRef archivio = NodeTools.getArchivio(su.getNodeService(), su.getSearchService(), archivioPath);
			su.getNodeService().setProperty(archivio, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATITRASMISSIONE,datiTrasmissioneStr);
	        // salvo i dati del titolare della contabilita' a livello di archivio
	        su.getNodeService().setProperty(archivio, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATITITOLARECONTABILITA, datiTitolareContabilitaStr);
	        String userName = su.getAuthenticationService().getCurrentUserName();
	        NodeRef personRef = su.getPersonService().getPerson(userName);
	        // salvo i dati del RCS a livello di utente
	        su.getNodeService().setProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATIRESPONSABILECONSERVAZIONE, datiResponsabileConservazioneStr);
	        // salvo i dati dei delegati RCS a livello di utente
	        su.getNodeService().setProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_AE_DATIDELEGATICONSERVAZIONE, datiDelegatiConservazioneStr);
	        // salvo i dati di conservazione a livello di utente
	        su.getNodeService().setProperty(personRef, SinekartaModel.PROP_QNAME_SINEKARTA_AE_LUOGOCONSERVAZIONE, luogoConservazioneStr);

	        if (documentDate==null) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "invalidDocumentDate", "invalidDocumentDateDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markFileName==null || markFileName.trim().equals("")) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			// controllo se il nome del file contiene caratteri invalidi
			markFileName = markFileName.trim();
			if (markFileName.indexOf('"')!=-1 ||
				markFileName.indexOf('*')!=-1 ||
				markFileName.indexOf('\\')!=-1 ||
				markFileName.indexOf('>')!=-1 ||
				markFileName.indexOf('<')!=-1 ||
				markFileName.indexOf('?')!=-1 ||
				markFileName.indexOf('/')!=-1 ||
				markFileName.indexOf(':')!=-1 ||
				markFileName.indexOf('|')!=-1 ||
				markFileName.endsWith(".")) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "invalidMarkFileName", "invalidMarkFileNameDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			if (markDescription==null || markDescription.trim().equals("")) {
				markDescription = markFileName;
			}
			
		} else if (stepName.equals(STEP_NAME_CERTIFICATE_CHOICE)) {
			
			if (selectedDocuments.isEmpty()) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "wrongSelection", "wrongSelectionDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			initMarkDocument();
			
			prepareDataToAppletCertificateChoice();

			su.setDataFromAppletCertificateChoice(null);

		} else if (stepName.equals(STEP_NAME_SIGN)) {
			
			try {
				prepareMarkDocument();
			} catch (Exception e) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "markFilePrepareError", "markFilePrepareErrorDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}
			
			try {
				prepareDataToAppletSign();
			} catch (MarkDocumentAlreadyExistsException e) {
				Util.addFatalMessage(BUNDLE_RCS_AEMARK, "markFileNameAlreadyExists", "markFileNameAlreadyExistsDesc");
				Application.getWizardManager().getState().setCurrentStep(Application.getWizardManager().getCurrentStep() - 1);
				return null;
			}

			su.setDataFromAppletSign(null);
			
		}

		return null;
	}

	public String getSummary() {
		Object[] args = new Object[] {selectedDocuments.size()};
		return MessageFormat.format(resourceBundle.getString(BUNDLE_SUMMARY), args);
	}
	
	/**
	 * search method (jsf backing bean method) to search for document that can be marked
	 * 
	 * @return jsf outcome action
	 */
	public String search() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		StoreRef storeRef = StoreRef.STORE_REF_WORKSPACE_SPACESSTORE;
		StringBuffer lucenePath=new StringBuffer(NodeTools.translateNamespacePath(su.getNamespaceService(), browseBean.getActionSpace().getNodePath()));
		if (subspace){
			lucenePath.append("//*");
		} else {
			lucenePath.append("/*");
		}
		StringBuffer querySb = new StringBuffer("PATH:\""+lucenePath.toString()+"\" AND TYPE:\""+Constants.STANDARD_CONTENT_MODEL_PREFIX+":content\"" +
							" AND ASPECT:\""+SinekartaModel.ASPECT_QNAME_TIMESTAMP_MARK+"\""
							);

		querySb.append(" AND -ASPECT:\""+SinekartaModel.ASPECT_QNAME_TIMESTAMP_AEMARK+"\"");

		if (!alreadySent){
			querySb.append(" AND -ASPECT:\""+SinekartaModel.ASPECT_QNAME_AEMARK_CREATED+"\"");
		} 
		
		if (documentName!=null && !documentName.trim().equals("")){
			String[] words = documentName.split(" ");
			querySb.append(" AND ( ");
			for (int i=0;i<words.length;i++) {
				String word = words[i];
				querySb.append(" @"+Constants.STANDARD_CONTENT_MODEL_PREFIX_EXTENDED+"name:\""+word+"\"");
			}
			querySb.append(" ) ");
		} 
		if (documentDateFrom!=null && documentDateTo==null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_DOCUMENT_DATE+":["+sdf.format(documentDateFrom)+" TO MAX]");
		}
		if (documentDateTo!=null && documentDateFrom==null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_DOCUMENT_DATE+":[MIN TO "+sdf.format(documentDateTo)+"]");
		}
		if (documentDateFrom!=null && documentDateTo!=null) {
			SimpleDateFormat sdf = new SimpleDateFormat(Constants.TIMESTAMP_FORMAT);
			querySb.append(" AND @"+SinekartaModel.SINEKARTA_PREFIX+"\\:"+SinekartaModel.PROP_DOCUMENT_DATE+":["+sdf.format(documentDateFrom)+" TO "+sdf.format(documentDateTo)+"]");
		}
        SearchParameters sp = new SearchParameters();
        sp.addStore(storeRef);
        sp.setLanguage(SearchService.LANGUAGE_LUCENE);
        sp.setQuery(querySb.toString());
        sp.addSort(SearchParameters.SORT_IN_DOCUMENT_ORDER_ASCENDING);
        ResultSet rs = null;
        try {
			rs = su.getSearchService().query(sp);
			documents = NodeRefWrapper.createList(su.getNodeService(), rs,getDocumentPath());
        } finally {
        	if (rs!=null) rs.close();
        }
		return null;
	}
	
	public String selectAll() {
		selectedDocuments.addAll(documents);
		return null;
	}
	
	public String selectNone() {
		selectedDocuments.clear();
		return null;
	}
	
	public String getDocumentPath() {
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		return NodeTools.translatePath(su.getNodeService(), browseBean.getActionSpace().getNodePath());
	}

	public String getMarkFolderPath() {
		// TODO this value has to be calculated, how???
		return "";
//		SinekartaUtility su = (SinekartaUtility)Util.getFacesBean(Constants.SINEKARTA_UTILITY_BACKING_BEAN);
//		StoreRef storeRef = new StoreRef(markDocumentArea.getMarkFolderPathStoreRefProtocol(), markDocumentArea.getMarkFolderPathStoreRefId());
//		NodeRef markFolderNodeRef = new NodeRef(storeRef, markDocumentArea.getMarkFolderPathNodeRefId());
//		return NodeTools.translatePath(su.getNodeService(), su.getNodeService().getPath(markFolderNodeRef));
	}

	public boolean isSubspace() {
		return subspace;
	}

	public void setSubspace(boolean subspace) {
		this.subspace = subspace;
	}

	public List<NodeRefWrapper> getDocuments() {
		return documents;
	}

	public void setDocuments(List<NodeRefWrapper> documents) {
		this.documents = documents;
	}

	public List<NodeRefWrapper> getSelectedDocuments() {
		return selectedDocuments;
	}

	public void setSelectedDocuments(List<NodeRefWrapper> selectedDocuments) {
		this.selectedDocuments = selectedDocuments;
	}

	public String getDocumentName() {
		return documentName;
	}

	public void setDocumentName(String documentName) {
		this.documentName = documentName;
	}

	public String getMarkFileName() {
		return markFileName;
	}

	public void setMarkFileName(String markFileName) {
		this.markFileName = markFileName;
	}

	public String getMarkDescription() {
		return markDescription;
	}

	public void setMarkDescription(String markDescription) {
		this.markDescription = markDescription;
	}

	public boolean isError() {
		return error;
	}

	public Date getDocumentDateFrom() {
		return documentDateFrom;
	}

	public void setDocumentDateFrom(Date documentDateFrom) {
		this.documentDateFrom = documentDateFrom;
	}

	public Date getDocumentDateTo() {
		return documentDateTo;
	}

	public void setDocumentDateTo(Date documentDateTo) {
		this.documentDateTo = documentDateTo;
	}

	public boolean isAlreadySent() {
		return alreadySent;
	}

	public void setAlreadySent(boolean alreadySent) {
		this.alreadySent = alreadySent;
	}

	public Date getDocumentDate() {
		return documentDate;
	}

	public void setDocumentDate(Date documentDate) {
		this.documentDate = documentDate;
	}

	public Comunicazione getComunicazione() {
		return comunicazione;
	}

	public void setComunicazione(Comunicazione comunicazione) {
		this.comunicazione = comunicazione;
	}

	public List<SelectItem> getTipiComunicazione() {
		return tipiComunicazione;
	}

	public DatiAnagrType getDelegatoConservazioneSelected() {
		return delegatoConservazioneSelected;
	}

	public void setDelegatoConservazioneSelected(
			DatiAnagrType delegatoConservazioneSelected) {
		this.delegatoConservazioneSelected = delegatoConservazioneSelected;
	}

	public List<SelectItem> getTipiImpegno() {
		return tipiImpegno;
	}

	public void setTipiImpegno(List<SelectItem> tipiImpegno) {
		this.tipiImpegno = tipiImpegno;
	}

	public String getIntermediarioTrasmissione() {
		return intermediarioTrasmissione;
	}

	public void setIntermediarioTrasmissione(String intermediarioTrasmissione) {
		this.intermediarioTrasmissione = intermediarioTrasmissione;
	}

}
