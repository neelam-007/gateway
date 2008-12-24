package com.l7tech.server.ems.ui;

import com.l7tech.server.ems.ui.pages.*;
import com.l7tech.server.ems.standardreports.ReportResource;
import com.l7tech.server.ems.user.UserPropertyManager;
import com.l7tech.util.SyspropUtil;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.FindException;
import org.apache.wicket.*;
import org.apache.wicket.authorization.Action;
import org.apache.wicket.authorization.IAuthorizationStrategy;
import org.apache.wicket.authorization.IUnauthorizedComponentInstantiationListener;
import org.apache.wicket.markup.html.PackageResourceGuard;
import org.apache.wicket.protocol.http.PageExpiredException;
import org.apache.wicket.protocol.http.WebApplication;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebRequestCycle;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.request.target.coding.HybridUrlCodingStrategy;
import org.apache.wicket.settings.*;
import org.apache.wicket.spring.injection.annot.SpringComponentInjector;
import org.apache.wicket.util.convert.ConverterLocator;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.lang.Bytes;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.http.HttpServletRequest;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Wicket WebApplication for Enterprise Manager.
 */
public class EsmApplication extends WebApplication {

    //- PUBLIC

    public static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_SYSTEM_TIME_ZONE = "[System Default Time Zone]";
    public static final String DEFAULT_HOME_PAGE = "SystemSettings";

    /**
     *
     */
    @Override
    public Class getHomePage() {
        Class homePage = SystemSettings.class;

        RequestCycle cycle = RequestCycle.get();
        if ( cycle != null ) {
            Session session = cycle.getSession();
            if ( session instanceof EsmSession) {
                EsmSession emsSession = (EsmSession) session;
                if ( emsSession.getPreferredPage() != null &&
                     !emsSession.getPreferredPage().isEmpty() ) {
                    NavigationModel navigationModel = new NavigationModel("com.l7tech.server.ems.ui.pages");
                    Class userHomePage = navigationModel.getPageClassForPage( emsSession.getPreferredPage() );
                    if ( userHomePage != null ) {
                        homePage = userHomePage;    
                    } else {
                        logger.warning("User home page not found '"+emsSession.getPreferredPage()+"'." );
                    }
                }
            }
        }

        return homePage;
    }

    /**
     * 
     */
    @Override
    public String getConfigurationType() {
        return SyspropUtil.getBoolean("com.l7tech.ems.development") ? WebApplication.DEVELOPMENT : WebApplication.DEPLOYMENT;
    }

