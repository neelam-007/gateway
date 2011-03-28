package com.l7tech.server.cluster;

import com.l7tech.objectmodel.ExternalEntityHeader;

/**
 * Interface for adding additional metadata to external entity headers.
 */
public interface ExternalEntityHeaderEnhancer {

    void enhance( ExternalEntityHeader header ) throws EnhancementException;

    public static final class EnhancementException extends Exception {
        public EnhancementException( final String message, final Throwable cause ) {
            super( message, cause );
        }
    }

    public static final class CompositeExternalEntityHeaderEnhancer implements ExternalEntityHeaderEnhancer {
        private final ExternalEntityHeaderEnhancer[] enhancers;

        public CompositeExternalEntityHeaderEnhancer( final ExternalEntityHeaderEnhancer[] enhancers ) {
            this.enhancers = enhancers;
        }

        @Override
        public void enhance( final ExternalEntityHeader header ) throws EnhancementException {
            for ( final ExternalEntityHeaderEnhancer enhancer : enhancers ) {
                enhancer.enhance( header );
            }
        }
    }
}
