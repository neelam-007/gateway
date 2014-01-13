package com.l7tech.policy.compatibility;

import com.l7tech.policy.assertion.ext.targetable.CustomMessageTargetableSupport;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests whether current version of {@link CustomMessageTargetableSupport} is backward compatible.<br/>
 * The test will try to de-serialize a Halibut version of {@link CustomMessageTargetableSupport} stream, contained within {@link #serialBase64Encoded}.
 */
public class CustomMessageTargetableSupportBackwardsCompatibilityTest extends BaseBackwardsCompatibility<CustomMessageTargetableSupport> {
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DO NOT MODIFY THIS VALUE!
    private static final String serialBase64Encoded = "rO0ABXNyAEhjb20ubDd0ZWNoLnBvbGljeS5jb21wYXRpYmlsaXR5LkJhc2VCYWNrd2FyZHNDb21wYXRpYmlsaXR5JFRlc3RBc3NlcnRpb26T46Zp37cT4gIAAUwACmN1c3RvbURhdGF0ABJMamF2YS9sYW5nL09iamVjdDt4cHNyAEljb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LnRhcmdldGFibGUuQ3VzdG9tTWVzc2FnZVRhcmdldGFibGVTdXBwb3J0nHfxc2QN5WoCAANaABNzb3VyY2VVc2VkQnlHYXRld2F5WgAXdGFyZ2V0TW9kaWZpZWRCeUdhdGV3YXlMABV0YXJnZXRNZXNzYWdlVmFyaWFibGV0ABJMamF2YS9sYW5nL1N0cmluZzt4cAABdAADZm9v";
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testCustomMessageTargetableSupport() throws Exception {
        Assert.assertNotNull(base64ToObject(serialBase64Encoded).getCustomData());
    }

    /**
     * The following unit-test generated the {@link #serialBase64Encoded} value.
     */
    @Ignore
    @Test
    public void generateInitialSerialisation() throws Exception {
        final CustomMessageTargetableSupport testCustomMessageTargetableSupport = new CustomMessageTargetableSupport("foo", true);
        testCustomMessageTargetableSupport.setSourceUsedByGateway(false);
        System.out.println(objectToBase64(createAssertion(testCustomMessageTargetableSupport)));
    }
}
