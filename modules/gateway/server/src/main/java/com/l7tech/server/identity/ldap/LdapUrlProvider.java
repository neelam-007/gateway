package com.l7tech.server.identity.ldap;

import org.jetbrains.annotations.Nullable;

/**
 * Interface implemented by providers of LDAP URLs.
 */
public interface LdapUrlProvider {
    /**
     * Get the last working LDAP URL.
     *
     * @return The ldap url that was last used to successfully connect to the ldap directory. May be null if
     *         previous attempt failed on all available urls.
     */
    String getLastWorkingLdapUrl();

    /**
     * Remember that the passed URL could not be used to connect normally and get the next ldap URL from
     * the list that should be tried to connect with. Will return null if all known urls have failed to
     * connect in the last while. (last while being a configurable timeout period defined in
     * serverconfig.properties under ldap.reconnect.timeout in ms)
     *
     * @param urlThatFailed the url that failed to connect, or null if no url was previously available
     * @return the next url in the list or null if all urls were marked as failure within the last while
     */
    String markCurrentUrlFailureAndGetFirstAvailableOne(@Nullable String urlThatFailed);
}
