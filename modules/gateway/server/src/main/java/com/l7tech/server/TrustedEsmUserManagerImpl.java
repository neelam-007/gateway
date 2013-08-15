package com.l7tech.server;

import com.l7tech.gateway.common.esmtrust.TrustedEsm;
import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;
import com.l7tech.server.security.rbac.RoleManager;
import com.l7tech.util.ConfigFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Entity manager for {@link com.l7tech.gateway.common.esmtrust.TrustedEsmUser}.
 *
 * TODO ensure user mappings get delete when the corresponding user is deleted.
 */
@Transactional(propagation= Propagation.REQUIRED, rollbackFor=Throwable.class)
public class TrustedEsmUserManagerImpl extends HibernateEntityManager<TrustedEsmUser, EntityHeader> implements TrustedEsmUserManager {
    private static final Logger logger = Logger.getLogger(TrustedEsmUserManagerImpl.class.getName());
    private static final boolean PERMIT_MAPPING_UPDATE = ConfigFactory.getBooleanProperty("com.l7tech.server.remotetrust.permitMappingUpdate", true);

    @Inject
    private TrustedEsmManager trustedEsmManager;

    @Inject
    private RoleManager roleManager;


    @Override
    public Class<? extends Entity> getImpClass() {
        return TrustedEsmUser.class;
    }

    @Override
    public Class<? extends Entity> getInterfaceClass() {
        return TrustedEsmUser.class;
    }

    @Override
    protected UniqueType getUniqueType() {
        return UniqueType.NONE;
    }

    @Override
    public String getTableName() {
        return "trusted_esm_user";
    }

    @Override
    public TrustedEsmUser configureUserMapping(User user, String esmId, X509Certificate esmCert, String esmUsername, String esmUserDisplayName)
            throws ObjectModelException, AccessControlException, CertificateException, CertificateMismatchException, MappingAlreadyExistsException
    {
        if (user == null)
            throw new IllegalArgumentException("Missing authenticated user");
        if (esmId == null || esmId.trim().length() < 1)
            throw new IllegalArgumentException("Missing or malformed ESM ID");
        if (esmCert == null)
            throw new IllegalArgumentException("Missing ESM certificate");
        if (esmUsername == null || esmUsername.trim().length() < 1)
            throw new IllegalArgumentException("Missing or malformed esmUsername");

        TrustedEsm trustedEsm = trustedEsmManager.getOrCreateEsmAssociation(user, esmId, esmCert);

        TrustedEsmUser trustedEsmUser = findByEsmUsername(trustedEsm, esmUsername);
        if (trustedEsmUser != null) {
            // Another local user -- possible the same one -- already mapped as this ESM user
            if ( !PERMIT_MAPPING_UPDATE )
                throw new MappingAlreadyExistsException("A mapping already exists for the specified user on the specified ESM instance.");

            //if not mapping to a different user, then we just return
            if (trustedEsmUser.getSsgUserId().equals(user.getId()) && trustedEsmUser.getProviderGoid().equals(user.getProviderId()))
                return trustedEsmUser;

            if (esmUserDisplayName != null && trustedEsmUser.getEsmUserDisplayName() == null)
                trustedEsmUser.setEsmUserDisplayName(esmUserDisplayName);

            try {
                //delete the old mapping
                delete(trustedEsmUser);
            } catch (DeleteException de) {
                //fall through to create the new mapping?
            }
        }

        trustedEsmUser = new TrustedEsmUser();
        trustedEsmUser.setTrustedEsm(trustedEsm);
        trustedEsmUser.setSsgUserId(user.getId());
        trustedEsmUser.setProviderGoid(user.getProviderId());
        trustedEsmUser.setEsmUserId(esmUsername);
        trustedEsmUser.setEsmUserDisplayName(esmUserDisplayName);

        long oid = save(trustedEsmUser);
        trustedEsmUser.setOid(oid);

        return trustedEsmUser;
    }

    @Override
    public boolean deleteMappingsForUser(User user) throws FindException, DeleteException {
        Map<String, Object> map = new HashMap<String, Object>();        
        map.put("ssgUserId", user.getId());
        map.put("providerGoid", user.getProviderId());

        boolean didDelete = false;
        List<TrustedEsmUser> found = findMatching(Arrays.asList(map));
        for (TrustedEsmUser trustedEsmUser : found) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Deleting ESM user mapping for user {0} ({1} on ESM {2})", new Object[] {
                        trustedEsmUser.getSsgUserId(), trustedEsmUser.getEsmUserId(), trustedEsmUser.getTrustedEsm().getName() });
            delete(trustedEsmUser);
            didDelete = true;
        }

        return didDelete;
    }

    @Override
    public boolean deleteMappingsForIdentityProvider(Goid identityProviderOid) throws FindException, DeleteException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("providerGoid", identityProviderOid);

        boolean didDelete = false;
        List<TrustedEsmUser> found = findMatching(Arrays.asList(map));
        for (TrustedEsmUser trustedEsmUser : found) {
            if (logger.isLoggable(Level.INFO))
                logger.log(Level.INFO, "Deleting ESM user mapping for user {0} on ESM {1}", new Object[] { trustedEsmUser, trustedEsmUser.getTrustedEsm().getId() });
            delete(trustedEsmUser);
            didDelete = true;
        }

        return didDelete;
    }

    @Override
    public Collection<TrustedEsmUser> findByEsmId(long trustedEsmOid) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("trustedEsm.oid", trustedEsmOid);
        return findMatching(Arrays.asList(map));
    }

    @Override
    public TrustedEsmUser findByEsmIdAndUserUUID(final long trustedEsmOid, final String esmUuid) throws FindException {
        TrustedEsmUser user = null;

        Map<String, Object> map = new HashMap<String, Object>();
        map.put("trustedEsm.oid", trustedEsmOid);
        map.put("esmUserId", esmUuid);
        Collection<TrustedEsmUser> users = findMatching(Arrays.asList(map));
        if ( users.size()==1 ) {
            user = users.iterator().next();
        } else if ( users.size() > 1 ) {
            logger.warning("Multiple users are mapped with identity '"+esmUuid+"', for esm '"+trustedEsmOid+"'.");
        }

        return user;
    }

    private TrustedEsmUser findByEsmUsername(TrustedEsm trustedEsm, String esmUsername) throws FindException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("trustedEsm", trustedEsm);
        map.put("esmUserId", esmUsername);
        List<TrustedEsmUser> result = findMatching(Arrays.asList(map));
        if (result.size() > 1)
            throw new FindException("Found more than one user on esm " + trustedEsm.getName() + " with username " + esmUsername);
        return result.size() == 1 ? result.get(0) : null;
    }

    @Override
    public void delete(long oid) throws DeleteException, FindException {
        findAndDelete(oid);
    }
}
