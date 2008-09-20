package com.l7tech.server.admin;

import org.apache.commons.collections.LRUMap;
import org.springframework.context.ApplicationEvent;
import org.springframework.beans.factory.InitializingBean;
import com.l7tech.util.HexUtils;
import com.l7tech.util.Background;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.TimeUnit;
import com.l7tech.identity.ValidationException;
import com.l7tech.identity.User;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.AuthenticationException;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.gateway.common.security.rbac.Role;
import com.l7tech.gateway.common.security.rbac.Permission;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.RoleAssignment;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.identity.AuthenticationResult;
import com.l7tech.server.identity.AuthenticatingIdentityProvider;
import com.l7tech.server.identity.internal.InternalIdentityProvider;
import com.l7tech.server.identity.internal.InternalGroupManager;
import com.l7tech.server.identity.internal.InternalUserManager;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.server.security.rbac.RoleManagerIdentitySource;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.InvalidPasswordException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.policy.assertion.credential.LoginCredentials;

import java.security.SecureRandom;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class handles authentication and tracking for admin sessions.
 *
 * <p>The cookie is used to look up the username that authenticated with it.  Anyone who can steal a cookie can
 * resume an admin session as that user; thus, the cookies must be sent over SSL, never written to disk by
 * either client or server, and not kept longer than necessary.</p>
 */
public class AdminSessionManager implements InitializingBean, RoleManagerIdentitySource {

    //- PUBLIC

    public AdminSessionManager( final ServerConfig config ) {
        this.groupCache = new GroupCache( "PrincipalCache_unified", config );
    }

    public void setRoleManager( final RoleManager roleManager ) {
        this.roleManager = roleManager;
    }

    public void setIdentityProviderFactory(IdentityProviderFactory identityProviderFactory) {
        this.identityProviderFactory = identityProviderFactory;
    }

    public void afterPropertiesSet() throws Exception {
        setupAdminProviders();
    }

