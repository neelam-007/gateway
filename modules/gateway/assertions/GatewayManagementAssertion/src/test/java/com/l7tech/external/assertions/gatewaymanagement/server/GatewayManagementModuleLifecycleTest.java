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
            "        <wsp:OneOrMore wsp:Usage=\"Required\">\n" +
            "            <L7p:CommentAssertion>\n" +
            "                <L7p:Comment stringValue=\"Check the request message size. If the maximum message size needs to be increased change the 'restman.request.message.maxSize' cluster property\"/>\n" +
            "            </L7p:CommentAssertion>\n" +
            "            <L7p:RequestSizeLimit>\n" +
            "                <L7p:Limit stringValue=\"${gateway.restman.request.message.maxSize}\"/>\n" +
            "            </L7p:RequestSizeLimit>\n" +
            "            <wsp:All wsp:Usage=\"Required\">\n" +
            "                <L7p:CustomizeErrorResponse>\n" +
            "                    <L7p:Content stringValueReference=\"inline\"><![CDATA[<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<l7:Error xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "    <l7:Type>InvalidRequest</l7:Type>\n" +
            "    <l7:Detail>the specified maximum data size limit would be exceeded</l7:Detail>\n" +
            "</l7:Error>]]></L7p:Content>\n" +
            "                    <L7p:ContentType stringValue=\"application/xml; charset=UTF-8\"/>\n" +
            "                    <L7p:HttpStatus stringValue=\"400\"/>\n" +
            "                </L7p:CustomizeErrorResponse>\n" +
            "                <L7p:FalseAssertion/>\n" +
            "            </wsp:All>\n" +
            "        </wsp:OneOrMore>\n" +
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
