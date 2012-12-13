package com.l7tech.external.assertions.jsontransformation.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion;
import com.l7tech.message.Message;
import com.l7tech.message.XmlKnob;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Test the JsonTransformationAssertion.
 *
 * @noinspection FieldCanBeLocal
 */
public class ServerJsonTransformationTest {
    private static final String EXPECTED_UGLY_JSON = "{\"menu\":{\"id\":\"file\",\"popup\":{\"menuitem\":" +
            "[{\"value\":\"New\",\"onclick\":\"CreateNewDoc()\"},{\"value\":\"Open\",\"onclick\":\"OpenDoc()\"}," +
            "{\"value\":\"Close\",\"onclick\":\"CloseDoc()\"}]},\"value\":\"File\"}}";

    private static final String EXPECTED_UGLY_XML = "<test><menu><id>file</id><popup><menuitem><value>New</value" +
            "><onclick>CreateNewDoc()</onclick></menuitem><menuitem><value>Open</value><onclick>OpenDoc()</onclick>" +
            "</menuitem><menuitem><value>Close</value><onclick>CloseDoc()</onclick></menuitem></popup><value>File" +
            "</value></menu></test>";

    private static final String SOAP_XML = "<soapenv:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " +
            "xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" " +
            "xmlns:urn=\"http://hugh:8081/axis/services/urn:EchoAttachmentsService\">\n" +
            "   <soapenv:Header/>\n" +
            "   <soapenv:Body>\n" +
            "      <urn:echoOne soapenv:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
            "         <delay xsi:type=\"xsd:long\">foobar!</delay>\n" +
            "      </urn:echoOne>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static final String JSONML = "{\n" +
            "    \"xmlns:xsd\": \"http://www.w3.org/2001/XMLSchema\",\n" +
            "    \"xmlns:urn\": \"http://hugh:8081/axis/services/urn:EchoAttachmentsService\",\n" +
            "    \"tagName\": \"soapenv:Envelope\",\n" +
            "    \"xmlns:xsi\": \"http://www.w3.org/2001/XMLSchema-instance\",\n" +
            "    \"childNodes\": [\n" +
            "        {\"tagName\": \"soapenv:Header\"},\n" +
            "        {\n" +
            "            \"tagName\": \"soapenv:Body\",\n" +
            "            \"childNodes\": [{\n" +
            "                \"tagName\": \"urn:echoOne\",\n" +
            "                \"childNodes\": [{\n" +
            "                    \"tagName\": \"delay\",\n" +
            "                    \"childNodes\": [\"foobar!\"],\n" +
            "                    \"xsi:type\": \"xsd:long\"\n" +
            "                }],\n" +
            "                \"soapenv:encodingStyle\": \"http://schemas.xmlsoap.org/soap/encoding/\"\n" +
            "            }]\n" +
            "        }\n" +
            "    ],\n" +
            "    \"xmlns:soapenv\": \"http://schemas.xmlsoap.org/soap/envelope/\"\n" +
            "}";

    private static final Map<String, String> EXPECTED_XML_NAMESPACES;

    static {
        Map<String, String> m = new HashMap<String, String>();
        m.put("xmlns:soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        m.put("xmlns:urn", "http://hugh:8081/axis/services/urn:EchoAttachmentsService");
        m.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
        m.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        EXPECTED_XML_NAMESPACES = Collections.unmodifiableMap(m);
    }

    private static final String EXPECTED_JSON_ROUNDTRIP = "{root={menu={id=file, popup={menuitem=[{value=New, onclick=CreateNewDoc()}, {value=Open, onclick=OpenDoc()}, {value=Close, onclick=CloseDoc()}]}, value=File}}}";

    private String xmlStr;
    private String jsonStr;
    private Map<Object, Object> objectMap;
    private StashManager stashManager;

    private static final String TEST_XHTML_STRING = "<ul>\n" +
            "<li style=\"color:red\">First Item</li>\n" +
            "<li title=\"Some hover text.\" style=\"color:green\">\n" +
            "Second Item\n" +
            "</li>\n" +
            "<li><span class=\"code-example-third\">Third</span>\n" +
            "Item</li>\n" +
            "</ul>";

    private static final String EXPECTED_XHTML_JSONML = "[\"ul\",[\"li\",{\"style\":\"color:red\"},\"First Item\"],[\"li\",{\"title\":\"Some hover text.\",\"style\":\"color:green\"},\"Second Item\"],[\"li\",[\"span\",{\"class\":\"code-example-third\"},\"Third\"],\"Item\"]]";

    @Before
    public void setUp() throws Exception {
        xmlStr = "<menu id=\"file\" value=\"File\">\n" +
                "  <popup>\n" +
                "    <menuitem value=\"New\" onclick=\"CreateNewDoc()\" />\n" +
                "    <menuitem value=\"Open\" onclick=\"OpenDoc()\" />\n" +
                "    <menuitem value=\"Close\" onclick=\"CloseDoc()\" />\n" +
                "  </popup>\n" +
                "</menu>";

        jsonStr = "{\"menu\": {\n" +
                "  \"id\": \"file\",\n" +
                "  \"value\": \"File\",\n" +
                "  \"popup\": {\n" +
                "    \"menuitem\": [\n" +
                "      {\"value\": \"New\", \"onclick\": \"CreateNewDoc()\"},\n" +
                "      {\"value\": \"Open\", \"onclick\": \"OpenDoc()\"},\n" +
                "      {\"value\": \"Close\", \"onclick\": \"CloseDoc()\"}\n" +
                "    ]\n" +
                "  }\n" +
                "}}";

        objectMap = new HashMap<Object, Object>();
    }

    private PolicyEnforcementContext getContext() {
        return PolicyEnforcementContextFactory.createPolicyEnforcementContext(new Message(), new Message());
    }

    private ServerJsonTransformationAssertion buildServerAssertion(final JsonTransformationAssertion ass) {
        return new ServerJsonTransformationAssertion(
                ass,
                new StashManagerFactory() {
                    @Override
                    public StashManager createStashManager() {
                        return new ByteArrayStashManager();
                    }
                }
        );
    }

    @Test
    public void testXml2JsonStandardConvention() throws Exception {
        PolicyEnforcementContext context = getContext();

        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("xml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.STANDARD);
        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("xml"), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(xmlStr.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);

        AssertionStatus result = sjta.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable("target");
        Assert.assertTrue(outputObj instanceof Message);
        Map mapObj = (Map) ((Message) outputObj).getJsonKnob().getJsonData().getJsonObject();
        assertJsonData(mapObj);
    }


    @Test
    public void testJsonRoundtripStandardConvention() throws Exception {
        PolicyEnforcementContext context = getContext();

        // do JSON->XML
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("newXml"));
        assertion.setRootTagString("root");
        assertion.setOtherTargetMessageVariable("json");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.JSON_to_XML);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.STANDARD);

        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("json"), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(jsonStr.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);
        AssertionStatus result = sjta.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.NONE);

