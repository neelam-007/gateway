package com.l7tech.server;

import com.l7tech.common.io.CertUtils;
import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.gateway.common.security.rbac.EntityType;
import static com.l7tech.gateway.common.security.rbac.EntityType.*;
import com.l7tech.gateway.common.security.rbac.OperationType;
import static com.l7tech.gateway.common.security.rbac.OperationType.CREATE;
import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.security.cert.TrustedCert;
import com.l7tech.security.cert.TrustedCertManager;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.AccessControlException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity manager for {@link com.l7tech.gateway.common.emstrust.TrustedEmsUser}.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedEmsUserManagerImpl extends HibernateEntityManager<TrustedEmsUser, EntityHeader> implements TrustedEmsUserManager {
    @Resource
    private TrustedEmsManager trustedEmsManager;

    @Resource
    private RoleManager roleManager;

    @Resource
    private TrustedCertManager trustedCertManager;


    public Class<? extends Entity> getImpClass() {
        return TrustedEmsUser.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return TrustedEmsUser.class;
    }

    public String getTableName() {
        return "trusted_ems_user";
    }

    private void require(User user, OperationType op, EntityType ent) throws ObjectModelException, AccessControlException {
        if (!roleManager.isPermittedForAnyEntityOfType(user, op, ent))
            throw new AccessControlException("Permission denied: " + op + " " + ent);
    }

    private TrustedCert lookupCert(X509Certificate cert) throws FindException, CertificateEncodingException {
        List tc = trustedCertManager.findByThumbprint(CertUtils.getThumbprintSHA1(cert));
        return tc != null && tc.size() > 0 ? (TrustedCert) tc.get(0) : null;
    }

    public long addUserMapping(User user, String emsId, X509Certificate emsCert, String emsUsername) throws ObjectModelException, AccessControlException, CertificateException {
        require(user, READ, TRUSTED_EMS);
        TrustedEms trustedEms = trustedEmsManager.findByUniqueName(emsId);

        if (trustedEms == null) {
            // Need to create a new TrustedEms first.

            require(user, READ, TRUSTED_CERT);
            TrustedCert trustedCert = lookupCert(emsCert);

            if (trustedCert == null) {
                // Need to create TrustedCert first.
                trustedCert = new TrustedCert();
                trustedCert.setName("EMS Cert: " + emsId);
                trustedCert.setCertificate(emsCert);
                trustedCert.setTrustedFor(TrustedCert.TrustedFor.TRUSTED_EMS, true);

                require(user, CREATE, TRUSTED_CERT);
                trustedCert.setOid(trustedCertManager.save(trustedCert));
            }

            trustedEms = new TrustedEms();
            trustedEms.setName(emsId);
            trustedEms.setTrustedCert(trustedCert);

            require(user, CREATE, TRUSTED_EMS);
            trustedEms.setOid(trustedEmsManager.save(trustedEms));
        }

        if (!CertUtils.certsAreEqual(trustedEms.getTrustedCert().getCertificate(), emsCert)) {
            // New cert being set for this EMS instance
            // TODO should we update the existing TrustedCert?  How do we keep from stomping user data?
            // TODO or should we always add a new TrustedCert instead?  How do we generate a nice unique name?
            throw new CertificateException("There is an existing trust relationship for this EMS instance using a different certificate.");
        }

        require(user, READ, TRUSTED_EMS_USER);
        TrustedEmsUser trustedEmsUser = findByEmsUsername(trustedEms, emsUsername);
        if (trustedEmsUser != null) {
            // Another local user -- possible the same one -- already mapped as this EMS user
            throw new ConstraintViolationException("A mapping already exists for the specified user on the specified EMS instance.");
        }

        trustedEmsUser = new TrustedEmsUser();
        trustedEmsUser.setTrustedEms(trustedEms);
        trustedEmsUser.setSsgUserId(user.getId());
        trustedEmsUser.setProviderOid(user.getProviderId());
        trustedEmsUser.setEmsUserId(emsUsername);

        require(user, CREATE, TRUSTED_EMS_USER);
        return save(trustedEmsUser);
    }

    private TrustedEmsUser findByEmsUsername(TrustedEms trustedEms, String emsUsername) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("trustedEms", trustedEms);
        map.put("emsUserId", emsUsername);
        List<TrustedEmsUser> result = findMatching(map);
        if (result.size() > 1)
            throw new FindException("Found more than one user on ems " + trustedEms.getName() + " with username " + emsUsername);
        return result.size() == 1 ? result.get(0) : null;
    }
}
