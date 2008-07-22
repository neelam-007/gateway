package com.l7tech.server.policy.assertion;

import java.net.URL;
import java.util.Collection;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.logging.Logger;
import java.util.logging.Level;

import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.common.http.HttpHeaders;

/**
 * Composite RoutingResultListener that calls all sub-listeners.
 *
 * @author $Author$
 * @version $Revision$
 */
public class CompositeRoutingResultListener implements RoutingResultListener {

    /**
     *
     */
    public CompositeRoutingResultListener() {
        listeners = new LinkedHashMap();
    }

    /**
     *
     */
    public void addListener(RoutingResultListener listener) {
        listeners.put(listener, new SafeRoutingResultListener(listener));
    }

    /**
     *
     */
    public void removeListener(RoutingResultListener listener) {
        listeners.remove(listener);
    }

    /**
     * Call all child listeners with the given parameters.
     *
     * @return true if ANY child returns true.
     */
    public boolean reroute(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
        boolean result = false;

        for (Iterator iterator = listeners.values().iterator(); iterator.hasNext();) {
            RoutingResultListener routingResultListener = (RoutingResultListener) iterator.next();
            if(routingResultListener.reroute(routedUrl, status, headers, context)) {
                result = true;
            }
        }

        return result;
    }

    /**
     * Call all child listeners with the given parameters.
     */
    public void routed(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
        for (Iterator iterator = listeners.values().iterator(); iterator.hasNext();) {
            RoutingResultListener routingResultListener = (RoutingResultListener) iterator.next();
            routingResultListener.routed(routedUrl, status, headers, context);
        }
    }

    /**
     * Call all child listeners with the given parameters.
     */
    public void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context) {
        for (Iterator iterator = listeners.values().iterator(); iterator.hasNext();) {
            RoutingResultListener routingResultListener = (RoutingResultListener) iterator.next();
            routingResultListener.failed(attemptedUrl, thrown, context);
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(CompositeRoutingResultListener.class.getName());

    private Map listeners;

    /**
     * Listener that handles a null delegate and unexpected exception.
     */
    private static class SafeRoutingResultListener implements RoutingResultListener
    {
        private final RoutingResultListener delegate;

        private SafeRoutingResultListener(RoutingResultListener delegate) {
            this.delegate = delegate;
        }

        public boolean reroute(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
            boolean value = false;
            if(delegate!=null) {
                try {
                    value = delegate.reroute(routedUrl, status, headers, context);
                }
                catch(Exception e) {
                    logger.log(Level.WARNING, "Unhandled exception in routing result listener.", e);
                }
            }
            return value;
        }

        public void routed(URL routedUrl, int status, HttpHeaders headers, PolicyEnforcementContext context) {
            if(delegate!=null) {
                try {
                    delegate.routed(routedUrl, status, headers, context);
                }
                catch(Exception e) {
                    logger.log(Level.WARNING, "Unhandled exception in routing result listener.", e);
                }
            }
        }

        public void failed(URL attemptedUrl, Throwable thrown, PolicyEnforcementContext context) {
            if(delegate!=null) {
                try {
                    delegate.failed(attemptedUrl, thrown, context);
                }
                catch(Exception e) {
                    logger.log(Level.WARNING, "Unhandled exception in routing result listener.", e);
                }
            }
        }
    }
}
