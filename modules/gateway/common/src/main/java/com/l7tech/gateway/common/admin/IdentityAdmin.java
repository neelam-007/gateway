package com.l7tech.gateway.common.admin;

import com.l7tech.gateway.common.security.rbac.RbacAdmin;
import com.l7tech.gateway.common.security.rbac.Secured;
import com.l7tech.identity.*;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;
import org.jetbrains.annotations.NotNull;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Map;
import java.util.Set;

import static com.l7tech.gateway.common.security.rbac.MethodStereotype.*;
import static com.l7tech.objectmodel.EntityType.*;

/**
 * Provides a remote interface for creating, searching, and retrieving identity providers, identity provider
 * configurations (including LDAP templates), and actual identities (users and groups).
 *
 * @see com.l7tech.identity.IdentityProviderConfig
 * @see EntityHeader
 */
@Transactional(propagation = Propagation.REQUIRED, rollbackFor = Throwable.class)
@Secured
@Administrative
public interface IdentityAdmin {
    String ROLE_NAME_TYPE_SUFFIX = "Identity Provider";
    String ROLE_NAME_PATTERN = RbacAdmin.ROLE_NAME_PREFIX + " {0} " + ROLE_NAME_TYPE_SUFFIX + RbacAdmin.ROLE_NAME_OID_SUFFIX;

    /**
     * Retrieve the server admin protocol version string.
     *
     * @return the server admin protocol version string, ie "20040603".  Never null
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    @Administrative(licensed = false)
    String echoVersion();

    /**
     * Retrieve all available identity provider configurations.  Every {@link com.l7tech.identity.IdentityProvider} has one and only one
     * {@link com.l7tech.identity.IdentityProviderConfig}, and each identity provider configuration belongs to one and only one
     * identity provider.
     *
     * @return array of entity headers for all existing ID provider config.  May be empty but never null.
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(readOnly = true)
    @Secured(types = ID_PROVIDER_CONFIG, stereotype = FIND_HEADERS)
    @Administrative(licensed = false)
    EntityHeader[] findAllIdentityProviderConfig() throws FindException;

    /**
     * Retrieve a specified identity provider configuration given its object ID.
     * Every {@link com.l7tech.identity.IdentityProvider} has one and only one
     * {@link com.l7tech.identity.IdentityProviderConfig}, and each identity provider configuration belongs to one and only one
     * identity provider.
     *
     * @param oid the unique object identifier of the {@link com.l7tech.identity.IdentityProviderConfig} to obtain
     * @return the requested {@link com.l7tech.identity.IdentityProviderConfig}, or null if it was not found.
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(readOnly = true)
    @Secured(types = ID_PROVIDER_CONFIG, stereotype = FIND_ENTITY)
    @Administrative(licensed = false)
    IdentityProviderConfig findIdentityProviderConfigByID(long oid) throws FindException;

    /**
     * Retrieve all available LDAP templates.  An LDAP template is a preconfigured set of LDAP properties for
     * various common LDAP implementations such as Microsoft Active Directory or Netscape LDAP.
     *
     * @return an array of LDAP identity provider configurations.  May be empty, but never null.
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(propagation = Propagation.SUPPORTS)
    @Administrative(licensed = false)
    LdapIdentityProviderConfig[] getLdapTemplates() throws FindException;

    /**
     * Store the specified new or existing identity provider configuration.  If the specified
     * {@link IdentityProviderConfig} contains a unique object ID that already exists, this will replace
     * the object'  s current configuration with the new configuration.  Otherwise, a new object will be created.
     *
     * @param cfg the identity provider configuration to create or update.  Must not be null.
     * @return the unique id of the object that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     */
    @Secured(types = ID_PROVIDER_CONFIG, stereotype = SAVE_OR_UPDATE)
    long saveIdentityProviderConfig(IdentityProviderConfig cfg)
            throws SaveException, UpdateException;

    /**
     * Delete the identity provider configuration specified by its unique object ID.
     *
     * @param oid the unique object ID of an already-existing {@link IdentityProviderConfig}
     * @throws DeleteException if the requested information could not be deleted
     * @throws DeleteException if the requested object ID did not exist
     */
    @Secured(types = ID_PROVIDER_CONFIG, stereotype = DELETE_BY_ID)
    void deleteIdentityProviderConfig(long oid) throws DeleteException;

