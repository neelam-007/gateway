package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.InterfaceTagMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.References;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.cluster.ClusterProperty;
import com.l7tech.gateway.common.transport.InterfaceTag;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.MockClusterPropertyManager;
import com.l7tech.util.Charsets;
import com.l7tech.util.CollectionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;

/**
 *
 */
public class InterfaceTagRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(InterfaceTagRestServerGatewayManagementAssertionTest.class.getName());

    private static final InterfaceTag interfaceTag = new InterfaceTag("blah", CollectionUtils.set("blah"));
    private static MockClusterPropertyManager clusterPropertyManager;
    private static final String interfaceTagBasePath = "interfaceTags/";
    private static final String propertyName = "interfaceTags";

    @InjectMocks
    InterfaceTagResourceFactory interfaceTagResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        clusterPropertyManager = applicationContext.getBean("clusterPropertyManager", MockClusterPropertyManager.class);
        interfaceTag.setName("Test_interface_tag");
        interfaceTag.setIpPatterns(CollectionUtils.set("1.1.1.1/11", "2.2.2.2/22"));
        clusterPropertyManager.save(new ClusterProperty(propertyName,entityToString(interfaceTag)));

    }

    @After
    public void after() throws Exception {
        super.after();
        clearClusterProperty();
    }

    protected static String MOToString(InterfaceTagMO interfaceTag){
        InterfaceTag entity = new InterfaceTag(interfaceTag.getName(),new LinkedHashSet<String>(interfaceTag.getAddressPatterns()));
        return InterfaceTag.toString(CollectionUtils.set(entity));
    }

    protected static String entityToString(InterfaceTag interfaceTag){
        return InterfaceTag.toString(CollectionUtils.set(interfaceTag));
    }

    protected void clearClusterProperty() throws FindException, DeleteException {
        ClusterProperty prop = clusterPropertyManager.findByUniqueName(propertyName);
        if(prop!=null)
            clusterPropertyManager.delete(prop);
    }

    private String nameAsIdentifier( final String name ) {
        return UUID.nameUUIDFromBytes(name.getBytes(Charsets.UTF8)).toString();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(interfaceTagBasePath + nameAsIdentifier(interfaceTag.getName()), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

        assertEquals("Interface Tag identifier:",  nameAsIdentifier(interfaceTag.getName()), reference.getId());
        assertEquals("Interface Tag name:", interfaceTag.getName(), ((InterfaceTagMO) reference.getResource()).getName());
        assertTrue("Interface Tag ip address:", interfaceTag.getIpPatterns().containsAll(((InterfaceTagMO) reference.getResource()).getAddressPatterns()));
    }

    @Test
    public void createEntityTest() throws Exception {

        InterfaceTagMO createObject = interfaceTagResourceFactory.internalAsResource(interfaceTag);
        createObject.setId(null);
        createObject.setName("New_interface_tag");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(interfaceTagBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());
        assertEquals("Interface Tag id:", nameAsIdentifier(createObject.getName()), getFirstReferencedGoid(response));

        ClusterProperty interfaceTagProperty = clusterPropertyManager.findByUniqueName(propertyName);

        assertTrue("Interface Tag value:", interfaceTagProperty.getValue().contains(MOToString(createObject)));
    }

    @Test
    public void createEntityNoExistingInterfaceTagsTest() throws Exception {
        clearClusterProperty();

        InterfaceTagMO createObject = interfaceTagResourceFactory.internalAsResource(interfaceTag);
        createObject.setId(null);
        createObject.setName("New_interface_tag");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(interfaceTagBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());
        assertEquals("Interface Tag id:", nameAsIdentifier(createObject.getName()), getFirstReferencedGoid(response));

        ClusterProperty interfaceTagProperty = clusterPropertyManager.findByUniqueName(propertyName);

        assertNotNull("Interface Tag cluster property exists:", interfaceTagProperty);
        assertTrue("Interface Tag value:", interfaceTagProperty.getValue().contains(MOToString(createObject)));
    }

    @Test
    public void createEntityInvalidIpAddressFormatTest() throws Exception {

        InterfaceTagMO createObject = interfaceTagResourceFactory.internalAsResource(interfaceTag);
        createObject.setId(null);
        createObject.setName("New_interface_tag");
        createObject.setAddressPatterns(CollectionUtils.list("not a id"));
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(interfaceTagBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        InterfaceTagMO createObject = interfaceTagResourceFactory.internalAsResource(interfaceTag);
        createObject.setId(null);
        createObject.setName("New_interface_tag");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(interfaceTagBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        Assert.assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(interfaceTagBasePath + nameAsIdentifier(interfaceTag.getName()), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        InterfaceTagMO entityGot = (InterfaceTagMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setAddressPatterns(CollectionUtils.list("5.5.5.5/55"));
        Response response = processRequest(interfaceTagBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Interface Tag id:", nameAsIdentifier(entityGot.getName()), getFirstReferencedGoid(response));

        // check entity
        ClusterProperty interfaceTagProperty = clusterPropertyManager.findByUniqueName(propertyName);

        assertNotNull("Interface Tag cluster property exists:", interfaceTagProperty);
        assertTrue("Interface Tag value:", interfaceTagProperty.getValue().contains(MOToString(entityGot)));
    }

    @Test
    public void updateNameFailTest() throws Exception {

        // get
        Response responseGet = processRequest(interfaceTagBasePath + nameAsIdentifier(interfaceTag.getName()), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        InterfaceTagMO entityGot = (InterfaceTagMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setName(entityGot+"_mod");
        Response response = processRequest(interfaceTagBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(interfaceTagBasePath + nameAsIdentifier(interfaceTag.getName()), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        ClusterProperty interfaceTagProperty = clusterPropertyManager.findByUniqueName(propertyName);

        Assert.assertNotNull(interfaceTagProperty);
        assertEquals("Empty cluster property value","",interfaceTagProperty.getValue());
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(interfaceTagBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference<References> reference = MarshallingUtils.unmarshal(Reference.class, source);

        // check entity
        Assert.assertEquals(1, reference.getResource().getReferences().size());
    }
}
