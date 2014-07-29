package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.StoredPasswordMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.message.Message;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.policy.assertion.TargetMessageType;
import com.l7tech.server.security.password.SecurePasswordManagerStub;
import com.l7tech.util.CollectionUtils;
import org.junit.*;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class ServerRestGatewayManagementAssertionContextVariableTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(ServerRestGatewayManagementAssertionContextVariableTest.class.getName());

    private static final SecurePassword securePassword = new SecurePassword();
    private static SecurePasswordManagerStub securePasswordManagerStub;
    private static final String securePasswordBasePath = "1.0/passwords/";
    private static final String targetVariable = "var";

    @InjectMocks
    protected SecurePasswordResourceFactory securePasswordResourceFactory;

    @Before
    public void before() throws Exception {
        securePasswordManagerStub = applicationContext.getBean("securePasswordManager", SecurePasswordManagerStub.class);
        securePassword.setDescription("Test password");
        securePassword.setName("Test password name");
        securePassword.setType(SecurePassword.SecurePasswordType.PASSWORD);
        securePassword.setEncodedPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));
        securePasswordManagerStub.save(securePassword);

        // configure the assertion
        RESTGatewayManagementAssertion restmanAssertion = new RESTGatewayManagementAssertion();
        restmanAssertion.setOtherTargetMessageVariable(targetVariable);
        restmanAssertion.setTarget(TargetMessageType.OTHER);
        restManagementAssertion = new ServerRESTGatewayManagementAssertion(restmanAssertion, applicationContext, "testGatewayManagementContext.xml");

        MockitoAnnotations.initMocks(this);
    }

    @After
    public void after() throws Exception {
        super.after();
        Collection<EntityHeader> entities = new ArrayList<>(securePasswordManagerStub.findAllHeaders());
        for (EntityHeader entity : entities) {
            securePasswordManagerStub.delete(entity.getGoid());
        }
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @Test
    public void getEntityTest() throws Exception {

        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable, new Message())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath+ securePassword.getId())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.GET.getProtocolName()).map();

        RestResponse response = processRequest(securePasswordBasePath + securePassword.getId(), null, HttpMethod.GET, null, "", contextVariables);
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);
        StoredPasswordMO result = (StoredPasswordMO) item.getContent();

        assertEquals("Secure Password identifier:", securePassword.getId(), result.getId());
        assertEquals("Secure Password Name:", securePassword.getName(), result.getName());
    }

    @Test
    public void createEntityTest() throws Exception {

        StoredPasswordMO createObject = securePasswordResourceFactory.asResource(securePassword);
        createObject.setId(null);
        createObject.setName("New name");
        createObject.setPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));

        final Message var = new Message();
        var.initialize( ManagedObjectFactory.write(createObject));
        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable,var)
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath)
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.POST.getProtocolName()).map();

        RestResponse response = processRequest(securePasswordBasePath, null, HttpMethod.POST, null,"",contextVariables);

        logger.log(Level.INFO, response.getBody());

        SecurePassword createdEntity = securePasswordManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Secure Password Name:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        StoredPasswordMO createObject = securePasswordResourceFactory.asResource(securePassword);
        createObject.setId(null);
        createObject.setName("New name");
        createObject.setPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));

        final Message var = new Message();
        var.initialize( ManagedObjectFactory.write(createObject));
        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable,var)
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath + goid.toString())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.PUT.getProtocolName()).map();


        RestResponse response = processRequest(securePasswordBasePath + goid.toString(),"", HttpMethod.PUT, null,"",contextVariables);

        assertEquals("Created Secure Password goid:", goid.toString(), getFirstReferencedGoid(response));

        SecurePassword createdEntity = securePasswordManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));
        assertEquals("Secure Password username:", createObject.getName(), createdEntity.getName());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get

        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable,new Message())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath + securePassword.getId())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.GET.getProtocolName()).map();
        RestResponse responseGet = processRequest(securePasswordBasePath + securePassword.getId(), null, HttpMethod.GET, null, "", contextVariables);
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        StoredPasswordMO entityGot = (StoredPasswordMO) MarshallingUtils.unmarshal(Item.class, source).getContent();

        final Message var = new Message();
        var.initialize( ManagedObjectFactory.write(entityGot));
        contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable,var)
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath + entityGot.getId())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.PUT.getProtocolName()).map();

        // update
        entityGot.setName("Updated name");
        RestResponse response = processRequest(securePasswordBasePath + entityGot.getId(),null, HttpMethod.PUT, null, "", contextVariables);

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created secure password goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        SecurePassword updatedEntity = securePasswordManagerStub.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Secure Password id:", securePassword.getId(), updatedEntity.getId());
        assertEquals("Secure Password name:", securePassword.getName(), updatedEntity.getName());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable, new Message())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath + securePassword.getId())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.DELETE.getProtocolName()).map();


        RestResponse response = processRequest(securePasswordBasePath + securePassword.getId(), null, HttpMethod.DELETE, null, "", contextVariables);
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(securePasswordManagerStub.findByPrimaryKey(securePassword.getGoid()));
    }

    @Test
    public void listEntitiesTest() throws Exception {

        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable, new Message())
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath)
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.GET.getProtocolName()).map();

        RestResponse response = processRequest(securePasswordBasePath, null, HttpMethod.GET, null, "", contextVariables);
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<StoredPasswordMO> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        // check entity
        Assert.assertEquals(securePasswordManagerStub.findAll().size(), item.getContent().size());
    }

    @Test
    public void createEntityTestNoUriFail() throws Exception {

        StoredPasswordMO createObject = securePasswordResourceFactory.asResource(securePassword);
        createObject.setId(null);
        createObject.setName("New name");
        createObject.setPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));

        final Message var = new Message();
        var.initialize( ManagedObjectFactory.write(createObject));
        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable,var)
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_ACTION,HttpMethod.GET.getProtocolName()).map();

        RestResponse response = processRequest(securePasswordBasePath, null, HttpMethod.POST, null,"",contextVariables);

        Assert.assertEquals(AssertionStatus.FAILED, response.getAssertionStatus());
    }

    @Test
    public void createEntityTestNoActionFail() throws Exception {

        StoredPasswordMO createObject = securePasswordResourceFactory.asResource(securePassword);
        createObject.setId(null);
        createObject.setName("New name");
        createObject.setPassword(securePasswordManagerStub.encryptPassword("password".toCharArray()));

        final Message var = new Message();
        var.initialize( ManagedObjectFactory.write(createObject));
        Map<String,Object> contextVariables =
                CollectionUtils.<String,Object>mapBuilder()
                        .put(targetVariable,var)
                        .put("restGatewayMan."+RESTGatewayManagementAssertion.SUFFIX_URI,securePasswordBasePath).map();

        RestResponse response = processRequest(securePasswordBasePath, null, HttpMethod.POST, null,"",contextVariables);

        Assert.assertEquals(AssertionStatus.FAILED, response.getAssertionStatus());
    }
}
