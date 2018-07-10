package com.l7tech.skunkworks.rest.migration;

import com.l7tech.common.http.HttpMethod;
import com.l7tech.common.io.XmlUtil;
import com.l7tech.gateway.api.*;
import com.l7tech.gateway.api.impl.MarshallingUtils;
import com.l7tech.objectmodel.EntityHeader;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.assertion.AssertionStatus;
import com.l7tech.server.folder.FolderManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.skunkworks.rest.tools.MigrationTestBase;
import com.l7tech.skunkworks.rest.tools.RestEntityTestBase;
import com.l7tech.skunkworks.rest.tools.RestResponse;
import com.l7tech.test.BugId;
import com.l7tech.test.conditional.ConditionalIgnore;
import com.l7tech.test.conditional.IgnoreOnDaily;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import com.l7tech.util.SyspropUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.http.entity.ContentType;
import org.hamcrest.Matchers;
import org.junit.*;

import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tests for read-only entities during migration.
 */
@ConditionalIgnore(condition = IgnoreOnDaily.class)
public class ReadOnlyEntitiesTest extends RestEntityTestBase {
    private static final Logger logger = Logger.getLogger(ReadOnlyEntitiesTest.class.getName());

    private ProtectedEntityTracker protectedEntityTracker;

    private Item<FolderMO> readOnlyFolder;
    private Item<PolicyMO> readOnlyPolicy;


    @BeforeClass
    public static void beforeClass() throws Exception {
        RestEntityTestBase.beforeClass();
    }

    // for now test with folder ans policy
    // todo: perhaps add more entity types in the future
    @Before
    public void before() throws Exception {
        protectedEntityTracker = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("protectedEntityTracker", ProtectedEntityTracker.class);
        Assert.assertNotNull(protectedEntityTracker);
        Assert.assertTrue(protectedEntityTracker.isEnabled());
        Assert.assertTrue(protectedEntityTracker.isEntityProtectionEnabled());

        final Collection<Pair<EntityType, String>> readOnlyEntitiyes = new HashSet<>();

        final FolderManager folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        Assert.assertNotNull(folderManager);

        // create test folder
        FolderMO folderMO = ManagedObjectFactory.createFolder();
        folderMO.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        folderMO.setName("Source parent folder");
        // send the request
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("folders", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(folderMO)));
        assertOkCreatedResponse(response);
        // extract the folder item
        readOnlyFolder = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getDatabaseBasedRestManagementEnvironment().processRequest("folders/" + readOnlyFolder.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        readOnlyFolder = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertNotNull(readOnlyFolder);
        Assert.assertNotNull(readOnlyFolder.getContent());
        Assert.assertThat(readOnlyFolder.getId(), Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(readOnlyFolder.getType(), Matchers.is(EntityType.FOLDER.name()));
        // add it as readonly
        Assert.assertThat(readOnlyEntitiyes, Matchers.not(Matchers.hasItem(Pair.pair(EntityType.valueOf(readOnlyFolder.getType()), readOnlyFolder.getId()))));
        readOnlyEntitiyes.add(Pair.pair(EntityType.valueOf(readOnlyFolder.getType()), readOnlyFolder.getId()));

        // create test policy
        final String assXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                        "    <wsp:All wsp:Usage=\"Required\">\n" +
                        "        <L7p:AuditDetailAssertion>\n" +
                        "            <L7p:Detail stringValue=\"HI 2\"/>\n" +
                        "        </L7p:AuditDetailAssertion>\n" +
                        "    </wsp:All>\n" +
                        "</wsp:Policy>";
        PolicyMO policyMO = ManagedObjectFactory.createPolicy();
        PolicyDetail policyDetail = ManagedObjectFactory.createPolicyDetail();
        policyMO.setPolicyDetail(policyDetail);
        policyDetail.setName("Source Policy");
        policyDetail.setPolicyType(PolicyDetail.PolicyType.INCLUDE);
        policyDetail.setFolderId(Folder.ROOT_FOLDER_ID.toString());
        policyDetail.setProperties(CollectionUtils.MapBuilder.<String, Object>builder().put("soap", false).map());
        ResourceSet policyResourceSet = ManagedObjectFactory.createResourceSet();
        policyResourceSet.setTag("policy");
        Resource policyResource = ManagedObjectFactory.createResource();
        policyResourceSet.setResources(Arrays.asList(policyResource));
        policyResource.setType("policy");
        policyResource.setContent(assXml );
        policyMO.setResourceSets(Arrays.asList(policyResourceSet));
        // send the request
        response = getDatabaseBasedRestManagementEnvironment().processRequest("policies", HttpMethod.POST, ContentType.APPLICATION_XML.toString(), XmlUtil.nodeToString(ManagedObjectFactory.write(policyMO)));
        assertOkCreatedResponse(response);
        // extract the folder item
        readOnlyPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        response = getDatabaseBasedRestManagementEnvironment().processRequest("policies/" + readOnlyPolicy.getId(), HttpMethod.GET, null, "");
        assertOkResponse(response);
        readOnlyPolicy = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));
        Assert.assertNotNull(readOnlyPolicy);
        Assert.assertNotNull(readOnlyPolicy.getContent());
        Assert.assertThat(readOnlyPolicy.getId(), Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertThat(readOnlyPolicy.getType(), Matchers.is(EntityType.POLICY.name()));
        // add it as readonly
        Assert.assertThat(readOnlyEntitiyes, Matchers.not(Matchers.hasItem(Pair.pair(EntityType.valueOf(readOnlyPolicy.getType()), readOnlyPolicy.getId()))));
        readOnlyEntitiyes.add(Pair.pair(EntityType.valueOf(readOnlyPolicy.getType()), readOnlyPolicy.getId()));

        // finally update our read-only list
        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(readOnlyEntitiyes);
        // make sure our entities are indeed read-only
        Assert.assertNotNull(protectedEntityTracker.getEntityProtection(readOnlyFolder.getId()));
        //noinspection ConstantConditions
        Assert.assertTrue(protectedEntityTracker.getEntityProtection(readOnlyFolder.getId()).isReadOnly());
        Assert.assertNotNull(protectedEntityTracker.getEntityProtection(readOnlyPolicy.getId()));
        //noinspection ConstantConditions
        Assert.assertTrue(protectedEntityTracker.getEntityProtection(readOnlyPolicy.getId()).isReadOnly());
    }

