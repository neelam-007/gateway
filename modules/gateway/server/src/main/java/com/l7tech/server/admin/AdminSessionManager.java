package com.l7tech.server.admin;

import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.cluster.ClusterMaster;
import com.l7tech.server.event.EntityClassEvent;
import com.l7tech.server.event.admin.Updated;
import com.l7tech.server.event.system.ReadyForMessages;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.identity.internal.InternalUserPasswordManager;
import com.l7tech.server.logon.LogonService;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.rbac.RbacServices;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.RoleManagerIdentitySourceSupport;
import com.l7tech.server.util.PostStartupApplicationListener;
import com.l7tech.util.*;
import org.apache.commons.collections.map.LRUMap;
import org.springframework.context.ApplicationEvent;

import javax.security.auth.login.LoginException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles authentication and tracking for admin sessions.
 * <p/>
 * <p>The cookie is used to look up the username that authenticated with it.  Anyone who can steal a cookie can
 * resume an admin session as that user; thus, the cookies must be sent over SSL, never written to disk by
 * either client or server, and not kept longer than necessary.</p>
 */
public class AdminSessionManager extends RoleManagerIdentitySourceSupport implements PostStartupApplicationListener, PropertyChangeListener {

    //- PUBLIC

    public AdminSessionManager(final Config config,
                               final LogonService logonService,
                               final Timer timer,
                               final ClusterMaster clusterMaster) {
        this.config = validated(config);
        this.logonService = logonService;
        this.timer = timer;
        this.clusterMaster = clusterMaster;

        int cacheSize = config.getIntProperty( ServerConfigParams.PARAM_PRINCIPAL_SESSION_CACHE_SIZE, 100);
        int cacheMaxTime = config.getIntProperty( ServerConfigParams.PARAM_PRINCIPAL_SESSION_CACHE_MAX_TIME, 300000);
        int cacheMaxGroups = config.getIntProperty( ServerConfigParams.PARAM_PRINCIPAL_SESSION_CACHE_MAX_PRINCIPAL_GROUPS, 50);

        this.groupCache = new GroupCache("PrincipalCache_unified", cacheSize, cacheMaxTime, cacheMaxGroups);
        this.sessionExpiryMillis.set(loadExpiryMillis());
    }

    public void setRoleManager(final RoleManager roleManager) {
        this.roleManager = roleManager;
    }

