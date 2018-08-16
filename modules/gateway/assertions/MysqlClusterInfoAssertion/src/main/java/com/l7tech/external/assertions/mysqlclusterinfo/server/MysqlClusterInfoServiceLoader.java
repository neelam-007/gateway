package com.l7tech.external.assertions.mysqlclusterinfo.server;

import com.l7tech.server.cluster.ClusterInfoManager;
import com.l7tech.server.extension.registry.sharedstate.SharedClusterInfoServiceRegistry;
import com.l7tech.util.Config;
import org.springframework.context.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MysqlClusterInfoServiceLoader {
    private static final Logger LOGGER = Logger.getLogger(MysqlClusterInfoServiceLoader.class.getName());

    private static MysqlClusterInfoServiceLoader instance;
    private final SharedClusterInfoServiceRegistry clusterInfoServiceRegistry;
    private final ClusterInfoManager clusterInfoManager;
    private final Config serverConfig;

    private MysqlClusterInfoServiceLoader(final ApplicationContext context) {
        this.clusterInfoServiceRegistry = context.getBean("sharedClusterInfoServiceRegistry", SharedClusterInfoServiceRegistry.class);
        this.clusterInfoManager = context.getBean("clusterInfoManager", ClusterInfoManager.class);
        this.serverConfig = context.getBean("serverConfig", Config.class);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        LOGGER.log(Level.FINE, "start loading module");
        if (instance == null) {
            instance = new MysqlClusterInfoServiceLoader(context);
            instance.initialize();
        }
        LOGGER.log(Level.FINE, "end loading module");
    }

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleUnloaded() {
        LOGGER.log(Level.FINE, "start unloading module");
        if (instance != null) {
            instance.destroy();
            instance = null;
        }
        LOGGER.log(Level.FINE, "end unloading module");
    }

    private void destroy() {
        LOGGER.log(Level.FINE, "start unregister module");
        this.clusterInfoServiceRegistry.unregister(MysqlClusterInfoService.KEY);
        LOGGER.log(Level.FINE, "end unregister module");
    }

    private void initialize() {
        LOGGER.log(Level.FINE, "start initialize module");
        this.clusterInfoServiceRegistry.register(MysqlClusterInfoService.KEY,
                new MysqlClusterInfoService(clusterInfoManager, serverConfig), MysqlClusterInfoService.KEY);
        LOGGER.log(Level.FINE, "end initialize module");

    }
}
