package com.l7tech.server.security.keystore;

import com.l7tech.gateway.common.security.keystore.SsgKeyEntry;

/**
 * Interface implemented by beans that can decide whether use of a given private key entry's private key
 * should be permitted.
 */
public interface KeyAccessFilter {
    /**
     * Check whether the specified key entry's private key should be usable in the current environment.
     *
     * @param keyEntry the key entry to examine.  Required.  The KeyAccessFilter shall not modify the key entry in any way.
     * @return true if it is permissible for this key entry's private key to be used by the current thread at this time; otherwise, false.
     */
    boolean isRestrictedAccessKeyEntry(SsgKeyEntry keyEntry);
}
