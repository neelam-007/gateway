package com.l7tech.server.policy.bundle;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.xml.soap.SoapUtil;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;

public class GatewayManagementDocumentUtilitiesTest {

    @Test
    public void testGetCreatedId() throws Exception {
        final Document doc = XmlUtil.parse(CREATED_RESPONSE);
        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(doc);
        assertNotNull(createdId);
        assertEquals(134807552L, createdId.longValue());
    }

    @Test
    public void testErrorResponse_GetCreatedId() throws Exception {
        final Document doc = XmlUtil.parse(ERROR_RESPONSE);
        final Long createdId = GatewayManagementDocumentUtilities.getCreatedId(doc);
        assertNull(createdId);
    }

    @Test
    public void testResponse_ErrorValues() throws Exception {
        final Document doc = XmlUtil.parse(ALREADY_EXISTS_RESPONSE);
        final List<String> errorDetails = GatewayManagementDocumentUtilities.getErrorDetails(doc);
        assertTrue(errorDetails.contains("env:Sender"));
        assertTrue(errorDetails.contains("wsman:AlreadyExists"));
    }

    @Test
    public void testResponse_InternalError() throws Exception {
        final Document doc = XmlUtil.parse(INTERNAL_ERROR_RESPONSE);
        GatewayManagementDocumentUtilities.getErrorDetails(doc);
        assertTrue(GatewayManagementDocumentUtilities.isInternalErrorResponse(doc));
    }

    @Test
    public void testResponse_InternalError_IsSomethingElse() throws Exception {
        final Document doc = XmlUtil.parse(CREATED_RESPONSE); // not an internal error
        assertFalse(GatewayManagementDocumentUtilities.isInternalErrorResponse(doc));
    }

    @Test
    public void testGetEntityNameFromElementFolder() throws Exception {
        String xml = "<l7:Folder xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" folderId=\"-5002\" id=\"123600896\" version=\"0\">\n" +
                "        <l7:Name>OAuth</l7:Name>\n" +
                "     </l7:Folder>";

        final Document folderDoc = XmlUtil.parse(xml);
        final Element folderElm = folderDoc.getDocumentElement();
        final String folderName = GatewayManagementDocumentUtilities.getEntityName(folderElm);
        assertEquals("Incorrect folder name found", "OAuth", folderName);
    }

    @Test
    public void testGetEntityNameFromElementPolicy() throws Exception {
        final Document entityDoc = XmlUtil.parse(POLICY_ENTITY);
        final Element detailElement = GatewayManagementDocumentUtilities.getPolicyDetailElement(entityDoc.getDocumentElement());
        final String entityName = GatewayManagementDocumentUtilities.getEntityName(detailElement);
        assertEquals("Incorrect name found", "Policy Name", entityName);
    }

    @Test
    public void testGetEntityNameFromElementService() throws Exception {
        final Document entityDoc = XmlUtil.parse(SERVICE_ENTITY);
        final Element detailElement = GatewayManagementDocumentUtilities.getServiceDetailElement(entityDoc.getDocumentElement());
        final String entityName = GatewayManagementDocumentUtilities.getEntityName(detailElement);
        assertEquals("Incorrect name found", "Service Name", entityName);
    }

    @Test
    public void testGetNamespaceMap() throws Exception {
        final Map<String,String> nsMap = GatewayManagementDocumentUtilities.getNamespaceMap();
        assertTrue(nsMap.containsKey("env"));
        assertTrue(nsMap.containsValue("http://www.w3.org/2003/05/soap-envelope"));
        assertTrue(nsMap.containsKey("wsen"));
        assertTrue(nsMap.containsValue("http://schemas.xmlsoap.org/ws/2004/09/enumeration"));
        assertTrue(nsMap.containsKey("wsman"));
        assertTrue(nsMap.containsValue("http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd"));
        assertTrue(nsMap.containsKey("l7"));
        assertTrue(nsMap.containsValue("http://ns.l7tech.com/2010/04/gateway-management"));
        assertTrue(nsMap.containsKey("L7p"));
        assertTrue(nsMap.containsValue("http://www.layer7tech.com/ws/policy"));
        assertTrue(nsMap.containsKey("wxf"));
        assertTrue(nsMap.containsValue("http://schemas.xmlsoap.org/ws/2004/09/transfer"));
        assertTrue(nsMap.containsKey("wsa"));
        assertTrue(nsMap.containsValue("http://schemas.xmlsoap.org/ws/2004/08/addressing"));

    }

