package com.l7tech.server;

import com.l7tech.gateway.common.esmtrust.TrustedEsmUser;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.*;

import java.security.AccessControlException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class TrustedEsmUserManagerStub extends EntityManagerStub<TrustedEsmUser, EntityHeader> implements TrustedEsmUserManager {

    @Override
    public TrustedEsmUser configureUserMapping(User user, String esmId, X509Certificate esmCert, String esmUsername, String esmUserDisplayName) throws ObjectModelException, AccessControlException, CertificateException, CertificateMismatchException, MappingAlreadyExistsException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public boolean deleteMappingsForUser(User user) throws FindException, DeleteException {
        List<TrustedEsmUser> found = new ArrayList< TrustedEsmUser>();
        for(TrustedEsmUser esmUser: entities.values()){
            if(user.getId().equals(esmUser.getSsgUserId())){
                found.add(esmUser);
            }
        }

        boolean didDelete = false;
        for(TrustedEsmUser esmUser: found){
            delete(esmUser);
            didDelete = true;
        }

        return didDelete;
    }

    @Override
    public boolean deleteMappingsForIdentityProvider(Goid identityProviderOid) throws FindException, DeleteException {
        List<TrustedEsmUser> found = new ArrayList< TrustedEsmUser>();
        for(TrustedEsmUser esmUser: entities.values()){
            if(identityProviderOid.equals(esmUser.getProviderGoid())){
                found.add(esmUser);
            }
        }

        boolean didDelete = false;
        for(TrustedEsmUser esmUser: found){
            delete(esmUser);
            didDelete = true;
        }

        return didDelete;
    }

    @Override
    public Collection<TrustedEsmUser> findByEsmId(Goid trustedEsmGoid) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }

    @Override
    public TrustedEsmUser findByEsmIdAndUserUUID(Goid trustedEsmGoid, String esmUsername) throws FindException {
        throw new UnsupportedOperationException("Not yet implemented for stub");
    }
}
