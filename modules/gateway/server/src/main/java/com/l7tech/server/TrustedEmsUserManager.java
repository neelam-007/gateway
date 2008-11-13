package com.l7tech.server;

import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Entity manager for {@link com.l7tech.gateway.common.emstrust.TrustedEmsUser}.
 */
public interface TrustedEmsUserManager extends EntityManager<TrustedEmsUser, EntityHeader> {
    /** Exception thrown if an attempt is made to configure a user mapping which already exists. */
    public static class MappingAlreadyExistsException extends Exception {
        public MappingAlreadyExistsException() {
        }

        public MappingAlreadyExistsException(String message) {
            super(message);
        }

        public MappingAlreadyExistsException(String message, Throwable cause) {
            super(message, cause);
        }

        public MappingAlreadyExistsException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * Add or update a mapping allowing this Gateway to be administered by the specified EMS user, using the access
     * rights of the specified User.
     * <p/>
     * Caller is responsible for ensuring that User has already been authenticated.
     * <p/>
     * This method will fail (by throwing AccessControlException) if the specified user does not possess a role
     * to read or change one of the entities that need to be changed to complete this operation.  The following
     * permissions are required to add a mapping for an already-associated EMS instance:
     * <ul>
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.gateway.common.emstrust.TrustedEmsUser}
     * </ul>
     * If there is not yet a trust association for this EMS instance, the user will in addition require
     * the following permissions:
     * <ul>
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.security.cert.TrustedCert}
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.gateway.common.emstrust.TrustedEms}
     * </ul>
     * <p/>
     *
     * @param user         an already-authenticated User who has at least one admin role.  Required.
     * @param emsId        a hopefully-globally-unique identifier for the EMS instance that owns emsCert.  Required.
     *                     This identifier is opaque to the Gateway.
     * @param emsCert      The certificate this EMS instance will use when vouching for emsUsername in admin requests.  Required.
     * @param emsUsername  The identifier this EMS instance will use when referring to this User in admin requests.  Required.
     * @return the newly-created TrustedEmsUser instance.  Never null.
     * @throws java.security.AccessControlException if the specified user lacks sufficient permission to create or update this mapping
     * @throws com.l7tech.objectmodel.ObjectModelException  if there is a problem accessing or updating the database
     * @throws java.security.cert.CertificateException if there is a problem with the emsCert
     * @throws CertificateMismatchException if the specified emsId has already been registered on this Gateway with a different
     *                                      certificate from emsCert.
     * @throws MappingAlreadyExistsException If a mapping already exists for the specified EMS username on the specified EMS instance.
     */
    TrustedEmsUser configureUserMapping(User user, String emsId, X509Certificate emsCert, String emsUsername) 
            throws ObjectModelException, AccessControlException, CertificateException, CertificateMismatchException, MappingAlreadyExistsException;

    /**
     * Deletes all EMS user mappings for the specified user.
     *
     * @param user the user whose mappings to delete.
     * @return true iff. any mappings were deleted.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     * @throws com.l7tech.objectmodel.DeleteException if DB problem
     */
    boolean deleteMappingsForUser(User user) throws FindException, DeleteException;

    /**
     * Deletes all EMS user mappings for users in the specified identity provider.
     *
     * @param identityProviderOid the OID of the identity provider whose EMS user mappings to delete.  Required.
     * @return true iff. any mappings were deleted.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     * @throws com.l7tech.objectmodel.DeleteException if DB problem
     */
    boolean deleteMappingsForIdentityProvider(long identityProviderOid) throws FindException, DeleteException;

    /**
     * Find all user mappings for the specified Trusted EMS, identified by its OID.
     *
     * @param trustedEmsOid OID of the EMS instance whose mappings to find.
     * @return a Collection of all TrustedEmsUser instances associated with this TrustedEms.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     */
    Collection<TrustedEmsUser> findByEmsId(long trustedEmsOid) throws FindException;

    /**
     * Find a user mapping for the specified Trusted EMS, identified by its OIDs.
     *
     * @param trustedEmsOid OID of the EMS instance whose mapping to find.
     * @param emsUsername UUID of the EMS user whose mapping to find.
     * @return a Collection of all TrustedEmsUser instances associated with this TrustedEms.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     */
    TrustedEmsUser findByEmsIdAndUserUUID(long trustedEmsOid, String emsUsername) throws FindException;
}
