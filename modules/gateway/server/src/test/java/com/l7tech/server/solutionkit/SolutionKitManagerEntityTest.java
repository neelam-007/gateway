package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.LicenseManager;
import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerTest;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.security.rbac.ProtectedEntityTracker;
import com.l7tech.test.BugId;
import com.l7tech.util.*;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;

import java.io.InputStream;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * SolutionKitManager test with derby backend
 */
public class SolutionKitManagerEntityTest extends EntityManagerTest {

    private SolutionKitManager solutionKitManager;

    private static final long GOID_HI_START = Long.MAX_VALUE - 1;

    @Before
    public void setUp() throws Exception {
        solutionKitManager = applicationContext.getBean("solutionKitManager", SolutionKitManager.class);
    }

    @After
    public void tearDown() throws Exception {
    }

    private <T> T getTargetObject(final Object proxy, final Class<T> targetClass) throws Exception {
        Assert.assertNotNull(proxy);
        Assert.assertNotNull(targetClass);
        if (AopUtils.isJdkDynamicProxy(proxy)) {
            return getTargetObject(((Advised)proxy).getTargetSource().getTarget(), targetClass);
        }
        return targetClass.cast(proxy); // expected to be cglib proxy then, which is simply a specialized class
    }

    private static SolutionKit createSampleKit(final long ordinal) {
        return createSampleKit(ordinal, null, null, null, null, null, null, null);
    }

    private static final String DEFAULT_MAPPINGS = "default mappings";
    private static final String DEFAULT_UNINSTALL_BUNDLE = "default uninstall bundle";

