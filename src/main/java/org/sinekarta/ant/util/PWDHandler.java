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

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.alfresco.webservice.util.WebServiceException;
import org.apache.ws.security.WSPasswordCallback;

public class PWDHandler implements CallbackHandler{

    /**
     * The implementation of the passwrod call back used by the WS Security
     * 
     * @see javax.security.auth.callback.CallbackHandler#handle(javax.security.auth.callback.Callback[])
     */
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
       for (int i = 0; i < callbacks.length; i++) 
       {
          if (callbacks[i] instanceof WSPasswordCallback) 
          {
             WSPasswordCallback pc = (WSPasswordCallback)callbacks[i];
             String ticket = AuthenticationUtils.getTicket();
             if (ticket == null)
             {
                 throw new WebServiceException("Ticket could not be found when calling callback handler.");
             }
             pc.setPassword(ticket);
          }
          else 
          {
             throw new UnsupportedCallbackException(callbacks[i], "Unrecognized Callback");
          }
       }
    }
    
}
