package com.l7tech.server.solutionkit;

import com.l7tech.gateway.api.Mapping;
import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.gateway.common.solutionkit.SolutionKitBuilder;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.policy.bundle.ssgman.restman.RestmanMessage;
import com.l7tech.test.BugId;
import com.l7tech.util.DomUtils;
import com.l7tech.util.Functions;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.w3c.dom.Element;

import java.text.MessageFormat;
import java.util.*;

import static com.l7tech.server.policy.bundle.GatewayManagementDocumentUtilities.MGMT_VERSION_NAMESPACE;

/**
 * {@code SolutionKitAdminHelper} unit test.<br/>
 * Feel free to add more {@code SolutionKitAdminHelper} tests here.
 */
@RunWith(MockitoJUnitRunner.class)
public class SolutionKitAdminHelperTest {

    @Mock
    private LicenseManager licenseManager;
    @Mock
    private IdentityProviderConfigManager identityProviderConfigManager;

    private SolutionKitManager solutionKitManager;

    private SolutionKitAdminHelper solutionKitAdminHelper;

    private static Map<Goid, String> goidToGuidMap = new HashMap<>();

    @Before
    public void before() throws Exception {
        final SolutionKit[] kits = createSampleSolutionKits();
        solutionKitManager = Mockito.spy(new SolutionKitManagerStub(kits));
        Assert.assertThat(solutionKitManager.findAll(), Matchers.containsInAnyOrder(kits));
        solutionKitAdminHelper = Mockito.spy(new SolutionKitAdminHelper(licenseManager, solutionKitManager, identityProviderConfigManager));
        Assert.assertNotNull("SolutionKitAdminHelper is created", solutionKitAdminHelper);

        goidToGuidMap.clear();
    }

    private static Goid newGoid(long ord) throws Exception {
        return new Goid(0, ord);
    }

    private static String newGuid(final Goid goid) throws Exception {
        Assert.assertNotNull(goid);
        goidToGuidMap.put(goid, UUID.randomUUID().toString());
        return goidToGuidMap.get(goid);
    }

    /**
     * SOLUTION KIT sk1 => single solution kit
     *          sk1_entity1 => JDBC_CONNECTION      => true
     *          sk1_entity2 => SECURE_PASSWORD      => true
     *          sk1_entity3 => EMAIL_LISTENER       => false
     *          sk1_entity4 => SERVER_MODULE_FILE   => true
     *          sk1_entity5 => SERVER_MODULE_FILE   => false
     *          sk1_entity6 => FOLDER               => true
     *          sk1_entity7 => POLICY               => true
     *          sk1_entity8 => SERVICE              => false
     *
     *
     * SOLUTION KIT sk2 => collection of scars
     *      SOLUTION KIT sk3 => child kit
     *          sk3_entity1 => CUSTOM_KEY_VALUE_STORE   => true
     *          sk3_entity2 => SECURE_PASSWORD          => true
     *          sk3_entity3 => SECURE_PASSWORD          => false
     *          sk3_entity4 => POLICY                   => true
     *          sk3_entity5 => SERVICE                  => false
     *      SOLUTION KIT sk4 => child kit
     *          sk4_entity1 => SERVER_MODULE_FILE   => false
     *      SOLUTION KIT sk5 => child kit
     *          sk5_entity1 => SECURE_PASSWORD   => false
     *          sk5_entity2 => SECURE_PASSWORD   => false
     *          sk5_entity3 => FOLDER            => false
     *          sk5_entity4 => FOLDER            => false
     *          sk5_entity5 => POLICY            => false
     *          sk5_entity4 => SERVICE           => false
     *
     *
     * SOLUTION KIT sk6 => child kit
     *          sk5_entity1 => SERVICE   => true
     */
    private static SolutionKit[] createSampleSolutionKits() throws Exception {
        return new SolutionKit[] {
                new SolutionKitBuilder()
                        .goid(newGoid(1))
                        .name("sk 1")
                        .skGuid(newGuid(newGoid(1)))
                        .skVersion("1.0")
                        .addOwnershipDescriptor("sk1_entity1", EntityType.JDBC_CONNECTION, true)
                        .addOwnershipDescriptor("sk1_entity2", EntityType.SECURE_PASSWORD, true)
                        .addOwnershipDescriptor("sk1_entity3", EntityType.EMAIL_LISTENER, false)
                        .addOwnershipDescriptor("sk1_entity4", EntityType.SERVER_MODULE_FILE, true)
                        .addOwnershipDescriptor("sk1_entity5", EntityType.SERVER_MODULE_FILE, false)
                        .addOwnershipDescriptor("sk1_entity6", EntityType.FOLDER, true)
                        .addOwnershipDescriptor("sk1_entity7", EntityType.POLICY, true)
                        .addOwnershipDescriptor("sk1_entity8", EntityType.SERVICE, false)
                        .build(),
                new SolutionKitBuilder()
                        .goid(newGoid(2))
                        .name("parent sk2")
                        .skGuid(newGuid(newGoid(2)))
                        .skVersion("2.0")
                        .build(),
                new SolutionKitBuilder()
                        .goid(newGoid(3))
                        .name("child sk3")
                        .skGuid(newGuid(newGoid(3)))
                        .skVersion("2.0")
                        .addOwnershipDescriptor("sk3_entity1", EntityType.CUSTOM_KEY_VALUE_STORE, true)
                        .addOwnershipDescriptor("sk3_entity2", EntityType.SECURE_PASSWORD, false)
                        .addOwnershipDescriptor("sk3_entity3", EntityType.SECURE_PASSWORD, true)
                        .addOwnershipDescriptor("sk3_entity4", EntityType.POLICY, false)
                        .addOwnershipDescriptor("sk3_entity5", EntityType.SERVICE, false)
                        .parent(newGoid(2))
                        .build(),
                new SolutionKitBuilder()
                        .goid(newGoid(4))
                        .name("child sk4")
                        .skGuid(newGuid(newGoid(4)))
                        .skVersion("2.0")
                        .addOwnershipDescriptor("sk4_entity1", EntityType.SERVER_MODULE_FILE, false)
                        .parent(newGoid(2))
                        .build(),
                new SolutionKitBuilder()
                        .goid(newGoid(5))
                        .name("child sk5")
                        .skGuid(newGuid(newGoid(5)))
                        .skVersion("2.0")
                        .addOwnershipDescriptor("sk5_entity1", EntityType.SECURE_PASSWORD, false)
                        .addOwnershipDescriptor("sk5_entity2", EntityType.SECURE_PASSWORD, false)
                        .addOwnershipDescriptor("sk5_entity3", EntityType.FOLDER, false)
                        .addOwnershipDescriptor("sk5_entity4", EntityType.FOLDER, false)
                        .addOwnershipDescriptor("sk5_entity5", EntityType.POLICY, false)
                        .addOwnershipDescriptor("sk5_entity6", EntityType.SERVICE, false)
                        .parent(newGoid(2))
                        .build(),
                new SolutionKitBuilder()
                        .goid(newGoid(6))
                        .name("child sk6")
                        .skGuid(newGuid(newGoid(6)))
                        .skVersion("1.0")
                        .addOwnershipDescriptor("sk6_entity1", EntityType.SERVICE, true)
                        .build(),
        };
    }

    /**
     * Simple holder of restman mappings.
     */
    private static class RestmanMapping {
        public final Mapping.Action action;
        public final Mapping.ActionTaken actionTaken;
        public final Mapping.ErrorType errorType;
        public final String srcId;
        public final String targetId;
        public final EntityType entityType;
        public final boolean isReadOnly;

        RestmanMapping(
                final Mapping.Action action,
                final Mapping.ActionTaken actionTaken,
                final String id,
                final EntityType entityType,
                final boolean isReadOnly
        ) {
            this(action, actionTaken, id, id, entityType, isReadOnly);
        }

