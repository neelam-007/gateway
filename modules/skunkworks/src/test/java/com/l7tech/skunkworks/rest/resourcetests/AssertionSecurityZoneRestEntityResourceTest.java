package com.l7tech.skunkworks.rest.resourcetests;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.external.assertions.gatewaymanagement.RESTGatewayManagementAssertion;
import com.l7tech.external.assertions.jdbcquery.JdbcQueryAssertion;
import com.l7tech.external.assertions.whichmodule.WhichModuleAssertion;
import com.l7tech.gateway.api.AssertionSecurityZoneMO;
import com.l7tech.gateway.api.Link;
import com.l7tech.gateway.api.ManagedObjectFactory;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.AssertionAccess;
import com.l7tech.policy.AssertionRegistry;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.policy.AssertionAccessManager;
import com.l7tech.server.security.rbac.SecurityZoneManager;
import com.l7tech.skunkworks.rest.tools.RestEntityTests;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import junit.framework.Assert;
import org.apache.http.entity.ContentType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class AssertionSecurityZoneRestEntityResourceTest extends RestEntityTests<AssertionAccess, AssertionSecurityZoneMO> {
    private static final Logger logger = Logger.getLogger(AssertionSecurityZoneRestEntityResourceTest.class.getName());

    private AssertionAccessManager assertionAccessManager;
    private SecurityZoneManager securityZoneManager;
    private SecurityZone securityZone1;
    private SecurityZone securityZone2;
    private ArrayList<AssertionAccess> assertionAccesses = new ArrayList<>();
    private AssertionRegistry assertionRegistry;
    private AssertionAccess assertionAccessUnregistered;

    @Before
    public void before() throws Exception {
        assertionAccessManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("assertionAccessManager", AssertionAccessManager.class);
        securityZoneManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("securityZoneManager", SecurityZoneManager.class);
        assertionRegistry = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("assertionRegistry", AssertionRegistry.class);

        securityZone1 = new SecurityZone();
        securityZone1.setName("Zone1");
        securityZone1.setPermittedEntityTypes(CollectionUtils.set(EntityType.ASSERTION_ACCESS));
        securityZoneManager.save(securityZone1);

        securityZone2 = new SecurityZone();
        securityZone2.setName("Zone2");
        securityZone2.setPermittedEntityTypes(CollectionUtils.set(EntityType.ASSERTION_ACCESS));
        securityZoneManager.save(securityZone2);

        assertionAccessUnregistered = new AssertionAccess("some.assertion.class");
        assertionAccessUnregistered.setSecurityZone(securityZone1);
        assertionAccessManager.save(assertionAccessUnregistered);

        AssertionAccess assertionAccess = new AssertionAccess(RESTGatewayManagementAssertion.class.getName());
        assertionAccess.setSecurityZone(securityZone1);
        assertionAccessManager.save(assertionAccess);
        assertionAccesses.add(assertionAccess);

        assertionAccess = new AssertionAccess(JdbcQueryAssertion.class.getName());
        assertionAccess.setSecurityZone(securityZone2);
        assertionAccessManager.save(assertionAccess);
        assertionAccesses.add(assertionAccess);

    }

    @After
    public void after() throws FindException, DeleteException {
        ArrayList<AssertionAccess> assertionAccesses = new ArrayList<>(assertionAccessManager.findAll());
        for (AssertionAccess assertionAccess : assertionAccesses) {
            assertionAccessManager.delete(assertionAccess.getGoid());
        }

        securityZoneManager.delete(securityZone1);
        securityZoneManager.delete(securityZone2);
    }

    @Override
    public List<String> getRetrievableEntityIDs() throws FindException {
        List<String> assertions = Functions.map(assertionAccesses, new Functions.Unary<String, AssertionAccess>() {
            @Override
            public String call(AssertionAccess assertionAccess) {
                return assertionAccess.getName();
            }
        });
        assertions.addAll(Functions.map(assertionRegistry.getAssertions(), new Functions.Unary<String, Assertion>() {
            @Override
            public String call(Assertion assertion) {
                return assertion.getClass().getName();
            }
        }));
        return assertions;
    }

    @Override
    public List<AssertionSecurityZoneMO> getCreatableManagedObjects() {
        return Collections.emptyList();
    }

    @Override
    public List<AssertionSecurityZoneMO> getUpdateableManagedObjects() {
        List<AssertionSecurityZoneMO> assertionSecurityZoneMOs = new ArrayList<>();

        AssertionSecurityZoneMO assertionSecurityZoneMO = ManagedObjectFactory.createAssertionAccess();
        assertionSecurityZoneMO.setName(JdbcQueryAssertion.class.getName());
        assertionSecurityZoneMO.setSecurityZoneId(securityZone1.getId());
        assertionSecurityZoneMOs.add(assertionSecurityZoneMO);

        assertionSecurityZoneMO = ManagedObjectFactory.createAssertionAccess();
        assertionSecurityZoneMO.setName(JdbcQueryAssertion.class.getName());
        assertionSecurityZoneMO.setSecurityZoneId(null);
        assertionSecurityZoneMOs.add(assertionSecurityZoneMO);

        return assertionSecurityZoneMOs;
    }

    @Override
    public Map<AssertionSecurityZoneMO, Functions.BinaryVoid<AssertionSecurityZoneMO, RestResponse>> getUnCreatableManagedObjects() {
        return Collections.emptyMap();
    }

    @Override
    public Map<AssertionSecurityZoneMO, Functions.BinaryVoid<AssertionSecurityZoneMO, RestResponse>> getUnUpdateableManagedObjects() {
        CollectionUtils.MapBuilder<AssertionSecurityZoneMO, Functions.BinaryVoid<AssertionSecurityZoneMO, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        AssertionSecurityZoneMO assertionSecurityZoneMO = ManagedObjectFactory.createAssertionAccess();
        assertionSecurityZoneMO.setName("my.new.assertion.class");
        assertionSecurityZoneMO.setSecurityZoneId(securityZone1.getId());
        builder.put(assertionSecurityZoneMO, new Functions.BinaryVoid<AssertionSecurityZoneMO, RestResponse>() {
            @Override
            public void call(AssertionSecurityZoneMO assertionSecurityZoneMO, RestResponse restResponse) {
                Assert.assertEquals(404, restResponse.getStatus());
            }
        });

        assertionSecurityZoneMO = ManagedObjectFactory.createAssertionAccess();
        assertionSecurityZoneMO.setName(assertionAccessUnregistered.getName());
        assertionSecurityZoneMO.setSecurityZoneId(securityZone1.getId());
        builder.put(assertionSecurityZoneMO, new Functions.BinaryVoid<AssertionSecurityZoneMO, RestResponse>() {
            @Override
            public void call(AssertionSecurityZoneMO assertionSecurityZoneMO, RestResponse restResponse) {
                Assert.assertEquals(404, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Test
    public void testUpdateEntityDifferentIDFailed() throws Exception {
        AssertionSecurityZoneMO assertionSecurityZoneMO = ManagedObjectFactory.createAssertionAccess();
        assertionSecurityZoneMO.setName(WhichModuleAssertion.class.getName());
        assertionSecurityZoneMO.setSecurityZoneId(securityZone1.getId());

        String urlName = JdbcQueryAssertion.class.getName();
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/" + urlName, HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(assertionSecurityZoneMO)));
        logger.log(Level.FINE, response.toString());

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 400, response.getStatus());
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnGettableManagedObjectIds() {
        CollectionUtils.MapBuilder<String, Functions.BinaryVoid<String, RestResponse>> builder = CollectionUtils.MapBuilder.builder();

        //SSG-8319
        builder.put("my.new.assertion.class", new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String name, RestResponse restResponse) {
                Assert.assertEquals(404, restResponse.getStatus());
            }
        });

        builder.put(assertionAccessUnregistered.getName(), new Functions.BinaryVoid<String, RestResponse>() {
            @Override
            public void call(String name, RestResponse restResponse) {
                Assert.assertEquals(404, restResponse.getStatus());
            }
        });

        return builder.map();
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getUnDeleteableManagedObjectIds() {
        return Collections.emptyMap();
    }

    @Test
    public void testDeleteNoExistingEntity() throws Exception {
        //method not allowed
    }

    @Override
    public Map<String, Functions.BinaryVoid<String, RestResponse>> getBadListQueries() {
        return Collections.emptyMap();
    }

    @Test
    public void testDeleteEntity() throws Exception {
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest(getResourceUri() + "/some.class", HttpMethod.DELETE, null, "");

        Assert.assertEquals("Expected successful assertion status", AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Expected successful response", 405, response.getStatus());
    }

    @Override
    public List<String> getDeleteableManagedObjectIDs() {
        return Collections.emptyList();
    }

    @Override
    public String getResourceUri() {
        return "assertionSecurityZones";
    }

    @Override
    public String getType() {
        return EntityType.ASSERTION_ACCESS.name();
    }

    @Override
    protected String getId(AssertionSecurityZoneMO item) {
        return item.getName();
    }

    @Override
    protected String getIdName() {
        return "name";
    }

    @Override
    public String getExpectedTitle(String id) throws FindException {
        return id;
    }

    @Override
    public void verifyLinks(String id, List<Link> links) throws FindException {
    }

    @Override
    public void verifyEntity(String id, AssertionSecurityZoneMO managedObject) throws Exception {
        AssertionAccess entity = assertionAccessManager.findByUniqueName(id);
        if(entity == null && assertionRegistry.isAssertionRegistered(id)){
            entity = new AssertionAccess(id);
        }
        if (managedObject == null) {
            Assert.assertNull(entity);
        } else {
            Assert.assertNotNull(entity);

            Assert.assertEquals(entity.getName(), managedObject.getName());
            if(entity.getSecurityZone() != null) {
                Assert.assertEquals(entity.getSecurityZone().getId(), managedObject.getSecurityZoneId());
            } else {
                Assert.assertNull(managedObject.getSecurityZoneId());
            }
        }
    }

    @Override
    public Map<String, List<String>> getListQueryAndExpectedResults() throws FindException {
        List<String> assertions = Functions.map(assertionRegistry.getAssertions(), new Functions.Unary<String, Assertion>() {
            @Override
            public String call(Assertion assertion) {
                return assertion.getClass().getName();
            }
        });
        return CollectionUtils.MapBuilder.<String, List<String>>builder()
                .put("", assertions)
                .put("name=" + URLEncoder.encode(assertionAccesses.get(0).getName()), Arrays.asList(assertionAccesses.get(0).getName()))
                .put("name=banName", Collections.<String>emptyList())
                .put("securityZone.id=" + Goid.DEFAULT_GOID, Functions.reduce(assertionRegistry.getAssertions(), new ArrayList<String>(), new Functions.Binary<ArrayList<String>, ArrayList<String>, Assertion>() {
                    @Override
                    public ArrayList<String> call(ArrayList<String> strings, Assertion assertion) {
                        AssertionAccess assertionAccess = assertionAccessManager.getAssertionAccessCached(assertion);
                        if (assertionAccess != null && assertionAccess.getSecurityZone() == null) {
                            strings.add(assertionAccess.getName());
                        }
                        return strings;
                    }
                }))
                .put("securityZone.id=" + securityZone1.getId(), Arrays.asList(assertionAccesses.get(0).getName()))
                .put("securityZone.id=" + securityZone2.getId(), Arrays.asList(assertionAccesses.get(1).getName()))
                .put("name=" + URLEncoder.encode(assertionAccesses.get(0).getName()) + "&name=" + URLEncoder.encode(assertionAccesses.get(1).getName()) + "&sort=name&order=desc", Arrays.asList(assertionAccesses.get(1).getName(), assertionAccesses.get(0).getName()))
                .map();
    }
}
