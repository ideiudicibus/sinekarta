package org.sinekarta.ant.util;

import javax.xml.rpc.ServiceException;

import org.alfresco.webservice.accesscontrol.AccessControlServiceLocator;
import org.alfresco.webservice.accesscontrol.AccessControlServiceSoapBindingStub;
import org.alfresco.webservice.action.ActionServiceLocator;
import org.alfresco.webservice.action.ActionServiceSoapBindingStub;
import org.alfresco.webservice.administration.AdministrationServiceLocator;
import org.alfresco.webservice.administration.AdministrationServiceSoapBindingStub;
import org.alfresco.webservice.authentication.AuthenticationServiceLocator;
import org.alfresco.webservice.authentication.AuthenticationServiceSoapBindingStub;
import org.alfresco.webservice.authoring.AuthoringServiceLocator;
import org.alfresco.webservice.authoring.AuthoringServiceSoapBindingStub;
import org.alfresco.webservice.classification.ClassificationServiceLocator;
import org.alfresco.webservice.classification.ClassificationServiceSoapBindingStub;
import org.alfresco.webservice.content.ContentServiceLocator;
import org.alfresco.webservice.content.ContentServiceSoapBindingStub;
import org.alfresco.webservice.dictionary.DictionaryServiceLocator;
import org.alfresco.webservice.dictionary.DictionaryServiceSoapBindingStub;
import org.alfresco.webservice.repository.RepositoryServiceLocator;
import org.alfresco.webservice.repository.RepositoryServiceSoapBindingStub;
import org.alfresco.webservice.util.WebServiceException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * copied from Alfresco (same Class of webservice client jar)
 * The original class was saving data in thread local, we need to save them on HttpSession
 * @author Roy Wetherall
 */
public final class WebServiceFactory
{
    /** Log */
    private static Log logger = LogFactory.getLog(WebServiceFactory.class);
    
    /**
     * Get the authentication service
     * 
     * @return
     */
    public static AuthenticationServiceSoapBindingStub getAuthenticationService()
    {
    	AuthenticationServiceSoapBindingStub authenticationService = null;
        try 
        {
            // Get the authentication service
            AuthenticationServiceLocator locator = new AuthenticationServiceLocator();
            locator.setAuthenticationServiceEndpointAddress(AuthenticationUtils.getConfiguration().getAuthenticationService());                
            authenticationService = (AuthenticationServiceSoapBindingStub)locator.getAuthenticationService();
        }
        catch (ServiceException jre) 
        {
        	if (logger.isDebugEnabled() == true)
            {
        		if (jre.getLinkedCause() != null)
                {
        			jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating authentication service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        authenticationService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());
        
        return authenticationService;
    }
    
    /**
     * Get the repository service
     * 
     * @return
     */
    public static RepositoryServiceSoapBindingStub getRepositoryService()
    {
    	RepositoryServiceSoapBindingStub repositoryService = null;           
		try 
		{
		    // Get the repository service
		    RepositoryServiceLocator locator = new RepositoryServiceLocator(AuthenticationUtils.getEngineConfiguration());
		    locator.setRepositoryServiceEndpointAddress(AuthenticationUtils.getConfiguration().getRepositoryService());                
		    repositoryService = (RepositoryServiceSoapBindingStub)locator.getRepositoryService();
            repositoryService.setMaintainSession(true);
		 }
		 catch (ServiceException jre) 
		 {
			 if (logger.isDebugEnabled() == true)
		     {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
		     }
		   
			 throw new WebServiceException("Error creating repositoryService service: " + jre.getMessage(), jre);
		}        
	
		// Time out after a minute
		repositoryService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());      
        
        return repositoryService;
    }
    
    /**
     * Get the authoring service
     * 
     * @return
     */
    public static AuthoringServiceSoapBindingStub getAuthoringService()
    {
    	AuthoringServiceSoapBindingStub authoringService = null;
                  
        try 
        {
            // Get the authoring service
            AuthoringServiceLocator locator = new AuthoringServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setAuthoringServiceEndpointAddress(AuthenticationUtils.getConfiguration().getAuthoringService());                
            authoringService = (AuthoringServiceSoapBindingStub)locator.getAuthoringService();
            authoringService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating authoring service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        authoringService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());       
        
        return authoringService;
    }
    
    /**
     * Get the classification service
     * 
     * @return
     */
    public static ClassificationServiceSoapBindingStub getClassificationService()
    {
    	ClassificationServiceSoapBindingStub classificationService = null;
            
        try 
        {
            // Get the classification service
            ClassificationServiceLocator locator = new ClassificationServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setClassificationServiceEndpointAddress(AuthenticationUtils.getConfiguration().getClassificationService());                
            classificationService = (ClassificationServiceSoapBindingStub)locator.getClassificationService();
            classificationService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating classification service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        classificationService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());        
        
        return classificationService;
    }
    
    /**
     * Get the action service
     * 
     * @return
     */
    public static ActionServiceSoapBindingStub getActionService()
    {
    	ActionServiceSoapBindingStub actionService = null;
            
        try 
        {
            // Get the action service
            ActionServiceLocator locator = new ActionServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setActionServiceEndpointAddress(AuthenticationUtils.getConfiguration().getActionService());                
            actionService = (ActionServiceSoapBindingStub)locator.getActionService();
            actionService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating action service: " + jre.getMessage(), jre);
        }        
            
        // Time out after a minute
        actionService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());      
        
        return actionService;
    }
    
