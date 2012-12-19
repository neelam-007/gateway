package com.l7tech.server.encass;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Entity manager for {@link EncapsulatedAssertionConfig}.
 */
public class EncapsulatedAssertionConfigManagerImpl extends HibernateEntityManager<EncapsulatedAssertionConfig,EntityHeader> implements EncapsulatedAssertionConfigManager {

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
}
