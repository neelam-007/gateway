package com.l7tech.gateway.common.custom;

import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.ext.CustomAssertion;
import com.l7tech.policy.assertion.ext.entity.*;
import com.l7tech.util.ExceptionUtils;
import com.l7tech.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Base test class for sett
 */
public abstract class CustomEntitiesTestBase {
    /**
     * Our sample key-value-store key-prefix
     */
    protected static String KEY_VALUE_STORE_KEY_PREFIX = "test-prefix";

    /**
     * Our sample {@code CustomEntitySerializer} uses {@code Properties} to store entity values directly from
     * {@link TestCustomAssertionEntity#entitiesSupport} object.<br/>
     * This represents the property-name of the object class-name needed when the object is deserialized.
     * This property should be unique and should not interfere with any existing object properties.
     */
    @SuppressWarnings("SpellCheckingInspection")
    protected static String PROPERTY_ENTITY_CLASS_NAME = "__PROPERTY_ENTITY_CLASS_NAME__261B8027_2487_4854_A0C5_0D549003CBEF__";

    /**
     * Sample CustomAssertion Entity implementing {@link CustomReferenceEntities}.
     * <p/>
     * <b>Constraint</b>: All implementations must be stateless and have a constructor with no params.
     */
    public static class TestCustomAssertionEntity implements CustomReferenceEntities, Serializable {
        private static final long serialVersionUID = -212225058786287673L;
        @NotNull
        protected final CustomReferenceEntitiesSupport entitiesSupport = new CustomReferenceEntitiesSupport();

        /**
         * Add a new dependency with specified entity.<br/>
         * Attribute will be pre-calculated here using entity type.
         *
         * @param id      dependent entity Id
         * @param type    dependent entity Type.
         */
        public void addEntityReference(
                @NotNull final String id,
                @NotNull final CustomEntityType type
        ) {
            // append ordering
            final String attr = type.name() + String.valueOf(CustomEntityReferenceSupportAccessor.getAllReferencedEntities(entitiesSupport).size() + 1);
            addEntityReference(attr, id, type);
        }

        /**
         * Internal helper function for adding a new dependency with specified attribute and entity.
         */
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

        @NotNull
        @Override
        public CustomReferenceEntitiesSupport getReferenceEntitiesSupport() {
            return entitiesSupport;
        }

        /**
         * Compares two {@link CustomReferenceEntitiesSupport.ReferenceElement} objects.
         *
         * @param entity1    first {@link CustomReferenceEntitiesSupport.ReferenceElement} object
         * @param entity2    second {@link CustomReferenceEntitiesSupport.ReferenceElement} object
         * @return {@code true} if objects are equals, {@code false} otherwise
         */
        private boolean areEntitiesEqual(@Nullable final Object entity1, @Nullable final Object entity2) {
            return (
                    areValuesEqual(
                            CustomEntityReferenceSupportAccessor.getEntityId(entity1),
                            CustomEntityReferenceSupportAccessor.getEntityId(entity2)
                    ) &&
                    areValuesEqual(
                            CustomEntityReferenceSupportAccessor.getEntityKeyPrefix(entity1),
                            CustomEntityReferenceSupportAccessor.getEntityKeyPrefix(entity2)
                    ) &&
                    areValuesEqual(
                            CustomEntityReferenceSupportAccessor.getEntityType(entity1),
                            CustomEntityReferenceSupportAccessor.getEntityType(entity2)
                    ) &&
                    areValuesEqual(
                            CustomEntityReferenceSupportAccessor.getSerializerClassName(entity1),
                            CustomEntityReferenceSupportAccessor.getSerializerClassName(entity2)
                    )
            );
        }

        /**
         * Compare two arbitrary values.
         */
        private <T> boolean areValuesEqual(@Nullable final T value1, @Nullable final T value2) {
            return (value1 != null) ? value1.equals(value2) : value2 == null;
        }