    @Override
    public Session newSession( final Request request, final Response response ) {
        EsmSession session = new EsmSession( request );

        ServletWebRequest servletWebRequest = (ServletWebRequest) request;
        HttpServletRequest servletRequest = servletWebRequest.getHttpServletRequest();
        EsmSecurityManager.LoginInfo info = getEsmSecurityManager().getLoginInfo( servletRequest.getSession(true) );
        if ( info != null ) {
            setUserPreferences( info.getUser(), session );
        }

        return session;
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
        applicationSettings.setPageExpiredErrorPage(HomeRedirectPage.class);
        applicationSettings.setAccessDeniedPage(HomeRedirectPage.class);
        applicationSettings.setInternalErrorPage(EsmError.class);
        applicationSettings.setDefaultMaximumUploadSize(Bytes.kilobytes(100));

        // show internal error page rather than default developer page
        IExceptionSettings exceptionSettings = getExceptionSettings();
        exceptionSettings.setUnexpectedExceptionDisplay(IExceptionSettings.SHOW_EXCEPTION_PAGE);

        IResourceSettings resourceSettings = getResourceSettings();
        resourceSettings.setPackageResourceGuard(new PackageResourceGuard(){
            @Override
            protected boolean acceptAbsolutePath( final String resourcePath ) {
                boolean accept = false;
                if ( resourcePath.startsWith("org/apache/wicket") ||
                     resourcePath.startsWith("com/l7tech/server/ems/ui/pages") ) {
                    accept = super.acceptAbsolutePath(resourcePath);
                } else {
                    logger.info("Rejecting access to resource '"+resourcePath+"'.");
                }

                return accept;
            }
        });
        getSharedResources().add("auditResource", new AuditResource());
        getSharedResources().add("logResource", new LogResource());
        getSharedResources().add("reportResource", new ReportResource());

        IMarkupSettings markupSettings = getMarkupSettings();
        markupSettings.setStripWicketTags(true);

        ISecuritySettings securitySettings = getSecuritySettings();
        securitySettings.setUnauthorizedComponentInstantiationListener( new IUnauthorizedComponentInstantiationListener(){
            @SuppressWarnings({"override"})
            public void onUnauthorizedInstantiation( final Component component ) {
                component.setEnabled( false );
                component.setVisible( false );
            }
        } );
        securitySettings.setAuthorizationStrategy( new IAuthorizationStrategy() {
            @Override
            public boolean isInstantiationAuthorized(Class aClass) {
                final boolean isPage = Page.class.isAssignableFrom(aClass);

                if ( isPage && !getEsmSecurityManager().isAuthorized( aClass ) ) {
                    throw new RedirectToUrlException( "/" );
                }

                if ( isPage && !getEsmSecurityManager().isLicensed( aClass ) ) {
                    throw new RestartResponseAtInterceptPageException( SystemSettings.class );
                }

                boolean permitted = !isPage ||  getEsmSecurityManager().isAuthenticated( aClass );

                if (logger.isLoggable(Level.FINER))
                    logger.finer("Instantiation authorized check for component '" +aClass + "' is " + permitted + ".");

                return permitted;
            }

            @Override
            public boolean isActionAuthorized(Component component, Action action) {
                boolean permitted =
                        getEsmSecurityManager().isAuthenticated( component ) &&
                        (component instanceof Page || getEsmSecurityManager().isLicensed( component ));

                if (logger.isLoggable(Level.FINER))
                    logger.finer("Action authorized check for component  '" + component.getId() + "', '" + action.getName() + "' is " + permitted);

                return permitted;
            }
        });

        ((ConverterLocator) this.getConverterLocator()).set( Date.class, new IConverter() {
            public DateFormat getDateFormat() {
                EsmSession session = ((EsmSession)RequestCycle.get().getSession());
                return session.buildDateFormat();
            }

            @Override
            public Object convertToObject(String s, Locale locale) {
                Date value = new Date(0);
                try {
                    value = getDateFormat().parse(s);
                } catch ( ParseException pe ) {
                    // not of interest
                }
                return value;
            }

            @Override
            public String convertToString(Object o, Locale locale) {
                return getDateFormat().format((Date)o);
            }
        } );

        // mount navigation pages
        NavigationModel navigationModel = new NavigationModel("com.l7tech.server.ems.ui.pages");
        for ( String page : navigationModel.getNavigationPages() ) {
            mountPage( "/" + navigationModel.getPageUrlForPage(page), navigationModel.getPageClassForPage(page) );
        }

        // mount other pages / templates
        mountTemplate("/Help.html");
        mountPage("/SSGClusterSelector.html", SSGClusterSelector.class);
        mountPage("/SSGClusterContentSelector.html", SSGClusterContentSelector.class);
        mountPage("/SSGClusterServiceContentSelector.html", SSGClusterServiceContentSelector.class);
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
                
                return new EsmError( e );
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

    public static String getDateTimeFormat( final String dateKey, final String timeKey ) {
        String timeValue = "friendly".equals(timeKey) ? "hh:mm:ss a" : "HH:mm:ss";

        return getDateFormat(dateKey) + " " + timeValue;
    }

    public static String getDateFormat( final String dateKey ) {
        return "friendly".equals(dateKey) ? "MMM dd, yyyy" : "yyyy-MM-dd";
    }

    public static boolean isValidTimezoneId(String id) {
        return id != null && Arrays.asList(TimeZone.getAvailableIDs()).contains(id);
    }

    public long getTimeStarted() {
        return timeStarted;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( EsmApplication.class.getName() );

    private static final Map<String,String> dates;
    private static final Map<String,String> times;

    private long timeStarted; // The time when EMS process started.
    private EsmSecurityManager esmSecurityManager;
    private UserPropertyManager userPropertyManager;

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
        mountSharedResource( templateName, new ResourceReference(EsmApplication.class, "pages" + templateName).getSharedResourceKey());
    }

    /**
     *
     */
    private void setUserPreferences( final User user, final EsmSession session ) {
        String dateformat = null;
        String datetimeformat = null;
        String zoneid = null;
        String preferredpage = null;

        try {
            Map<String, String> props = getUserPropertyManager().getUserProperties( user );
            String dateFormat = props.get("dateformat");
            String timeFormat = props.get("timeformat");

            if (dateFormat == null) {
                dateFormat = "formal";
                props.put("dateformat", "formal");
            }
            if (timeFormat == null) {
                timeFormat = "formal";
                props.put("timeformat", "formal");
            }

            dateformat = EsmApplication.getDateFormat(dateFormat);
            datetimeformat = EsmApplication.getDateTimeFormat(dateFormat, timeFormat);
            zoneid = props.get("timezone");
            preferredpage = props.get("homepage");
        } catch ( FindException fe ) {
            // use default format
        }

        if (dateformat == null) {
            dateformat = EsmApplication.DEFAULT_DATE_FORMAT;

        }
        if (datetimeformat == null) {
            datetimeformat = EsmApplication.DEFAULT_DATETIME_FORMAT;
        }
        if (!EsmApplication.isValidTimezoneId(zoneid)) {
            zoneid = TimeZone.getDefault().getID();
        }
        if (preferredpage == null) {
            preferredpage = EsmApplication.DEFAULT_HOME_PAGE;
        }

        session.setDateFormatPattern( dateformat );
        session.setDateTimeFormatPattern( datetimeformat );
        session.setTimeZoneId(zoneid);
        session.setPreferredPage(preferredpage);
    }

    /**
     *
     */
    private EsmSecurityManager getEsmSecurityManager() {
        EsmSecurityManager esmSecurityManager = this.esmSecurityManager;
        if ( esmSecurityManager == null ) {
            BeanFactory beanFactory = WebApplicationContextUtils.getWebApplicationContext(getWicketFilter().getFilterConfig().getServletContext());
            esmSecurityManager = (EsmSecurityManager) beanFactory.getBean( "securityManager", EsmSecurityManager.class );
            this.esmSecurityManager = esmSecurityManager;
        }
        return esmSecurityManager;
    }

    /**
     *
     */
    private UserPropertyManager getUserPropertyManager() {
        UserPropertyManager userPropertyManager = this.userPropertyManager;
        if ( userPropertyManager == null ) {
            BeanFactory beanFactory = WebApplicationContextUtils.getWebApplicationContext(getWicketFilter().getFilterConfig().getServletContext());
            userPropertyManager = (UserPropertyManager) beanFactory.getBean( "userPropertyManager", UserPropertyManager.class );
            this.userPropertyManager = userPropertyManager;
        }
        return userPropertyManager;
    }
}
