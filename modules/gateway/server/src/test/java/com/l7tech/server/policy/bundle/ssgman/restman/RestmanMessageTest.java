package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Test the methods of a Restman message.
 */
public class RestmanMessageTest {
    private final static String ERROR_RESPONSE_XML = "<l7:Error xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n"+
            "   <l7:Type>BadRequest</l7:Type>\n"+
            "   <l7:TimeStamp>2014-11-10T12:26:46.275-08:00</l7:TimeStamp>\n"+
            "   <l7:Link rel=\"self\" uri=\"https://tluong-pc.l7tech.local:8443/restman/1.0/bundle?versionComment=MyComment\"/>\n"+
            "   <l7:Detail>HTTP 400 Bad Request. Caused by: The prefix \"l7\" for element \"l7:Bundle\" is not bound.</l7:Detail>\n"+
            "</l7:Error>";
    private final String validRequestXml;
    private final String errorMappingResponseXml;

    public RestmanMessageTest() throws IOException {
        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/server/policy/bundle/bundles/RestmanBundle1/MigrationBundle1.0.xml"));
        validRequestXml = new String(bytes, RestmanInvoker.UTF_8);

        bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/server/policy/bundle/ssgman/restman/MigrationBundleMappingErrorResponse.xml"));
        errorMappingResponseXml = new String(bytes, RestmanInvoker.UTF_8);
    }

    @Test
    public void errorResponse() throws Exception {
        RestmanMessage responseMessage = new RestmanMessage(XmlUtil.stringToDocument(ERROR_RESPONSE_XML));

        assertTrue(responseMessage.isErrorResponse());

        // error response is different than mapping error
        assertFalse(responseMessage.hasMappingError());
        assertEquals(0, responseMessage.getMappingErrors().size());

        // xml in should be same as xml out
        assertEquals(ERROR_RESPONSE_XML, responseMessage.getAsString());
    }

    @Test
    public void validRequest() throws Exception {
        final RestmanMessage requestMessage = new RestmanMessage(XmlUtil.stringToDocument(validRequestXml));

        // xml in should be same as xml out.  note: newline inserted by XmlUtil.nodeToString(...) is just \n, vs \r\n in validRequestXml
        assertEquals(validRequestXml.replace("\r\n", "\n"), requestMessage.getAsString());

        // bundle reference items
        assertEquals(10, requestMessage.getBundleReferenceItems().size());

        // set action, no properties
        String id = "f1649a0664f1ebb6235ac238a6f71b75";
        EntityMappingInstructions.MappingAction action = EntityMappingInstructions.MappingAction.NewOrExisting;
        Properties properties = null;
        requestMessage.setMappingAction(id, action, properties);
        Pair<EntityMappingInstructions.MappingAction, Properties> result = requestMessage.getMappingAction(id);
        assertEquals(action, result.left);
        assertEquals(properties, result.right);

        // set action and FailOnNew property
        id = "f1649a0664f1ebb6235ac238a6f71a6d";
        action = EntityMappingInstructions.MappingAction.NewOrExisting;
        properties = new Properties();
        properties.put("FailOnNew", "true");
        requestMessage.setMappingAction(id, action, properties);
        result = requestMessage.getMappingAction(id);
        assertEquals(action, result.left);
        assertEquals(properties, result.right);

        // set action and targetId property
        id = "0567c6a8f0c4cc2c9fb331cb03b4de6f";
        action = EntityMappingInstructions.MappingAction.AlwaysCreateNew;
        properties = new Properties();
        properties.put("targetId", "32323232323232323232323232323232");
        requestMessage.setMappingAction(id, action, properties);
        result = requestMessage.getMappingAction(id);
        assertEquals(action, result.left);
        assertEquals(0, result.right.size());   // targetId stored as mapping attribute, not property
    }

    @Test
    public void mappingErrorResponse() throws Exception {
        final RestmanMessage responseMessage = spy(new RestmanMessage(XmlUtil.stringToDocument(errorMappingResponseXml)));

        // test response should have some mapping errors
        assertTrue(responseMessage.hasMappingError());
        assertEquals(10, responseMessage.getMappingErrors().size());

        // hasMappingError() and getMappingErrors() should reuse results from loadMappingErrors(), which should be called only once
        verify(responseMessage, times(1)).loadMappingErrors();
    }
}
