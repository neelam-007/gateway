package com.l7tech.server.solutionkit;

import com.l7tech.gateway.common.solutionkit.EntityOwnershipDescriptor;
import com.l7tech.gateway.common.solutionkit.SolutionKit;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.Goid;
import com.l7tech.server.EntityManagerTest;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.Triple;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.hibernate.Hibernate;
import org.hibernate.SQLQuery;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.*;


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

    @SuppressWarnings("UnusedDeclaration")
    static class SolutionKitBuilder {
        private Goid goid;
        private String name;
        private Integer version;
        private String skGuid;
        private String skVersion;
        private String mappings;
        private String uninstallBundle;
        private Long lastUpdateTime;

        private final Map<String, String> properties = new HashMap<>();
        private final Map<String, String> installProperties = new HashMap<>();
        private final Collection<Triple<String, EntityType, Boolean>> ownershipDescriptors = new ArrayList<>();

        /**
         * Pre-attached {@link SolutionKit}.
         * The builder will append new properties or override existing ones.
         */
        private SolutionKit solutionKit;


        /**
         * Default constructor
         */
        public SolutionKitBuilder() {
            this(new SolutionKit());
        }

        /**
         * Initialize the builder with preexisting solution kit.
         * This way the builder will append new properties or override existing ones.
         *
         * @param solutionKit    the {@code SolutionKit} to attach to.
         */
        public SolutionKitBuilder(final SolutionKit solutionKit) {
            this.solutionKit = solutionKit;
        }

        public SolutionKitBuilder goid(final Goid goid) {
            this.goid = goid;
            return this;
        }

        public SolutionKitBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public SolutionKitBuilder version(final Integer version) {
            this.version = version;
            return this;
        }

        public SolutionKitBuilder skGuid(final String skGuid) {
            this.skGuid = skGuid;
            return this;
        }

        public SolutionKitBuilder skVersion(final String skVersion) {
            this.skVersion = skVersion;
            return this;
        }

        public SolutionKitBuilder mappings(final String mappings) {
            this.mappings = mappings;
            return this;
        }

        public SolutionKitBuilder uninstallBundle(final String uninstallBundle) {
            this.uninstallBundle = uninstallBundle;
            return this;
        }

        public SolutionKitBuilder lastUpdateTime(final Long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
            return this;
        }

        public SolutionKitBuilder addProperty(final String name, final String value) {
            this.properties.put(name, value);
            return this;
        }

        public SolutionKitBuilder clearProperties() {
            this.properties.clear();
            return this;
        }

        public SolutionKitBuilder addInstallProperty(final String name, final String value) {
            this.installProperties.put(name, value);
            return this;
        }

        public SolutionKitBuilder clearInstallProperties() {
            this.installProperties.clear();
            return this;
        }

        private SolutionKitBuilder addOwnershipDescriptor(final String id, final EntityType type, final boolean readOnly) {
            this.ownershipDescriptors.add(Triple.triple(id, type, readOnly));
            return this;
        }

        public SolutionKit build() {
            Assert.assertNotNull(solutionKit);

            if (goid != null) {
                solutionKit.setGoid(goid);
            }
            if (name != null) {
                solutionKit.setName(name);
            }
            if (version != null) {
                solutionKit.setVersion(version);
            }
            if (skGuid != null) {
                solutionKit.setSolutionKitGuid(skGuid);
            }
            if (skVersion != null) {
                solutionKit.setSolutionKitVersion(skVersion);
            }
            if (mappings != null) {
                solutionKit.setMappings(mappings);
            }
            if (uninstallBundle != null) {
                solutionKit.setUninstallBundle(uninstallBundle);
            }
            if (lastUpdateTime != null) {
                solutionKit.setLastUpdateTime(lastUpdateTime);
            }

            final Set<String> propertyKeys = CollectionUtils.set(SolutionKit.getPropertyKeys());
            for (final Map.Entry<String, String> property : properties.entrySet()) {
                if (propertyKeys.contains(property.getKey())) {
                    solutionKit.setProperty(property.getKey(), property.getValue());
                } else {
                    Assert.fail("Unsupported property: " + property.getKey());
                }
            }

            for (final Map.Entry<String, String> property : installProperties.entrySet()) {
                solutionKit.setInstallationProperty(property.getKey(), property.getValue());
            }

            final Set<EntityOwnershipDescriptor> ownershipDescriptorSet = new HashSet<>(ownershipDescriptors.size());
            for (final Triple<String, EntityType, Boolean> ownershipDescriptor : ownershipDescriptors) {
                Assert.assertTrue(StringUtils.isNotEmpty(ownershipDescriptor.left) && ownershipDescriptor.middle != null && ownershipDescriptor.right != null);
                ownershipDescriptorSet.add(
                        new EntityOwnershipDescriptor(
                                solutionKit,
                                ownershipDescriptor.left,
                                ownershipDescriptor.middle,
                                ownershipDescriptor.right
                        )
                );
            }
            solutionKit.addEntityOwnershipDescriptors(ownershipDescriptorSet);

            return solutionKit;
        }
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
        builder.version(version);
        builder.skGuid(skGuid != null ? skGuid : UUID.randomUUID().toString());
        builder.skVersion(skVersion != null ? skVersion : "1.0");
        builder.mappings(mappings != null ? mappings : DEFAULT_MAPPINGS);
        builder.uninstallBundle(uninstallBundle != null ? uninstallBundle : DEFAULT_UNINSTALL_BUNDLE);
        builder.lastUpdateTime(lastUpdateTime != null ? lastUpdateTime : new Date().getTime());
        return builder.build();
    }

    @Test
    public void test_invalid_ownership_descriptor_entity_type() throws Exception {
        // create a sample solution kit
        final SolutionKit newEntity = new SolutionKitBuilder(createSampleKit(1))
                .goid(null) // reset the goid
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "desc for sk1")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false")
                .addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "2014-11-03T12:56:35.603-08:00")
                .addOwnershipDescriptor("id1", EntityType.FOLDER, false)
                .addOwnershipDescriptor("id2", EntityType.POLICY, true)
                .addOwnershipDescriptor("id3", EntityType.SECURE_PASSWORD, true)
                .build();
        final Goid goid = new Goid(GOID_HI_START, 1);
        solutionKitManager.save(goid, newEntity);
        Assert.assertEquals(goid, newEntity.getGoid());

        session.flush();
        session.clear();

        final Collection<SolutionKit> kits = solutionKitManager.findAll();
        Assert.assertThat(newEntity, Matchers.isIn(kits));

        // update the solution_kit_goid set "id1" entity_type to "blah blah"
        final SQLQuery query = session.createSQLQuery("update solution_kit_meta set entity_type = 'blah blah' where solution_kit_goid = :goid and entity_id = :entityId");
        query.setBinary("goid", goid.getBytes());
        query.setString("entityId", "id1");
        final int rows = query.executeUpdate();
        Assert.assertThat("one row updated", rows, Matchers.equalTo(1));

        session.flush();
        session.clear();

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
                .goid(null) // reset the goid
                .addProperty(SolutionKit.SK_PROP_DESC_KEY, "desc for sk1")
                .addProperty(SolutionKit.SK_PROP_IS_COLLECTION_KEY, "false")
                .addProperty(SolutionKit.SK_PROP_TIMESTAMP_KEY, "2014-11-03T12:56:35.603-08:00")
                .addOwnershipDescriptor("id1", EntityType.FOLDER, false)
                .addOwnershipDescriptor("id2", EntityType.POLICY, true)
                .addOwnershipDescriptor("id3", EntityType.SECURE_PASSWORD, true)
                .build();
        final Goid goid = new Goid(GOID_HI_START, 1);
        solutionKitManager.save(goid, newEntity);
        Assert.assertEquals(goid, newEntity.getGoid());

        session.flush();
        session.clear();
        session.evict(newEntity); // remove from cache

        final Collection<SolutionKit> all = solutionKitManager.findAll();
        for (final SolutionKit kit : all) {
            session.evict(kit); // remove from cache
            Assert.assertTrue(Hibernate.isInitialized(kit.getEntityOwnershipDescriptors()));
        }
    }

    @Test
    public void testFindBySolutionKitGuid() throws Exception {

    }

    @Test
    public void testFindBySolutionKitGuidAndIM() throws Exception {

    }

    @Test
    public void testUpdateProtectedEntityTracking() throws Exception {

    }

    @Test
    public void testFindAllChildrenHeadersByParentGoid() throws Exception {

    }

    @Test
    public void testFindAllChildrenByParentGoid() throws Exception {

    }

    @Test
    public void testFindAllExcludingChildren() throws Exception {

    }

    @Test
    public void testFindParentSolutionKits() throws Exception {

    }
}