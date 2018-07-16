package com.l7tech.server.identity.ldap;

import com.l7tech.identity.ldap.LdapUrlBasedIdentityProviderConfig;
import com.l7tech.server.ServerConfig;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.l7tech.identity.ldap.LdapUrlBasedIdentityProviderConfig.PROP_LDAP_RECONNECT_TIMEOUT;

/**
 * Manages a list of LDAP URLs and keeps track of which one last worked.
 */
public class LdapUrlProviderImpl implements LdapUrlProvider {
    private static final Logger logger = Logger.getLogger(LdapUrlProviderImpl.class.getName());
    private static final Long DEFAULT_RECONNECT_TIMEOUT = 60000L;

    private final ReentrantReadWriteLock fallbackLock = new ReentrantReadWriteLock();
    private final LdapUrlBasedIdentityProviderConfig providerConfig;
    private final String[] ldapUrls;
    private final ServerConfig serverConfig;
    private final Long[] urlStatus;
    private String lastSuccessfulLdapUrl;

    public LdapUrlProviderImpl(LdapUrlBasedIdentityProviderConfig providerConfig, ServerConfig serverConfig) {
        this.providerConfig = providerConfig;
        this.ldapUrls = providerConfig.getLdapUrl();
        this.serverConfig = serverConfig;
        if (ldapUrls != null) {
            urlStatus = new Long[ldapUrls.length];
            lastSuccessfulLdapUrl = ldapUrls[0];
        } else {
            urlStatus = new Long[0];
        }
    }

    /**
     * @return The ldap url that was last used to successfully connect to the ldap directory. May be null if
     *         previous attempt failed on all available urls.
     */
    @Override
    public String getLastWorkingLdapUrl() {
        final Lock read = fallbackLock.readLock();
        try {
            read.lockInterruptibly();
            return lastSuccessfulLdapUrl;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            read.unlock();
        }
    }

    /**
     * Remember that the passed URL could not be used to connect normally and get the next ldap URL from
     * the list that should be tried to connect with. Will return null if all known urls have failed to
     * connect in the last while. (last while being a configurable timeout period defined in
     * serverconfig.properties under ldapReconnectTimeout in ms)
     *
     * @param urlThatFailed the url that failed to connect, or null if no url was previously available
     * @return the next url in the list or null if all urls were marked as failure within the last while
     */
    @Override
    public String markCurrentUrlFailureAndGetFirstAvailableOne(String urlThatFailed) {
        final Lock write = fallbackLock.writeLock();
        try {
            write.lockInterruptibly();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        try {
            Long retryFailedConnectionTimeout = providerConfig.getReconnectTimeout();
            if (retryFailedConnectionTimeout == null) {
                try {
                    retryFailedConnectionTimeout = Long.parseLong(serverConfig.getProperty(PROP_LDAP_RECONNECT_TIMEOUT));
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, PROP_LDAP_RECONNECT_TIMEOUT + " property not configured properly. using default", e);
                    retryFailedConnectionTimeout = DEFAULT_RECONNECT_TIMEOUT;
                }
            }

            //noinspection StringEquality
            if (urlThatFailed != lastSuccessfulLdapUrl) return lastSuccessfulLdapUrl;
            if (urlThatFailed != null) {
                int failurePos = 0;
                for (int i = 0; i < ldapUrls.length; i++) {
                    //noinspection StringEquality
                    if (ldapUrls[i] == urlThatFailed) {
                        failurePos = i;
                        urlStatus[i] = System.currentTimeMillis();
                        logger.info("Blacklisting url for next " + (retryFailedConnectionTimeout / 1000) +
                                " seconds : " + ldapUrls[i]);
                    }
                }
                if (failurePos > (ldapUrls.length - 1)) {
                    throw new RuntimeException("passed a url not in list"); // this should not happen
                }
            }
            // find first available url
            for (int i = 0; i < ldapUrls.length; i++) {
                boolean thisoneok = false;
                if (urlStatus[i] == null) {
                    thisoneok = true;
                    logger.fine("Try url not on blacklist yet " + ldapUrls[i]);
                } else {
                    long howLong = System.currentTimeMillis() - urlStatus[i];
                    if (howLong > retryFailedConnectionTimeout) {
                        thisoneok = true;
                        urlStatus[i] = null;
                        logger.fine("Ldap URL has been blacklisted long enough. Trying it again: " + ldapUrls[i]);
                    }
                }
                if (thisoneok) {
                    logger.info("Trying to recover using this url: " + ldapUrls[i]);
                    lastSuccessfulLdapUrl = ldapUrls[i];
                    return lastSuccessfulLdapUrl;
                }
            }
            logger.fine("All ldap urls are blacklisted.");
            lastSuccessfulLdapUrl = null;
            return null;
        } finally {
            write.unlock();
        }
    }
}
