package com.l7tech.policy.exporter;

/**
 * Iterface for subscription to external reference errors.
 */
public interface ExternalReferenceErrorListener {

    void warning( String title, String message );    
}
