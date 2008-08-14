package com.l7tech.server.ems;

import com.l7tech.server.ems.pages.*;
import org.apache.wicket.Component;
import org.apache.wicket.Page;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.settings.*;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wicket WebApplication for Enterprise Manager.
 */
public class EmsApplication extends WebApplication {

    //- PUBLIC

    /**
     *
     */
    public Class getHomePage() {
        return SystemSettings.class;
    }

    /**
     * 
     */
    public String getConfigurationType() {
        return WebApplication.DEPLOYMENT;
    }

    //- PROTECTED

    /**
     * Initialize the application
     */
    @Override
    protected void init() {
        super.init();

        // Wires up SpringBean annotation references
        addComponentInstantiationListener(new SpringComponentInjector(this));

        IApplicationSettings applicationSettings = getApplicationSettings();
        applicationSettings.setPageExpiredErrorPage(Login.class);
        applicationSettings.setAccessDeniedPage(Login.class);
        applicationSettings.setInternalErrorPage(EmsError.class);
        applicationSettings.setDefaultMaximumUploadSize(Bytes.kilobytes(100));

        // show internal error page rather than default developer page
        IExceptionSettings exceptionSettings = getExceptionSettings();
        exceptionSettings.setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_INTERNAL_ERROR_PAGE);

        IResourceSettings resourceSettings = getResourceSettings();
        resourceSettings.setResourceStreamLocator(new ResourceLocator());

        IMarkupSettings markupSettings = getMarkupSettings();
        markupSettings.setStripWicketTags(true);

        ISecuritySettings securitySettings = getSecuritySettings();
        securitySettings.setAuthorizationStrategy( new IAuthorizationStrategy() {
            public boolean isInstantiationAuthorized(Class aClass) {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Instantiation authorized check for component : " +aClass);

                return Login.class.equals(aClass) ||
                       Setup.class.equals(aClass) ||
                       !Page.class.isAssignableFrom(aClass) ||
                       EmsSecurityManagerImpl.isAuthenticated();
            }

            public boolean isActionAuthorized(Component component, Action action) {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Action authorized check for component : " + component.getId() + ", " + action.getName());
                return component.getPage() instanceof Login ||
                       component.getPage() instanceof Setup ||
                       EmsSecurityManagerImpl.isAuthenticated();
            }
        });

        // mount pages
        mountBookmarkablePage("/Audits.html", Audits.class);
        mountTemplate("/Backup.html");
        mountTemplate("/EnterpriseGateways.html");
        mountBookmarkablePage("/EnterpriseUsers.html", EnterpriseUsers.class);
        mountTemplate("/ExternalReports.html");
        mountTemplate("/Help.html");
        mountBookmarkablePage("/Login.html", Login.class);
        mountTemplate("/Logs.html");
        mountTemplate("/Messages.html");
        mountTemplate("/SSGClusterSelector.html");
        mountTemplate("/PolicyApproval.html");
        mountTemplate("/PolicyMapping.html");
        mountTemplate("/PolicyMigration.html");
        mountTemplate("/PolicySubmission.html");
        mountTemplate("/Restore.html");
        mountTemplate("/Setup.html");
        mountTemplate("/StandardReports.html");
        mountTemplate("/SubmissionReceived.html");
        mountTemplate("/SystemSettings.html");
        mountTemplate("/TestWebService.html");
        mountTemplate("/UserSelector.html");
        mountBookmarkablePage("/UserSettings.html", UserSettings.class);
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EmsApplication.class.getName() );

    /**
     *
     */
    private void mountTemplate( String templateName ) {
        mountSharedResource( templateName, new ResourceReference(EmsApplication.class, "resources/templates" + templateName).getSharedResourceKey());
    }

    /**
     * ResourceStreamLocator that understands our resource structure
     */
    private static class ResourceLocator extends ResourceStreamLocator {
        public IResourceStream locate(final Class clazz, final String path) {
            //logger.info("Processing locate call for path '"+path+"'.");
            return super.locate(clazz, path.replace("pages", "resources/templates"));
        }
    }
}
