package com.l7tech.server.identity.ldap;

import EDU.oswego.cs.dl.util.concurrent.ReadWriteLock;
import EDU.oswego.cs.dl.util.concurrent.Sync;
import EDU.oswego.cs.dl.util.concurrent.WriterPreferenceReadWriteLock;
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
import com.l7tech.server.ServerConfig;
import com.l7tech.server.audit.Auditor;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.ConfigurableIdentityProvider;
import com.l7tech.server.identity.DigestAuthenticator;
import com.l7tech.server.identity.cert.CertificateAuthenticator;
import com.l7tech.server.logon.LogonService;
import com.l7tech.server.transport.http.SslClientSocketFactory;
import com.l7tech.server.util.ManagedTimer;
import com.l7tech.server.util.ManagedTimerTask;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import com.l7tech.util.ResourceUtils;
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
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
@SuppressWarnings({ "ThrowableResultOfMethodCallIgnored" })
public class LdapIdentityProviderImpl
        implements LdapIdentityProvider, InitializingBean, DisposableBean, ApplicationContextAware, ConfigurableIdentityProvider, PropertyChangeListener
{
    private Auditor auditor;
    private final static long DEFAULT_INDEX_REBUILD_INTERVAL = 1000*60*10; // 10 minutes
    private final static long DEFAULT_CACHE_CLEANUP_INTERVAL = 1000*60*10; // 10 minutes
    private final static long DEFAULT_CACHED_CERT_ENTRY_LIFE = 1000*60*10; // 10 minutes
    private static final long MIN_INDEX_REBUILD_TIME = 10000; //10 seconds
    private static final long MIN_CERT_CACHE_LIFETIME = 10000; //10 seconds
    private static final long MAX_CACHE_AGE_VALUE = 30000;

    private final AtomicLong rebuildTimerLength = new AtomicLong(DEFAULT_INDEX_REBUILD_INTERVAL);
    private final AtomicLong cleanupTimerLength = new AtomicLong(DEFAULT_CACHE_CLEANUP_INTERVAL);
    private static long cachedCertEntryLife = DEFAULT_CACHED_CERT_ENTRY_LIFE;

    private LogonService logonService;

    private final ManagedTimer timer = new ManagedTimer("LDAP Certificate Cache Timer");
    private ManagedTimerTask rebuildTask;
    private ManagedTimerTask cleanupTask;

    public LdapIdentityProviderImpl() {
        ldapConnectionTimeout = DEFAULT_LDAP_CONNECTION_TIMEOUT;
        ldapReadTimeout = DEFAULT_LDAP_READ_TIMEOUT;
    }

    @Override
    public void setIdentityProviderConfig(IdentityProviderConfig configuration) throws InvalidIdProviderCfgException {
        if (this.config != null) {
            throw new InvalidIdProviderCfgException("Provider is already configured");
        }
        this.config = (LdapIdentityProviderConfig) configuration;
        if (this.config.getLdapUrl() == null || this.config.getLdapUrl().length < 1) {
            throw new InvalidIdProviderCfgException("This config does not contain an ldap url"); // should not happen
        }

        // worker thread which cleans old certs in the cache
        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                cleanupCertCache();
            }
        }, 5000, cleanupTimerLength.longValue()); // every 5 minutes

        if ( userManager == null ) {
            throw new InvalidIdProviderCfgException("UserManager is not set");
        }
        if ( groupManager == null ) {
            throw new InvalidIdProviderCfgException("GroupManager is not set");
        }

        userManager.configure( this );
        groupManager.configure( this );

        initializeFallbackMechanism();

        if (serverConfig != null) { //should be injected
            ldapConnectionTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_CONNECTION_TIMEOUT, DEFAULT_LDAP_CONNECTION_TIMEOUT, MAX_CACHE_AGE_VALUE);
            ldapReadTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_READ_TIMEOUT, DEFAULT_LDAP_READ_TIMEOUT, MAX_CACHE_AGE_VALUE);
        }
    }

    /**
     * This is a map which indexes the DN of LDAP entries for the entry's certificate serial and issuer dn.
     * Key: a string, lowercased, that is a Pair of IssuerDn and serial num.
     * Value: the DN of the LDAP entry containing the actual certificate
     */
    private HashMap<Pair<String, String>, String> certIndex = new HashMap<Pair<String, String>, String>();

    /**
     * This is a map to cache user certificates by the LDAP entry DN
     * Key: a string, lowercased, the DN of the LDAP entry to which the cert belong. This DN comes from the index certIndex
     * Value: the X509Certificate
     */
    private final HashMap<String, CertCacheEntry> certCache = new HashMap<String, CertCacheEntry>();

    /**
     * a lock for accessing the index above
     */
    private final ReentrantReadWriteLock indexLock = new ReentrantReadWriteLock();

    /**
     * a lock for accessing the index above
     */
    private final ReentrantReadWriteLock cacheLock = new ReentrantReadWriteLock();

    public void getIndexRebuildIntervalProperty() {
        logger.fine("Checking the ldap cache rebuild interval property");
        long indexRebuildInterval = DEFAULT_INDEX_REBUILD_INTERVAL;

        String scp = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_LDAPCERTINDEX_REBUILD_INTERVAL);
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
        logger.fine("Certificate index rebuild interval = " + rebuildTimerLength);
    }

    public void getCachedCertEntryLifeProperty() {

        long cleanupLife = DEFAULT_CACHED_CERT_ENTRY_LIFE;

        String scp = ServerConfig.getInstance().getPropertyCached(ServerConfig.PARAM_LDAPCERT_CACHE_LIFETIME);
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
        cachedCertEntryLife = cleanupLife;
        logger.fine("Certificate cache entry lifetime = " + cleanupLife);
    }

    private static class RebuildTask extends ManagedTimerTask {
        final LdapIdentityProviderImpl ldapProvRef;
        final String providerName;
        final long oid;
        RebuildTask(LdapIdentityProviderImpl prov) {
            this.ldapProvRef = prov;
            providerName = prov.getConfig().getName();
            oid = prov.getConfig().getOid();
        }

        @Override
        protected void doRun() {
            //when the referant Identity provider goes away, this timer should stop
            if (ldapProvRef.wasRemoved()) {
                logger.info("Cancelling cert index task for '" + providerName + "' oid: " + oid);
                cancel();
            } else if (!ldapProvRef.certsAreEnabled()) {
                logger.fine("LDAP identity provider \"" + providerName + "\" is not configured to enable user certificates in LDAP. The user certificate index will not be built.");
            } else {
                long oldIndexInterval = ldapProvRef.rebuildTimerLength.get();
                ldapProvRef.getIndexRebuildIntervalProperty();
                long newIndexInterval = ldapProvRef.rebuildTimerLength.get();

                if (oldIndexInterval != newIndexInterval) {
                    logger.info(MessageFormat.format(
                            "Certificate index rebuild interval has changed (old value = {0}, new value = {1}). Rescheduling this task with new interval.", 
                            oldIndexInterval,
                            newIndexInterval));
                    ldapProvRef.rescheduleIndexRebuildTask();
                } else {
                    ldapProvRef.rebuildCertIndex();
                }
            }
        }
    }

    private void rescheduleIndexRebuildTask() {
        cancelTasks(rebuildTask);
        rebuildTask = new RebuildTask(this);
        scheduleTasks(new Pair<ManagedTimerTask, Long>(rebuildTask, rebuildTimerLength.get())); 
    }

    private static class CleanupTask extends ManagedTimerTask {
        final LdapIdentityProviderImpl ldapProv;
        String providerName;


        CleanupTask(LdapIdentityProviderImpl prov) {
            this.ldapProv = prov;
            providerName = prov.getConfig().getName();
        }

        @Override
        protected void doRun() {
            //when the referant Identity provider goes away, this timer should stop
            if (ldapProv.wasRemoved()) {
                logger.info("Cancelling cert cache cleanup task for '" + providerName + "' oid: " + ldapProv.config.getOid());
                cancel();
            }
            else {
                ldapProv.cleanupCertCache();
            }
        }
    }

    public LdapIdentityProviderImpl(IdentityProviderConfig config) {
        this.config = (LdapIdentityProviderConfig)config;
        if (this.config.getLdapUrl() == null || this.config.getLdapUrl().length < 1) {
            throw new IllegalArgumentException("This config does not contain an ldap url"); // should not happen
        }

        rebuildTask = new RebuildTask(this);
        cleanupTask = new CleanupTask(this);

        getTaskPeriods();
        scheduleTasks(
                new Pair<ManagedTimerTask, Long>(rebuildTask, rebuildTimerLength.get()),
                new Pair<ManagedTimerTask, Long>(cleanupTask, cleanupTimerLength.get()));
    }

    private boolean wasRemoved() {
        if (configManager == null) {
            logger.warning("Config Manager is null.");
        } else {
            try {
                return configManager.findByPrimaryKey(config.getOid()) == null;
            } catch (FindException e) {
                logger.warning("Error checking identity configuration for " + config);
            }
        }
        return false; // don't assume it was removed if there are errors getting to it
    }

    public void scheduleTasks(Pair<ManagedTimerTask, Long> ... whichTasks) {
        for (Pair<ManagedTimerTask, Long> whichTask : whichTasks) {
            timer.schedule(whichTask.left, 5000, whichTask.right);
        }
    }

    public void getTaskPeriods() {
        getIndexRebuildIntervalProperty();
    }

    public class CertCacheEntry {
        public CertCacheEntry(X509Certificate cert) {
            entryCreation = System.currentTimeMillis();
            this.cert = cert;
        }
        public X509Certificate cert;
        public long entryCreation;
    }

    private void cleanupCertCache() {
        getCachedCertEntryLifeProperty();

        ArrayList<String> todelete = new ArrayList<String>();
        long now = System.currentTimeMillis();
        cacheLock.readLock().lock();
        try {
            Set<String> keys = certCache.keySet();
            for (String key : keys) {
                CertCacheEntry cce = certCache.get(key);
                if ((now - cce.entryCreation) > cachedCertEntryLife) {
                    todelete.add(key);
                }
            }
        } finally {
            cacheLock.readLock().unlock();
        }
        if (todelete.size() > 0) {
            cacheLock.writeLock().lock();
            try {
                for (String key : todelete) {
                    logger.info("removing from cache " + key);
                    certCache.remove(key);
                }
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
    }

    private void rebuildCertIndex() {
        logger.fine("Re-creating ldap user certificate index for " + config.getName());
        DirContext context = null;
        NamingEnumeration answer = null;
        HashMap<Pair<String,String>, String> tmpCertIndex = new HashMap<Pair<String, String>, String>();
        try {
            context = getBrowseContext();
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            UserMappingConfig[] mappings = config.getUserMappings();
            for (UserMappingConfig mapping : mappings) {
                String certAttributeName = mapping.getUserCertAttrName();
                answer = context.search(config.getSearchBase(), "(" + certAttributeName + "=*)", sc);

                while (answer.hasMore()) {
                    SearchResult sr = (SearchResult)answer.next();
                    Object tmp = LdapUtils.extractOneAttributeValue(sr.getAttributes(), certAttributeName);
                    if (tmp != null) {
                        if (tmp instanceof byte[]) {
                            X509Certificate cert = CertUtils.decodeCert((byte[])tmp);
                            if (cert.getSerialNumber() != null && cert.getIssuerX500Principal() != null) {
                                BigInteger serialNumber = cert.getSerialNumber();
                                X500Principal issuerDn = cert.getIssuerX500Principal();

                                Pair<String, String> key = makeIndexKey(issuerDn, serialNumber);
                                String val = sr.getNameInNamespace();
                                logger.fine("Indexing " + key + " for " + val);
                                tmpCertIndex.put(key, val);
                            }
                        }
                    }
                }
            }
            indexLock.writeLock().lock();
            try {
                certIndex = tmpCertIndex;
            } finally {
                indexLock.writeLock().unlock();
            }
            logger.fine("serial cert index size: " + tmpCertIndex.size());
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while recreating ldap user certificate index for LDAP Provider '" + config.getName() + "': " + ExceptionUtils.getMessage(e), ExceptionUtils.getDebugException(e));
        } finally {
            if (context != null) {
                if (answer != null) {
                    ResourceUtils.closeQuietly(answer);
                }
                ResourceUtils.closeQuietly(context);
            }
        }
    }

    private boolean certsAreEnabled() {
        return getConfig().isUserCertsEnabled();
    }

    public void setServerConfig(ServerConfig serverConfig) {
        this.serverConfig = serverConfig;
    }

    public void setCertificateAuthenticator(CertificateAuthenticator certificateAuthenticator) {
        this.certificateAuthenticator = certificateAuthenticator;
    }

    public void initializeFallbackMechanism() {
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
    public IdentityProviderConfig getConfig() {
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

    public LogonService getLogonService() {
        return logonService;
    }

    public void setLogonService(LogonService logonService) {
        this.logonService = logonService;
    }

    /**
     * @return The ldap url that was last used to successfully connect to the ldap directory. May be null if
     *         previous attempt failed on all available urls.
     */
    @Override
    public String getLastWorkingLdapUrl() {
        Sync read = fallbackLock.readLock();
        try {
            read.acquire();
            return lastSuccessfulLdapUrl;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (read != null) read.release();
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
        Sync write = fallbackLock.writeLock();
        try {
            write.acquire();
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
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (write != null) write.release();
        }
    }

    @Override
    public AuthenticationResult authenticate(LoginCredentials pc) throws AuthenticationException {
        LdapUser realUser = null;
        if (pc.getFormat() == CredentialFormat.KERBEROSTICKET) {
            KerberosServiceTicket ticket = (KerberosServiceTicket) pc.getPayload();

            Collection<IdentityHeader> headers;
            try {
                headers = search(ticket);
                if (headers.size() > 1) {
                    throw new AuthenticationException("Found multiple LDAP users for kerberos principal '" + ticket.getClientPrincipalName() + "'.");
                }
                else if (!headers.isEmpty()){
                    for (IdentityHeader header : headers) {
                        if (header.getType() == EntityType.USER) {
                            realUser = userManager.findByPrimaryKey(header.getStrId());
                        }
                    }
                    return new AuthenticationResult(realUser);
                }
            } catch (FindException e) {
                throw new AuthenticationException("Couldn't find LDAP user for kerberos principal '" + ticket.getClientPrincipalName() + "'.", e);
            }
        } else {
            try {
                realUser = userManager.findByLogin(pc.getLogin());
            } catch (FindException e) {
                throw new AuthenticationException("Couldn't authenticate credentials", e);
            }
        }

        if (realUser == null) return null;

        final CredentialFormat format = pc.getFormat();
        if (format == CredentialFormat.CLEARTEXT) {
            long now = System.currentTimeMillis();
            logonService.hookPreLoginCheck(realUser, now);
            return authenticatePasswordCredentials(pc, realUser);
        } else if (format == CredentialFormat.DIGEST) {
            return DigestAuthenticator.authenticateDigestCredentials(pc, realUser);
        } else {
            if (format == CredentialFormat.CLIENTCERT || format == CredentialFormat.SAML) {

                    //get the LDAP cert for this user if LDAP certs are enabled for this provider
                    if (certsAreEnabled() && realUser.getLdapCert() != null) {
                        try {
                            return certificateAuthenticator.authenticateX509Credentials(pc,
                                                                             CertUtils.decodeCert(realUser.getLdapCert()),
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

    private AuthenticationResult authenticatePasswordCredentials(LoginCredentials pc, LdapUser realUser) throws BadCredentialsException {
        // basic authentication
        boolean res = userManager.authenticateBasic(realUser.getDn(), new String(pc.getCredentials()));
        if (res) {
            // success
            AuthenticationResult ar = new AuthenticationResult(realUser);
            logonService.updateLogonAttempt(realUser, ar);
            return ar;
            //return new AuthenticationResult(realUser);
        }
        logger.info("credentials did not authenticate for " + pc.getLogin());
        //logonService.updateLogonAttempt(realUser, null);
        throw new BadCredentialsException("credentials did not authenticate");
    }

    @Override
    public long getMaxSearchResultSize() {
        if (maxSearchResultSize <= 0) {
            String tmp = serverConfig.getPropertyCached(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE);
            if (tmp == null) {
                logger.info(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " is not set. using default value.");
                maxSearchResultSize = 100;
            } else {
                long tmpl;
                try {
                    tmpl = Long.parseLong(tmp);
                    if (tmpl <= 0) {
                        logger.info(ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " has invalid value: " + tmp +
                                    ". using default value.");
                        maxSearchResultSize = 100;
                    } else {
                        logger.info("read system value " + ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE + " of " + tmp);
                        maxSearchResultSize = tmpl;
                    }
                } catch (NumberFormatException e) {
                    logger.log(Level.WARNING, "the property " + ServerConfig.MAX_LDAP_SEARCH_RESULT_SIZE +
                                              " has an invalid format. falling back on default value.", e);
                    maxSearchResultSize = 100;
                }
            }
        }
        return maxSearchResultSize;
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
    public X509Certificate findCertByIssuerAndSerial( final X500Principal issuerDN, final BigInteger certSerial ) {
        X509Certificate lookedupCert = null;

        if ( certsAreEnabled() ) {
            // look for presence of cert in index
            Pair<String, String> key = makeIndexKey(issuerDN, certSerial);
            indexLock.readLock().lock();
            String dnforcert = null;
            try {
                dnforcert = certIndex.get(key);
            } finally {
                indexLock.readLock().unlock();
            }

            if (dnforcert == null) {
                logger.info("The certificate with Issuer DN '" + issuerDN + "' and  Serial Number '" + certSerial + "' is not indexed. Returning null");
                return null;
            }

            // try to find cert in cert cache
            X509Certificate output = null;
            cacheLock.readLock().lock();
            try {
                CertCacheEntry cce = certCache.get(dnforcert);
                if (cce != null) output = cce.cert;
            } finally {
                cacheLock.readLock().unlock();
            }
            if (output != null) {
                logger.fine("Cert found in cache");
                return output;
            }

            // load the cert from ldap
            DirContext context = null;
            try {
                UserMappingConfig[] mappings = config.getUserMappings();

                context = getBrowseContext();
                Attributes attributes = context.getAttributes(dnforcert);
                Object tmp;
                for (UserMappingConfig mapping : mappings) {
                    String userCertAttrName = mapping.getUserCertAttrName();
                    if (userCertAttrName == null || "".equals(userCertAttrName)) {
                        logger.warning("No user certificate attribute has been configured for user mapping " + mapping.getObjClass());
                    } else {
                        tmp = LdapUtils.extractOneAttributeValue(attributes, userCertAttrName);
                        if (tmp != null) {
                            if (tmp instanceof byte[]) {
                                logger.info("Found the certificate in LDAP");
                                lookedupCert = CertUtils.decodeCert((byte[])tmp);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "error looking up user in directory " + dnforcert, e);
            } finally {
                try {
                    if (context != null) context.close();
                } catch (NamingException e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }

            if (lookedupCert == null) {
                logger.info("the cert is in the index but not in ldap (" + dnforcert + ")");
                return null;
            }

            // add the cert to the cert cache and return
            cacheLock.writeLock().lock();
            try {
                logger.info("caching cert for " + dnforcert);
                certCache.put(dnforcert, new CertCacheEntry(lookedupCert));
            } finally {
                cacheLock.writeLock().unlock();
            }
        }
        
        return lookedupCert;
    }

    private Pair<String,String> makeIndexKey(X500Principal issuerDN, BigInteger certSerial) {
        return new Pair<String, String>(issuerDN.getName(X500Principal.RFC2253), certSerial.toString());
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
        } catch (NamingException e) {
            throw new FindException("LDAP search error with filter " + filter, e);
        } finally {
            if (context != null) {
                if (answer != null) {
                    ResourceUtils.closeQuietly(answer);
                }
                ResourceUtils.closeQuietly(context);
            }
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
        StringBuffer output = new StringBuffer();
        if (terms.length > 1) output.append("(|");

        for (LdapSearchTerm term : terms) {
            output.append("(&");
            output.append("(objectClass=").append(term.objectclass).append(")");
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
            UnsynchronizedNamingProperties env = new UnsynchronizedNamingProperties();
            // fla note: this is weird. new BrowseContext objects are created at every operation so they
            // should not cross threads.
            env.put("java.naming.ldap.version", "3");
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            // when getting javax.naming.CommunicationException at
            env.put(Context.PROVIDER_URL, ldapurl);
            env.put("com.sun.jndi.ldap.connect.pool", "true");
            env.put("com.sun.jndi.ldap.connect.timeout", Long.toString(ldapConnectionTimeout));
            env.put("com.sun.jndi.ldap.read.timeout", Long.toString(ldapReadTimeout));
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
                    env.put("java.naming.ldap.factory.socket", SslClientSocketFactory.class.getName());
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

            env.lock();

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
                // note. i am not embedding the NamingException because it sometimes
                // contains com.sun.jndi.ldap.LdapCtx which does not implement serializable
                String msg;
                if (e instanceof javax.naming.AuthenticationException) {
                    msg = "Cannot connect to this directory, authentication failed.";
                    logger.log(Level.INFO, "LDAP configuration test failure. " + msg, ExceptionUtils.getDebugException(e));
                } else {
                    msg = "Cannot connect to this directory.";
                    logger.log(Level.INFO, "LDAP configuration test failure. " + msg, ExceptionUtils.getDebugException(e));
                }
                throw new InvalidIdProviderCfgException(msg, e);
            }

            // That's all for a quick test
            if ( quick ) return;

            String filter;
            SearchControls sc = new SearchControls();
            sc.setSearchScope(SearchControls.SUBTREE_SCOPE);

            // make sure the base DN is valid and contains at least one entry
            try {
                context.search(config.getSearchBase(), "(objectClass=*)", sc);
            } catch (NamingException e) {
                throw new InvalidIdProviderCfgException("Cannot search using base: " + config.getSearchBase(), e);
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
                    if (answer != null) ResourceUtils.closeQuietly(answer);
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
                    if (answer != null) ResourceUtils.closeQuietly(answer);
                }
            }

            // merge all errors in a special report
            StringBuffer error = new StringBuffer();

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

            if (error.length() > 0) {
                logger.fine("Test produced following error(s): " + error.toString());
                throw new InvalidIdProviderCfgException(error.toString());
            } else
                logger.finest("this ldap config was tested successfully");
        } finally {
            if (context != null) ResourceUtils.closeQuietly(context);
            cancelTasks(rebuildTask, cleanupTask);
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
    public boolean updateFailedLogonAttempt(LoginCredentials lc) {
        LdapUser realUser;
        try {
            realUser = userManager.findByLogin(lc.getLogin());
        } catch (FindException e) {
            return false;
        }

        logonService.updateLogonAttempt(realUser, null);
        return true;
    }

    @Override
    public boolean hasClientCert(LoginCredentials lc) throws AuthenticationException {
        try {
            LdapUser ldapUser = userManager.findByLogin(lc.getLogin());
            if (ldapUser != null) {
                //check where we should be looking for the cert, the cert could reside either in LDAP or gateway
                if (certsAreEnabled()) {
                    //telling use to search cert through ldap
                    return ldapUser.getLdapCert() != null;
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

    public void setClientCertManager(ClientCertManager clientCertManager) {
        this.clientCertManager = clientCertManager;
    }

    public void setConfigManager(IdentityProviderConfigManager configManager) {
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
    }

    @Override
    public void destroy() throws Exception {
        cancelTasks(rebuildTask, cleanupTask);
    }

    private void cancelTasks(ManagedTimerTask ... tasks) {
        for (ManagedTimerTask task : tasks) {
            if (task != null) {
                task.cancel();
            }
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
        Attribute userAccountControlAttr = attibutes.get("userAccountControl");
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
        Attribute accountExpiresAttr = attibutes.get("accountExpires");
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
        Attributes atts = sr.getAttributes();
        final String dn = sr.getNameInNamespace();
        // is it user or group ?
        Attribute objectclasses = atts.get("objectclass");
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
        return ldapConnectionTimeout;
    }

    @Override
    public long getLdapReadTimeout() {
        return ldapReadTimeout;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        String propertyName = evt.getPropertyName();
        if (ServerConfig.PARAM_LDAP_CONNECTION_TIMEOUT.equals(propertyName)) {
            ldapConnectionTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_CONNECTION_TIMEOUT, DEFAULT_LDAP_CONNECTION_TIMEOUT, MAX_CACHE_AGE_VALUE);
        } else if (ServerConfig.PARAM_LDAP_READ_TIMEOUT.equals(propertyName)) {
            ldapReadTimeout = serverConfig.getTimeUnitPropertyCached(ServerConfig.PARAM_LDAP_READ_TIMEOUT, DEFAULT_LDAP_READ_TIMEOUT, MAX_CACHE_AGE_VALUE);
        }
    }

    private static final Logger logger = Logger.getLogger(LdapIdentityProviderImpl.class.getName());

    private ServerConfig serverConfig;
    private LdapIdentityProviderConfig config;
    private IdentityProviderConfigManager configManager;
    private ClientCertManager clientCertManager;
    private LdapUserManager userManager;
    private LdapGroupManager groupManager;
    private CertificateAuthenticator certificateAuthenticator;

    private String lastSuccessfulLdapUrl;
    private long retryFailedConnectionTimeout;
    private final ReadWriteLock fallbackLock = new WriterPreferenceReadWriteLock();
    private String[] ldapUrls;
    private Long[] urlStatus;
    private long ldapConnectionTimeout;
    private long ldapReadTimeout;

    private final BigInteger fileTimeConversionfactor = new BigInteger("116444736000000000");
    private final BigInteger fileTimeConversionfactor2 = new BigInteger("10000");
    private long maxSearchResultSize = -1;
}