    public void setIdentityProviderFactory(final IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void setPasswordEnforcerManager(final PasswordEnforcerManager passwordEnforcerManager) {
        this.passwordEnforcerManager = passwordEnforcerManager;
    }

    public void setRbacServices(final RbacServices rbacServices) {
        this.rbacServices = rbacServices;
    }

    /**
     * Reloads IdentityProviders on change.
     */
    @Override
    public void onApplicationEvent(final ApplicationEvent event) {
        if (event instanceof EntityClassEvent) {
            EntityClassEvent eie = (EntityClassEvent) event;
            if (eie.getEntityClass() == IdentityProviderConfig.class) {
                setupAdminProviders();
            }
        } else if (event instanceof Updated) {
            // handles direct group updates and GroupMembershipEvents
            final Updated updatedEvent = (Updated) event;
            final Entity perhapsGroup = updatedEvent.getEntity();
            if ( perhapsGroup instanceof Group ) {
                groupCache.invalidate(((Group)perhapsGroup).getProviderId());
            }
            if ( perhapsGroup instanceof User){
                groupCache.invalidate(((User)perhapsGroup).getProviderId());
            }

        } else if (event instanceof ReadyForMessages) {
            // check for inactive users once a day
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if(!clusterMaster.isMaster()){
                        return;
                    }
                    logonService.updateInactivityInfo();
                }
            }, 1L, TimeUnit.HOURS.toMillis(24L));

            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    if(!clusterMaster.isMaster()){
                        return;
                    }
                    logonService.checkLogonInfos();
                }
            }, 1L, TimeUnit.HOURS.toMillis(24L));

        }
    }

    @Override
    public void propertyChange(final PropertyChangeEvent event) {
        final String propertyName = event.getPropertyName();

        if (propertyName != null && propertyName.equals("principalSessionCacheMaxTime")) {
            int cacheMaxTime = config.getIntProperty( ServerConfigParams.PARAM_PRINCIPAL_SESSION_CACHE_MAX_TIME, 300000);
            logger.config("Updating principal session cache max time '" + cacheMaxTime + "'.");
            groupCache.setCacheMaxTime(cacheMaxTime);
        }

        if (propertyName != null && propertyName.equals( ServerConfigParams.PARAM_SESSION_EXPIRY)) {
            long expiryMillis = loadExpiryMillis();
            logger.config("Updating session inactivity period '" + expiryMillis + "'.");
            this.sessionExpiryMillis.set(expiryMillis);
        }
    }

    /**
     * Authorize an administrative user against admin enabled identity providers.
     *
     * @param providerId The users identity provider
     * @param userId     The users id
     * @return The user or null if not authenticated.
     * @throws ObjectModelException If an error occurs during authorization.
     */
    public User authorize(final long providerId,
                          final String userId) throws ObjectModelException {
        Set<IdentityProvider> providers = getAdminIdentityProviders();
        User user = null;

        for (IdentityProvider provider : providers) {
            if (provider.getConfig().getOid() == providerId) {
                try {
                    User authdUser = provider.getUserManager().findByPrimaryKey(userId);
                    if (authdUser != null) {
                        //Validate the user , now authenticated so that we know all of their group roles
                        checkPerms(authdUser);
                        logger.fine("Authorized on " + provider.getConfig().getName());

                        user = authdUser;
                    }
                } catch (AuthenticationException ae) {
                    logger.warning("Authorization failed for user '" + userId + "', due to '" + ExceptionUtils.getMessage(ae) + "'.");
                }

                break;
            }
        }

        return user;
    }

    public AuthenticationResult authenticate(final LoginCredentials creds) throws ObjectModelException, LoginException, FailAttemptsExceededException, FailInactivityPeriodExceededException, UserDisabledException {
        boolean useCert = config.getBooleanProperty("security.policyManager.forbidPasswordWhenCertPresent", true);
        return authenticate(creds, useCert);
    }

    /**
     * Authenticate an administrative user against admin enabled identity providers.
     *
     * @param creds The users credentials
     * @return The user or null if not authenticated.
     * @throws ObjectModelException If an error occurs during authentication.
     */
    public AuthenticationResult authenticate(final LoginCredentials creds, final boolean requireCertIfExists) throws ObjectModelException, LoginException, FailAttemptsExceededException, FailInactivityPeriodExceededException, UserDisabledException {
        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();
        AuthenticationResult authResult = null;
        User user = null;

        final CredentialFormat credentialFormat = creds.getFormat();
        boolean formatOkForAdminAccess = credentialFormat == CredentialFormat.CLEARTEXT || credentialFormat == CredentialFormat.CLIENTCERT;
        if (!formatOkForAdminAccess)
            throw new LoginException("Unsupported credential format for admin access: " + credentialFormat);

        boolean needsClientCert = false;
        User providerFoundUser = null;  //temp provider container to know which provider first failed to authenticate
        for (IdentityProvider provider : providers) {
            try {
                //exit loop if the user needs a client certificate to login
                if (needsClientCert) {
                    break;
                }

                //verify if the client was assigned with a cert already and require to use it.  We only enforce this
                //for internal identity provider
                if (requireCertIfExists) {
                    if (credentialFormat == CredentialFormat.CLEARTEXT && provider.hasClientCert(creds.getLogin())) {
                        needsClientCert = true;
                        throw new BadCredentialsException("User '" + creds.getLogin() + "' did not use certificate as login credentials.");
                    }
                }

                User currentUser = null;
                try {
                    currentUser = ((AuthenticatingIdentityProvider) provider).findUserByCredential(creds);
                    if (currentUser == null) {
                        continue;
                    }

                    logonService.hookPreLoginCheck(currentUser);
                    final AuthenticationResult providerAuthResult = ((AuthenticatingIdentityProvider) provider).authenticate(creds, true);
                    final User providerAuthdUser = providerAuthResult == null ? null : providerAuthResult.getUser();
                    if (providerAuthdUser != null) {
                        //Validate the user , now authenticated so that we know all of their group roles
                        checkPerms(providerAuthdUser);
                        logger.info("Authenticated on " + provider.getConfig().getName());

                        //Ensure password not expired, ignore password expire if using cert to login
                        if (passwordEnforcerManager.isPasswordExpired(providerAuthdUser) && (creds.getClientCert() == null)) {
                            throw new CredentialExpiredPasswordDetailsException("Password expired.", passwordEnforcerManager.getPasswordPolicy().getDescription());
                        }

                        authResult = providerAuthResult;
                        user = providerAuthdUser;
                        break;
                    }
                } catch (BadCredentialsException bce) {
                    //we have found our first username with bad credentials, so we'll update the logon attempt
                    //on this particular one and not on preceeding ones
                    if (providerFoundUser == null) {
                        providerFoundUser = currentUser;
                    }
                }
            } catch (AuthenticationException e) {
                logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
                if (ExceptionUtils.causedBy(e, FailAttemptsExceededException.class)) {
                    throw new FailAttemptsExceededException(e.getMessage());
                }
                if (ExceptionUtils.causedBy(e, FailInactivityPeriodExceededException.class)) {
                    throw new FailInactivityPeriodExceededException(e.getMessage());
                }
                if (ExceptionUtils.causedBy(e, UserDisabledException.class)) {
                    throw new UserDisabledException(e.getMessage());
                }
            }
        }

        if (user != null) {
            logonService.updateLogonAttempt(user, authResult);
        } else {
            //if we have failed to authenticate this user from all provider, we'll need to update failed logon attempt
            //for the first provider that found user with the same username
            if (providerFoundUser != null) {
                logonService.updateLogonAttempt(providerFoundUser, null);
            }
            if (needsClientCert) {
                throw new LoginRequireClientCertificateException();
            }
        }

        return authResult;
    }

    /**
     * Change password for the given user.  This is just another method to assist in changing the password by supplying
     * a username instead of the user object. The String username should be the user requesting the change. This will
     * result in all password rules being applied and a password history update for the user.
     *
     * @param username    The username
     * @param password    The password for the user
     * @param newPassword The new password for the user
     * @return TRUE if the password was changed, false if user could not be authenticated.
     * @throws ObjectModelException  problem updating user
     * @throws IllegalStateException if user is not internal
     */
    public boolean changePassword(final String username,
                                  final String password,
                                  final String newPassword) throws LoginException, ObjectModelException {
        //try the internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();

        //find the user based on the username
        User user = null;
        for (IdentityProvider provider : providers) {
            user = provider.getUserManager().findByLogin(username);
            if (user != null) {
                try {
                    if (provider.hasClientCert(username)) {
                        throw new LoginException("Password change not permitted due to issued certificate.");
                    }
                    break;
                } catch (AuthenticationException e) {
                    logger.log(Level.WARNING, "Error processing user for password change.", e);
                    user = null;
                }
            }
        }

        //if failed to find the user from all providers, cannot change the password
        if (user == null) return false;

        //proceed to change the password
        return changePassword(user, password, newPassword);
    }

    /**
     * Change password for the given administrative user.
     *
     * @param user        The user
     * @param password    The password for the user
     * @param newPassword The new password for the user\
     * @return true if the password was changed
     * @throws InvalidPasswordException if the new password violate the password policy
     * @throws ObjectModelException     if a DB error occurred
     */
    public boolean changePassword(final User user,
                                  final String password,
                                  final String newPassword) throws ObjectModelException {
        boolean passwordUpdated = false;

        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();

        IdentityProvider identityProvider = null;
        for (IdentityProvider provider : providers) {
            if (provider.getConfig().getOid() == user.getProviderId()) {
                identityProvider = provider;
                break;
            }
        }

        if (identityProvider != null) {
            LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                    new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, user.getLogin(), password.toCharArray()), null);
            try {
                AuthenticationResult authResult = ((AuthenticatingIdentityProvider) identityProvider).authenticate(creds, true);
                User authenticatedUser = authResult == null ? null : authResult.getUser();

                if (authenticatedUser instanceof InternalUser) {
                    final InternalUser disconnectedUser = new InternalUser();
                    {   //limit scope
                        InternalUser internalUser = (InternalUser) authenticatedUser;
                        disconnectedUser.copyFrom(internalUser);
                        disconnectedUser.setVersion(internalUser.getVersion());
                    }
                    checkPerms(disconnectedUser);

                    passwordEnforcerManager.isPasswordPolicyCompliant(disconnectedUser, newPassword, password);
                    passwordEnforcerManager.setUserPasswordPolicyAttributes(disconnectedUser, false);

                    InternalUserManager userManager = ((InternalIdentityProvider) identityProvider).getUserManager();
                    final InternalUserPasswordManager passwordManager = userManager.getUserPasswordManager();
                    final String oldPassword = disconnectedUser.getHashedPassword();
                    final boolean updated = passwordManager.configureUserPasswordHashes(disconnectedUser, newPassword);
                    if (!updated) {
                        throw new IllegalStateException("User should have been updated");
                    }
                    disconnectedUser.addPasswordChange(oldPassword);
                    userManager.update(disconnectedUser);
                    passwordUpdated = true;
                } else {
                    throw new IllegalStateException("Cannot change password for user.");
                }
            } catch (AuthenticationException ae) {
                logger.info("Authentication for password change failed on " + identityProvider.getConfig().getName() + ": " + ExceptionUtils.getMessage(ae));
            }
        }

        return passwordUpdated;
    }

    /**
     * Record a successful authentication for the specified login and return a cookie that can be used
     * to resume the session from now on.
     *
     * @param authenticatedUser the principal that was successfully authenticated.  Must not be null.
     * @param sessionInfo       the additional session info such as port number, etc.
     * @return a cookie string that can be used with {@link #resumeSession(String)} later to recover the principal.  Never null or empty.
     *         Always contains at least 16 bytes of entropy.
     */
    public synchronized String createSession(final User authenticatedUser,
                                             final Object sessionInfo) {
        if (authenticatedUser == null) throw new NullPointerException();

        byte[] bytes = new byte[20];
        random.nextBytes(bytes);
        String cookie = HexUtils.encodeBase64(bytes, true);

        sessionMap.put(cookie, new SessionHolder(authenticatedUser, cookie, sessionInfo));
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Authenticated user {0}, setting admin session cookie: {1}", new String[]{authenticatedUser.getName(), cookie});
        return cookie;
    }

    /**
     * Retrieve the additional session info for a previously-authenticated user.
     *
     * @param session the session ID that was originally returned from {@link #createSession(User, Object)}.  Must not be null or empty.
     * @return the additional session info.  Return null if not found the given session.
     */
    public synchronized Object getSessionInfo(final String session) {
        if (session == null) throw new NullPointerException();
        SessionHolder holder = (SessionHolder) sessionMap.get(session);
        if (holder == null) return null;
        return holder.getSessionInfo();
    }

    /**
     * Attempt to resume a session for a previously-authenticated user.
     *
     * @param session the session ID that was originally returned from {@link #createSession(User, Object)}.  Must not be null or empty.
     * @return the user associated with this session ID, or null if the session doesn't exist or has expired.
     * @throws AuthenticationException if the session is no longer authorized
     * @throws ObjectModelException    if information required to perform session validation cannot be accessed
     */
    public synchronized User resumeSession(final String session) throws AuthenticationException, ObjectModelException {
        if (session == null) throw new NullPointerException();
        SessionHolder holder = (SessionHolder) sessionMap.get(session);
        if (holder == null) {
            logger.log(Level.WARNING, "Admin session/cookie not found: {0}.", logger.isLoggable(Level.FINER) ? session : "<not shown>");
            return null;
        }

        User user = holder.getUser();
        checkPerms(user);

        return user;
    }

    /**
     * Destroy the session with the given identifier, does nothing if the session is not found.
     *
     * @param session The session identifier
     */
    @SuppressWarnings({"unchecked"})
    public synchronized void destroySession(final String session) {
        sessionMap.remove(session);
    }

    /**
     * Check if the given session is expired.
     * <p/>
     * <p>If the session is not expired the last activity for the session is
     * optionally updated</p>
     *
     * @param session        The session identifier to check.
     * @param updateActivity True to update the last activity for the session.
     * @return True if the session is expired or not found.
     */
    public boolean isExpired(final String session,
                             final boolean updateActivity) {
        boolean expired = true;

        final SessionHolder holder = session == null ? null : (SessionHolder) sessionMap.get(session);
        if (holder != null) {
            if ((System.currentTimeMillis() - holder.getLastUsed()) <= sessionExpiryMillis.get()) {
                expired = false;

                if (updateActivity) {
                    holder.onUsed();
                }
            } else {
                logger.info("Expiring administrative session for user '" + holder.getUser().getName() + "'.");
                sessionMap.remove(session);
            }
        }

        return expired;
    }

    /**
     * Look up our internal cache for the supplied User.
     * <p/>
     * <p>If our cache is old the user is revalidated and so is all of it's associated
     * group headers.</p>
     */
    @Override
    public Set<IdentityHeader> getGroups(final User u,
                                         final boolean skipAccountValidation) throws FindException {
        Long pId = u.getProviderId();

        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();

        //find the identity provider
        Set<IdentityHeader> pSet = new HashSet<IdentityHeader>();
        for (IdentityProvider iP : providers) {
            if (iP.getConfig().getOid() == pId) {
                //Get the group memberhsip from the group cache.
                try {
                    Set<IdentityHeader> groupPrincipals = this.groupCache.getCachedValidatedGroups(u, iP, skipAccountValidation);
                    if (groupPrincipals != null) {
                        pSet.addAll(groupPrincipals);
                    }
                    //any other cache's we have for Principals we want to associate with a users subject
                    //add them here...
                } catch (ValidationException ve) {
                    throw new FindException("User validation failed.", ve);
                }

                return pSet;
            }
        }

        return Collections.emptySet();
    }

    public boolean isAdministrativeUser(final User user) throws FindException {
        return rbacServices.isAdministrativeUser(new Pair<Long, String>(user.getProviderId(), user.getId()), user);
    }

    public static void setCertRequirementWavedChecker(Functions.Nullary<Boolean> certReqWavedChecker) {
        if (certReqWavedChecker != null) {
            if (certRequirementWavedChecker != null)
                throw new IllegalStateException("Cert requirement waved checker has already been set.");
            certRequirementWavedChecker = certReqWavedChecker;
        }
    }

    //- PROTECTED

    @Override
    protected Set<IdentityProvider> getAdminIdentityProviders() {
        Set<IdentityProvider> providers;
        synchronized (providerSync) {
            if (adminProviders == null) {
                setupAdminProviders();
            }
            providers = adminProviders;
        }

        return providers;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AdminSessionManager.class.getName());
    private static final InternalFirstComparator INTERNAL_FIRST_COMPARATOR = new InternalFirstComparator();

    private static final long DEFAULT_MAX_INACTIVITY_TIME = TimeUnit.DAYS.toMillis(1L); // close after 24 hours of inactivity
    private static final long DEFAULT_SESSION_CLEANUP_INTERVAL = TimeUnit.MINUTES.toMillis(1L); // check every minute for stale sessions

    private static final long MAX_INACTIVITY_TIME = ConfigFactory.getLongProperty( "com.l7tech.server.admin.sessionExpiryAge", DEFAULT_MAX_INACTIVITY_TIME );
    private static final long SESSION_CLEANUP_INTERVAL = ConfigFactory.getLongProperty( "com.l7tech.server.admin.sessionCleanupInterval", DEFAULT_SESSION_CLEANUP_INTERVAL );

    // spring components
    private final Config config;
    private final LogonService logonService;
    private final Timer timer;
    private RbacServices rbacServices;
    private IdentityProviderFactory identityProviderFactory;
    private PasswordEnforcerManager passwordEnforcerManager;
    private final ClusterMaster clusterMaster;
    private static Functions.Nullary<Boolean> certRequirementWavedChecker;

    //
    @SuppressWarnings({"deprecation"})
    private final LRUMap sessionMap = new LRUMap(1000);
    private static final SecureRandom random = new SecureRandom();
    private final Object providerSync = new Object();
    private final GroupCache groupCache;

    private Set<IdentityProvider> adminProviders;
    private static final long DEFAULT_GATEWAY_SESSION_EXPIRY = TimeUnit.MINUTES.toMillis(30L);
    private final AtomicLong sessionExpiryMillis = new AtomicLong();

    private long loadExpiryMillis() {
        return config.getTimeUnitProperty( ServerConfigParams.PARAM_SESSION_EXPIRY, DEFAULT_GATEWAY_SESSION_EXPIRY);
    }

    {
        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                synchronized (AdminSessionManager.this) {
                    final Collection values = sessionMap.values();
                    final long now = System.currentTimeMillis();
                    for (final Iterator i = values.iterator(); i.hasNext();) {
                        final SessionHolder holder = (SessionHolder) i.next();
                        final long age = now - holder.getLastUsed();
                        if (age > sessionExpiryMillis.get()) {
                            if (logger.isLoggable(Level.INFO))
                                logger.log(Level.INFO, "Expiring administrative session for user '" + holder.getUser().getName() + "'.");
                            i.remove();
                        }
                    }
                }
            }
        }, SESSION_CLEANUP_INTERVAL * 4L, SESSION_CLEANUP_INTERVAL);
    }

    private void checkPerms(final User user) throws AuthenticationException, FindException {
        boolean hasPermission = isAdministrativeUser(user);
        if (!hasPermission) {
            throw new AuthenticationException(user.getName() +
                    " does not have privilege to access administrative services");
        }
    }

    private void setupAdminProviders() {
        try {
            // Find any non-internal providers that support admin
            SortedSet<IdentityProvider> adminProviders = new TreeSet<IdentityProvider>(INTERNAL_FIRST_COMPARATOR);

            for (IdentityProvider provider : identityProviderFactory.findAllIdentityProviders()) {
                IdentityProviderConfig config = provider.getConfig();
                if (config.isAdminEnabled()) {
                    logger.info("Enabling " + config.getName() + " for admin logins");
                    adminProviders.add(provider);
                }
            }

            synchronized (providerSync) {
                this.adminProviders = Collections.unmodifiableSet(adminProviders);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't setup admin providers", e);
        }
    }

    private Config validated(final Config config) {
        final ValidatedConfig validatedConfig = new ValidatedConfig(config, logger);
        validatedConfig.setMinimumValue( ServerConfigParams.PARAM_SESSION_EXPIRY, TimeUnit.MINUTES.toMillis(1L));
        validatedConfig.setMaximumValue( ServerConfigParams.PARAM_SESSION_EXPIRY, MAX_INACTIVITY_TIME);
        return validatedConfig;
    }


    /**
     * Within internal providers, lower-numbered providers sort first, and all internal providers sort before all
     * non-internal providers.
     */
    private static class InternalFirstComparator implements Comparator<IdentityProvider> {
        @Override
        public int compare(IdentityProvider o1, IdentityProvider o2) {
            IdentityProviderConfig config1 = o1.getConfig();
            IdentityProviderConfig config2 = o2.getConfig();
            if (config1.type() == IdentityProviderType.INTERNAL && config2.type() == IdentityProviderType.INTERNAL) {
                // Both internal, lower-numbered (i.e. -2 for "the" internal provider) comes first
                return config1.getOid() < config2.getOid() ? -1 : 1;
            } else {
                // Internal comes first
                return config1.type() == IdentityProviderType.INTERNAL ? -1 : 1;
            }
        }
    }

    private static class SessionHolder {
        private final User user;
        private final String cookie;
        private final Object sessionInfo;
        private volatile long lastUsed;

        private SessionHolder(final User user,
                              final String cookie,
                              final Object sessionInfo) {
            this.user = user;
            this.cookie = cookie;
            this.lastUsed = System.currentTimeMillis();
            this.sessionInfo = sessionInfo;
        }

        public User getUser() {
            return user;
        }

        public String getCookie() {
            return cookie;
        }

        public long getLastUsed() {
            return lastUsed;
        }

        public void onUsed() {
            lastUsed = System.currentTimeMillis();
        }

        /**
         * Get the session info, which contains port number, etc.
         *
         * @return an object of the class {@link com.l7tech.server.admin.ManagerAppletFilter.AdditionalSessionInfo}
         */
        public Object getSessionInfo() {
            return sessionInfo;
        }
    }
}