    private static SolutionKit createSampleKit(
            final long ordinal,
            final String name,
            final Integer version,
            final String skGuid,
            final String skVersion,
            final String mappings,
            final String uninstallBundle,
            final Long lastUpdateTime
    ) {
        final SolutionKitBuilder builder = new SolutionKitBuilder();
        builder.goid(new Goid(GOID_HI_START, ordinal));
        builder.name(name != null ? name : "sk" + ordinal);
        builder.addProperty(SolutionKit.SK_PROP_DESC_KEY, "desc for sk" + ordinal);
        builder.addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false");  // default is a single skar
        builder.addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, String.valueOf(new Date().getTime()));
        builder.version(version);
        builder.skGuid(skGuid != null ? skGuid : UUID.randomUUID().toString());
        builder.skVersion(skVersion != null ? skVersion : "1.0");
        builder.mappings(mappings != null ? mappings : DEFAULT_MAPPINGS);
        builder.uninstallBundle(uninstallBundle != null ? uninstallBundle : DEFAULT_UNINSTALL_BUNDLE);
        builder.lastUpdateTime(lastUpdateTime != null ? lastUpdateTime : new Date().getTime());
        return builder.build();
    }

    private void flushSession() {
        session.flush();
        session.clear();
    }

    @Test
    public void test_invalid_ownership_descriptor_entity_type() throws Exception {
        // create a sample solution kit
        final SolutionKit newEntity = new SolutionKitBuilder(createSampleKit(1))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .addOwnershipDescriptor("id1", EntityType.FOLDER, false)
                .addOwnershipDescriptor("id2", EntityType.POLICY, true)
                .addOwnershipDescriptor("id3", EntityType.SECURE_PASSWORD, true)
                .build();
        final Goid goid = new Goid(GOID_HI_START, 1);
        solutionKitManager.save(goid, newEntity);
        Assert.assertEquals(goid, newEntity.getGoid());

        flushSession();

        final Collection<SolutionKit> kits = solutionKitManager.findAll();
        Assert.assertThat(newEntity, Matchers.isIn(kits));

        // update the solution_kit_goid set "id1" entity_type to "blah blah"
        final SQLQuery query = session.createSQLQuery("update solution_kit_meta set entity_type = 'blah blah' where solution_kit_goid = :goid and entity_id = :entityId");
        query.setBinary("goid", goid.getBytes());
        query.setString("entityId", "id1");
        final int rows = query.executeUpdate();
        Assert.assertThat("one row updated", rows, Matchers.equalTo(1));

        flushSession();

        try {
            solutionKitManager.findAll();
            Assert.fail("findAll should have failed");
        } catch (Exception e) {
            // expected
            // should be java.lang.IllegalArgumentException but any exception would work
        }
    }

    @Test
    public void testEntityDescriptorsEagerFetch() throws Exception {
        // create a sample solution kit
        final SolutionKit newEntity = new SolutionKitBuilder(createSampleKit(1))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .addOwnershipDescriptor("id1", EntityType.FOLDER, false)
                .addOwnershipDescriptor("id2", EntityType.POLICY, true)
                .addOwnershipDescriptor("id3", EntityType.SECURE_PASSWORD, true)
                .build();
        final Goid goid = new Goid(GOID_HI_START, 1);
        solutionKitManager.save(goid, newEntity);
        Assert.assertEquals(goid, newEntity.getGoid());

        flushSession();
        session.evict(newEntity); // remove from cache

        final Collection<SolutionKit> all = solutionKitManager.findAll();
        for (final SolutionKit kit : all) {
            session.evict(kit); // remove from cache
            Assert.assertTrue(Hibernate.isInitialized(kit.getEntityOwnershipDescriptors()));
        }
    }

    static class SampleBuilder {
        private static final String DEFAULT_EMPTY_UNINSTALL_BUNDLE = "com/l7tech/server/solutionkit/empty_uninstall_bundle.xml";
        private static final String DEFAULT_EMPTY_UNINSTALL_MAPPINGS = "com/l7tech/server/solutionkit/empty_uninstall_mappings.xml";

        private final String installBundle;
        private final String installMappings;
        private final String upgradeBundle;
        private final String upgradeMappings;
        private final String uninstallBundle;
        private final String uninstallMappings;

        public SampleBuilder(final String resourceFolder) throws Exception {
            this(resourceFolder, null, null);
        }

        public SampleBuilder(final String resourceFolder, final String resourceUninstallBundle, final String resourceUninstallMappings) throws Exception {
            Assert.assertThat(resourceFolder, Matchers.not(Matchers.isEmptyOrNullString()));
            final String resInstallBundle = resourceFolder + "/install_bundle.xml";
            final String resInstallMappings = resourceFolder + "/install_mappings.xml";
            final String resUpgradeBundle = resourceFolder + "/upgrade_bundle.xml";
            final String resUpgradeMappings = resourceFolder + "/upgrade_mappings.xml";
            final String resUninstallBundle = StringUtils.isNotBlank(resourceUninstallBundle) ? resourceUninstallBundle : DEFAULT_EMPTY_UNINSTALL_BUNDLE;
            final String resUninstallMappings = StringUtils.isNotBlank(resourceUninstallMappings) ? resourceUninstallMappings : DEFAULT_EMPTY_UNINSTALL_MAPPINGS;

            InputStream resourceStream = SolutionKitManagerEntityTest.class.getClassLoader().getResourceAsStream(resInstallBundle);
            Assert.assertNotNull(resourceStream);
            this.installBundle = new String(IOUtils.slurpStream(resourceStream));

            resourceStream = SolutionKitManagerEntityTest.class.getClassLoader().getResourceAsStream(resInstallMappings);
            Assert.assertNotNull(resourceStream);
            this.installMappings = new String(IOUtils.slurpStream(resourceStream));

            resourceStream = SolutionKitManagerEntityTest.class.getClassLoader().getResourceAsStream(resUpgradeBundle);
            Assert.assertNotNull(resourceStream);
            this.upgradeBundle = new String(IOUtils.slurpStream(resourceStream));

            resourceStream = SolutionKitManagerEntityTest.class.getClassLoader().getResourceAsStream(resUpgradeMappings);
            Assert.assertNotNull(resourceStream);
            this.upgradeMappings = new String(IOUtils.slurpStream(resourceStream));

            resourceStream = SolutionKitManagerEntityTest.class.getClassLoader().getResourceAsStream(resUninstallBundle);
            Assert.assertNotNull(resourceStream);
            this.uninstallBundle = new String(IOUtils.slurpStream(resourceStream));

            resourceStream = SolutionKitManagerEntityTest.class.getClassLoader().getResourceAsStream(resUninstallMappings);
            Assert.assertNotNull(resourceStream);
            this.uninstallMappings = new String(IOUtils.slurpStream(resourceStream));
        }

        public String getInstallBundle() {
            return installBundle;
        }

        public String getInstallMappings() {
            return installMappings;
        }

        public String getUpgradeBundle() {
            return upgradeBundle;
        }

        public String getUpgradeMappings() {
            return upgradeMappings;
        }

        public String getUninstallBundle() {
            return uninstallBundle;
        }

        public String getUninstallMappings() {
            return uninstallMappings;
        }
    }

    /**
     * =================================================================================================================
     * Solution Kit1 (SK1):
     * =================================================================================================================
     *
     * Creates 3 folders in the root folder.
     * Ownership is taken over (i.e. action Delete followed by AlwaysCreateNew)
     *
     * Install:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | true
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
     *
     * Upgrade:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | true
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
     *
     * Uninstall:
     *      * nothing is deleted
     * =================================================================================================================
     *
     *
     * =================================================================================================================
     * Solution Kit2 (SK2):
     * =================================================================================================================
     *
     * Creates 3 folders in the root folder.
     * Ownership is taken over (i.e. action Delete followed by AlwaysCreateNew)
     *
     * Install:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
     *
     * Upgrade:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
     *
     * Uninstall:
     *      * nothing is deleted
     * =================================================================================================================
     *
     *
     * =================================================================================================================
     * Solution Kit3 (SK3):
     * =================================================================================================================
     *
     * Creates 3 folders in the root folder.
     * Ownership is taken over (i.e. action Delete followed by AlwaysCreateNew)
     *
     * Install:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | true
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
     *
     * Upgrade:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | true
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
     *
     * Uninstall:
     *      * nothing is deleted
     * =================================================================================================================
     *
     *
     * =================================================================================================================
     * Solution Kit4 (SK4):
     * =================================================================================================================
     *
     * Creates 3 folders in the root folder.
     * Ownership is taken over (i.e. action Delete followed by AlwaysCreateNew)
     *
     * Install:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
     *
     * Upgrade:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
     * FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
     * FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
     *
     * Uninstall:
     *      * nothing is deleted
     * =================================================================================================================
     *
     *
     * =================================================================================================================
     * Solution Kit5 (SK5):
     * =================================================================================================================
     *
     * Creates 3 folders in the root folder.
     * Ownership is taken over (i.e. action Delete followed by AlwaysCreateNew)
     *
     * Install:
     * Type     | Name           | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER   | test           | 57eac30c4a5648db1f90617a721642fd | true
     * POLICY   | testFragment1  | 57eac30c4a5648db1f90617a72164391 | true
     * SERVICE  | test01         | 57eac30c4a5648db1f90617a72164341 | true
     *
     * Upgrade:
     * Type    | Name     | Goid                             | Read-Only
     * -----------------------------------------------------------------------------------------------------------------
     * FOLDER   | test           | 57eac30c4a5648db1f90617a721642fd | false
     * POLICY   | testFragment1  | 57eac30c4a5648db1f90617a72164391 | false
     * SERVICE  | test01         | 57eac30c4a5648db1f90617a72164341 | false
     *
     * Uninstall:
     *      * nothing is deleted
     * =================================================================================================================
     *
     *
     * =================================================================================================================
     * Testing for Folder1:
     * =================================================================================================================
     *
     * 1. Install SK1(Folder1 is readonly), then SK2(Folder1 is user editable), then SK3(Folder1 is readonly), finally SK4(Folder1 is user editable):
     *      * ProtectedEntityTracker would have user editable for Folder1 i.e. SK4(0)->SK3(1)->SK2(0)->SK1(1)
     * 2. Uninstall SK4:
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK3(1)->SK2(0)->SK1(1)
     * 3. Upgrade SK2 (Folder1 to read-only):
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK2(0)->SK3(1)->SK1(1)
     * 4. Upgrade SK1 (Folder1 to user editable):
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK1(1)->SK2(0)->SK3(1)
     * 5. Uninstall SK1:
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK2(0)->SK3(1)
     * 6. Install SK4 once more:
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK4(0)->SK2(0)->SK3(1)
     * 7. Upgrade SK4:
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK4(0)->SK2(0)->SK3(1)
     * 8. Install SK5:
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK4(0)->SK2(0)->SK3(1)
     * 9. Upgrade SK5:
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK4(0)->SK2(0)->SK3(1)
     * 10. Uninstall SK5:
     *      * ProtectedEntityTracker would have read-only for Folder1 i.e. SK4(0)->SK2(0)->SK3(1)
     *
     * @throws Exception
     */
    @BugId("SSG-12307")
    @Test
    public void testTakingOverEntityReadOnlyNess() throws Exception {
        Mockito.validateMockitoUsage();

        // mock LicenseManager as it's not available as a bean
        final LicenseManager licenseManager = Mockito.mock(LicenseManager.class);
        Assert.assertNotNull(licenseManager);

        // mock our own proxy SolutionKitManager, save and update are proxied via the real solutionKitManager
        final SolutionKitManager skManager = Mockito.spy(
                new SolutionKitManagerProxy(
                        solutionKitManager,
                        new Functions.NullaryVoidThrows<RuntimeException>() {
                            @Override
                            public void call() throws RuntimeException {
                                flushSession();
                            }
                        }
                )
        );
        Assert.assertNotNull(skManager);

        // Mock IdentityProviderConfigManager as it's not available as a bean
        final IdentityProviderConfigManager identityProviderConfigManager = Mockito.mock(IdentityProviderConfigManager.class);

        // create the admin helper
        //final SolutionKitAdminHelper helper = new SolutionKitAdminHelper(licenseManager, skManager, identityProviderConfigManager);
        final SolutionKitAdminHelper helper = Mockito.spy(new SolutionKitAdminHelper(licenseManager, skManager, identityProviderConfigManager));
        Assert.assertNotNull(helper);

        // samples
        final SampleBuilder builder1 = new SampleBuilder("com/l7tech/server/solutionkit/kit1");
        final SampleBuilder builder2 = new SampleBuilder("com/l7tech/server/solutionkit/kit2");
        final SampleBuilder builder3 = new SampleBuilder("com/l7tech/server/solutionkit/kit3");
        final SampleBuilder builder4 = new SampleBuilder("com/l7tech/server/solutionkit/kit4");
        final SampleBuilder builder5 = new SampleBuilder("com/l7tech/server/solutionkit/kit5");

        // create our sample kits (SK1, SK2, SK3 and SK4)
        final SolutionKit sk1 = new SolutionKitBuilder(createSampleKit(1))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .uninstallBundle(builder1.getUninstallBundle())
                .build();
        final SolutionKit sk2 = new SolutionKitBuilder(createSampleKit(2))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .uninstallBundle(builder2.getUninstallBundle())
                .build();
        final SolutionKit sk3 = new SolutionKitBuilder(createSampleKit(3))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .uninstallBundle(builder3.getUninstallBundle())
                .build();
        final SolutionKit sk4 = new SolutionKitBuilder(createSampleKit(4))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .uninstallBundle(builder4.getUninstallBundle())
                .build();
        final SolutionKit sk5 = new SolutionKitBuilder(createSampleKit(5))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .uninstallBundle(builder5.getUninstallBundle())
                .build();

        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 1a. install sk1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test descriptors
        // Install:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | true
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
        doTestInstallBundle(
                skManager,
                helper,
                sk1,
                builder1,
                new Functions.NullaryThrows<Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>>, Exception>() {
                    @Override
                    public Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> call() throws Exception {
                        return CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                                .put(
                                        sk1.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .unmodifiableMap()
                                )
                                .unmodifiableMap();
                    }
                }
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), false)
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 1b. install sk2
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test descriptors
        // Install:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
        doTestInstallBundle(
                skManager,
                helper,
                sk2,
                builder2,
                new Functions.NullaryThrows<Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>>, Exception>() {
                    @Override
                    public Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> call() throws Exception {
                        return CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                                .put(
                                        sk1.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk2.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .unmodifiableMap()
                                )
                                .unmodifiableMap();
                    }
                }
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), false)
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 1c. install sk3
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test descriptors
        // Install:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | true
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
        doTestInstallBundle(
                skManager,
                helper,
                sk3,
                builder3,
                new Functions.NullaryThrows<Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>>, Exception>() {
                    @Override
                    public Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> call() throws Exception {
                        return CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                                .put(
                                        sk1.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk2.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk3.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .unmodifiableMap()
                                )
                                .unmodifiableMap();
                    }
                }
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), true )
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 1d. install sk4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test descriptors
        // Install:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
        doTestInstallBundle(
                skManager,
                helper,
                sk4,
                builder4,
                new Functions.NullaryThrows<Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>>, Exception>() {
                    @Override
                    public Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> call() throws Exception {
                        return CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                                .put(
                                        sk1.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk2.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk3.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk4.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .unmodifiableMap()
                                )
                                .unmodifiableMap();
                    }
                }
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), true )
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 2. delete sk4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        doTestUninstallBundle(
                skManager,
                helper,
                sk4.getGoid(),
                builder4,
                CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                        .put(
                                sk1.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk2.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk3.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .unmodifiableMap()
                        )
                        .unmodifiableMap()
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), true )
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 3. upgrade sk2
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Upgrade:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | false
        doTestUpgradeBundle(
                skManager,
                helper,
                sk2.getGoid(),
                builder2,
                CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                        .put(
                                sk1.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 4))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 4))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 4))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk2.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk3.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                        .unmodifiableMap()
                        )
                        .unmodifiableMap()
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), false)
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 4. upgrade sk1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Upgrade:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | true
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
        doTestUpgradeBundle(
                skManager,
                helper,
                sk1.getGoid(),
                builder1,
                CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                        .put(
                                sk1.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk2.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk3.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .unmodifiableMap()
                        )
                        .unmodifiableMap()
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), true),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), true),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), true)
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 5. delete sk1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        doTestUninstallBundle(
                skManager,
                helper,
                sk1.getGoid(),
                builder1,
                CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                        .put(
                                sk2.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 1))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk3.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .unmodifiableMap()
                        )
                        .unmodifiableMap()
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), false)
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 6. install sk4 once more
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        final SolutionKit sk4Prime = new SolutionKitBuilder(createSampleKit(4))
                .goid(Goid.DEFAULT_GOID) // reset the goid
                .uninstallBundle(builder4.getUninstallBundle())
                .build();
        // test descriptors
        // Install:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | true
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
        doTestInstallBundle(
                skManager,
                helper,
                sk4Prime,
                builder4,
                new Functions.NullaryThrows<Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>>, Exception>() {
                    @Override
                    public Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> call() throws Exception {
                        return CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                                .put(
                                        sk2.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 2))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk3.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 4))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 4))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 4))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk4Prime.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .unmodifiableMap()
                                )
                                .unmodifiableMap();
                    }
                }
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), true )
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 7. upgrade sk4Prime
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Upgrade:
        // Type    | Name     | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER  | Folder1  | 12acd9006a6f7d2ea9b992f3882e98c2 | false
        // FOLDER  | Folder2  | 12acd9006a6f7d2ea9b992f3882e9904 | false
        // FOLDER  | Folder3  | 12acd9006a6f7d2ea9b992f3882e9945 | true
        doTestUpgradeBundle(
                skManager,
                helper,
                sk4Prime.getGoid(),
                builder4,
                CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                        .put(
                                sk2.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk3.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk4Prime.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                        .unmodifiableMap()
                        )
                        .unmodifiableMap()
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), true )
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 8. install sk5
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // test descriptors
        // Install:
        // Type     | Name           | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER   | test           | 57eac30c4a5648db1f90617a721642fd | true
        // POLICY   | testFragment1  | 57eac30c4a5648db1f90617a72164391 | true
        // SERVICE  | test01         | 57eac30c4a5648db1f90617a72164341 | true
        doTestInstallBundle(
                skManager,
                helper,
                sk5,
                builder5,
                new Functions.NullaryThrows<Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>>, Exception>() {
                    @Override
                    public Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> call() throws Exception {
                        return CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                                .put(
                                        sk2.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk3.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk4Prime.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .unmodifiableMap()
                                )
                                .put(
                                        sk5.getGoid(),
                                        CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                                .put("57eac30c4a5648db1f90617a721642fd", Triple.triple(EntityType.FOLDER,  true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("57eac30c4a5648db1f90617a72164391", Triple.triple(EntityType.POLICY,  true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .put("57eac30c4a5648db1f90617a72164341", Triple.triple(EntityType.SERVICE, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                                .unmodifiableMap()
                                )
                                .unmodifiableMap();
                    }
                }
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "12acd9006a6f7d2ea9b992f3882e9945"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "57eac30c4a5648db1f90617a721642fd"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.POLICY , "57eac30c4a5648db1f90617a72164391"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.SERVICE, "57eac30c4a5648db1f90617a72164341"), true )
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 9. upgrade sk5
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Upgrade:
        // Type     | Name           | Goid                             | Read-Only
        // -----------------------------------------------------------------------------------------------------------------
        // FOLDER   | test           | 57eac30c4a5648db1f90617a721642fd | false
        // POLICY   | testFragment1  | 57eac30c4a5648db1f90617a72164391 | false
        // SERVICE  | test01         | 57eac30c4a5648db1f90617a72164341 | false
        doTestUpgradeBundle(
                skManager,
                helper,
                sk5.getGoid(),
                builder5,
                CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                        .put(
                                sk2.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk3.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk4Prime.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk5.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("57eac30c4a5648db1f90617a721642fd", Triple.triple(EntityType.FOLDER,  false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("57eac30c4a5648db1f90617a72164391", Triple.triple(EntityType.POLICY,  false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("57eac30c4a5648db1f90617a72164341", Triple.triple(EntityType.SERVICE, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .unmodifiableMap()
                        )
                        .unmodifiableMap()
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "12acd9006a6f7d2ea9b992f3882e9945"), true ),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER , "57eac30c4a5648db1f90617a721642fd"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.POLICY , "57eac30c4a5648db1f90617a72164391"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.SERVICE, "57eac30c4a5648db1f90617a72164341"), false)
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Step 10. delete sk5
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        doTestUninstallBundle(
                skManager,
                helper,
                sk5.getGoid(),
                builder5,
                CollectionUtils.<Goid, Map<String, Triple<EntityType, Boolean, Long>>>mapBuilder()
                        .put(
                                sk2.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 3))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk3.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP - 5))
                                        .unmodifiableMap()
                        )
                        .put(
                                sk4Prime.getGoid(),
                                CollectionUtils.<String, Triple<EntityType, Boolean, Long>>mapBuilder()
                                        .put("12acd9006a6f7d2ea9b992f3882e98c2", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9904", Triple.triple(EntityType.FOLDER, false, EntityOwnershipDescriptor.OWNED_STAMP))
                                        .put("12acd9006a6f7d2ea9b992f3882e9945", Triple.triple(EntityType.FOLDER, true , EntityOwnershipDescriptor.OWNED_STAMP))
                                        .unmodifiableMap()
                        )
                        .unmodifiableMap()
        );
        verifyProtectedEntityTracker(
                CollectionUtils.list(
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e98c2"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9904"), false),
                        Pair.pair(new EntityHeaderRef(EntityType.FOLDER, "12acd9006a6f7d2ea9b992f3882e9945"), true )
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    }

    private void doTestInstallBundle(
            final SolutionKitManager skManager,
            final SolutionKitAdminHelper helper,
            final SolutionKit solutionKit,
            final SampleBuilder builder,
            final Functions.NullaryThrows<Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>>, Exception> expectedDescriptorsPerKitCallback
    ) throws Exception {
        Assert.assertNotNull(skManager);
        Assert.assertNotNull(helper);
        Assert.assertNotNull(solutionKit);
        Assert.assertNotNull(builder);
        Assert.assertNotNull(expectedDescriptorsPerKitCallback);

        // mock importBundle to return sk install mappings (i.e. builder.getInstallMappings())
        Mockito.doReturn(builder.getInstallMappings()).when(skManager).importBundle(Mockito.anyString(), Mockito.<SolutionKit>any(), Mockito.anyBoolean());
        Mockito.doReturn(builder.getInstallMappings()).when(helper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        // solutionKit sanity check
        Assert.assertThat(solutionKit.getGoid(), Matchers.anyOf(Matchers.equalTo(Goid.DEFAULT_GOID), Matchers.nullValue()));
        Assert.assertThat(solutionKit.getEntityOwnershipDescriptors(), Matchers.anyOf(Matchers.emptyCollectionOf(EntityOwnershipDescriptor.class), Matchers.nullValue()));

        // install solutionKit
        final Goid skGoid = helper.install(solutionKit, builder.getInstallBundle(), false);
        Assert.assertThat(skGoid, Matchers.allOf(Matchers.not(Matchers.equalTo(Goid.DEFAULT_GOID)), Matchers.notNullValue()));
        Assert.assertThat(solutionKit.getGoid(), Matchers.equalTo(skGoid));

        flushSession(); // flush hibernate session just in case
        // retrieve persisted entity
        final SolutionKit sk = solutionKitManager.findByPrimaryKey(skGoid);
        Assert.assertNotNull(sk);
        Assert.assertThat(sk.getMappings(), Matchers.equalTo(builder.getInstallMappings()));

        // test descriptors
        verifyDescriptors(expectedDescriptorsPerKitCallback.call());
    }

    private void doTestUninstallBundle(
            final SolutionKitManager skManager,
            final SolutionKitAdminHelper helper,
            final Goid skGoid,
            final SampleBuilder builder,
            final Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> expectedDescriptorsPerKit
    ) throws Exception {
        Assert.assertNotNull(skManager);
        Assert.assertNotNull(helper);
        Assert.assertThat(skGoid, Matchers.allOf(Matchers.not(Matchers.equalTo(Goid.DEFAULT_GOID)), Matchers.notNullValue()));
        Assert.assertNotNull(builder);
        Assert.assertNotNull(expectedDescriptorsPerKit);

        // mock importBundle to return sk uninstall mappings (i.e. builder.getInstallMappings())
        Mockito.doReturn(builder.getUninstallMappings()).when(skManager).importBundle(Mockito.anyString(), Mockito.<SolutionKit>any(), Mockito.anyBoolean());
        Mockito.doReturn(builder.getUninstallMappings()).when(helper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());


        // uninstall the kit
        helper.uninstall(skGoid);

        flushSession(); // flush hibernate session just in case
        // make sure the kit is
        final SolutionKit sk = solutionKitManager.findByPrimaryKey(skGoid);
        Assert.assertNull(sk);

        // test version maps
        verifyDescriptors(expectedDescriptorsPerKit);
    }

    private void doTestUpgradeBundle(
            final SolutionKitManager skManager,
            final SolutionKitAdminHelper helper,
            Goid skGoid,
            final SampleBuilder builder,
            final Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> expectedDescriptorsPerKit
    ) throws Exception {
        Assert.assertNotNull(skManager);
        Assert.assertNotNull(helper);
        Assert.assertThat(skGoid, Matchers.allOf(Matchers.not(Matchers.equalTo(Goid.DEFAULT_GOID)), Matchers.notNullValue()));
        Assert.assertNotNull(builder);
        Assert.assertNotNull(expectedDescriptorsPerKit);

        // mock importBundle to return sk upgrade mappings (i.e. builder.getUpgradeMappings())
        Mockito.doReturn(builder.getUpgradeMappings()).when(skManager).importBundle(Mockito.anyString(), Mockito.<SolutionKit>any(), Mockito.anyBoolean());
        Mockito.doReturn(builder.getUpgradeMappings()).when(helper).getMappingsWithEntityNameAddedToProperties(Mockito.anyString(), Mockito.anyString());

        flushSession(); // flush hibernate session just in case
        // get the kit to upgrade
        SolutionKit solutionKit = skManager.findByPrimaryKey(skGoid);
        Assert.assertNotNull(solutionKit);

        // install solutionKit
        skGoid = helper.install(solutionKit, builder.getUpgradeBundle(), true);
        Assert.assertThat(skGoid, Matchers.allOf(Matchers.not(Matchers.equalTo(Goid.DEFAULT_GOID)), Matchers.notNullValue()));
        Assert.assertThat(solutionKit.getGoid(), Matchers.equalTo(skGoid));

        flushSession(); // flush hibernate session just in case
        // retrieve persisted entity
        solutionKit = solutionKitManager.findByPrimaryKey(skGoid);
        Assert.assertNotNull(solutionKit);
        Assert.assertThat(solutionKit.getMappings(), Matchers.equalTo(builder.getUpgradeMappings()));

        // test descriptors
        verifyDescriptors(expectedDescriptorsPerKit);
    }

    private void verifyDescriptors(
            final Map<Goid, Map<String, Triple<EntityType, Boolean, Long>>> expectedDescriptorsPerKit
    ) throws Exception {
        Assert.assertNotNull(expectedDescriptorsPerKit);

        flushSession(); // flush hibernate session just in case

        // make sure all kits have the correct version stamp
        final Collection<SolutionKit> kits = solutionKitManager.findAll();
        Assert.assertThat(kits, Matchers.allOf(Matchers.hasSize(expectedDescriptorsPerKit.size()), Matchers.notNullValue()));
        for (final SolutionKit kit : kits) {
            Assert.assertThat(kit.getGoid(), Matchers.allOf(Matchers.not(Matchers.equalTo(Goid.DEFAULT_GOID)), Matchers.notNullValue()));
            final Map<String, Triple<EntityType, Boolean, Long>> expectedDescriptors = expectedDescriptorsPerKit.get(kit.getGoid());
            Assert.assertNotNull(expectedDescriptors);
            final Collection<EntityOwnershipDescriptor> descriptors = kit.getEntityOwnershipDescriptors();
            Assert.assertThat(descriptors, Matchers.allOf(Matchers.hasSize(expectedDescriptors.size()), Matchers.notNullValue()));
            for (final EntityOwnershipDescriptor descriptor : descriptors) {
                Assert.assertNotNull(descriptor);
                Assert.assertThat(descriptor.getEntityId(), Matchers.not(Matchers.isEmptyOrNullString()));
                final Triple<EntityType, Boolean, Long> expectedDescriptor = expectedDescriptors.get(descriptor.getEntityId());
                Assert.assertNotNull(expectedDescriptor);
                Assert.assertThat(descriptor.getEntityType(), Matchers.is(expectedDescriptor.left));
                Assert.assertThat(descriptor.isReadOnly(), Matchers.is(expectedDescriptor.middle));
                Assert.assertThat(descriptor.getVersionStamp(), Matchers.is(expectedDescriptor.right));
            }
        }
    }

    private SolutionKitManagerImpl solutionKitManagerImpl;
    private SolutionKitManagerImpl getSolutionKitManagerImpl() throws Exception {
        if (solutionKitManagerImpl == null) {
            solutionKitManagerImpl = getTargetObject(solutionKitManager, SolutionKitManagerImpl.class);
        }
        return solutionKitManagerImpl;
    }

    private ProtectedEntityTracker protectedEntityTracker;
    private ProtectedEntityTracker getProtectedEntityTracker() throws Exception {
        if (protectedEntityTracker == null) {
            protectedEntityTracker = applicationContext.getBean("protectedEntityTracker", ProtectedEntityTracker.class);
        }
        return protectedEntityTracker;
    }

    private void verifyProtectedEntityTracker(
            Collection<Pair<EntityHeaderRef, Boolean>> expectedReadOnlyFlags
    ) throws Exception {
        Assert.assertNotNull(expectedReadOnlyFlags);
        final SolutionKitManagerImpl solutionKitManagerImpl = getSolutionKitManagerImpl();
        Assert.assertNotNull(solutionKitManagerImpl);
        final ProtectedEntityTracker protectedEntityTracker = getProtectedEntityTracker();
        Assert.assertNotNull(protectedEntityTracker);
        // updateProtectedEntityTracking by emulating EntityInvalidationEvent
        solutionKitManagerImpl.handleEvent(new EntityInvalidationEvent(this, SolutionKit.class, new Goid[0], new char[0]));
        // now make sure the protected entity tracker has the right state
        for (final Pair<EntityHeaderRef, Boolean> expectedReadOnlyFlag : expectedReadOnlyFlags) {
            Assert.assertNotNull(expectedReadOnlyFlag.left);
            Assert.assertNotNull(expectedReadOnlyFlag.right);
            Assert.assertThat(
                    protectedEntityTracker.isReadOnlyEntity(expectedReadOnlyFlag.left),
                    Matchers.is(expectedReadOnlyFlag.right)
            );
        }
    }

    // TODO: add more tests here
}
