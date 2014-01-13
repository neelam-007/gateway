package com.l7tech.policy.compatibility;

import com.l7tech.policy.variable.DataType;
import junit.framework.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Tests whether current version of {@link DataType} is backward compatible.<br/>
 * The test will try to de-serialize a Halibut version of {@link DataType} stream, contained within {@link #serialBase64Encoded}.
 */
public class DataTypeBackwardsCompatibilityTest extends BaseBackwardsCompatibility<DataType> {
    /////////////////////////////////////////////////////////////////////////////////////////////////////////
    // DO NOT MODIFY THIS VALUE!
    private static final String serialBase64Encoded = "rO0ABXNyAEhjb20ubDd0ZWNoLnBvbGljeS5jb21wYXRpYmlsaXR5LkJhc2VCYWNrd2FyZHNDb21wYXRpYmlsaXR5JFRlc3RBc3NlcnRpb26T46Zp37cT4gIAAUwACmN1c3RvbURhdGF0ABJMamF2YS9sYW5nL09iamVjdDt4cHNyACNjb20ubDd0ZWNoLnBvbGljeS52YXJpYWJsZS5EYXRhVHlwZW/On13BNPTMAgADTAAEbmFtZXQAEkxqYXZhL2xhbmcvU3RyaW5nO0wACXNob3J0TmFtZXEAfgAEWwAMdmFsdWVDbGFzc2VzdAASW0xqYXZhL2xhbmcvQ2xhc3M7eHB0ABFYLjUwOSBDZXJ0aWZpY2F0ZXQABGNlcnR1cgASW0xqYXZhLmxhbmcuQ2xhc3M7qxbXrsvNWpkCAAB4cAAAAAF2cgAiamF2YS5zZWN1cml0eS5jZXJ0Llg1MDlDZXJ0aWZpY2F0Zd1tvKg37534AgAAeHIAHmphdmEuc2VjdXJpdHkuY2VydC5DZXJ0aWZpY2F0Zc499MTyCAobAgABTAAEdHlwZXEAfgAEeHA=";
    /////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Test
    public void testDataType() throws Exception {
        Assert.assertNotNull(base64ToObject(serialBase64Encoded).getCustomData());
    }

    /**
     * The following unit-test generated the {@link #serialBase64Encoded} value.
     */
    @Ignore
    @Test
    public void generateInitialSerialisation() throws Exception {
        final DataType testDataType = DataType.CERTIFICATE;
        System.out.println(objectToBase64(createAssertion(testDataType)));
    }
}
