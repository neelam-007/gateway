package com.l7tech.external.assertions.mysqlcounter.server;

import com.l7tech.server.extension.registry.sharedstate.SharedCounterProviderRegistry;
import org.hibernate.SessionFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("UnusedDeclaration")
public class MysqlCounterLoader {
    private static final Logger LOGGER = Logger.getLogger(MysqlCounterLoader.class.getName());

    private static MysqlCounterLoader instance;
    private final PlatformTransactionManager transactionManager;
    private final SharedCounterProviderRegistry counterProviderRegistry;
    private final SessionFactory sessionFactory;

    private MysqlCounterLoader(final ApplicationContext context) {
        this.transactionManager = context.getBean("transactionManager", PlatformTransactionManager.class);
        this.counterProviderRegistry = context.getBean("sharedCounterProviderRegistry", SharedCounterProviderRegistry.class);
        this.sessionFactory = context.getBean("sessionFactory", SessionFactory.class);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleLoaded(final ApplicationContext context) {
        LOGGER.log(Level.FINE,"start loading module");
        if (instance == null) {
            instance = new MysqlCounterLoader(context);
            instance.initialize();
        }
        LOGGER.log(Level.FINE,"end loading module");
    }

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleUnloaded() {
        LOGGER.log(Level.FINE,"start unloading module");
        if (instance != null) {
            instance.destroy();
            instance = null;
        }
        LOGGER.log(Level.FINE,"end unloading module");
    }

    private void destroy() {
        LOGGER.log(Level.FINE,"start unregister module");
        this.counterProviderRegistry.unregister(MysqlCounterProvider.KEY);
        LOGGER.log(Level.FINE,"end unregister module");
    }

    private void initialize() {
        LOGGER.log(Level.FINE,"start initialize module");
        this.counterProviderRegistry.register(MysqlCounterProvider.KEY,
                new MysqlCounterProvider(this.transactionManager, sessionFactory), MysqlCounterProvider.KEY);
        LOGGER.log(Level.FINE,"end initialize module");

    }
}