        // do XML->JSON
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("newXml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        ServerJsonTransformationAssertion sjta1 = buildServerAssertion(assertion);
        result = sjta1.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.NONE);

        Object outputObj = context.getVariable("target");
        Map mapObj = (Map) ((Message) outputObj).getJsonKnob().getJsonData().getJsonObject();
        Assert.assertEquals(EXPECTED_JSON_ROUNDTRIP, mapObj.toString());
    }

    private void assertJsonData(Map jsonMap) {
        Assert.assertTrue(jsonMap.containsKey("menu"));
        Assert.assertTrue(((Map) jsonMap.get("menu")).containsKey("value"));
        Assert.assertTrue(((Map) jsonMap.get("menu")).containsKey("id"));
        Assert.assertTrue(((Map) jsonMap.get("menu")).containsKey("popup"));
        Assert.assertTrue(((Map) ((Map) jsonMap.get("menu")).get("popup")).containsKey("menuitem"));
        Assert.assertTrue(((Map) ((Map) jsonMap.get("menu")).get("popup")).get("menuitem") instanceof ArrayList);
    }

    @Test
    public void testXmlToJsonml() throws Exception {
        PolicyEnforcementContext context = getContext();
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("xml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);
        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("xml"), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(SOAP_XML.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);

        AssertionStatus result = sjta.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable("target");
        Assert.assertTrue(outputObj instanceof Message);
        Map mapObj = (Map) ((Message) outputObj).getJsonKnob().getJsonData().getJsonObject();
        assertJsonmlData(mapObj);
    }

    @Test
    public void testXmlToJsonmlAsArray() throws Exception {
        PolicyEnforcementContext context = getContext();
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("xml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);
        assertion.setArrayForm(true);
        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("xml"), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(TEST_XHTML_STRING.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);

        AssertionStatus result = sjta.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable("target");
        Assert.assertTrue(outputObj instanceof Message);
        List arrayObject = (List) ((Message) outputObj).getJsonKnob().getJsonData().getJsonObject();
        JSONArray actualArray = new JSONArray(arrayObject);
        JSONArray expectedArray = new JSONArray(EXPECTED_XHTML_JSONML);
        Assert.assertEquals(expectedArray.toString(), actualArray.toString());

    }

    private void assertJsonmlData(Map jsonMap) {
        Assert.assertTrue(jsonMap.containsKey("xmlns:urn"));
        Assert.assertTrue(jsonMap.containsKey("xmlns:soapenv"));
        Object childNodes = jsonMap.get("childNodes");
        Assert.assertTrue(childNodes instanceof ArrayList);
        Object o = ((ArrayList) childNodes).get(0);
        Assert.assertTrue(o instanceof Map);
        Map m = (Map) o;
        Assert.assertEquals("soapenv:Header", m.get("tagName"));
    }

    @Test
    public void testJsonmlToXmlRoundTrip() throws Exception {
        PolicyEnforcementContext context = getContext();

        // do JSONML->XML
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("newXml"));
        assertion.setRootTagString("root");
        assertion.setOtherTargetMessageVariable("json");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.JSON_to_XML);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);

        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("json"), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(JSONML.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);
        AssertionStatus result = sjta.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.NONE);

        // do XML->JSONML
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("newXml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);
        ServerJsonTransformationAssertion sjta1 = buildServerAssertion(assertion);
        result = sjta1.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.NONE);

        Object outputObj = context.getVariable("target");
        Map mapObj = (Map) ((Message) outputObj).getJsonKnob().getJsonData().getJsonObject();
        assertJsonmlData(mapObj);
    }

    @Test
    public void testXmlToJsonmlRoundTrip() throws Exception {
        PolicyEnforcementContext context = getContext();

        //XML TO JSONML
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("newXml"));
        assertion.setRootTagString("root");
        assertion.setOtherTargetMessageVariable("xml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);

        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("xml"), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(SOAP_XML.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);
        AssertionStatus result = sjta.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.NONE);

        //JSONML TO XML
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("newXml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.JSON_to_XML);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);
        ServerJsonTransformationAssertion sjta1 = buildServerAssertion(assertion);
        result = sjta1.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.NONE);

        Object outputObj = context.getVariable("target");
        Assert.assertTrue(outputObj instanceof Message);

        XmlKnob knob = ((Message) outputObj).getXmlKnob();
        Document document = knob.getDocumentReadOnly();

        Assert.assertEquals("soapenv:Envelope", document.getDocumentElement().getTagName());
        //soap env namespaces...
        NamedNodeMap envAttrs = document.getDocumentElement().getAttributes();
        for (int i = 0; i < envAttrs.getLength(); ++i) {
            Node attr = envAttrs.item(i);
            String nodeName = attr.getNodeName();
            String nodeValue = attr.getNodeValue();

            String expectedValue = EXPECTED_XML_NAMESPACES.get(nodeName);
            Assert.assertTrue(expectedValue != null);
            Assert.assertEquals(expectedValue, nodeValue);
        }
        Node delay = document.getElementsByTagName("delay").item(0);
        Assert.assertEquals("foobar!", delay.getFirstChild().getNodeValue());
    }

    @Test
    public void testFailedXmlTransformation() throws Exception {
        PolicyEnforcementContext context = getContext();
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("newXml"));
        assertion.setRootTagString("root");
        assertion.setOtherTargetMessageVariable("xml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);

        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("xml"), false);
        //test by giving it a JSON (or anything other than a well-formed xml) and it should fail
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(JSONML.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);
        AssertionStatus result = sjta.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.FAILED);
    }

    @Test
    public void testFailedJsonTransformation() throws Exception {
        PolicyEnforcementContext context = getContext();
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("newXml"));
        assertion.setRootTagString("root");
        assertion.setOtherTargetMessageVariable("testJSON");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.JSON_to_XML);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);

        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("testJSON"), false);
        //source is supposed to be a JSON string, give it a xml string or anything else and it should fail
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.APPLICATION_JSON, new ByteArrayInputStream(SOAP_XML.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);
        AssertionStatus result = sjta.checkRequest(context);
        Assert.assertEquals(result, AssertionStatus.FAILED);
    }

    @Test
    public void testJsonPrettyPrintFalse() {
        try {
            String uglyJSON = ServerJsonTransformationAssertion.doTransformation(xmlStr,
                    JsonTransformationAssertion.Transformation.XML_to_JSON,
                    JsonTransformationAssertion.TransformationConvention.STANDARD,
                    "test", false, false);
            Assert.assertEquals(uglyJSON, EXPECTED_UGLY_JSON);
        } catch (JSONException e) {
            Assert.fail("Error testing testJsonPrettyPrint(): " + e.getMessage());
        }
    }

}
