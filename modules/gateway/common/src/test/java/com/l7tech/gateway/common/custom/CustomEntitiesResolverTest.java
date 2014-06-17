package com.l7tech.gateway.common.custom;

import com.l7tech.gateway.common.security.password.SecurePassword;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.assertion.CustomAssertionHolder;
import com.l7tech.policy.assertion.ext.Category;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.entity.*;
import com.l7tech.policy.assertion.ext.store.KeyValueStore;
import com.l7tech.util.HexUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Test CustomEntitiesResolver
 */
@SuppressWarnings("serial")
@RunWith(MockitoJUnitRunner.class)
public class CustomEntitiesResolverTest extends CustomEntitiesTestBase {
    private CustomEntitiesResolver resolver;

    @Mock
    private KeyValueStore keyValueStore;

    @Before
    public void setUp() throws Exception {
        //assertionRegistry = new AssertionRegistry();

        resolver = Mockito.spy(new CustomEntitiesResolver(
                keyValueStore,
                new ClassNameToEntitySerializer() {
                    @Override
                    public CustomEntitySerializer getSerializer(final String className) {
                        if (className.startsWith(CustomEntitiesTestBase.class.getName() + "$")) {
                            return createEntitySerializer();
                        }
                        return null;
                    }
                }
        ));
    }

