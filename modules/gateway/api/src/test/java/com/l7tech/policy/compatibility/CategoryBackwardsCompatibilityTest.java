package com.l7tech.policy.compatibility;

import com.l7tech.policy.assertion.ext.Category;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests whether current version of {@link Category} is backward compatible.<br/>
 * The test will try to de-serialize a Halibut version of {@link Category} stream, contained within {@link #serialBase64Encoded}.
 */
public class CategoryBackwardsCompatibilityTest extends BaseBackwardsCompatibility<Category> {
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DO NOT MODIFY THIS VALUE!
    private static final String serialBase64Encoded = "rO0ABXNyAEhjb20ubDd0ZWNoLnBvbGljeS5jb21wYXRpYmlsaXR5LkJhc2VCYWNrd2FyZHNDb21wYXRpYmlsaXR5JFRlc3RBc3NlcnRpb26T46Zp37cT4gIAAUwACmN1c3RvbURhdGF0ABJMamF2YS9sYW5nL09iamVjdDt4cHNyAChjb20ubDd0ZWNoLnBvbGljeS5hc3NlcnRpb24uZXh0LkNhdGVnb3J5WrCcZaFE/jUCAAJJAAVteUtleUwABm15TmFtZXQAEkxqYXZhL2xhbmcvU3RyaW5nO3hwAAAACnQAC1BvbGljeUxvZ2lj";
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testCategory() throws Exception {
        Assert.assertNotNull(base64ToObject(serialBase64Encoded).getCustomData());
    }
}