    /**
     * Retrieve all available {@link com.l7tech.identity.User}s for a given {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} whose users to load.
     * @return an {@link com.l7tech.objectmodel.EntityHeaderSet}s with the headers for all
     *         {@link com.l7tech.identity.User}s within this {@link IdentityProviderConfig}.
     *         May be empty but never null.
     * @throws FindException if the specified object ID did not exist or was not an identity provider config
     * @throws FindException if there was a problem accessing the requested information
     */
    @Transactional(readOnly = true)
    @Secured(types = USER, stereotype = FIND_HEADERS)
    EntityHeaderSet<IdentityHeader> findAllUsers(long idProvCfgId) throws FindException;

    /**
     * Search for {@link com.l7tech.identity.Identity}s matching a pattern within the specified {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} whose users to load.
     * @param types       list of {@link EntityType}s to include in the results.  Must not be null or empty.
     * @param pattern     the search pattern to match against entity names.
     *                    This is a String that may contain the metacharacters
     *                    "*" or "?" to match zero or more arbirary characters, or exactly one character,
     *                    respectively.  The match is always case-insensitive for ASCII characters.
     *                    <p/>
     *                    Examples:  <ul><li>pattern "ike" will match user "ike" or "Ike" but not users "mike" or "ike22";</li>
     *                    <li>pattern "ike*" will match "ike", "Ike" and "ike22" but not "mike";</li>
     *                    <li>pattern "i?e*" will match "ike", "Ike", "ice" and "ike22" but not "eke" or "ikke"</li></ul>
     * @return an {@link com.l7tech.objectmodel.EntityHeaderSet}s with the  {@link com.l7tech.objectmodel.IdentityHeader}s
     *         for all the {@link com.l7tech.identity.User}s matching the specified requirements.
     * @throws FindException if there was a problem accessing the requested information.
     */
    @Transactional(readOnly = true)
    @Secured(types = {USER, GROUP}, stereotype = FIND_HEADERS)
    EntityHeaderSet<IdentityHeader> searchIdentities(long idProvCfgId, EntityType[] types, String pattern)
            throws FindException;

    /**
     * Search for a {@link com.l7tech.identity.User} by its user ID within an {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} in which to search.
     * @param userId      the unique identifier of the user within this {@link IdentityProviderConfig}.  Must not be empty or null.
     * @return the requested {@link com.l7tech.identity.User}, or null if no user with that user ID was found in this ID provider
     * @throws FindException if there was a problem accessing the requested information
     */
    @Transactional(readOnly = true)
    @Secured(types = USER, stereotype = FIND_ENTITY, relevantArg = 1)
    User findUserByID(long idProvCfgId, String userId)
            throws FindException;

    /**
     * Search for a {@link User} by its login name.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} in which to search.
     * @param login       The user login.  See {@link User#getLogin}
     * @return the requested {@link User}, or null if no user with that login was found in this ID provider
     * @throws FindException if there was a problem accessing the requested information
     */
    @Transactional(readOnly = true)
    @Secured(types = USER, stereotype = FIND_ENTITY)
    User findUserByLogin(long idProvCfgId, String login)
            throws FindException;

    /**
     * Delete a {@link User} by its unique identifier.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} in which to search
     * @param userId      the unique identifier of the {@link User} to delete.  Must not be null or empty
     * @throws DeleteException         if the requested information could not be deleted
     * @throws ObjectNotFoundException if no user with the specified unique identifier exists in the specified
     *                                 identity provider
     */
    @Secured(types = USER, stereotype = DELETE_IDENTITY_BY_ID, relevantArg = 1)
    void deleteUser(long idProvCfgId, String userId)
            throws DeleteException, ObjectNotFoundException;

