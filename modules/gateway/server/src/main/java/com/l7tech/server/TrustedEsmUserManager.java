package com.l7tech.server;

import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;

/**
 * Entity manager for {@link com.l7tech.gateway.common.esmtrust.TrustedEsmUser}.
 */
public interface TrustedEsmUserManager extends GoidEntityManager<TrustedEsmUser, EntityHeader> {
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
     * Add or update a mapping allowing this Gateway to be administered by the specified ESM user, using the access
     * rights of the specified User.
     * <p/>
     * Caller is responsible for ensuring that User has already been authenticated.
     * <p/>
     * This method will fail (by throwing AccessControlException) if the specified user does not possess a role
     * to read or change one of the entities that need to be changed to complete this operation.  The following
     * permissions are required to add a mapping for an already-associated ESM instance:
     * <ul>
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.gateway.common.esmtrust.TrustedEsmUser}
     * </ul>
     * If there is not yet a trust association for this ESM instance, the user will in addition require
     * the following permissions:
     * <ul>
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.security.cert.TrustedCert}
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.gateway.common.esmtrust.TrustedEsm}
     * </ul>
     * <p/>
     *
     * @param user         an already-authenticated User who has at least one admin role.  Required.
     * @param esmId        a hopefully-globally-unique identifier for the ESM instance that owns esmCert.  Required.
     *                     This identifier is opaque to the Gateway.
     * @param esmCert      The certificate this ESM instance will use when vouching for esmUsername in admin requests.  Required.
     * @param esmUsername  The identifier this ESM instance will use when referring to this User in admin requests.  Required.
     * @param esmUserDisplayName Friendly name to display in UI for this mapping, or null.  Used for cosmetic purposes only.  Optional.
     * @return the newly-created TrustedEsmUser instance.  Never null.
     * @throws java.security.AccessControlException if the specified user lacks sufficient permission to create or update this mapping
     * @throws com.l7tech.objectmodel.ObjectModelException  if there is a problem accessing or updating the database
     * @throws java.security.cert.CertificateException if there is a problem with the esmCert
     * @throws CertificateMismatchException if the specified esmId has already been registered on this Gateway with a different
     *                                      certificate from esmCert.
     * @throws MappingAlreadyExistsException If a mapping already exists for the specified ESM username on the specified ESM instance.
     */
    TrustedEsmUser configureUserMapping(User user, String esmId, X509Certificate esmCert, String esmUsername, String esmUserDisplayName)
            throws ObjectModelException, AccessControlException, CertificateException, CertificateMismatchException, MappingAlreadyExistsException;

    /**
     * Deletes all ESM user mappings for the specified user.
     *
     * @param user the user whose mappings to delete.
     * @return true iff. any mappings were deleted.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     * @throws com.l7tech.objectmodel.DeleteException if DB problem
     */
    boolean deleteMappingsForUser(User user) throws FindException, DeleteException;

    /**
     * Deletes all ESM user mappings for users in the specified identity provider.
     *
     * @param identityProviderOid the OID of the identity provider whose ESM user mappings to delete.  Required.
     * @return true iff. any mappings were deleted.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     * @throws com.l7tech.objectmodel.DeleteException if DB problem
     */
    boolean deleteMappingsForIdentityProvider(Goid identityProviderOid) throws FindException, DeleteException;

    /**
     * Find all user mappings for the specified Trusted ESM, identified by its OID.
     *
     * @param trustedEsmGoid OID of the ESM instance whose mappings to find.
     * @return a Collection of all TrustedEsmUser instances associated with this TrustedEsm.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     */
    Collection<TrustedEsmUser> findByEsmId(Goid trustedEsmGoid) throws FindException;

    /**
     * Find a user mapping for the specified Trusted ESM, identified by its OIDs.
     *
     * @param trustedEsmGoid OID of the ESM instance whose mapping to find.
     * @param esmUsername UUID of the ESM user whose mapping to find.
     * @return a Collection of all TrustedEsmUser instances associated with this TrustedEsm.  May be empty but never null.
     * @throws com.l7tech.objectmodel.FindException if DB problem
     */
    TrustedEsmUser findByEsmIdAndUserUUID(Goid trustedEsmGoid, String esmUsername) throws FindException;
}
