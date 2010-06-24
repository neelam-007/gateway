package com.l7tech.server.ems.setup;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.DeleteException;

/**
 * Encapsulates behavior for setup of an ESM instance.
 */
@Transactional(propagation=Propagation.REQUIRED, rollbackFor=Throwable.class)
public interface SetupManager {

    /**
     * Get the ID for the ESM instance.
     *
     * @return The ID (which is a GUID).
     */
    String getEsmId();

    /**
     * Delete the current license.
     *
     * @throws DeleteException if an error occurs.
     */
    void deleteLicense() throws DeleteException;

    /**
     * Configure an HTTPS listener for the given ip / port.
     *
     * @param ipaddress The ipaddress to listen on
     * @param port The port to listen on
     * @throw SetupException if an error occurs.
     */
    void configureListener( String ipaddress, int port ) throws SetupException;

    /**
     * Set the timeout to use for any new HTTP sessions.
     *
     * @param sessionTimeout The timeout in seconds
     * @throws SetupException If an error occurs.
     */
    void setSessionTimeout( int sessionTimeout ) throws SetupException;
}
