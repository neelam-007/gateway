package com.l7tech.server.policy;

import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class MockEncapsulatedAssertionConfigManager extends EntityManagerStub<EncapsulatedAssertionConfig,EntityHeader> implements EncapsulatedAssertionConfigManager {
    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> findByPolicyOid(long policyOid) throws FindException {
        return Collections.emptyList();
    }
}