        RestmanMapping(
                final Mapping.Action action,
                final Mapping.ActionTaken actionTaken,
                final String srcId,
                final String targetId,
                final EntityType entityType,
                final boolean isReadOnly
        ) {
            Assert.assertNotNull(action);
            Assert.assertNotNull(actionTaken);
            Assert.assertNotNull(srcId);
            Assert.assertNotNull(targetId);
            Assert.assertNotNull(entityType);

            this.action = action;
            this.actionTaken = actionTaken;
            this.errorType = null;
            this.srcId = srcId;
            this.targetId = targetId;
            this.entityType = entityType;
            this.isReadOnly = isReadOnly;
        }

        RestmanMapping(
                final Mapping.Action action,
                final Mapping.ErrorType errorType,
                final String id,
                final EntityType entityType,
                final boolean isReadOnly
        ) {
            this(action, errorType, id, id, entityType, isReadOnly);
        }

        RestmanMapping(
                final Mapping.Action action,
                final Mapping.ErrorType errorType,
                final String srcId,
                final String targetId,
                final EntityType entityType,
                final boolean isReadOnly
        ) {
            Assert.assertNotNull(action);
            Assert.assertNotNull(errorType);
            Assert.assertNotNull(srcId);
            Assert.assertNotNull(targetId);
            Assert.assertNotNull(entityType);

            this.action = action;
            this.actionTaken = null;
            this.errorType = errorType;
            this.srcId = srcId;
            this.targetId = targetId;
            this.entityType = entityType;
            this.isReadOnly = isReadOnly;
        }

        public boolean isError() {
            return errorType != null;
        }
    }

    private static String buildMappings(final RestmanMapping ... restmanMappings) {
        return buildMappings(Arrays.asList(restmanMappings));
    }

    private static String buildMappings(final Collection<RestmanMapping> restmanMappings) {
        Assert.assertNotNull(restmanMappings);
        String mappings ="<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">" + System.lineSeparator() +
                "    <l7:Name>Bundle mappings</l7:Name>" + System.lineSeparator() +
                "    <l7:Type>BUNDLE MAPPINGS</l7:Type>" + System.lineSeparator() +
                "    <l7:TimeStamp>2015-10-13T16:46:35.422-07:00</l7:TimeStamp>" + System.lineSeparator() +
                "    <l7:Link rel=\"self\" uri=\"/1.0/bundle?versionComment=Simple+Server+Module+File+%28v1.1%29\"/>" + System.lineSeparator() +
                "    <l7:Resource>" + System.lineSeparator() +
                "        <l7:Mappings>" + System.lineSeparator();

        for (RestmanMapping mapping : restmanMappings) {
            mappings += buildMapping(mapping);
        }

        mappings +=
                "        </l7:Mappings>" + System.lineSeparator() +
                        "    </l7:Resource>" + System.lineSeparator() +
                        "</l7:Item>";

        return mappings;
    }

    private static String buildMapping(final RestmanMapping restmanMapping) {
        Assert.assertNotNull(restmanMapping);

        String mapping = MessageFormat.format("            <l7:Mapping action=\"{0}\" {1}=\"{2}\" srcId=\"{3}\" srcUri=\"some uri\" {4} type=\"{5}\">" + System.lineSeparator(),
                restmanMapping.action.name(),
                restmanMapping.isError() ? "errorType" : "actionTaken",
                restmanMapping.isError() ? restmanMapping.errorType.name() : restmanMapping.actionTaken.name(),
                restmanMapping.srcId,
                restmanMapping.isError() ? "" : "targetId=\"" + restmanMapping.targetId + "\" targetUri=\"some uri\"",
                restmanMapping.entityType.name()
        );
        if (restmanMapping.isReadOnly) {
            mapping += "                <l7:Properties>" + System.lineSeparator() +
                    "                    <l7:Property key=\"SK_ReadOnlyEntity\"><l7:BooleanValue>true</l7:BooleanValue></l7:Property>" + System.lineSeparator() +
                    "                </l7:Properties>" + System.lineSeparator();
        }
        mapping += "            </l7:Mapping>" + System.lineSeparator();

        return mapping;
    }

    private static Collection<EntityOwnershipDescriptor> deepCopyDescriptors(final Collection<EntityOwnershipDescriptor> original) {
        Assert.assertNotNull(original);
        final Collection<EntityOwnershipDescriptor> copy = new ArrayList<>();
        for (final EntityOwnershipDescriptor descriptor : original) {
            final EntityOwnershipDescriptor newDescriptor = new EntityOwnershipDescriptor(descriptor.getSolutionKit(), "id1", EntityType.FOLDER, false); // some test values
            newDescriptor.copyFrom(descriptor, descriptor.getSolutionKit());
            copy.add(newDescriptor);
        }
        return Collections.unmodifiableCollection(copy);
    }

    @Test
    public void testUpdateEntityOwnershipDescriptors() throws Exception {
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test install
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // AlwaysCreateNew  -> CreatedNew       - SERVER_MODULE_FILE       - 37953ab3e89a8b7df721c055d4aeb6a1 - read-only
        // NewOrExisting    -> UsedExisting     - SECURE_PASSWORD          - 37953ab3e89a8b7df721c055d4aeb6a2 - ignored
        // NewOrExisting    -> CreatedNew       - POLICY                   - 37953ab3e89a8b7df721c055d4aeb6a3 - read-only
        // Ignore           -> CreatedNew       - SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a4 - read-only
        // NewOrExisting    -> Ignored          - SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a5 - ignore
        // NewOrExisting    -> CreatedNew       - ENCAPSULATED_ASSERTION   - 37953ab3e89a8b7df721c055d4aeb6a6 - not read-only
        // NewOrExisting    -> UpdatedExisting  - CUSTOM_KEY_VALUE_STORE   - 37953ab3e89a8b7df721c055d4aeb6a7 - ignore
        // NewOrExisting    -> Deleted          - EMAIL_LISTENER           - 37953ab3e89a8b7df721c055d4aeb6a8 - ignore
        // NewOrExisting    -> CreatedNew       - FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6a9 - not read-only
        String restmanMappings = buildMappings(
                new RestmanMapping(Mapping.Action.AlwaysCreateNew, Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a1", EntityType.SERVER_MODULE_FILE, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UsedExisting,    "37953ab3e89a8b7df721c055d4aeb6a2", EntityType.SECURE_PASSWORD, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a3", EntityType.POLICY, true),
                new RestmanMapping(Mapping.Action.Ignore,          Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a4", EntityType.SERVICE, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Ignored,         "37953ab3e89a8b7df721c055d4aeb6a5", EntityType.SERVICE, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a6", EntityType.ENCAPSULATED_ASSERTION, false),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UpdatedExisting, "37953ab3e89a8b7df721c055d4aeb6a7", EntityType.CUSTOM_KEY_VALUE_STORE, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Deleted,         "37953ab3e89a8b7df721c055d4aeb6a8", EntityType.EMAIL_LISTENER, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a9", EntityType.FOLDER, false)
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        // create a sample Kit without any ownership descriptors
        SolutionKit solutionKit = new SolutionKitBuilder()
                .name("new sk1")
                .skGuid(UUID.randomUUID().toString())
                .skVersion("1.0")
                .build();

        // make sure both mappings and uninstall bundle are empty
        Assert.assertNull(solutionKit.getMappings());
        Assert.assertNull(solutionKit.getUninstallBundle());
        // make sure initial solutionKit doesn't have any entity ownership descriptors
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.anyOf(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class), Matchers.nullValue()));

        Goid goid = solutionKitAdminHelper.install(solutionKit, "", false);
        Assert.assertNotNull(goid);
        Assert.assertThat(goid, Matchers.equalTo(solutionKit.getGoid()));
        Assert.assertNotNull(solutionKitManager.findByPrimaryKey(goid));
        Assert.assertThat(solutionKitManager.findByPrimaryKey(goid), Matchers.sameInstance(solutionKit));
        // make sure the mappings are set
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

