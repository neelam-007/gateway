package com.l7tech.server.identity.ldap;

import com.l7tech.server.ServerConfigParams;
import com.l7tech.util.Config;
import com.l7tech.util.ValidatedConfig;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.atomic.AtomicLong;
import java.text.MessageFormat;

/**
 * Configuration bean for LDAP runtime properties.
 */
public class LdapRuntimeConfig implements PropertyChangeListener {

    //- PUBLIC

    public LdapRuntimeConfig( final Config config ) {
        this.config = validated( config );
        initializeConfigProperties();
    }

    public long getRebuildTimerLength() {
        return rebuildTimerLength.get();
    }

    public long getCleanupTimerLength() {
        return cleanupTimerLength.get();
    }

    public long getCachedCertEntryLife() {
        return cachedCertEntryLife.get();
    }

    public long getLdapConnectionTimeout() {
        return ldapConnectionTimeout.get();
    }

    public long getLdapReadTimeout() {
        return ldapReadTimeout.get();
    }

    public long getMaxSearchResultSize() {
        return maxSearchResultSize.get();
    }

    public long getMaxGroupSearchResultSize() {
        return maxGroupSearchResultSize.get();
    }

    public long getRetryFailedConnectionTimeout() {
        return retryFailedConnectionTimeout.get();
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if ( ServerConfigParams.PARAM_LDAP_CONNECTION_TIMEOUT.equals(propertyName)) {
            loadConnectionTimeout();
        } else if ( ServerConfigParams.PARAM_LDAP_READ_TIMEOUT.equals(propertyName)) {
            loadReadTimeout();
        } else if ( ServerConfigParams.PARAM_MAX_LDAP_SEARCH_RESULT_SIZE.equals(propertyName)) {
            loadMaxSearchResultSize();
            loadMaxGroupSearchResultSize(); // defaults to max search results size
        } else if ( ServerConfigParams.PARAM_MAX_LDAP_GROUP_SEARCH_RESULT_SIZE.equals(propertyName)) {
            loadMaxGroupSearchResultSize();
        } else if (PROP_RECONNECT_TIMEOUT.equals(propertyName)) {
            loadReconnectTimeout();
        } else if ( ServerConfigParams.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL.equals(propertyName)) {
            loadIndexRebuildIntervalProperty();
        } else if ( ServerConfigParams.PARAM_LDAPCERT_CACHE_LIFETIME.equals(propertyName)) {
            loadCachedCertEntryLifeProperty();
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger( LdapRuntimeConfig.class.getName() );

    private final Config config;

    private static final String PROP_RECONNECT_TIMEOUT = "ldap.reconnect.timeout";

    private static final long DEFAULT_INDEX_REBUILD_INTERVAL = 1000L * 60L * 10L; // 10 minutes
    private static final long DEFAULT_CACHE_CLEANUP_INTERVAL = 1000L * 60L * 10L; // 10 minutes
    private static final long DEFAULT_CACHED_CERT_ENTRY_LIFE = 1000L * 60L * 10L; // 10 minutes
    private static final long MIN_INDEX_REBUILD_TIME = 10000L; //10 seconds
    private static final long MIN_CERT_CACHE_LIFETIME = 10000L; //10 seconds
    private static final long DEFAULT_MAX_SEARCH_RESULT_SIZE = 100L;
    private static final long DEFAULT_RECONNECT_TIMEOUT = 60000L;

    private final AtomicLong rebuildTimerLength = new AtomicLong(DEFAULT_INDEX_REBUILD_INTERVAL);
    private final AtomicLong cleanupTimerLength = new AtomicLong(DEFAULT_CACHE_CLEANUP_INTERVAL);
    private final AtomicLong cachedCertEntryLife = new AtomicLong(DEFAULT_CACHED_CERT_ENTRY_LIFE);
    private final AtomicLong ldapConnectionTimeout = new AtomicLong( (long) LdapIdentityProvider.DEFAULT_LDAP_CONNECTION_TIMEOUT );
    private final AtomicLong ldapReadTimeout = new AtomicLong( (long) LdapIdentityProvider.DEFAULT_LDAP_READ_TIMEOUT );
    private final AtomicLong maxSearchResultSize = new AtomicLong(DEFAULT_MAX_SEARCH_RESULT_SIZE);
    private final AtomicLong maxGroupSearchResultSize = new AtomicLong(DEFAULT_MAX_SEARCH_RESULT_SIZE);
    private final AtomicLong retryFailedConnectionTimeout = new AtomicLong(DEFAULT_RECONNECT_TIMEOUT);


    private void initializeConfigProperties() {
        loadConnectionTimeout();
        loadReadTimeout();
        loadMaxSearchResultSize();
        loadMaxGroupSearchResultSize();
        loadReconnectTimeout();
        loadIndexRebuildIntervalProperty();
        loadCachedCertEntryLifeProperty();
    }

    private void loadConnectionTimeout() {
        long ldapConnectionTimeout = config.getTimeUnitProperty( ServerConfigParams.PARAM_LDAP_CONNECTION_TIMEOUT, (long) LdapIdentityProvider.DEFAULT_LDAP_CONNECTION_TIMEOUT );
        logger.config("Connection timeout = " + ldapConnectionTimeout);
        this.ldapConnectionTimeout.set(ldapConnectionTimeout);
    }

    private void loadReadTimeout() {
        long ldapReadTimeout = config.getTimeUnitProperty( ServerConfigParams.PARAM_LDAP_READ_TIMEOUT, (long) LdapIdentityProvider.DEFAULT_LDAP_READ_TIMEOUT );
        logger.config("Read timeout = " + ldapReadTimeout);
        this.ldapReadTimeout.set(ldapReadTimeout);
    }

    private void loadMaxSearchResultSize() {
        String tmp = config.getProperty( ServerConfigParams.PARAM_MAX_LDAP_SEARCH_RESULT_SIZE, Long.toString(DEFAULT_MAX_SEARCH_RESULT_SIZE));
        if (tmp == null) {
            logger.info( ServerConfigParams.PARAM_MAX_LDAP_SEARCH_RESULT_SIZE + " is not set. using default value.");
            maxSearchResultSize.set(DEFAULT_MAX_SEARCH_RESULT_SIZE);
        } else {
            try {
                long tmpl = Long.parseLong(tmp);
                if (tmpl <= 0L ) {
                    logger.info( ServerConfigParams.PARAM_MAX_LDAP_SEARCH_RESULT_SIZE + " has invalid value: " + tmp +
                                ". using default value.");
                    maxSearchResultSize.set(DEFAULT_MAX_SEARCH_RESULT_SIZE);
                } else {
                    logger.info("Read system value " + ServerConfigParams.PARAM_MAX_LDAP_SEARCH_RESULT_SIZE + " of " + tmp);
                    maxSearchResultSize.set(tmpl);
                }
            } catch (NumberFormatException e) {
                logger.log( Level.WARNING, "The property " + ServerConfigParams.PARAM_MAX_LDAP_SEARCH_RESULT_SIZE +
                                          " has an invalid format. falling back on default value.", e);
                maxSearchResultSize.set(DEFAULT_MAX_SEARCH_RESULT_SIZE);
            }
        }
    }

    private void loadMaxGroupSearchResultSize() {
        maxGroupSearchResultSize.set( config.getLongProperty( ServerConfigParams.PARAM_MAX_LDAP_GROUP_SEARCH_RESULT_SIZE, DEFAULT_MAX_SEARCH_RESULT_SIZE ) );
    }

    private void loadReconnectTimeout() {
        // configure timeout period
        String property = config.getProperty(PROP_RECONNECT_TIMEOUT, Long.toString(DEFAULT_RECONNECT_TIMEOUT));
        if (property == null || property.length() < 1) {
            retryFailedConnectionTimeout.set(DEFAULT_RECONNECT_TIMEOUT);
            logger.warning(PROP_RECONNECT_TIMEOUT + " server property not set. using default");
        } else {
            try {
                retryFailedConnectionTimeout.set(Long.parseLong(property));
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, PROP_RECONNECT_TIMEOUT + " property not configured properly. using default", e);
                retryFailedConnectionTimeout.set(DEFAULT_RECONNECT_TIMEOUT);
            }
        }
    }

    private void loadIndexRebuildIntervalProperty() {
        long indexRebuildInterval = DEFAULT_INDEX_REBUILD_INTERVAL;

        String scp = config.getProperty( ServerConfigParams.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL, Long.toString(DEFAULT_INDEX_REBUILD_INTERVAL));
        if (scp != null) {
            try {
                indexRebuildInterval = Long.parseLong(scp);
                if (indexRebuildInterval < MIN_INDEX_REBUILD_TIME) {
                    logger.info( MessageFormat.format("Property {0} is less than the minimum value {1} (configured value = {2}). Using the default value ({3} ms)",
                            ServerConfigParams.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL,
                            MIN_INDEX_REBUILD_TIME,
                            indexRebuildInterval,
                            DEFAULT_INDEX_REBUILD_INTERVAL));
                    indexRebuildInterval = DEFAULT_INDEX_REBUILD_INTERVAL;
                }
                logger.fine("Read property " + ServerConfigParams.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL + " with value " + indexRebuildInterval);
            } catch (NumberFormatException e) {
                logger.warning(MessageFormat.format("Error parsing property {0} with value {1}. Using the default value ({2})",
                        ServerConfigParams.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL,
                        scp,
                        DEFAULT_INDEX_REBUILD_INTERVAL));
            }
        }
        rebuildTimerLength.set(indexRebuildInterval);
        logger.config("Certificate index rebuild interval = " + indexRebuildInterval);
    }

    private void loadCachedCertEntryLifeProperty() {
        long cleanupLife = DEFAULT_CACHED_CERT_ENTRY_LIFE;

        String scp = config.getProperty( ServerConfigParams.PARAM_LDAPCERT_CACHE_LIFETIME, Long.toString(DEFAULT_CACHED_CERT_ENTRY_LIFE));
        if (scp != null) {
            try {
                cleanupLife = Long.parseLong(scp);
                if (cleanupLife < MIN_CERT_CACHE_LIFETIME) {
                    logger.info(MessageFormat.format("Property {0} is less than the minimum value {1} (configured value = {2}). Using the default value ({3} ms)",
                            ServerConfigParams.PARAM_LDAPCERT_CACHE_LIFETIME,
                            MIN_CERT_CACHE_LIFETIME,
                            cleanupLife,
                            DEFAULT_CACHED_CERT_ENTRY_LIFE));
                    cleanupLife = DEFAULT_CACHED_CERT_ENTRY_LIFE;
                }
            } catch (NumberFormatException e) {
                logger.warning(MessageFormat.format("Could not parse property {0} with value {1}. Using the default ({2} ms)",
                        ServerConfigParams.PARAM_LDAPCERT_CACHE_LIFETIME,
                        scp,
                        DEFAULT_CACHED_CERT_ENTRY_LIFE));
            }
        }
        cachedCertEntryLife.set(cleanupLife);
        logger.config("Certificate cache entry lifetime = " + cleanupLife);
    }

    private Config validated( final Config config ) {
        final ValidatedConfig validatedConfig = new ValidatedConfig( config, logger );

        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_MAX_LDAP_GROUP_SEARCH_RESULT_SIZE, 1 );

        return validatedConfig;
    }
}
