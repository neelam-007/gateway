package com.l7tech.gateway.common.spring.util;

import java.lang.ref.WeakReference;
import java.util.logging.Logger;
import java.util.Map;

import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationContext;

/**
 * Application listener implementation that allows garbage collection.
 *
 * <p>This listener holds a weak reference to a wrapped listener so that garbage
 * collection is not prevented. When the underlying listener is no longer available
 * this listener will remove itself from the given ApplicationEventMulticaster.</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 * @see ApplicationEventMulticaster
 * @see ApplicationListener
 */
public class WeakReferenceApplicationListener extends DelegatingApplicationListener {

    //- PUBLIC

    /**
     * Create a WeakReferenceApplicationListener.
     *
     * @param applicationEventMulticaster The multicaster this listener is registered with.
     * @param applicationListener         The wrapped listener
     */
    public WeakReferenceApplicationListener(ApplicationEventMulticaster applicationEventMulticaster, ApplicationListener applicationListener) {
        super(null);
        this.applicationEventMulticaster = applicationEventMulticaster;
        this.listenerRef = new WeakReference(applicationListener);
    }

    /**
     * Utility method that creates a WeakReferenceApplicationListener and
     * registers it with the given application context.
     *
     * @param applicationContext  The context in which to search for a multicaster
     * @param applicationListener The listener to wrap and register.
     * @return true if the listener was successfully registered.
     */
    public static boolean addApplicationListener(ApplicationContext applicationContext, ApplicationListener applicationListener) {
        boolean added = false;
        Map casters = applicationContext.getBeansOfType(ApplicationEventMulticaster.class);
        if(casters!=null && !casters.isEmpty()) {
            if(casters.size() > 1)
                logger.warning("Multiple potential event multicasters!");

            ApplicationEventMulticaster aem = (ApplicationEventMulticaster) casters.values().iterator().next();
            aem.addApplicationListener(new WeakReferenceApplicationListener(aem, applicationListener));
            added = true;
        }
        else {
            logger.warning("No available application event multicasters!");
        }
        return added;
    }

    //- PROTECTED

    /**
     * Get the weakly referenced listener if still available.
     *
     * @return the listener or null
     */
    protected ApplicationListener getApplicationListener() {
        ApplicationListener listener = (ApplicationListener) listenerRef.get();

        if(listener==null) {
            logger.info("Removing application event listener (delegate listener no longer exists).");
            applicationEventMulticaster.removeApplicationListener(this);
        }

        return listener;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(WeakReferenceApplicationListener.class.getName());

    private final ApplicationEventMulticaster applicationEventMulticaster;
    private final WeakReference listenerRef;
}
