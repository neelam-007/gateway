package com.l7tech.jini.lookup;

import com.l7tech.common.locator.ObjectFactory;
import com.l7tech.common.util.Locator;

import java.util.Collection;
import java.util.logging.Logger;

/**
 * The class <code>ServiceFactory</code> is Locator <code>ObjectFactory</code>
 * implementation that uses currently configyred Jini lookup service.
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class ServiceFactory implements ObjectFactory {
    protected static final Logger logger = Logger.getLogger(ServiceFactory.class.getName());

    /**
     * create the object instance of the class cl with
     * optional context <code>Collection</code>.
     *
     * @param cl the class that
     * @param context optional context collection
     * @return the object instance of the class type
     */
    public Object getInstance(Class cl, Collection context) {
        ServiceLookup sl =
          (ServiceLookup)Locator.getDefault().lookup(ServiceLookup.class);
        if (sl == null) {
            String msg = "Service Locator could not be obtained" +
              "Check the application services configuration";
            logger.severe(msg);
            throw new RuntimeException(msg);
        }
        return sl.lookup(cl, context);
    }
}
