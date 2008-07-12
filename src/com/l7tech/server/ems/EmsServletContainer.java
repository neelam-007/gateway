package com.l7tech.server.ems;

import com.l7tech.common.Component;
import com.l7tech.common.util.ExceptionUtils;
import com.l7tech.common.util.HexUtils;
import com.l7tech.common.util.ResourceUtils;
import com.l7tech.common.util.SyspropUtil;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.event.system.TransportEvent;
import com.l7tech.server.tomcat.VirtualDirContext;
import com.l7tech.server.tomcat.VirtualDirEntry;
import com.l7tech.server.tomcat.VirtualDirEntryImpl;
import org.apache.catalina.Engine;
import org.apache.catalina.Host;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.StandardContext;
import org.apache.catalina.core.StandardThreadExecutor;
import org.apache.catalina.core.StandardWrapper;
import org.apache.catalina.startup.Embedded;
import org.apache.coyote.ProtocolHandler;
import org.apache.coyote.http11.Http11Protocol;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;

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
    private final int httpsPort;
    private ApplicationContext applicationContext;
    private Embedded embedded;
    private Engine engine;
    private Host host;
    private StandardContext context;
    private StandardThreadExecutor executor;

    public EmsServletContainer(int httpsPort) {
        this.httpsPort = httpsPort;
        this.instanceId = nextInstanceId.getAndIncrement();
        //noinspection ThisEscapedInObjectConstruction
        instancesById.put(instanceId, new WeakReference<EmsServletContainer>(this));
    }


    private void initializeServletEngine() throws LifecycleException {
        embedded = new Embedded();

        // Create the thread pool
        executor = new StandardThreadExecutor();
        executor.setName("executor");
        executor.setDaemon(true);
        executor.setMaxIdleTime(SyspropUtil.getInteger("com.l7tech.server.ems.http.maxIdleTime", 60000));
        executor.setMaxThreads(SyspropUtil.getInteger("com.l7tech.server.ems.http.maxThreads", 100));
        executor.setMinSpareThreads(SyspropUtil.getInteger("com.l7tech.server.ems.http.minSpareThreads", 15));
        embedded.addExecutor(executor);
        try {
            executor.start();
        } catch (org.apache.catalina.LifecycleException e) {
            final String msg = "Unable to start executor for HTTP/HTTPS connections: " + ExceptionUtils.getMessage(e);
            getApplicationContext().publishEvent(new TransportEvent(this, Component.GW_HTTPRECV, null, Level.WARNING, "Error", msg));
            throw new LifecycleException(msg, e);
        }

        engine = embedded.createEngine();
        engine.setName("ssg");
        engine.setDefaultHost(getListenAddress());
        embedded.addEngine(engine);

        final String s = new File(".").getAbsolutePath();
        host = embedded.createHost(getListenAddress(), s);
        engine.addChild(host);

        context = (StandardContext)embedded.createContext("", s);
        context.addParameter(INIT_PARAM_INSTANCE_ID, Long.toString(instanceId));
        context.setName("");
        context.setResources(new VirtualDirContext("VirtualInf")); // no resources

        context.setDisplayName("Layer 7 Enterprise Service Manager Server");
        context.addParameter("contextConfigLocation", "classpath:com/l7tech/server/ems/resources/webApplicationContext.xml");
        context.addApplicationListener("com.l7tech.server.ems.EmsContextLoaderListener");

        StandardWrapper dflt = (StandardWrapper)context.createWrapper();
        dflt.setServletClass(EmsRestServlet.class.getName());
        dflt.setName("default");
        dflt.setLoadOnStartup(1);
        context.addChild(dflt);
        context.addServletMapping("/", "default");

        host.addChild(context);

        Connector c = embedded.createConnector((String)null, httpsPort, "http");
        c.setEnableLookups(false);
        ProtocolHandler ph = c.getProtocolHandler();
        if (ph instanceof Http11Protocol) {
            ((Http11Protocol)ph).setExecutor(executor);
        } else
            throw new LifecycleException("Unable to start HTTP listener on port " + c.getPort() + ": Unrecognized protocol handler: " + ph.getClass().getName());
        embedded.addConnector(c);
        try {
            embedded.start();
        } catch (org.apache.catalina.LifecycleException e) {
            throw new LifecycleException(e);
        }
    }

    /**
     * Create a VirtualDirEntry out of an item in the server resources classpath.
     *
     * @param name the local name, ie "web.xml".
     * @return a VirtualDirEntry.  never null
     */
    private VirtualDirEntry createDirEntryFromClassPathResource(String name) {
        InputStream is = null;
        try {
            String fullname = RESOURCE_PREFIX + name;
            is = getClass().getClassLoader().getResourceAsStream(fullname);
            if (is == null)
                throw new MissingResourceException("Missing resource: " + fullname, getClass().getName(), fullname);
            return new VirtualDirEntryImpl(name, HexUtils.slurpStream(is));
        } catch (IOException e) {
            throw (MissingResourceException)new MissingResourceException("Error reading resource: " + name, getClass().getName(), name).initCause(e);
        } finally {
            ResourceUtils.closeQuietly(is);
        }
    }

    public void afterPropertiesSet() throws Exception {
        initializeServletEngine();
    }

    public void destroy() throws Exception {
        embedded.destroy();
    }

    private static String getListenAddress() {
        String addr;
        try {
            addr = InetAddress.getLocalHost().getCanonicalHostName();
        } catch (UnknownHostException e) {
            addr = "0.0.0.0";
        }
        return addr;
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
     * Find the HttpTransportModule corresponding to the specified instance ID.
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
