package org.sinekarta.ant.util;

import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;

import org.alfresco.webservice.authentication.AuthenticationFault;
import org.alfresco.webservice.authentication.AuthenticationResult;
import org.alfresco.webservice.util.AuthenticationDetails;
import org.alfresco.webservice.util.WebServiceException;
import org.apache.axis.EngineConfiguration;
import org.apache.axis.configuration.FileProvider;

/**
 * copied from Alfresco (same Class of webservice client jar)
 * The original class was saving data in thread local, in ant we need sequential access
 * @author Roy Wetherall
 */
public class AuthenticationUtils 
{
    /** WS security information */
    private static final String WS_SECURITY_INFO = 
         "<deployment xmlns='http://xml.apache.org/axis/wsdd/' xmlns:java='http://xml.apache.org/axis/wsdd/providers/java'>" +
         "   <transport name='http' pivot='java:org.apache.axis.transport.http.HTTPSender'/>" +
         "   <globalConfiguration >" +
         "     <requestFlow >" +
         "       <handler type='java:org.apache.ws.axis.security.WSDoAllSender' >" +
         "               <parameter name='action' value='UsernameToken Timestamp'/>" +
         "               <parameter name='user' value='ticket'/>" +
         "               <parameter name='passwordCallbackClass' value='org.sinekarta.ant.util.PWDHandler'/>" +
         "               <parameter name='passwordType' value='PasswordText'/>" +
         "           </handler>" +
         "       <handler name='cookieHandler' type='java:org.sinekarta.ant.util.CookieHandler' />" +
         "     </requestFlow >" +
         "   </globalConfiguration>" +
         "</deployment>";
    
    /** Thread local containing the current authentication details */
    private Configuration configuration;
    
    private AuthenticationDetails authenticationDetails;
    
    private static final AuthenticationUtils mySelf = new AuthenticationUtils();
    
    /**
     * Start a session
     * 
     * @param username
     * @param password
     * @throws AuthenticationFault
     */
    public static void startSession(Configuration configuration, String username, String password)
        throws AuthenticationFault
    {
        try
        {
        	// mi salvo la configurazione che si usera' in questo task
        	mySelf.configuration=configuration;
            // Start the session
            AuthenticationResult result = WebServiceFactory.getAuthenticationService().startSession(username, password);           
            
            // Store the ticket for use later
            mySelf.authenticationDetails = new AuthenticationDetails(result.getUsername(), result.getTicket(), result.getSessionid());
        }
        catch (RemoteException exception)
        {
            if (exception instanceof AuthenticationFault)
            {
                // Rethrow the authentication exception
                throw (AuthenticationFault)exception;
            }
            else
            {
                // Throw the exception as a wrapped runtime exception
                throw new WebServiceException("Error starting session.", exception);
            }
        }             
    }
    
    /**
     * Start a session
     * 
     * @param username
     * @param password
     * @param timeoutInterval timeout interval
     * @throws AuthenticationFault
     */
    public static void startSession(Configuration configuration, String username, String password, long timeoutInterval)
    	throws AuthenticationFault
	{
		startSession(configuration, username, password);
	
		AuthenticationDetails ad = getAuthenticationDetails();
		ad.setTimeoutInterval(timeoutInterval);
	}
    
    /**
	 * @return if timeoutInterval is not set return false.
	 */
    public static boolean isCurrentTicketTimedOut()
    {
    	boolean to = getAuthenticationDetails().isTimedOut();
    	
    	if (to)
    		endSession();
    	
    	return to;
    }


    /**
     * Ends the current session
     */
    public static void endSession()
    {
        if (mySelf.authenticationDetails != null)
        {
            try
            {
                WebServiceFactory.getAuthenticationService().endSession(mySelf.authenticationDetails.getTicket());
                mySelf.authenticationDetails = null;
            }
            catch (RemoteException exception)
            {
                exception.printStackTrace();
                throw new WebServiceException("Error ending session.", exception);
            }
        }
    }
    
    /**
     * Get the ticket for the current authentication details on the current thread
     * 
     * @return  String  the ticket
     */
    public static String getTicket()
    {
        String result = null;
        AuthenticationDetails authDetails = mySelf.authenticationDetails;
        if (authDetails != null)
        {
            result = authDetails.getTicket();
        }
        return result;
    }
    
    /**
     * Get the authentication details for the current thread
     * 
     * @return  the authentication details
     */
    public static AuthenticationDetails getAuthenticationDetails()
    {
        return mySelf.authenticationDetails;
    }
    
    /**
     * Gets the engine configuration used to create the web service references
     * 
     * @return  EngineConfiguration     the engine configuration
     */
    public static EngineConfiguration getEngineConfiguration()
    {
        return new FileProvider(new ByteArrayInputStream(WS_SECURITY_INFO.getBytes()));
    }

	public static Configuration getConfiguration() {
		return mySelf.configuration;
	}    
}
