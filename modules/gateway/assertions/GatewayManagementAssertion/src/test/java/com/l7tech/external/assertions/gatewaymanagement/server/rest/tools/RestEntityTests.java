package com.l7tech.external.assertions.gatewaymanagement.server.rest.tools;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.server.RestResponse;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.Goid;
import com.l7tech.objectmodel.PersistentEntity;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.util.ConfiguredSessionFactoryBean;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class RestEntityTests<E extends PersistentEntity, M extends ManagedObject> extends RestEntityTestBase implements RestEntityResourceUtil<E, M> {
    private static final Logger logger = Logger.getLogger(RestEntityTests.class.getName());

    @Test
    public void testGet() throws Exception {
        List<String> entitiesExpected = getRetrievableEntityIDs();

        for (String entityId : entitiesExpected) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + entityId, HttpMethod.GET, null, "");
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 200, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

            Assert.assertEquals("Id's don't match", entityId, reference.getId());
            Assert.assertEquals("Type is incorrect", getType().toString(), reference.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(entityId), reference.getTitle());

            Assert.assertTrue("Need at least one link", reference.getLinks() != null && reference.getLinks().size() > 0);
            Link self = findLink("self", reference.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + entityId, self.getUri());

            verifyLinks(entityId, reference.getLinks());

            verifyEntity(entityId, (M) reference.getResource());
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
            Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

            Assert.assertEquals("Type is incorrect", getType().toString(), reference.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(reference.getId()), reference.getTitle());

            Assert.assertTrue("Need at least one link", reference.getLinks() != null && reference.getLinks().size() > 0);
            Link self = findLink("self", reference.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + reference.getId(), self.getUri());

            verifyLinks(reference.getId(), reference.getLinks());

            Assert.assertNull(reference.getResource());
            mo.setId(reference.getId());
            verifyEntity(reference.getId(), mo);
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
    public void testCreateWithIdEntity() throws Exception {
        List<M> entitiesToCreate = getCreatableManagedObjects();

        for (M mo : entitiesToCreate) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + mo.getId(), HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 201, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

            Assert.assertEquals("Id's don't match", mo.getId(), reference.getId());
            Assert.assertEquals("Type is incorrect", getType().toString(), reference.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(mo.getId()), reference.getTitle());

            Assert.assertTrue("Need at least one link", reference.getLinks() != null && reference.getLinks().size() > 0);
            Link self = findLink("self", reference.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + mo.getId(), self.getUri());

            verifyLinks(mo.getId(), reference.getLinks());

            Assert.assertNull(reference.getResource());
            verifyEntity(mo.getId(), mo);
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
            Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

            Assert.assertEquals("Id's don't match", mo.getId(), reference.getId());
            Assert.assertEquals("Type is incorrect", getType().toString(), reference.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(mo.getId()), reference.getTitle());

            Assert.assertTrue("Need at least one link", reference.getLinks() != null && reference.getLinks().size() > 0);
            Link self = findLink("self", reference.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + mo.getId(), self.getUri());

            verifyLinks(mo.getId(), reference.getLinks());

            Assert.assertNull(reference.getResource());
            verifyEntity(mo.getId(), mo);
        }
    }

    @Test
    public void testUpdateEntityFailed() throws Exception {
        Map<M,Functions.BinaryVoid<M,RestResponse>> entitiesToCreate = getUnUpdateableManagedObjects();

        for (M mo : entitiesToCreate.keySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + mo.getId(), HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(mo)));
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());

            entitiesToCreate.get(mo).call(mo, response);
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
                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "count=1", HttpMethod.GET, null, "");
                testList("count=1", response, expectedIds.subList(0, 1));

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "count=1&offset=1", HttpMethod.GET, null, "");
                testList("count=1&offset=1", response, expectedIds.subList(1, 2));

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "offset=500", HttpMethod.GET, null, "");
                testList("offset=500", response, Collections.<String>emptyList());

                List<String> reverseList = new ArrayList<>(expectedIds);
                Collections.reverse(reverseList);

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=desc", HttpMethod.GET, null, "");
                testList("sort=id&order=desc", response, reverseList);

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=desc&count=1", HttpMethod.GET, null, "");
                testList("sort=id&order=desc&count=1", response, reverseList.subList(0, 1));

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=desc&count=1&offset=1", HttpMethod.GET, null, "");
                testList("sort=id&order=desc&count=1&offset=1", response, reverseList.subList(1, 2));
            }
        }
    }

    @Test
    public void testListEntitiesFailed() throws Exception {
        Map<String, Functions.BinaryVoid<String, RestResponse>> badQueries = getBadListQueries();

        for (String query : badQueries.keySet()) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), query, HttpMethod.GET, null, "");
            logger.log(Level.FINE, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());

            badQueries.get(query).call(query, response);
        }
    }

    protected void testList(String query, RestResponse response, List<String> expectedIds) throws java.io.IOException {
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Error for search Query: " + query + "Message: " + "Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Reference reference = MarshallingUtils.unmarshal(Reference.class, source);

        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", "List", reference.getType());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", getType().toString() + " list", reference.getTitle());

        Assert.assertTrue("Error for search Query: " + query + "Message: " + "Need at least one link", reference.getLinks() != null && reference.getLinks().size() > 0);
        Link self = findLink("self", reference.getLinks());
        Assert.assertNotNull("Error for search Query: " + query + "Message: " + "self link must be present", self);
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "?" + query, self.getUri());

        Assert.assertNotNull("Error for search Query: " + query, reference.getResource());
        Assert.assertTrue("Error for search Query: " + query, reference.getResource() instanceof References);

        References references = (References) reference.getResource();

        if (expectedIds.isEmpty()) {
            Assert.assertNull("Error for search Query: " + query, references.getReferences());
        } else {
            Assert.assertNotNull("Error for search Query: " + query, references.getReferences());
            Assert.assertEquals("Error for search Query: " + query, expectedIds.size(), references.getReferences().size());
            for (int i = 0; i < expectedIds.size(); i++) {
                Reference<M> entity = references.getReferences().get(i);

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
