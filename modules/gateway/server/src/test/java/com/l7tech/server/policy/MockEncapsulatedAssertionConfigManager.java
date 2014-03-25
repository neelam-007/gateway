package com.l7tech.server.policy;

import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.server.EntityManagerStub;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 *
 */
public class MockEncapsulatedAssertionConfigManager extends EntityManagerStub<EncapsulatedAssertionConfig,ZoneableGuidEntityHeader> implements EncapsulatedAssertionConfigManager {
    @NotNull
    @Override
    public Collection<EncapsulatedAssertionConfig> findByPolicyGoid(Goid policyOid) throws FindException {
        return Collections.emptyList();
    }

    @Override
    public EncapsulatedAssertionConfig findByGuid(String guid) throws FindException {
        return findByHeader(new ZoneableGuidEntityHeader(guid, EntityType.ENCAPSULATED_ASSERTION, null, null, null));
    }
}
