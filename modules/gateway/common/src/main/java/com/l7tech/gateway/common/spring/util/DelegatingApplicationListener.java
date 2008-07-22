package com.l7tech.gateway.common.spring.util;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.springframework.context.ApplicationListener;
import org.springframework.context.ApplicationEvent;

/**
 * ApplicationListener that delegates to the given object.
 *
 * <p>The wrapped listener may be null. Any exceptions thrown will
 * be logged and not propagated (including runtime exceptions).</p>
 *
 * @author Steve Jones, $Author$
 * @version $Revision$
 */
public class DelegatingApplicationListener implements ApplicationListener {

    //- PUBLIC

    /**
     * Create a DelegatingApplicationListener with the given delegate.
     *
     * @param applicationListener The ApplicationListener to add.
     */
    public DelegatingApplicationListener(ApplicationListener applicationListener) {
        this.applicationListener = applicationListener;
    }

    /**
     * Handle an application event.
     *
     * <p>The underlying listener is passed the event (if not null)</p>
     *
     * @param event The ApplicationEvent
     */
    public void onApplicationEvent(ApplicationEvent event) {
        ApplicationListener applicationListener = getApplicationListener();
        if(applicationListener!=null) {
            try {
                applicationListener.onApplicationEvent(event);
            }
            catch(Exception e) {
                logger.log(Level.WARNING, "Exception in event listener.", e);
            }
        }
    }

    //- PROTECTED

    /**
     * Get the delegate listener.
     *
     * <p>This may return null.</p>
     *
     * @return The listener.
     */
    protected ApplicationListener getApplicationListener() {
        return applicationListener;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(DelegatingApplicationListener.class.getName());

    private final ApplicationListener applicationListener;
}
