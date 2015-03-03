package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.RevocationCheckingPolicyMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.security.RevocationCheckPolicy;
import com.l7tech.gateway.common.security.RevocationCheckPolicyItem;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.identity.cert.TestRevocationCheckPolicyManager;
import com.l7tech.util.CollectionUtils;
import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class RevocationCheckingPolicyRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(RevocationCheckingPolicyRestServerGatewayManagementAssertionTest.class.getName());

    private static final RevocationCheckPolicy revocationCheckPolicy = new RevocationCheckPolicy();
    private static final RevocationCheckPolicyItem item = new RevocationCheckPolicyItem();
    private static TestRevocationCheckPolicyManager revocationCheckPolicyManager;
    private static final String revocationCheckPolicyBasePath = "revocationCheckingPolicies/";

    @InjectMocks
    protected RevocationCheckingPolicyResourceFactory revocationCheckPolicyResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        revocationCheckPolicyManager = assertionContext.getBean("revocationCheckPolicyManager", TestRevocationCheckPolicyManager.class);
        revocationCheckPolicy.setGoid(new Goid(0, 1234L));
        revocationCheckPolicy.setName("Test MQ Config 1");
        revocationCheckPolicy.setContinueOnServerUnavailable(false);
        item.setType(RevocationCheckPolicyItem.Type.CRL_FROM_URL);
        item.setUrl("URL");
        revocationCheckPolicy.setRevocationCheckItems(CollectionUtils.list(item));
        revocationCheckPolicyManager.save(revocationCheckPolicy);

    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(revocationCheckPolicyManager.findAllHeaders());
        for (EntityHeader entity : entities) {
            revocationCheckPolicyManager.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(revocationCheckPolicyBasePath + revocationCheckPolicy.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        assertEquals("Rev Check policy identifier:", revocationCheckPolicy.getId(), item.getId());
        assertEquals("Rev Check policy name:", revocationCheckPolicy.getName(), ((RevocationCheckingPolicyMO) item.getContent()).getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        RevocationCheckingPolicyMO createObject = revocationCheckPolicyResourceFactory.asResource(revocationCheckPolicy);
        createObject.setId(null);
        createObject.setName("New rev checking policy");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(revocationCheckPolicyBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));

        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        RevocationCheckPolicy createdEntity = revocationCheckPolicyManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Rev Check policy name:", createObject.getName(), createdEntity.getName());
    }


    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(revocationCheckPolicyBasePath + revocationCheckPolicy.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        RevocationCheckingPolicyMO entityGot = (RevocationCheckingPolicyMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        // update
        entityGot.setName(entityGot.getName() + "_mod");
        RestResponse response = processRequest(revocationCheckPolicyBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        RevocationCheckPolicy updatedEntity = revocationCheckPolicyManager.findByPrimaryKey(new Goid(revocationCheckPolicy.getId()));
        assertEquals("Rev Check policy name:", entityGot.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(revocationCheckPolicyBasePath + revocationCheckPolicy.getId(), HttpMethod.DELETE, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        RestResponse responseGet = processRequest(revocationCheckPolicyBasePath + revocationCheckPolicy.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Status resource not found:", responseGet.getStatus(), HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void listEntitiesTest() throws Exception {

        RestResponse response = processRequest(revocationCheckPolicyBasePath, HttpMethod.GET, null, "");
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<RevocationCheckingPolicyMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(1, item.getContent().size());
    }
}
