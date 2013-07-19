package com.l7tech.server.policy;

import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.GuidEntityHeader;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.GoidEntityManagerStub;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class MockEncapsulatedAssertionConfigManager extends GoidEntityManagerStub<EncapsulatedAssertionConfig,GuidEntityHeader> implements EncapsulatedAssertionConfigManager {
    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> findByPolicyOid(long policyOid) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public EncapsulatedAssertionConfig findByGuid(String guid) throws FindException {
        return findByHeader(new GuidEntityHeader(guid, EntityType.ENCAPSULATED_ASSERTION, null, null));
    }
}
