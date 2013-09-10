package com.l7tech.policy.exporter;

import com.l7tech.gateway.common.security.rbac.OperationType;
import com.l7tech.gateway.common.security.rbac.PermissionDeniedException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.policy.assertion.Include;
import com.l7tech.test.BugId;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class IncludedPolicyReferenceTest {
    private static final String POLICY_GUID = "abc123";
    private IncludedPolicyReference reference;
    private Include include;
    @Mock
    private ExternalReferenceFinder finder;

    @Before
    public void setup() {
        include = new Include();
        include.setPolicyGuid(POLICY_GUID);
        reference = new IncludedPolicyReference(finder, include);
    }

    @BugId("SSG-7622")
    @Test(expected = PermissionDeniedException.class)
    public void verifyReferencePermissionDenied() throws Exception {
        when(finder.findPolicyByGuid(POLICY_GUID)).thenThrow(new PermissionDeniedException(OperationType.READ, EntityType.POLICY));
        reference.verifyReference();
    }
}