    /**
     * Get the content service
     * 
     * @return  the content service
     */
    public static ContentServiceSoapBindingStub getContentService()
    {
    	ContentServiceSoapBindingStub contentService = null;           
        try 
        {
            // Get the content service
            ContentServiceLocator locator = new ContentServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setContentServiceEndpointAddress(AuthenticationUtils.getConfiguration().getContentService());                
            contentService = (ContentServiceSoapBindingStub)locator.getContentService();
            contentService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating content service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        contentService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());       
        
        return contentService;
    }
    
    /**
     * Get the access control service
     * 
     * @return  the access control service
     */
    public static AccessControlServiceSoapBindingStub getAccessControlService()
    {
    	AccessControlServiceSoapBindingStub accessControlService = null;           
        try 
        {
            // Get the access control service
            AccessControlServiceLocator locator = new AccessControlServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setAccessControlServiceEndpointAddress(AuthenticationUtils.getConfiguration().getAccessControlService());                
            accessControlService = (AccessControlServiceSoapBindingStub)locator.getAccessControlService();
            accessControlService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating access control service: " + jre.getMessage(), jre);
        }        
            
        // Time out after a minute
        accessControlService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());
        
        return accessControlService;
    }
    
    /**
     * Get the administration service
     * 
     * @return  the administration service
     */
    public static AdministrationServiceSoapBindingStub getAdministrationService()
    {
    	AdministrationServiceSoapBindingStub administrationService = null;
            
        try 
        {
            // Get the adminstration service
            AdministrationServiceLocator locator = new AdministrationServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setAdministrationServiceEndpointAddress(AuthenticationUtils.getConfiguration().getAdministrationService());                
            administrationService = (AdministrationServiceSoapBindingStub)locator.getAdministrationService();
            administrationService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating administration service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        administrationService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());       
        
        return administrationService;
    }
    
    /**
     * Get the dictionary service
     * 
     * @return  the dictionary service
     */
    public static DictionaryServiceSoapBindingStub getDictionaryService()
    {
    	DictionaryServiceSoapBindingStub dictionaryService = null;
           
        try 
        {
            // Get the dictionary service
            DictionaryServiceLocator locator = new DictionaryServiceLocator(AuthenticationUtils.getEngineConfiguration());
            locator.setDictionaryServiceEndpointAddress(AuthenticationUtils.getConfiguration().getDictionaryService());                
            dictionaryService = (DictionaryServiceSoapBindingStub)locator.getDictionaryService();
            dictionaryService.setMaintainSession(true);
        }
        catch (ServiceException jre) 
        {
            if (logger.isDebugEnabled() == true)
            {
                if (jre.getLinkedCause() != null)
                {
                    jre.getLinkedCause().printStackTrace();
                }
            }
   
            throw new WebServiceException("Error creating dictionary service: " + jre.getMessage(), jre);
        }        
        
        // Time out after a minute
        dictionaryService.setTimeout(AuthenticationUtils.getConfiguration().getTimeoutMilliseconds());

        return dictionaryService;
    }

}

