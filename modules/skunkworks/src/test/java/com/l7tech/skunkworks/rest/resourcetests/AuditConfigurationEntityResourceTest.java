package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.gateway.api.AuditConfigurationMO;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.audit.AuditConfiguration;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.IdentityHeader;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.audit.AuditConfigurationManager;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.Functions;
import java.io.StringReader;
import java.util.Collection;
import java.util.logging.Logger;
import javax.xml.transform.stream.StreamSource;
import org.junit.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class AuditConfigurationEntityResourceTest extends RestEntityTestBase {

    private static final Logger logger = Logger.getLogger(AuditConfigurationEntityResourceTest.class.getName());

    private final Goid internalProviderId = IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID;

    protected static IdentityProvider provider;
    protected static UserManager userManager;
    private final PasswordHasher passwordHasher = new Sha512CryptPasswordHasher();

    protected static AuditConfigurationManager auditConfigurationManager;

    @Before
    public void before() throws Exception {

        auditConfigurationManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("auditConfigurationManager", AuditConfigurationManager.class);

        IdentityProviderFactory identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);
        provider = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
        userManager = provider.getUserManager();
    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<IdentityHeader> all = userManager.findAllHeaders();
        for (IdentityHeader user : all) {
            if (!user.getStrId().equals(new Goid(0, 3).toString())) {
                userManager.delete(user.getStrId());
            }
        }
    }

    private InternalUser createUnprivilegedUser() throws com.l7tech.objectmodel.SaveException {
        InternalUser user = new InternalUser();
        user.setName("Unprivileged");
        user.setLogin("unprivilegedUser");
        user.setHashedPassword(passwordHasher.hashPassword("pass".getBytes()));
        userManager.save(user, null);
        return user;
    }

    public String getResourceUri() {
        return "auditConfiguration";
    }

    public String getType() {
        return EntityType.AUDIT_CONFIG.name();
    }

    @Test
    public void testListUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("auditConfiguration" , null, HttpMethod.GET, null, "",null, user);
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        final ItemsList<AuditConfigurationMO> auditConfigurations = MarshallingUtils.unmarshal(ItemsList.class, source);
        Assert.assertNull("Unprivilaged items should be filtered out", auditConfigurations.getContent());
    }

    @Test
    public void testListPrivileged() throws Exception {

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("auditConfiguration" , null, HttpMethod.GET, null, "",null);
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());
        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        final ItemsList<AuditConfigurationMO> auditConfigurations = MarshallingUtils.unmarshal(ItemsList.class, source);
        Assert.assertEquals( 1, auditConfigurations.getContent().size());
    }

    @Test
    public void testGetUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("auditConfiguration/default" , null, HttpMethod.GET, null, "",null, user);
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(401, response.getStatus());
    }

    @Test
    public void testGetPrivileged() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("auditConfiguration/default" , null, HttpMethod.GET, null, "",null);
        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(200, response.getStatus());

        Assert.assertNotNull("Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        AuditConfiguration entity = auditConfigurationManager.findByPrimaryKey(AuditConfiguration.ENTITY_ID);

        Assert.assertEquals("Id's don't match", entity.getId(), item.getId());
        Assert.assertEquals("Type is incorrect", EntityType.AUDIT_CONFIG.toString(), item.getType());
        Assert.assertEquals("Title is incorrect", AuditConfiguration.ENTITY_NAME, item.getName());
        Assert.assertNotNull("TimeStamp must always be present", item.getDate());

        Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = Functions.grepFirst(item.getLinks(), new Functions.Unary<Boolean, Link>() {
            @Override
            public Boolean call(Link i) {
                return i.getRel().equals("self");
            }
        });
        Assert.assertNotNull("self link must be present", self);
        Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + "auditConfiguration/default", self.getUri());

        verifyEntity(entity, (AuditConfigurationMO) item.getContent());

    }

    public void verifyEntity(AuditConfiguration entity, AuditConfigurationMO managedObject) throws FindException {
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);
            Assert.assertEquals(entity.isAlwaysSaveInternal(), managedObject.getAlwaysSaveInternal());
        }
    }
}
