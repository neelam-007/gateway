package com.l7tech.server.policy.assertion.sla;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.sla.ThroughputQuota;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * @author jbufu
 */
public class ServerThroughputQuotaTest {

    @Test
    public void testCompatibilityBug5043Format() throws Exception {
        final String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "            <L7p:ThroughputQuota>\n" +
            "                <L7p:CounterName stringValue=\"quota1\"/>\n" +
            "                <L7p:Quota longValue=\"202\"/>\n" +
            "            </L7p:ThroughputQuota>\n" +
            "    </wsp:Policy>";

        AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(ThroughputQuota.class);

        final ThroughputQuota assertion = (ThroughputQuota) wspReader.parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertTrue("Expected throughput quota 202, got '" + assertion.getQuota(), assertion.getQuota().equals("202"));
    }
    
}
