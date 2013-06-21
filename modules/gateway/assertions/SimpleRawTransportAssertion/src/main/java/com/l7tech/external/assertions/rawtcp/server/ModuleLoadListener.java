package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.gateway.common.transport.SsgConnector;
import com.l7tech.server.LifecycleException;
import com.l7tech.server.search.DependencyProcessorRegistry;
import com.l7tech.server.search.processors.DoNothingDependencyProcessor;
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
    private static DependencyProcessorRegistry processorRegistry;

    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance != null) {
            logger.log(Level.WARNING, "Simple raw transport module is already initialized");
        } else {
            instance = SimpleRawTransportModule.createModule(context);
            instance.registerApplicationEventListener();
            try {
                instance.start();
            } catch (LifecycleException e) {
                logger.log(Level.WARNING, "Simple raw transport module threw exception on startup: " + ExceptionUtils.getMessage(e), e);
            }

            // Get the ssg connector dependency processor map to add the tcp connector dependency processor
            //noinspection unchecked
            processorRegistry = context.getBean( "ssgConnectorDependencyProcessorRegistry", DependencyProcessorRegistry.class );
            // Us the DoNothingDependencyProcessor because the tcp connector does not add any dependencies beyond the the defaults.
            processorRegistry.register(SimpleRawTransportModule.SCHEME_RAW_TCP, new DoNothingDependencyProcessor<SsgConnector>());
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
        //remove the dependency processor
        processorRegistry.remove(SimpleRawTransportModule.SCHEME_RAW_TCP);
    }

}
