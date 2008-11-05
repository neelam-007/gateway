package com.l7tech.server.ems;

import com.l7tech.server.ems.pages.EmsError;
import com.l7tech.server.ems.pages.Login;
import com.l7tech.server.ems.pages.Setup;
import com.l7tech.server.ems.pages.SystemSettings;
import org.apache.wicket.*;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.markup.html.PackageResourceGuard;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.request.target.coding.HybridUrlCodingStrategy;
import org.apache.wicket.settings.*;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.convert.ConverterLocator;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.lang.Bytes;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.util.resource.locator.ResourceStreamLocator;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wicket WebApplication for Enterprise Manager.
 */
public class EmsApplication extends WebApplication {

    //- PUBLIC

    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

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

    public Session newSession(Request request, Response response) {
        return new EmsSession( request );
    }

    //- PROTECTED

    /**
     * Initialize the application
     */
    @Override
    protected void init() {
        // Record the starting time of ems process
        timeStarted = System.currentTimeMillis();
        
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
        exceptionSettings.setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_EXCEPTION_PAGE);

        IResourceSettings resourceSettings = getResourceSettings();
        resourceSettings.setResourceStreamLocator(new ResourceLocator());
        resourceSettings.setPackageResourceGuard(new PackageResourceGuard(){
            protected boolean acceptAbsolutePath( final String resourcePath ) {
                boolean accept = false;
                if ( resourcePath.startsWith("org/apache/wicket") ||
                     (resourcePath.startsWith("com/l7tech/server/ems/resources/css") ||
                      resourcePath.startsWith("com/l7tech/server/ems/resources/js") ||
                      resourcePath.startsWith("com/l7tech/server/ems/resources/templates") ||
                      resourcePath.startsWith("com/l7tech/server/ems/resources/yui"))) {
                    accept = super.acceptAbsolutePath(resourcePath);
                } else {
                    logger.info("Rejecting access to resource '"+resourcePath+"'.");
                }

                return accept;
            }
        });
        getSharedResources().add("auditResource", new AuditResource());
        getSharedResources().add("logResource", new LogResource());

        IMarkupSettings markupSettings = getMarkupSettings();
        markupSettings.setStripWicketTags(true);

        ISecuritySettings securitySettings = getSecuritySettings();
        securitySettings.setAuthorizationStrategy( new IAuthorizationStrategy() {
            public boolean isInstantiationAuthorized(Class aClass) {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Instantiation authorized check for component : " +aClass);

                return EmsError.class.equals(aClass) ||
                       Login.class.equals(aClass) ||
                       Setup.class.equals(aClass) ||
                       !Page.class.isAssignableFrom(aClass) ||
                       EmsSecurityManagerImpl.isAuthenticated();
            }

            public boolean isActionAuthorized(Component component, Action action) {
                if (logger.isLoggable(Level.FINER))
                    logger.finer("Action authorized check for component : " + component.getId() + ", " + action.getName());
                return component.getPage() instanceof EmsError ||
                       component.getPage() instanceof Login ||
                       component.getPage() instanceof Setup ||
                       EmsSecurityManagerImpl.isAuthenticated();
            }
        });

        ((ConverterLocator) this.getConverterLocator()).set( Date.class, new IConverter() {
            public DateFormat getDateFormat() {
                String formatPattern = ((EmsSession)RequestCycle.get().getSession()).getDateTimeFormatPattern();
                if ( formatPattern == null ) {
                    formatPattern = DEFAULT_DATE_FORMAT;
                }

                return new SimpleDateFormat(formatPattern);
            }

            public Object convertToObject(String s, Locale locale) {
                Date value = new Date(0);
                try {
                    value = getDateFormat().parse(s);
                } catch ( ParseException pe ) {
                    // not of interest
                }
                return value;
            }

            public String convertToString(Object o, Locale locale) {
                return getDateFormat().format((Date)o);
            }
        } );

        // mount navigation pages
        NavigationModel navigationModel = new NavigationModel("com.l7tech.server.ems.pages");
        for ( String page : navigationModel.getNavigationPages() ) {
            mountPage( "/" + navigationModel.getPageUrlForPage(page), navigationModel.getPageClassForPage(page) );
        }

        // mount other pages / templates
        mountTemplate("/Help.html");
        mountPage("/Login.html", Login.class);
        mountTemplate("/SSGClusterSelector.html");
        mountTemplate("/SrcSSGClusterContent.html");
        mountTemplate("/DestSSGClusterContent.html");
        mountTemplate("/UserSelector.html");
        mountTemplate("/SubmissionReceived.html");
    }

    public void mountPage( final String url, final Class pageClass ) {
        super.mount( new HybridUrlCodingStrategy( url, pageClass, false ) );
    }

    @Override
    public RequestCycle newRequestCycle(Request request, Response response) {
        return new WebRequestCycle( this, (WebRequest) request, response ){
            @Override
            public Page onRuntimeException(Page page, RuntimeException e) {
                if ( e instanceof PageExpiredException ) {
                    return super.onRuntimeException(page, e);
                }
                
                return new EmsError( e );
            }
        };
    }

    public static List<String> getDateFormatkeys() {
        return new ArrayList<String>(dates.keySet());
    }

    public static List<String> getTimeFormatkeys() {
        return new ArrayList<String>(times.keySet());
    }

    public static String getDateFormatExample( final String key ) {
        return dates.get(key);
    }

    public static String getTimeFormatExample( final String key ) {
        return times.get(key);
    }

    public static String getDateFormat( final String dateKey, final String timeKey ) {
        String dateValue = "friendly".equals(dateKey) ? "MMM dd, yyyy" : "yyyy-MM-dd";
        String timeValue = "friendly".equals(timeKey) ? "hh:mm:ss a" : "HH:mm:ss";

        return dateValue + " " + timeValue;
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EmsApplication.class.getName() );

    private static final Map<String,String> dates;
    private static final Map<String,String> times;

    private long timeStarted; // The time when EMS process started.

    static {
        Map<String,String> dateMap = new LinkedHashMap<String,String>();
        dateMap.put("formal", "2010-01-31");
        dateMap.put("friendly", "Jan 31, 2010");

        Map<String,String> timeMap = new LinkedHashMap<String,String>();
        timeMap.put("formal", "13:00:00");
        timeMap.put("friendly", "01:00:00 PM");

        dates = Collections.unmodifiableMap(dateMap);
        times = Collections.unmodifiableMap(timeMap);
    }

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
