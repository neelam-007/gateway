package com.l7tech.service;

/**
 * @author alex
 * @version $Revision$
 */
public interface ServiceListener {
    void serviceCreated( PublishedService service );
    void serviceDeleted( PublishedService service );
    void serviceUpdated( PublishedService service );
}