    @Test
    public void testGetSelectorId() throws Exception {
        final Document doc = XmlUtil.parse(CREATED_RESPONSE);
        final List<Long> selectorId = GatewayManagementDocumentUtilities.getSelectorId(doc, false);
        assertNotNull(selectorId);
        assertFalse(selectorId.isEmpty());
        assertEquals(Long.valueOf(134807552L), selectorId.get(0));
    }

    @Test
    public void testGetMultipleSelectorId() throws Exception {
        final Document doc = XmlUtil.parse(MULTIPLE_SELECTOR_IDS);
        final List<Long> selectorIds = GatewayManagementDocumentUtilities.getSelectorId(doc, true);
        assertNotNull(selectorIds);
        assertFalse(selectorIds.isEmpty());
        assertEquals(2, selectorIds.size());
        assertEquals(Long.valueOf(32407556), selectorIds.get(0));
        assertEquals(Long.valueOf(32407555), selectorIds.get(1));
    }

    @Test(expected = GatewayManagementDocumentUtilities.UnexpectedManagementResponse.class)
    public void testGetSelectorIdMultipleNotExpected() throws Exception {
        final Document doc = XmlUtil.parse(MULTIPLE_SELECTOR_IDS);
        GatewayManagementDocumentUtilities.getSelectorId(doc, false);
    }

    public void testGetErrorDetails() throws Exception {

    }

    @Test
    public void testResponse_ResourceAlreadyExists() throws Exception {
        final Document doc = XmlUtil.parse(ALREADY_EXISTS_RESPONSE);
        assertTrue(GatewayManagementDocumentUtilities.resourceAlreadyExists(doc));
    }

    public void testResourceAlreadyExists() throws Exception {

    }

    public void testUpdatePolicyIncludes() throws Exception {

    }

    @Test
    public void testGetEntityElements() throws Exception {
        final Document response = XmlUtil.parse(SOAP_PUT_FOLDER_RESPONSE);
        final Element bodyElement = SoapUtil.getBodyElement(response);
        final List<Element> folder = GatewayManagementDocumentUtilities.getEntityElements(bodyElement, "Folder");
        assertFalse(folder.isEmpty());
        assertEquals(1, folder.size());
        assertEquals("123456789", folder.get(0).getAttribute("id"));
    }

    // - PRIVATE

    private static final String CREATED_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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

    private static final String ERROR_RESPONSE = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
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

    private final String ALREADY_EXISTS_RESPONSE = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
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

    private static final String SOAP_PUT_FOLDER_RESPONSE = "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/transfer/PutResponse</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:e09654e2-b980-461e-a062-861849909e49</wsa:MessageID>\n" +
            "        <wsa:RelatesTo>uuid:89834f79-9404-41dd-bad5-0a7f49d47255</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <l7:Folder folderId=\"-5002\" id=\"123456789\" version=\"1\">\n" +
            "            <l7:Name>Test Name</l7:Name>\n" +
            "        </l7:Folder>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";

    private final String INTERNAL_ERROR_RESPONSE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
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
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.dmtf.org/wbem/wsman/1/wsman/fault</wsa:Action>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:5b00cd48-5320-448e-80c6-88d608b6cde5</wsa:MessageID>\n" +
            "        <wsa:RelatesTo xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">uuid:d231e98c-3732-4aa5-8477-1168b01e5915</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <env:Fault xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
            "            <env:Code>\n" +
            "                <env:Value>env:Receiver</env:Value>\n" +
            "                <env:Subcode>\n" +
            "                    <env:Value>wsman:InternalError</env:Value>\n" +
            "                </env:Subcode>\n" +
            "            </env:Code>\n" +
            "            <env:Reason>\n" +
            "                <env:Text xml:lang=\"en-US\">The service cannot comply with the request due to internal processing errors.</env:Text>\n" +
            "            </env:Reason>\n" +
            "            <env:Detail/>\n" +
            "        </env:Fault>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>";

