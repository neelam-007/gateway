package com.l7tech.policy.assertion.xmlsec;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.test.BugId;
import org.junit.Assert;
import org.junit.Test;

public class WssConfigurationAssertionTest {
    private final String JAVELIN_DEFAULT_ASSERTION_WITH_ADD_TIMESTAMP_CHECKED_AND_SIGN_TIMESTAMP_CHECKED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "        <L7p:WssConfiguration>\n" +
                    "            <L7p:KeyReference stringValue=\"BinarySecurityToken\"/>\n" +
                    "            <L7p:ProtectTokens booleanValue=\"false\"/>\n" +
                    "            <L7p:Target target=\"REQUEST\"/>\n" +
                    "        </L7p:WssConfiguration>\n" +
                    "</wsp:Policy>";

    private final String JAVELIN_DEFAULT_ASSERTION_WITH_ADD_TIMESTAMP_UNCHECKED_AND_SIGN_TIMESTAMP_CHECKED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "        <L7p:WssConfiguration>\n" +
                    "            <L7p:AddTimestamp booleanValue=\"false\"/>\n" +
                    "            <L7p:KeyReference stringValue=\"BinarySecurityToken\"/>\n" +
                    "            <L7p:ProtectTokens booleanValue=\"false\"/>\n" +
                    "            <L7p:Target target=\"REQUEST\"/>\n" +
                    "        </L7p:WssConfiguration>\n" +
                    "</wsp:Policy>";

    private final String JAVELIN_DEFAULT_ASSERTION_WITH_ADD_TIMESTAMP_CHECKED_AND_SIGN_TIMESTAMP_UNCHECKED =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                    "        <L7p:WssConfiguration>\n" +
                    "            <L7p:KeyReference stringValue=\"BinarySecurityToken\"/>\n" +
                    "            <L7p:ProtectTokens booleanValue=\"false\"/>\n" +
                    "            <L7p:SignTimestamp booleanValue=\"false\"/>\n" +
                    "            <L7p:Target target=\"REQUEST\"/>\n" +
                    "        </L7p:WssConfiguration>\n" +
                    "</wsp:Policy>";

    @BugId("SSG-11379")
    @Test
    public void testSerialization_ParseJavelinPolicyWithAddTimestampAndSignTimeStampAsTrue() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(WssConfigurationAssertion.class);
        WspReader wspReader = new WspReader(registry);

        WssConfigurationAssertion assertion = (WssConfigurationAssertion) wspReader.parseStrictly(
                JAVELIN_DEFAULT_ASSERTION_WITH_ADD_TIMESTAMP_CHECKED_AND_SIGN_TIMESTAMP_CHECKED, WspReader.INCLUDE_DISABLED);

        Assert.assertEquals(true, assertion.isAddTimestamp());
        Assert.assertEquals(true, assertion.isSignTimestamp());
    }

    @BugId("SSG-11379")
    @Test
    public void testSerialization_ParseJavelinPolicyWithAddTimestampFalseAndSignTimeStampAsTrue() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(WssConfigurationAssertion.class);
        WspReader wspReader = new WspReader(registry);

        WssConfigurationAssertion assertion = (WssConfigurationAssertion) wspReader.parseStrictly(
                JAVELIN_DEFAULT_ASSERTION_WITH_ADD_TIMESTAMP_UNCHECKED_AND_SIGN_TIMESTAMP_CHECKED, WspReader.INCLUDE_DISABLED);

        Assert.assertEquals(false, assertion.isAddTimestamp());
        Assert.assertEquals(true, assertion.isSignTimestamp());
    }

    @BugId("SSG-11379")
    @Test
    public void testSerialization_ParseJavelinPolicyWithAddTimestampTrueAndSignTimeStampAsFalse() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(WssConfigurationAssertion.class);
        WspReader wspReader = new WspReader(registry);

        WssConfigurationAssertion assertion = (WssConfigurationAssertion) wspReader.parseStrictly(
                JAVELIN_DEFAULT_ASSERTION_WITH_ADD_TIMESTAMP_CHECKED_AND_SIGN_TIMESTAMP_UNCHECKED, WspReader.INCLUDE_DISABLED);

        Assert.assertEquals(true, assertion.isAddTimestamp());
        Assert.assertEquals(false, assertion.isSignTimestamp());
    }
}
