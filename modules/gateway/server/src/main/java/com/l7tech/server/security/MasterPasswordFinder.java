package com.l7tech.server.security;

/**
 * Interface implemented by strategies for obtaining the master password.
 */
public interface MasterPasswordFinder {
    /**
     * Get the Master Password from wherever this MasterPasswordFinder gets it.
     *
     * @return the master password.  Never empty or null.
     * @throws IllegalStateException if no master password can be found.
     */
    char[] findMasterPassword();
}
