package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;

/**
 * Interface implemented by beans that can decide whether use of a given private key entry's private key
 * might need to be restricted.
 */
public interface KeyAccessFilter {
    /**
     * Check whether the specified key entry's private key should be usable in the current environment.
     *
     * @param keyEntry the key entry to examine.  Required.  The KeyAccessFilter shall not modify the key entry in any way.
     * @return true if the specified key entry should be marked as having restricted access to its private key field.  false if the key allows (relatively) unrestricted access within the Gateway.
     */
    boolean isRestrictedAccessKeyEntry(SsgKeyEntry keyEntry);
}
