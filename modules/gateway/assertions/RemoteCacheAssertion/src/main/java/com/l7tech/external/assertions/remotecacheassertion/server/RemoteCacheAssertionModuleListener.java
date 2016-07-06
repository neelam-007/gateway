package com.l7tech.external.assertions.remotecacheassertion.server;

import com.l7tech.external.assertions.remotecacheassertion.RemoteCacheEntity;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.entity.GenericEntityMetadata;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.io.File;

/**
 * Created with IntelliJ IDEA.
 * User: kpak
 * Date: 6/21/12
 * Time: 10:34 AM
 * To change this template use File | Settings | File Templates.
 */
public class RemoteCacheAssertionModuleListener implements ApplicationListener {
    private static GenericEntityManager gem;

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

    @SuppressWarnings({"UnusedDeclaration"})
    public static synchronized void onModuleUnloaded (ApplicationContext context) {
        if (gem != null) {
            if (gem.isRegistered(RemoteCacheEntity.class.getName())) {
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

    @Override
    public void onApplicationEvent(ApplicationEvent applicationEvent) {
        // Do nothing.
    }
}
