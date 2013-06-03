package com.l7tech.external.assertions.ftprouting.server;

import com.l7tech.server.LifecycleException;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author nilic
 */
public class FtpServerModuleLoadListener {

    private static final Logger logger = Logger.getLogger(FtpServerModuleLoadListener.class.getName());
    private static FtpServerModule instance;

    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "FTP module is already initialized");
        } else {
            instance = FtpServerModule.createModule(context);
            try {
                instance.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "FTP module threw exception on startup: " + ExceptionUtils.getMessage(e), e);
                instance = null;
            }
        }
    }

    /*
     * Called reflectively by module class loader when module is unloaded, to ask us to clean up any globals
     * that would otherwise keep our instances from getting collected.
     */
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            logger.log(Level.INFO, "FTP module is shutting down");
            try {
                instance.destroy();

            } catch (Exception e) {
                logger.log(Level.WARNING, "FTP module threw exception on shutdown: " + ExceptionUtils.getMessage(e), e);
            } finally {
                instance = null;
            }
        }
    }
}
