package com.l7tech.identity;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;

import javax.security.auth.Subject;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Set;

/**
 * Provides a remote interface for creating, searching, and retrieving identity providers, identity provider
 * configurations (including LDAP templates), and actual identities (users and groups).
 *
 * @see IdentityProviderConfig
 * @see EntityHeader
 * @see Entity
 */
public interface IdentityAdmin {
    /**
     * Retrieve the server admin protocol version string.
     *
     * @return the server admin protocol version string, ie "20040603".  Never null
     */
    String echoVersion() throws RemoteException;

    /**
     * Retrieve all available identity provider configurations.  Every {@link IdentityProvider} has one and only one
     * {@link IdentityProviderConfig}, and each identity provider configuration belongs to one and only one
     * identity provider.
     *
     * @return array of entity headers for all existing ID provider config.  May be empty but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     * @throws RemoteException on remote communication error
     */
    EntityHeader[] findAllIdentityProviderConfig() throws RemoteException, FindException;

    /**
     * Retrieve a specified identity provider configuration given its object ID.
     * Every {@link IdentityProvider} has one and only one
     * {@link IdentityProviderConfig}, and each identity provider configuration belongs to one and only one
     * identity provider.
     *
     * @param oid   the unique object identifier of the {@link IdentityProviderConfig} to obtain
     * @return      the requested {@link IdentityProviderConfig}, or null if it was not found.
     * @throws FindException   if there was a problem accessing the requested information.
     * @throws RemoteException on remote communication error
     */
    IdentityProviderConfig findIdentityProviderConfigByID(long oid) throws RemoteException, FindException;

    /**
     * Retrieve all available LDAP templates.  An LDAP template is a preconfigured set of LDAP properties for
     * various common LDAP implementations such as Microsoft Active Directory or Netscape LDAP.
     *
     * @return an array of LDAP identity provider configurations.  May be empty, but never null.
     * @throws FindException   if there was a problem accessing the requested information.
     * @throws RemoteException on remote communication error
     */
    LdapIdentityProviderConfig[] getLdapTemplates() throws RemoteException, FindException;

    /**
     * Store the specified new or existing identity provider configuration.  If the specified
     * {@link IdentityProviderConfig} contains a unique object ID that already exists, this will replace
     * the object'  s current configuration with the new configuration.  Otherwise, a new object will be created.
     *
     * @param cfg the identity provider configuration to create or update.  Must not be null.
     * @return the unique id of the object that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws RemoteException on remote communication error
     */
    long saveIdentityProviderConfig(IdentityProviderConfig cfg)
      throws RemoteException, SaveException, UpdateException;

    /**
     * Delete the identity provider configuration specified by its unique object ID.
     *
     * @param oid  the unique object ID of an already-existing {@link IdentityProviderConfig}
     * @throws DeleteException if the requested information could not be deleted
     * @throws DeleteException if the requested object ID did not exist
     * @throws RemoteException on remote communication error
     */
    void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException;

    /**
     * Retrieve all available {@link User}s for a given {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId     the unique object ID of the {@link IdentityProviderConfig} whose users to load.
     * @return array of {@link EntityHeader}s for all {@link User}s within this {@link IdentityProviderConfig}.
     *         May be empty but never null.
     * @throws FindException if the specified object ID did not exist or was not an identity provider config
     * @throws FindException if there was a problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    EntityHeader[] findAllUsers(long idProvCfgId) throws RemoteException, FindException;

    /**
     * Search for {@link Entity}s matching a pattern within the specified {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId  the unique object ID of the {@link IdentityProviderConfig} whose users to load.
     * @param types        list of {@link EntityType}s to include in the results.  Must not be null or empty.
     * @param pattern      the search pattern to match against entity names.
     *                     This is a String that may contain the metacharacters
     *                     "*" or "?" to match zero or more arbirary characters, or exactly one character,
     *                     respectively.  The match is always case-insensitive for ASCII characters.
     *                     <p>
     *                     Examples:  <ul><li>pattern "ike" will match user "ike" or "Ike" but not users "mike" or "ike22";</li>
     *                                <li>pattern "ike*" will match "ike", "Ike" and "ike22" but not "mike";</li>
     *                                <li>pattern "i?e*" will match "ike", "Ike", "ice" and "ike22" but not "eke" or "ikke"</li></ul>
     *
     * @return array of {@link EntityHeader}s matching the specified requirements.
     * @throws FindException   if there was a problem accessing the requested information.
     * @throws RemoteException on remote communication error
     */
    EntityHeader[] searchIdentities(long idProvCfgId, EntityType[] types, String pattern)
      throws RemoteException, FindException;