    /**
     * Store the specified new or updated {@link User} object.  If the specified
     * {@link User} contains a unique identifier that already exists, this will replace
     * the objects current state with the new state.  Otherwise, a new object will be created.
     * <p/>
     * Warning: this method cannot be used to change a users password. Any attempts to do so will be rejected.
     *
     * @param idProvCfgId       the {@link IdentityProviderConfig} in which the {@link User} already exists, or in which it
     *                          should be created.  Must not be null
     * @param user              the new or updated {@link User} instance.  Must not be null.  If this user contains
     *                          a null unique identifier, this call will create a new {@link User} instance.  Otherwise,
     *                          if this user contains a unique identifier, the specified {@link User}'s state will be
     *                          replaced with the data from user.
     * @param groupHeaders      the set of {@link IdentityHeader}s corresponding to the {@link com.l7tech.identity.Group}s to which this
     *                          {@link User} should belong when this call completes.
     *                          May be empty but not null.
     *                          If the user already exists, any existing group memberships not included in this set will
     *                          be severed.
     * @param clearTextPassword if not null, then the user is being created. If this is not true an exception will be thrown.
     * @return the unique identifier that was updated or created.
     * @throws SaveException           if the requested information could not be saved
     * @throws UpdateException         if the requested information could not be updated
     * @throws ObjectNotFoundException if the user contained a non-null unique identifier, but no {@link User} with this
     *                                 unique identifier exists in the specified identity provider.
     * @throws com.l7tech.objectmodel.InvalidPasswordException
     *                                 if clearTextPassword does not meet the password requirements
     */
    @Secured(types = USER, stereotype = SAVE_OR_UPDATE, relevantArg = 1)
    String saveUser(long idProvCfgId, User user, Set<IdentityHeader> groupHeaders, String clearTextPassword)
            throws SaveException, UpdateException, ObjectNotFoundException, InvalidPasswordException;

    /**
     * See {@link IdentityAdmin#saveUser(long, com.l7tech.identity.User, java.util.Set, String)}. Delegates with a value
     * of null for clearTextPassword. This method cannot be used to create a new Internal User.
     */
    @Secured(types = USER, stereotype = SAVE_OR_UPDATE, relevantArg = 1)
    String saveUser(long idProvCfgId, User user, Set<IdentityHeader> groupHeaders)
            throws SaveException, UpdateException, ObjectNotFoundException;

    /**
     * Change a users password.
     *
     * @param user                 user to set password for. Any changes on this User entity are ignored
     * @param newClearTextPassword users clear text password
     * @throws FindException            any problems finding user
     * @throws UpdateException          any problems updating user
     * @throws InvalidPasswordException if users new password does not meet the password requirements for the internal
     *                                  identity provider.
     */
    @Secured(types = USER, stereotype = SAVE_OR_UPDATE, relevantArg = 0)
    void changeUsersPassword(User user, String newClearTextPassword)
            throws FindException, UpdateException, InvalidPasswordException;

    /**
     * Retrieve all available {@link com.l7tech.identity.Group}s for a given {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} whose groups to load.
     * @return an {@link com.l7tech.objectmodel.EntityHeaderSet} with the {@link IdentityHeader}s
     *         for all {@link com.l7tech.identity.Group}s within this {@link IdentityProviderConfig}.
     *         May be empty but never null.
     * @throws FindException if the specified object ID did not exist or was not an identity provider config
     * @throws FindException if there was a problem accessing the requested information
     */
    @Transactional(readOnly = true)
    @Secured(types = GROUP, stereotype = FIND_HEADERS)
    EntityHeaderSet<IdentityHeader> findAllGroups(long idProvCfgId) throws FindException;

    /**
     * Search for a {@link com.l7tech.identity.Group} by its user ID within an {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} in which to search.
     * @param groupId     the unique identifier of the {@link com.l7tech.identity.Group} within this {@link IdentityProviderConfig}.
     *                    Must not be empty or null.
     * @return the requested {@link com.l7tech.identity.Group}, or null if no group with that unique identifier was found in this ID provider
     * @throws FindException if there was a problem accessing the requested information
     */
    @Transactional(readOnly = true)
    @Secured(types = GROUP, stereotype = FIND_ENTITY, relevantArg = 1)
    Group findGroupByID(long idProvCfgId, String groupId)
            throws FindException;

    /**
     * Search for a {@link Group} by its name.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} in which to search.
     * @param name        The group name.  See {@link Group#getName}
     * @return the requested {@link Group}, or null if no group with that name was found in this ID provider
     * @throws FindException if there was a problem accessing the requested information
     */
    @Transactional(readOnly = true)
    @Secured(types = GROUP, stereotype = FIND_ENTITY)
    Group findGroupByName(long idProvCfgId, String name)
            throws FindException;

    /**
     * Delete a {@link Group} by its unique identifier.
     *
     * @param idProvCfgId the unique object ID of the {@link IdentityProviderConfig} in which to search
     * @param groupId     the unique identifier of the {@link Group} to delete.  Must not be null or empty
     * @throws DeleteException         if the requested information could not be deleted
     * @throws ObjectNotFoundException if no {@link Group} with the specified unique identifier exists in the specified
     *                                 identity provider
     */
    @Secured(types = GROUP, stereotype = DELETE_IDENTITY_BY_ID, relevantArg = 1)
    void deleteGroup(long idProvCfgId, String groupId)
            throws DeleteException, ObjectNotFoundException;

