package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;

/**
 * KeyAccessFilter that permits access to all keys
 */
public class PermissiveKeyAccessFilter implements KeyAccessFilter {

    /**
     * Check whether the specified key entry's private key should be usable in the current environment.
     *
     * @param keyEntry the key entry to examine.
     * @return This implementation always returns false.
     */
    @Override
    public boolean isRestrictedAccessKeyEntry( final SsgKeyEntry keyEntry ) {
        return false;
    }
}