    /**
     * Search for a {@link User} by its user ID within an {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId  the unique object ID of the {@link IdentityProviderConfig} in which to search.
     * @param userId    the unique identifier of the user within this {@link IdentityProviderConfig}.  Must not be empty or null.
     * @return the requested {@link User}, or null if no user with that user ID was found in this ID provider
     * @throws FindException   if there was a problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    User findUserByID(long idProvCfgId, String userId)
      throws RemoteException, FindException;

    /**
     * Search for a {@link User} by its login name.
     *
     * @param idProvCfgId  the unique object ID of the {@link IdentityProviderConfig} in which to search.
     * @param login  The user login.  See {@link User#getLogin}
     * @return the requested {@link User}, or null if no user with that login was found in this ID provider
     * @throws FindException   if there was a problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    User findUserByLogin(long idProvCfgId, String login)
      throws RemoteException, FindException;

    /**
     * Delete a {@link User} by its unique identifier.
     *
     * @param idProvCfgId   the unique object ID of the {@link IdentityProviderConfig} in which to search
     * @param userId        the unique identifier of the {@link User} to delete.  Must not be null or empty
     * @throws DeleteException          if the requested information could not be deleted
     * @throws ObjectNotFoundException  if no user with the specified unique identifier exists in the specified
     *                                  identity provider
     * @throws RemoteException on remote communication error
     */
    void deleteUser(long idProvCfgId, String userId)
      throws RemoteException, DeleteException, ObjectNotFoundException;

