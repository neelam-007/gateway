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
import com.l7tech.util.Charsets;
import net.minidev.json.JSONObject;
import net.minidev.json.parser.JSONParser;
import org.json.JSONArray;
import org.json.JSONException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.util.*;

import static com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion.Transformation.XML_to_JSON;
import static com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion.TransformationConvention.STANDARD;
import static org.junit.Assert.assertEquals;

/**
 * Test the JsonTransformationAssertion.
 *
 * @noinspection FieldCanBeLocal
 */
public class ServerJsonTransformationTest {
    private static final String EXPECTED_UGLY_JSON = "{\"menu\":{\"popup\":{\"menuitem\":[{\"onclick\":\"CreateNewDoc()\",\"value\":\"New\"},{\"onclick\":\"OpenDoc()\",\"value\":\"Open\"},{\"onclick\":\"CloseDoc()\",\"value\":\"Close\"}]},\"id\":\"file\",\"value\":\"File\"}}";

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
        Map<String, String> m = new HashMap<>();
        m.put("xmlns:soapenv", "http://schemas.xmlsoap.org/soap/envelope/");
        m.put("xmlns:urn", "http://hugh:8081/axis/services/urn:EchoAttachmentsService");
        m.put("xmlns:xsd", "http://www.w3.org/2001/XMLSchema");
        m.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
        EXPECTED_XML_NAMESPACES = Collections.unmodifiableMap(m);
    }

    private static final String EXPECTED_JSON_ROUNDTRIP = "{\"root\" : {\"menu\" : {\"id\" : \"file\", \"popup\" : {\"menuitem\" : [{\"value\" : \"New\", \"onclick\" : \"CreateNewDoc()\"}, {\"value\" : \"Open\", \"onclick\" : \"OpenDoc()\"}, {\"value\" : \"Close\", \"onclick\" : \"CloseDoc()\"}]}, \"value\" : \"File\"}}}";

    private String xmlStr;
    private String jsonStr;

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

        Assert.assertEquals( result, AssertionStatus.NONE );
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
        Assert.assertEquals( result, AssertionStatus.NONE );

        // do XML->JSON
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("newXml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        ServerJsonTransformationAssertion sjta1 = buildServerAssertion(assertion);
        result = sjta1.checkRequest(context);
        Assert.assertEquals( result, AssertionStatus.NONE );

        //get expected JSON object
        JSONParser parser = new JSONParser(JSONParser.DEFAULT_PERMISSIVE_MODE);
        JSONObject expectedJson = (JSONObject)parser.parse(EXPECTED_JSON_ROUNDTRIP);

        Object outputObj = context.getVariable("target");
        JSONObject actualJson = new JSONObject((Map) ((Message) outputObj).getJsonKnob().getJsonData().getJsonObject());
        Assert.assertEquals(expectedJson, actualJson);
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

        Assert.assertEquals( result, AssertionStatus.NONE );
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

        Assert.assertEquals( result, AssertionStatus.NONE );
        Object outputObj = context.getVariable("target");
        Assert.assertTrue(outputObj instanceof Message);
        List arrayObject = (List) ((Message) outputObj).getJsonKnob().getJsonData().getJsonObject();
        JSONArray actualArray = new JSONArray(arrayObject);
        JSONArray expectedArray = new JSONArray(EXPECTED_XHTML_JSONML);
        Assert.assertEquals( expectedArray.toString(), actualArray.toString() );

    }

    private void assertJsonmlData(Map jsonMap) {
        Assert.assertTrue(jsonMap.containsKey("xmlns:urn"));
        Assert.assertTrue(jsonMap.containsKey("xmlns:soapenv"));
        Object childNodes = jsonMap.get("childNodes");
        Assert.assertTrue(childNodes instanceof ArrayList);
        Object o = ((ArrayList) childNodes).get(0);
        Assert.assertTrue(o instanceof Map);
        Map m = (Map) o;
        Assert.assertEquals( "soapenv:Header", m.get( "tagName" ) );
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
        Assert.assertEquals( result, AssertionStatus.NONE );

        // do XML->JSONML
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("newXml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);
        ServerJsonTransformationAssertion sjta1 = buildServerAssertion(assertion);
        result = sjta1.checkRequest(context);
        Assert.assertEquals( result, AssertionStatus.NONE );

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
        Assert.assertEquals( result, AssertionStatus.NONE );

        //JSONML TO XML
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("newXml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.JSON_to_XML);
        assertion.setConvention(JsonTransformationAssertion.TransformationConvention.JSONML);
        ServerJsonTransformationAssertion sjta1 = buildServerAssertion(assertion);
        result = sjta1.checkRequest(context);
        Assert.assertEquals( result, AssertionStatus.NONE );

        Object outputObj = context.getVariable("target");
        Assert.assertTrue(outputObj instanceof Message);

        XmlKnob knob = ((Message) outputObj).getXmlKnob();
        Document document = knob.getDocumentReadOnly();

        Assert.assertEquals( "soapenv:Envelope", document.getDocumentElement().getTagName() );
        //soap env namespaces...
        NamedNodeMap envAttrs = document.getDocumentElement().getAttributes();
        for (int i = 0; i < envAttrs.getLength(); ++i) {
            Node attr = envAttrs.item(i);
            String nodeName = attr.getNodeName();
            String nodeValue = attr.getNodeValue();

            String expectedValue = EXPECTED_XML_NAMESPACES.get(nodeName);
            Assert.assertTrue(expectedValue != null);
            Assert.assertEquals( expectedValue, nodeValue );
        }
        Node delay = document.getElementsByTagName("delay").item(0);
        Assert.assertEquals( "foobar!", delay.getFirstChild().getNodeValue() );
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
                    "test", false, false, false);
            Assert.assertEquals( uglyJSON, EXPECTED_UGLY_JSON );
        } catch (JSONException e) {
            Assert.fail("Error testing testJsonPrettyPrint(): " + e.getMessage());
        }
    }

    @Test
    public void testXmlToJsonNumericValues() throws Exception {
        // Basic sanity checks
        assertQuoted( true, "" );
        assertQuoted( true, "-" );
        assertQuoted( true, "a" );
        assertQuoted( false, "0" );
        assertQuoted( true, "-0" );
        assertQuoted( false, "-1" );
        assertQuoted( false, "1" );
        assertQuoted( false, "123" );
        assertQuoted( false, "-123" );
        assertQuoted( true, "NaN" );
        assertQuoted( true, "Infinity" );
        assertQuoted( true, "-Infinity" );

        // Leading zeroes require quoting
        assertQuoted( true, "0123" );
        assertQuoted( true, "00123" );
        assertQuoted( true, "00447809598454" );

        // Leading and trailing whitespace is eaten during the conversion
        assertEquals( "1", valueAsJson( "   1   ", true ) );

        // Long values should convert as numbers
        assertQuoted( false, "9223372036854775807" ); // Long.MAX_VALUE
        assertQuoted( false, "-9223372036854775808" ); // Long.MIN_VALUE

        // Too large integer must be converted as a string
        assertQuoted( true, "9223372036854775808" );  // MAX_VALUE + 1
        assertQuoted( true, "-9223372036854775809" ); // MIN_VALUE - 1

        // Double precision literals should convert as numbers
        assertQuoted( false, "2.2250738585072014E-308" ); // Double.MIN_NORMAL
        assertQuoted( false, "3.231973937965903E109" );
        assertQuoted( false, "-6.512791530968047E-264" );
        assertQuoted( false, "4.571666488570002E-56" );
        assertQuoted( false, "3.0824318140031153E-282" );
        assertQuoted( false, "3.56537449002E295" );
        assertQuoted( false, "2234.5432" );
        assertQuoted( false, "-8234144573352565" );
        assertQuoted( false, "-8.23414457335256" );
        assertQuoted( false, "-823.4144573352565" );

        // High magnitude exponents
        assertQuoted( false, "-1.94193840957016E307" );
        assertQuoted( false, "1.94193840957016E307" );
        assertQuoted( false, "-1.94193840957016E-309" );
        assertQuoted( false, "1.94193840957016E-309" );

        // Subnormal representation
        assertQuoted( false, "2.2E-322" );
        assertQuoted( false, "-2.2E-322" );

        // Leading or trailing zeroes on mantissa or exponent require quoting
        assertQuoted( true, "03.231973937965903E109" );
        assertQuoted( true, "-6.512791530968047E-064" );
        assertQuoted( true, "0.071666488570002E-56" );
        assertQuoted( true, "3.0824318140031153E-0082" );

        // CVE-2010-4476 DBL_MIN parsing bug -- WARNING: Attempting to parse this string as a double causes older JVMs to infinite loop
        assertQuoted( true, "2.2250738585072012E-308" );

        // If exponent magnitude exceeds what can be represented by Double, convert as string
        assertQuoted( true, "-1.94193840957016E308" );
        assertQuoted( true, "1.94193840957016E308" );
        assertQuoted( true, "2.2E-323" );
        assertQuoted( true, "-2.2E-323" );

        // If mantissa precision exceeds what can be represented by Double, convert as string
        assertQuoted( true,  "7.4190590630331777E177" );
        assertQuoted( true,  "73498567342362346.523562356" );

        // Conservative string-representation-preserving quoting of values that would be altered by a parseDouble() -> toString() round trip
        assertQuoted( true, "-823414457.3352" );          // would be changed into exponent notation
        assertQuoted( true, "-82341445733525.65" );       // would be changed into exponent notation
        assertQuoted( true, "-823414457335256.5" );       // would be changed into exponent notation
        assertQuoted( true, "3.0824318140031153e-282" );  // would have lowercase "e" changed to "E"
        assertQuoted( true, "-5.553308173443313e230" );   // would have lowercase "e" changed to "E"
        assertQuoted( true, "0.0" );                      // would be changed to "0"
        assertQuoted( true, "-0.0" );                     // would be changed to "-0"
    }

    @Test
    public void testRandomlyGeneratedDoubles() throws Exception {
        Random rand = new Random( 8484L );
        byte[] b = new byte[ 8 ];
        for ( int i = 0; i < 1000; ++i ) {
            rand.nextBytes( b );
            double d = ByteBuffer.wrap( b ).getDouble();
            String s = Double.toString( d );

            if ( "NaN".equals( s ) ) {
                assertQuoted( true, s );
                continue;
            }

            // Should not be quoted as it is valid output from Double.toString()
            try {
                assertQuoted( false, s );
            } catch ( Exception e ) {
                throw new AssertionError( "Failed on value: " + s + ": " + e.getMessage(), e );
            }
        }

    }

    // Assert whether a value converted to JSON with useNumbers==true gets converted as a number or a quoted string
    private void assertQuoted( boolean shouldBeQuoted, String origValue ) throws Exception {
        if ( shouldBeQuoted ) {
            assertEquals( "\"" + origValue + "\"", valueAsJson( origValue, true ) );
        } else {
            assertEquals( origValue, valueAsJson( origValue, true ) );
        }
    }

    private String valueAsJson( String value, boolean useNumbers ) throws Exception {
        String json = convert( "<a>" + value + "</a>", XML_to_JSON, STANDARD, useNumbers );
        return json.split( ":|\\}" )[1];
    }

    @Test
    public void testNumbersAlwaysQuoted() throws Exception {
        // When useNumbers == false, numeric values are always quoted even if the quotes could safely be emitted
        assertEquals( "\"0\"", valueAsJson( "0", false ) );
        assertEquals( "\"123\"", valueAsJson( "123", false ) );
        assertEquals( "\"-9223372036854775808\"", valueAsJson( "-9223372036854775808", false ) );
        assertEquals( "\"9223372036854775808\"", valueAsJson( "9223372036854775808", false ) );
        assertEquals( "\"-9223372036854775809\"", valueAsJson( "-9223372036854775809", false ) );
        assertEquals( "\"4.571666488570002E-56\"", valueAsJson( "4.571666488570002E-56", false ) );
    }

    private String convert( String input,
                            JsonTransformationAssertion.Transformation transform,
                            JsonTransformationAssertion.TransformationConvention convention,
                            boolean useNumbers ) throws Exception {
        PolicyEnforcementContext context = getContext();

        JsonTransformationAssertion ass = new JsonTransformationAssertion();
        ass.setDestinationMessageTarget( new MessageTargetableSupport( "output" ) );
        ass.setOtherTargetMessageVariable( "input" );
        ass.setTarget( TargetMessageType.OTHER );
        ass.setTransformation( transform );
        ass.setConvention( convention );
        ass.setUseNumbersWhenPossible( useNumbers );
        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("input"), false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.TEXT_DEFAULT, new ByteArrayInputStream(input.getBytes()));

        ServerJsonTransformationAssertion sass = buildServerAssertion(ass);
        AssertionStatus result = sass.checkRequest(context);

        assertEquals( result, AssertionStatus.NONE );
        Message output = (Message)context.getVariable("output");

        byte[] outBytes = output.getMimeKnob().getFirstPart().getBytesIfAvailableOrSmallerThan( 1000000 );
        return new String( outBytes, Charsets.UTF8 );
    }


}