    static void assertOkCreatedResponse(final RestResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response: " + response.getStatus() + "\n" + response.getBody(), 201, response.getStatus());
        Assert.assertNotNull(response.getBody());
    }

    static void assertOkResponse(final RestResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response: " + response.getStatus() + "\n" + response.getBody(), 200, response.getStatus());
    }

    static void assertConflictResponse(final RestResponse response) {
        Assert.assertNotNull(response);
        Assert.assertEquals(AssertionStatus.NONE, response.getAssertionStatus());
        Assert.assertEquals("Unexpected Response: " + response.getStatus() + "\n" + response.getBody(), 409, response.getStatus());
    }

    static String objectToString(final Object object) throws IOException {
        Assert.assertNotNull(object);
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final StreamResult result = new StreamResult(bout);
        MarshallingUtils.marshal(object, result, false);
        return bout.toString();
    }

    @After
    public void after() throws Exception {
        // clean-up

        final FolderManager folderManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("folderManager", FolderManager.class);
        Assert.assertNotNull(folderManager);
        final PolicyManager policyManager = getDatabaseBasedRestManagementEnvironment().getApplicationContext().getBean("policyManager", PolicyManager.class);
        Assert.assertNotNull(policyManager);

        for(EntityHeader header : policyManager.findAllHeaders()) {
            policyManager.delete(header.getGoid());
        }
        Assert.assertThat(policyManager.findAllHeaders(), Matchers.empty());

        for(EntityHeader header : folderManager.findAllHeaders()) {
            if (!Folder.ROOT_FOLDER_ID.equals(header.getGoid())) {
                folderManager.delete(header.getGoid());
            }
        }
        Assert.assertThat(folderManager.findAllHeaders(), Matchers.hasSize(1));
        Assert.assertThat(folderManager.findAllHeaders().iterator().next(), Matchers.notNullValue());
        Assert.assertThat(folderManager.findAllHeaders().iterator().next().getGoid(), Matchers.equalTo(Folder.ROOT_FOLDER_ID));
    }

