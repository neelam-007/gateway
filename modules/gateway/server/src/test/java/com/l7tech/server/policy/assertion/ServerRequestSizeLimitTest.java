package com.l7tech.server.policy.assertion;

import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.RequestSizeLimit;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.policy.wsp.WspReader;
import junit.framework.TestCase;
import org.junit.Test;

/**
 * @author jbufu
 */
public class ServerRequestSizeLimitTest extends TestCase {

    @Test
    public void testCompatibilityBug5044Format() throws Exception {
        final String policyXml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "    <wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "            <L7p:RequestSizeLimit>\n" +
            "                <L7p:Limit longValue=\"4896768\"/>\n" +
            "            </L7p:RequestSizeLimit>\n" +
            "    </wsp:Policy>\n";

        AssertionRegistry tmf = new AssertionRegistry();
        tmf.setApplicationContext(null);
        WspConstants.setTypeMappingFinder(tmf);
        WspReader wspReader = new WspReader(tmf);
        tmf.registerAssertion(RequestSizeLimit.class);

        final RequestSizeLimit assertion = (RequestSizeLimit) wspReader.parseStrictly(policyXml, WspReader.INCLUDE_DISABLED);
        assertTrue("Expected request size limit 4782 kbytes, got '" + assertion.getLimit(), assertion.getLimit().equals("4782"));
    }

}