    /**
     * Store the specified new or updated {@link Group} object.  If the specified
     * {@link Group} contains a unique identifier that already exists, this will replace
     * the objects current state with the new state.  Otherwise, a new object will be created.
     *
     * @param idProvCfgId the object ID of the {@link IdentityProviderConfig} in which the {@link Group} already exists,
     *                    or in which it should be created.  Must not be null
     * @param group       the new or updated {@link Group} instance.  Must not be null.  If this group contains
     *                    a null unique identifier, this call will create a new {@link Group} instance.  Otherwise,
     *                    if this group contains a unique identifier, the specified {@link Group}'s state will be
     *                    replaced with the data from group.
     * @param userHeaders the set of {@link IdentityHeader}s corresponding to the {@link User}s which should belong
     *                    to this {@link Group} when this call completes.
     *                    May be empty but not null.
     *                    If the group already exists, any existing group memberships not included in this set will
     *                    be severed.
     * @return the unique identifier that was updated or created.
     * @throws SaveException           if the requested information could not be saved
     * @throws UpdateException         if the requested information could not be updated
     * @throws ObjectNotFoundException if the group contained a non-null unique identifier, but no {@link Group} with this
     *                                 unique identifier exists in the specified identity provider.
     */
    @Secured(types = GROUP, stereotype = SAVE_OR_UPDATE, relevantArg = 1)
    String saveGroup(long idProvCfgId, Group group, Set<IdentityHeader> userHeaders)
            throws SaveException, UpdateException, ObjectNotFoundException;

    /**
     * Get a user's X.509 certificate as Base64-encoded ASN.1 DER.
     *
     * @param user the {@link User} whose certificate is to be retrieved.  Must not be null.  Must be an
     *             already-existing {@link User} instance obtained from {@link #findUserByID(long, String)} or
     *             {@link #findUserByLogin(long, String)}.
     * @return the user's X.509 certificate DER encoded and then converted to Base64, or null if the user did not
     *         have one.
     * @throws CertificateEncodingException if an encoding error occurs producing the DER
     * @throws FindException                if the requested information could not be accessed
     */
    @Transactional(readOnly = true)
    @Secured(types = USER, stereotype = GET_PROPERTY_OF_ENTITY)
    String getUserCert(User user) throws FindException, CertificateEncodingException;

    /**
     * Check if the currently logged on user has a client cert. Used to warn the user when they go to
     * change their password.
     *
     * @return true if the user has a cert.
     * @throws FindException if problem searching for users certificate.
     * @throws com.l7tech.identity.AuthenticationException
     *                       if current user cannot be found.
     */
    @Transactional(readOnly = true)
    @Administrative(licensed = false)
    boolean doesCurrentUserHaveCert() throws FindException, AuthenticationException;

    /**
     * @return true if the current user's password can be changed.
     * @throws com.l7tech.objectmodel.FindException
     *          if problem looking up users identity provider.
     * @throws com.l7tech.identity.AuthenticationException
     *          if current user cannot be found.
     */
    @Transactional(readOnly = true)
    @Administrative(licensed = false)
    boolean currentUsersPasswordCanBeChanged() throws AuthenticationException, FindException;

    /**
     * Revoke a user's X.509 certificate and lock the password where possible.  This removes any (possibly-shadowed)
     * X.509 certificate that is being stored for the specified {@link User}.  If the user is an internal user,
     * this will also change the user's password to a string of 32 random bytes, effective locking out the
     * account until a new password is assigned.  This is to prevent a holder of a stolen password from simply
     * applying for a new certificate on behalf of the user in question.
     *
     * @param user the user whose certificate should be revoked and password locked, if possible.  May not be null.
     *             Must be an already-existing {@link User} instance obtained from
     *             {@link #findUserByID(long, String)} or {@link #findUserByLogin(long, String)}.
     * @throws ObjectNotFoundException if the specified user does not exist
     * @throws UpdateException         if there was a problem updating the database
     */
    @Secured(types = USER, stereotype = SET_PROPERTY_OF_ENTITY)
    void revokeCert(User user) throws UpdateException, ObjectNotFoundException;

