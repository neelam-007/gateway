package com.l7tech.server.identity.cert;

import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.objectmodel.*;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Restrictions;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Manager for Revocation Check Policies.
 * 
 * @author Steve Jones
 * @see com.l7tech.gateway.common.security.RevocationCheckPolicy RevocationCheckPolicy
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
    public Goid save(RevocationCheckPolicy revocationCheckPolicy) throws SaveException {
        Goid oid = super.save(revocationCheckPolicy);
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
            updateDefault(revocationCheckPolicy.getGoid(), revocationCheckPolicy);
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

    @Override
    public void updateDefault(Goid oid, RevocationCheckPolicy revocationCheckPolicy) throws FindException, UpdateException {
        // set default
        if ( revocationCheckPolicy.isDefaultPolicy() ) {
            Collection<RevocationCheckPolicy> policies =  findAll();
            for (RevocationCheckPolicy policy : policies) {
                if (policy.isDefaultPolicy() && !policy.getGoid().equals(oid)) {
                    policy.setDefaultPolicy(false);
                    update(policy);
                }
            }
        }
    }

    private final DetachedCriteria getDefaultCriteria =
            DetachedCriteria.forClass(getImpClass()).add(Restrictions.eq("defaultPolicy", Boolean.TRUE));

    @Override
    public RevocationCheckPolicy getDefaultPolicy() throws FindException {
        return getHibernateTemplate().execute(new ReadOnlyHibernateCallback<RevocationCheckPolicy>() {
            protected RevocationCheckPolicy doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                return (RevocationCheckPolicy) getDefaultCriteria.getExecutableCriteria(session).uniqueResult();
            }
        });
    }
}
