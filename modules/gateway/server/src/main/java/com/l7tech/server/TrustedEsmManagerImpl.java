package com.l7tech.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.security.AccessControlException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.List;

import static com.l7tech.gateway.common.security.rbac.OperationType.CREATE;
import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import static com.l7tech.objectmodel.EntityType.TRUSTED_CERT;
import static com.l7tech.objectmodel.EntityType.TRUSTED_ESM;

/**
 * Entity manager for {@link com.l7tech.gateway.common.esmtrust.TrustedEsm}.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedEsmManagerImpl extends HibernateGoidEntityManager<TrustedEsm, EntityHeader> implements TrustedEsmManager {
    @Inject
    private RoleManager roleManager;

    @Inject
    private TrustedCertManager trustedCertManager;

    @Override
    public Class<? extends Entity> getImpClass() {
        return TrustedEsm.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return TrustedEsm.class;
    }

    @Override
    public String getTableName() {
        return "trusted_esm";
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

    @Override
    public TrustedEsm findEsmById( final String esmId ) throws FindException {
        return findByUniqueName(esmId);
    }

    @Override
    public TrustedEsm getOrCreateEsmAssociation(User user, String esmId, X509Certificate emsCert) throws ObjectModelException, AccessControlException, CertificateException, CertificateMismatchException {
        if (user == null)
            throw new IllegalArgumentException("Missing authenticated user");
        if (esmId == null || esmId.trim().length() < 1)
            throw new IllegalArgumentException("Missing or malformed ESM ID");
        if (emsCert == null)
            throw new IllegalArgumentException("Missing ESM certificate");

        TrustedEsm trustedEms = findByUniqueName(esmId);
        
        if (trustedEms != null) {
            if (!CertUtils.certsAreEqual(trustedEms.getTrustedCert().getCertificate(), emsCert)) {
                // It is NOT safe to just replace the cert -- this could allow an admin user to instantly
                // reassign all existing user mappings to a totally different ESM without being completely aware of it.
                throw new CertificateMismatchException("Specified ESM certificate does not match the previously-known certificate for the specified ESM ID");
            }
            return trustedEms;
        }

        // Need to create a new association.  See if we already recognize this cert.
        TrustedCert trustedCert = lookupCert(user, emsCert);        

        if (trustedCert == null) {
            // Need to create a TrustedCert first.
            trustedCert = new TrustedCert();
            trustedCert.setName("ESM Cert: " + esmId);
            trustedCert.setCertificate(emsCert);
            trustedCert.setRevocationCheckPolicyType(TrustedCert.PolicyUsageType.NONE);

            require(user, CREATE, TRUSTED_CERT);
            trustedCert.setGoid(trustedCertManager.save(trustedCert));
        }

        trustedEms = new TrustedEsm();
        trustedEms.setName(esmId);
        trustedEms.setTrustedCert(trustedCert);

        require(user, CREATE, TRUSTED_ESM);
        trustedEms.setGoid(save(trustedEms));
        return trustedEms;
    }
}