        // test entity ownership descriptors
        // SERVER_MODULE_FILE       - 37953ab3e89a8b7df721c055d4aeb6a1 - read-only
        // POLICY                   - 37953ab3e89a8b7df721c055d4aeb6a3 - read-only
        // SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a4 - read-only
        // ENCAPSULATED_ASSERTION   - 37953ab3e89a8b7df721c055d4aeb6a6 - not read-only
        // FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6a9 - not read-only
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.hasSize(5), Matchers.notNullValue()));
        for (final EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
            Assert.assertThat(descriptor.getSolutionKit(), Matchers.sameInstance(solutionKit));
            Assert.assertThat(
                    descriptor.getEntityId(),
                    Matchers.anyOf(
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a1"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a3"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a4"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a6"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a9")
                    )
            );
            switch (descriptor.getEntityId()) {
                case "37953ab3e89a8b7df721c055d4aeb6a1":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVER_MODULE_FILE));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a3":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.POLICY));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a4":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVICE));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a6":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.ENCAPSULATED_ASSERTION));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a9":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.FOLDER));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                default:
                    Assert.fail("Unexpected entity id: " + descriptor.getEntityId());
                    break;
            }
        }
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test upgrade
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // NewOrExisting    -> UpdatedExisting  - SERVER_MODULE_FILE       - 37953ab3e89a8b7df721c055d4aeb6a1 - read-only       => read-only
        // NewOrExisting    -> UsedExisting     - SECURE_PASSWORD          - 37953ab3e89a8b7df721c055d4aeb6a2 - ignored         => ignored (sk is not the owner)
        // NewOrExisting    -> UsedExisting     - POLICY                   - 37953ab3e89a8b7df721c055d4aeb6a3 - read-only       => not read-only
        // Ignore           -> UpdatedExisting  - SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a4 - read-only       => not read-only
        // NewOrExisting    -> Ignored          - SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a5 - ignore          => ignored (sk is not the owner)
        // NewOrExisting    -> CreatedNew       - ENCAPSULATED_ASSERTION   - 37953ab3e89a8b7df721c055d4aeb6a6 - not read-only   => recreate as SCHEDULED_TASK and read-only
        // NewOrExisting    -> CreatedNew       - CUSTOM_KEY_VALUE_STORE   - 37953ab3e89a8b7df721c055d4aeb6a7 - ignore          => read-only
        // NewOrExisting    -> Deleted          - EMAIL_LISTENER           - 37953ab3e89a8b7df721c055d4aeb6a8 - ignore          => ignored (doesn't exist i.e. sk is not the owner)
        // NewOrExisting    -> UsedExisting     - FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6a9 - not read-only   => read-only
        // NewOrExisting    -> UpdatedExisting  - FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6aa - (doesn't exist) => ignored (sk is not the owner)
        // NewOrExisting    -> UsedExisting     - GENERIC                  - 37953ab3e89a8b7df721c055d4aeb6ab - (doesn't exist) => ignored (sk is not the owner)
        restmanMappings = buildMappings(
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UpdatedExisting, "37953ab3e89a8b7df721c055d4aeb6a1", EntityType.SERVER_MODULE_FILE, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UsedExisting,    "37953ab3e89a8b7df721c055d4aeb6a2", EntityType.SECURE_PASSWORD, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UsedExisting,    "37953ab3e89a8b7df721c055d4aeb6a3", EntityType.POLICY, false),
                new RestmanMapping(Mapping.Action.Ignore,          Mapping.ActionTaken.UpdatedExisting, "37953ab3e89a8b7df721c055d4aeb6a4", EntityType.SERVICE, false),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Ignored,         "37953ab3e89a8b7df721c055d4aeb6a5", EntityType.SERVICE, false), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a6", EntityType.SCHEDULED_TASK, true), // recreate with different type and read-only status
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a7", EntityType.CUSTOM_KEY_VALUE_STORE, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Deleted,         "37953ab3e89a8b7df721c055d4aeb6a8", EntityType.EMAIL_LISTENER, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UsedExisting,    "37953ab3e89a8b7df721c055d4aeb6a9", EntityType.FOLDER, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UpdatedExisting, "37953ab3e89a8b7df721c055d4aeb6aa", EntityType.FOLDER, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UsedExisting,    "37953ab3e89a8b7df721c055d4aeb6ab", EntityType.GENERIC, true) // should be ignored
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        solutionKit = solutionKitManager.findByPrimaryKey(goid);
        Assert.assertNotNull(solutionKit);

        goid = solutionKitAdminHelper.install(solutionKit, "", true);
        Assert.assertThat(goid, Matchers.equalTo(solutionKit.getGoid()));

        // make sure the mappings are properly set
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

        // test entity ownership descriptors
        // SERVER_MODULE_FILE       - 37953ab3e89a8b7df721c055d4aeb6a1 - read-only
        // POLICY                   - 37953ab3e89a8b7df721c055d4aeb6a3 - not read-only
        // SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a4 - not read-only
        // SCHEDULED_TASK           - 37953ab3e89a8b7df721c055d4aeb6a6 - read-only (recreated)
        // CUSTOM_KEY_VALUE_STORE   - 37953ab3e89a8b7df721c055d4aeb6a7 - read-only
        // FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6a9 - read-only
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.hasSize(6), Matchers.notNullValue()));
        for (final EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
            Assert.assertThat(descriptor.getSolutionKit(), Matchers.sameInstance(solutionKit));
            Assert.assertThat(
                    descriptor.getEntityId(),
                    Matchers.anyOf(
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a1"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a3"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a4"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a6"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a7"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a9")
                    )
            );
            switch (descriptor.getEntityId()) {
                case "37953ab3e89a8b7df721c055d4aeb6a1":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVER_MODULE_FILE));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a3":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.POLICY));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a4":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVICE));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a6":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SCHEDULED_TASK));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a7":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.CUSTOM_KEY_VALUE_STORE));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a9":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.FOLDER));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                default:
                    Assert.fail("Unexpected entity id: " + descriptor.getEntityId());
                    break;
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test delete
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // NewOrExisting    -> UpdatedExisting  - SERVER_MODULE_FILE       - 37953ab3e89a8b7df721c055d4aeb6a1 - read-only        =>  read-only  (no change)
        // NewOrExisting    -> CreatedNew       - SECURE_PASSWORD          - 37953ab3e89a8b7df721c055d4aeb6a2 - ignored          =>  read-only  (created)
        // NewOrExisting    -> Deleted          - POLICY                   - 37953ab3e89a8b7df721c055d4aeb6a3 - not read-only    =>  deleted
        // Ignore           -> UpdatedExisting  - SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a4 - not read-only    =>  not read-only  (no change)
        // NewOrExisting    -> CreatedNew       - SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a5 - ignored          =>  read-only  (created)
        // NewOrExisting    -> Deleted          - SCHEDULED_TASK           - 37953ab3e89a8b7df721c055d4aeb6a6 - read-only        =>  deleted
        // NewOrExisting    -> Deleted          - CUSTOM_KEY_VALUE_STORE   - 37953ab3e89a8b7df721c055d4aeb6a7 - read-only        =>  deleted
        // NewOrExisting    -> CreatedNew       - EMAIL_LISTENER           - 37953ab3e89a8b7df721c055d4aeb6a8 - ignored          =>  not read-only  (created)
        // NewOrExisting    -> Deleted          - FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6a9 - read-only        =>  deleted
        // NewOrExisting    -> CreatedNew       - FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6aa - ignored          =>  read-only (created)
        // NewOrExisting    -> CreatedNew       - GENERIC                  - 37953ab3e89a8b7df721c055d4aeb6ab - ignored          =>  not read-only (created)
        restmanMappings = buildMappings(
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.UpdatedExisting, "37953ab3e89a8b7df721c055d4aeb6a1", EntityType.SERVER_MODULE_FILE, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a2", EntityType.SECURE_PASSWORD, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Deleted,         "37953ab3e89a8b7df721c055d4aeb6a3", EntityType.POLICY, false),
                new RestmanMapping(Mapping.Action.Ignore,          Mapping.ActionTaken.UpdatedExisting, "37953ab3e89a8b7df721c055d4aeb6a4", EntityType.SERVICE, false),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a5", EntityType.SERVICE, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Deleted,         "37953ab3e89a8b7df721c055d4aeb6a6", EntityType.SCHEDULED_TASK, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Deleted,         "37953ab3e89a8b7df721c055d4aeb6a7", EntityType.CUSTOM_KEY_VALUE_STORE, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6a8", EntityType.EMAIL_LISTENER, false),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.Deleted,         "37953ab3e89a8b7df721c055d4aeb6a9", EntityType.FOLDER, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6aa", EntityType.FOLDER, true),
                new RestmanMapping(Mapping.Action.NewOrExisting,   Mapping.ActionTaken.CreatedNew,      "37953ab3e89a8b7df721c055d4aeb6ab", EntityType.GENERIC, false)
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        solutionKit = solutionKitManager.findByPrimaryKey(goid);
        Assert.assertNotNull(solutionKit);

        goid = solutionKitAdminHelper.install(solutionKit, "", true);
        Assert.assertThat(goid, Matchers.equalTo(solutionKit.getGoid()));

        // make sure the mappings are properly set
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

        // test entity ownership descriptors
        // SERVER_MODULE_FILE       - 37953ab3e89a8b7df721c055d4aeb6a1 - read-only
        // SECURE_PASSWORD          - 37953ab3e89a8b7df721c055d4aeb6a2 - read-only
        // SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a4 - not read-only
        // SERVICE                  - 37953ab3e89a8b7df721c055d4aeb6a5 - read-only
        // EMAIL_LISTENER           - 37953ab3e89a8b7df721c055d4aeb6a8 - not read-only
        // FOLDER                   - 37953ab3e89a8b7df721c055d4aeb6aa - read-only
        // GENERIC                  - 37953ab3e89a8b7df721c055d4aeb6ab - not read-only
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.hasSize(7), Matchers.notNullValue()));
        for (final EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
            Assert.assertThat(descriptor.getSolutionKit(), Matchers.sameInstance(solutionKit));
            //noinspection unchecked
            Assert.assertThat(
                    descriptor.getEntityId(),
                    Matchers.anyOf(
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a1"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a2"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a4"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a5"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a8"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6aa"),
                            Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6ab")
                    )
            );
            switch (descriptor.getEntityId()) {
                case "37953ab3e89a8b7df721c055d4aeb6a1":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVER_MODULE_FILE));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a2":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SECURE_PASSWORD));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a4":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVICE));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a5":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SERVICE));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6a8":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.EMAIL_LISTENER));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6aa":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.FOLDER));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "37953ab3e89a8b7df721c055d4aeb6ab":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.GENERIC));
                    Assert.assertFalse(descriptor.isReadOnly());
                    break;
                default:
                    Assert.fail("Unexpected entity id: " + descriptor.getEntityId());
                    break;
            }
        }

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    /**
     * Convenient method for getting the next {@code EntityType} in the specified enum.
     *
     * @param entityType    starting {@code EntityType}.  Required and cannot be {@code null}.
     */
    private static EntityType next(final EntityType entityType) {
        Assert.assertNotNull(entityType);
        return EntityType.values()[(entityType.ordinal() + 1) % EntityType.values().length];
    }

    @BugId("SSG-12242")
    @Test
    public void testUpdateEntityOwnershipDescriptorsUpdatesAndOrphans() throws Exception {
        // loop through all sample kits and flip the readonly-ness
        for (final SolutionKit solutionKit : solutionKitManager.findAll()) {
            Assert.assertNotNull(solutionKit);

            Collection<EntityOwnershipDescriptor> descriptors = solutionKit.getEntityOwnershipDescriptors();
            if (descriptors != null && !descriptors.isEmpty()) {
                // cache org flags
                final Map<String, Pair<EntityType, Boolean>> orgFlags = Functions.toMap(
                        descriptors,
                        new Functions.Unary<Pair<String, Pair<EntityType, Boolean>>, EntityOwnershipDescriptor>() {
                            @Override
                            public Pair<String, Pair<EntityType, Boolean>> call(final EntityOwnershipDescriptor descriptor) {
                                Assert.assertNotNull(descriptor);
                                return Pair.pair(descriptor.getEntityId(), Pair.pair(descriptor.getEntityType(), descriptor.isReadOnly()));
                            }
                        }
                );
                Assert.assertNotNull(orgFlags);
                Assert.assertThat(orgFlags.values(), Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(descriptors.size())));

                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // build an update mappings with flipped read-only flag
                String restmanMappings = buildMappings(
                        Functions.map(
                                descriptors,
                                new Functions.UnaryThrows<RestmanMapping, EntityOwnershipDescriptor, Exception>() {
                                    @Override
                                    public RestmanMapping call(final EntityOwnershipDescriptor descriptor) throws Exception {
                                        Assert.assertNotNull(descriptor);
                                        // create mapping with flipped read-only flag
                                        final EntityType nextType = next(descriptor.getEntityType());
                                        Assert.assertThat(nextType, Matchers.not(Matchers.equalTo(descriptor.getEntityType())));
                                        // changing the entity type has no affect during update
                                        return new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, descriptor.getEntityId(), nextType, !descriptor.isReadOnly());
                                    }
                                }
                        )
                );
                Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
                Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

                Goid goid = solutionKitAdminHelper.install(solutionKit, "", true);
                Assert.assertNotNull(goid);
                Assert.assertThat(solutionKitManager.findByPrimaryKey(goid), Matchers.allOf(Matchers.notNullValue(), Matchers.sameInstance(solutionKit)));
                // make sure the mappings are set
                Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

                descriptors = solutionKit.getEntityOwnershipDescriptors();
                Assert.assertThat(descriptors, Matchers.allOf(Matchers.hasSize(orgFlags.size()), Matchers.notNullValue()));
                for (final EntityOwnershipDescriptor descriptor : descriptors) {
                    Assert.assertThat(descriptor.getSolutionKit(), Matchers.allOf(Matchers.notNullValue(), Matchers.sameInstance(solutionKit)));
                    Assert.assertThat(orgFlags, Matchers.hasKey(descriptor.getEntityId()));
                    final Pair<EntityType, Boolean> flag = orgFlags.get(descriptor.getEntityId());
                    Assert.assertThat(descriptor.getEntityType(), Matchers.equalTo(flag.left));
                    Assert.assertThat(descriptor.isReadOnly(), Matchers.equalTo(!flag.right));
                }
                ////////////////////////////////////////////////////////////////////////////////////////////////////////


                ////////////////////////////////////////////////////////////////////////////////////////////////////////
                // SSG-12242: test for orphans by building mappings with CreatedNew, in additions flipping read-only flag and modifying entity type
                restmanMappings = buildMappings(
                        Functions.map(
                                descriptors,
                                new Functions.UnaryThrows<RestmanMapping, EntityOwnershipDescriptor, Exception>() {
                                    @Override
                                    public RestmanMapping call(final EntityOwnershipDescriptor descriptor) throws Exception {
                                        Assert.assertNotNull(descriptor);
                                        // create mapping with flipped read-only flag
                                        final EntityType nextType = next(descriptor.getEntityType());
                                        Assert.assertThat(nextType, Matchers.not(Matchers.equalTo(descriptor.getEntityType())));
                                        // changing the entity type has no affect during update
                                        return new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, descriptor.getEntityId(), nextType, !descriptor.isReadOnly());
                                    }
                                }
                        )
                );
                Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
                Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

                goid = solutionKitAdminHelper.install(solutionKit, "", true);
                Assert.assertNotNull(goid);
                Assert.assertThat(solutionKitManager.findByPrimaryKey(goid), Matchers.allOf(Matchers.notNullValue(), Matchers.sameInstance(solutionKit)));
                // make sure the mappings are set
                Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

                descriptors = solutionKit.getEntityOwnershipDescriptors();
                Assert.assertThat(descriptors, Matchers.allOf(Matchers.hasSize(orgFlags.size()), Matchers.notNullValue()));
                for (final EntityOwnershipDescriptor descriptor : descriptors) {
                    Assert.assertThat(descriptor.getSolutionKit(), Matchers.allOf(Matchers.notNullValue(), Matchers.sameInstance(solutionKit)));
                    Assert.assertThat(orgFlags, Matchers.hasKey(descriptor.getEntityId()));
                    final Pair<EntityType, Boolean> flag = orgFlags.get(descriptor.getEntityId());
                    final EntityType nextType = next(flag.left);
                    Assert.assertThat(nextType, Matchers.not(Matchers.equalTo(flag.left)));
                    Assert.assertThat(descriptor.getEntityType(), Matchers.equalTo(nextType));
                    Assert.assertThat(descriptor.isReadOnly(), Matchers.equalTo(flag.right));
                }
                ////////////////////////////////////////////////////////////////////////////////////////////////////////
            }
        }
    }

    @Test
    public void testUpdateEntityOwnershipDescriptorsWithEntitiesNotOwnedBySolutionKit() throws Exception {
        String restmanMappings = buildMappings(
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting,    "37953ab3e89a8b7df721c055d4aeb6a1", EntityType.SERVER_MODULE_FILE, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrUpdate,   Mapping.ActionTaken.UpdatedExisting, "37953ab3e89a8b7df721c055d4aeb6a2", EntityType.SECURE_PASSWORD, true), // should be ignored
                new RestmanMapping(Mapping.Action.Delete,        Mapping.ActionTaken.Deleted,         "37953ab3e89a8b7df721c055d4aeb6a3", EntityType.POLICY, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.Ignored,         "37953ab3e89a8b7df721c055d4aeb6a4", EntityType.SERVICE, true) // should be ignored
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test install
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // create a sample Kit without any ownership descriptors
        SolutionKit solutionKit = new SolutionKitBuilder()
                .name("new sk1")
                .skGuid(UUID.randomUUID().toString())
                .skVersion("1.0")
                .build();

        // make sure both mappings and uninstall bundle are empty
        Assert.assertNull(solutionKit.getMappings());
        Assert.assertNull(solutionKit.getUninstallBundle());
        // make sure initial solutionKit doesn't have any entity ownership descriptors
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.anyOf(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class), Matchers.nullValue()));

        Goid goid = solutionKitAdminHelper.install(solutionKit, "", false);
        Assert.assertNotNull(goid);
        Assert.assertNotNull(solutionKitManager.findByPrimaryKey(goid));
        // make sure the mappings are set
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

        // test entity ownership descriptors (should be empty as no entities are owned by the kit)
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.anyOf(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class), Matchers.nullValue()));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test upgrade
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        goid = newGoid(1);
        // SOLUTION KIT sk1 => single solution kit
        //          sk1_entity1 => JDBC_CONNECTION      => true
        //          sk1_entity2 => SECURE_PASSWORD      => true
        //          sk1_entity3 => EMAIL_LISTENER       => false
        //          sk1_entity4 => SERVER_MODULE_FILE   => true
        //          sk1_entity5 => SERVER_MODULE_FILE   => false
        //          sk1_entity6 => FOLDER               => true
        //          sk1_entity7 => POLICY               => true
        //          sk1_entity8 => SERVICE              => false
        solutionKit = solutionKitManager.findByPrimaryKey(goid);
        Assert.assertNotNull(solutionKit);

        // make sure our test entities are not already owned by the solution kit (sk1 that is)
        final Collection<EntityOwnershipDescriptor> orgDescriptors = deepCopyDescriptors(solutionKit.getEntityOwnershipDescriptors());
        for (EntityOwnershipDescriptor descriptor : orgDescriptors) {
            Assert.assertThat(
                    descriptor.getEntityId(),
                    Matchers.allOf(
                            Matchers.not(Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a1")),
                            Matchers.not(Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a2")),
                            Matchers.not(Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a3")),
                            Matchers.not(Matchers.equalTo("37953ab3e89a8b7df721c055d4aeb6a4"))
                    )
            );
        }

        goid = solutionKitAdminHelper.install(solutionKit, "", true);
        Assert.assertThat(goid, Matchers.equalTo(solutionKit.getGoid()));

        // make sure descriptors are unchanged
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.hasSize(orgDescriptors.size()));
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.containsInAnyOrder(orgDescriptors.toArray(new EntityOwnershipDescriptor[orgDescriptors.size()])));

        // test with sk3
        //
        // SOLUTION KIT sk3 => child kit
        //     sk3_entity1 => CUSTOM_KEY_VALUE_STORE   => true
        //     sk3_entity2 => SECURE_PASSWORD          => true
        //     sk3_entity3 => SECURE_PASSWORD          => false
        //     sk3_entity4 => POLICY                   => true
        //     sk3_entity5 => SERVICE                  => false
        final SolutionKit kit3 = solutionKitManager.findByPrimaryKey(newGoid(3));
        Assert.assertNotNull(kit3);
        Assert.assertThat(kit3.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.not(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class)), Matchers.notNullValue()));

        // simulate updating all entities owned by sk3
        restmanMappings = buildMappings(
                Functions.map(
                        kit3.getEntityOwnershipDescriptors(),
                        new Functions.UnaryThrows<RestmanMapping, EntityOwnershipDescriptor, Exception>() {
                            @Override
                            public RestmanMapping call(final EntityOwnershipDescriptor descriptor) throws Exception {
                                Assert.assertNotNull(descriptor);
                                return new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, descriptor.getEntityId(), descriptor.getEntityType(), !descriptor.isReadOnly());
                            }
                        }
                )
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        goid = solutionKitAdminHelper.install(solutionKit, "", true);
        Assert.assertThat(goid, Matchers.equalTo(solutionKit.getGoid()));

        // make sure descriptors are unchanged
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.hasSize(orgDescriptors.size()));
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.containsInAnyOrder(orgDescriptors.toArray(new EntityOwnershipDescriptor[orgDescriptors.size()])));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test upgrade
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

        // this is a collection of scars
        goid = newGoid(2);
        // SOLUTION KIT sk2 => collection of scars
        //      SOLUTION KIT sk3 => child kit
        //          sk3_entity1 => CUSTOM_KEY_VALUE_STORE   => true
        //          sk3_entity2 => SECURE_PASSWORD          => true
        //          sk3_entity3 => SECURE_PASSWORD          => false
        //          sk3_entity4 => POLICY                   => true
        //          sk3_entity5 => SERVICE                  => false
        //      SOLUTION KIT sk4 => child kit
        //          sk4_entity1 => SERVER_MODULE_FILE   => false
        //      SOLUTION KIT sk5 => child kit
        //          sk5_entity1 => SECURE_PASSWORD   => false
        //          sk5_entity2 => SECURE_PASSWORD   => false
        //          sk5_entity3 => FOLDER            => false
        //          sk5_entity4 => FOLDER            => false
        //          sk5_entity5 => POLICY            => false
        //          sk5_entity4 => SERVICE           => false
        solutionKit = solutionKitManager.findByPrimaryKey(goid);
        Assert.assertNotNull(solutionKit);

        // simulate updating all entities owned by the children
        restmanMappings = buildMappings(
                Functions.flatmap(
                        Arrays.asList(
                                solutionKitManager.findByPrimaryKey(newGoid(3)),
                                solutionKitManager.findByPrimaryKey(newGoid(4)),
                                solutionKitManager.findByPrimaryKey(newGoid(5))
                        ),
                        new Functions.UnaryThrows<Iterable<RestmanMapping>, SolutionKit, Exception>() {
                            @Override
                            public Iterable<RestmanMapping> call(final SolutionKit solutionKit) throws Exception {
                                Assert.assertNotNull(solutionKit);
                                final Collection<EntityOwnershipDescriptor> descriptors = solutionKit.getEntityOwnershipDescriptors();
                                final Collection<RestmanMapping> mappings = new ArrayList<>(descriptors != null ? descriptors.size() : 0);
                                if (descriptors != null && !descriptors.isEmpty()) {
                                    for (EntityOwnershipDescriptor descriptor : descriptors) {
                                        mappings.add(new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.UsedExisting, descriptor.getEntityId(), descriptor.getEntityType(), !descriptor.isReadOnly()));
                                    }
                                }
                                return mappings;
                            }
                        }
                )
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        goid = solutionKitAdminHelper.install(solutionKit, "", true);
        Assert.assertNotNull(goid);
        Assert.assertThat(goid, Matchers.equalTo(solutionKit.getGoid()));
        Assert.assertThat(solutionKitManager.findByPrimaryKey(goid), Matchers.allOf(Matchers.notNullValue(), Matchers.sameInstance(solutionKit)));
        // make sure the mappings are set
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

        // expected no changes to the descriptors
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.anyOf(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class), Matchers.nullValue()));

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    }

    @Test
    public void testUpdateEntityOwnershipDescriptorsWithErrorMappings() throws Exception {
        final String restmanMappings = buildMappings(
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, "7bf91daabff1558dd35b12b9f1f3ab71", EntityType.POLICY, true),
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ErrorType.TargetExists, "7bf91daabff1558dd35b12b9f1f3ab72", EntityType.SERVICE, true), // should be ignored
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, "7bf91daabff1558dd35b12b9f1f3ab73", EntityType.SECURE_PASSWORD, true)
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        // create a sample Kit without any ownership descriptors
        final SolutionKit solutionKit = new SolutionKitBuilder()
                .name("new sk1")
                .skGuid(UUID.randomUUID().toString())
                .skVersion("1.0")
                .build();

        // make sure both mappings and uninstall bundle are empty
        Assert.assertNull(solutionKit.getMappings());
        Assert.assertNull(solutionKit.getUninstallBundle());
        // make sure initial solutionKit doesn't have any entity ownership descriptors
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.anyOf(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class), Matchers.nullValue()));

        final Goid goid = solutionKitAdminHelper.install(solutionKit, "", false);
        Assert.assertNotNull(goid);
        Assert.assertNotNull(solutionKitManager.findByPrimaryKey(goid));
        // make sure the mappings are set
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(restmanMappings));

        // test entity ownership descriptors
        // POLICY           - 7bf91daabff1558dd35b12b9f1f3ab71 - read-only
        // SECURE_PASSWORD  - 7bf91daabff1558dd35b12b9f1f3ab73 - read-only
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.hasSize(2), Matchers.notNullValue()));
        for (final EntityOwnershipDescriptor descriptor : solutionKit.getEntityOwnershipDescriptors()) {
            Assert.assertThat(descriptor.getSolutionKit(), Matchers.sameInstance(solutionKit));
            Assert.assertThat(
                    descriptor.getEntityId(),
                    Matchers.anyOf(
                            Matchers.equalTo("7bf91daabff1558dd35b12b9f1f3ab71"),
                            Matchers.equalTo("7bf91daabff1558dd35b12b9f1f3ab73")
                    )
            );
            switch (descriptor.getEntityId()) {
                case "7bf91daabff1558dd35b12b9f1f3ab71":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.POLICY));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                case "7bf91daabff1558dd35b12b9f1f3ab73":
                    Assert.assertThat(descriptor.getEntityType(), Matchers.is(EntityType.SECURE_PASSWORD));
                    Assert.assertTrue(descriptor.isReadOnly());
                    break;
                default:
                    Assert.fail("Unexpected entity id: " + descriptor.getEntityId());
                    break;
            }
        }
    }

    @Test
    public void testUpdateEntityOwnershipDescriptorsTakingOverOwnershipOfEntity() throws Exception {
        // SOLUTION KIT sk1 => single solution kit
        //          sk1_entity1 => JDBC_CONNECTION      => true
        //          sk1_entity2 => SECURE_PASSWORD      => true
        //          sk1_entity3 => EMAIL_LISTENER       => false
        //          sk1_entity4 => SERVER_MODULE_FILE   => true
        //          sk1_entity5 => SERVER_MODULE_FILE   => false
        //          sk1_entity6 => FOLDER               => true
        //          sk1_entity7 => POLICY               => true
        //          sk1_entity8 => SERVICE              => false
        //
        // SOLUTION KIT sk6 => child kit
        //          sk5_entity1 => SERVICE   => true
        //
        // sk6 to take over sk1_entity3 and sk1_entity6 from sk1
        final SolutionKit kit1 = solutionKitManager.findByPrimaryKey(newGoid(1));
        Assert.assertNotNull(kit1);
        Assert.assertThat(kit1.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.not(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class)), Matchers.notNullValue()));
        Assert.assertThat(
                kit1.getEntityOwnershipDescriptors(),
                Matchers.hasItems(
                        new EntityOwnershipDescriptor(kit1, "sk1_entity3", EntityType.EMAIL_LISTENER, false),
                        new EntityOwnershipDescriptor(kit1, "sk1_entity6", EntityType.FOLDER, true)
                )
        );
        final Collection<EntityOwnershipDescriptor> orgDescriptorsKit1 = deepCopyDescriptors(kit1.getEntityOwnershipDescriptors());
        Assert.assertThat(
                orgDescriptorsKit1,
                Matchers.allOf(
                        Matchers.notNullValue(),
                        Matchers.hasSize(kit1.getEntityOwnershipDescriptors().size()),
                        Matchers.containsInAnyOrder(kit1.getEntityOwnershipDescriptors().toArray())
                )
        );

        final SolutionKit kit6 = solutionKitManager.findByPrimaryKey(newGoid(6));
        Assert.assertNotNull(kit6);
        Assert.assertThat(kit6.getEntityOwnershipDescriptors(), Matchers.allOf(Matchers.not(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class)), Matchers.notNullValue()));
        Assert.assertThat(
                kit6.getEntityOwnershipDescriptors(),
                Matchers.not(
                        Matchers.hasItems(
                                new EntityOwnershipDescriptor(kit6, "sk1_entity3", EntityType.EMAIL_LISTENER, false),
                                new EntityOwnershipDescriptor(kit6, "sk1_entity6", EntityType.FOLDER, true)
                        )
                )
        );
        final int sk6OrgSize = kit6.getEntityOwnershipDescriptors().size();

        String restmanMappings = buildMappings(
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.Deleted,    "sk1_entity3", EntityType.EMAIL_LISTENER, false),
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, "sk1_entity3", EntityType.EMAIL_LISTENER, true), // flip the flag
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.Deleted,    "sk1_entity6", EntityType.FOLDER, false),
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.CreatedNew, "sk1_entity6", EntityType.FOLDER, false) // flip the flag
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        Goid goid = solutionKitAdminHelper.install(kit6, "", true);
        Assert.assertNotNull(goid);
        Assert.assertThat(goid, Matchers.equalTo(kit6.getGoid()));
        Assert.assertThat(solutionKitManager.findByPrimaryKey(goid), Matchers.allOf(Matchers.sameInstance(kit6), Matchers.notNullValue()));

        // make sure sk1 entities have not been changed
        Assert.assertThat(
                orgDescriptorsKit1,
                Matchers.allOf(
                        Matchers.notNullValue(),
                        Matchers.hasSize(kit1.getEntityOwnershipDescriptors().size()),
                        Matchers.containsInAnyOrder(kit1.getEntityOwnershipDescriptors().toArray())
                )
        );

        // make sure sk1_entity3 and sk1_entity6 are in sk6
        Assert.assertThat(
                kit6.getEntityOwnershipDescriptors(),
                Matchers.hasItems(
                        new EntityOwnershipDescriptor(kit6, "sk1_entity3", EntityType.EMAIL_LISTENER, true),
                        new EntityOwnershipDescriptor(kit6, "sk1_entity6", EntityType.FOLDER, false)
                )
        );
        Assert.assertThat(kit6.getEntityOwnershipDescriptors(), Matchers.hasSize(sk6OrgSize + 2));


        // test delete of taken over entity
        restmanMappings = buildMappings(
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.Deleted, "sk1_entity3", EntityType.EMAIL_LISTENER, false),
                new RestmanMapping(Mapping.Action.NewOrExisting, Mapping.ActionTaken.Deleted, "sk1_entity6", EntityType.FOLDER, false)
        );
        Mockito.doReturn(restmanMappings).when(solutionKitManager).importBundle(Mockito.anyString(), Mockito.any(SolutionKit.class), Mockito.anyBoolean());
        Mockito.doReturn(restmanMappings).when(solutionKitAdminHelper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        goid = solutionKitAdminHelper.install(kit6, "", true);
        Assert.assertNotNull(goid);
        Assert.assertThat(goid, Matchers.equalTo(kit6.getGoid()));
        Assert.assertThat(solutionKitManager.findByPrimaryKey(goid), Matchers.allOf(Matchers.sameInstance(kit6), Matchers.notNullValue()));

        // make sure sk1 entities have not been changed
        Assert.assertThat(
                orgDescriptorsKit1,
                Matchers.allOf(
                        Matchers.notNullValue(),
                        Matchers.hasSize(kit1.getEntityOwnershipDescriptors().size()),
                        Matchers.containsInAnyOrder(kit1.getEntityOwnershipDescriptors().toArray())
                )
        );

        // make sure not sk1_entity3 and sk1_entity6 are in sk6
        Assert.assertThat(
                kit6.getEntityOwnershipDescriptors(),
                Matchers.not(
                        Matchers.hasItems(
                                new EntityOwnershipDescriptor(kit6, "sk1_entity3", EntityType.EMAIL_LISTENER, true),
                                new EntityOwnershipDescriptor(kit6, "sk1_entity6", EntityType.FOLDER, false)
                        )
                )
        );
        Assert.assertThat(kit6.getEntityOwnershipDescriptors(), Matchers.hasSize(sk6OrgSize));

    }

    @Test
    public void testGetMappingsWithEntityNameAddedToProperties () throws Exception {
        final String installBundle = "<l7:Bundle xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "\t\t\t<l7:References>\n" +
                "\t\t\t\t<l7:Item>\n" +
                "\t\t\t\t\t<l7:Name>cassandra_2_36_1_5</l7:Name>\n" +
                "\t\t\t\t\t<l7:Id>a23e999b6ac5fbb0c30217c04644878d</l7:Id>\n" +
                "\t\t\t\t\t<l7:Type>CASSANDRA_CONFIGURATION</l7:Type>\n" +
                "\t\t\t\t\t<l7:TimeStamp>2015-08-10T12:56:48.825-07:00</l7:TimeStamp>\n" +
                "\t\t\t\t\t<l7:Resource>\n" +
                "\t\t\t\t\t\t<l7:CassandraConnection id=\"a23e999b6ac5fbb0c30217c04644878d\" version=\"0\">\n" +
                "\t\t\t\t\t\t\t<l7:Name>cassandra_2_36_1_5</l7:Name>\n" +
                "\t\t\t\t\t\t\t<l7:Keyspace>qa</l7:Keyspace>\n" +
                "\t\t\t\t\t\t\t<l7:ContactPoint>10.7.33.102</l7:ContactPoint>\n" +
                "\t\t\t\t\t\t\t<l7:Port>9042</l7:Port>\n" +
                "\t\t\t\t\t\t\t<l7:Username>cassandra_Updated</l7:Username>\n" +
                "\t\t\t\t\t\t\t<l7:Compression>NONE</l7:Compression>\n" +
                "\t\t\t\t\t\t\t<l7:Ssl>false</l7:Ssl>\n" +
                "\t\t\t\t\t\t\t<l7:Enabled>true</l7:Enabled>\n" +
                "\t\t\t\t\t\t\t<l7:Properties>\n" +
                "\t\t\t\t\t\t\t\t<l7:Property key=\"ConnectionProperty\">\n" +
                "\t\t\t\t\t\t\t\t\t<l7:StringValue>Property Value</l7:StringValue>\n" +
                "\t\t\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t\t\t</l7:Properties>\n" +
                "\t\t\t\t\t\t</l7:CassandraConnection>\n" +
                "\t\t\t\t\t</l7:Resource>\n" +
                "\t\t\t\t</l7:Item>\n" +
                "\t\t\t\t<l7:Item>\n" +
                "\t\t\t\t\t<l7:Name>service_2_36_1_5</l7:Name>\n" +
                "\t\t\t\t\t<l7:Id>a23e999b6ac5fbb0c30217c046448799</l7:Id>\n" +
                "\t\t\t\t\t<l7:Type>SERVICE</l7:Type>\n" +
                "\t\t\t\t\t<l7:TimeStamp>2015-08-10T12:56:48.844-07:00</l7:TimeStamp>\n" +
                "\t\t\t\t\t<l7:Resource>\n" +
                "\t\t\t\t\t\t<l7:Service id=\"a23e999b6ac5fbb0c30217c046448799\" version=\"2\">\n" +
                "\t\t\t\t\t\t\t<l7:ServiceDetail folderId=\"0000000000000000ffffffffffffec76\" id=\"a23e999b6ac5fbb0c30217c046448799\" version=\"2\">\n" +
                "\t\t\t\t\t\t\t\t<l7:Name>service_2_36_1_5</l7:Name>\n" +
                "\t\t\t\t\t\t\t\t<l7:Enabled>true</l7:Enabled>\n" +
                "\t\t\t\t\t\t\t\t<l7:ServiceMappings>\n" +
                "\t\t\t\t\t\t\t\t\t<l7:HttpMapping>\n" +
                "\t\t\t\t\t\t\t\t\t\t<l7:UrlPattern>/service_2_36_1_5</l7:UrlPattern>\n" +
                "\t\t\t\t\t\t\t\t\t\t<l7:Verbs>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t<l7:Verb>GET</l7:Verb>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t<l7:Verb>POST</l7:Verb>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t<l7:Verb>PUT</l7:Verb>\n" +
                "\t\t\t\t\t\t\t\t\t\t\t<l7:Verb>DELETE</l7:Verb>\n" +
                "\t\t\t\t\t\t\t\t\t\t</l7:Verbs>\n" +
                "\t\t\t\t\t\t\t\t\t</l7:HttpMapping>\n" +
                "\t\t\t\t\t\t\t\t</l7:ServiceMappings>\n" +
                "\t\t\t\t\t\t\t\t<l7:Properties>\n" +
                "\t\t\t\t\t\t\t\t\t<l7:Property key=\"internal\">\n" +
                "\t\t\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
                "\t\t\t\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t\t\t\t\t<l7:Property key=\"policyRevision\">\n" +
                "\t\t\t\t\t\t\t\t\t\t<l7:LongValue>2</l7:LongValue>\n" +
                "\t\t\t\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t\t\t\t\t<l7:Property key=\"soap\">\n" +
                "\t\t\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
                "\t\t\t\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t\t\t\t\t<l7:Property key=\"tracingEnabled\">\n" +
                "\t\t\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
                "\t\t\t\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t\t\t\t\t<l7:Property key=\"wssProcessingEnabled\">\n" +
                "\t\t\t\t\t\t\t\t\t\t<l7:BooleanValue>false</l7:BooleanValue>\n" +
                "\t\t\t\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t\t\t\t</l7:Properties>\n" +
                "\t\t\t\t\t\t\t</l7:ServiceDetail>\n" +
                "\t\t\t\t\t\t\t<l7:Resources>\n" +
                "\t\t\t\t\t\t\t\t<l7:ResourceSet tag=\"policy\">\n" +
                "\t\t\t\t\t\t\t\t\t<l7:Resource type=\"policy\" version=\"1\">&lt;?xml version=&quot;1.0&quot; encoding=&quot;UTF-8&quot;?&gt;\n" +
                "&lt;wsp:Policy xmlns:L7p=&quot;http://www.layer7tech.com/ws/policy&quot; xmlns:wsp=&quot;http://schemas.xmlsoap.org/ws/2002/12/policy&quot;&gt;\n" +
                "    &lt;wsp:All wsp:Usage=&quot;Required&quot;&gt;\n" +
                "        &lt;L7p:CassandraQuery&gt;\n" +
                "            &lt;L7p:ConnectionName stringValue=&quot;cassandra_11_5_1_61&quot;/&gt;\n" +
                "            &lt;L7p:FetchSize intValue=&quot;5000&quot;/&gt;\n" +
                "            &lt;L7p:MaxRecords intValue=&quot;10&quot;/&gt;\n" +
                "            &lt;L7p:QueryDocument stringValue=&quot;test&quot;/&gt;\n" +
                "            &lt;L7p:QueryTimeout stringValueNull=&quot;null&quot;/&gt;\n" +
                "        &lt;/L7p:CassandraQuery&gt;\n" +
                "    &lt;/wsp:All&gt;\n" +
                "&lt;/wsp:Policy&gt;\n" +
                "\t\t\t\t\t\t\t\t\t</l7:Resource>\n" +
                "\t\t\t\t\t\t\t\t</l7:ResourceSet>\n" +
                "\t\t\t\t\t\t\t</l7:Resources>\n" +
                "\t\t\t\t\t\t</l7:Service>\n" +
                "\t\t\t\t\t</l7:Resource>\n" +
                "\t\t\t\t</l7:Item>\n" +
                "\t\t\t</l7:References>\n" +
                "\t\t\t<l7:Mappings>\n" +
                "\t\t\t\t<l7:Mapping action=\"NewOrExisting\" srcId=\"a23e999b6ac5fbb0c30217c04644878d\" srcUri=\"http://david90.ca.com:8080/restman/1.0/cassandraConnections/a23e999b6ac5fbb0c30217c04644878d\" type=\"CASSANDRA_CONFIGURATION\"/>\n" +
                "\t\t\t\t<l7:Mapping action=\"NewOrExisting\" srcId=\"0000000000000000ffffffffffffec76\" srcUri=\"http://david90.ca.com:8080/restman/1.0/folders/0000000000000000ffffffffffffec76\" type=\"FOLDER\">\n" +
                "\t\t\t\t\t<l7:Properties>\n" +
                "\t\t\t\t\t\t<l7:Property key=\"FailOnNew\">\n" +
                "\t\t\t\t\t\t\t<l7:BooleanValue>true</l7:BooleanValue>\n" +
                "\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t</l7:Properties>\n" +
                "\t\t\t\t</l7:Mapping>\n" +
                "\t\t\t\t<l7:Mapping action=\"AlwaysCreateNew\" srcId=\"a23e999b6ac5fbb0c30217c046448799\" srcUri=\"http://david90.ca.com:8080/restman/1.0/services/a23e999b6ac5fbb0c30217c046448799\" type=\"SERVICE\"/>\n" +
                "\t\t\t</l7:Mappings>\n" +
                "\t\t</l7:Bundle>";

        final String installMappings = "<l7:Item xmlns:l7=\"http://ns.l7tech.com/2010/04/gateway-management\">\n" +
                "    <l7:Name>Bundle mappings</l7:Name>\n" +
                "    <l7:Type>BUNDLE MAPPINGS</l7:Type>\n" +
                "    <l7:TimeStamp>2015-11-03T09:37:20.586-08:00</l7:TimeStamp>\n" +
                "    <l7:Link rel=\"self\" uri=\"/1.0/bundle?versionComment=Test4+Solution+Kit+%28v1.0.01%29&amp;test=true\"/>\n" +
                "    <l7:Resource>\n" +
                "        <l7:Mappings>\n" +
                "\t\t\t\t<l7:Mapping action=\"NewOrExisting\" srcId=\"a23e999b6ac5fbb0c30217c04644878d\" srcUri=\"http://david90.ca.com:8080/restman/1.0/cassandraConnections/a23e999b6ac5fbb0c30217c04644878d\" type=\"CASSANDRA_CONFIGURATION\"/>\n" +
                "\t\t\t\t<l7:Mapping action=\"NewOrExisting\" srcId=\"0000000000000000ffffffffffffec76\" srcUri=\"http://david90.ca.com:8080/restman/1.0/folders/0000000000000000ffffffffffffec76\" type=\"FOLDER\">\n" +
                "\t\t\t\t\t<l7:Properties>\n" +
                "\t\t\t\t\t\t<l7:Property key=\"FailOnNew\">\n" +
                "\t\t\t\t\t\t\t<l7:BooleanValue>true</l7:BooleanValue>\n" +
                "\t\t\t\t\t\t</l7:Property>\n" +
                "\t\t\t\t\t</l7:Properties>\n" +
                "\t\t\t\t</l7:Mapping>\n" +
                "\t\t\t\t<l7:Mapping action=\"AlwaysCreateNew\" srcId=\"a23e999b6ac5fbb0c30217c046448799\" srcUri=\"http://david90.ca.com:8080/restman/1.0/services/a23e999b6ac5fbb0c30217c046448799\" type=\"SERVICE\"/>\n" +
                "\t\t\t</l7:Mappings>\n" +
                "    </l7:Resource>\n" +
                "</l7:Item>";

        //run the method we're testing
        String resultMappings = solutionKitAdminHelper.getMappingsWithEntityNameAddedToProperties(installBundle, installMappings);
        Assert.assertNotNull(resultMappings);

        //iterate through the returned result mappings and check to see if for a given srcId, we have a new property
        //with a 'name' that matches the one in our static install bundle above
        final RestmanMessage resultMappingsMessage = new RestmanMessage(resultMappings);
        for (Element mappingElement: resultMappingsMessage.getMappings()) {
            Assert.assertNotNull(mappingElement);
            String srcId = mappingElement.getAttribute(RestmanMessage.MAPPING_SRC_ID_ATTRIBUTE);
            Assert.assertNotNull(srcId);
            //Get the Properties element
            Element propertiesElement = DomUtils.findFirstChildElementByName(mappingElement, MGMT_VERSION_NAMESPACE, RestmanMessage.NODE_NAME_PROPERTIES);
            Assert.assertNotNull(propertiesElement);
            //From Properties, get the Property element
            Element propertyElement = DomUtils.findFirstChildElementByName(propertiesElement, MGMT_VERSION_NAMESPACE, RestmanMessage.NODE_NAME_PROPERTY);
            Assert.assertNotNull(propertyElement);
            if (srcId.equals("a23e999b6ac5fbb0c30217c04644878d")) {
                Assert.assertThat("cassandra_2_36_1_5", Matchers.equalToIgnoringWhiteSpace(propertyElement.getTextContent()));
            }
            if (srcId.equals("a23e999b6ac5fbb0c30217c046448799")) {
                Assert.assertThat("service_2_36_1_5", Matchers.equalToIgnoringWhiteSpace(propertyElement.getTextContent()));
            }
        }
    }



    // TODO: add more SolutionKitAdminHelper tests here
}