    /**
     * Revoke all certificates.
     *
     * @return the number of certificates that were revoked
     * @throws UpdateException
     */
    @Secured(types = USER, stereotype = DELETE_MULTI)
    int revokeCertificates() throws UpdateException;

    /**
     * Store a certificate for a user who does not yet have one.
     *
     * @param user the {@link User} who is to be assigned a certificate.  Must not be null.
     *             Must be an already-existing {@link User} instance obtained from
     *             {@link #findUserByID(long, String)} or {@link #findUserByLogin(long, String)}.
     * @param cert the certificate to save.  Must not be null.
     * @throws UpdateException if user was not in a state that allows the creation of a certificate
     *                         (already has a cert; has tried too many times to create a cert; ID provider is
     *                         not able to store user certs; or some other problem)
     * @throws UpdateException if there was a problem updating the database
     */
    @Secured(types = USER, stereotype = SET_PROPERTY_OF_ENTITY, relevantArg = 0)
    void recordNewUserCert(User user, Certificate cert) throws UpdateException;

    /**
     * Test if the prospective {@link IdentityProviderConfig} would work if it were to be saved.  For LDAP
     * identity providers, this will cause the Gateway to attempt to connect to the LDAP server and verify that
     * the base DN is valid and contains at least one entry and that the user and group mappings appear to work.
     * For federated identity providers this will currently always succeed.  The internal identity provider
     * should not be tested.
     *
     * @param cfg the {@link IdentityProviderConfig} to test.  Does not need to have been saved yet.  Must not be null.
     * @param testUsername test username if required by this ID provider config, otherwise null.
     * @param testPassword test password if required by this ID provider config, otherwise null.
     * @throws com.l7tech.identity.InvalidIdProviderCfgException
     *          if the test fails
     */
    void testIdProviderConfig(IdentityProviderConfig cfg, String testUsername, char[] testPassword)
            throws InvalidIdProviderCfgException;
    
    void testNtlmConfig(Map<String, String> props) throws InvalidIdProviderCfgException;

    /**
     * Get the specified {@link User}'s set of group membership {@link IdentityHeader}s.
     *
     * @param providerId the object ID of the {@link IdentityProviderConfig} in which this user can be found
     * @param userId     the unique identifier of the {@link User} whose {@link Group} memberships to look up.  Must not be null.
     * @return the Set of {@link IdentityHeader}s corresponding to {@link Group}s that this {@link User} belongs to.
     *         May be empty but not null.
     * @throws FindException if the specified information could not be accessed
     */
    @Transactional(readOnly = true)
    @Secured(types = USER, stereotype = GET_IDENTITY_PROPERTY_BY_ID, relevantArg = 1)
    Set<IdentityHeader> getGroupHeaders(long providerId, String userId) throws FindException;

    /**
     * Get the specified {@link Group}'s set of group membership {@link IdentityHeader}s.
     *
     * @param providerId the object ID of the {@link IdentityProviderConfig} in which this group can be found
     * @param groupId     the unique identifier of the {@link Group} whose {@link Group} memberships to look up.  Must not be null.
     * @return the Set of {@link IdentityHeader}s corresponding to {@link Group}s that this {@link Group} belongs to.
     *         May be empty but not null.
     * @throws FindException if the specified information could not be accessed
     */
    @Transactional(readOnly = true)
    @Secured(types = GROUP, stereotype = GET_IDENTITY_PROPERTY_BY_ID, relevantArg = 1)
    Set<IdentityHeader> getGroupHeadersForGroup(final long providerId, @NotNull final String groupId) throws FindException;

    /**
     * Get the specified {@link Group}'s set of member user {@link IdentityHeader}s.
     *
     * @param providerId the object ID of the {@link IdentityProviderConfig} in which this group can be found
     * @param groupId    the unique identifier of the {@link Group} whose {@link User} members to look up.  Must not be null.
     * @return the Set of {@link IdentityHeader}s corresponding to {@link User}s who belong to this {@link Group}.
     *         May be empty but not null.
     * @throws FindException if the specified information could not be accessed
     */
    @Transactional(readOnly = true)
    @Secured(types = GROUP, stereotype = GET_IDENTITY_PROPERTY_BY_ID, relevantArg = 1)
    Set<IdentityHeader> getUserHeaders(long providerId, String groupId) throws FindException;

