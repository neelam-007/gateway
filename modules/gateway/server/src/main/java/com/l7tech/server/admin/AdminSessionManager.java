package com.l7tech.server.admin;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.apache.commons.collections.map.LRUMap;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.identity.*;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.security.PasswordEnforcerManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.RoleManagerIdentitySourceSupport;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.logon.LogonService;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.LoginCredentials;
import com.l7tech.policy.assertion.credential.CredentialFormat;
import com.l7tech.policy.assertion.credential.http.HttpDigest;
import com.l7tech.security.token.SecurityTokenType;
import com.l7tech.security.token.UsernamePasswordSecurityToken;

import javax.security.auth.login.LoginException;
import javax.security.auth.login.CredentialExpiredException;
import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

/**
 * This class handles authentication and tracking for admin sessions.
 *
 * <p>The cookie is used to look up the username that authenticated with it.  Anyone who can steal a cookie can
 * resume an admin session as that user; thus, the cookies must be sent over SSL, never written to disk by
 * either client or server, and not kept longer than necessary.</p>
 */
public class AdminSessionManager extends RoleManagerIdentitySourceSupport implements ApplicationListener, PropertyChangeListener {

    //- PUBLIC

    public AdminSessionManager( final ServerConfig config, final LogonService logonService ) {
        this.serverConfig = config;
        this.logonService = logonService;

        int cacheSize = config.getIntProperty(ServerConfig.PARAM_PRINCIPAL_SESSION_CACHE_SIZE, 100);
        int cacheMaxTime = config.getIntProperty(ServerConfig.PARAM_PRINCIPAL_SESSION_CACHE_MAX_TIME, 300000);
        int cacheMaxGroups = config.getIntProperty(ServerConfig.PARAM_PRINCIPAL_SESSION_CACHE_MAX_PRINCIPAL_GROUPS, 50);

        this.groupCache = new GroupCache( "PrincipalCache_unified", cacheSize, cacheMaxTime, cacheMaxGroups );
    }