    private static final String POLICY_ENTITY = "<l7:Policy xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" guid=\"60cec430-0767-429c-8eff-62891c2eb343\"\n" +
            "               id=\"123961350\" version=\"6\">\n" +
            "        <l7:PolicyDetail folderId=\"123600897\" guid=\"60cec430-0767-429c-8eff-62891c2eb343\" id=\"123961350\" version=\"6\">\n" +
            "            <l7:Name>Policy Name</l7:Name>\n" +
            "            <l7:PolicyType>Include</l7:PolicyType>\n" +
            "            <l7:Properties>\n" +
            "                <l7:Property key=\"revision\">\n" +
            "                    <l7:LongValue>7</l7:LongValue>\n" +
            "                </l7:Property>\n" +
            "                <l7:Property key=\"soap\">\n" +
            "                    <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                </l7:Property>\n" +
            "            </l7:Properties>\n" +
            "        </l7:PolicyDetail>\n" +
            "        <l7:Resources>\n" +
            "            <l7:ResourceSet tag=\"policy\">\n" +
            "                <l7:Resource type=\"policy\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
            "                    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
            "                    xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
            "                    &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
            "                    &lt;L7p:AuditDetailAssertion&gt;\n" +
            "                    &lt;L7p:Detail stringValue=\"Test audit\"/&gt;\n" +
            "                    &lt;/L7p:AuditDetailAssertion&gt;\n" +
            "                    &lt;/wsp:All&gt;\n" +
            "                    &lt;/wsp:Policy&gt;\n" +
            "                </l7:Resource>\n" +
            "            </l7:ResourceSet>\n" +
            "        </l7:Resources>\n" +
            "    </l7:Policy>\n";

    private static final String SERVICE_ENTITY = "<l7:Service xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\" id=\"123797510\" version=\"2\">\n" +
            "        <l7:ServiceDetail folderId=\"123600899\" id=\"123797510\" version=\"2\">\n" +
            "            <l7:Name>Service Name</l7:Name>\n" +
            "            <l7:Enabled>true</l7:Enabled>\n" +
            "            <l7:ServiceMappings>\n" +
            "                <l7:HttpMapping>\n" +
            "                    <l7:UrlPattern>/serviceurl</l7:UrlPattern>\n" +
            "                    <l7:Verbs>\n" +
            "                        <l7:Verb>GET</l7:Verb>\n" +
            "                        <l7:Verb>POST</l7:Verb>\n" +
            "                    </l7:Verbs>\n" +
            "                </l7:HttpMapping>\n" +
            "            </l7:ServiceMappings>\n" +
            "            <l7:Properties>\n" +
            "                <l7:Property key=\"policyRevision\">\n" +
            "                    <l7:LongValue>3</l7:LongValue>\n" +
            "                </l7:Property>\n" +
            "                <l7:Property key=\"wssProcessingEnabled\">\n" +
            "                    <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                </l7:Property>\n" +
            "                <l7:Property key=\"soap\">\n" +
            "                    <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                </l7:Property>\n" +
            "                <l7:Property key=\"internal\">\n" +
            "                    <l7:BooleanValue>false</l7:BooleanValue>\n" +
            "                </l7:Property>\n" +
            "            </l7:Properties>\n" +
            "        </l7:ServiceDetail>\n" +
            "        <l7:Resources>\n" +
            "            <l7:ResourceSet tag=\"policy\">\n" +
            "                <l7:Resource type=\"policy\" version=\"2\">&lt;?xml version=\"1.0\" encoding=\"UTF-8\"?&gt;\n" +
            "                    &lt;wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\"\n" +
            "                    xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\"&gt;\n" +
            "                    &lt;wsp:All wsp:Usage=\"Required\"&gt;\n" +
            "                    &lt;L7p:CommentAssertion&gt;\n" +
            "                    &lt;L7p:Comment stringValue=\"Just a comment\"/&gt;\n" +
            "                    &lt;/L7p:CommentAssertion&gt;\n" +
            "                    &lt;/wsp:All&gt;\n" +
            "                    &lt;/wsp:Policy&gt;\n" +
            "                </l7:Resource>\n" +
            "            </l7:ResourceSet>\n" +
            "        </l7:Resources>\n" +
            "    </l7:Service>";