    /**
     * Get the password policy associated with the identity provider
     *
     * @param providerId The identity provider that the password policy is associated with
     * @return the associated password policy
     * @throws FindException
     */
    @Transactional(readOnly = true)
    @Secured(types = PASSWORD_POLICY, stereotype = FIND_ENTITY)
    IdentityProviderPasswordPolicy getPasswordPolicyForIdentityProvider(long providerId) throws FindException;

    /**
     * Get the password policy description associated with the current user's identity provider
     *
     * @return the associated password policy
     * @throws FindException
     */
    @Transactional(readOnly = true)
    String getPasswordPolicyDescriptionForIdentityProvider() throws FindException;

    /**
     * Get the account minimums or null if there are none.
     *
     * @return The account minimums.
     */
    @Transactional(readOnly = true)
    AccountMinimums getAccountMinimums();

    /**
     * Gtet the account minimum maps or null if there are none.
     *
     * @return the account minimums map
     */
    @Transactional(readOnly = true)
    Map<String, AccountMinimums> getAccountMinimumsMap();

    /**
     * Get the Map of password policy minimums.
     * <p/>
     * <p>The Map is of names to password policies.</p>
     *
     * @return The minimums map.
     */
    @Transactional(readOnly = true)
    Map<String, IdentityProviderPasswordPolicy> getPasswordPolicyMinimums();

    /**
     * Saves the password policy for the identity provider
     * Note: only works for the ONE internal identity provider with special oid {@link IdentityProviderConfigManager#INTERNALPROVIDER_SPECIAL_OID}
     *
     * @param providerId Identity provider ID
     * @param policy     password policy to save
     * @return the ID of the password policy
     * @throws SaveException
     * @throws UpdateException
     * @throws ObjectNotFoundException
     */
    @Secured(types = PASSWORD_POLICY, stereotype = SAVE_OR_UPDATE, relevantArg = 1)
    String updatePasswordPolicy(long providerId, IdentityProviderPasswordPolicy policy)
            throws SaveException, UpdateException, ObjectNotFoundException;

    /**
     * Sets the change password flag in each admin user in the identity provider
     * <p/>
     * Note: only works for the ONE internal identity provider
     *
     * @param identityProviderConfigId the ID of the identity provider
     * @throws ObjectModelException If an error occurs
     */
    @Secured(types = ID_PROVIDER_CONFIG, stereotype = SAVE_OR_UPDATE)
    void forceAdminUsersResetPassword(long identityProviderConfigId) throws ObjectModelException;

    /**
     * Activates the user by resetting the values in the corresponding logon info
     *
     * @param user the user to reset the logon info
     * @throws FindException
     * @throws UpdateException
     */
    @Secured(types = USER, stereotype = SAVE_OR_UPDATE)
    void activateUser(User user) throws FindException, UpdateException;

    @Transactional(readOnly = true)
    @Secured(types = USER, stereotype = GET_PROPERTY_OF_ENTITY)
    LogonInfo.State getLogonState(User user) throws FindException;

    /**
     * Account minimums bean
     */
    class AccountMinimums implements Serializable {
        private final String name;
        private final int attempts;
        private final int lockout;
        private final int expiry;  // -1 for no minimum
        private final int inactivity;

        public AccountMinimums(final String name,
                               final int attempts,
                               final int lockout,
                               final int expiry,
                               final int inactivity) {
            this.name = name;
            this.attempts = attempts;
            this.lockout = lockout;
            this.expiry = expiry;
            this.inactivity = inactivity;
        }

        /**
         * Get the name for the account minimums.
         *
         * @return The name
         */
        public String getName() {
            return name;
        }

        /**
         * Get the number of permitted logon attempts.
         *
         * @return The permitted number of attempts.
         */
        public int getAttempts() {
            return attempts;
        }

        /**
         * The session inactivity timeout.
         * <p/>
         * <p>The number of minutes of inactivity after which a logon session
         * will expire.</p>
         *
         * @return The session expiry time, -1 for no minimum.
         */
        public int getExpiry() {
            return expiry;
        }

        /**
         * Get the account inactivity threshold.
         * <p/>
         * <p>The number of days after which an account is considered inactive.</p>
         *
         * @return The inactivity threshold.
         */
        public int getInactivity() {
            return inactivity;
        }

        /**
         * Get the account lockout duration.
         * <p/>
         * <p>The number of minutes an account is locked out for.</p>
         *
         * @return The lockout duration.
         */
        public int getLockout() {
            return lockout;
        }
    }
}
