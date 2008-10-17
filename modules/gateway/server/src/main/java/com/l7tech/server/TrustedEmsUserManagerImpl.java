package com.l7tech.server;

import com.l7tech.gateway.common.emstrust.TrustedEms;
import com.l7tech.gateway.common.emstrust.TrustedEmsUser;
import com.l7tech.objectmodel.EntityType;
import static com.l7tech.objectmodel.EntityType.TRUSTED_EMS_USER;
import com.l7tech.gateway.common.security.rbac.OperationType;
import static com.l7tech.gateway.common.security.rbac.OperationType.CREATE;
import static com.l7tech.gateway.common.security.rbac.OperationType.READ;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity manager for {@link com.l7tech.gateway.common.emstrust.TrustedEmsUser}.
 *
 * TODO ensure user mappings get delete when the corresponding user is deleted.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedEmsUserManagerImpl extends HibernateEntityManager<TrustedEmsUser, EntityHeader> implements TrustedEmsUserManager {
    private static final Logger logger = Logger.getLogger(TrustedEmsUserManagerImpl.class.getName());

    @Resource
    private TrustedEmsManager trustedEmsManager;

    @Resource
    private RoleManager roleManager;


    public Class<? extends Entity> getImpClass() {
        return TrustedEmsUser.class;
    }

    public Class<? extends Entity> getInterfaceClass() {
        return TrustedEmsUser.class;
    }

    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    public String getTableName() {
        return "trusted_ems_user";
    }

    private void require(User user, OperationType op, EntityType ent) throws ObjectModelException, AccessControlException {
        if (!roleManager.isPermittedForAnyEntityOfType(user, op, ent))
            throw new AccessControlException("Permission denied: " + op + " " + ent);
    }


    public TrustedEmsUser configureUserMapping(User user, String emsId, X509Certificate emsCert, String emsUsername) throws ObjectModelException, AccessControlException, CertificateException
    {
        if (user == null)
            throw new IllegalArgumentException("Missing authenticated user");
        if (emsId == null || emsId.trim().length() < 1)
            throw new IllegalArgumentException("Missing or malformed EMS ID");
        if (emsCert == null)
            throw new IllegalArgumentException("Missing EMS certificate");
        if (emsUsername == null || emsUsername.trim().length() < 1)
            throw new IllegalArgumentException("Missing or malformed emsUsername");

        TrustedEms trustedEms = trustedEmsManager.getOrCreateEmsAssociation(user, emsId, emsCert);

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
        long oid = save(trustedEmsUser);
        trustedEmsUser.setOid(oid);
        return trustedEmsUser;
    }

    public boolean deleteMappingsForUser(User user) throws FindException, DeleteException {
        Map<String, Object> map = new HashMap<String, Object>();        
        map.put("ssgUserId", user.getId());
        map.put("providerOid", user.getProviderId());

        boolean didDelete = false;
        List<TrustedEmsUser> found = findMatching(Arrays.asList(map));
        for (TrustedEmsUser trustedEmsUser : found) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Deleting EMS user mapping for user {0} ({1} on EMS {2})", new Object[] {
                        trustedEmsUser.getSsgUserId(), trustedEmsUser.getEmsUserId(), trustedEmsUser.getTrustedEms().getName() });
            delete(trustedEmsUser);
            didDelete = true;
        }

        return didDelete;
    }

    public boolean deleteMappingsForIdentityProvider(long identityProviderOid) throws FindException, DeleteException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("providerOid", identityProviderOid);

        boolean didDelete = false;
        List<TrustedEmsUser> found = findMatching(Arrays.asList(map));
        for (TrustedEmsUser trustedEmsUser : found) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Deleting EMS user mapping for user {0} on EMS {1}", new Object[] { trustedEmsUser, trustedEmsUser.getTrustedEms().getId() });
            delete(trustedEmsUser);
            didDelete = true;
        }

        return didDelete;
    }

    private TrustedEmsUser findByEmsUsername(TrustedEms trustedEms, String emsUsername) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("trustedEms", trustedEms);
        map.put("emsUserId", emsUsername);
        List<TrustedEmsUser> result = findMatching(Arrays.asList(map));
        if (result.size() > 1)
            throw new FindException("Found more than one user on ems " + trustedEms.getName() + " with username " + emsUsername);
        return result.size() == 1 ? result.get(0) : null;
    }
}