        @Override
        public boolean equals(@Nullable final Object o) {
            if (this == o) return true;
            if (o == null || !(o instanceof TestCustomAssertionEntity)) return false;

            final TestCustomAssertionEntity that = (TestCustomAssertionEntity) o;

            final Map<String,?> entities = CustomEntityReferenceSupportAccessor.getAllReferencedEntitiesMap(entitiesSupport);
            final Map<String,?> thatEntities = CustomEntityReferenceSupportAccessor.getAllReferencedEntitiesMap(that.entitiesSupport);

            if (entities == thatEntities)
                return true;
            if (thatEntities.size() != entities.size())
                return false;

            for (final Map.Entry entry : entities.entrySet()) {
                final String key = (String) entry.getKey();
                final Object value = entry.getValue();
                if (value == null) {
                    if (!(thatEntities.get(key) == null && thatEntities.containsKey(key))) {
                        return false;
                    }
                } else if (!areEntitiesEqual(value, thatEntities.get(key))) {
                    return false;
                }
            }

            return true;
        }

        @Override
        public int hashCode() {
            return CustomEntityReferenceSupportAccessor.getAllReferencedEntitiesMap(entitiesSupport).hashCode();
        }
    }

    /**
     * Sample CustomAssertion with entity dependencies.
     */
    public static class TestCustomAssertionWithEntities extends TestCustomAssertionEntity implements CustomAssertion {
        private static final long serialVersionUID = 5549578152881150130L;
        @NotNull
        @Override
        public String getName() { return "Test Custom Assertion referencing entities"; }
    }

    /**
     * Create our sample {@link CustomEntitySerializer}.<br/>
     * This implementation uses {@code Properties} to store entity dependencies directly from
     * {@link TestCustomAssertionEntity#entitiesSupport} object.
     * <p/>
     * It uses {@link #PROPERTY_ENTITY_CLASS_NAME} to store the object class-name along with the rest of the properties.
     * So make sure you are not using {@link #PROPERTY_ENTITY_CLASS_NAME} value as attribute for other dependent entities.
     *
     * @return a new entity serializer object.
     */
    @NotNull
    protected static CustomEntitySerializer<TestCustomAssertionEntity> createEntitySerializer() {
        return new CustomEntitySerializer<TestCustomAssertionEntity>() {
            private static final long serialVersionUID = 1419943678579911879L;

            @Nullable
            @Override
            public byte[] serialize(@Nullable final TestCustomAssertionEntity entity) {
                if (entity == null) {
                    return null;
                }

                // add all referenced entities as properties
                final Properties prop = new Properties();
                prop.setProperty(PROPERTY_ENTITY_CLASS_NAME, entity.getClass().getName());
                for (final Map.Entry entryObj : CustomEntityReferenceSupportAccessor.getAllReferencedEntitiesMap(entity.entitiesSupport).entrySet()) {
                    prop.setProperty((String) (entryObj.getKey()), CustomEntityReferenceSupportAccessor.getEntityId(entryObj.getValue()));
                }

                try (final ByteArrayOutputStream out = new ByteArrayOutputStream(1024)) {
                    prop.storeToXML(out, entity.getClass().getName());
                    return out.toByteArray();
                } catch (IOException e) {
                    fail("Error while writing custom entity:\n" + ExceptionUtils.getMessage(e));
                    return null;
                }
            }

            @Nullable
            @Override
            public TestCustomAssertionEntity deserialize(@Nullable final byte[] bytes) {
                if (bytes == null) {
                    return null;
                }

                final Properties prop = new Properties();
                try (final ByteArrayInputStream in = new ByteArrayInputStream(bytes)) {
                    prop.loadFromXML(in);
                    final TestCustomAssertionEntity entity = createEntity((String) prop.get(PROPERTY_ENTITY_CLASS_NAME));
                    for (final Map.Entry propEntry : prop.entrySet()) {
                        final String key = (String) propEntry.getKey();
                        if (!PROPERTY_ENTITY_CLASS_NAME.equals(key)) {
                            entity.addEntityReference(key, prop.getProperty(key), typeFromPropKey(key));
                        }
                    }
                    return entity;
                } catch (IOException e) {
                    fail("Error while reading custom entity:\n" + ExceptionUtils.getMessage(e));
                    return null;
                }
            }

            /**
             * Helper method for creating new entity object instance from serialized class-name.
             *
             * @param className    Entity class-name
             */
            @NotNull
            private TestCustomAssertionEntity createEntity(@NotNull final String className) {
                try {
                    //noinspection unchecked
                    return createNewInstance((Class<? extends TestCustomAssertionEntity>)Class.forName(className));
                } catch (ClassNotFoundException e) {
                    fail(e.getMessage());
                    return null;
                }
            }

            /**
             * Helper method for creating new entity object instance from serialized {@code Class} object.
             *
             * @param theClass    Entity {@code Class} object
             */
            @NotNull
            private <T> T createNewInstance(@NotNull final Class<T> theClass) {
                try {
                    if (theClass.isAnonymousClass()) {
                        //noinspection unchecked
                        return (T)createNewInstance(theClass.getSuperclass());
                    } else if (theClass.isLocalClass() || theClass.isMemberClass()) {
                        //noinspection unchecked
                        final Constructor<T>[] constructors = (Constructor<T>[])theClass.getDeclaredConstructors();
                        assertSame("Only one constructor supported for TestCustomAssertionEntity objects", constructors.length, 1);
                        final Constructor<T> theConstructor = constructors[0];
                        theConstructor.setAccessible(true);
                        final Class<?>[] params = theConstructor.getParameterTypes();
                        if (params.length == 0) {
                            return theConstructor.newInstance();
                        } else {
                            assertSame("There should only be one ", 1, params.length);
                            return theConstructor.newInstance(createNewInstance(params[0]));
                        }
                    }
                    return theClass.newInstance();
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    fail("Error while instantiate serialized entity object:\n" + ExceptionUtils.getMessage(e));
                    return null;
                }
            }

            /**
             * Helper function for extracting entity type from entity attribute.
             */
            @NotNull
            private CustomEntityType typeFromPropKey(@NotNull final String attr) {
                for (final CustomEntityType type : CustomEntityType.values()) {
                    if (attr.startsWith(type.name())) {
                        return type;
                    }
                }
                fail(String.format("Unsupported attribute value %1$s", attr));
                return null;
            }
        };
    }

