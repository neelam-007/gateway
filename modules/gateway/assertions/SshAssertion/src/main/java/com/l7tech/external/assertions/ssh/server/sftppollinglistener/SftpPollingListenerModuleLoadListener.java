package com.l7tech.external.assertions.ssh.server.sftppollinglistener;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.cluster.ClusterPropertyManager;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.server.util.ThreadPoolBean;
import com.l7tech.util.ExceptionUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Load logic to initialize the SFTP Polling Listener module.
 */
public class SftpPollingListenerModuleLoadListener implements SftpPollingListenerConstants {
    private static final Logger logger = Logger.getLogger(SftpPollingListenerModuleLoadListener.class.getName());

    // Manages all SFTP polling listener processes
    private static SftpPollingListenerModule sftpPollingListenerProcess;

    // SSG Application event proxy
    private static ApplicationEventProxy applicationEventProxy;

    // SSG cluster property manager
    private static ClusterPropertyManager clusterPropertyManager;

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        if (applicationEventProxy == null)
            applicationEventProxy = getBean(context, "applicationEventProxy", ApplicationEventProxy.class);

        if (clusterPropertyManager == null)
            clusterPropertyManager = getBean(context, "clusterPropertyManager", ClusterPropertyManager.class);

        if (sftpPollingListenerProcess != null) {
            logger.log(Level.WARNING, "SFTP polling listener module is already initialized");
        } else {
            // (1) Create (if does not exist) all context variables used by this module
            initializeModuleClusterProperties(ServerConfig.getInstance());

            // (2) Instantiate the SFTP polling boot process
            LicenseManager lm = context.getBean("licenseManager", LicenseManager.class);
            ThreadPoolBean pool = new ThreadPoolBean(
                    ServerConfig.getInstance(),
                    "SFTP polling Listener Pool",
                    LISTENER_THREAD_LIMIT_PROPERTY,
                    LISTENER_THREAD_LIMIT_UI_PROPERTY,
                    25);
            sftpPollingListenerProcess = new SftpPollingListenerModule(pool, clusterPropertyManager, lm, SftpPollingListenerResourceManager.getInstance(context));
            sftpPollingListenerProcess.setApplicationContext(context);
            applicationEventProxy.addApplicationListener(sftpPollingListenerProcess);

            // (3) Start the module
            try {
                // Start the boot process which will start the listeners
                logger.log(Level.INFO, "SftpPollingListenerModuleLoadListener - starting SftpPollingListenerModule...");
                sftpPollingListenerProcess.doStart();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "SFTP polling listener module threw exception during startup: " + ExceptionUtils.getMessage(e), e);
            }

            // Only initialize all the resource managers when the SSG is "ready for messages"
            applicationEventProxy.addApplicationListener( new ApplicationListener() {
                @Override
                public void onApplicationEvent(ApplicationEvent event) {

                    // only do this when the SSG is ready for messages
                    if (event instanceof ReadyForMessages) {
                        // initialize the config resource manager (queue config persistent store)
                        SftpPollingListenerResourceManager resMgr = SftpPollingListenerResourceManager.getInstance(context);
                        resMgr.init();
                    }
                }
            } );
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded() {
        if (sftpPollingListenerProcess != null) {
            logger.log(Level.INFO, "Shutdown SFTP polling listener module");
            try {
                sftpPollingListenerProcess.doStop();
                sftpPollingListenerProcess.destroy();

            } catch (Exception e) {
                logger.log(Level.WARNING, "SFTP polling listener module threw exception during shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                sftpPollingListenerProcess = null;
            }
        }
    }

    private static <T> T getBean(BeanFactory beanFactory, String beanName, Class<T> beanClass) {
        T got = beanFactory.getBean(beanName, beanClass);
        if (got != null && beanClass.isAssignableFrom(got.getClass()))
            return got;
        throw new IllegalStateException("Unable to get bean from application context: " + beanName);
    }

    /**
     * Checks for the existence of all cluster properties used by this module. If one does not already exist, then
     * it is created with the default value.
     *
     * @param config ServerConfig instance
     */
    private static void initializeModuleClusterProperties(final ServerConfig config) {

        Map<String, String> names = config.getClusterPropertyNames();

        List<String[]> toAdd = new ArrayList<String[]>();
        for (String[] tuple : MODULE_CLUSTER_PROPERTIES) {
            if (!names.containsKey(tuple[0])) {
                // create it
                toAdd.add(tuple);
            }
        }
        config.registerServerConfigProperties(toAdd.toArray(new String[toAdd.size()][]));
    }
}
