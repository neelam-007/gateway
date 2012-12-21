package com.l7tech.server.encass;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.util.ReadOnlyHibernateCallback;
import org.hibernate.Criteria;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Collection;

/**
 * Entity manager for {@link EncapsulatedAssertionConfig}.
 */
public class EncapsulatedAssertionConfigManagerImpl extends HibernateEntityManager<EncapsulatedAssertionConfig,GuidEntityHeader> implements EncapsulatedAssertionConfigManager {

    private static final String HQL_FIND_ENCASS_CONFIGS_REFERENCING_POLICY_OID =
        "from encapsulated_assertion" +
            " in class " + EncapsulatedAssertionConfig.class.getName() +
            " where encapsulated_assertion.policy.oid = ?";


    @Override
    public Class<? extends Entity> getImpClass() {
        return EncapsulatedAssertionConfig.class;
    }

    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> findByPolicyOid(long policyOid) throws FindException {
        //noinspection unchecked
        return (Collection<EncapsulatedAssertionConfig>)getHibernateTemplate().find(HQL_FIND_ENCASS_CONFIGS_REFERENCING_POLICY_OID, policyOid);
    }

    @Override
    public EncapsulatedAssertionConfig findByHeader(final EntityHeader header) throws FindException {
        if ( header instanceof GuidEntityHeader && ((GuidEntityHeader)header).getGuid() != null ) {
            return findByGuid( ((GuidEntityHeader)header).getGuid() );
        } else {
            return super.findByHeader( header );
        }
    }

    @Override
    public EncapsulatedAssertionConfig findByGuid(final String guid) throws FindException {
        try {
            //noinspection unchecked
            return (EncapsulatedAssertionConfig)getHibernateTemplate().execute(new ReadOnlyHibernateCallback() {
                @Override
                protected Object doInHibernateReadOnly(Session session) throws HibernateException, SQLException {
                    Criteria crit = session.createCriteria(getImpClass());
                    crit.add(Restrictions.eq("guid", guid));
                    return crit.uniqueResult();
                }
            });
        } catch (Exception e) {
            throw new FindException("Couldn't check uniqueness", e);
        }
    }
}
