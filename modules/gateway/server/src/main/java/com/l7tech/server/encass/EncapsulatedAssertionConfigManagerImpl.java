package com.l7tech.server.encass;

import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.HibernateEntityManager;
import com.l7tech.server.policy.variable.EncapsulatedAssertionConfigManager;

/**
 * Entity manager for {@link EncapsulatedAssertionConfig}.
 */
public class EncapsulatedAssertionConfigManagerImpl extends HibernateEntityManager<EncapsulatedAssertionConfig,EntityHeader> implements EncapsulatedAssertionConfigManager {
    @Override
    public Class<? extends Entity> getImpClass() {
        return EncapsulatedAssertionConfig.class;
    }
}
