package com.l7tech.policy.exporter;

/**
 * Interface for subscription to external reference errors.
 */
public interface ExternalReferenceErrorListener {

    void warning( String title, String message );    
}
