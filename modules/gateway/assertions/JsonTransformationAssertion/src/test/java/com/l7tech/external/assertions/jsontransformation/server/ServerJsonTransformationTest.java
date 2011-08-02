package com.l7tech.external.assertions.jsontransformation.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.ContentTypeHeader;
import com.l7tech.common.mime.StashManager;
import com.l7tech.external.assertions.jsontransformation.JsonTransformationAssertion;
import com.l7tech.message.Message;
import com.l7tech.policy.assertion.MessageTargetableSupport;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.StashManagerFactory;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.message.PolicyEnforcementContext;
import com.l7tech.server.message.PolicyEnforcementContextFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Test the JsonTransformationAssertion.
 * @noinspection FieldCanBeLocal
 */
public class ServerJsonTransformationTest {
    private String xmlStr;
    private String jsonStr;
    private Map<Object,Object> objectMap;
    private StashManager stashManager;

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

    private ServerJsonTransformationAssertion buildServerAssertion( final JsonTransformationAssertion ass ) {
        return new ServerJsonTransformationAssertion(
            ass,
            new StashManagerFactory(){
                @Override
                public StashManager createStashManager() {
                    return new ByteArrayStashManager();
                }
            }
        );
    }

    @Test
    public void testXml2Json() throws Exception {
        PolicyEnforcementContext context = getContext();

        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("target"));
        assertion.setOtherTargetMessageVariable("xml");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.XML_to_JSON);

        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("xml"),false);
        xmlMessage.initialize(new ByteArrayStashManager(), ContentTypeHeader.XML_DEFAULT, new ByteArrayInputStream(xmlStr.getBytes()));

        ServerJsonTransformationAssertion sjta = buildServerAssertion(assertion);

        AssertionStatus result = sjta.checkRequest(context);

        Assert.assertEquals(result, AssertionStatus.NONE);
        Object outputObj = context.getVariable("target");
        Assert.assertTrue(outputObj instanceof Message);
        Map mapObj = (Map) ((Message)outputObj).getJsonKnob().getJsonData().getJsonObject();
        assertJsonData(mapObj);
    }


    @Test
    public void testJsonRoundtrip() throws Exception {
        PolicyEnforcementContext context = getContext();

        // do JSON->XML
        JsonTransformationAssertion assertion = new JsonTransformationAssertion();
        assertion.setDestinationMessageTarget(new MessageTargetableSupport("newXml"));
        assertion.setRootTagString("");
        assertion.setOtherTargetMessageVariable("json");
        assertion.setTarget(TargetMessageType.OTHER);
        assertion.setTransformation(JsonTransformationAssertion.Transformation.JSON_to_XML);

        Message xmlMessage = context.getOrCreateTargetMessage(new MessageTargetableSupport("json"),false);
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
        Map mapObj = (Map) ((Message)outputObj).getJsonKnob().getJsonData().getJsonObject();
        assertJsonData(mapObj);
    }

    private void assertJsonData (Map jsonMap){
        Assert.assertTrue( jsonMap.containsKey("menu"));
        Assert.assertTrue( ((Map)jsonMap.get("menu")).containsKey("value"));
        Assert.assertTrue( ((Map)jsonMap.get("menu")).containsKey("id"));
        Assert.assertTrue( ((Map)jsonMap.get("menu")).containsKey("popup"));
        Assert.assertTrue( ((Map)((Map)jsonMap.get("menu")).get("popup")).containsKey("menuitem"));
        Assert.assertTrue( ((Map)((Map)jsonMap.get("menu")).get("popup")).get("menuitem") instanceof ArrayList);
    }
}
