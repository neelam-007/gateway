package com.l7tech.external.assertions.rawtcp;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.wsp.WspReader;
import com.l7tech.test.BugNumber;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;

public class SimpleRawTransportAssertionTest {

    @Test
    @BugNumber(9621)
    public void testBackwardCompatibilityForPortType() throws IOException {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<exp:Export Version=\"3.0\"\n" +
                "    xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
                "    xmlns:exp=\"http://www.layer7tech.com/ws/policy/export\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <exp:References/>\n" +
                "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "        <wsp:All wsp:Usage=\"Required\">\n" +
                "            <L7p:SimpleRawTransport>\n" +
                "                <L7p:TargetHost stringValue=\"localhost\"/>\n" +
                "                <L7p:TargetPort intValue=\"9999\"/>\n" +
                "            </L7p:SimpleRawTransport>\n" +
                "        </wsp:All>\n" +
                "    </wsp:Policy>\n" +
                "</exp:Export>\n";
        AssertionRegistry registry = new AssertionRegistry();
        registry.registerAssertion(SimpleRawTransportAssertion.class);
        WspReader wspr = new WspReader(registry);
        AllAssertion assertion = (AllAssertion) wspr.parsePermissively(xml, WspReader.Visibility.includeDisabled);
        assertEquals("9999", ((SimpleRawTransportAssertion) assertion.getChildren().get(0)).getTargetPort());

    }
}
