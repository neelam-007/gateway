package com.l7tech.server;

import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.ObjectModelException;

import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Entity manager for {@link TrustedEms}.
 */
public interface TrustedEmsManager extends EntityManager<TrustedEms, EntityHeader> {
    /**
     * Look up or add a TrustedEms entry allowing the specified EMS instance to manage this Gateway cluster
     * when authenticated by the specified EMS certificate.
     * <p/>
     * Before this EMS association can be used to send admin requests, at least one user mapping must be
     * configured from a user on this EMS to a local admin user on this Gateway cluster.
     * <p/>
     * Caller is responsible for ensuring that User has already been authenticated.
     * <p/>
     * This method will fail (by throwing AccessControlException) if the specified user does not possess a role
     * to read or change one of the entities that need to be changed to complete this operation.  The exact
     * permissions required depend on what needs to be done, but the following are always sufficient:
     * <ul>
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.security.cert.TrustedCert}
     * <li>READ, CREATE and/or UPDATE of ANY {@link com.l7tech.gateway.common.emstrust.TrustedEms}
     * </ul>
     *
     * @param requestingUser the admin user on whose behalf this association is being created.  Used
     *                       for RBAC enforcement.  Required.
     * @param emsId        a hopefully-globally-unique identifier for the EMS instance that owns emsCert.  Required.
     *                     This identifier is opaque to the Gateway.
     * @param emsCert      The certificate this EMS instance will use when vouching for emsUsername in admin requests.  Required.
     * @return the found, added or updated TrustedEms instance.  Never null.
     * @throws java.security.AccessControlException if the specified user lacks sufficient permission to create or update this association
     * @throws com.l7tech.objectmodel.ObjectModelException  if there is a problem accessing or updating the database
     * @throws CertificateException if emsCert cannot be encoded
     * @throws CertificateMismatchException if an association already exists for the specified emsId but with a certificate other than emsCert
     */
    TrustedEms getOrCreateEmsAssociation(User requestingUser, String emsId, X509Certificate emsCert) throws ObjectModelException, AccessControlException, CertificateException, CertificateMismatchException;
}