    /**
     * Generates sample {@code SecurePassword} Id.<br/>
     * Generate a {@link Goid} having {@code high} value set to {@code 1} (which indicates {@code SecurePassword}),
     * and {@code low} value set to {@code index}.
     */
    @NotNull
    protected static String securePasswordId(final int index) {
        return new Goid(1, index).toString();
    }

    /**
     * Generates sample {@code PrivateKey} Id.<br/>
     * Generate a {@link Goid} having {@code high} value set to {@code 2} (which indicates {@code PrivateKey}),
     * and {@code low} value set to {@code index}.
     */
    @NotNull
    protected static String privateKeyId(final int index) {
        return new Goid(2, index).toString() + ":alias" + index;
    }

    /**
     * Generates sample {@code CustomKeyValueStore} Id.<br/>
     * Generate a {@link Goid} having {@code high} value set to {@code 3} (which indicates {@code CustomKeyValueStore}),
     * and {@code low} value set to {@code index}.
     */
    @NotNull
    protected static String keyValueStoreId(final int index) {
        return new Goid(3, index).toString();
    }

    /**
     * Generates sample policy fragment goid.<br/>
     * Generate a {@link Goid} having {@code high} value set to {@code 4} (which indicates {@code PolicyFragment}),
     * and {@code low} value set to {@code index}.
     */
    @NotNull
    protected static String policyFragmentId(final int index) {
        return new Goid(4, index).toString();
    }

    /**
     * Extract the alias and goid from the specified {@code privateKeyId}
     *
     * @param privateKeyId    private-key-id containing both key-alias and key-id.
     * @return a {@link Pair Pair&lt;Goid, String&gt;} containing the {@code privateKeyId}. Never {@code null}
     * @throws IllegalArgumentException when the specified {@code privateKeyId} is not properly formatted.
     */
    @NotNull
    protected static Pair<Goid, String> getPrivateKeyGoidAliasPair(@NotNull final String privateKeyId) {
        // Add none default key only.
        final String[] keyIdSplit = privateKeyId.split(":");
        if (keyIdSplit.length != 2) {
            throw new IllegalArgumentException("Invalid key ID format.");
        }
        return Pair.pair(Goid.parseGoid(keyIdSplit[0]), keyIdSplit[1]);
    }
}