    /**
     * Reloads IdentityProviders on change.
     */
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof EntityInvalidationEvent) {
            EntityInvalidationEvent eie = (EntityInvalidationEvent) event;
            if (eie.getEntityClass() == IdentityProviderConfig.class) {
                try {
                    setupAdminProviders();
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Couldn't setup admin providers", e);
                }
            }
        }
    }

    /**
     * Authenticate an administrative user against admin enabled identity providers.
     *
     * @param creds The users credentials
     * @return The user or null if not authenticated.
     * @throws ObjectModelException If an error occurs during authentication.
     */
    public User authenticate( final LoginCredentials creds ) throws ObjectModelException {
        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers;
        synchronized(providerSync) {
            providers = adminProviders;
        }
        User user = null;

        for (IdentityProvider provider : providers) {
            try {
                AuthenticationResult authResult = ((AuthenticatingIdentityProvider)provider).authenticate(creds);
                User authdUser = authResult == null ? null : authResult.getUser();
                if ( authdUser != null ) {
                    //Validate the user , now authenticated so that we know all of their group roles
                    checkPerms(authdUser);
                    logger.info("Authenticated on " + provider.getConfig().getName());
                    user = authdUser;
                    break;
                }
            } catch (AuthenticationException e) {
                logger.info("Authentication failed on " + provider.getConfig().getName() + ": " + ExceptionUtils.getMessage(e));
            }
        }

        return user;
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
        Set<IdentityProvider> providers;
        synchronized(providerSync) {
            providers = adminProviders;
        }

        IdentityProvider identityProvider = null;
        for (IdentityProvider provider : providers) {
            if ( provider.getConfig().getOid() == user.getProviderId() ) {
                identityProvider = provider;
                break;
            }
        }

        if ( identityProvider != null ) {
            LoginCredentials creds = new LoginCredentials(user.getLogin(), password.toCharArray() , null);
            try {
                AuthenticationResult authResult = ((AuthenticatingIdentityProvider)identityProvider).authenticate(creds);
                User authenticatedUser = authResult == null ? null : authResult.getUser();

                if ( authenticatedUser instanceof InternalUser ) {
                    InternalUser internalUser = (InternalUser) authenticatedUser;
                    internalUser.setCleartextPassword(newPassword);
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
        return cookie;
    }

    /**
     * Retrieve the additional session info for a previously-authenticated user.
     * @param session the session ID that was originally returned from {@link #createSession}.  Must not be null or empty.
     * @return the additional session info.  Return null if not found the given session.
     */
    public Object getSessionInfo( final String session ) {
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
        if (holder == null) return null;
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
    public Set<IdentityHeader> getGroups( final User u ) throws FindException {
        Long pId = u.getProviderId();

        // Try internal first (internal accounts with the same credentials should hide externals)
        Set<IdentityProvider> providers;
        synchronized(providerSync) {
            providers = adminProviders;
        }

        //find the identity provider
        Set<IdentityHeader> pSet = new HashSet<IdentityHeader>();
        for(IdentityProvider iP : providers){
            if(iP.getConfig().getOid() == pId){
                //Get the group memberhsip from the group cache.
                try {
                    Set<IdentityHeader> groupPrincipals = this.groupCache.getCachedValidatedGroups(u,iP);
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

    /**
     * Ensure that the current (perhaps not yet committed) role assignments are valid.
     *
     * <p>This check ensures that:</p>
     *
     * <li>
     *   <ul>There is an administrative assignement to at least one internal user or group.</ul>
     *   <ul>If a group is assigned that there is at least one user in that group.</ul>
     *   <ul>That the user that is assigned is not an expired account.</ul>
     * </li>
     *
     * @throws UpdateException If there is a problem with assignments or an error while checking.
     */
    public void validateRoleAssignments() throws UpdateException {
        try {
            // Try internal first (internal accounts with the same credentials should hide externals)
            Set<IdentityProvider> providers;
            synchronized( providerSync ) {
                providers = adminProviders;
            }

            boolean found = false;
            IdentityProvider provider = !providers.isEmpty() ? providers.iterator().next() : null;
            if ( provider instanceof InternalIdentityProvider ) {
                InternalIdentityProvider internalProvider = (InternalIdentityProvider) provider;
                InternalUserManager userManager = internalProvider.getUserManager();
                InternalGroupManager groupManager = internalProvider.getGroupManager();
                long expiryMinTime = System.currentTimeMillis() + TimeUnit.HOURS.toMillis(1); // good for an hour at least

                Role adminRole = roleManager.findByPrimaryKey( Role.ADMIN_ROLE_OID );
                if ( adminRole != null ) {
                    Set<String> checkedUserOids = new HashSet<String>();

                    // Check for user assignment first
                    for ( RoleAssignment assignment : adminRole.getRoleAssignments() ) {
                        if ( assignment.getProviderId()==internalProvider.getConfig().getOid() ) {
                            if ( EntityType.USER.getName().equals(assignment.getEntityType()) ) {
                                InternalUser user = userManager.findByPrimaryKey( assignment.getIdentityId() );
                                if ( user != null && (user.getExpiration()<0 || user.getExpiration() < expiryMinTime)) {
                                    found = true;
                                    break;
                                } else {
                                    checkedUserOids.add( assignment.getIdentityId() );
                                }
                            }
                        }
                    }

                    if ( !found ) {
                        // Check group assignments
                        for ( RoleAssignment assignment : adminRole.getRoleAssignments() ) {
                            if ( assignment.getProviderId()==internalProvider.getConfig().getOid() ) {
                                if ( EntityType.GROUP.getName().equals(assignment.getEntityType()) ) {
                                    Set<IdentityHeader> users = groupManager.getUserHeaders( assignment.getIdentityId() );
                                    for ( IdentityHeader userHeader : users ) {
                                        if ( checkedUserOids.contains( userHeader.getStrId() ) ) continue;

                                        InternalUser user = userManager.findByPrimaryKey( userHeader.getStrId() );
                                        if ( user != null && (user.getExpiration()<0 || user.getExpiration() < expiryMinTime)) {
                                            found = true;
                                            break;
                                        } else {
                                            checkedUserOids.add( assignment.getIdentityId() );
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if ( !found ) {
                        throw new UpdateException( "At least one internal user must be assigned to the administrative role (can be via group assignment)." );
                    }
                }
            }
        } catch ( FindException fe ) {
            throw new UpdateException( "Error checking role assignements.", fe );
        }
    }

    //- PRIVATE

    private static final Logger logger = Logger.getLogger(AdminSessionManager.class.getName());
    private static final InternalFirstComparator INTERNAL_FIRST_COMPARATOR = new InternalFirstComparator();

    private static final long REAP_DELAY = TimeUnit.MINUTES.toMillis(20); // Check every 20 min for stale sessions
    private static final long REAP_STALE_AGE = TimeUnit.DAYS.toMillis(1); // reap sessions after 24 hours of inactivity

    // spring components
    private IdentityProviderFactory identityProviderFactory;
    private RoleManager roleManager;

    //
    @SuppressWarnings({"deprecation"})
    private final LRUMap sessionMap = new LRUMap(1000);
    private final SecureRandom random = new SecureRandom();
    private final Object providerSync = new Object();
    private final GroupCache groupCache;

    private Set<IdentityProvider> adminProviders;

    {
        Background.scheduleRepeated(new TimerTask() {
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

    private void setupAdminProviders() throws FindException {
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
    }

    /**
     * Within internal providers, lower-numbered providers sort first, and all internal providers sort before all
     * non-internal providers.
     */
    private static class InternalFirstComparator implements Comparator<IdentityProvider> {
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
