package com.l7tech.policy.assertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import org.junit.Test;

import static org.junit.Assert.*;

public class HardcodedResponseAssertionTest {
    /**
     * Prior to Fangtooth, response status was stored as an int.
     */
    @Test
    public void ensureBackwardsCompatibilityPreFangtooth() throws Exception {
        // note response status is an int
        final String policyXmlPreFangtooth = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">\n" +
                "        <L7p:HardcodedResponse>\n" +
                "            <L7p:Base64ResponseBody stringValue=\"PG1lc3NhZ2U+dGVzdDwvbWVzc2FnZT4=\"/>\n" +
                "            <L7p:ResponseStatus intValue=\"400\"/>\n" +
                "        </L7p:HardcodedResponse>\n" +
                "    </wsp:All>\n" +
                "</wsp:Policy>";
        final AssertionRegistry assreg = new AssertionRegistry();
        assreg.registerAssertion(HardcodedResponseAssertion.class);
        WspConstants.setTypeMappingFinder(assreg);
        final AllAssertion allAssertion = (AllAssertion) WspReader.getDefault().parseStrictly(policyXmlPreFangtooth, WspReader.INCLUDE_DISABLED);

        final HardcodedResponseAssertion assertion = (HardcodedResponseAssertion) allAssertion.getChildren().get(0);
        assertEquals("400", assertion.getResponseStatus());
    }
}
