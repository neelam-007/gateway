package com.l7tech.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.security.rbac.EntityType;
import static com.l7tech.gateway.common.security.rbac.EntityType.TRUSTED_CERT;
import static com.l7tech.gateway.common.security.rbac.EntityType.TRUSTED_EMS;
import com.l7tech.gateway.common.security.rbac.OperationType;
import static com.l7tech.gateway.common.security.rbac.OperationType.CREATE;
import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import static com.l7tech.gateway.common.security.rbac.OperationType.UPDATE;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import static com.l7tech.security.cert.TrustedCert.TrustedFor;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.AccessControlException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * Entity manager for {@link TrustedEms}.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedEmsManagerImpl extends HibernateEntityManager<TrustedEms, EntityHeader> implements TrustedEmsManager {
    @Resource
    private RoleManager roleManager;

    @Resource
    private TrustedCertManager trustedCertManager;

    public Class<? extends Entity> getImpClass() {
        return TrustedEms.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return TrustedEms.class;
    }

    public String getTableName() {
        return "trusted_ems";
    }

    private void require(User user, OperationType op, EntityType ent) throws ObjectModelException, AccessControlException {
        if (!roleManager.isPermittedForAnyEntityOfType(user, op, ent))
            throw new AccessControlException("Permission denied: " + op + " " + ent);
    }

    private TrustedCert lookupCert(User user, X509Certificate cert) throws ObjectModelException, CertificateEncodingException {
        require(user, READ, TRUSTED_CERT);
        List tc = trustedCertManager.findByThumbprint(CertUtils.getThumbprintSHA1(cert));
        return tc != null && tc.size() > 0 ? (TrustedCert) tc.get(0) : null;
    }

    public TrustedEms getOrCreateEmsAssociation(User user, String emsId, X509Certificate emsCert) throws ObjectModelException, AccessControlException, CertificateException
    {
        if (user == null)
            throw new IllegalArgumentException("Missing authenticated user");
        if (emsId == null || emsId.trim().length() < 1)
            throw new IllegalArgumentException("Missing or malformed EMS ID");
        if (emsCert == null)
            throw new IllegalArgumentException("Missing EMS certificate");

        require(user, READ, TRUSTED_EMS);
        TrustedEms trustedEms = findByUniqueName(emsId);

        if (trustedEms != null) {
            if (!CertUtils.certsAreEqual(trustedEms.getTrustedCert().getCertificate(), emsCert)) {
                // It is NOT safe to just replace the cert -- this could allow an admin user to instantly
                // reassign all existing user mappings to a totally different EMS without being completely aware of it.
                throw new CertificateException("Specified EMS certificate does not match the previously-known certificate for the specified EMS ID");
            }
            return trustedEms;
        }

        // Need to create a new association.  See if we already recognize this cert.
        TrustedCert trustedCert = lookupCert(user, emsCert);        

        if (trustedCert == null) {
            // Need to create a TrustedCert first.
            trustedCert = new TrustedCert();
            trustedCert.setName("EMS Cert: " + emsId);
            trustedCert.setCertificate(emsCert);
            trustedCert.setTrustedFor(TrustedFor.TRUSTED_EMS, true);
            trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);

            require(user, CREATE, TRUSTED_CERT);
            trustedCert.setOid(trustedCertManager.save(trustedCert));
        } else {
            // Ensure existing TrustedCert allows EMS usage.
            if (!trustedCert.isTrustedFor(TrustedFor.TRUSTED_EMS)) {
                trustedCert.setTrustedFor(TrustedFor.TRUSTED_EMS, true);
                
                require(user, UPDATE, TRUSTED_CERT);
                trustedCertManager.update(trustedCert);
            }
        }

        trustedEms = new TrustedEms();
        trustedEms.setName(emsId);
        trustedEms.setTrustedCert(trustedCert);

        require(user, CREATE, TRUSTED_EMS);
        trustedEms.setOid(save(trustedEms));
        return trustedEms;
    }
}
