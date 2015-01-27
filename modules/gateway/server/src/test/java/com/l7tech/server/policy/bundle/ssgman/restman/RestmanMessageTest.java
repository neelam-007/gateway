package com.l7tech.server.policy.bundle.ssgman.restman;

import com.l7tech.common.io.XmlUtil;
import com.l7tech.server.bundling.EntityMappingInstructions;
import com.l7tech.util.IOUtils;
import com.l7tech.util.Pair;
import org.junit.Test;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

import static org.hamcrest.Matchers.startsWith;
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
    private static final String POLICY_ELEMENT = "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">";
    private final String validRequestXml;
    private final String errorMappingResponseXml;
    private final RestmanMessage requestMessage;

    public RestmanMessageTest() throws IOException, SAXException {
        byte[] bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/server/policy/bundle/bundles/RestmanBundle1/MigrationBundle1.0.xml"));
        validRequestXml = new String(bytes, RestmanInvoker.UTF_8);
        requestMessage = new RestmanMessage(validRequestXml);

        bytes = IOUtils.slurpUrl(getClass().getResource("/com/l7tech/server/policy/bundle/ssgman/restman/MigrationBundleMappingErrorResponse.xml"));
        errorMappingResponseXml = new String(bytes, RestmanInvoker.UTF_8);
    }

    @Test
    public void errorResponse() throws Exception {
        RestmanMessage responseMessage = new RestmanMessage(ERROR_RESPONSE_XML);

        assertTrue(responseMessage.isErrorResponse());

        // error response is different than mapping error
        assertFalse(responseMessage.hasMappingError());
        assertEquals(0, responseMessage.getMappingErrors().size());

        // xml in should be same as xml out
        assertEquals(ERROR_RESPONSE_XML, responseMessage.getAsString());
    }

    @Test
    public void validRequest() throws Exception {
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
        final RestmanMessage responseMessage = spy(new RestmanMessage(errorMappingResponseXml));

        // test response should have some mapping errors
        assertTrue(responseMessage.hasMappingError());
        assertEquals(10, responseMessage.getMappingErrors().size());

        // hasMappingError() and getMappingErrors() should reuse results from loadMappingErrors(), which should be called only once
        verify(responseMessage, times(1)).loadMappingErrors();
    }

    @Test
    public void resourceSetPolicy() throws Exception {
        final RestmanMessage requestMessage = spy(new RestmanMessage(validRequestXml));

        // get policy resource from a policy
        assertThat(requestMessage.getResourceSetPolicy("f1649a0664f1ebb6235ac238a6f71b61"), startsWith(POLICY_ELEMENT));

        // get policy resource from a service
        assertThat(requestMessage.getResourceSetPolicy("f1649a0664f1ebb6235ac238a6f71ba9"), startsWith(POLICY_ELEMENT));

        // verify list sizes are the same
        assertEquals(requestMessage.getResourceSetPolicies().size(), requestMessage.getResourceSetPolicyElements().size());

        // should reuse results from loadResourceSetPolicies(), which should be called only once
        verify(requestMessage, times(1)).loadResourceSetPolicies();
    }

    @Test
    public void testHasRootFolderItem() {
        assertFalse("There is no root folder item.", requestMessage.hasRootFolderItem());
    }

    @Test
    public void testHasRootFolderMapping() {
        assertFalse("There is no root folder mapper.", requestMessage.hasRootFolderMapping());
    }

    @Test
    public void testGetEntityName() {
        // Entity Type: Folder; Entity Name: Simple Policy Bundle
        String entityName = requestMessage.getEntityName("f1649a0664f1ebb6235ac238a6f71b0c");
        assertNotSame("N/A", entityName);
        assertEquals("Entity name matched", "Simple Policy Bundle", entityName);

        // Entity Type: POLICY; Entity Name: simpleIncludedPolicyFragment
        entityName = requestMessage.getEntityName("f1649a0664f1ebb6235ac238a6f71b61");
        assertNotSame("N/A", entityName);
        assertEquals("Entity name matched", "simpleIncludedPolicyFragment", entityName);
    }

    @Test
    public void testGetEntityType() {
        // Entity Type: Folder; Entity Id: f1649a0664f1ebb6235ac238a6f71b0c
        String entityType = requestMessage.getEntityType("f1649a0664f1ebb6235ac238a6f71b0c");
        assertEquals("Entity type matched", "FOLDER", entityType);

        // Entity Type: POLICY; Entity Id: f1649a0664f1ebb6235ac238a6f71b61
        entityType = requestMessage.getEntityType("f1649a0664f1ebb6235ac238a6f71b61");
        assertEquals("Entity type matched", "POLICY", entityType);
    }

    @Test
    public void testSetTargetIdInRootFolderMapping() throws SAXException, IOException {
        // Test addRootFolderMapping
        final RestmanMessage requestMessage = new RestmanMessage(validRequestXml);
        assertFalse("Initially there is no root folder mapping.", requestMessage.hasRootFolderMapping());

        requestMessage.addRootFolderMapping("2d41aa636524442706fd09ad724f78fa");
        assertTrue("A new root folder mapping has been added.", requestMessage.hasRootFolderMapping());

        String requestXml = XmlUtil.nodeToString(requestMessage.document);
        assertTrue("The attribute targetId is found.", requestXml.contains("targetId=\"2d41aa636524442706fd09ad724f78fa\""));

        // Test setRootFolderMappingTargetId
        requestMessage.setRootFolderMappingTargetId("7bf91daabff1558dd35b12b9f1f3ab7b");
        requestXml = XmlUtil.nodeToString(requestMessage.document);
        assertFalse("The value of the previous attribute targetId is changed.", requestXml.contains("targetId=\"2d41aa636524442706fd09ad724f78fa\""));
        assertTrue("The new value of the attribute targetId is correct.", requestXml.contains("targetId=\"7bf91daabff1558dd35b12b9f1f3ab7b\""));
    }

    @Test
    public void testLoadMappings() throws IOException, SAXException {
        // Test this method used by getting mapping from a restman request message.
        List<Element> mappings = requestMessage.getMappings();
        assertNotNull(mappings);
        assertEquals("There are 10 mappings in the request message", 10, mappings.size());

        Element firstElement = mappings.get(0);
        assertEquals("action matched", "NewOrUpdate", firstElement.getAttribute("action"));
        assertEquals("srcId matched", "f1649a0664f1ebb6235ac238a6f71b0c", firstElement.getAttribute("srcId"));
        assertEquals("type matched", "FOLDER", firstElement.getAttribute("type"));

        // Test this method used by getting mapping from a restman result message.
        final RestmanMessage responseMessage = new RestmanMessage(errorMappingResponseXml);
        mappings = responseMessage.getMappings();
        assertNotNull(mappings);
        assertEquals("There are 10 mappings in the response message", 10, mappings.size());

        final Element lastElement = mappings.get(9);
        assertEquals("action matched", "NewOrUpdate", lastElement.getAttribute("action"));
        assertEquals("errorType matched", "UniqueKeyConflict", lastElement.getAttribute("errorType"));
        assertEquals("srcId matched", "f1649a0664f1ebb6235ac238a6f71b4c", lastElement.getAttribute("srcId"));
        assertEquals("type matched", "POLICY", lastElement.getAttribute("type"));
    }
}