    static Mapping buildMapping(final Item item, final Mapping.Action action, final Map<String,Object> properties) {
        return buildMapping(item, action, null, properties);
    }

    static Mapping buildMapping(final Item item, final Mapping.Action action) {
        return buildMapping(item, action, null, null);
    }

    static Mapping buildMapping(final Item item, final Mapping.Action action, final String optionalTargetId, final Map<String,Object> optionalProperties) {
        Assert.assertNotNull(item);
        Assert.assertThat(item.getId(), Matchers.not(Matchers.isEmptyOrNullString()));
        Assert.assertNotNull(action);

        Mapping mapping = ManagedObjectFactory.createMapping();
        mapping.setAction(action);
        mapping.setSrcId(item.getId());
        mapping.setSrcUri("http://localhost:80/restman/1.0/policies/" + item.getId());
        mapping.setType(item.getType());
        if (StringUtils.isNotEmpty(optionalTargetId)) {
            mapping.setTargetId(optionalTargetId);
            mapping.setTargetUri("http://localhost:80/restman/1.0/policies/" + optionalTargetId);
        }
        if (optionalProperties != null) {
            mapping.setProperties(optionalProperties);
        }

        return mapping;
    }

    private void doTestReadOnly(
            final Mapping.Action action,
            final Functions.UnaryVoid<RestResponse> responseValidator,
            final Functions.UnaryVoid<Collection<Mapping>> mappingsValidator // order if folder then policy
    ) throws Exception {
        Assert.assertNotNull(action);

        Bundle bundle = ManagedObjectFactory.createBundle();
        bundle.setMappings(
                Arrays.asList(
                        buildMapping(
                                new ItemBuilder<FolderMO>("root-folder", Folder.ROOT_FOLDER_ID.toString(), EntityType.FOLDER.name()).build(),
                                Mapping.Action.NewOrExisting,
                                CollectionUtils.<String, Object>mapBuilder().put("FailOnNew", true).unmodifiableMap()
                        ),
                        buildMapping(readOnlyFolder, action),
                        buildMapping(readOnlyPolicy, action)
                )
        );
        bundle.setReferences(
                Arrays.<Item>asList(
                        readOnlyFolder,
                        readOnlyPolicy
                )
        );
        logger.log(Level.INFO, objectToString(bundle));
        RestResponse response = getDatabaseBasedRestManagementEnvironment().processRequest("bundle", "test=true", HttpMethod.PUT, ContentType.APPLICATION_XML.toString(), objectToString(bundle));
        responseValidator.call(response);

        Item<Mappings> mappings = MarshallingUtils.unmarshal(Item.class, new StreamSource(new StringReader(response.getBody())));

        //verify the mappings
        Assert.assertEquals("There should be 3 mapping after the import", 3, mappings.getContent().getMappings().size());

        Mapping folderMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), readOnlyFolder.getId());
        Assert.assertNotNull(folderMapping);
        Assert.assertThat(folderMapping.getSrcId(), Matchers.equalTo(readOnlyFolder.getId()));
        Assert.assertThat(folderMapping.getType(), Matchers.equalTo(EntityType.FOLDER.toString()));
        Assert.assertThat(folderMapping.getAction(), Matchers.is(action));

        Mapping policyMapping = MigrationTestBase.getMapping(mappings.getContent().getMappings(), readOnlyPolicy.getId());
        Assert.assertNotNull(policyMapping);
        Assert.assertThat(policyMapping.getSrcId(), Matchers.equalTo(readOnlyPolicy.getId()));
        Assert.assertThat(policyMapping.getType(), Matchers.equalTo(EntityType.POLICY.toString()));
        Assert.assertThat(policyMapping.getAction(), Matchers.is(action));

        mappingsValidator.call(Arrays.asList(folderMapping, policyMapping));
    }

    @Test
    @BugId("SSG-12813")
    public void testMigrateReadOnlyEntities() throws Exception {
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test update and delete of read-only entities
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        Functions.UnaryVoid<RestResponse> responseValidator = new Functions.UnaryVoid<RestResponse>() {
            @Override
            public void call(final RestResponse response) {
                assertConflictResponse(response);
            }
        };
        Functions.UnaryVoid<Collection<Mapping>> mappingsValidator = new Functions.UnaryVoid<Collection<Mapping>>() {
            @Override
            public void call(final Collection<Mapping> mappings) {
                for (Mapping mapping : mappings) {
                    Assert.assertThat(mapping.getErrorType(), Matchers.is(Mapping.ErrorType.TargetReadOnly));
                    Assert.assertThat(mapping.<String>getProperty("ErrorMessage"), Matchers.containsString("Target entity is read-only: Not possible to"));
                }
            }
        };

        doTestReadOnly(Mapping.Action.NewOrUpdate, responseValidator, mappingsValidator);
        doTestReadOnly(Mapping.Action.Delete, responseValidator, mappingsValidator);
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test update with used existing
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        doTestReadOnly(
                Mapping.Action.NewOrExisting,
                new Functions.UnaryVoid<RestResponse>() {
                    @Override
                    public void call(final RestResponse response) {
                        assertOkResponse(response);
                    }
                },
                new Functions.UnaryVoid<Collection<Mapping>>() {
                    @Override
                    public void call(final Collection<Mapping> mappings) {
                        for (Mapping mapping : mappings) {
                            Assert.assertNull(mapping.getErrorType());
                            Assert.assertNull(mapping.getProperty("ErrorMessage"));
                            Assert.assertThat(mapping.getActionTaken(), Matchers.is(Mapping.ActionTaken.UsedExisting));
                            Assert.assertThat(mapping.getTargetId(), Matchers.equalTo(mapping.getSrcId()));
                        }
                    }
                }
        );
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test update and delete of read-only entities with entity protector disabled
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // finally test with disabled protector
        SyspropUtil.setProperty("com.l7tech.server.protectedEntityTracker.enable", Boolean.FALSE.toString());
        Assert.assertFalse(protectedEntityTracker.isEnabled());
        Assert.assertFalse(protectedEntityTracker.isEntityProtectionEnabled());

        responseValidator = new Functions.UnaryVoid<RestResponse>() {
            @Override
            public void call(final RestResponse response) {
                assertOkResponse(response);
            }
        };
        doTestReadOnly(
                Mapping.Action.NewOrUpdate,
                responseValidator,
                new Functions.UnaryVoid<Collection<Mapping>>() {
                    @Override
                    public void call(final Collection<Mapping> mappings) {
                        for (Mapping mapping : mappings) {
                            Assert.assertNull(mapping.getErrorType());
                            Assert.assertNull(mapping.getProperty("ErrorMessage"));
                            Assert.assertThat(mapping.getActionTaken(), Matchers.is(Mapping.ActionTaken.UpdatedExisting));
                            Assert.assertThat(mapping.getTargetId(), Matchers.equalTo(mapping.getSrcId()));
                        }
                    }
                }
        );

        doTestReadOnly(
                Mapping.Action.Delete,
                responseValidator,

                new Functions.UnaryVoid<Collection<Mapping>>() {
                    @Override
                    public void call(final Collection<Mapping> mappings) {
                        for (Mapping mapping : mappings) {
                            Assert.assertNull(mapping.getErrorType());
                            Assert.assertNull(mapping.getProperty("ErrorMessage"));
                            Assert.assertThat(mapping.getActionTaken(), Matchers.is(Mapping.ActionTaken.Deleted));
                            Assert.assertThat(mapping.getTargetId(), Matchers.equalTo(mapping.getSrcId()));
                        }
                    }
                }
        );
        //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }
}
