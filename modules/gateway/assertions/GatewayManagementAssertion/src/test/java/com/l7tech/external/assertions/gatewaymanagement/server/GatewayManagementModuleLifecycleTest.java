package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.gateway.common.service.ServiceTemplate;
import com.l7tech.gateway.common.service.ServiceType;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.wsp.WspConstants;
import com.l7tech.test.BugId;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author alee, 12/1/2014
 */
public class GatewayManagementModuleLifecycleTest {
    private static final String DEFAULT_RESTMAN_POLICY = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
            "    <wsp:All wsp:Usage=\"Required\">\n" +
            "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
            "            <L7p:SslAssertion>\n" +
            "                <L7p:RequireClientAuthentication booleanValue=\"true\"/>\n" +
            "            </L7p:SslAssertion>\n" +
            "            <wsp:All wsp:Usage=\"Required\">\n" +
            "                <L7p:SslAssertion/>\n" +
            "                <L7p:HttpBasic/>\n" +
            "            </wsp:All>\n" +
            "        </wsp:OneOrMore>\n" +
            "        <L7p:Authentication>\n" +
            "            <L7p:IdentityProviderOid goidValue=\"0000000000000000fffffffffffffffe\"/>\n" +
            "        </L7p:Authentication>\n" +
            "        <L7p:RESTGatewayManagement/>\n" +
            "    </wsp:All>\n" +
            "</wsp:Policy>\n";

    @BugId("SSG-9887")
    @Test
    public void createRestServiceTemplate() {
        WspConstants.setTypeMappingFinder(new AssertionRegistry());
        final ServiceTemplate template = GatewayManagementModuleLifecycle.createRestServiceTemplate();
        assertEquals("Gateway REST Management Service", template.getName());
        assertEquals("/restman/*", template.getDefaultUriPrefix());
        assertEquals(ServiceType.OTHER_INTERNAL_SERVICE, template.getType());
        assertEquals(DEFAULT_RESTMAN_POLICY, template.getDefaultPolicyXml());
    }
}
