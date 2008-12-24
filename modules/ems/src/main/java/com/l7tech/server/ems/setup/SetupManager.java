package com.l7tech.server.ems.setup;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import com.l7tech.objectmodel.DeleteException;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;

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
     * Save the given key / cert and return the generated alias.
     *
     * @param key The private key to use
     * @param certificateChain The certificate to use
     * @return The generated alias value
     * @throws SetupException If an error occurs
     */
    String saveSsl( PrivateKey key, X509Certificate[] certificateChain ) throws SetupException;

    /**
     * Generate an SSL certificate for the ginve host returning the generated alias.
     *
     * @param hostname The hostname to generate a certificate for.
     * @return The generated alias value
     * @throws SetupException If an error occurs
     */
    String generateSsl( String hostname ) throws SetupException;

    /**
     * Use the given alias for the SSL listener.
     *
     * @param alias The alias to use.
     * @throws SetupException If an error occurs
     */
    void setSslAlias( String alias ) throws SetupException;

    /**
     * Set the timeout to use for any new HTTP sessions.
     *
     * @param sessionTimeout The timeout in seconds
     * @throws SetupException If an error occurs.
     */
    void setSessionTimeout( int sessionTimeout ) throws SetupException;
}