    /**
     * Store the specified new or updated {@link User} object.  If the specified
     * {@link User} contains a unique identifier that already exists, this will replace
     * the objects current state with the new state.  Otherwise, a new object will be created.
     *
     * @param idProvCfgId the {@link IdentityProviderConfig} in which the {@link User} already exists, or in which it
     *                    should be created.  Must not be null
     * @param user         the new or updated {@link User} instance.  Must not be null.  If this user contains
     *                     a null unique identifier, this call will create a new {@link User} instance.  Otherwise,
     *                     if this user contains a unique identifier, the specified {@link User}'s state will be
     *                     replaced with the data from user.
     * @param groupHeaders the set of {@link EntityHeader}s corresponding to the {@link Group}s to which this
     *                     {@link User} should belong when this call completes.
     *                     May be empty but not null.
     *                     If the user already exists, any existing group memberships not included in this set will
     *                     be severed.
     * @return the unique identifier that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws ObjectNotFoundException  if the user contained a non-null unique identifier, but no {@link User} with this
     *                                  unique identifier exists in the specified identity provider.
     * @throws RemoteException on remote communication error
     */
    String saveUser(long idProvCfgId, User user, Set groupHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException;

    /**
     * Retrieve all available {@link Group}s for a given {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId     the unique object ID of the {@link IdentityProviderConfig} whose groups to load.
     * @return array of {@link EntityHeader}s for all {@link Group}s within this {@link IdentityProviderConfig}.
     *         May be empty but never null.
     * @throws FindException if the specified object ID did not exist or was not an identity provider config
     * @throws FindException if there was a problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    EntityHeader[] findAllGroups(long idProvCfgId) throws RemoteException, FindException;

    /**
     * Search for a {@link Group} by its user ID within an {@link IdentityProviderConfig}.
     *
     * @param idProvCfgId  the unique object ID of the {@link IdentityProviderConfig} in which to search.
     * @param groupId    the unique identifier of the {@link Group} within this {@link IdentityProviderConfig}.
     *                   Must not be empty or null.
     * @return the requested {@link Group}, or null if no group with that unique identifier was found in this ID provider
     * @throws FindException   if there was a problem accessing the requested information
     * @throws RemoteException on remote communication error
     */
    Group findGroupByID(long idProvCfgId, String groupId)
      throws RemoteException, FindException;

    /**
     * Delete a {@link Group} by its unique identifier.
     *
     * @param idProvCfgId   the unique object ID of the {@link IdentityProviderConfig} in which to search
     * @param groupId        the unique identifier of the {@link Group} to delete.  Must not be null or empty
     * @throws DeleteException          if the requested information could not be deleted
     * @throws ObjectNotFoundException  if no {@link Group} with the specified unique identifier exists in the specified
     *                                  identity provider
     * @throws RemoteException on remote communication error
     */
    void deleteGroup(long idProvCfgId, String groupId)
      throws RemoteException, DeleteException, ObjectNotFoundException;

    /**
     * Store the specified new or updated {@link Group} object.  If the specified
     * {@link Group} contains a unique identifier that already exists, this will replace
     * the objects current state with the new state.  Otherwise, a new object will be created.
     *
     * @param idProvCfgId the object ID of the {@link IdentityProviderConfig} in which the {@link Group} already exists,
     *                    or in which it should be created.  Must not be null
     * @param group        the new or updated {@link Group} instance.  Must not be null.  If this group contains
     *                     a null unique identifier, this call will create a new {@link Group} instance.  Otherwise,
     *                     if this group contains a unique identifier, the specified {@link Group}'s state will be
     *                     replaced with the data from group.
     * @param userHeaders the set of {@link EntityHeader}s corresponding to the {@link User}s which should belong
     *                    to this {@link Group} when this call completes.
     *                     May be empty but not null.
     *                     If the group already exists, any existing group memberships not included in this set will
     *                     be severed.
     * @return the unique identifier that was updated or created.
     * @throws SaveException   if the requested information could not be saved
     * @throws UpdateException if the requested information could not be updated
     * @throws ObjectNotFoundException  if the group contained a non-null unique identifier, but no {@link Group} with this
     *                                  unique identifier exists in the specified identity provider.
     * @throws RemoteException on remote communication error
     */
    String saveGroup(long idProvCfgId, Group group, Set userHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException;

    /**
     * Get a user's X.509 certificate as Base64-encoded ASN.1 DER.
     *
     * @param user the {@link User} whose certificate is to be retrieved.  Must not be null.  Must be an
     *             already-existing {@link User} instance obtained from {@link #findUserByID(long, String)} or
     *             {@link #findUserByLogin(long, String)}.
     * @return the user's X.509 certificate DER encoded and then converted to Base64, or null if the user did not
     *         have one.
     * @throws CertificateEncodingException if an encoding error occurs producing the DER
     * @throws FindException if the requested information could not be accessed
     * @throws RemoteException on remote communication error
     */
    String getUserCert(User user) throws RemoteException, FindException, CertificateEncodingException;

    /**
     * Revoke a user's X.509 certificate and lock the password where possible.  This removes any (possibly-shadowed)
     * X.509 certificate that is being stored for the specified {@link User}.  If the user is an internal user,
     * this will also change the user's password to a string of 32 random bytes, effective locking out the
     * account until a new password is assigned.  This is to prevent a holder of a stolen password from simply
     * applying for a new certificate on behalf of the user in question.
     *
     * @param user  the user whose certificate should be revoked and password locked, if possible.  May not be null.
     *              Must be an already-existing {@link User} instance obtained from
     *              {@link #findUserByID(long, String)} or {@link #findUserByLogin(long, String)}.
     * @throws ObjectNotFoundException if the specified user does not exist
     * @throws UpdateException if there was a problem updating the database
     * @throws RemoteException on remote communication error
     */
    void revokeCert(User user) throws RemoteException, UpdateException, ObjectNotFoundException;

    /**
     * Store a certificate for a user who does not yet have one.
     *
     * @param user  the {@link User} who is to be assigned a certificate.  Must not be null.
     *              Must be an already-existing {@link User} instance obtained from
     *              {@link #findUserByID(long, String)} or {@link #findUserByLogin(long, String)}.
     * @param cert  the certificate to save.  Must not be null.
     * @throws UpdateException if user was not in a state that allows the creation of a certificate
     *                         (already has a cert; has tried too many times to create a cert; ID provider is
     *                          not able to store user certs; or some other problem)
     * @throws UpdateException if there was a problem updating the database
     * @throws RemoteException on remote communication error
     */
    void recordNewUserCert(User user, Certificate cert) throws RemoteException, UpdateException;

    /**
     * Test if the prospective {@link IdentityProviderConfig} would work if it were to be saved.  For LDAP
     * identity providers, this will cause the Gateway to attempt to connect to the LDAP server and verify that
     * the base DN is valid and contains at least one entry and that the user and group mappings appear to work.
     * For federated identity providers this will currently always succeed.  The internal identity provider
     * should not be tested.
     *
     * @param cfg  the {@link IdentityProviderConfig} to test.  Does not need to have been saved yet.  Must not be null.
     * @throws InvalidIdProviderCfgException if the test fails
     * @throws RemoteException on remote communication error
     */
    void testIdProviderConfig(IdentityProviderConfig cfg)
      throws RemoteException, InvalidIdProviderCfgException;

    /**
     * Get the specified {@link User}'s set of group membership {@link EntityHeader}s.
     *
     * @param providerId the object ID of the {@link IdentityProviderConfig} in which this user can be found
     * @param userId the unique identifier of the {@link User} whose {@link Group} memberships to look up.  Must not be null.
     * @return the Set of {@link EntityHeader}s corresponding to {@link Group}s that this {@link User} belongs to.
     *         May be empty but not null.
     * @throws FindException if the specified information could not be accessed
     * @throws RemoteException on remote communication error
     */
    Set getGroupHeaders(long providerId, String userId) throws RemoteException, FindException;

    /**
     * Get the specified {@link Group}'s set of member user {@link EntityHeader}s.
     *
     * @param providerId the object ID of the {@link IdentityProviderConfig} in which this group can be found
     * @param groupId the unique identifier of the {@link Group} whose {@link User} members to look up.  Must not be null.
     * @return the Set of {@link EntityHeader}s corresponding to {@link User}s who belong to this {@link Group}.
     *         May be empty but not null.
     * @throws FindException if the specified information could not be accessed
     * @throws RemoteException on remote communication error
     */
    Set getUserHeaders(long providerId, String groupId) throws RemoteException, FindException;

    /**
     * Determine the roles for the given subject.
     *
     * @param subject the subject whose roles to get
     * @return the <code>Set</code> of roles (groups) of which the subject is a member
     * @throws AccessControlException if the current subject is not allowed to perform the operation.
     *                                The invocation tests whether the current subject  (the subject carrying out the operaton)
     *                                has privileges to perform the operation. The operators are not allowed to perform this operation
     *                                except for themselves.
     * @throws RemoteException on remote communication error
     */
    Set getRoles(Subject subject) throws RemoteException, AccessControlException;
}
