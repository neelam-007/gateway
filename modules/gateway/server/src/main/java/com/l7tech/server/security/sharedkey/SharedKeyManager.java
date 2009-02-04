package com.l7tech.server.security.sharedkey;

import com.l7tech.objectmodel.FindException;

/**
 * Allow SSG access to a symmetric key shared throughout the cluster. This key is
 * stored in the database, itself secured with the SSL keystore (encrypted with pub
 * key and decrypted with the private key).
 *
 * The shared key is initially created the first time it is requested. This is done
 * atomically through the cluster.
 *
 * Whenever the SSL keystore, the shared key will be re-encrypted using the new SSL
 * keystore. This will be handled by the Config Wizard.
 *
 * <p/>
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: May 18, 2007<br/>
 */
public interface SharedKeyManager {
    
    /**
     * Get the shared key
     *
     * @return byte[64] containing the shared symmetric key (raw, unencrypted)
     * @throws com.l7tech.objectmodel.FindException if there was a problem finding or creating the key
     */
    byte[] getSharedKey() throws FindException;

    /**
     * Get the shared key encrypted for the given identifier.
     *
     * @param identifier The encryption identifier.
     * @return The Base64 encoded encrypted shared key.
     * @throws FindException If the key cannot be found.
     */
    String getEncrytpedSharedKey( String identifier ) throws FindException;

    /**
     * Save the given shared key.
     *
     * @param key The key to save
     * @throws FindException If an error occurs.
     */
    void saveAndEncryptSharedKey( final byte[] key ) throws FindException;
}
