package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.gateway.api.Item;
import com.l7tech.gateway.api.ItemsList;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.PolicyVersionMO;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.policy.PolicyVersion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.PolicyVersionManager;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ServiceVersionRestEntityResourceTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(ServiceVersionRestEntityResourceTest.class.getName());

    private ServiceManager serviceManager;
    private PolicyVersionManager policyVersionManager;
    private PublishedService publishedService;
    private List<PolicyVersion> policyVersions = new ArrayList<>();

    @Before
    public void before() throws ObjectModelException {
        serviceManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("serviceManager", ServiceManager.class);
        policyVersionManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyVersionManager", PolicyVersionManager.class);

        //Create Policy
        publishedService = new PublishedService();
        publishedService.setPolicy(new Policy(
                PolicyType.PRIVATE_SERVICE,
                "My Policy",
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\"/>\n" +
                        "</wsp:Policy>",
                false
        ));
        serviceManager.save(publishedService);

        policyVersions.add(policyVersionManager.checkpointPolicy(publishedService.getPolicy(), true, true));
        publishedService.getPolicy().setXml(publishedService.getPolicy().getXml()+"<!-- comment1 -->");
        policyVersions.add(policyVersionManager.checkpointPolicy(publishedService.getPolicy(), true, "comment1", false));
        publishedService.getPolicy().setXml(publishedService.getPolicy().getXml()+"<!-- comment2 -->");
        policyVersions.add(policyVersionManager.checkpointPolicy(publishedService.getPolicy(), true, "comment2", false));

    }

    @After
    public void after() throws FindException, DeleteException {
        Collection<PublishedService> all = serviceManager.findAll();
        for (PublishedService service : all) {
            serviceManager.delete(service.getGoid());
        }
    }

    @Test
    public void getPolicyVersion() throws Exception {
        for (PolicyVersion policyVersion : policyVersions) {
            RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + policyVersion.getOrdinal(), HttpMethod.GET, null, "");
            logger.log(Level.INFO, response.toString());

            Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
            Assert.assertEquals("Expected successful response", 200, response.getStatus());
            Assert.assertNotNull("Expected not null response body", response.getBody());

            final StreamSource source = new StreamSource(new StringReader(response.getBody()));
            Item item = MarshallingUtils.unmarshal(Item.class, source);

            Assert.assertEquals("Id's don't match", policyVersion.getId(), item.getId());
            Assert.assertEquals("Type is incorrect", getType().toString(), item.getType());
            Assert.assertEquals("Type is incorrect", getExpectedTitle(policyVersion.getId()), item.getName());

            Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
            Link self = findLink("self", item.getLinks());
            Assert.assertNotNull("self link must be present", self);
            Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + policyVersion.getOrdinal(), self.getUri());

            verifyEntity(policyVersion.getId(), (PolicyVersionMO) item.getContent());
        }
    }

    @Test
    public void getActivePolicyVersion() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/active", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        PolicyVersion activePolicyVersion = policyVersions.get(policyVersions.size()-1);
        Assert.assertEquals("Id's don't match", activePolicyVersion.getId(), item.getId());
        Assert.assertEquals("Type is incorrect", getType().toString(), item.getType());
        Assert.assertEquals("Type is incorrect", getExpectedTitle(activePolicyVersion.getId()), item.getName());

        Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = findLink("self", item.getLinks());
        Assert.assertNotNull("self link must be present", self);
        Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + activePolicyVersion.getOrdinal(), self.getUri());

        verifyEntity(activePolicyVersion.getId(), (PolicyVersionMO) item.getContent());
    }

    @Test
    public void getNonExistingPolicyVersionBadVersion() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/123", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected 404 response", 404, response.getStatus());
    }

    @Test
    public void getNonExistingPolicyVersionBadPolicy() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("services/" + new Goid(0, 123) + "/versions/0", HttpMethod.GET, null, "");
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected 404 response", 404, response.getStatus());
    }

    @Test
    public void updateVersionComment() throws Exception {
        PolicyVersion policyVersion = policyVersions.get(0);
        String updatedComment = "My Updated Comment";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + policyVersion.getOrdinal() + "/comment", HttpMethod.PUT, "text/plain", updatedComment);
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        Assert.assertEquals("Id's don't match", policyVersion.getId(), item.getId());
        Assert.assertEquals("Type is incorrect", getType().toString(), item.getType());
        Assert.assertEquals("Type is incorrect", getExpectedTitle(policyVersion.getId()), item.getName());

        Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = findLink("self", item.getLinks());
        Assert.assertNotNull("self link must be present", self);
        Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + policyVersion.getOrdinal(), self.getUri());

        verifyEntity(policyVersion.getId(), (PolicyVersionMO) item.getContent());
    }

    @Test
    public void updateActiveVersionComment() throws Exception {
        PolicyVersion policyVersion = policyVersions.get(policyVersions.size()-1);
        String updatedComment = "My Updated Comment";
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/active/comment", HttpMethod.PUT, "text/plain", updatedComment);
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        Item item = MarshallingUtils.unmarshal(Item.class, source);

        Assert.assertEquals("Id's don't match", policyVersion.getId(), item.getId());
        Assert.assertEquals("Type is incorrect", getType().toString(), item.getType());
        Assert.assertEquals("Type is incorrect", getExpectedTitle(policyVersion.getId()), item.getName());

        Assert.assertTrue("Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = findLink("self", item.getLinks());
        Assert.assertNotNull("self link must be present", self);
        Assert.assertEquals("self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "/" + policyVersion.getOrdinal(), self.getUri());

        verifyEntity(policyVersion.getId(), (PolicyVersionMO) item.getContent());
    }

    @Test
    public void updateVersionCommentBadVersion() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/123/comment", HttpMethod.PUT, "text/plain", "My Comment");
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected 404 response", 404, response.getStatus());
    }

    @Test
    public void  updateVersionCommentBadPolicy() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("services/" + new Goid(0, 123) + "/versions/0/comment", HttpMethod.PUT, "text/plain", "My Comment");
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected 404 response", 404, response.getStatus());
    }

    @Test
    public void updateVersionCommentTooLong() throws Exception {
        String comment = "";
        for (int i = 0; i < 256; i++) {
            comment+="a";
        }
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/1/comment", HttpMethod.PUT, "text/plain", comment);
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected 400 response", 400, response.getStatus());
    }

    @Test
    public void activatePolicyVersion() throws Exception {
        PolicyVersion policyVersion = policyVersions.get(0);
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/"+policyVersion.getOrdinal()+"/activate", HttpMethod.POST, null, "");
        logger.log(Level.INFO, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 204, response.getStatus());
        Assert.assertTrue("Expected empty response body", response.getBody() == null || response.getBody().isEmpty());

        List<PolicyVersion> versions = policyVersionManager.findAllForPolicy(publishedService.getPolicy().getGoid());
        for(PolicyVersion version : versions){
            if(version.getId().equals(policyVersion.getId())) {
                Assert.assertTrue("Version " + policyVersion.getOrdinal() + " should be active", version.isActive());
            } else {
                Assert.assertFalse("Version " + policyVersion.getOrdinal() + " should not be active", version.isActive());
            }
        }
    }

    public String getResourceUri() {
        return "services/" + publishedService.getId() + "/versions";
    }

    public EntityType getType() {
        return EntityType.POLICY_VERSION;
    }

    public String getExpectedTitle(String id) throws FindException {
        PolicyVersion entity = policyVersionManager.findByPrimaryKey(Goid.parseGoid(id));
        Assert.assertNotNull(entity);
        return "Policy Version: " + entity.getOrdinal();
    }

    public void verifyEntity(String id, PolicyVersionMO managedObject) throws FindException {
        PolicyVersion entity = policyVersionManager.findByPrimaryKey(Goid.parseGoid(id));
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getId(), managedObject.getId());
            Assert.assertEquals(entity.getName(), managedObject.getComment());
            Assert.assertEquals(entity.getOrdinal(), managedObject.getOrdinal());
            Assert.assertEquals(entity.getPolicyGoid().toString(), managedObject.getPolicyId());
            Assert.assertEquals(entity.getTime(), managedObject.getTime());
            Assert.assertEquals(entity.getXml(), managedObject.getXml());
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

                List<String> orderedList = new ArrayList<>(expectedIds);
                Collections.sort(orderedList);

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=asc", HttpMethod.GET, null, "");
                testList("sort=id&order=asc", response, orderedList);

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=asc&count=1", HttpMethod.GET, null, "");
                testList("sort=id&order=asc&count=1", response, orderedList.subList(0, 1));

                response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri(), "sort=id&order=asc&count=1&offset=1", HttpMethod.GET, null, "");
                testList("sort=id&order=asc&count=1&offset=1", response, orderedList.subList(1, 2));

                List<String> reverseList = new ArrayList<>(expectedIds);
                Collections.sort(reverseList);
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

    protected void testList(String query, RestResponse response, List<String> expectedIds) throws java.io.IOException {
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Expected successful response", 200, response.getStatus());
        Assert.assertNotNull("Error for search Query: " + query + "Message: " + "Expected not null response body", response.getBody());

        final StreamSource source = new StreamSource(new StringReader(response.getBody()));
        ItemsList<PolicyVersion> item = MarshallingUtils.unmarshal(ItemsList.class, source);

        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", "List", item.getType());
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "Type is incorrect", getType().toString() + " list", item.getName());

        Assert.assertTrue("Error for search Query: " + query + "Message: " + "Need at least one link", item.getLinks() != null && item.getLinks().size() > 0);
        Link self = findLink("self", item.getLinks());
        Assert.assertNotNull("Error for search Query: " + query + "Message: " + "self link must be present", self);
        Assert.assertEquals("Error for search Query: " + query + "Message: " + "self link is incorrect", getDatabaseBasedRestManagementEnvironment().getUriStart() + getResourceUri() + "?" + query, self.getUri());

        List<Item<PolicyVersion>> references = item.getContent();

        if (expectedIds.isEmpty()) {
            Assert.assertNull("Error for search Query: " + query, references);
        } else {
            Assert.assertNotNull("Error for search Query: " + query, references);
            Assert.assertEquals("Error for search Query: " + query, expectedIds.size(), references.size());
            for (int i = 0; i < expectedIds.size(); i++) {
                Item<PolicyVersion> entity = references.get(i);

                Assert.assertEquals("Error for search Query: " + query + "Message: " + "Returned entities are either incorrect or in the wrong order. Expected item " + i + " to have a different ID. Expected Order: " + expectedIds.toString() + "\nActual Response:\n" + response.toString(), expectedIds.get(i), entity.getId());
            }
        }
    }

    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", Functions.map(policyVersions, new Functions.Unary<String, PolicyVersion>() {
                    @Override
                    public String call(PolicyVersion policyVersion) {
                        return policyVersion.getId();
                    }
                }))
                .put("comment=" + getEncode(policyVersions.get(0).getName()), Arrays.asList(policyVersions.get(0).getId()))
                .put("comment=" + getEncode(policyVersions.get(0).getName()) + "&comment=" + getEncode(policyVersions.get(1).getName()), Functions.map(policyVersions.subList(0, 2), new Functions.Unary<String, PolicyVersion>() {
                    @Override
                    public String call(PolicyVersion policyVersion) {
                        return policyVersion.getId();
                    }
                }))
                .put("comment=banName", Collections.<String>emptyList())
                .put("active=false", Functions.map(policyVersions.subList(0, policyVersions.size() - 1), new Functions.Unary<String, PolicyVersion>() {
                    @Override
                    public String call(PolicyVersion policyVersion) {
                        return policyVersion.getId();
                    }
                }))
                .put("active=true", Arrays.asList(policyVersions.get(policyVersions.size() - 1).getId()))
                .put("id="+policyVersions.get(0).getId(), Arrays.asList(policyVersions.get(0).getId()))
                .put("id="+policyVersions.get(1).getId(), Arrays.asList(policyVersions.get(1).getId()))
                .put("id="+new Goid(123,123), Collections.<String>emptyList())
                .put("id="+policyVersions.get(1).getId()+"&id="+policyVersions.get(2).getId(), Arrays.asList(policyVersions.get(1).getId(), policyVersions.get(2).getId()))
                .put("comment=" + getEncode(policyVersions.get(0).getName()) + "&comment=" + getEncode(policyVersions.get(1).getName()) + "&sort=id&order=desc", Arrays.asList(policyVersions.get(1).getId(), policyVersions.get(0).getId()))
                .map();
    }

    private String getEncode(String string) {
        return string==null?"":URLEncoder.encode(string);
    }

    protected Link findLink(@NotNull String self, List<Link> links) {
        for (Link link : links) {
            if (self.equals(link.getRel())) {
                return link;
            }
        }
        return null;
    }
}
