package com.l7tech.server.processcontroller;

import org.apache.cxf.transport.servlet.CXFServlet;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHolder;
import org.mortbay.resource.Resource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * An embedded servlet container that the PC uses to host itself.
 *
 * TODO [steve] This needs cleanup
 */
public class PCServletContainer implements ApplicationContextAware, InitializingBean, DisposableBean {
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    @SuppressWarnings({ "UnusedDeclaration" })
    private static final Logger logger = Logger.getLogger(PCServletContainer.class.getName());

    private static final AtomicLong nextInstanceId = new AtomicLong(1);
    private static final Map<Long, Reference<PCServletContainer>> instancesById =
            new ConcurrentHashMap<Long, Reference<PCServletContainer>>();

    private final long instanceId;
    private final int httpPort;
    private ApplicationContext applicationContext;
    private Server server;

    public PCServletContainer() {
        this.instanceId = nextInstanceId.getAndIncrement();
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<PCServletContainer>(this));

        this.httpPort = Integer.getInteger("com.l7tech.server.processcontroller.httpPort", 8765);
    }

    private void initializeServletEngine() throws Exception {
        server = new Server(httpPort);
        final Context root = new Context(server, "/", Context.SESSIONS);
        root.setBaseResource(Resource.newClassPathResource("com/l7tech/server/processcontroller/resources/web"));
        root.setDisplayName("Layer 7 Process Controller");
        root.setAttribute("javax.servlet.context.tempdir", new File("/tmp")); //TODO [steve] temp directory ?
        root.addEventListener(new PCContextLoaderListener());
        root.setClassLoader(Thread.currentThread().getContextClassLoader());

        //noinspection unchecked
        final Map<String, String> initParams = root.getInitParams();
        initParams.put("contextConfigLocation", "classpath:com/l7tech/server/processcontroller/resources/processControllerWebApplicationContext.xml");
        initParams.put(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));

        final CXFServlet cxfServlet = new CXFServlet();
        final ServletHolder cxfHolder = new ServletHolder(cxfServlet);
        root.addServlet(cxfHolder, "/services/*");

        //Set DefaultServlet to handle all static resource requests
        final DefaultServlet defaultServlet = new DefaultServlet();
        final ServletHolder defaultHolder = new ServletHolder(defaultServlet);
        root.addServlet(defaultHolder, "/");

        for (Connector c : server.getConnectors()) {
            c.setHost("localhost"); // TODO make this configurable in case of EM
        }

        server.start();
    }

    private void shutdownServletEngine() throws Exception {
        server.stop();
        server.destroy();
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
    public static PCServletContainer getInstance(long id) {
        Reference<PCServletContainer> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }
}