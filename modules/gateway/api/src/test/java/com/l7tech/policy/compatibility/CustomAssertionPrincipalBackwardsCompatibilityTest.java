package com.l7tech.policy.compatibility;

import com.l7tech.policy.assertion.ext.CustomAssertionPrincipal;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests whether current version of {@link CustomAssertionPrincipal} is backward compatible.<br/>
 * The test will try to de-serialize a Halibut version of {@link CustomAssertionPrincipal} stream, contained within {@link #serialBase64Encoded}.
 */
public class CustomAssertionPrincipalBackwardsCompatibilityTest extends BaseBackwardsCompatibility<CustomAssertionPrincipal> {
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DO NOT MODIFY THIS VALUE!
    private static final String serialBase64Encoded = "rO0ABXNyAEhjb20ubDd0ZWNoLnBvbGljeS5jb21wYXRpYmlsaXR5LkJhc2VCYWNrd2FyZHNDb21wYXRpYmlsaXR5JFRlc3RBc3NlcnRpb26T46Zp37cT4gIAAUwACmN1c3RvbURhdGF0ABJMamF2YS9sYW5nL09iamVjdDt4cHNyADhjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LkN1c3RvbUFzc2VydGlvblByaW5jaXBhbLkIqNY7jz04AgABTAAEbmFtZXQAEkxqYXZhL2xhbmcvU3RyaW5nO3hwdAADZm9v";
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testCustomAssertionPrincipal() throws Exception {
        Assert.assertNotNull(base64ToObject(serialBase64Encoded).getCustomData());
    }
}
