package com.l7tech.server.security.rbac;

import com.l7tech.gateway.common.module.ServerModuleFile;
import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.gateway.common.security.rbac.EntityProtectionInfo;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.Entity;
import com.l7tech.objectmodel.EntityHeaderRef;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.policy.CustomKeyValueStore;
import com.l7tech.policy.Policy;
import com.l7tech.server.ServerConfig;
import com.l7tech.server.ServerConfigParams;
import com.l7tech.server.ServerConfigStub;
import com.l7tech.util.CollectionUtils;
import com.l7tech.util.ConfigFactory;
import com.l7tech.util.Pair;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Callable;

public class ProtectedEntityTrackerTest {

    private ProtectedEntityTracker protectedEntityTracker;
    private ServerConfig config;

    @Before
    public void setUp() throws Exception {
        config = new ServerConfigStub();
        config.putProperty(ServerConfigParams.PARAM_PROTECTED_ENTITY_TRACKER_ENABLE, String.valueOf(true));
        protectedEntityTracker = new ProtectedEntityTracker(config);
        ConfigFactory.clearCachedConfig();

        Assert.assertNotNull(protectedEntityTracker);
        Assert.assertTrue(protectedEntityTracker.isEnabled());
    }

    @After
    public void tearDown() throws Exception {
        // clean up
        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(Collections.<Pair< EntityType, String>>emptyList());
        Assert.assertThat(protectedEntityTracker.getProtectedEntities(), Matchers.anyOf(Matchers.nullValue(Map.class), Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap())));
    }

    @Test
    public void testIsEnabled() throws Exception {
        // add few test samples
        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(
                CollectionUtils.list(
                        Pair.pair(EntityType.SERVER_MODULE_FILE, "id1"),
                        Pair.pair(EntityType.SECURE_PASSWORD, "id2"),
                        Pair.pair(EntityType.CUSTOM_KEY_VALUE_STORE, "id3"),
                        Pair.pair(EntityType.FOLDER, "id4")
                )
        );

        // by default the tracker is enabled
        Assert.assertTrue(protectedEntityTracker.isEnabled());
        Assert.assertTrue(protectedEntityTracker.isEntityProtectionEnabled());
        Assert.assertThat(protectedEntityTracker.getProtectedEntities(), Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap()))));

        // disable it
        config.putProperty(ServerConfigParams.PARAM_PROTECTED_ENTITY_TRACKER_ENABLE, String.valueOf(false));
        ConfigFactory.clearCachedConfig();

        // test that the tracker is disabled
        Assert.assertFalse(protectedEntityTracker.isEnabled());
        Assert.assertFalse(protectedEntityTracker.isEntityProtectionEnabled());
        Assert.assertThat(protectedEntityTracker.getProtectedEntities(), Matchers.anyOf(Matchers.nullValue(Map.class), Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap())));
    }

    @Test
    public void testBulkUpdateReadOnlyEntitiesList() throws Exception {
        // make sure the tracker is enabled and there are no protected entities
        Assert.assertTrue(protectedEntityTracker.isEnabled());
        Assert.assertThat(protectedEntityTracker.getProtectedEntities(), Matchers.anyOf(Matchers.nullValue(Map.class), Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap())));

        // add few test samples
        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(
                CollectionUtils.list(
                        Pair.pair(EntityType.SERVER_MODULE_FILE, "id1"),
                        Pair.pair(EntityType.SECURE_PASSWORD, "id2"),
                        Pair.pair(EntityType.CUSTOM_KEY_VALUE_STORE, "id3"),
                        Pair.pair(EntityType.FOLDER, "id4")
                )
        );
        // test protected entities
        Map<String, EntityProtectionInfo> entityProtectionInfoMap = protectedEntityTracker.getProtectedEntities();
        Assert.assertThat(entityProtectionInfoMap, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap()))));
        Assert.assertThat(entityProtectionInfoMap.values(), Matchers.hasSize(4));
        Assert.assertThat(entityProtectionInfoMap,
                Matchers.allOf(
                        Matchers.hasKey("id1"),
                        Matchers.hasKey("id2"),
                        Matchers.hasKey("id3"),
                        Matchers.hasKey("id4")
                )
        );
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new ServerModuleFile()));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.SERVER_MODULE_FILE, null)));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new ServerModuleFile() {
            @Override
            public String getId() {
                return "id2";
            }
        }));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.SERVER_MODULE_FILE, "id2")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new ServerModuleFile() {
            @Override
            public String getId() {
                return "id1";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.SERVER_MODULE_FILE, "id1")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new SecurePassword() {
            @Override
            public String getId() {
                return "id2";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.SECURE_PASSWORD, "id2")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new CustomKeyValueStore() {
            @Override
            public String getId() {
                return "id3";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.CUSTOM_KEY_VALUE_STORE, "id3")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new Folder() {
            @Override
            public String getId() {
                return "id4";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.FOLDER, "id4")));

        // add few test samples (id11, id12, id13, id14 and id15) with duplicates (id11, id13 and id14)
        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(
                CollectionUtils.list(
                        Pair.pair(EntityType.SERVER_MODULE_FILE, "id11"),
                        Pair.pair(EntityType.SECURE_PASSWORD, "id12"),
                        Pair.pair(EntityType.CUSTOM_KEY_VALUE_STORE, "id13"),
                        Pair.pair(EntityType.FOLDER, "id14"),
                        Pair.pair(EntityType.POLICY, "id14"), // override id114
                        Pair.pair(EntityType.SERVICE, "id13"), // override id113
                        Pair.pair(EntityType.FOLDER, "id15"),
                        Pair.pair(EntityType.ENCAPSULATED_ASSERTION, "id11") // override id11
                )
        );
        // test protected entities
        entityProtectionInfoMap = protectedEntityTracker.getProtectedEntities();
        Assert.assertThat(entityProtectionInfoMap, Matchers.allOf(Matchers.notNullValue(), Matchers.not(Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap()))));
        Assert.assertThat(entityProtectionInfoMap.values(), Matchers.hasSize(5));
        Assert.assertThat(entityProtectionInfoMap,
                Matchers.allOf(
                        Matchers.hasKey("id11"),
                        Matchers.hasKey("id12"),
                        Matchers.hasKey("id13"),
                        Matchers.hasKey("id14"),
                        Matchers.hasKey("id15")
                )
        );
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EncapsulatedAssertionConfig() {
            @Override
            public String getId() {
                return "id11";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.ENCAPSULATED_ASSERTION, "id11")));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new ServerModuleFile() {
            @Override
            public String getId() {
                return "id11";
            }
        }));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.SERVER_MODULE_FILE, "id11")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new SecurePassword() {
            @Override
            public String getId() {
                return "id12";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.SECURE_PASSWORD, "id12")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new PublishedService() {
            @Override
            public String getId() {
                return "id13";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.SERVICE, "id13")));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new CustomKeyValueStore() {
            @Override
            public String getId() {
                return "id13";
            }
        }));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.CUSTOM_KEY_VALUE_STORE, "id13")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new Policy() {
            @Override
            public String getId() {
                return "id14";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.POLICY, "id14")));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new Folder() {
            @Override
            public String getId() {
                return "id14";
            }
        }));
        Assert.assertFalse(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.FOLDER, "id14")));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new Folder() {
            @Override
            public String getId() {
                return "id15";
            }
        }));
        Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(new EntityHeaderRef(EntityType.FOLDER, "id15")));
    }

    @Test
    public void testDoWithEntityProtectionDisabled() throws Exception {
        // make sure its enabled and there are no protected entities
        Assert.assertTrue(protectedEntityTracker.isEnabled());
        Assert.assertTrue(protectedEntityTracker.isEntityProtectionEnabled());
        Assert.assertThat(protectedEntityTracker.getProtectedEntities(), Matchers.anyOf(Matchers.nullValue(Map.class), Matchers.equalTo(Collections.<String, EntityProtectionInfo>emptyMap())));

        // add few test samples
        protectedEntityTracker.bulkUpdateReadOnlyEntitiesList(
                CollectionUtils.list(
                        Pair.pair(EntityType.SERVER_MODULE_FILE, "id1"),
                        Pair.pair(EntityType.SECURE_PASSWORD, "id2"),
                        Pair.pair(EntityType.CUSTOM_KEY_VALUE_STORE, "id3"),
                        Pair.pair(EntityType.SERVICE, "id4")
                )
        );

        // make sure our test samples are read-only
        final Collection<Entity> testEntities = Collections.unmodifiableCollection(
                CollectionUtils.<Entity>list(
                        new ServerModuleFile() {
                            @Override
                            public String getId() {
                                return "id1";
                            }
                        },
                        new SecurePassword() {
                            @Override
                            public String getId() {
                                return "id2";
                            }
                        },
                        new CustomKeyValueStore() {
                            @Override
                            public String getId() {
                                return "id3";
                            }
                        },
                        new PublishedService() {
                            @Override
                            public String getId() {
                                return "id4";
                            }
                        }
                )
        );
        // test with protection enabled
        Assert.assertThat(testEntities, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(4)));
        for (final Entity entity : testEntities) {
            Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(entity));
        }
        // now test with protection disabled
        protectedEntityTracker.doWithEntityProtectionDisabled(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Assert.assertFalse(protectedEntityTracker.isEntityProtectionEnabled());
                return null;
            }
        });

        // make sure our test samples are read-only
        final Collection<EntityHeaderRef> testEntityHeaders = Collections.unmodifiableCollection(
                CollectionUtils.list(
                        new EntityHeaderRef(EntityType.SERVER_MODULE_FILE, "id1"),
                        new EntityHeaderRef(EntityType.SECURE_PASSWORD, "id2"),
                        new EntityHeaderRef(EntityType.CUSTOM_KEY_VALUE_STORE, "id3"),
                        new EntityHeaderRef(EntityType.SERVICE, "id4")
                )
        );
        // test with protection enabled
        Assert.assertThat(testEntityHeaders, Matchers.allOf(Matchers.notNullValue(), Matchers.hasSize(4)));
        for (final EntityHeaderRef entityHeader : testEntityHeaders) {
            Assert.assertTrue(protectedEntityTracker.isReadOnlyEntity(entityHeader));
        }
        // now test with protection disabled
        protectedEntityTracker.doWithEntityProtectionDisabled(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                Assert.assertFalse(protectedEntityTracker.isEntityProtectionEnabled());
                return null;
            }
        });
    }
}