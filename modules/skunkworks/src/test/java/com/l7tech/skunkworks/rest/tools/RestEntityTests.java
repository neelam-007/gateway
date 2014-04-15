package com.l7tech.skunkworks.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.common.password.PasswordHasher;
import com.l7tech.common.password.Sha512CryptPasswordHasher;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.identity.UserManager;
import com.l7tech.identity.internal.InternalUser;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.identity.IdentityProviderFactory;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class RestEntityTests<E, M extends ManagedObject> extends RestEntityTestBase implements RestEntityResourceUtil<E, M> {
    private static final Logger logger = Logger.getLogger(RestEntityTests.class.getName());
    private static IdentityProviderFactory identityProviderFactory;
    private static IdentityProvider provider;
    protected static UserManager userManager;

    @BeforeClass
    public static void beforeClass() throws Exception {
        RestEntityTestBase.beforeClass();

        //This will be null on daily builds
        if(getDatabaseBasedRestManagementEnvironment() != null){
            identityProviderFactory = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("identityProviderFactory", IdentityProviderFactory.class);
            provider = identityProviderFactory.getProvider(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID);
            userManager = provider.getUserManager();
        }
    }

    @Test
    public void testGet() throws Exception {
        List<String> entitiesExpected = getRetrievableEntityIDs();

        for (String entityId : entitiesExpected) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + entityId, HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 200, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item item = MarshallingUtils.unmarshal(Item.class, source);

            Assert.assertEquals("Id's don't match", entityId, item.getId());
            Assert.assertEquals("Type is incorrect", getType(), item.getType());
            Assert.assertEquals("Title is incorrect", getExpectedTitle(entityId), item.getName());

            Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
            Link self = findLink("self", item.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + entityId, self.getUri());

            verifyLinks(entityId, item.getLinks());

            verifyEntity(entityId, (M) item.getContent());
        }
    }

    @Test
    public void testGetNotExisting() throws Exception {
        Goid badId = new Goid(0, 0);
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + badId.toString(), HttpMethod.GET, null, "");
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 404, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
    }

    @Test
    public void testGetEntityFailed() throws Exception {
        Map<String, Functions.BinaryVoid<String, RestResponse>> entitiesToGet = getUnGettableManagedObjectIds();

        for (String id : entitiesToGet.keySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + id, HttpMethod.GET, null, "");
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());

            entitiesToGet.get(id).call(id, response);
        }
    }

    private final PasswordHasher passwordHasher = new Sha512CryptPasswordHasher();

    @Test
    public void testGetUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();
        try {
            List<String> entitiesExpected = getRetrievableEntityIDs();

            for (String id : entitiesExpected) {
                RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + id, null, HttpMethod.GET, null, "", user);
                logger.log(Level.FINE, response.toString());

                Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
                Assert.assertEquals(401, response.getStatus());
            }
        } finally {
            userManager.delete(user);
        }
    }

    protected InternalUser createUnprivilegedUser() throws com.l7tech.objectmodel.SaveException {
        InternalUser user = new InternalUser();
        user.setName("Unprivileged");
        user.setLogin("unprivilegedUser");
        user.setHashedPassword(passwordHasher.hashPassword("password".getBytes()));
        userManager.save(user, null);
        return user;
    }

    @Test
    public void testCreateEntity() throws Exception {
        List<M> entitiesToCreate = getCreatableManagedObjects();

        for (M mo : entitiesToCreate) {
            mo.setId(null);
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 201, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item item = MarshallingUtils.unmarshal(Item.class, source);

            Assert.assertEquals("Type is incorrect", getType(), item.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(item.getId()), item.getName());

            Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
            Link self = findLink("self", item.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + item.getId(), self.getUri());

            verifyLinks(item.getId(), item.getLinks());

            Assert.assertNull(item.getContent());
            mo.setId(item.getId());
            verifyEntity(item.getId(), mo);
        }
    }

    @Test
    public void testCreateEntityFailed() throws Exception {
        Map<M,Functions.BinaryVoid<M,RestResponse>> entitiesToCreate = getUnCreatableManagedObjects();

        for (M mo : entitiesToCreate.keySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());

            entitiesToCreate.get(mo).call(mo, response);
        }
    }

    @Test
    public void testCreateEntityUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();
        try {
            List<M> entitiesToCreate = getCreatableManagedObjects();

            for (M mo : entitiesToCreate) {
                mo.setId(null);
                RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), null, HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)), user);
                logger.log(Level.FINE, response.toString());

                Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
                Assert.assertEquals(401, response.getStatus());
            }
        } finally {
            userManager.delete(user);
        }
    }

    @Test
    public void testCreateWithIdEntity() throws Exception {
        List<M> entitiesToCreate = getCreatableManagedObjects();

        for (M mo : entitiesToCreate) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + mo.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 201, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item item = MarshallingUtils.unmarshal(Item.class, source);

            Assert.assertEquals("Id's don't match", mo.getId(), item.getId());
            Assert.assertEquals("Type is incorrect", getType(), item.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(mo.getId()), item.getName());

            Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
            Link self = findLink("self", item.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + mo.getId(), self.getUri());

            verifyLinks(mo.getId(), item.getLinks());

            Assert.assertNull(item.getContent());
            verifyEntity(mo.getId(), mo);
        }
    }

    @Test
    public void testCreateWithIdEntityUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();
        try {
            List<M> entitiesToCreate = getCreatableManagedObjects();

            for (M mo : entitiesToCreate) {
                RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + mo.getId(), null, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)), user);
                logger.log(Level.FINE, response.toString());

                Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
                Assert.assertEquals(401, response.getStatus());
            }
        } finally {
            userManager.delete(user);
        }
    }

    @Test
    public void testUpdateEntity() throws Exception {
        List<M> entitiesToUpdate = getUpdateableManagedObjects();

        for (M mo : entitiesToUpdate) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + mo.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 200, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item item = MarshallingUtils.unmarshal(Item.class, source);

            Assert.assertEquals("Id's don't match", mo.getId(), item.getId());
            Assert.assertEquals("Type is incorrect", getType(), item.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(mo.getId()), item.getName());

            Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
            Link self = findLink("self", item.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + mo.getId(), self.getUri());

            verifyLinks(mo.getId(), item.getLinks());

            Assert.assertNull(item.getContent());
            verifyEntity(mo.getId(), mo);
        }
    }

    @Test
    public void testUpdateEntityFailed() throws Exception {
        Map<M,Functions.BinaryVoid<M,RestResponse>> entitiesToCreate = getUnUpdateableManagedObjects();

        for (M mo : entitiesToCreate.keySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + mo.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());

            entitiesToCreate.get(mo).call(mo, response);
        }
    }

    @Test
    public void testUpdateEntityUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();
        try {
            List<M> entitiesToCreate = getUpdateableManagedObjects();

            for (M mo : entitiesToCreate) {
                RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + mo.getId(), null, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)), user);
                logger.log(Level.FINE, response.toString());

                Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
                Assert.assertEquals(401, response.getStatus());
            }
        } finally {
            userManager.delete(user);
        }
    }

    @Test
    public void testDeleteEntity() throws Exception {
        List<String> entitiesIDsToDelete = getDeleteableManagedObjectIDs();

        for (String id : entitiesIDsToDelete) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + id, HttpMethod.DELETE, null, "");
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 204, response.getStatus());
            Assert.assertEquals("Expected empty response body", "", response.getBody());

            verifyEntity(id, null);
        }
    }

    @Test
    public void testDeleteEntityUnprivileged() throws Exception {
        InternalUser user = createUnprivilegedUser();
        try {
            List<String> entitiesIDsToDelete = getDeleteableManagedObjectIDs();

            for (String id : entitiesIDsToDelete) {
                RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + id, null, HttpMethod.DELETE, null, "", user);
                logger.log(Level.FINE, response.toString());

                Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
                Assert.assertEquals(401, response.getStatus());
            }
        } finally {
            userManager.delete(user);
        }
    }

    @Test
    public void testDeleteNoExistingEntity() throws Exception {
        Goid badId = new Goid(0, 0);
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + badId.toString(), HttpMethod.DELETE, null, "");
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 404, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());
    }

    @Test
    public void testDeleteEntityFailed() throws Exception {
        Map<String, Functions.BinaryVoid<String, RestResponse>> entitiesToGet = getUnDeleteableManagedObjectIds();

        for (String id : entitiesToGet.keySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + id, HttpMethod.DELETE, null, "");
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());

            entitiesToGet.get(id).call(id, response);
        }
    }

    @Test
    public void testListEntities() throws Exception {
        Map<String, List<String>> listQueryAndExpectedResults = getListQueryAndExpectedResults();

        Assert.assertNotNull("must have the empty query for a list all", listQueryAndExpectedResults.get(""));
        Assert.assertTrue("must have at least 2 listable elements", listQueryAndExpectedResults.get("").size() > 1);

        for (String query : listQueryAndExpectedResults.keySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), query, HttpMethod.GET, null, "");
            logger.log(Level.FINE, response.toString());
            List<String> expectedIds = listQueryAndExpectedResults.get(query);

            testList(query, response, expectedIds);

            if (query.isEmpty()) {
                List<String> orderedList = new ArrayList<>(expectedIds);
                Collections.sort(orderedList);

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=asc", HttpMethod.GET, null, "");
                testList("sort=id&order=asc", response, orderedList);

                List<String> reverseList = new ArrayList<>(expectedIds);
                Collections.sort(reverseList);
                Collections.reverse(reverseList);

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=desc", HttpMethod.GET, null, "");
                testList("sort=id&order=desc", response, reverseList);

            }
        }
    }

    @Test
    public void testListEntitiesUnprivileged() throws Exception {
        //test unprivileged
        InternalUser user = createUnprivilegedUser();
        try {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "", HttpMethod.GET, null, "", user);
            testList("", response, Collections.<String>emptyList());
        } finally {
            userManager.delete(user);
        }
    }

    @Test
    public void testListEntitiesFailed() throws Exception {
        Map<String, Functions.BinaryVoid<String, RestResponse>> badQueries = getBadListQueries();

        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "badparam=test", HttpMethod.GET, null, "");
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals(400, response.getStatus());

        for (String query : badQueries.keySet()) {
            response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), query, HttpMethod.GET, null, "");
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());

            badQueries.get(query).call(query, response);
        }
    }

    @Test
    public void testGetTemplate() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/template", HttpMethod.GET, null, "");
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        Assert.assertNull("Id for template should be null", item.getId());
        Assert.assertEquals("Type is incorrect", getType(), item.getType());
        Assert.assertEquals("Type is incorrect", getType() + " Template", item.getName());

        Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = findLink("self", item.getLinks());
        Assert.assertNotNull("self link must be present", self);
        Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/template", self.getUri());

        Assert.assertNotNull("template must not be null", item.getContent());
    }

    protected void testList(String query, RestResponse response, List<String> expectedIds) throws java.io.IOException {
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Error for search Query: " + query + "Message: " + "Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<M> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", "List", item.getType());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", getType() + " list", item.getName());

        Assert.assertTrue("Error for search Query: " + query + "Message: " + "Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = findLink("self", item.getLinks());
        Assert.assertNotNull("Error for search Query: " + query + "Message: " + "self link must be present", self);
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "?" + query, self.getUri());

        List<Item<M>> references = item.getContent();

        if (expectedIds.isEmpty()) {
            Assert.assertNull("Error for search Query: " + query, references);
        } else {
            Assert.assertNotNull("Error for search Query: " + query, references);
            Assert.assertEquals("Error for search Query: " + query, expectedIds.size(), references.size());
            for (int i = 0; i < expectedIds.size(); i++) {
                Item<M> entity = references.get(i);

                Assert.assertEquals("Error for search Query: " + query + "Message: " + "Returned entities are either incorrect or in the wrong order. Expected item " + i + " to have a different ID. Expected Order: " + expectedIds.toString() + "\nActual Response:\n" + response.toString(), expectedIds.get(i), entity.getId());
            }
        }
    }

    protected Link findLink(@NotNull String self, List<Link> links) {
        for (Link link : links) {
            if (self.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }

    private static ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator configuredGOIDGenerator = new ConfiguredSessionFactoryBean.ConfiguredGOIDGenerator();

    protected Goid getGoid() {
        return (Goid) configuredGOIDGenerator.generate(null, null);
    }
}
