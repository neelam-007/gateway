package com.l7tech.identity;

import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.*;

import javax.security.auth.Subject;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.AccessControlException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Set;

/**
 * Class IdentityAdmin.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 *         <p/>
 *         Interface for the remote administration of the identity types.
 *         Two server-side implementations in subpackahes rmi and ws.
 */
public interface IdentityAdmin extends Remote {
    String echoVersion() throws RemoteException;

    EntityHeader[] findAllIdentityProviderConfig() throws RemoteException, FindException;

    EntityHeader[] findAllIdentityProviderConfigByOffset(int offset, int windowSize)
      throws RemoteException, FindException;

    IdentityProviderConfig findIdentityProviderConfigByPrimaryKey(long oid) throws RemoteException, FindException;

    LdapIdentityProviderConfig[] getLdapTemplates() throws RemoteException, FindException;

    long saveIdentityProviderConfig(IdentityProviderConfig cfg)
      throws RemoteException, SaveException, UpdateException;

    void deleteIdentityProviderConfig(long oid) throws RemoteException, DeleteException;

    EntityHeader[] findAllUsers(long idProvCfgId) throws RemoteException, FindException;

    EntityHeader[] findAllUsersByOffset(long idProvCfgId, int offset, int windowSize)
      throws RemoteException, FindException;

    EntityHeader[] searchIdentities(long idProvCfgId, EntityType[] types, String pattern)
      throws RemoteException, FindException;

    User findUserByPrimaryKey(long idProvCfgId, String userId)
      throws RemoteException, FindException;

    User findUserByLogin(long idProvCfgId, String login)
      throws RemoteException, FindException;

    void deleteUser(long idProvCfgId, String userId)
      throws RemoteException, DeleteException, ObjectNotFoundException;

    String saveUser(long idProvCfgId, User user, Set groupHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException;

    EntityHeader[] findAllGroups(long idProvCfgId) throws RemoteException, FindException;

    EntityHeader[] findAllGroupsByOffset(long idProvCfgId, int offset, int windowSize)
      throws RemoteException, FindException;

    Group findGroupByPrimaryKey(long idProvCfgId, String groupId)
      throws RemoteException, FindException;

    void deleteGroup(long idProvCfgId, String groupId)
      throws RemoteException, DeleteException, ObjectNotFoundException;

    String saveGroup(long idProvCfgId, Group group, Set userHeaders)
      throws RemoteException, SaveException, UpdateException, ObjectNotFoundException;

    String getUserCert(User user) throws RemoteException, FindException, CertificateEncodingException;

    /**
     * will also revoke user's password if internal user
     */
    void revokeCert(User user) throws RemoteException, UpdateException, ObjectNotFoundException;

    void recordNewUserCert(User user, Certificate cert) throws RemoteException, UpdateException;

    void testIdProviderConfig(IdentityProviderConfig cfg)
      throws RemoteException, InvalidIdProviderCfgException;

    Set getGroupHeaders(long providerId, String userId) throws RemoteException, FindException;

    Set getUserHeaders(long providerId, String groupId) throws RemoteException, FindException;

    /**
     * Determine the roles for the given subject.
     *
     * @param subject the subject for which to get roles for
     * @return the <code>Set</code> of roles (groups) the subject is memeber of
     * @throws RemoteException        on remote invocation error
     * @throws AccessControlException if the current subject is not allowed to perform the operation.
     *                                The invocation tests whether the current subject  (the subject carrying out the operaton)
     *                                has privileges to perform the operation. The operators are not allowed to perform this operation
     *                                except for themselves.
     */
    Set getRoles(Subject subject) throws RemoteException, AccessControlException;
}