    public void setRoleManager( final RoleManager roleManager ) {
        this.roleManager = roleManager;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void setPasswordEnforcerManager(final PasswordEnforcerManager passwordEnforcerManager) {
        this.passwordEnforcerManager = passwordEnforcerManager;
    }

    /**
     * Reloads IdentityProviders on change.
     */
    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if (eie.getEntityClass() == IdentityProviderConfig.class) {
                setupAdminProviders();
            }
        }
    }

    @Override
    public void propertyChange( final PropertyChangeEvent event ) {
        if ( event.getPropertyName().equals("principalSessionCacheMaxTime") &&
             event.getNewValue() != null ) {
            int cacheMaxTime = serverConfig.getIntProperty(ServerConfig.PARAM_PRINCIPAL_SESSION_CACHE_MAX_TIME, 300000);
            logger.config("Updating principal session cache max time '"+cacheMaxTime+"'.");
            groupCache.setCacheMaxTime( cacheMaxTime );
        }
    }

    /**
     * Authorize an administrative user against admin enabled identity providers.
     *
     * @param providerId The users identity provider
     * @param userId The users id
     * @return The user or null if not authenticated.
     * @throws ObjectModelException If an error occurs during authorization.
     */
    public User authorize( final long providerId, final String userId ) throws ObjectModelException {
        Set<IdentityProvider> providers = getAdminIdentityProviders();
        User user = null;

        for ( IdentityProvider provider : providers ) {
            if ( provider.getConfig().getOid() == providerId ) {
                try {
                    User authdUser = provider.getUserManager().findByPrimaryKey( userId );
                    if ( authdUser != null ) {
                        //Validate the user , now authenticated so that we know all of their group roles
                        checkPerms(authdUser);
                        logger.fine("Authorized on " + provider.getConfig().getName());

                        user = authdUser;
                    }
                } catch ( AuthenticationException ae ) {
                    logger.warning("Authorization failed for user '"+userId+"', due to '"+ExceptionUtils.getMessage(ae)+"'.");
                }

                break;
            }
        }

        return user;
    }

    /**
     * Authenticate an administrative user against admin enabled identity providers.
     *
     * @param creds The users credentials
     * @return The user or null if not authenticated.
     * @throws ObjectModelException If an error occurs during authentication.
     */
    public User authenticate( final LoginCredentials creds ) throws ObjectModelException, LoginException, FailAttemptsExceededException {
        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();
        AuthenticationResult authResult = null;
        User user = null;

        boolean needsClientCert = false;
        User providerFoundUser = null;  //temp provider container to know which provider first failed to authenticate
        for (IdentityProvider provider : providers) {
            try {
                //exit loop if the user needs a client certificate to login
                if (needsClientCert) {
                    break;
                }

                //verify if the client was assigned with a cert already and require to use it.  We only inforce this
                //for internal identity provider
                boolean useSTIG = serverConfig.getBooleanProperty("security.stig.enabled", true);
                if (useSTIG) {
                    if (creds.getFormat() == CredentialFormat.CLEARTEXT && provider.hasClientCert(creds)) {
                        needsClientCert = true;
                        throw new BadCredentialsException("User '" + creds.getLogin() + "' did not use certificate as login credentials.");
                    }
                }

                User currentUser = null;
                try {
                    currentUser = ((AuthenticatingIdentityProvider)provider).findUserByCredential( creds );
                    if ( currentUser == null ) {
                        continue;
                    }

                    logonService.hookPreLoginCheck( currentUser );
                    authResult = ((AuthenticatingIdentityProvider)provider).authenticate(creds);
                    User authdUser = authResult == null ? null : authResult.getUser();
                    if ( authdUser != null ) {
                        //Validate the user , now authenticated so that we know all of their group roles
                        checkPerms(authdUser);
                        logger.info("Authenticated on " + provider.getConfig().getName());

                        //Ensure password not expired, ignore password expire checking if have certificate
                        if (passwordEnforcerManager.isPasswordExpired(authdUser) && !provider.hasClientCert(creds)) {
                            throw new CredentialExpiredException("Password expired.");
                        }

                        user = authdUser;
                        break;
                    }
                } catch ( BadCredentialsException bce ) {
                    //we have found our first username with bad credentials, so we'll update the logon attempt
                    //on this particular one and not on preceeding ones
                    if ( providerFoundUser == null  ) {
                        providerFoundUser = currentUser;
                    }
                }
            } catch (AuthenticationException e) {
                logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
                if (ExceptionUtils.causedBy(e, FailAttemptsExceededException.class)) {
                    throw new FailAttemptsExceededException(e.getMessage());
                }
            }
        }

        if (user != null) {
            logonService.updateLogonAttempt( user, authResult );
        } else {
            //if we have failed to authenticate this user from all provider, we'll need to update failed logon attempt
            //for the first provider that found user with the same username
            if (providerFoundUser != null) {
                logonService.updateLogonAttempt( providerFoundUser, null );
            }
            if (needsClientCert) {
                throw new LoginRequireClientCertificateException();
            }
        }

        return user;
    }

    /**
     * Change password for the given user.  This is just another method to assist in changing the password by supplying
     * a username instead of the user object.
     *  
     * @param username  The username
     * @param password  The password for the user
     * @param newPassword   The new password for the user
     * @return  TRUE if the password was changed
     * @throws ObjectModelException 
     */
    public boolean changePassword(final String username, final String password, final String newPassword) throws ObjectModelException {
        //try the internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();

        //find the user based on the username
        User user = null;
        for (IdentityProvider provider : providers) {
            user = provider.getUserManager().findByLogin(username);
            if (user != null) break;
        }

        //if failed to find the user from all providers, cannot change the password
        if(user == null) return false;

        //proceed to change the password
        return changePassword(user, password, newPassword);
    }

    /**
     * Change password for the given user.
     *
     * @param user The user
     * @param password The password for the user
     * @param newPassword The new password for the user\
     * @return true if the password was changed
     * @throws InvalidPasswordException if the new password violate the password policy
     * @throws ObjectModelException if a DB error occurred
     */
    public boolean changePassword( final User user,
                                   final String password,
                                   final String newPassword ) throws ObjectModelException {
        boolean passwordUpdated = false;

        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();

        IdentityProvider identityProvider = null;
        for (IdentityProvider provider : providers) {
            if ( provider.getConfig().getOid() == user.getProviderId() ) {
                identityProvider = provider;
                break;
            }
        }

        if ( identityProvider != null ) {
            LoginCredentials creds = LoginCredentials.makeLoginCredentials(
                    new UsernamePasswordSecurityToken(SecurityTokenType.UNKNOWN, user.getLogin(), password.toCharArray()) , null);
            try {
                AuthenticationResult authResult = ((AuthenticatingIdentityProvider)identityProvider).authenticate(creds);
                User authenticatedUser = authResult == null ? null : authResult.getUser();

                if ( authenticatedUser instanceof InternalUser ) {
                    InternalUser internalUser = (InternalUser) authenticatedUser;
                    checkPerms(internalUser);
                    passwordEnforcerManager.isSTIGCompilance(internalUser, newPassword, HexUtils.encodePasswd(internalUser.getLogin(), newPassword, HttpDigest.REALM), password);
                    internalUser.setPasswordChanges(System.currentTimeMillis(), newPassword);
                    internalUser.setPasswordExpiry(passwordEnforcerManager.getSTIGExpiryPasswordDate(System.currentTimeMillis()));
                    ((InternalIdentityProvider)identityProvider).getUserManager().update(internalUser);
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
     * @param authenticatedUser  the principal that was successfully authenticated.  Must not be null.
     * @param sessionInfo the additional session info such as port number, etc.
     * @return a cookie string that can be used with {@link #resumeSession} later to recover the principal.  Never null or empty.
     *         Always contains at least 16 bytes of entropy.
     */
    public synchronized String createSession( final User authenticatedUser, final Object sessionInfo ) {
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
     * @param session the session ID that was originally returned from {@link #createSession}.  Must not be null or empty.
     * @return the additional session info.  Return null if not found the given session.
     */
    public synchronized Object getSessionInfo( final String session ) {
        if (session == null) throw new NullPointerException();
        SessionHolder holder = (SessionHolder)sessionMap.get(session);
        if (holder == null) return null;
        return holder.getSessionInfo();
    }

    /**
     * Attempt to resume a session for a previously-authenticated user.
     *
     * @param session  the session ID that was originally returned from {@link #createSession}.  Must not be null or empty.
     * @return the user associated with this session ID, or null if the session doesn't exist or has expired.
     * @throws AuthenticationException if the session is no longer authorized
     * @throws ObjectModelException if information required to perform session validation cannot be accessed
     */
    public synchronized User resumeSession( final String session ) throws AuthenticationException, ObjectModelException {
        if (session == null) throw new NullPointerException();
        SessionHolder holder = (SessionHolder)sessionMap.get(session);
        if (holder == null) {
            logger.log(Level.WARNING, "Admin session/cookie not found: {0}.", logger.isLoggable(Level.FINER) ? session : "<not shown>");
            return null;
        }
        holder.onUsed();

        User user = holder.getUser();
        checkPerms( user );

        return user;
    }

    /**
     * Attempt to destroy a session for a previously-authenticated user.  Takes no action if the
     * specified session does not exist.
     *
     * @param user the user that was originally passed to {@link #createSession}.
     */
    @SuppressWarnings({"unchecked"})
    public synchronized void destroySession( final User user ) {
        boolean destroyed = false;
        for (Iterator<SessionHolder> iter = sessionMap.values().iterator(); iter.hasNext();) {
            SessionHolder holder = iter.next();
            // Object equality, not login name! (one user can have many sessions)
            if (holder.getUser() == user) {
                if ( !destroyed ) {
                    destroyed = true;
                    iter.remove();
                } else {
                    logger.warning("Admin session destroy matched multiple sessions for principal '"+user.getLogin()+"'.");
                }
            }
        }

        if ( !destroyed ) {
            logger.warning("Admin session not found for principal '"+user.getLogin()+"'.");
        }
    }

    /**
     * Look up our internal cache for the supplied User.
     *
     * <p>If our cache is old the user is revalidated and so is all of it's associated
     * group headers.</p>
     */
    @Override
    public Set<IdentityHeader> getGroups(final User u, boolean skipAccountValidation) throws FindException {
        Long pId = u.getProviderId();

        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers = getAdminIdentityProviders();

        //find the identity provider
        Set<IdentityHeader> pSet = new HashSet<IdentityHeader>();
        for(IdentityProvider iP : providers){
            if(iP.getConfig().getOid() == pId){
                //Get the group memberhsip from the group cache.
                try {
                    Set<IdentityHeader> groupPrincipals = this.groupCache.getCachedValidatedGroups(u,iP, skipAccountValidation);
                    if(groupPrincipals != null){
                        pSet.addAll(groupPrincipals);
                    }
                    //any other cache's we have for Principals we want to associate with a users subject
                    //add them here...
                } catch ( ValidationException ve ) {
                    throw new FindException( "User validation failed.", ve );
                }

                return pSet;
            }
        }

        return Collections.emptySet();
    }

    @Override
    protected Set<IdentityProvider> getAdminIdentityProviders(){
        Set<IdentityProvider> providers;
        synchronized( providerSync ) {
            if ( adminProviders == null ) {
                setupAdminProviders();
            }
            providers = adminProviders;
        }

        return providers;
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AdminSessionManager.class.getName());
    private static final InternalFirstComparator INTERNAL_FIRST_COMPARATOR = new InternalFirstComparator();

    private static final long REAP_DELAY = TimeUnit.MINUTES.toMillis(20); // Check every 20 min for stale sessions
    private static final long REAP_STALE_AGE = TimeUnit.DAYS.toMillis(1); // reap sessions after 24 hours of inactivity

    // spring components
    private final ServerConfig serverConfig;
    private final LogonService logonService;
    private IdentityProviderFactory identityProviderFactory;
    private PasswordEnforcerManager passwordEnforcerManager;

    //
    @SuppressWarnings({"deprecation"})
    private final LRUMap sessionMap = new LRUMap(1000);
    private final SecureRandom random = new SecureRandom();
    private final Object providerSync = new Object();
    private final GroupCache groupCache;

    private Set<IdentityProvider> adminProviders;

    {
        Background.scheduleRepeated(new TimerTask() {
            @Override
            public void run() {
                synchronized (AdminSessionManager.this) {
                    Collection values = sessionMap.values();
                    long now = System.currentTimeMillis();
                    for (Iterator i = values.iterator(); i.hasNext();) {
                        SessionHolder holder = (SessionHolder)i.next();
                        long age = now - holder.getLastUsed();
                        if (age > REAP_STALE_AGE) {
                            if (logger.isLoggable(Level.INFO))
                                logger.log(Level.INFO, "Removing stale admin session: " + holder.getUser().getName());
                            i.remove();
                        }
                    }
                }
            }
        }, REAP_DELAY, REAP_DELAY);
    }

    private void checkPerms(final User user) throws AuthenticationException, FindException {
        boolean hasPermission = false;
        // TODO is holding any CRUD permission sufficient?
        Collection<Role> roles = roleManager.getAssignedRoles(user);
        roles: for (Role role : roles) {
            for (Permission perm : role.getPermissions()) {
                if (perm.getEntityType() != null && perm.getOperation() != OperationType.NONE) {
                    hasPermission = true;
                    break roles;
                }
            }
        }

        if ( !hasPermission ) {
            throw new AuthenticationException( user.getName() +
                    " does not have privilege to access administrative services" );
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

            synchronized(providerSync) {
                this.adminProviders = Collections.unmodifiableSet(adminProviders);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Couldn't setup admin providers", e);
        }
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
        private long lastUsed;

        public SessionHolder( User user, String cookie, Object sessionInfo) {
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
         * @return an object of the class {@link com.l7tech.server.admin.ManagerAppletFilter.AdditionalSessionInfo}
         */
        public Object getSessionInfo() {
            return sessionInfo;
        }
    }
}
