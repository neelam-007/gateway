package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntity;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.entity.GenericEntityMetadata;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 6/21/12
 * Time: 10:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheAssertionModuleListener {
    private static final Logger LOGGER = Logger.getLogger(RemoteCacheAssertionModuleListener.class.getName());

    private static GenericEntityManager gem;
    private static ApplicationListener appListener;

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleLoaded (ApplicationContext context) {
        ServerConfig serverConfig = context.getBean("serverConfig", ServerConfig.class);

        CoherenceClassLoader coherenceClassLoader = CoherenceClassLoader.getInstance(RemoteCacheAssertionModuleListener.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        File parent = new File(coherenceClassLoader.getJarPath());
        createDirectory(parent);

        GemFireClassLoader gemFireclassLoader = GemFireClassLoader.getInstance(RemoteCacheAssertionModuleListener.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        parent = new File(gemFireclassLoader.getJarPath());
        createDirectory(parent);

        TerracottaToolkitClassLoader terracottaClassLoader = TerracottaToolkitClassLoader.getInstance(RemoteCacheAssertionModuleListener.class.getClassLoader(), serverConfig.getProperty("com.l7tech.server.home"));
        parent = new File(terracottaClassLoader.getJarPath());
        createDirectory(parent);

        registerGenericEntities(context);

        ApplicationEventProxy appEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        appListener = new ApplicationListener() {
            @Override
            public void onApplicationEvent(ApplicationEvent applicationEvent) {

                if (applicationEvent instanceof EntityInvalidationEvent) {
                    RemoteCachesManager remoteCachesManager = RemoteCachesManagerImpl.getInstance();
                    if(null == remoteCachesManager) {
                        return;
                    }
                    final EntityInvalidationEvent event = (EntityInvalidationEvent) applicationEvent;

                    if (GenericEntity.class.equals(event.getEntityClass())) {
                        Goid[] goids = event.getEntityIds();
                        char[] ops = event.getEntityOperations();
                        for (int i = 0; i < ops.length; i++) {
                            LOGGER.log(Level.FINE, "Received EntityInvalidationEvent on RemoteCacheEntity with goid {0} and action {1}", new Object[]{goids[i], ops[i]});
                            switch (ops[i]) {
                                case EntityInvalidationEvent.CREATE:
                                    // Nothing to invalidate
                                    break;
                                case EntityInvalidationEvent.DELETE:
                                case EntityInvalidationEvent.UPDATE:
                                    remoteCachesManager.invalidateRemoteCache(goids[i]);
                                    break;
                                default:
                                    LOGGER.log(Level.WARNING, "Unexpected EntityInvalidationEvent Operation: " + ops[i]);
                                    break;
                            }
                        }
                    }
                }
            }
        };
        appEventProxy.addApplicationListener(appListener);
    }

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded (ApplicationContext context) {
        LOGGER.log(Level.INFO, "Unloading module RemoteCacheAssertion...");
        ApplicationEventProxy appEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        appEventProxy.removeApplicationListener(appListener);
        unregisterGenericEntities();
    }

    /**
     * Register generic entities for this module
     * @param context
     */
    private static void registerGenericEntities(final ApplicationContext context) {
        if (gem == null) {
            gem = context.getBean("genericEntityManager", GenericEntityManager.class);
        }

        if (gem.isRegistered(RemoteCacheEntity.class.getName())) {
            gem.unRegisterClass(RemoteCacheEntity.class.getName());
        }

        final GenericEntityMetadata meta = new GenericEntityMetadata().
                addSafeXmlClasses("java.util.HashMap").
                addSafeXmlConstructors("java.util.HashMap()").
                addSafeXmlMethods(
                        "java.util.HashMap.put(java.lang.Object,java.lang.Object)",
                        "java.util.HashMap.remove(java.lang.Object)");
        gem.registerClass(RemoteCacheEntity.class, meta);
    }

    /**
     * Unregister generic entities for this module
     */
    private static void unregisterGenericEntities() {
        if (gem != null) {
            if(gem.isRegistered(RemoteCacheEntity.class.getName())) {
                gem.unRegisterClass(RemoteCacheEntity.class.getName());
            }
            gem = null;
        }
    }

    private static void createDirectory(File parent) {
        if(!parent.exists()) {
            parent.mkdir();
        } else if(!parent.isDirectory()) {
            return;
        }
    }
}
