package com.l7tech.server.log;

import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.StringTokenizer;

import com.l7tech.common.log.SinkConfiguration;

/**
 * Support class for MessageSink implementations.
 *
 * @author Steve Jones
 */
abstract class MessageSinkSupport implements MessageSink {

    //- PUBLIC

    public void message( final MessageCategory category, final LogRecord record ) {        
        if ( category != null && record != null) {
            if ( categories.contains( category ) &&
                 record.getLevel().intValue() >= threshold ) {
                processMessage( category, record );
            }
        }
    }

    //- PACKAGE

    MessageSinkSupport( final SinkConfiguration configuration ) {
        this.configuration = configuration;
        this.threshold = getThreshold( configuration );
        this.categories = buildCategories( configuration );
    }

    SinkConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Process the given record.
     *
     * @param category The category for the message
     * @param record The record to process
     */
    abstract void processMessage( final MessageCategory category, final LogRecord record );

    //- PRIVATE

    private final SinkConfiguration configuration;
    private final Set<MessageCategory> categories;
    private final int threshold;

    /**
     * Get the threshold for the configuration
     */
    private int getThreshold( final SinkConfiguration configuration ) {
        Level level = Level.WARNING;

        switch ( configuration.getSeverity() ) {
            case CONFIG:
                level = Level.CONFIG;
                break;
            case INFO:
                level = Level.INFO;
                break;
            case WARNING:
                level = Level.WARNING;
                break;
            case SEVERE:
                level = Level.SEVERE;
                break;
        }

        return level.intValue();
    }

    /**
     * Build a set of MessageCategories for the given configuration
     */
    private Set<MessageCategory> buildCategories( final SinkConfiguration configuration ) {
        Set<MessageCategory> categories = new HashSet();

        String categoryString = configuration.getCategories();
        if ( categoryString != null ) {
            StringTokenizer strtok = new StringTokenizer( categoryString, ",");

            while ( strtok.hasMoreTokens() ) {
                String catStr = strtok.nextToken().trim();
                if ( SinkConfiguration.CATEGORY_GATEWAY_LOGS.equals(catStr) ) {
                    categories.add(MessageCategory.LOG);
                } else if ( SinkConfiguration.CATEGORY_TRAFFIC_LOGS.equals(catStr) ) {
                    categories.add(MessageCategory.TRAFFIC);
                } else if ( SinkConfiguration.CATEGORY_AUDITS.equals(catStr) ) {
                    categories.add(MessageCategory.AUDIT);                    
                }
            }
        }

        return Collections.unmodifiableSet( categories );
    }
}
