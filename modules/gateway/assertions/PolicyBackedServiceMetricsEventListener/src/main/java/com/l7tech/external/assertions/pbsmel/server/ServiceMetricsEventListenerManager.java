package com.l7tech.external.assertions.pbsmel.server;

import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.polback.PolicyBackedService;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.extension.registry.event.EventListenerRegistry;
import com.l7tech.server.polback.PolicyBackedServiceManager;
import com.l7tech.server.polback.PolicyBackedServiceRegistry;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.util.ExceptionUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationListener;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages registration and un-registration of event listeners in the event listener registry.
 */
public class ServiceMetricsEventListenerManager {

    private static final Logger LOGGER = Logger.getLogger(ServiceMetricsEventListenerManager.class.getName());

    private static final String PBS_METRICS_EVENT_LISTENER_KEY = "service-metrics-processor-pbs-";

    private static ServiceMetricsEventListenerManager instance = null;

    private final ApplicationEventProxy applicationEventProxy;
    private final PolicyBackedServiceManager policyBackedServiceManager;
    private final PolicyBackedServiceRegistry pbsreg;
    private final EventListenerRegistry eventListenerRegistry;

    private Map<Goid, ServiceMetricsEventListener> eventListeners;
    private ApplicationListener applicationListener;

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleLoaded(ApplicationContext context) {
        if (instance == null) {
            instance = new ServiceMetricsEventListenerManager(context);
            instance.initialize();
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static synchronized void onModuleUnloaded() {
        if (instance != null) {
            instance.destroy();
            instance = null;
        }
    }

    private ServiceMetricsEventListenerManager(final ApplicationContext context) {
        this.applicationEventProxy = context.getBean("applicationEventProxy", ApplicationEventProxy.class);
        this.policyBackedServiceManager = context.getBean("policyBackedServiceManager", PolicyBackedServiceManager.class);
        this.pbsreg = context.getBean("policyBackedServiceRegistry", PolicyBackedServiceRegistry.class);
        this.eventListenerRegistry = context.getBean("eventListenerRegistry", EventListenerRegistry.class);
    }

    private void initialize() {
        this.eventListeners = new ConcurrentHashMap<>();
        this.pbsreg.registerPolicyBackedServiceTemplate(ServiceMetricsProcessor.class);

        this.applicationListener = event -> {
            if (event instanceof EntityInvalidationEvent) {
                processEntityInvalidationEvent( (EntityInvalidationEvent) event );
            }
        };
        this.applicationEventProxy.addApplicationListener(applicationListener);

        // Register event listeners for each service metrics processor policy-backed services.
        try {
            for (PolicyBackedService pbs : policyBackedServiceManager.findAll()) {
                if (ServiceMetricsProcessor.class.getName().equals(pbs.getServiceInterfaceName())) {
                    this.registerEventListener(pbs);
                }
            }
        } catch (FindException e) {
            LOGGER.log(Level.WARNING, ExceptionUtils.getDebugException(e),
                    () -> "Error finding service metrics processor policy-backed services. " + ExceptionUtils.getMessage(e));
        }
    }

    private void destroy() {
        this.applicationEventProxy.removeApplicationListener(applicationListener);
        this.applicationListener = null;

        for (Goid goid : eventListeners.keySet()) {
            this.unregisterEventListener(goid);
        }

        this.pbsreg.unregisterPolicyBackedServiceTemplate(ServiceMetricsProcessor.class);
        this.eventListeners.clear();
    }

    void processEntityInvalidationEvent(final EntityInvalidationEvent eie) {
        if (!PolicyBackedService.class.equals(eie.getEntityClass())) {
            return;
        }

        for (int ix = 0; ix < eie.getEntityIds().length; ix++) {
            final Goid goid = eie.getEntityIds()[ix];
            if (eie.getEntityOperations()[ix] == EntityInvalidationEvent.CREATE) {
                try {
                    final PolicyBackedService pbs = policyBackedServiceManager.findByPrimaryKey(goid);
                    if (pbs != null) {
                        registerEventListener(pbs);
                    }
                } catch (FindException e) {
                    LOGGER.log(Level.WARNING, ExceptionUtils.getDebugException(e),
                            () -> "Error registering service metrics event listener for policy-backed service entity (" + goid + "). " + ExceptionUtils.getMessage(e));
                }
            } else if (eie.getEntityOperations()[ix] == EntityInvalidationEvent.DELETE) {
                this.unregisterEventListener(goid);
            }
            // Ignore UPDATE event.
        }
    }

    private void registerEventListener(final PolicyBackedService pbs) {
        final Goid goid = pbs.getGoid();
        eventListeners.computeIfAbsent(goid, v -> {
            final ServiceMetricsEventListener listener = new ServiceMetricsEventListener(pbsreg, pbs);
            eventListenerRegistry.register(getExtensionKey(pbs.getGoid()), listener);
            return listener;
        });
    }

    private void unregisterEventListener(final Goid goid) {
        eventListenerRegistry.unregister(getExtensionKey(goid));
        eventListeners.remove(goid);
    }

    static String getExtensionKey(final Goid goid) {
        return PBS_METRICS_EVENT_LISTENER_KEY + Goid.toString(goid);
    }

    static ServiceMetricsEventListenerManager getInstance() {
        return instance;
    }
}
