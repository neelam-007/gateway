package com.l7tech.external.assertions.samlpassertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import org.junit.Test;

import static org.junit.Assert.*;

public class ProcessSamlAuthnRequestAssertionTest {
    private static final String XML_9_2 =
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <L7p:ProcessSamlAuthnRequest>\n" +
            "            <L7p:SamlProtocolBinding samlProtocolBinding=\"HttpPost\"/>\n" +
            "        </L7p:ProcessSamlAuthnRequest>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>";

    /**
     * DE220639 - Preserved backwards compatiblity to ensure the AssertionConsumerServiceURL is mandatory
     */
    @Test
    public void testAssertionConsumerServiceURLIsManatoryWhenUpgrading() throws Exception {
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(ProcessSamlAuthnRequestAssertion.class);

        WspReader wspReader = new WspReader(registry);

        // test deserialization
        AllAssertion assertion =
                (AllAssertion) wspReader.parseStrictly(XML_9_2, WspReader.INCLUDE_DISABLED);

        ProcessSamlAuthnRequestAssertion processSamlAss = (ProcessSamlAuthnRequestAssertion) assertion.getChildren().iterator().next();
        assertTrue("AssertionConsumerServiceURL should be required",
                processSamlAss.isRequiredAssertionConsumerServiceURL());
    }
}
