package com.l7tech.server.identity.ldap;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.audit.SystemMessages;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.*;
import com.l7tech.identity.cert.ClientCertManager;
import com.l7tech.identity.ldap.GroupMappingConfig;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.identity.ldap.LdapUser;
import com.l7tech.identity.ldap.UserMappingConfig;
import com.l7tech.identity.mapping.IdentityMapping;
import com.l7tech.identity.mapping.LdapAttributeMapping;
import com.l7tech.kerberos.KerberosServiceTicket;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.policy.variable.VariableNameSyntaxException;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.policy.variable.ExpandVariables;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.server.identity.DigestAuthenticator;
import com.l7tech.server.identity.cert.CertificateAuthenticator;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.HexUtils;
import com.sun.jndi.ldap.LdapURL;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import javax.naming.*;
import javax.naming.directory.*;
import javax.security.auth.x500.X500Principal;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Server-side implementation of the LDAP provider.
 * <p/>
 * This handles any type of directory thgough a LdapIdentityProviderConfig
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jan 21, 2004<br/>
 */
@LdapClassLoaderRequired
public class LdapIdentityProviderImpl
        implements LdapIdentityProvider, InitializingBean, DisposableBean, ApplicationContextAware, ConfigurableIdentityProvider, PropertyChangeListener
{

    public LdapIdentityProviderImpl() {
    }

    @Override
    public void setIdentityProviderConfig( final IdentityProviderConfig configuration ) throws InvalidIdProviderCfgException {
        if (this.config != null) {
            throw new InvalidIdProviderCfgException("Provider is already configured");
        }
        this.config = (LdapIdentityProviderConfig) configuration;
        if (this.config.getLdapUrl() == null || this.config.getLdapUrl().length < 1) {
            throw new InvalidIdProviderCfgException("This config does not contain an ldap url"); // should not happen
        }

        if ( userManager == null ) {
            throw new InvalidIdProviderCfgException("UserManager is not set");
        }
        if ( groupManager == null ) {
            throw new InvalidIdProviderCfgException("GroupManager is not set");
        }

        userManager.configure( this );
        groupManager.configure( this );

        if ( this.config.getReturningAttributes() != null ) {
            returningAttributes = buildReturningAttributes();
        }

        initializeFallbackMechanism();
        initializeConfigProperties();

        if ( certIndexEnabled() ) {
            rebuildTask = new RebuildTask(this);
        }

        cleanupTask = new CleanupTask(this);
    }

    @Override
    public void startMaintenance() {
        scheduleTask( rebuildTask, rebuildTimerLength.get() );
        scheduleTask( cleanupTask, cleanupTimerLength.get() );
    }

    private void initializeConfigProperties() {
        loadConnectionTimeout();
        loadReadTimeout();
        loadMaxSearchResultSize();
        loadIndexRebuildIntervalProperty();
        loadCachedCertEntryLifeProperty();
    }

    private void loadConnectionTimeout() {
        long ldapConnectionTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_CONNECTION_TIMEOUT, DEFAULT_LDAP_CONNECTION_TIMEOUT, MAX_CACHE_AGE_VALUE);
        logger.config("Connection timeout = " + ldapConnectionTimeout);
    }

    private void loadReadTimeout() {
        long ldapReadTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_READ_TIMEOUT, DEFAULT_LDAP_READ_TIMEOUT, MAX_CACHE_AGE_VALUE);
        logger.config("Read timeout = " + ldapReadTimeout);
    }

    private void loadMaxSearchResultSize() {
        String tmp = serverConfig.getPropertyCached(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE);
        if (tmp == null) {
            logger.info(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " is not set. using default value.");
            maxSearchResultSize.set(DEFAULT_MAX_SEARCH_RESULT_SIZE);
        } else {
            try {
                long tmpl = Long.parseLong(tmp);
                if (tmpl <= 0) {
                    logger.info(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " has invalid value: " + tmp +
                                ". using default value.");
                    maxSearchResultSize.set(DEFAULT_MAX_SEARCH_RESULT_SIZE);
                } else {
                    logger.info("Read system value " + ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " of " + tmp);
                    maxSearchResultSize.set(tmpl);
                }
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "The property " + ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE +
                                          " has an invalid format. falling back on default value.", e);
                maxSearchResultSize.set(DEFAULT_MAX_SEARCH_RESULT_SIZE);
            }
        }
    }

    private void loadIndexRebuildIntervalProperty() {
        long indexRebuildInterval = DEFAULT_INDEX_REBUILD_INTERVAL;

        String scp = serverConfig.getPropertyCached(ServerConfig.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL);
        if (scp != null) {
            try {
                indexRebuildInterval = Long.parseLong(scp);
                if (indexRebuildInterval < MIN_INDEX_REBUILD_TIME) {
                    logger.info(MessageFormat.format("Property {0} is less than the minimum value {1} (configured value = {2}). Using the default value ({3} ms)",
                            ServerConfig.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL,
                            MIN_INDEX_REBUILD_TIME,
                            indexRebuildInterval,
                            DEFAULT_CACHED_CERT_ENTRY_LIFE));
                    indexRebuildInterval = DEFAULT_INDEX_REBUILD_INTERVAL;
                }
                logger.fine("Read property " + ServerConfig.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL + " with value " + indexRebuildInterval);
            } catch (NumberFormatException e) {
                logger.warning(MessageFormat.format("Error parsing property {0} with value {1}. Using the default value ({2})",
                        ServerConfig.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL,
                        scp,
                        DEFAULT_INDEX_REBUILD_INTERVAL));
            }
        }
        rebuildTimerLength.set(indexRebuildInterval);
        logger.config("Certificate index rebuild interval = " + indexRebuildInterval);
    }

    private void loadCachedCertEntryLifeProperty() {
        long cleanupLife = DEFAULT_CACHED_CERT_ENTRY_LIFE;

        String scp = serverConfig.getPropertyCached(ServerConfig.PARAM_LDAPCERT_CACHE_LIFETIME);
        if (scp != null) {
            try {
                cleanupLife = Long.parseLong(scp);
                if (cleanupLife < MIN_CERT_CACHE_LIFETIME) {
                    logger.info(MessageFormat.format("Property {0} is less than the minimum value {1} (configured value = {2}). Using the default value ({3} ms)",
                            ServerConfig.PARAM_LDAPCERT_CACHE_LIFETIME,
                            MIN_CERT_CACHE_LIFETIME,
                            cleanupLife,
                            DEFAULT_CACHED_CERT_ENTRY_LIFE));
                    cleanupLife = DEFAULT_CACHED_CERT_ENTRY_LIFE;
                }
            } catch (NumberFormatException e) {
                logger.warning(MessageFormat.format("Could not parse property {0} with value {1}. Using the default ({2} ms)",
                        ServerConfig.PARAM_LDAPCERT_CACHE_LIFETIME,
                        scp,
                        DEFAULT_CACHED_CERT_ENTRY_LIFE));
            }
        }
        cachedCertEntryLife.set(cleanupLife);
        logger.config("Certificate cache entry lifetime = " + cleanupLife);
    }

    /**
     * Task to rebulid the certificate index (if required)
     */
    private static class RebuildTask extends ManagedTimerTask {
        private final LdapIdentityProviderImpl ldapProvRef;
        private final String providerName;
        private final long oid;
        private long currentIndexInterval;

        RebuildTask( final LdapIdentityProviderImpl prov) {
            this.ldapProvRef = prov;
            this.providerName = prov.getConfig().getName();
            this.oid = prov.getConfig().getOid();
            currentIndexInterval = ldapProvRef.rebuildTimerLength.get();
        }

        @Override
        protected void doRun() {
            // When the referant Identity provider goes away, this timer should stop
            if ( ldapProvRef.isStale() ) {
                cancel();
            } else {
                long newIndexInterval = ldapProvRef.rebuildTimerLength.get();

                if (currentIndexInterval != newIndexInterval) {
                    logger.info(MessageFormat.format(
                            "Certificate index rebuild interval has changed (old value = {0}, new value = {1}). Rescheduling this task with new interval.", 
                            currentIndexInterval,
                            newIndexInterval));
                    currentIndexInterval = newIndexInterval;
                    ldapProvRef.rescheduleIndexRebuildTask();
                } else {
                    ldapProvRef.doRebuildCertIndex();
                }
            }
        }

        @Override
        public boolean cancel() {
            logger.info("Cancelling cert index task for '" + providerName + "' oid: " + oid);
            return super.cancel();
        }
    }

    private void rescheduleIndexRebuildTask() {
        cancelTasks(rebuildTask);
        rebuildTask = new RebuildTask(this);
        scheduleTask(rebuildTask, rebuildTimerLength.get());
    }

    private static class CleanupTask extends ManagedTimerTask {
        private final LdapIdentityProviderImpl ldapProv;
        private final String providerName;
        private final long oid;

        CleanupTask( final LdapIdentityProviderImpl prov ) {
            this.ldapProv = prov;
            this.providerName = prov.getConfig().getName();
            this.oid = prov.getConfig().getOid();
        }

        @Override
        protected void doRun() {
            //when the referant Identity provider goes away, this timer should stop
            if ( ldapProv.isStale() ) {
                cancel();
            } else {
                ldapProv.cleanupCertCache();
            }
        }

        @Override
        public boolean cancel() {
            logger.info("Cancelling cert cache cleanup task for '" + providerName + "' oid: " + oid);
            return super.cancel();
        }
    }

    private boolean isStale() {
        if ( configManager == null ) {
            logger.warning("Config Manager is null.");
        } else {
            try {
                IdentityProviderConfig config = configManager.findByPrimaryKey(this.config.getOid());
                return config == null || config.getVersion()!=this.config.getVersion();
            } catch (FindException e) {
                logger.warning("Error checking identity configuration for " + config);
            }
        }
        return false; // don't assume it was removed if there are errors getting to it
    }

    private void cleanupCertCache() {
        ArrayList<CertCacheKey> todelete = new ArrayList<CertCacheKey>();
        long now = System.currentTimeMillis();
        cacheLock.readLock().lock();
        try {
            Set<CertCacheKey> keys = certCache.keySet();
            for (CertCacheKey key : keys) {
                CertCacheEntry cce = certCache.get(key);
                if ((now - cce.entryCreation) > cachedCertEntryLife.get()) {
                    todelete.add(key);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        if (todelete.size() > 0) {
            cacheLock.writeLock().lock();
            try {
                for (CertCacheKey key : todelete) {
                    logger.fine("Removing certificate from cache '" + key + "'.");
                    certCache.remove(key);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
    }

    private void doRebuildCertIndex() {
        try {
            doWithLDAPContextClassLoader( new Functions.NullaryVoidThrows<NamingException>() {
                @Override
                public void call() throws NamingException {
                    rebuildCertIndex();
                }
            } );
        } catch (NamingException e) {
            logger.log(Level.WARNING, "Error while recreating ldap user certificate index for LDAP Provider '" + config.getName() + "': " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));            
        }
    }

    private void rebuildCertIndex() {
        logger.fine("Re-creating ldap user certificate index for " + config.getName());
        final CertIndex index = new CertIndex(
                new HashMap<Pair<String, String>, CertCacheKey>(),
                new HashMap<String, CertCacheKey>(),
                new HashMap<String, CertCacheKey>(),
                false);

        DirContext context = null;
        try {
            final Set<String> distinctCertAttributeNames = getDistinctCertificateAttributeNames();

            String filter = null;
            if ( getConfig().getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX ) {
                StringBuilder builder = new StringBuilder();
                boolean multiple = distinctCertAttributeNames.size() > 1;
                if (multiple) builder.append("(|");
                for ( String attr : distinctCertAttributeNames ) {
                    builder.append( '(' );
                    builder.append( attr );
                    builder.append( "=*)" );
                }
                if (multiple) builder.append(')');
                filter = builder.toString();
            } else if ( getConfig().getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX_CUSTOM ) {
                filter = getConfig().getUserCertificateIndexSearchFilter();
            }

            if ( filter != null ) {
                logger.fine( "LDAP user certificate search filter is '"+filter+"'." );

                SearchControls sc = new SearchControls();
                sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
                sc.setReturningAttributes(distinctCertAttributeNames.toArray(new String[distinctCertAttributeNames.size()]));

                NamingEnumeration<SearchResult> answer = null;
                try {
                    context = getBrowseContext();
                    answer = context.search(config.getSearchBase(), filter, sc);

                    while (answer.hasMore()) {
                        final SearchResult sr = answer.next();
                        final Attributes attributes = sr.getAttributes();
                        final String dn = sr.getNameInNamespace();

                        for ( String certAttributeName : distinctCertAttributeNames ) {
                            final Object certificateObj = LdapUtils.extractOneAttributeValue(attributes, certAttributeName);
                            if (certificateObj instanceof byte[]) {
                                try {
                                    final X509Certificate cert = CertUtils.decodeCert((byte[])certificateObj);
                                    index.addCertificateToIndexes( dn, cert );
                                } catch ( CertificateException e ) {
                                    logger.log( Level.WARNING, "Could not process certificate for '"+dn+"': " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                                }
                            }
                        }
                    }
                } finally {
                    ResourceUtils.closeQuietly( answer );
                }

                certIndexRef.set( index.immutable() );

                logger.fine("LDAP user certificate index rebuilt '"+index.describe()+"'.");
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while recreating ldap user certificate index for LDAP Provider '" + config.getName() + "': " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } finally {
            ResourceUtils.closeQuietly( context );
        }
    }

    private Set<String> getDistinctCertificateAttributeNames() {
        final Set<String> distinctCertAttributeNames = new HashSet<String>();
        final UserMappingConfig[] mappings = config.getUserMappings();
        for (UserMappingConfig mapping : mappings) {
            String certAttributeName = mapping.getUserCertAttrName();
            if (certAttributeName == null || certAttributeName.trim().length() < 1) {
                logger.fine("User mapping for class " + mapping.getObjClass() +
                            " contained empty cert attribute name. This mapping will be " +
                            "ignored as part of the indexing process.");
            } else {
                distinctCertAttributeNames.add( certAttributeName.trim() );
            }
        }
        return distinctCertAttributeNames;
    }

    private boolean certsAreEnabled() {
        return getConfig().getUserCertificateUseType() != LdapIdentityProviderConfig.UserCertificateUseType.NONE;
    }

    private boolean certIndexEnabled() {
        return getConfig().getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX ||
               getConfig().getUserCertificateUseType() == LdapIdentityProviderConfig.UserCertificateUseType.INDEX_CUSTOM;
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setCertificateAuthenticator(CertificateAuthenticator certificateAuthenticator) {
        this.certificateAuthenticator = certificateAuthenticator;
    }

    private void initializeFallbackMechanism() {
        // configure timeout period
        String property = serverConfig.getPropertyCached("ldap.reconnect.timeout");
        if (property == null || property.length() < 1) {
            retryFailedConnectionTimeout = 60000;
            logger.warning("ldap.reconnect.timeout server property not set. using default");
        } else {
            try {
                retryFailedConnectionTimeout = Long.parseLong(property);
            } catch (NumberFormatException e) {
                logger.log(Level.WARNING, "ldap.reconnect.timeout property not configured properly. using default", e);
                retryFailedConnectionTimeout = 60000;
            }
        }
        // build a table of ldap urls status
        ldapUrls = config.getLdapUrl();
        urlStatus = new Long[ldapUrls.length];
        lastSuccessfulLdapUrl = ldapUrls[0];
    }

    @Override
    public LdapIdentityProviderConfig getConfig() {
        return config;
    }

    @Override
    public LdapUserManager getUserManager() {
        return userManager;
    }

    @Override
    public LdapGroupManager getGroupManager() {
        return groupManager;
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
     * serverconfig.properties under ldap.reconnect.timeout in ms)
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

    @Override
    public AuthenticationResult authenticate(final LoginCredentials pc) throws AuthenticationException {
        LdapUser realUser;
        try {
            realUser = findUserByCredential( pc );
        } catch (FindException e) {
            if (pc.getFormat() == CredentialFormat.KERBEROSTICKET) {
                KerberosServiceTicket ticket = (KerberosServiceTicket) pc.getPayload();
                throw new AuthenticationException("Couldn't find LDAP user for kerberos principal '" + ticket.getClientPrincipalName() + "'.", e);
            } else {
                throw new AuthenticationException("Couldn't authenticate credentials", e);
            }
        }

        if (realUser == null) return null;

        final CredentialFormat format = pc.getFormat();
        if (format == CredentialFormat.CLEARTEXT) {
            return authenticatePasswordCredentials(pc, realUser);
        } else if (format == CredentialFormat.DIGEST) {
            return DigestAuthenticator.authenticateDigestCredentials(pc, realUser);
        } else if (format  == CredentialFormat.KERBEROSTICKET) {
            return new AuthenticationResult( realUser, pc.getSecurityTokens() );
        } else {
            if (format == CredentialFormat.CLIENTCERT || format == CredentialFormat.SAML) {

                    //get the LDAP cert for this user if LDAP certs are enabled for this provider
                    if (certsAreEnabled() && realUser.getLdapCertBytes() != null) {
                        try {
                            return certificateAuthenticator.authenticateX509Credentials(pc,
                                                                             realUser.getCertificate(),
                                                                             realUser,
                                                                             config.getCertificateValidationType(),
                                                                             auditor, false);
                        } catch (CertificateException e) {
                            throw new AuthenticationException("Problem decoding cert located in LDAP: " + ExceptionUtils.getMessage(e), e);
                        }
                    } else {
                        return certificateAuthenticator.authenticateX509Credentials(pc, realUser, config.getCertificateValidationType(), auditor);
                    }
            } else {
                String msg = "Attempt to authenticate using unsupported method on this provider: " + pc.getFormat();
                logger.log(Level.SEVERE, msg);
                throw new AuthenticationException(msg);
            }
        }
    }

    /**
     * This provider does not support lookup of users by credential.
     */
    @Override
    public LdapUser findUserByCredential( LoginCredentials pc ) throws FindException {
        LdapUser user = null;

        if (pc.getFormat() == CredentialFormat.KERBEROSTICKET) {
            KerberosServiceTicket ticket = (KerberosServiceTicket) pc.getPayload();

            Collection<IdentityHeader> headers;
            headers = search(ticket);
            if (headers.size() > 1) {
                throw new FindException("Found multiple LDAP users for kerberos principal '" + ticket.getClientPrincipalName() + "'.");
            }
            else if (!headers.isEmpty()){
                for (IdentityHeader header : headers) {
                    if (header.getType() == EntityType.USER) {
                        user = userManager.findByPrimaryKey(header.getStrId());
                    }
                }
            }
        } else {
            user = userManager.findByLogin(pc.getLogin());
        }

        return user;
    }

    private AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc, LdapUser realUser) throws BadCredentialsException {
        // basic authentication
        boolean res = userManager.authenticateBasic(realUser.getDn(), new String(pc.getCredentials()));
        if (res) {
            // success
            return new AuthenticationResult(realUser, pc.getSecurityTokens());
        }
        logger.info("credentials did not authenticate for " + pc.getLogin());
        throw new BadCredentialsException("credentials did not authenticate");
    }

    @Override
    public long getMaxSearchResultSize() {
        return maxSearchResultSize.get();
    }
    
    @Override
    public Collection<String> getReturningAttributes() {
        Collection<String> attributes = null;

        if ( returningAttributes != null ) {
            attributes = Collections.unmodifiableCollection( Arrays.asList( returningAttributes ) );           
        }

        return attributes;
    }

    /**
     * searches the ldap provider for identities
     *
     * @param types        any combination of EntityType.USER and or EntityType.GROUP
     * @param searchString the search string for the users and group names, use "*" for all
     * @return a collection containing EntityHeader objects
     */
    @Override
    public EntityHeaderSet<IdentityHeader> search(EntityType[] types, String searchString) throws FindException {
        if (types == null || types.length < 1) {
            throw new IllegalArgumentException("must pass at least one type");
        }

        boolean wantUsers = false;
        boolean wantGroups = false;

        for (EntityType type : types) {
            if (type == EntityType.USER)
                wantUsers = true;
            else if (type == EntityType.GROUP) wantGroups = true;
        }
        if (!wantUsers && !wantGroups) {
            throw new IllegalArgumentException("types must contain users and or groups");
        }

        String userFilter = userSearchFilterWithParam(searchString);
        String grpFilter = groupSearchFilterWithParam(searchString);

        String filter;
        if (wantUsers && wantGroups) {
            // no group mapping is now allowed
            if (grpFilter == null) {
                filter = userFilter;
            } else
                filter = "(|" + userFilter + grpFilter + ")";
        } else if (wantUsers) {
            filter = userSearchFilterWithParam(searchString);
        } else
            filter = groupSearchFilterWithParam(searchString);

        // no group mapping is now allowed
        if (filter == null) return EntityHeaderSet.empty();

        return doSearch(filter);
    }

    /**
     * Find the LDAP users that correspond to the given ticket
     */
    private Collection<IdentityHeader> search( final KerberosServiceTicket ticket ) throws FindException {
        String principal = ticket.getClientPrincipalName();

        int index1 = principal.indexOf( '@' );
        int index2 = principal.lastIndexOf( '@' );
        boolean isEnterprise = index1 != index2;
        String value = principal;
        if ( index2 != -1 ) {
            value = principal.substring( 0, index2 );
        }

        if ( value.length() == 0 ) {
            throw new FindException( "Error processing kerberos principal name '"+principal+"', cannot determine REALM." );
        }

        if ( logger.isLoggable( Level.FINEST ) ) {
            logger.log( Level.FINEST, "Performing LDAP search by kerberos principal ''{1}'' (enterprise:{0})", new Object[]{isEnterprise, principal} );
        }

        ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();
        for (int i = 0; i < config.getUserMappings().length; i++) {
            UserMappingConfig userMappingConfig = config.getUserMappings()[i];

            String mappingName = null;
            if ( isEnterprise ) {
                mappingName = userMappingConfig.getKerberosEnterpriseAttrName();
            }

            if ( mappingName == null ) {
                mappingName = userMappingConfig.getKerberosAttrName();

                if ( mappingName == null ) {
                    mappingName = userMappingConfig.getLoginAttrName();
                }
            }

            terms.add( new LdapSearchTerm( userMappingConfig.getObjClass(), mappingName, value ) );
        }

        String filter = makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]));

        return doSearch(filter);
    }

    @Override
    public X509Certificate findCertBySki( final String ski ) {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            // look for presence of cert in index
            CertIndex index = certIndexRef.get();
            CertCacheKey certCacheKey = index.getCertCacheKeyBySki( ski );

            if ( certCacheKey != null ) {
                lookedupCert = getCertificateByKey(certCacheKey);
            }

            if ( lookedupCert == null ) {
                if ( !certIndexEnabled() ) { // then search for cert and add to cache if found
                    lookedupCert = getCertificateBySki( ski );
                } else if ( certCacheKey==null ) {
                    logger.fine("The certificate with SKI '" + ski + "' is not indexed.");
                }
            }
        }

        return lookedupCert;
    }

    @Override
    public X509Certificate findCertByIssuerAndSerial( final X500Principal issuerDN, final BigInteger certSerial ) {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            // look for presence of cert in index
            CertIndex index = certIndexRef.get();
            CertCacheKey certCacheKey = index.getCertCacheKeyByIssuerAndSerial( issuerDN, certSerial );

            if ( certCacheKey != null ) {
                lookedupCert = getCertificateByKey(certCacheKey);
            }

            if ( lookedupCert == null ) {
                if ( !certIndexEnabled() ) { // then search for cert and add to cache if found
                    lookedupCert = getCertificateByIssuerAndSerial( issuerDN, certSerial );
                } else if ( certCacheKey==null ) {
                    logger.fine("The certificate with Issuer DN '" + issuerDN + "' and  Serial Number '" + certSerial + "' is not indexed.");
                }
            }
        }
        
        return lookedupCert;
    }

    @Override
    public X509Certificate findCertByThumbprintSHA1( final String thumbprintSHA1 ) throws FindException {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            // look for presence of cert in index
            CertIndex index = certIndexRef.get();
            CertCacheKey certCacheKey = index.getCertCacheKeyByThumbprintSHA1( thumbprintSHA1 );

            if ( certCacheKey != null ) {
                lookedupCert = getCertificateByKey(certCacheKey);
            } else {
                logger.fine("The certificate with Thumbprint SHA1 '" + thumbprintSHA1 + "' is not indexed.");
            }
        }

        return lookedupCert;
    }

    private X509Certificate getCertificateBySki( final String ski ) {
        X509Certificate certificate = null;

        try {
            String filter = config.getUserCertificateSKISearchFilter();
            if ( filter != null && !filter.isEmpty() ) {
                certificate = getCertificateWithFilter( filter, null, null, ski );
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Error looking up certificate by SKI in directory.", fe);
        }

        return certificate;
    }

    private X509Certificate getCertificateByIssuerAndSerial( final X500Principal issuerDN, final BigInteger serial ) {
        X509Certificate certificate = null;

        try {
            String filter = config.getUserCertificateIssuerSerialSearchFilter();
            if ( filter != null && !filter.isEmpty() ) {
                certificate = getCertificateWithFilter( filter, issuerDN, serial, null );
            }
        } catch (FindException fe) {
            logger.log(Level.WARNING, "Error looking up certificate by issuer/serial in directory.", fe);
        }

        return certificate;
    }

    /**
     * Get certificate from LDAP using the given filter and add to indexes and cache.
     */
    private X509Certificate getCertificateWithFilter( final String filterTemplate,
                                                      final X500Principal issuer,
                                                      final BigInteger serial,
                                                      final String ski ) throws FindException {
        X509Certificate certificate = null;

        final String filter = formatCertificateSearchFilter( filterTemplate, issuer, serial, ski );

        DirContext context = null;
        NamingEnumeration<SearchResult> answer = null;
        try {
            final Set<String> distinctCertAttributeNames = getDistinctCertificateAttributeNames();

            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(1);
            sc.setReturningAttributes(distinctCertAttributeNames.toArray(new String[distinctCertAttributeNames.size()]));

            context = getBrowseContext();
            answer = context.search(config.getSearchBase(), filter, sc);
            if ( answer.hasMore() ) {
                final SearchResult sr = answer.next();
                final Attributes attributes = sr.getAttributes();
                final String dn = sr.getNameInNamespace();

                List<X509Certificate> certs = new ArrayList<X509Certificate>();
                for ( String certAttributeName : distinctCertAttributeNames ) {
                    final Object certificateObj = LdapUtils.extractOneAttributeValue(attributes, certAttributeName);
                    if (certificateObj instanceof byte[]) {
                        X509Certificate cert = CertUtils.decodeCert((byte[])certificateObj);

                        // Stash for indexing
                        certs.add( cert );

                        // Check if this the target certificate
                        if ( issuer != null && serial != null ) {
                            if ( issuer.equals(cert.getIssuerX500Principal()) && serial.equals(cert.getSerialNumber())) {
                                certificate = cert;
                            }
                        } else if ( ski != null ) {
                            if ( ski.equals( CertUtils.getSki(cert))) {
                                certificate = cert;
                            }
                        }
                    }
                }
                X509Certificate[] certificates = certs.toArray( new X509Certificate[certs.size()] );
                cacheAndIndexCertificates( dn, certificates );
            }
        } catch (NamingException e) {
            throw new FindException("LDAP search error with filter " + filter, e);
        } catch (CertificateException e) {
            throw new FindException("LDAP search error with filter " + filter, e);
        } finally {
            ResourceUtils.closeQuietly( answer );
            ResourceUtils.closeQuietly( context );
        }

        if ( certificate == null ) {
            clearIndex( issuer, serial, ski );
        }

        return certificate;
    }

    private void cacheAndIndexCertificates( final String dn, final X509Certificate[] certificates ) throws CertificateException {
        final CertIndex index = certIndexRef.get();

        final Map<CertCacheKey, CertCacheEntry> newCertCacheEntries = new HashMap<CertCacheKey, CertCacheEntry>();
        for ( X509Certificate cert : certificates ) {
            CertCacheKey certCacheKey = index.addCertificateToIndexes( dn, cert );
            if ( certCacheKey != null ) {
                newCertCacheEntries.put( certCacheKey, new CertCacheEntry(cert) );
            }
        }

        for ( CertCacheKey certCacheKey : newCertCacheEntries.keySet() ) {
            logger.fine("Caching cert for " + certCacheKey);
        }

        cacheLock.writeLock().lock();
        try {
            certCache.putAll(newCertCacheEntries);
        } finally {
            cacheLock.writeLock().unlock();
        }
    }

    private void clearIndex( final X500Principal issuer, final BigInteger serial, final String ski ) {
        final CertIndex index = certIndexRef.get();
        index.removeIndex( issuer, serial, ski );
    }

    private X509Certificate getCertificateByKey( final CertCacheKey certCacheKey ) {
        X509Certificate output = null;

        // try to find cert in cert cache
        cacheLock.readLock().lock();
        try {
            CertCacheEntry cce = certCache.get(certCacheKey);
            if (cce != null) output = cce.cert;
        } finally {
            cacheLock.readLock().unlock();
        }

        if (output != null) {
            logger.fine("Cert found in cache '"+certCacheKey+"'.");
        } else {
            // load the cert from ldap
            DirContext context = null;
            try {
                UserMappingConfig[] mappings = config.getUserMappings();

                context = getBrowseContext();
                Attributes attributes = context.getAttributes(certCacheKey.getKey());
                for ( UserMappingConfig mapping : mappings ) {
                    String userCertAttrName = mapping.getUserCertAttrName();
                    if (userCertAttrName == null || userCertAttrName.trim().isEmpty()) {
                        logger.fine("No user certificate attribute has been configured for user mapping " + mapping.getObjClass());
                    } else {
                        Object certificateObj = LdapUtils.extractOneAttributeValue(attributes, userCertAttrName.trim());
                        if (certificateObj instanceof byte[]) {
                            logger.fine("Found a certificate in directory for " + certCacheKey.getKey());
                            X509Certificate certificate = CertUtils.decodeCert((byte[])certificateObj);
                            if ( certCacheKey.getValue().equals(CertUtils.getThumbprintSHA1(certificate)) ) {
                                output = certificate;
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Error looking up user in directory " + certCacheKey, e);
            } finally {
                ResourceUtils.closeQuietly( context );
            }

            if (output == null) {
                logger.fine("Certificate is in the index but not in directory (" + certCacheKey + ")");
                return null;
            }

            // add the cert to the cert cache and return
            logger.fine("Caching cert for " + certCacheKey);
            cacheLock.writeLock().lock();
            try {
                certCache.put(certCacheKey, new CertCacheEntry(output));
            } finally {
                cacheLock.writeLock().unlock();
            }
        }

        return output;
    }

    private EntityHeaderSet<IdentityHeader> doSearch(String filter) throws FindException {
        EntityHeaderSet<IdentityHeader> output = new EntityHeaderSet<IdentityHeader>(new TreeSet<IdentityHeader>());
        DirContext context = null;
        NamingEnumeration answer = null;
        try {
            // search string for users and or groups based on passed types wanted
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setCountLimit(getMaxSearchResultSize());
            sc.setReturningAttributes(returningAttributes);
            context = getBrowseContext();
            answer = context.search(config.getSearchBase(), filter, sc);
            while (answer.hasMore()) {
                // get this item
                SearchResult sr = (SearchResult)answer.next();
                IdentityHeader header = searchResultToHeader(sr);
                // if we successfully constructed a header, add it to result list
                if (header != null)
                    output.add(header);
                else
                    logger.info("entry not valid or objectclass not supported for dn=" + sr.getNameInNamespace() + ". this " +
                      "entry will not be presented as part of the search results.");
            }
        } catch (SizeLimitExceededException e) {
            // add something to the result that indicates the fact that the search criteria is too wide
            logger.log(Level.FINE, "the search results exceede the maximum: '" + e.getMessage() + "'");
            output.setMaxExceeded(getMaxSearchResultSize());
            // dont throw here, we still want to return what we got
        } catch (javax.naming.AuthenticationException ae) {
            throw new FindException("LDAP search error: Authentication failed.", ae);
        } catch (PartialResultException pre) {
            logger.log(Level.WARNING, "LDAP search error, partial result.", pre);
            // don't throw, return the partial result
        } catch (NamingException e) {
            throw new FindException("LDAP search error with filter " + filter, e);
        } finally {
            ResourceUtils.closeQuietly( answer );
            ResourceUtils.closeQuietly( context );
        }
        return output;
    }

    @Override
    public EntityHeaderSet<IdentityHeader> search(boolean getusers, boolean getgroups, IdentityMapping mapping, Object attValue) throws FindException {
        if (mapping instanceof LdapAttributeMapping) {
            LdapAttributeMapping lam = (LdapAttributeMapping) mapping;
            String attName = lam.getCustomAttributeName();

            if (!(getusers || getgroups)) {
                logger.info("Nothing to search for - specified EntityType not supported by this IdentityMapping");
                return EntityHeaderSet.empty();
            }

            String userFilter = null;
            String groupFilter = null;
            if (getusers) {
                ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();
                for (int i = 0; i < config.getUserMappings().length; i++) {
                    UserMappingConfig userMappingConfig = config.getUserMappings()[i];
                    terms.add(new LdapSearchTerm(userMappingConfig.getObjClass(), attName, attValue.toString()));
                }
                userFilter = makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]));
            }

            if (getusers) {
                ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();
                for (int i = 0; i < config.getGroupMappings().length; i++) {
                    GroupMappingConfig groupMappingConfig = config.getGroupMappings()[i];
                    terms.add(new LdapSearchTerm(groupMappingConfig.getObjClass(), attName, attValue.toString()));
                }
                groupFilter = makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]));
            }

            String filter;
            if (getusers && getgroups) {
                filter = "(|" + userFilter + groupFilter + ")";
            } else if (getusers) {
                filter = userFilter;
            } else {
                filter = groupFilter;
            }

            return doSearch(filter);
        } else {
            throw new FindException("Unsupported AttributeMapping type");
        }
    }

    @Override
    public String getAuthRealm() {
        return HttpDigest.REALM;
    }

    /**
     * builds a search filter for all user object classes based on the config object
     */
    @Override
    public String userSearchFilterWithParam(String param) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");

        UserMappingConfig[] userTypes = config.getUserMappings();
        ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();

        // Find all known classes of user, by both name and login
        for (UserMappingConfig userType : userTypes) {
            terms.add(new LdapSearchTerm(userType.getObjClass(), userType.getLoginAttrName(), param));
            terms.add(new LdapSearchTerm(userType.getObjClass(), userType.getNameAttrName(), param));
        }
        return makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]));
    }

    private String makeSearchFilter(LdapSearchTerm[] terms) {
        StringBuilder output = new StringBuilder();
        if (terms.length > 1) output.append("(|");

        for (LdapSearchTerm term : terms) {
            output.append("(&");
            output.append("(objectClass").append("=").append(term.objectclass).append(")");
            output.append("(").append(term.searchAttribute).append("=").append(term.searchValue).append(")");
            output.append(")");
        }

        if (terms.length > 1) output.append(")");
        return output.toString();
    }

    /**
     * builds a search filter for all group object classes based on the config object
     *
     * @return the search filter or null if no group mappings are declared for this config
     */
    @Override
    public String groupSearchFilterWithParam(String param) {
        if (config == null) throw new IllegalStateException("this provider needs a config!");
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        if (groupTypes == null || groupTypes.length <= 0) return null;

        ArrayList<LdapSearchTerm> terms = new ArrayList<LdapSearchTerm>();
        for (GroupMappingConfig groupType : groupTypes) {
            terms.add(new LdapSearchTerm(groupType.getObjClass(), groupType.getNameAttrName(), param));
        }

        return makeSearchFilter(terms.toArray(new LdapSearchTerm[terms.size()]));
    }

    @Override
    public DirContext getBrowseContext() throws NamingException {
        String ldapurl = getLastWorkingLdapUrl();
        if (ldapurl == null) {
            ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
        }
        while (ldapurl != null) {
            Hashtable<? super String, ? super String> env = LdapUtils.newEnvironment();
            env.put("java.naming.ldap.version", "3");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldapurl);
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.timeout", Long.toString(getLdapConnectionTimeout()));
            env.put("com.sun.jndi.ldap.read.timeout", Long.toString(getLdapReadTimeout()));
            env.put( Context.REFERRAL, "follow" );
            String dn = config.getBindDN();
            if (dn != null && dn.length() > 0) {
                String pass = config.getBindPasswd();
                env.put(Context.SECURITY_AUTHENTICATION, "simple");
                env.put(Context.SECURITY_PRINCIPAL, dn);
                env.put(Context.SECURITY_CREDENTIALS, pass);
            }

            try {
                LdapURL url = new LdapURL(ldapurl);
                if (url.useSsl()) {
                    env.put("java.naming.ldap.factory.socket", LdapSslCustomizerSupport.getSSLSocketFactoryClassname( config.isClientAuthEnabled(), config.getKeystoreId(), config.getKeyAlias() ) );
                    env.put(Context.SECURITY_PROTOCOL, "ssl");
                }
            } catch (NamingException e) {
                logger.log(Level.WARNING, "Malformed LDAP URL " + ldapurl + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
                continue;
            } catch (IllegalArgumentException e) {
                logger.log(Level.WARNING, "Malformed LDAP URL " + ldapurl + ": " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
                continue;
            }

            LdapUtils.lock( env );

            try {
                // Create the initial directory context.
                return new InitialDirContext(env);
            } catch (CommunicationException e) {
                logger.log(Level.WARNING, "Could not establish context using LDAP URL " + ldapurl + ". " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            } catch (RuntimeException e) {
                logger.log(Level.WARNING, "Could not establish context using LDAP URL " + ldapurl + ". " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
                ldapurl = markCurrentUrlFailureAndGetFirstAvailableOne(ldapurl);
            }
        }
        throw new CommunicationException("Could not establish context on any of the ldap urls.");
    }

    @Override
    public void test(final boolean quick) throws InvalidIdProviderCfgException {
        DirContext context = null;
        try {
            // make sure we can connect
            try {
                context = getBrowseContext();
            } catch (NamingException e) {
                String msg;
                if (e instanceof javax.naming.AuthenticationException) {
                    msg = "Cannot connect to this directory, authentication failed.";
                    logger.log(Level.INFO, "LDAP configuration test failure. " + msg, ExceptionUtils.getDebugException(e));
                } else {
                    msg = "Cannot connect to this directory '"+ExceptionUtils.getMessage(e)+"'.";
                    logger.log(Level.INFO, "LDAP configuration test failure. " + msg, ExceptionUtils.getDebugException(e));
                }

                // note. i am not embedding the NamingException because it sometimes
                // contains com.sun.jndi.ldap.LdapCtx which does not implement serializable
                throw new InvalidIdProviderCfgException(msg);
            }

            // That's all for a quick test
            if ( quick ) return;

            String filter;
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);
            sc.setReturningAttributes(returningAttributes);            

            // make sure the base DN is valid and contains at least one entry
            NamingEnumeration entrySearchEnumeration = null;
            try {
                sc.setCountLimit( 1 );
                entrySearchEnumeration = context.search(config.getSearchBase(), "(objectClass=*)", sc);
            } catch (NamingException e) {
                // note. i am not embedding the NamingException because it sometimes
                // contains com.sun.jndi.ldap.LdapCtx which does not implement serializable
                throw new InvalidIdProviderCfgException("Cannot search using base: " + config.getSearchBase());
            } finally {
                sc.setCountLimit( 0 );
                ResourceUtils.closeQuietly( entrySearchEnumeration );
            }

            // check user mappings. make sure they work
            boolean atLeastOneUser = false;
            UserMappingConfig[] userTypes = config.getUserMappings();
            Collection<UserMappingConfig> offensiveUserMappings = new ArrayList<UserMappingConfig>();
            Collection<UserMappingConfig> userMappingsWithoutLoginAttribute = new ArrayList<UserMappingConfig>();
            for (UserMappingConfig userType : userTypes) {
                if (userType.getLoginAttrName() == null || userType.getLoginAttrName().length() < 1) {
                    userMappingsWithoutLoginAttribute.add(userType);
                    continue;
                }
                filter = "(|" +
                        "(&" +
                        "(objectClass=" + userType.getObjClass() + ")" +
                        "(" + userType.getLoginAttrName() + "=*)" +
                        ")" +
                        "(&" +
                        "(objectClass=" + userType.getObjClass() + ")" +
                        "(" + userType.getNameAttrName() + "=*)" +
                        ")" +
                        ")";

                NamingEnumeration answer = null;
                try {
                    answer = context.search(config.getSearchBase(), filter, sc);
                    while (answer.hasMore()) {
                        SearchResult sr = (SearchResult) answer.next();
                        EntityHeader header = searchResultToHeader(sr);
                        // if we successfully constructed a header, add it to result list
                        if (header != null) {
                            atLeastOneUser = true;
                            break;
                        }
                    }
                } catch (NamingException e) {
                    offensiveUserMappings.add(userType);
                    logger.log(Level.FINE, "error testing user mapping" + userType.getObjClass(), e);
                } finally {
                    ResourceUtils.closeQuietly(answer);
                }
            }

            // check group mappings. make sure they work
            GroupMappingConfig[] groupTypes = config.getGroupMappings();
            Collection<GroupMappingConfig> offensiveGroupMappings = new ArrayList<GroupMappingConfig>();
            boolean atLeastOneGroup = false;
            for (GroupMappingConfig groupType : groupTypes) {
                filter = "(&" +
                        "(objectClass=" + groupType.getObjClass() + ")" +
                        "(" + groupType.getNameAttrName() + "=*)" +
                        ")";
                NamingEnumeration answer = null;
                try {
                    answer = context.search(config.getSearchBase(), filter, sc);
                    while (answer.hasMore()) {
                        SearchResult sr = (SearchResult) answer.next();
                        EntityHeader header = searchResultToHeader(sr);
                        // if we successfully constructed a header, add it to result list
                        if (header != null) {
                            atLeastOneGroup = true;
                            break;
                        }
                    }
                } catch (NamingException e) {
                    offensiveGroupMappings.add(groupType);
                    logger.log(Level.FINE, "error testing group mapping" + groupType.getObjClass(), e);
                } finally {
                    ResourceUtils.closeQuietly(answer);
                }
            }

            // test search filters if any
            sc.setCountLimit( 100 );
            boolean atLeastOneCertIndexed = true;
            boolean validSearchByIssuerSerial = true;
            boolean validSearchBySKI = true;
            switch ( config.getUserCertificateUseType() ) {
                case INDEX_CUSTOM:
                    {
                        NamingEnumeration answer = null;
                        atLeastOneCertIndexed = false;
                        try {
                            answer = context.search(config.getSearchBase(), config.getUserCertificateIndexSearchFilter(), sc);
                            out:
                            while ( answer.hasMore() ) {
                                final SearchResult sr = (SearchResult) answer.next();
                                final Attributes atts = sr.getAttributes();

                                for ( UserMappingConfig userType : userTypes ) {
                                    String attrName = userType.getUserCertAttrName();
                                    if ( attrName==null || attrName.trim().isEmpty() ) continue;
                                    if ( atts.get( attrName ) != null ) {
                                        atLeastOneCertIndexed = true;
                                        break out;
                                    }
                                }
                            }
                        } catch (Exception e) { // Search with "(" for AIOOBE, ")" for IllegalStateException
                            logger.log(Level.FINE, "Error testing certificate index search filter", e);
                        } finally {
                            ResourceUtils.closeQuietly(answer);
                        }
                    }
                    break;
                case SEARCH:
                    String issuerSerialSearchFilter = config.getUserCertificateIssuerSerialSearchFilter();
                    if ( issuerSerialSearchFilter!=null && !issuerSerialSearchFilter.isEmpty() ) {
                        NamingEnumeration answer = null;
                        validSearchByIssuerSerial = false;
                        try {
                            String searchFilter = formatCertificateSearchFilter(issuerSerialSearchFilter);
                            answer = context.search(config.getSearchBase(), searchFilter, sc);
                            validSearchByIssuerSerial = true;
                        } catch (Exception e) { // Search with "(" for AIOOBE, ")" for IllegalStateException
                            logger.log(Level.FINE, "Error testing certificate index search filter", e);
                        } finally {
                            ResourceUtils.closeQuietly(answer);
                        }
                    }
                    String skiSearchFilter = config.getUserCertificateSKISearchFilter();
                    if ( skiSearchFilter!=null && !skiSearchFilter.isEmpty() ) {
                        NamingEnumeration answer = null;
                        validSearchBySKI = false;
                        try {
                            String searchFilter = formatCertificateSearchFilter(skiSearchFilter);
                            answer = context.search(config.getSearchBase(), searchFilter, sc);
                            validSearchBySKI = true;
                        } catch (NamingException e) {
                            logger.log(Level.FINE, "Error testing certificate index search filter", e);
                        } finally {
                            ResourceUtils.closeQuietly(answer);
                        }
                    }
                    break;
                default:
            }

            // merge all errors in a special report
            StringBuilder error = new StringBuilder();

            if (userMappingsWithoutLoginAttribute.size() > 0) {
                if (error.length() > 0) error.append('\n');
                error.append("The following user mapping(s) do not define login attribute.");
                for (UserMappingConfig userMappingConfig : userMappingsWithoutLoginAttribute) {
                    error.append(" ").append(userMappingConfig.getObjClass());
                }
            }

            if (offensiveUserMappings.size() > 0 || offensiveGroupMappings.size() > 0) {
                if (error.length() > 0) error.append('\n');
                error.append("The following mappings caused errors:");
                for (UserMappingConfig offensiveUserMapping : offensiveUserMappings) {
                    error.append(" User mapping ").append(offensiveUserMapping.getObjClass());
                }
                for (GroupMappingConfig offensiveGroupMapping : offensiveGroupMappings) {
                    error.append(" Group mapping ").append(offensiveGroupMapping.getObjClass());
                }
            }

            if (!atLeastOneUser) {
                if (error.length() > 0) error.append('\n');
                error.append("This configuration did not yield any users");
            }

            if (!atLeastOneGroup && groupTypes.length > 0) {
                if (error.length() > 0) error.append('\n');
                error.append("This configuration did not yield any group");
            }

            if ( !atLeastOneCertIndexed ) {
                if (error.length() > 0) error.append('\n');
                error.append("The search filter for certificate indexing did not yield any user certificates");
            }

            if ( !validSearchByIssuerSerial ) {
                if (error.length() > 0) error.append('\n');
                error.append("The search filter for user certificates by issuer name and serial number is invalid.");
            }

            if ( !validSearchBySKI ) {
                if (error.length() > 0) error.append('\n');
                error.append("The search filter for user certificates by subject key identifier is invalid.");
            }

            if (error.length() > 0) {
                logger.fine("Test produced following error(s): " + error.toString());
                throw new InvalidIdProviderCfgException(error.toString());
            } else
                logger.finest("this ldap config was tested successfully");
        } finally {
            ResourceUtils.closeQuietly(context);
        }
    }

    @Override
    public void preSaveClientCert(LdapUser user, X509Certificate[] certChain) throws ClientCertManager.VetoSave {
        // ClientCertManagerImp's default rules are OK
    }

    @Override
    public void setUserManager(LdapUserManager userManager) {
        this.userManager = userManager;
    }

    @Override
    public void setGroupManager(LdapGroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public boolean hasClientCert(LoginCredentials lc) throws AuthenticationException {
        try {
            LdapUser ldapUser = userManager.findByLogin(lc.getLogin());
            if (ldapUser != null) {
                //check where we should be looking for the cert, the cert could reside either in LDAP or gateway
                if (certsAreEnabled()) {
                    //telling use to search cert through ldap
                    return ldapUser.getLdapCertBytes() != null;
                } else {
                    //telling use to search the gateway
                    return clientCertManager.getUserCert(ldapUser) != null;
                }
            } else {
                //we cant even find the user, so there won't be a cert available
                return false;
            }
        } catch (FindException fe) {
            throw new AuthenticationException(String.format("Couldn't find user '%s'", lc.getLogin()), fe);
        }
    }

    public void setClientCertManager(final ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setConfigManager(final IdentityProviderConfigManager configManager) {
        this.configManager = configManager;
    }

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        if (clientCertManager == null) throw new IllegalStateException("The Client Certificate Manager is required");
        if (auditor == null) throw new IllegalStateException("Auditor has not been initialized");
        if (userManager == null) throw new IllegalStateException("UserManager has not been initialized");
        if (groupManager == null) throw new IllegalStateException("GroupManager has not been initialized");
        if (serverConfig == null) throw new IllegalStateException("ServerConfig has not been initialized");
    }

    @Override
    public void destroy() throws Exception {
        cancelTasks(rebuildTask, cleanupTask);
    }

    private void scheduleTask( final ManagedTimerTask task, final long period ) {
        if ( task != null ) {
            timer.schedule(task, 5000, period);
        }
    }

    private void cancelTasks(ManagedTimerTask ... tasks) {
        for (ManagedTimerTask task : tasks) {
            if ( task != null ) {
                task.cancel();
            }
        }
    }

    /**
     * Invoke the given callback with the context classloader that loads SSL socket factories. 
     */
    private void doWithLDAPContextClassLoader( final Functions.NullaryVoidThrows<NamingException> callback ) throws NamingException {
        final ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader( LdapSslCustomizerSupport.getSSLSocketFactoryClassLoader() );
            callback.call();
        } finally {
            Thread.currentThread().setContextClassLoader( originalContextClassLoader );
        }
    }

    /**
     * In MSAD, there is an attribute named userAccountControl which contains information
     * regarding the validity of the account pointed to by the ldap entry. This method will return false
     * IF the attribute is present AND IF one of those attributes is set:
     * 0x00000002 	The user account is disabled.
     * 0x00000010 	The account is currently locked out.
     * 0x00800000 	The user password has expired.
     * <p/>
     * Otherwise, will return true.
     * <p/>
     * See bugzilla #1116, #1466 for the justification of this check.
     */
    @Override
    public boolean isValidEntryBasedOnUserAccountControlAttribute(String userDn, Attributes attibutes) {
        final long DISABLED_FLAG = 0x00000002;
        final long LOCKED_FLAG = 0x00000010;
        final long EXPIRED_FLAG = 0x00800000; // add a check for this flag in an attempt to fix 1466
        Attribute userAccountControlAttr = attibutes.get( LdapUtils.LDAP_ATTR_USER_ACCOUNT_CONTROL );
        if (userAccountControlAttr != null && userAccountControlAttr.size() > 0) {
            Object found = null;
            try {
                found = userAccountControlAttr.get(0);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Problem accessing the userAccountControl attribute");
            }
            Long value = null;
            if (found instanceof String) {
                value = new Long((String)found);
            } else if (found instanceof Long) {
                value = (Long)found;
            } else if (found != null) {
                logger.severe("FOUND userAccountControl attribute but " +
                  "is of unexpected type: " + found.getClass().getName());
            }
            if (value != null) {
                if ((value & DISABLED_FLAG) == DISABLED_FLAG) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_DISABLED, userDn);
                    return false;
                } else if ((value & LOCKED_FLAG) == LOCKED_FLAG) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_LOCKED, userDn);
                    return false;
                }  else if ((value & EXPIRED_FLAG) == EXPIRED_FLAG) {
                    auditor.logAndAudit(SystemMessages.AUTH_USER_EXPIRED, userDn);
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * this check is only relevent to msad directories, it should be generalized and parametrable when
     * we discover other types of account expiration mechanism in other directory types.
     *
     * this looks at the attribute accountExpires if present and checks if the specific account
     * is expired based on that value.
     *
     * @return true is account is expired, false otherwise
     */
    @Override
    public boolean checkExpiredMSADAccount(String userDn, Attributes attibutes) {
        Attribute accountExpiresAttr = attibutes.get( LdapUtils.LDAP_ATTR_ACCOUNT_EXPIRES );
        if (accountExpiresAttr != null && accountExpiresAttr.size() > 0) {
            Object found = null;
            try {
                found = accountExpiresAttr.get(0);
            } catch (NamingException e) {
                logger.log(Level.SEVERE, "Problem accessing the accountExpiresAttr attribute");
            }
            if (found != null) {
                if (found instanceof String) {
                    String value = (String)found;
                    if (value.equals("0")) {
                        return false;
                    } else {
                        BigInteger thisvalue = new BigInteger(value);
                        BigInteger result = thisvalue.subtract(fileTimeConversionfactor).
                                                            divide(fileTimeConversionfactor2);
                        if (System.currentTimeMillis() > result.longValue()) {
                            auditor.logAndAudit(SystemMessages.AUTH_USER_EXPIRED, userDn);
                            return true;
                        }
                    }
                } else {
                    logger.severe("the attribute accountExpires was present but yielded a type not " +
                                  "supported: " + found.getClass().getName());
                }
            }
        }
        return false;
    }

    /**
     * Constructs an EntityHeader for the dn passed.
     *
     * @param sr The search result
     * @return the EntityHeader for the dn or null if the object class is not supported or if the entity
     *         should be ignored (perhaps disabled)
     */
    @Override
    public IdentityHeader searchResultToHeader(SearchResult sr) {
        final Attributes atts = sr.getAttributes();
        final String dn = sr.getNameInNamespace();
        // is it user or group ?
        Attribute objectclasses = atts.get( OBJECTCLASS_ATTRIBUTE_NAME );
        // check if it's a user
        UserMappingConfig[] userTypes = config.getUserMappings();
        for (UserMappingConfig userType : userTypes) {
            String userclass = userType.getObjClass();
            if (LdapUtils.attrContainsCaseIndependent(objectclasses, userclass)) {
                if (!isValidEntryBasedOnUserAccountControlAttribute(dn, atts) || checkExpiredMSADAccount(dn, atts)) {
                    return null;
                }
                Object tmp;
                String login = null;
                try {
                    tmp = LdapUtils.extractOneAttributeValue(atts, userType.getLoginAttrName());
                } catch (NamingException e) {
                    logger.log(Level.WARNING, "cannot extract user login", e);
                    tmp = null;
                }
                if (tmp != null) {
                    login = tmp.toString();
                }

                // if cn is present use it
                String cn = null;
                try{
                    tmp = LdapUtils.extractOneAttributeValue(atts, userType.getNameAttrName());
                }catch(NamingException e){
                    logger.log(Level.WARNING, "cannot extract cn", e);
                    tmp = null;
                }
                if(tmp != null){
                    cn = tmp.toString();
                }

                // if description attribute present, use it
                String description = null;
                try {
                    tmp = LdapUtils.extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                if (login != null) {
                    return new IdentityHeader(config.getOid(), dn, EntityType.USER, login, description, cn, null);
                } else {
                    return null;
                }
            }
        }
        // check that it's a group
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        for (GroupMappingConfig groupType : groupTypes)
            if (LdapUtils.attrContainsCaseIndependent(objectclasses, groupType.getObjClass())) {
                String groupName = null;
                Attribute valuesWereLookingFor = atts.get(groupType.getNameAttrName());
                if (valuesWereLookingFor != null && valuesWereLookingFor.size() > 0) {
                    try {
                        groupName = valuesWereLookingFor.get(0).toString();
                    } catch (NamingException e) {
                        logger.warning("cannot extract name from this group");
                    }
                }
                if (groupName == null) groupName = dn;
                // if description attribute present, use it
                Object tmp;
                String description = null;
                try {
                    tmp = LdapUtils.extractOneAttributeValue(atts, DESCRIPTION_ATTRIBUTE_NAME);
                } catch (NamingException e) {
                    logger.log(Level.FINEST, "no description for this entry", e);
                    tmp = null;
                }
                if (tmp != null) {
                    description = tmp.toString();
                }
                return new IdentityHeader(config.getOid(), dn, EntityType.GROUP, groupName, description, null, null);
            }
        return null;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.auditor = new Auditor(this, applicationContext, logger);
    }

    /*
     * ValidationException exceptions do not state that the user belongs to an ldap or in which
     * ldap the user was not found
     **/
    @Override
    public void validate(LdapUser u) throws ValidationException {
        User validatedUser;
        try{
            validatedUser = userManager.findByPrimaryKey(u.getId());
        }
        catch (FindException e){
            throw new ValidationException("User " + u.getLogin()+" did not validate", e);
        }

        if(validatedUser == null){
            throw new ValidationException("IdentityProvider User " + u.getLogin()+" not found");
        }
    }

    @Override
    public long getLdapConnectionTimeout() {
        return ldapConnectionTimeout.get();
    }

    @Override
    public long getLdapReadTimeout() {
        return ldapReadTimeout.get();
    }

    @Override
    public void propertyChange(final PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (ServerConfig.PARAM_LDAP_CONNECTION_TIMEOUT.equals(propertyName)) {
            loadConnectionTimeout();
        } else if (ServerConfig.PARAM_LDAP_READ_TIMEOUT.equals(propertyName)) {
            loadReadTimeout();
        } else if (ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE.equals(propertyName)) {
            loadMaxSearchResultSize();
        } else if (ServerConfig.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL.equals(propertyName)) {
            loadIndexRebuildIntervalProperty();
        } else if (ServerConfig.PARAM_LDAPCERT_CACHE_LIFETIME.equals(propertyName)) {
            loadCachedCertEntryLifeProperty();
        }
    }

    private String formatCertificateSearchFilter( final String filter ) throws InvalidIdProviderCfgException {
        try {
            return formatCertificateSearchFilter( filter, new X500Principal("cn=Test Issuer, ou=Test Organization, c=Test"), BigInteger.ZERO, HexUtils.encodeBase64(new byte[1])) ;
        } catch (FindException fe) {
            throw new InvalidIdProviderCfgException( fe.getMessage() );
        }
    }

    private String formatCertificateSearchFilter( final String filter, final X500Principal issuer, final BigInteger serial, final String ski ) throws FindException  {
        Map<String,String> varMap = new HashMap<String,String>();

        if ( issuer != null ) {
            varMap.put( "issuer", issuer.getName() );
            varMap.put( "issuer.canonical", issuer.getName(X500Principal.CANONICAL));
            varMap.put( "issuer.rfc2253", issuer.getName() );
        }

        if ( serial != null ) {
            varMap.put( "serialnumber", serial.toString() );
        }

        if ( ski != null ) {
            varMap.put( "subjectkeyidentifier", ski );
            varMap.put( "subjectkeyidentifier.hex", HexUtils.hexDump(HexUtils.decodeBase64(ski)));
        }

        try {
            return ExpandVariables.process( filter, varMap, auditor, true, new Functions.Unary<String,String>(){
                @Override
                public String call( final String value ) {
                    return LdapUtils.filterEscape( value );
                }
            } );
        } catch ( VariableNameSyntaxException vnse ) {
            throw new FindException("Search filter variable error '"+ExceptionUtils.getMessage(vnse)+"'.");
        } catch ( IllegalArgumentException iae ) {
            throw new FindException("Search filter variable processing error '"+ExceptionUtils.getMessage(iae)+"'.");
        }
    }

    private String[] buildReturningAttributes() {
        Set<String> attributeNames = new LinkedHashSet<String>();

        // Various hard coded attributes that we use
        attributeNames.add( OBJECTCLASS_ATTRIBUTE_NAME );
        attributeNames.add( DESCRIPTION_ATTRIBUTE_NAME );
        attributeNames.add( LdapUtils.LDAP_ATTR_USER_ACCOUNT_CONTROL );
        attributeNames.add( LdapUtils.LDAP_ATTR_ACCOUNT_EXPIRES );
        attributeNames.add( LdapUtils.LDAP_ATTR_USER_CERTIFICATE );

        // User mapping attributes
        UserMappingConfig[] userTypes = config.getUserMappings();
        for (UserMappingConfig userType : userTypes) {
            addValidName( attributeNames, userType.getEmailNameAttrName() );
            addValidName( attributeNames, userType.getFirstNameAttrName() );
            addValidName( attributeNames, userType.getKerberosAttrName() );
            addValidName( attributeNames, userType.getKerberosEnterpriseAttrName() );
            addValidName( attributeNames, userType.getLastNameAttrName() );
            addValidName( attributeNames, userType.getLoginAttrName() );
            addValidName( attributeNames, userType.getNameAttrName() );
            addValidName( attributeNames, userType.getPasswdAttrName() );
            addValidName( attributeNames, userType.getUserCertAttrName() );
        }

        // Group mapping attributes
        GroupMappingConfig[] groupTypes = config.getGroupMappings();
        for (GroupMappingConfig groupType : groupTypes) {
            addValidName( attributeNames, groupType.getMemberAttrName() );
            addValidName( attributeNames, groupType.getNameAttrName() );
        }

        // Configured attributes
        String[] attributes = config.getReturningAttributes();
        if ( attributes != null ) {
            attributeNames.addAll( Arrays.asList(attributes) );
        }

        return attributeNames.toArray( new String[attributeNames.size()] );
    }

    private void addValidName( final Collection<? super String> names, final String name ) {
        if ( name != null && !name.isEmpty() ) {
            names.add( name.trim() );
        }
    }

    private static final class CertCacheKey extends Pair<String,String>{
        public CertCacheKey( final String dn, final String thumbprint) {
            super(dn,thumbprint);
        }
    }

    private static final class CertCacheEntry {
        private final X509Certificate cert;
        private final long entryCreation;

        CertCacheEntry(X509Certificate cert) {
            this.entryCreation = System.currentTimeMillis();
            this.cert = cert;
        }
    }

    private static class CertIndex {
        /**
         * This is a map which indexes the DN of LDAP entries for the entry's certificate serial and issuer dn.
         * Key: a string, lowercased, that is a Pair of IssuerDn and serial num.
         * Value: the DN of the LDAP entry containing the certificate and the certificates thumbprint
         */
        private final Map<Pair<String, String>, CertCacheKey> certIndexByIssuerSerial;

        /**
         * This is a map which indexes the DN of LDAP entries for the entry's certificate SKI.
         * Key: a string, BASE64 SKI.
         * Value: the DN of the LDAP entry containing the certificate and the certificates thumbprint
         */
        private final Map<String, CertCacheKey> certIndexBySki;

        /**
         * This is a map which indexes the DN of LDAP entries for the entry's ThumbprintSHA1.
         * Key: a string, BASE64 SKI.
         * Value: the DN of the LDAP entry containing the certificate and the certificates thumbprint
         */
        private final Map<String, CertCacheKey> certIndexByThumbprintSHA1;

        private final boolean immutable;

        protected CertIndex() {
            this.certIndexByIssuerSerial = new ConcurrentHashMap<Pair<String, String>, CertCacheKey>();
            this.certIndexBySki = new ConcurrentHashMap<String, CertCacheKey>();
            this.certIndexByThumbprintSHA1 = new ConcurrentHashMap<String, CertCacheKey>();
            this.immutable = false;
        }

        protected CertIndex( final Map<Pair<String, String>, CertCacheKey> certIndexByIssuerSerial,
                             final Map<String, CertCacheKey> certIndexBySki,
                             final Map<String, CertCacheKey> certIndexByThumbprintSHA1,
                             final boolean immutable ) {
            this.certIndexByIssuerSerial = certIndexByIssuerSerial;
            this.certIndexBySki = certIndexBySki;
            this.certIndexByThumbprintSHA1 = certIndexByThumbprintSHA1;
            this.immutable = immutable;
        }

        protected CertCacheKey getCertCacheKeyByIssuerAndSerial( final X500Principal issuer, final BigInteger serial ) {
            Pair<String, String> key = makeIndexKey(issuer, serial);
            return certIndexByIssuerSerial.get(key);
        }

        protected CertCacheKey getCertCacheKeyBySki( final String ski ) {
            return certIndexBySki.get(ski);
        }

        protected CertCacheKey getCertCacheKeyByThumbprintSHA1( final String thumbprintSHA1 ) {
            return certIndexByThumbprintSHA1.get(thumbprintSHA1);
        }

        protected CertCacheKey addCertificateToIndexes( final String dn,
                                                        final X509Certificate cert ) throws CertificateException {
            CertCacheKey certCacheKey = null;

            if ( !immutable ) {
                final String thumbprintSHA1 = CertUtils.getThumbprintSHA1(cert);
                certCacheKey = new CertCacheKey( dn, thumbprintSHA1 );

                if (cert.getSerialNumber() != null && cert.getIssuerX500Principal() != null) {
                    X500Principal issuer = cert.getIssuerX500Principal();
                    BigInteger serial = cert.getSerialNumber();

                    Pair<String, String> key = makeIndexKey(issuer, serial);
                    logger.finer("Indexing certificate by issuer/serial " + key + " for " + dn);
                    certIndexByIssuerSerial.put(key, certCacheKey);
                }
                String ski = CertUtils.getSki( cert );
                if ( ski != null ) {
                    logger.finer("Indexing certificate by ski " + ski + " for " + dn);
                    certIndexBySki.put(ski, certCacheKey);
                }

                logger.finer("Indexing certificate by thumbprintSHA1 " + thumbprintSHA1 + " for " + dn);
                certIndexByThumbprintSHA1.put(thumbprintSHA1, certCacheKey);
            }

            return certCacheKey;
        }

        protected void removeIndex( final X500Principal issuer, final BigInteger serial, final String ski ) {
            if ( !immutable ) {
                if ( issuer != null && serial != null ) {
                    certIndexByIssuerSerial.remove(makeIndexKey(issuer, serial));
                }

                if ( ski != null ) {
                    certIndexBySki.remove(ski);
                }
            }
        }

        protected CertIndex immutable() {
            return immutable ? this : new CertIndex( 
                    Collections.unmodifiableMap(certIndexByIssuerSerial),
                    Collections.unmodifiableMap(certIndexBySki),
                    Collections.unmodifiableMap(certIndexByThumbprintSHA1),
                    true);
        }

        protected String describe() {
            StringBuilder description = new StringBuilder();
            description.append("Issuer/Serial index size: ");
            description.append(certIndexByIssuerSerial.size());
            description.append(", SKI index size: ");
            description.append(certIndexBySki.size());
            description.append(", ThumbprintSHA1 index size: ");
            description.append(certIndexByThumbprintSHA1.size());
            return description.toString();
        }

        private Pair<String,String> makeIndexKey(X500Principal issuerDN, BigInteger certSerial) {
            return new Pair<String, String>(issuerDN.getName(X500Principal.RFC2253), certSerial.toString());
        }
    }

    private static final Logger logger = Logger.getLogger(LdapIdentityProviderImpl.class.getName());

    private static final BigInteger fileTimeConversionfactor = new BigInteger("116444736000000000");
    private static final BigInteger fileTimeConversionfactor2 = new BigInteger("10000");

    private static final long DEFAULT_INDEX_REBUILD_INTERVAL = 1000*60*10; // 10 minutes
    private static final long DEFAULT_CACHE_CLEANUP_INTERVAL = 1000*60*10; // 10 minutes
    private static final long DEFAULT_CACHED_CERT_ENTRY_LIFE = 1000*60*10; // 10 minutes
    private static final long MIN_INDEX_REBUILD_TIME = 10000; //10 seconds
    private static final long MIN_CERT_CACHE_LIFETIME = 10000; //10 seconds
    private static final long MAX_CACHE_AGE_VALUE = 30000;
    private static final long DEFAULT_MAX_SEARCH_RESULT_SIZE = 100;

    private final AtomicLong rebuildTimerLength = new AtomicLong(DEFAULT_INDEX_REBUILD_INTERVAL);
    private final AtomicLong cleanupTimerLength = new AtomicLong(DEFAULT_CACHE_CLEANUP_INTERVAL);
    private final AtomicLong cachedCertEntryLife = new AtomicLong(DEFAULT_CACHED_CERT_ENTRY_LIFE);
    private final AtomicLong ldapConnectionTimeout = new AtomicLong(DEFAULT_LDAP_CONNECTION_TIMEOUT);
    private final AtomicLong ldapReadTimeout = new AtomicLong(DEFAULT_LDAP_READ_TIMEOUT);
    private final AtomicLong maxSearchResultSize = new AtomicLong(DEFAULT_MAX_SEARCH_RESULT_SIZE);

    private final ManagedTimer timer = new ManagedTimer("LDAP Certificate Cache Timer");
    private ManagedTimerTask rebuildTask;
    private ManagedTimerTask cleanupTask;

    private Auditor auditor;
    private ServerConfig serverConfig;
    private LdapIdentityProviderConfig config;
    private IdentityProviderConfigManager configManager;
    private ClientCertManager clientCertManager;
    private LdapUserManager userManager;
    private LdapGroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;

    private String lastSuccessfulLdapUrl;
    private long retryFailedConnectionTimeout;
    private final ReentrantReadWriteLock fallbackLock = new ReentrantReadWriteLock();
    private String[] ldapUrls;
    private Long[] urlStatus;
    private String[] returningAttributes;

    /**
     * Certificate index by issuer/serial and SKI
     */
    private final AtomicReference<CertIndex> certIndexRef = new AtomicReference<CertIndex>(new CertIndex());

    /**
     * This is a map to cache user certificates by the LDAP entry DN
     * Key: a string, lowercased, the DN of the LDAP entry to which the cert belong. This DN comes from the index certIndex
     * Value: the X509Certificate
     */
    private final HashMap<CertCacheKey, CertCacheEntry> certCache = new HashMap<CertCacheKey, CertCacheEntry>();

    /**
     * a lock for accessing the index above
     */
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();
}
