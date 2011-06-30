package com.l7tech.external.assertions.ssh.server;

import com.l7tech.server.LifecycleException;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listener that ensures the SSH module gets initialized.
 */
public class SshServerModuleLoadListener {
    private static final Logger logger = Logger.getLogger(SshServerModuleLoadListener.class.getName());
    private static SshServerModule instance;

    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "SSH module is already initialized");
        } else {
            instance = SshServerModule.createModule(context);
            instance.registerApplicationEventListener();
            try {
                instance.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "SSH module threw exception on startup: " + ExceptionUtils.getMessage(e), e);
            }
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "SSH module is shutting down");
            try {
                instance.destroy();

            } catch (Exception e) {
                logger.log(Level.WARNING, "SSH module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
    }

}
