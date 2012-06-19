package com.l7tech.external.assertions.comparison.server;

import com.l7tech.external.assertions.comparison.server.convert.ValueConverter;
import com.l7tech.util.DateTimeConfigUtils;
import org.springframework.context.ApplicationContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author darmstrong
 */
public class ComparisonAssertionModuleLifecycle {

    /*
     * Called by the ServerAssertionRegistry when the module containing this class is first loaded
     */
    public static synchronized void onModuleLoaded( final ApplicationContext context ) {
        if (dateTimeConfigUtils != null) {
            logger.log(Level.WARNING, "Comparison Assertion module is already initialized");
        } else {
            dateTimeConfigUtils = context.getBean(DateTimeConfigUtils.class);
            ValueConverter.Factory.setDateParser(dateTimeConfigUtils);
        }

    }

    // - PRIVATE

    private static final Logger logger = Logger.getLogger(ComparisonAssertionModuleLifecycle.class.getName());
    private static DateTimeConfigUtils dateTimeConfigUtils = null;

}
