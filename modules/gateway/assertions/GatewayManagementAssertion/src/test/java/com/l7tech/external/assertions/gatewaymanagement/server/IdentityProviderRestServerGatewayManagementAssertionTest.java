package com.l7tech.external.assertions.gatewaymanagement.server;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.IdentityProviderMO;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.gateway.api.Reference;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.identity.IdentityProviderType;
import com.l7tech.identity.ldap.LdapIdentityProviderConfig;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.identity.TestIdentityProviderConfigManager;
import org.apache.http.entity.ContentType;
import org.junit.*;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.logging.Logger;

import static junit.framework.Assert.assertEquals;

/**
 *
 */
public class IdentityProviderRestServerGatewayManagementAssertionTest extends ServerRestGatewayManagementAssertionTestBase {
    private static final Logger logger = Logger.getLogger(IdentityProviderRestServerGatewayManagementAssertionTest.class.getName());

    private static TestIdentityProviderConfigManager identityProviderConfigManager;
    private static final String identityProviderBasePath = "identityProviders/";

    private static IdentityProviderConfig idProviderConfig;


    @InjectMocks
    protected IdentityProviderResourceFactory identityProviderResourceFactory;

    @Before
    public void before() throws Exception {
        super.before();
        identityProviderConfigManager = applicationContext.getBean("identityProviderConfigManager", TestIdentityProviderConfigManager.class);
        idProviderConfig = provider(new Goid(1, 2L), IdentityProviderType.LDAP, "LDAP", "userLookupByCertMode", "CERT");
        ((LdapIdentityProviderConfig) idProviderConfig).setClientAuthEnabled(true);
        ((LdapIdentityProviderConfig) idProviderConfig).setLdapUrl(new String[]{"ldap://test:789"});
        ((LdapIdentityProviderConfig) idProviderConfig).setSearchBase("searchBase");
        ((LdapIdentityProviderConfig) idProviderConfig).setBindDN("bindDN");
        ((LdapIdentityProviderConfig) idProviderConfig).setBindPasswd("password");
        ((LdapIdentityProviderConfig) idProviderConfig).setTemplateName("MicrosoftActiveDirectory");

        identityProviderConfigManager.save(idProviderConfig);

    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ServerRestGatewayManagementAssertionTestBase.beforeClass();
    }

    @After
    public void after() throws Exception {
        super.after();
    }

    @Test
    public void getEntityTest() throws Exception {
        RestResponse response = processRequest(identityProviderBasePath + idProviderConfig.getId(), HttpMethod.GET, null, "");
        logger.info(response.toString());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);
        IdentityProviderMO result = (IdentityProviderMO) reference.getResource();

        assertEquals("Identity Provider identifier:", idProviderConfig.getId(), result.getId());
        assertEquals("Identity Provider name:", idProviderConfig.getName(), result.getName());
        assertEquals("Identity Provider type:", IdentityProviderMO.IdentityProviderType.LDAP, result.getIdentityProviderType());
    }

    @Test
    public void createEntityTest() throws Exception {

        IdentityProviderMO createObject = identityProviderResourceFactory.asResource(idProviderConfig);
        createObject.setId(null);
        createObject.setName("New Identity Provider");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(identityProviderBasePath, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        IdentityProviderConfig createdProvider = identityProviderConfigManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Identity Provider name:", createdProvider.getName(), createObject.getName());
        assertEquals("Identity Provider type:", createdProvider.getTypeVal(), IdentityProviderType.LDAP.toVal());
    }

    @Test
    public void createEntityWithIDTest() throws Exception {

        Goid goid = new Goid(12345678L, 5678);
        IdentityProviderMO createObject = identityProviderResourceFactory.asResource(idProviderConfig);
        createObject.setId(null);
        createObject.setName("New Identity Provider");
        Document request = ManagedObjectFactory.write(createObject);
        RestResponse response = processRequest(identityProviderBasePath + goid, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(request));
        logger.info(response.toString());

        assertEquals("Created Identity Provider goid:", goid.toString(), getFirstReferencedGoid(response));

        IdentityProviderConfig createdProvider = identityProviderConfigManager.findByPrimaryKey(goid);
        assertEquals("Identity Provider name:", createdProvider.getName(), createObject.getName());
        assertEquals("Identity Provider type:", createdProvider.getTypeVal(), IdentityProviderType.LDAP.toVal());
    }

    @Test
    public void updateEntityTest() throws Exception {

        // get
        RestResponse responseGet = processRequest(identityProviderBasePath + idProviderConfig.getId(), HttpMethod.GET, null, "");
        Assert.assertEquals(AssertionStatus.NONE, responseGet.getAssertionStatus());
        final StreamSource source = new StreamSource(new StringReader(responseGet.getBody()));
        IdentityProviderMO entityGot = (IdentityProviderMO) MarshallingUtils.unmarshal(Reference.class, source).getResource();

        // update
        entityGot.setName(entityGot.getName() + "_mod");
        RestResponse response = processRequest(identityProviderBasePath + entityGot.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(entityGot)));
        logger.info(response.toString());

        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        assertEquals("Created Identity Provider goid:", entityGot.getId(), getFirstReferencedGoid(response));

        // check entity
        IdentityProviderConfig updatedProvider = identityProviderConfigManager.findByPrimaryKey(new Goid(getFirstReferencedGoid(response)));

        assertEquals("Identity Provider id:", updatedProvider.getId(), idProviderConfig.getId());
        assertEquals("Identity Provider name:", updatedProvider.getName(), entityGot.getName());
        assertEquals("Identity Provider type:", updatedProvider.getTypeVal(), IdentityProviderType.LDAP.toVal());
    }

    @Test
    public void deleteEntityTest() throws Exception {

        RestResponse response = processRequest(identityProviderBasePath + idProviderConfig.getId(), HttpMethod.DELETE, null, "");
        logger.info(response.toString());
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());

        // check entity
        Assert.assertNull(identityProviderConfigManager.findByPrimaryKey(idProviderConfig.getGoid()));
    }

}
