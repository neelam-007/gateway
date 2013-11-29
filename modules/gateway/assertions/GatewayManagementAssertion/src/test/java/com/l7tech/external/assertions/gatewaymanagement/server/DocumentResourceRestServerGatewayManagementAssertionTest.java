package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.Reference;
import com.l7tech.external.assertions.gatewaymanagement.server.rest.entities.References;
import com.l7tech.gateway.api.Resource;
import com.l7tech.gateway.api.ResourceDocumentMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.resources.ResourceEntry;
import com.l7tech.gateway.common.resources.ResourceEntryHeader;
import com.l7tech.gateway.common.resources.ResourceType;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.globalresources.ResourceEntryManagerStub;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class DocumentResourceRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(DocumentResourceRestServerGatewayManagementAssertionTest.class.getName());

    private static final ResourceEntry resource = new ResourceEntry();
    private static ResourceEntryManagerStub resourceEntryManagerStub;
    private static final String documentResourceBasePath = "resources/";

    @InjectMocks
    protected DocumentResourceFactory documentResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();

        resourceEntryManagerStub = applicationContext.getBean("resourceEntryManager", ResourceEntryManagerStub.class);
        resource.setGoid(new Goid(0, 1234L));
        resource.setUri("books.dtd");
        resource.setType(ResourceType.DTD);
        resource.setContentType(ResourceType.DTD.getMimeType());
        resource.setResourceKey1("books");
        resource.setContent("<!ELEMENT book ANY>");
        resource.setDescription("The books DTD.");

        resourceEntryManagerStub.save(resource);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<ResourceEntryHeader> entities = new ArrayList<>(resourceEntryManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            resourceEntryManagerStub.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        Response response = processRequest(documentResourceBasePath + resource.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        ResourceDocumentMO result = ManagedObjectFactory.read(response.getBody(), ResourceDocumentMO.class);

        assertEquals("Document resource identifier:", resource.getId(), result.getId());
        assertEquals("Document resource source url:", resource.getUri(), result.getResource().getSourceUrl());
    }

    @Test
    public void createDTDTest() throws Exception {

        ResourceDocumentMO createObject = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("books2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        createObject.setResource(createResource);
        createObject.setProperties(new HashMap<String, Object>());
        createObject.getProperties().put("description", "new dtd");
        createObject.getProperties().put("publicIdentifier", "books2");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(documentResourceBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        ResourceEntry createdEntity = resourceEntryManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Document resource source url:", createResource.getSourceUrl(), createdEntity.getUri());
    }

    @Test
    public void createBadPublicIdentifierTest() throws Exception {

        ResourceDocumentMO createObject = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("books2.dtd");
        createResource.setType("dtd");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        createObject.setResource(createResource);
        createObject.setProperties(new HashMap<String, Object>());
        createObject.getProperties().put("description", "new dtd");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(documentResourceBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createInvalidSchemaTypeTest() throws Exception {

        ResourceDocumentMO createObject = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("books2.dtd");
        createResource.setType("bad type");
        createResource.setContent("<![CDATA[<!ELEMENT book ANY>]]>");
        createObject.setResource(createResource);
        createObject.setProperties(new HashMap<String, Object>());
        createObject.getProperties().put("description", "new dtd");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(documentResourceBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createInvalidSchemaTest() throws Exception {

        ResourceDocumentMO createObject = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("books2.xsd");
        createResource.setType("xmlschema");
        createResource.setContent("<<xs:schema targetNamespace=\"urn:books2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"book\" type=\"xs:string\"/></xs:schema>");
        createObject.setResource(createResource);
        createObject.setProperties(new HashMap<String, Object>());
        createObject.getProperties().put("description", "new schema");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(documentResourceBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        Assert.assertTrue(response.getBody().contains("INVALID_VALUES"));
    }

    @Test
    public void createSchemaTest() throws Exception {

        ResourceDocumentMO createObject = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("books2.xsd");
        createResource.setType("xmlschema");
        createResource.setContent("<xs:schema targetNamespace=\"urn:books2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"book\" type=\"xs:string\"/></xs:schema>");
        createObject.setResource(createResource);
        createObject.setProperties(new HashMap<String, Object>());
        createObject.getProperties().put("description", "new schema");
        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(documentResourceBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        ResourceEntry createdEntity = resourceEntryManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Document resource source url:", createResource.getSourceUrl(), createdEntity.getUri());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        ResourceDocumentMO createObject = ManagedObjectFactory.createResourceDocument();
        Resource createResource = ManagedObjectFactory.createResource();
        createResource.setSourceUrl("books2.xsd");
        createResource.setType("xmlschema");
        createResource.setContent("<xs:schema targetNamespace=\"urn:books2\" xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"><xs:element name=\"book\" type=\"xs:string\"/></xs:schema>");
        createObject.setResource(createResource);
        createObject.setProperties(new HashMap<String, Object>());
        createObject.getProperties().put("description", "new schema");

        Document request = ManagedObjectFactory.write(createObject);
        Response response = processRequest(documentResourceBasePath + goid.toString(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        assertEquals("Created Document resource goid:", goid.toString(), getFirstReferencedGoid(response));

        ResourceEntry createdEntity = resourceEntryManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Document resource source url:", createResource.getSourceUrl(), createdEntity.getUri());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        Response responseGet = processRequest(documentResourceBasePath + resource.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        ResourceDocumentMO entityGot = MarshallingUtils.unmarshal(ResourceDocumentMO.class, source);

        // update
        entityGot.getProperties().put("description", "new description");
        Response response = processRequest(documentResourceBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Document resource goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        ResourceEntry updatedConnector = resourceEntryManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Document resource id:", updatedConnector.getId(), resource.getId());
        assertEquals("Document resource source url:", resource.getUri(), updatedConnector.getUri());
        assertEquals("Document resource source url:", "new description", updatedConnector.getDescription());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Response response = processRequest(documentResourceBasePath + resource.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(resourceEntryManagerStub.findByPrimaryKey(resource.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Response response = processRequest(documentResourceBasePath, HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        JAXBContext jsxb = JAXBContext.newInstance(References.class, Reference.class);
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        References references = jsxb.createUnmarshaller().unmarshal(source, References.class).getValue();

        // check entity
        Assert.assertEquals(resourceEntryManagerStub.findAll().size(), references.getReferences().size());
    }
}
