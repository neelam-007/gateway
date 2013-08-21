package com.l7tech.server;

import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Entity manager for {@link com.l7tech.gateway.common.esmtrust.TrustedEsm}.
 */
public interface TrustedEsmManager extends EntityManager<TrustedEsm, EntityHeader> {

    /**
     * Find TrustedEms by ID.
     *
     * @param esmId The unique ESM identifier.
     * @return The TrustedEsm or null if not found.
     * @throws FindException if an error occurs during lookup
     */
    TrustedEsm findEsmById(String esmId) throws FindException;

    /**
     * Look up or add a TrustedEsm entry allowing the specified ESM instance to manage this Gateway cluster
     * when authenticated by the specified ESM certificate.
     * <p/>
     * Before this ESM association can be used to send admin requests, at least one user mapping must be
     * configured from a user on this ESM to a local admin user on this Gateway cluster.
     * <p/>
     * Caller is responsible for ensuring that User has already been authenticated.
     * <p/>
     * This method will fail (by throwing AccessControlException) if the specified user does not possess a role
     * to read or change one of the entities that need to be changed to complete this operation.  The exact
     * permissions required depend on what needs to be done, but the following are always sufficient:
     * <ul>
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.security.cert.TrustedCert}
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.gateway.common.esmtrust.TrustedEsm}
     * </ul>
     *
     * @param requestingUser the admin user on whose behalf this association is being created.  Used
     *                       for RBAC enforcement.  Required.
     * @param esmId        a hopefully-globally-unique identifier for the ESM instance that owns esmCert.  Required.
     *                     This identifier is opaque to the Gateway.
     * @param esmCert      The certificate this ESM instance will use when vouching for esmUsername in admin requests.  Required.
     * @return the found, added or updated TrustedEsm instance.  Never null.
     * @throws java.security.AccessControlException if the specified user lacks sufficient permission to create or update this association
     * @throws com.l7tech.objectmodel.ObjectModelException  if there is a problem accessing or updating the database
     * @throws CertificateException if esmCert cannot be encoded
     * @throws CertificateMismatchException if an association already exists for the specified esmId but with a certificate other than esmCert
     */
    TrustedEsm getOrCreateEsmAssociation(User requestingUser, String esmId, X509Certificate esmCert) throws ObjectModelException, AccessControlException, CertificateException, CertificateMismatchException;
}
