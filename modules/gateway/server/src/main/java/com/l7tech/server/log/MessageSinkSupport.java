package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.StringTokenizer;

import com.l7tech.common.log.HybridDiagnosticContextKeys;
import com.l7tech.common.log.HybridDiagnosticContextMatcher;
import com.l7tech.common.log.HybridDiagnosticContextMatcher.MatcherRules;
import com.l7tech.gateway.common.log.SinkConfiguration;

/**
 * Support class for MessageSink implementations.
 *
 * @author Steve Jones
 */
abstract class MessageSinkSupport implements MessageSink {

    //- PUBLIC

    @Override
    public void message( final MessageCategory category, final LogRecord record ) {        
        if ( record != null && acceptMessage( category, record ) ) {
            processMessage( category, record );
        }
    }

    //- PACKAGE

    MessageSinkSupport( final SinkConfiguration configuration ) {
        this.configuration = configuration;
        this.threshold = configuration.getSeverity().toLoggingLevel().intValue();
        this.categories = buildCategories( configuration );
        this.rules = new MatcherRules(configuration.getFilters(),HybridDiagnosticContextKeys.PREFIX_MATCH_PROPERTIES);
    }

    /**
     * Get the configuration for this sink.
     *
     * @return The SinkConfiguration.
     */
    SinkConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Get the threshold for this sink
     */
    int getThreshold() {
        return threshold;
    }

    /**
     * Is the given category enabled for this sink.
     *
     * @param category The category to check.
     * @return True if enabled.
     */
    boolean isCategoryEnabled( final MessageCategory category ) {
        boolean enabled = false;

        if ( category != null && categories.contains( category ) ) {
            enabled = true;
        }

        return enabled;
    }

    /**
     * Process the given record.
     *
     * @param category The category for the message
     * @param record The record to process
     */
    abstract void processMessage( final MessageCategory category, final LogRecord record );

    /**
     * Exception class used for initialization errors
     */
    static class ConfigurationException extends Exception {
        ConfigurationException(String message) {
            super(message);
        }

        ConfigurationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    //- PRIVATE

    private final SinkConfiguration configuration;
    private final Set<MessageCategory> categories;
    private final int threshold;
    private final MatcherRules rules;

    private boolean acceptMessage( final MessageCategory category,
                                   final LogRecord record ) {
        return
                record.getLevel().intValue() >= getThreshold() &&
                isCategoryEnabled( category ) &&
                HybridDiagnosticContextMatcher.matches( rules );
    }

    /**
     * Build a set of MessageCategories for the given configuration
     */
    private Set<MessageCategory> buildCategories( final SinkConfiguration configuration ) {
        Set<MessageCategory> categories = new HashSet<MessageCategory>();

        String categoryString = configuration.getCategories();
        if ( categoryString != null ) {
            StringTokenizer strtok = new StringTokenizer( categoryString, ",");

            while ( strtok.hasMoreTokens() ) {
                String catStr = strtok.nextToken().trim();
                try {
                    categories.add(MessageCategory.valueOf(catStr));
                } catch (IllegalArgumentException iae) {
                    // ignore invalid categories
                }
            }
        }

        return Collections.unmodifiableSet( categories );
    }
}