    private static final String MULTIPLE_SELECTOR_IDS = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<env:Envelope xmlns:env=\"http://www.w3.org/2003/05/soap-envelope\"\n" +
            "    xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\"\n" +
            "    xmlns:mdo=\"http://schemas.wiseman.dev.java.net/metadata/messagetypes\"\n" +
            "    xmlns:mex=\"http://schemas.xmlsoap.org/ws/2004/09/mex\"\n" +
            "    xmlns:wsa=\"http://schemas.xmlsoap.org/ws/2004/08/addressing\"\n" +
            "    xmlns:wse=\"http://schemas.xmlsoap.org/ws/2004/08/eventing\"\n" +
            "    xmlns:wsen=\"http://schemas.xmlsoap.org/ws/2004/09/enumeration\"\n" +
            "    xmlns:wsman=\"http://schemas.dmtf.org/wbem/wsman/1/wsman.xsd\"\n" +
            "    xmlns:wsmeta=\"http://schemas.dmtf.org/wbem/wsman/1/wsman/version1.0.0.a/default-addressing-model.xsd\"\n" +
            "    xmlns:wxf=\"http://schemas.xmlsoap.org/ws/2004/09/transfer\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\">\n" +
            "    <env:Header>\n" +
            "        <wsa:Action env:mustUnderstand=\"true\" xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">http://schemas.xmlsoap.org/ws/2004/09/enumeration/EnumerateResponse</wsa:Action>\n" +
            "        <wsman:TotalItemsCountEstimate>21</wsman:TotalItemsCountEstimate>\n" +
            "        <wsa:MessageID env:mustUnderstand=\"true\">uuid:a57dc958-fc31-45f9-b526-3085803d68fa</wsa:MessageID>\n" +
            "        <wsa:RelatesTo>uuid:10522123-a11f-4ec1-b698-58496ab89c3a</wsa:RelatesTo>\n" +
            "        <wsa:To env:mustUnderstand=\"true\">http://schemas.xmlsoap.org/ws/2004/08/addressing/role/anonymous</wsa:To>\n" +
            "    </env:Header>\n" +
            "    <env:Body>\n" +
            "        <wsen:EnumerateResponse>\n" +
            "            <wsen:Expires>2147483647-12-31T23:59:59.999-14:00</wsen:Expires>\n" +
            "            <wsen:EnumerationContext>2422903f-0d6b-44b2-9104-daa7a38e9dbe</wsen:EnumerationContext>\n" +
            "            <wsman:Items>\n" +
            "                <wsman:Item>\n" +
            "                    <wsman:XmlFragment>\n" +
            "                        <l7:UrlPattern>/oauth/manager</l7:UrlPattern>\n" +
            "                    </wsman:XmlFragment>\n" +
            "                    <wsa:EndpointReference>\n" +
            "                        <wsa:Address env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:Address>\n" +
            "                        <wsa:ReferenceParameters>\n" +
            "                            <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI>\n" +
            "                            <wsman:SelectorSet>\n" +
            "                                <wsman:Selector Name=\"id\">32407556</wsman:Selector>\n" +
            "                            </wsman:SelectorSet>\n" +
            "                        </wsa:ReferenceParameters>\n" +
            "                    </wsa:EndpointReference>\n" +
            "                </wsman:Item>\n" +
            "                <wsman:Item>\n" +
            "                    <wsman:XmlFragment>\n" +
            "                        <l7:UrlPattern>/oauth/manager</l7:UrlPattern>\n" +
            "                    </wsman:XmlFragment>\n" +
            "                    <wsa:EndpointReference>\n" +
            "                        <wsa:Address env:mustUnderstand=\"true\">http://localhost:8080/wsman</wsa:Address>\n" +
            "                        <wsa:ReferenceParameters>\n" +
            "                            <wsman:ResourceURI>http://ns.l7tech.com/2010/04/gateway-management/services</wsman:ResourceURI>\n" +
            "                            <wsman:SelectorSet>\n" +
            "                                <wsman:Selector Name=\"id\">32407555</wsman:Selector>\n" +
            "                            </wsman:SelectorSet>\n" +
            "                        </wsa:ReferenceParameters>\n" +
            "                    </wsa:EndpointReference>\n" +
            "                </wsman:Item>\n" +
            "            </wsman:Items>\n" +
            "            <wsman:EndOfSequence/>\n" +
            "        </wsen:EnumerateResponse>\n" +
            "    </env:Body>\n" +
            "</env:Envelope>\n";
}
