package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.server.LifecycleException;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener that ensures the simple raw transport module gets initialized.
 */
public class ModuleLoadListener {
    private static final Logger logger = Logger.getLogger(ModuleLoadListener.class.getName());
    private static SimpleRawTransportModule instance;

    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "Simple raw transport module is already initialized");
        } else {
            instance = SimpleRawTransportModule.createModule(context);
            try {
                instance.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "Simple raw transport module threw exception on startup: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "Simple raw transport module is shutting down");
            try {
                instance.destroy();

            } catch (Exception e) {
                logger.log(Level.WARNING, "Simple raw transport module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
    }

}
