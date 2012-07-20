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

import javax.faces.component.UIComponent;
import javax.faces.component.UISelectItems;
import javax.faces.component.UISelectOne;
import javax.faces.context.FacesContext;

import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.web.app.servlet.FacesHelper;
import org.alfresco.web.bean.generator.BaseComponentGenerator;
import org.alfresco.web.ui.repo.component.property.PropertySheetItem;
import org.alfresco.web.ui.repo.component.property.UIPropertySheet;
import org.sinekarta.alfresco.web.converter.DocumentTypeConverterForAlfresco;

public class DocumentTypeGenerator extends BaseComponentGenerator {

	@SuppressWarnings("unchecked")
	public UIComponent generate(FacesContext context, String id) {
		UISelectOne component = (UISelectOne)context.getApplication().createComponent(UISelectOne.COMPONENT_TYPE);
//		component.setValue(Configuration.getInstance().getLinguaDefaultOcr());
		FacesHelper.setupComponentId(context, component, id);
		// create the list of choices
		UISelectItems itemsComponent = (UISelectItems) context.getApplication().createComponent("javax.faces.SelectItems");
		SinekartaUtility su = SinekartaUtility.getCurrentInstance();
		itemsComponent.setValue(su.getTipiDocumentoForAlfresco());
		// add the items as a child component
		component.getChildren().add(itemsComponent);
		return component;
	}

    @Override
    protected void setupConverter(FacesContext context, 
            UIPropertySheet propertySheet, PropertySheetItem property, 
            PropertyDefinition propertyDef, UIComponent component)
    {
          createAndSetConverter(context, DocumentTypeConverterForAlfresco.CONVERTER_ID, component);
    }
      
}
