package com.l7tech.server.ems;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.ServletHolder;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.servlet.Servlet;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * An embedded servlet container that the EMS uses to host itself.
 */
public class EmsServletContainer implements ApplicationContextAware, InitializingBean, DisposableBean {
    public static final String RESOURCE_PREFIX = "com/l7tech/server/ems/resources/";
    public static final String INIT_PARAM_INSTANCE_ID = "httpTransportModuleInstanceId";

    private static final AtomicLong nextInstanceId = new AtomicLong(1);
    private static final Map<Long, Reference<EmsServletContainer>> instancesById =
            new ConcurrentHashMap<Long, Reference<EmsServletContainer>>();

    private final long instanceId;
    private final int httpPort;
    private final Servlet emsRestServlet;
    private ApplicationContext applicationContext;
    private Server server;

    public EmsServletContainer(int httpPort, Servlet emsRestServlet) {
        this.instanceId = nextInstanceId.getAndIncrement();
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<EmsServletContainer>(this));

        this.httpPort = httpPort;
        this.emsRestServlet = emsRestServlet;
    }

    private void initializeServletEngine() throws Exception {
        server = new Server(httpPort);
        Context root = new Context(server, "/", Context.SESSIONS);
        root.setDisplayName("Layer 7 Enterprise Service Manager Server");

        //noinspection unchecked
        final Map<String, String> initParams = root.getInitParams();
        initParams.put("contextConfigLocation", "classpath:com/l7tech/server/ems/resources/webApplicationContext.xml");
        initParams.put(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));
        
        root.addEventListener(new EmsContextLoaderListener());
        root.addServlet(new ServletHolder(emsRestServlet), "/*");
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
    public static EmsServletContainer getInstance(long id) {
        Reference<EmsServletContainer> instance = instancesById.get(id);
        return instance == null ? null : instance.get();
    }
}