    @Test
    public void testSimpleDependencies() {
        // mock our KeyValueStore
        Mockito.doAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String key = (String) param1;
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                // KVS1
                // |_SP1
                // |_SP2
                // |_PK1
                // |_PK2
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                // Expected output of entities order:
                //   PK1, PK2, SP1, SP2, KVS1
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                if (keyValueStoreId(1).equals(key)) {
                    return createEntitySerializer().serialize(
                            new TestCustomAssertionEntity() {{
                                addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1 -- Attr: SecurePassword1
                                addEntityReference(securePasswordId(2), CustomEntityType.SecurePassword);    // SP2 -- Attr: SecurePassword2
                                addEntityReference(privateKeyId(1), CustomEntityType.PrivateKey);            // PK1 -- Attr: PrivateKey3
                                addEntityReference(privateKeyId(2), CustomEntityType.PrivateKey);            // PK2 -- Attr: PrivateKey4
                            }}
                    );
                }
                return null;
            }
        }).when(keyValueStore).get(Matchers.anyString());

        // get entities
        final EntityHeader[] entityHeaders = resolver.getEntitiesUsed(
                new CustomAssertionHolder() {{
                    setCategories(Category.ACCESS_CONTROL);
                    setDescriptionText("Test Custom Assertion");
                    setCustomAssertion(
                            new TestCustomAssertionWithEntities() {{
                                addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1
                                addEntityReference(privateKeyId(1), CustomEntityType.PrivateKey);            // PK1
                                addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1
                            }}
                    );
                }}
        );

        // expected entities PK1, PK2, SP1, SP2, KVS1
        assertEquals(5, entityHeaders.length);

        final Set<String> expectedEntities = new HashSet<>();
        expectedEntities.add(privateKeyId(1));
        expectedEntities.add(privateKeyId(2));
        expectedEntities.add(securePasswordId(1));
        expectedEntities.add(securePasswordId(2));
        expectedEntities.add(keyValueStoreId(1));

        for (final EntityHeader entityHeader : entityHeaders) {
            assertTrue(expectedEntities.remove(EntityType.CUSTOM_KEY_VALUE_STORE.equals(entityHeader.getType()) ? entityHeader.getName() : entityHeader.getStrId()));
        }
        assertTrue(expectedEntities.isEmpty());
    }

    @Test
    public void testCyclicDependencies() {
        // mock our KeyValueStore
        Mockito.doAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String key = (String) param1;

                /////////////////////////////////////////////////////////////////////////////////////////////////////
                // KVS1        KVS2          KVS3
                // |_SP1       |_SP2         |_SP3
                // |_KVS2      |_KVS3        |_KVS1
                // |_KVS3
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                // Expected output of entities order:
                //   SP1, SP2, SP3, KVS1, KVS2, KVS3
                /////////////////////////////////////////////////////////////////////////////////////////////////////
                if (keyValueStoreId(1).equals(key)) {
                    return createEntitySerializer().serialize(
                            new TestCustomAssertionEntity() {{
                                addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1 -- Attr: SecurePassword1
                                addEntityReference(keyValueStoreId(2), CustomEntityType.KeyValueStore);      // KVS2 -- Attr: KeyValueStore2
                                addEntityReference(keyValueStoreId(3), CustomEntityType.KeyValueStore);      // KVS3 -- Attr: KeyValueStore3
                            }}
                    );
                } else if (keyValueStoreId(2).equals(key)) {
                    return createEntitySerializer().serialize(
                            new TestCustomAssertionEntity() {{
                                addEntityReference(securePasswordId(2), CustomEntityType.SecurePassword);    // SP2 -- Attr: SecurePassword1
                                addEntityReference(keyValueStoreId(3), CustomEntityType.KeyValueStore);      // KVS3 -- Attr: KeyValueStore2
                            }}
                    );
                } else if (keyValueStoreId(3).equals(key)) {
                    return createEntitySerializer().serialize(
                            new TestCustomAssertionEntity() {{
                                addEntityReference(securePasswordId(3), CustomEntityType.SecurePassword);    // SP3 -- Attr: SecurePassword1
                                addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1 -- Attr: KeyValueStore2
                            }}
                    );
                }
                return null;
            }
        }).when(keyValueStore).get(Matchers.anyString());

        // get entities
        final EntityHeader[] entityHeaders = resolver.getEntitiesUsed(
                new CustomAssertionHolder() {{
                    setCategories(Category.ACCESS_CONTROL);
                    setDescriptionText("Test Custom Assertion");
                    setCustomAssertion(
                            new TestCustomAssertionWithEntities() {{
                                addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1
                            }}
                    );
                }}
        );
        assertNotNull(entityHeaders);

        // expected output: SP1, SP2, SP3, KVS1, KVS2, KVS3
        assertEquals(6, entityHeaders.length);

        final Set<String> expectedEntities = new HashSet<>();
        expectedEntities.add(securePasswordId(1));
        expectedEntities.add(securePasswordId(2));
        expectedEntities.add(securePasswordId(3));
        expectedEntities.add(keyValueStoreId(1));
        expectedEntities.add(keyValueStoreId(2));
        expectedEntities.add(keyValueStoreId(3));

        for (final EntityHeader entityHeader : entityHeaders) {
            assertTrue(expectedEntities.remove(EntityType.CUSTOM_KEY_VALUE_STORE.equals(entityHeader.getType()) ? entityHeader.getName() : entityHeader.getStrId()));
        }
        assertTrue(expectedEntities.isEmpty());
    }

    @Test
    public void testCustomEntityDescriptor() {
        class TestEntityWithCustomEntityDescriptor extends TestCustomAssertionEntity implements CustomEntityDescriptor {
            @Override public <R> R getProperty(String name, Class<R> rClass) { return null; }
            @Override public <R> R getUiObject(String uiName, Class<R> uiClass) { return null; }
        }

        Mockito.doAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String key = (String) param1;
                if (keyValueStoreId(1).equals(key)) {
                    return createEntitySerializer().serialize(new TestCustomAssertionEntity());
                } else if (keyValueStoreId(2).equals(key)) {
                    return createEntitySerializer().serialize(new TestEntityWithCustomEntityDescriptor()); // implements CustomEntityDescriptor
                }
                return null;
            }
        }).when(keyValueStore).get(Matchers.anyString());

        // get entities
        final EntityHeader[] entityHeaders = resolver.getEntitiesUsed(
                new CustomAssertionHolder() {{
                    setCategories(Category.ACCESS_CONTROL);
                    setDescriptionText("Test Custom Assertion");
                    setCustomAssertion(
                            new TestCustomAssertionWithEntities() {{
                                addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1
                                addEntityReference(keyValueStoreId(2), CustomEntityType.KeyValueStore);      // KVS2 - CustomEntityDescriptor
                            }}
                    );
                }}
        );
        assertNotNull(entityHeaders);
        assertEquals("There are exactly 2 entity headers", 2, entityHeaders.length);

        for (final EntityHeader entityHeader : entityHeaders) {
            assertEquals(EntityType.CUSTOM_KEY_VALUE_STORE, entityHeader.getType());
            assertTrue(entityHeader instanceof CustomKeyStoreEntityHeader);
            final CustomKeyStoreEntityHeader customKeyStoreEntityHeader = (CustomKeyStoreEntityHeader)entityHeader;
            // Entity id is either KVS1 or KVS2
            assertTrue(keyValueStoreId(1).equals(customKeyStoreEntityHeader.getName()) || keyValueStoreId(2).equals(customKeyStoreEntityHeader.getName()));
            assertEquals(KEY_VALUE_STORE_KEY_PREFIX, customKeyStoreEntityHeader.getEntityKeyPrefix());
            assertNotNull(customKeyStoreEntityHeader.getEntityBase64Value());
            assertTrue(Arrays.equals(HexUtils.decodeBase64(customKeyStoreEntityHeader.getEntityBase64Value()), keyValueStore.get(customKeyStoreEntityHeader.getName())));
            if (keyValueStoreId(1).equals(customKeyStoreEntityHeader.getName())) {
                assertNull(customKeyStoreEntityHeader.getEntitySerializer());
            } else {
                assertNotNull(customKeyStoreEntityHeader.getEntitySerializer());
                assertTrue(customKeyStoreEntityHeader.getEntitySerializer().startsWith(CustomEntitiesTestBase.class.getName() + "$"));
                assertTrue(createEntitySerializer().deserialize(HexUtils.decodeBase64(customKeyStoreEntityHeader.getEntityBase64Value())) instanceof TestEntityWithCustomEntityDescriptor);
            }
        }
    }

    @Test
    public void testCustomAssertionNotImplementingCustomReferenceEntities() {
        // get entities for custom assertion not implementing CustomReferenceEntities
        final EntityHeader[] entityHeaders = resolver.getEntitiesUsed(
                new CustomAssertionHolder() {{
                    setCategories(Category.ACCESS_CONTROL);
                    setDescriptionText("Test Custom Assertion");
                    setCustomAssertion(
                            new CustomAssertion() {
                                @Override public String getName() { return "Test Custom Assertions"; }
                                protected final CustomReferenceEntitiesSupport entitiesSupport = new CustomReferenceEntitiesSupport();
                                public void addEntityReference(
                                        @NotNull final String id,
                                        @NotNull final CustomEntityType type
                                ) {
                                    // append ordering
                                    final String attr = type.name() + String.valueOf(CustomEntityReferenceSupportAccessor.getAllReferencedEntities(entitiesSupport).size() + 1);
                                    addEntityReference(attr, id, type);
                                }
                                protected void addEntityReference(
                                        @NotNull final String attr,
                                        @NotNull final String id,
                                        @NotNull final CustomEntityType type
                                ) {
                                    assertTrue(attr.startsWith(type.name()));
                                    if (CustomEntityType.KeyValueStore.equals(type)) {
                                        entitiesSupport.setKeyValueStoreReference(
                                                attr,
                                                id,
                                                KEY_VALUE_STORE_KEY_PREFIX,
                                                createEntitySerializer()
                                        );
                                    } else {
                                        entitiesSupport.setReference(attr, id, type);
                                    }
                                }
                                {
                                    addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1
                                }}
                    );
                }}
        );
        // make sure no entities are exported
        assertEquals(0, entityHeaders.length);

        // make sure processEntityReference is not called
        Mockito.verify(resolver, Mockito.never()).processEntityReference(Mockito.anyCollectionOf(EntityHeader.class), Mockito.<CustomReferenceEntities>any());

        // make sure keyValueStore was not accessed
        Mockito.verify(keyValueStore, Mockito.never()).get(Mockito.anyString());
    }

    @Test
    public void testReplaceEntity() throws Exception {
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3
        // |_SP1       |_SP1         |_SP1
        // |_SP2       |_SP3         |_SP2
        // |_KVS2      |_KVS3        |_SP3
        // |_KVS3                    |_KVS1
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        // initial key-value-store entities
        /////////////////////////////////////////////////////////////////////////////////////////////////////
        final Map<String, byte[]> keyValueStoreEntityBytes = new LinkedHashMap<>();
        keyValueStoreEntityBytes.put(
                keyValueStoreId(1),     // KVS1
                createEntitySerializer().serialize(
                        new TestCustomAssertionEntity() {{
                            addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1 -- Attr: SecurePassword1
                            addEntityReference(securePasswordId(2), CustomEntityType.SecurePassword);    // SP2 -- Attr: SecurePassword2
                            addEntityReference(keyValueStoreId(2), CustomEntityType.KeyValueStore);      // KVS2 -- Attr: KeyValueStore3
                            addEntityReference(keyValueStoreId(3), CustomEntityType.KeyValueStore);      // KVS3 -- Attr: KeyValueStore3
                        }}
                )
        );
        keyValueStoreEntityBytes.put(
                keyValueStoreId(2),      // KVS2
                createEntitySerializer().serialize(
                        new TestCustomAssertionEntity() {{
                            addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1 -- Attr: SecurePassword1
                            addEntityReference(securePasswordId(3), CustomEntityType.SecurePassword);    // SP3 -- Attr: SecurePassword2
                            addEntityReference(keyValueStoreId(3), CustomEntityType.KeyValueStore);      // KVS3 -- Attr: KeyValueStore3
                        }}
                )
        );
        keyValueStoreEntityBytes.put(
                keyValueStoreId(3),     // KVS3
                createEntitySerializer().serialize(
                        new TestCustomAssertionEntity() {{
                            addEntityReference(securePasswordId(1), CustomEntityType.SecurePassword);    // SP1 -- Attr: SecurePassword1
                            addEntityReference(securePasswordId(2), CustomEntityType.SecurePassword);    // SP2 -- Attr: SecurePassword2
                            addEntityReference(securePasswordId(3), CustomEntityType.SecurePassword);    // SP3 -- Attr: SecurePassword3
                            addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1 -- Attr: KeyValueStore4
                        }}
                )
        );

        // mock our KeyValueStore.get
        Mockito.doAnswer(new Answer<byte[]>() {
            @Override
            public byte[] answer(final InvocationOnMock invocation) throws Throwable {
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String key = (String) param1;
                return keyValueStoreEntityBytes.get(key);
            }
        }).when(keyValueStore).get(Matchers.anyString());
        // mock our KeyValueStore.saveOrUpdate, which should be called from replaceEntity
        Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(final InvocationOnMock invocation) throws Throwable {
                assertEquals(2, invocation.getArguments().length);
                final Object param1 = invocation.getArguments()[0];
                assertTrue("First Param is String", param1 instanceof String);
                final String key = (String) param1;
                final Object param2 = invocation.getArguments()[1];
                assertTrue("Second Param is byte[]", param2 instanceof byte[]);
                final byte[] bytes = (byte[]) param2;
                if (keyValueStoreEntityBytes.containsKey(key)) {
                    keyValueStoreEntityBytes.put(key, bytes);
                } else {
                    fail("Unknown key-value-store-id: \"" + key + "\"");
                }
                return null;
            }
        }).when(keyValueStore).saveOrUpdate(Matchers.anyString(), Matchers.<byte[]>any());

        // get entities
        final CustomAssertionHolder customAssertionHolder = new CustomAssertionHolder() {{
            setCategories(Category.ACCESS_CONTROL);
            setDescriptionText("Test Custom Assertion");
            setCustomAssertion(
                    new TestCustomAssertionWithEntities() {{
                        addEntityReference(keyValueStoreId(1), CustomEntityType.KeyValueStore);      // KVS1
                    }}
            );
        }};
        final EntityHeader[] entityHeaders = resolver.getEntitiesUsed(customAssertionHolder);

        // verify entity headers
        verifyEntityHeaders(
                entityHeaders,
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(1));   // SP1
                    add(securePasswordId(2));   // SP2
                    add(securePasswordId(3));   // SP3
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(3));    // KVS3
                }}
        );


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3
        // |_SP1       |_SP1         |_SP1
        // |_SP2       |_SP3         |_SP2
        // |_KVS2      |_KVS3        |_SP3
        // |_KVS3                    |_KVS1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace SP1 with SP4
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3
        // |_SP4*      |_SP4*        |_SP4*
        // |_SP2       |_SP3         |_SP2
        // |_KVS2      |_KVS3        |_SP3
        // |_KVS3                    |_KVS1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new SecurePasswordEntityHeader(Goid.parseGoid(securePasswordId(1)), EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name()),
                new SecurePasswordEntityHeader(Goid.parseGoid(securePasswordId(4)), EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name()),
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        true, // KVS1 changed, since it contains SP1
                        true, // KVS2 changed, since it contains SP1
                        true, // KVS3 changed, since it contains SP1
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(2));   // SP2
                    add(securePasswordId(3));   // SP3
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(3));    // KVS3
                }}
        );


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3
        // |_SP4       |_SP4         |_SP4
        // |_SP2       |_SP3         |_SP2
        // |_KVS2      |_KVS3        |_SP3
        // |_KVS3                    |_KVS1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace SP3 with SP5
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3
        // |_SP4       |_SP4         |_SP4
        // |_SP2       |_SP5*        |_SP2
        // |_KVS2      |_KVS3        |_SP5*
        // |_KVS3                    |_KVS1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new SecurePasswordEntityHeader(Goid.parseGoid(securePasswordId(3)), EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name()),
                new SecurePasswordEntityHeader(Goid.parseGoid(securePasswordId(5)), EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name()),
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        false,  // KVS1 not changed
                        true,   // KVS2 changed, since it contains SP3
                        true,   // KVS3 changed, since it contains SP3
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(2));   // SP2
                    add(securePasswordId(5));   // SP5
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(3));    // KVS3
                }}
        );


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3
        // |_SP4       |_SP4         |_SP4
        // |_SP2       |_SP5         |_SP2
        // |_KVS2      |_KVS3        |_SP5
        // |_KVS3                    |_KVS1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace SP2 with SP6
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3
        // |_SP4       |_SP4         |_SP4
        // |_SP6*      |_SP5         |_SP6*
        // |_KVS2      |_KVS3        |_SP5
        // |_KVS3                    |_KVS1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new SecurePasswordEntityHeader(Goid.parseGoid(securePasswordId(2)), EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name()),
                new SecurePasswordEntityHeader(Goid.parseGoid(securePasswordId(6)), EntityType.SECURE_PASSWORD, null, null, SecurePassword.SecurePasswordType.PASSWORD.name()),
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        true,   // KVS1 changed, since it contains SP2
                        false,  // KVS2 not changed
                        true,   // KVS3 changed, since it contains SP3
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(6));   // SP6
                    add(securePasswordId(5));   // SP5
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(3));    // KVS3
                }}
        );


        // add KVS4
        keyValueStoreEntityBytes.put(
                keyValueStoreId(4),          // KVS4
                createEntitySerializer().serialize(
                        new TestCustomAssertionEntity() {{
                            addEntityReference(privateKeyId(1), CustomEntityType.PrivateKey);    // PK1 -- Attr: PrivateKey1
                        }}
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS3          KVS4
        // |_SP4       |_SP4         |_SP4         |_PK1
        // |_SP6       |_SP5         |_SP6
        // |_KVS2      |_KVS3        |_SP5
        // |_KVS3                    |_KVS1
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace KVS3 with KVS4
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2          KVS4
        // |_SP4       |_SP4         |_PK1
        // |_SP6       |_SP5
        // |_KVS2      |_KVS4*
        // |_KVS4*
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new CustomKeyStoreEntityHeader(keyValueStoreId(3), KEY_VALUE_STORE_KEY_PREFIX, null, null), // old
                new CustomKeyStoreEntityHeader(keyValueStoreId(4), KEY_VALUE_STORE_KEY_PREFIX, null, null), // new
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        true,   // KVS1 changed, since it contains KVS3
                        true,   // KVS2 changed, since it contains KVS3
                        false,  // KVS3 not changed
                        false,  // KVS4 not changed
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(6));   // SP6
                    add(securePasswordId(5));   // SP5
                    add(privateKeyId(1));       // PK1
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(4));    // KVS4
                }}
        );


        // add KVS5
        keyValueStoreEntityBytes.put(
                keyValueStoreId(5),          // KVS5
                createEntitySerializer().serialize(
                        new TestCustomAssertionEntity() {{
                            addEntityReference(privateKeyId(2), CustomEntityType.PrivateKey);    // PK2 -- Attr: PrivateKey1
                        }}
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS2         KVS4        KVS5
        // |_SP4       |_SP4        |_PK1       |_PK2
        // |_SP6       |_SP5
        // |_KVS2      |_KVS4
        // |_KVS4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace KVS2 with KVS5
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS5
        // |_SP4       |_PK1       |_PK2
        // |_SP6
        // |_KVS5*
        // |_KVS4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new CustomKeyStoreEntityHeader(keyValueStoreId(2), KEY_VALUE_STORE_KEY_PREFIX, null, null), // old
                new CustomKeyStoreEntityHeader(keyValueStoreId(5), KEY_VALUE_STORE_KEY_PREFIX, null, null), // new
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        true,   // KVS1 changed, since it contains KVS2
                        false,  // KVS2 not changed
                        false,  // KVS3 not changed
                        false,  // KVS4 not changed
                        false,  // KVS5 not changed
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(6));   // SP6
                    add(privateKeyId(1));       // PK1
                    add(privateKeyId(2));       // PK2
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(4));    // KVS4
                    add(keyValueStoreId(5));    // KVS5
                }}
        );


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS5
        // |_SP4       |_PK1       |_PK2
        // |_SP6
        // |_KVS5
        // |_KVS4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace KVS5 with KVS2
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2
        // |_SP4       |_PK1       |_SP4
        // |_SP6                   |_SP5
        // |_KVS2*                 |_KVS4
        // |_KVS4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new CustomKeyStoreEntityHeader(keyValueStoreId(5), KEY_VALUE_STORE_KEY_PREFIX, null, null), // old
                new CustomKeyStoreEntityHeader(keyValueStoreId(2), KEY_VALUE_STORE_KEY_PREFIX, null, null), // new
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        true,   // KVS1 changed, since it contains KVS2
                        false,  // KVS2 not changed
                        false,  // KVS3 not changed
                        false,  // KVS4 not changed
                        false,  // KVS5 not changed
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(6));   // SP6
                    add(securePasswordId(5));   // SP5
                    add(privateKeyId(1));       // PK1
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(4));    // KVS4
                }}
        );


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2
        // |_SP4       |_PK1       |_SP4
        // |_SP6                   |_SP5
        // |_KVS2                  |_KVS4
        // |_KVS4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace KVS5 with KVS2
        // After replacement (no changes):
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2
        // |_SP4       |_PK1       |_SP4
        // |_SP6                   |_SP5
        // |_KVS2                  |_KVS4
        // |_KVS4
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new CustomKeyStoreEntityHeader(keyValueStoreId(5), KEY_VALUE_STORE_KEY_PREFIX, null, null), // old
                new CustomKeyStoreEntityHeader(keyValueStoreId(2), KEY_VALUE_STORE_KEY_PREFIX, null, null), // new
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        false,  // KVS1 not changed
                        false,  // KVS2 not changed
                        false,  // KVS3 not changed
                        false,  // KVS4 not changed
                        false,  // KVS5 not changed
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(6));   // SP6
                    add(securePasswordId(5));   // SP5
                    add(privateKeyId(1));       // PK1
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(4));    // KVS4
                }}
        );


        // add KVS6
        keyValueStoreEntityBytes.put(
                keyValueStoreId(6),          // KVS6
                createEntitySerializer().serialize(
                        new TestCustomAssertionEntity() {{
                            addEntityReference(keyValueStoreId(3), CustomEntityType.KeyValueStore);     // KVS3 -- Attr: KeyValueStore1
                            addEntityReference(privateKeyId(3), CustomEntityType.PrivateKey);           // PK3 -- Attr: PrivateKey2
                            addEntityReference(keyValueStoreId(4), CustomEntityType.KeyValueStore);     // KVS4 -- Attr: KeyValueStore3
                            addEntityReference(keyValueStoreId(2), CustomEntityType.KeyValueStore);     // KVS2 -- Attr: KeyValueStore4
                            addEntityReference(keyValueStoreId(6), CustomEntityType.KeyValueStore);     // KVS6 -- Attr: KeyValueStore5
                        }}
                )
        );
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2          KVS6
        // |_SP4       |_PK1       |_SP4         |_KVS3
        // |_SP6                   |_SP5         |_PK3
        // |_KVS2                  |_KVS4        |_KVS4
        // |_KVS4                                |_KVS2
        //                                       |_KVS6
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace KVS4 with KVS6
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2          KVS6          KVS3
        // |_SP4       |_PK1       |_SP4         |_KVS3        |_SP4
        // |_SP6                   |_SP5         |_PK3         |_SP6
        // |_KVS2                  |_KVS6*       |_KVS4        |_SP5
        // |_KVS6*                               |_KVS2        |_KVS1
        //                                       |_KVS6
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new CustomKeyStoreEntityHeader(keyValueStoreId(4), KEY_VALUE_STORE_KEY_PREFIX, null, null), // old
                new CustomKeyStoreEntityHeader(keyValueStoreId(6), KEY_VALUE_STORE_KEY_PREFIX, null, null), // new
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        true,   // KVS1 changed, since it contains KVS4
                        true,   // KVS2 changed, since it contains KVS4
                        false,  // KVS3 not changed
                        false,  // KVS4 not changed
                        false,  // KVS5 not changed
                        false,  // KVS6 not changed
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(5));   // SP5
                    add(securePasswordId(6));   // SP6
                    add(privateKeyId(1));       // PK1
                    add(privateKeyId(3));       // PK3
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(3));    // KVS3
                    add(keyValueStoreId(4));    // KVS4
                    add(keyValueStoreId(6));    // KVS6
                }}
        );


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2          KVS6          KVS3
        // |_SP4       |_PK1       |_SP4         |_KVS3        |_SP4
        // |_SP6                   |_SP5         |_PK3         |_SP6
        // |_KVS2                  |_KVS6        |_KVS4        |_SP5
        // |_KVS6                                |_KVS2        |_KVS1
        //                                       |_KVS6
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace PK3 with PK2
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2          KVS6          KVS3
        // |_SP4       |_PK1       |_SP4         |_KVS3        |_SP4
        // |_SP6                   |_SP5         |_PK2*        |_SP6
        // |_KVS2                  |_KVS6        |_KVS4        |_SP5
        // |_KVS6                                |_KVS2        |_KVS1
        //                                       |_KVS6
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new SsgKeyHeader(privateKeyId(3), getPrivateKeyGoidAliasPair(privateKeyId(3)).left, getPrivateKeyGoidAliasPair(privateKeyId(3)).right, null), // old
                new SsgKeyHeader(privateKeyId(2), getPrivateKeyGoidAliasPair(privateKeyId(2)).left, getPrivateKeyGoidAliasPair(privateKeyId(2)).right, null), // new
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        false,  // KVS1 not changed
                        false,  // KVS2 not changed
                        false,  // KVS3 not changed
                        false,  // KVS4 not changed
                        false,  // KVS5 not changed
                        true,   // KVS6 changed, since it contains PK3
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(5));   // SP5
                    add(securePasswordId(6));   // SP6
                    add(privateKeyId(1));       // PK1
                    add(privateKeyId(2));       // PK2
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(2));    // KVS2
                    add(keyValueStoreId(3));    // KVS3
                    add(keyValueStoreId(4));    // KVS4
                    add(keyValueStoreId(6));    // KVS6
                }}
        );


        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS2          KVS6          KVS3
        // |_SP4       |_PK1       |_SP4         |_KVS3        |_SP4
        // |_SP6                   |_SP5         |_PK2         |_SP6
        // |_KVS2                  |_KVS6        |_KVS4        |_SP5
        // |_KVS6                                |_KVS2        |_KVS1
        //                                       |_KVS6
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // Replace KVS2 with KVS6
        // After replacement:
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        // KVS1        KVS4        KVS6          KVS3
        // |_SP4       |_PK1       |_KVS3        |_SP4
        // |_SP6                   |_PK2         |_SP6
        // |_KVS6*                 |_KVS4        |_SP5
        // |_KVS6                  |_KVS6*       |_KVS1
        //                         |_KVS6
        ////////////////////////////////////////////////////////////////////////////////////////////////////////////////
        verifyReplaceEntity(
                new CustomKeyStoreEntityHeader(keyValueStoreId(2), KEY_VALUE_STORE_KEY_PREFIX, null, null), // old
                new CustomKeyStoreEntityHeader(keyValueStoreId(6), KEY_VALUE_STORE_KEY_PREFIX, null, null), // new
                customAssertionHolder,
                Collections.unmodifiableCollection(keyValueStoreEntityBytes.values()),
                new boolean[] {
                        true,   // KVS1 changed, since it contains KVS2
                        false,  // KVS2 not changed
                        false,  // KVS3 not changed
                        false,  // KVS4 not changed
                        false,  // KVS5 not changed
                        true,   // KVS6 changed, since it contains KVS2
                },
                new HashSet<String>() {{        // expected output:
                    add(securePasswordId(4));   // SP4
                    add(securePasswordId(5));   // SP5
                    add(securePasswordId(6));   // SP6
                    add(privateKeyId(1));       // PK1
                    add(privateKeyId(2));       // PK2
                    add(keyValueStoreId(1));    // KVS1
                    add(keyValueStoreId(3));    // KVS3
                    add(keyValueStoreId(4));    // KVS4
                    add(keyValueStoreId(6));    // KVS6
                }}
        );
    }

    private void verifyReplaceEntity(
            @NotNull final EntityHeader oldEntity,
            @NotNull final EntityHeader newEntity,
            @NotNull final CustomAssertionHolder customAssertionHolder,
            @NotNull final Collection<byte[]> keyValueStoreBytes,
            @NotNull final boolean[] expectedKeyValueStoreBytesChanges,
            @NotNull final Set<String> expectedEntities
    ) throws Exception {
        // copy the existing key-value-store bytes, before replacing entities
        final byte[][] keyValueStoreBytesBefore = toArray(keyValueStoreBytes);
        final byte[][] copyKeyValueStoreEntityBytes = deepCopy(keyValueStoreBytesBefore);
        assertTrue(deepEquals(copyKeyValueStoreEntityBytes, keyValueStoreBytesBefore)); // same values

        // replace entities
        resolver.replaceEntity(oldEntity, newEntity, customAssertionHolder);

        // make sure all entities have been replaced
        final byte[][] keyValueStoreBytesAfter = toArray(keyValueStoreBytes);
        assertEquals(keyValueStoreBytesAfter.length, expectedKeyValueStoreBytesChanges.length);
        for (int i = 0; i < keyValueStoreBytesAfter.length; ++i) {
            assertTrue(Arrays.equals(copyKeyValueStoreEntityBytes[i], keyValueStoreBytesAfter[i]) != expectedKeyValueStoreBytesChanges[i]);
        }

        // get entity headers
        final EntityHeader[] entityHeaders = resolver.getEntitiesUsed(customAssertionHolder);

        // verify entity headers
        verifyEntityHeaders(entityHeaders, expectedEntities);
    }

    private void verifyEntityHeaders(
            @NotNull final EntityHeader[] entityHeaders,
            @NotNull final Set<String> expectedEntities
    ) throws Exception {
        // verify that correct entities are exported
        assertNotNull(entityHeaders);
        assertEquals(expectedEntities.size(), entityHeaders.length);
        for (final EntityHeader entityHeader : entityHeaders) {
            assertTrue(expectedEntities.remove(EntityType.CUSTOM_KEY_VALUE_STORE.equals(entityHeader.getType()) ? entityHeader.getName() : entityHeader.getStrId()));
        }
        assertTrue(expectedEntities.isEmpty());
    }

    @NotNull
    private static byte[][] deepCopy(@NotNull final byte[][] source) {
        final byte[][] result = new byte[source.length][];
        for (int i = 0; i < source.length; ++i) {
            result[i] = Arrays.copyOf(source[i], source[i].length);
        }
        return result;
    }

    private static boolean deepEquals(@NotNull final byte[][] first, @NotNull final byte[][] second) {
        if (first.length != second.length) { return false; }
        for (int i = 0 ; i < first.length; ++i) {
            if (!Arrays.equals(first[i], second[i])) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    private static byte[][] toArray(@NotNull final Collection<byte[]> source) {
        return source.toArray(new byte[source.size()][]);
    }
}
