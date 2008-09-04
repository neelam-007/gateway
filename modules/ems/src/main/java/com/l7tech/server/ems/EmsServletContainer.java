package com.l7tech.server.ems;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.jetty.servlet.FilterHolder;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.resource.Resource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.apache.wicket.protocol.http.WicketFilter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.FilterChain;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.security.auth.Subject;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.io.File;
import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.security.PrivilegedActionException;

import com.l7tech.gateway.common.spring.remoting.RemoteUtils;
import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.server.util.FirewallUtils;

/**
 * An embedded servlet container that the EMS uses to host itself.
 *
 * TODO [steve] This needs cleanup
 */
public class EmsServletContainer implements ApplicationContextAware, InitializingBean, DisposableBean {
    public static final String RESOURCE_PREFIX = "com/l7tech/server/ems/resources/";
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    private static final Logger logger = Logger.getLogger(EmsServletContainer.class.getName());

    private static final AtomicLong nextInstanceId = new AtomicLong(1);
    private static final Map<Long, Reference<EmsServletContainer>> instancesById =
            new ConcurrentHashMap<Long, Reference<EmsServletContainer>>();

    private final long instanceId;
    private final int httpPort;
    private ApplicationContext applicationContext;
    private Server server;

    public EmsServletContainer(int httpPort) {
        this.instanceId = nextInstanceId.getAndIncrement();
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<EmsServletContainer>(this));

        this.httpPort = httpPort;
    }

    private void initializeServletEngine() throws Exception {
        server = new Server(httpPort);

        File temp = new File("/tmp");
        final Context root = new Context(server, "/", Context.SESSIONS);
        root.setBaseResource(Resource.newClassPathResource("com/l7tech/server/ems/resources")); //TODO [steve] map root elsewhere and add other mappings for css/images/etc
        root.setDisplayName("Layer 7 Enterprise Service Manager Server");
        root.setAttribute("javax.servlet.context.tempdir", temp); //TODO [steve] temp directory ?
        root.addEventListener(new EmsContextLoaderListener());
        root.setClassLoader(Thread.currentThread().getContextClassLoader());

        //noinspection unchecked
        final Map<String, String> initParams = root.getInitParams();
        initParams.put("contextConfigLocation", "classpath:com/l7tech/server/ems/resources/webApplicationContext.xml");
        initParams.put(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));

        // Add security handler
        final Filter securityFilter = new Filter(){
            private EmsSecurityManager securityManager;
            private ServletContext context;
            public void init(final FilterConfig filterConfig) throws ServletException {
                context = filterConfig.getServletContext();
                securityManager = (EmsSecurityManager) context.getAttribute("securityManager");
            }
            public void destroy() {}

            public void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse, final FilterChain filterChain) throws IOException, ServletException {
                final HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
                final HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;
                final IOException[] ioeHolder = new IOException[1];
                final ServletException[] seHolder = new ServletException[1];
                RemoteUtils.runWithConnectionInfo(servletRequest.getRemoteAddr(), httpServletRequest, new Runnable(){
                    public void run() {
                        try {
                            if ( securityManager.canAccess( httpServletRequest.getSession(true), httpServletRequest ) ) {
                                if ( logger.isLoggable(Level.FINER) )
                                    logger.finer("Allowing access to resource '" + httpServletRequest.getRequestURI() + "'.");
                                Subject subject = new Subject();
                                EmsSecurityManager.LoginInfo info = securityManager.getLoginInfo(httpServletRequest.getSession(true));
                                if ( info != null ) {
                                    subject.getPrincipals().add( info.getUser() );
                                }
                                Subject.doAs(subject, new PrivilegedExceptionAction<Object>(){
                                    public Object run() throws Exception {
                                        filterChain.doFilter( servletRequest, servletResponse );
                                        return null;
                                    }
                                });
                            } else {
                                logger.info("Forbid access to resource : '" + httpServletRequest.getRequestURI() + "'." );
                                httpServletResponse.sendRedirect("/Login.html");
                            }
                        } catch(IOException ioe) {
                            ioeHolder[0] = ioe;
                        } catch (PrivilegedActionException pae) {
                            Throwable exception = pae.getCause();
                            if (exception instanceof IOException) {
                                ioeHolder[0] = (IOException) exception;
                            } else if (exception instanceof ServletException) {
                                seHolder[0] = (ServletException) exception;
                            } else {
                                throw ExceptionUtils.wrap(exception);
                            }
                        }
                    }
                });

                // rethrow exceptions
                if (ioeHolder[0] != null) throw ioeHolder[0];
                if (seHolder[0] != null) throw seHolder[0];
            }
        };
        FilterHolder fsHolder = new FilterHolder(securityFilter);
        root.addFilter(fsHolder, "/*", Handler.REQUEST);

        // Add wicket handler
        final WicketFilter wicketFilter = new WicketFilter();
        FilterHolder fHolder = new FilterHolder(wicketFilter);
        fHolder.setInitParameter("applicationClassName", EmsApplication.class.getName());
        fHolder.setName("wicketFilter");
        root.addFilter(fHolder, "/*", Handler.REQUEST);

        //Set DefaultServlet to handle all static resource requests
        DefaultServlet defaultServlet = new DefaultServlet();         
        ServletHolder defaultHolder = new ServletHolder(defaultServlet);
        root.addServlet(defaultHolder, "/");

        server.start();

        List<SsgConnector> connectors = new ArrayList<SsgConnector>();
        SsgConnector rc = new SsgConnector();
        rc.setPort(httpPort);
        connectors.add(rc);
        FirewallUtils.openFirewallForConnectors( temp, connectors );  // TODO use conf/var directory for rules?
    }

    private void shutdownServletEngine() throws Exception {
        server.stop();
        server.destroy();
        FirewallUtils.closeFirewallForConnectors( new File("/tmp") );  // TODO use conf/var directory for rules?
    }

    public void afterPropertiesSet() throws Exception {
        initializeServletEngine();
    }

    public void destroy() throws Exception {
        shutdownServletEngine();
    }

    protected ApplicationContext getApplicationContext() {
        return applicationContext;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    /**
     * Get the number that can be used to uniquely identify this EmsServletContainer instance within its
     * classloader.  This number can later be passed to {@link #getInstance(long)} to retrieve a referene
     * to the instance.
     *
     * @return the instance ID of this EmsServletContainer instance.
     */
    long getInstanceId() {
        return instanceId;
    }

    /**
     * Find the EmsServletContainer corresponding to the specified instance ID.
     * <p/>
     * This is normally used by the SsgJSSESocketFactory to locate a Connector's owner HttpTransportModule
     * so it can get at the SsgKeyStoreManager.
     *
     * @see #getInstanceId
     * @param id the instance ID to search for.  Required.
     * @return  the corresopnding HttpTransportModule instance, or null if not found.
     */
    public static EmsServletContainer getInstance(long id) {
        Reference<EmsServletContainer> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }
}
