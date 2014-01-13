package com.l7tech.policy.compatibility;

import com.l7tech.policy.variable.VariableMetadata;
import junit.framework.Assert;
import org.junit.Test;

/**
 * Tests whether current version of {@link VariableMetadata} is backward compatible.<br/>
 * The test will try to de-serialize a Halibut version of {@link VariableMetadata} stream, contained within {@link #serialBase64Encoded}.
 */
public class VariableMetadataBackwardsCompatibilityTest extends BaseBackwardsCompatibility<VariableMetadata> {
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DO NOT MODIFY THIS VALUE!
    private static final String serialBase64Encoded = "rO0ABXNyAEhjb20ubDd0ZWNoLnBvbGljeS5jb21wYXRpYmlsaXR5LkJhc2VCYWNrd2FyZHNDb21wYXRpYmlsaXR5JFRlc3RBc3NlcnRpb26T46Zp37cT4gIAAUwACmN1c3RvbURhdGF0ABJMamF2YS9sYW5nL09iamVjdDt4cHNyACtjb20ubDd0ZWNoLnBvbGljeS52YXJpYWJsZS5WYXJpYWJsZU1ldGFkYXRh+RUF8GSHuLkCAAdaAAttdWx0aXZhbHVlZFoACHByZWZpeGVkWgAIc2V0dGFibGVMAA1jYW5vbmljYWxOYW1ldAASTGphdmEvbGFuZy9TdHJpbmc7TAAEbmFtZXEAfgAETAAKcmVwbGFjZWRCeXEAfgAETAAEdHlwZXQAJUxjb20vbDd0ZWNoL3BvbGljeS92YXJpYWJsZS9EYXRhVHlwZTt4cAEBAXQAA0ZPT3QAA2Zvb3QAA2JhcnNyACNjb20ubDd0ZWNoLnBvbGljeS52YXJpYWJsZS5EYXRhVHlwZW/On13BNPTMAgADTAAEbmFtZXEAfgAETAAJc2hvcnROYW1lcQB+AARbAAx2YWx1ZUNsYXNzZXN0ABJbTGphdmEvbGFuZy9DbGFzczt4cHQAB01lc3NhZ2V0AAdtZXNzYWdldXIAEltMamF2YS5sYW5nLkNsYXNzO6sW167LzVqZAgAAeHAAAAABdnIAEGphdmEubGFuZy5PYmplY3QAAAAAAAAAAAAAAHhw";
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testVariableMetadata() throws Exception {
        Assert.assertNotNull(base64ToObject(serialBase64Encoded).getCustomData());
    }
}
