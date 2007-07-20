package com.l7tech.server.identity.cert;

import java.util.Collection;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.HibernateEntityManager;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.common.security.RevocationCheckPolicy;

/**
 * Manager for Revocation Check Policies.
 * 
 * @author Steve Jones
 * @see com.l7tech.common.security.RevocationCheckPolicy RevocationCheckPolicy
 */
public class RevocationCheckPolicyManagerImpl
        extends HibernateEntityManager<RevocationCheckPolicy, EntityHeader>
        implements RevocationCheckPolicyManager {

    //- PUBLIC

    /**
     * Persist a new RevocationCheckPolicy.
     *
     * <p>If the given policy is the new default then the existing default
     * policy is updated.</p>
     *
     * @param revocationCheckPolicy The new policy to save
     * @return The oid of the entity.
     * @throws SaveException if the save failed
     */
    public long save(RevocationCheckPolicy revocationCheckPolicy) throws SaveException {
        long oid = super.save(revocationCheckPolicy);
        try {
            updateDefault(oid, revocationCheckPolicy);
        } catch( ObjectModelException ome) {
            throw new SaveException(ome);
        }
        return oid;
    }

    /**
     * Update an RevocationCheckPolicy in persistent storage.
     *
     * <p>If the given policy is the default then any existing default
     * policy is updated.</p>
     *
     * @param revocationCheckPolicy The policy to persist
     * @throws UpdateException if the update failed
     */
    public void update(RevocationCheckPolicy revocationCheckPolicy) throws UpdateException {
        super.update(revocationCheckPolicy);
        try {
            updateDefault(revocationCheckPolicy.getOid(), revocationCheckPolicy);
        } catch( ObjectModelException ome) {
            throw new UpdateException(ome);
        }
    }

    public Class getImpClass() {
        return RevocationCheckPolicy.class;
    }

    public Class getInterfaceClass() {
        return RevocationCheckPolicy.class;
    }

    public String getTableName() {
        return TABLE_NAME;
    }

    //- PROTECTED

    //- PRIVATE

    private static final String TABLE_NAME = "rcp";

    /**
     * Update any policies flagged as default.
     */
    private void updateDefault(long oid, RevocationCheckPolicy revocationCheckPolicy) throws FindException, UpdateException {
        // set default
        if ( revocationCheckPolicy.isDefaultPolicy() ) {
            Collection<RevocationCheckPolicy> policies =  findAll();
            for (RevocationCheckPolicy policy : policies) {
                if (policy.isDefaultPolicy() && policy.getOid()!=oid) {
                    policy.setDefaultPolicy(false);
                    update(policy);
                }
            }
        }
    }
}
