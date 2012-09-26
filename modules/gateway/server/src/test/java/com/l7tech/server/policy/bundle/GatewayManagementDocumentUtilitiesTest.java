package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import org.junit.Test;
import org.w3c.dom.Document;

import java.util.List;

import static org.junit.Assert.*;

public class GatewayManagementDocumentUtilitiesTest {

    @Test
    public void testGetCreatedId() throws Exception {
        final Document doc = XmlUtil.parse(createdResponse);
        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(doc);
        assertNotNull(createdId);
        assertEquals(134807552L, createdId.longValue());
    }

    @Test
    public void testErrorResponse_GetCreatedId() throws Exception {
        final Document doc = XmlUtil.parse(errorResponse);
        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(doc);
        assertNull(createdId);
    }

    @Test
    public void testResponse_ErrorValues() throws Exception {
        final Document doc = XmlUtil.parse(alreadyExistsResponse);
        final List<String> errorDetails = GatewayManagementDocumentUtilities.getErrorDetails(doc);
        assertTrue(errorDetails.contains("env:Sender"));
        assertTrue(errorDetails.contains("wsman:AlreadyExists"));
    }

    public void testGetNamespaceMap() throws Exception {

    }

    public void testGetSelectorId() throws Exception {

    }


    public void testGetErrorDetails() throws Exception {

    }

    @Test
    public void testResponse_ResourceAlreadyExists() throws Exception {
        final Document doc = XmlUtil.parse(alreadyExistsResponse);
        assertTrue(GatewayManagementDocumentUtilities.resourceAlreadyExists(doc));
    }

    public void testResourceAlreadyExists() throws Exception {

    }

    public void testUpdatePolicyIncludes() throws Exception {

    }

    // - PRIVATE

    final String createdResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/transfer/CreateResponse</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:ce1f79e3-479d-4602-a93c-230bfe0f6050</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:73442d63-37ee-4908-8d18-c635e327d515</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wxf:ResourceCreated xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <wsa:Address env:mustUnderstand=\"true\">https://localhost:9443/wsman/</wsa:Address>\n" +
            "            <wsa:ReferenceParameters>\n" +
            "                <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/folders</wsman:ResourceURI>\n" +
            "                <wsman:SelectorSet>\n" +
            "                    <wsman:Selector Name=\"id\">134807552</wsman:Selector>\n" +
            "                </wsman:SelectorSet>\n" +
            "            </wsa:ReferenceParameters>\n" +
            "        </wxf:ResourceCreated>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    private final static String errorResponse = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/transfer/fault</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:e17c4e90-8693-4a9f-ac8d-c00a25d06e0f</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:7f1dd1be-120e-435c-9cff-c7e2d2f93e00</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <env:Fault xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <env:Code>\n" +
            "                <env:Value>env:Sender</env:Value>\n" +
            "                <env:Subcode>\n" +
            "                    <env:Value>wxf:InvalidRepresentation</env:Value>\n" +
            "                </env:Subcode>\n" +
            "            </env:Code>\n" +
            "            <env:Reason>\n" +
            "                <env:Text xml:lang=\"en-US\">The XML content was invalid.</env:Text>\n" +
            "            </env:Reason>\n" +
            "            <env:Detail>\n" +
            "                <env:Text xml:lang=\"en-US\">Resource validation failed due to 'INVALID_VALUES' invalid parent folder</env:Text>\n" +
            "                <wsman:FaultDetail>http://schemas.dmtf.org/wbem/wsman/1/wsman/faultDetail/InvalidValues</wsman:FaultDetail>\n" +
            "            </env:Detail>\n" +
            "        </env:Fault>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";

    final String alreadyExistsResponse = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.dmtf.org/wbem/wsman/1/wsman/fault</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:7486aa54-f144-4656-badf-16d9570fc37d</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:6a947b0a-415d-490d-a1d1-fcf57a2ba329</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <env:Fault xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <env:Code>\n" +
            "                <env:Value>env:Sender</env:Value>\n" +
            "                <env:Subcode>\n" +
            "                    <env:Value>wsman:AlreadyExists</env:Value>\n" +
            "                </env:Subcode>\n" +
            "            </env:Code>\n" +
            "            <env:Reason>\n" +
            "                <env:Text xml:lang=\"en-US\">The sender attempted to create a resource which already exists.</env:Text>\n" +
            "            </env:Reason>\n" +
            "            <env:Detail>\n" +
            "                <env:Text xml:lang=\"en-US\">(folder, name)  must be unique</env:Text>\n" +
            "            </env:Detail>\n" +
            "        </env:Fault>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

}
