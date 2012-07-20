package org.sinekarta.alfresco.model.agenziaEntrate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "datiDelegatoConservazione"
})
@XmlRootElement(name = "Delegati")
public class Delegati implements Serializable {

	private static final long serialVersionUID = 1L;
	
    @XmlElement(name = "DatiDelegatoConservazione")
    protected List<DatiAnagrType> datiDelegatoConservazione;

    public List<DatiAnagrType> getDatiDelegatoConservazione() {
        if (datiDelegatoConservazione == null) {
            datiDelegatoConservazione = new ArrayList<DatiAnagrType>();
        }
        return this.datiDelegatoConservazione;
    }

    public void setDatiDelegatoConservazione(List<DatiAnagrType> datiDelegatoConservazione) {
        this.datiDelegatoConservazione = datiDelegatoConservazione;
    }
}
