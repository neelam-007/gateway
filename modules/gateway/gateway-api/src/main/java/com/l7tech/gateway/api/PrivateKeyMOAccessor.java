package com.l7tech.gateway.api;

import java.util.Set;

/**
 *  Accessor for PrivateKeyMO that allows import/export of keys.
 */
public interface PrivateKeyMOAccessor extends Accessor<PrivateKeyMO> {

    /**
     * Set the special purposes for a private key.
     *
     * @param identifier The identifier for the private key (required)
     * @param specialPurposes The special purposes to set (required)
     * @return The resource representing the updated private key
     */
    PrivateKeyMO setSpecialPurposes( String identifier,
                                     Set<String> specialPurposes ) throws AccessorException;

    /**
     * Generate a DER encoded Certificate Signing Request.
     *
     * @param identifier The identifier for the private key (required)
     * @param dn The DN for the CSR subject (optional)
     * @return The CSR data
     */
    byte[] generateCsr( String identifier,
                        String dn ) throws AccessorException;

    /**
     * Create a new private key.
     *
     * @param context The context to use (required)
     * @return The resource representing the new private key
     */
    PrivateKeyMO createKey( final PrivateKeyCreationContext context ) throws AccessorException;

    /**
     * Import a private key from a PKCS12 keystore.
     *
     * <p>If an alias is not supplied there must be a single entry in the given
     * keystore.</p>
     *
     * @param identifier The identifier for the new private key (required)
     * @param alias The alias in the given keystore (optional)
     * @param password The password in the given keystore (required)
     * @param keystoreBytes The keystore bytes (required)
     * @return The resource representing the new private key
     */
    PrivateKeyMO importKey( String identifier,
                            String alias,
                            String password,
                            byte[] keystoreBytes ) throws AccessorException;

    /**
     * Export a private key as a PKCS12 keystore.
     *
     * <p>If an alias is not supplied then the alias of the private key is
     * used.</p>
     *
     * @param identifier The identifier of the private key to export (required)
     * @param alias The alias to use in the keystore (optional)
     * @param password The password to use in the keystore (required)
     * @return The keystore bytes
     */
    byte[] exportKey( String identifier,
                      String alias,
                      String password ) throws AccessorException;
}
