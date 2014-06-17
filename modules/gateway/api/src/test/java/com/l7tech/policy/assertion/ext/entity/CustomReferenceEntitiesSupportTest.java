package com.l7tech.policy.assertion.ext.entity;

import org.junit.Test;

import java.lang.reflect.Constructor;

import static junit.framework.Assert.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Test CustomReferenceEntitiesSupport
 */
@SuppressWarnings("serial")
public class CustomReferenceEntitiesSupportTest {

    @Test
    public void testAddRemoveReferences() throws Exception {
        final CustomReferenceEntitiesSupport entitiesSupport = new CustomReferenceEntitiesSupport();
        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
        assertSame(1, entitiesSupport.references.size());
        assertEquals(entitiesSupport.getReference("attr1"), "id1");
        assertNull(entitiesSupport.getReference("attr_not_existing"));

        entitiesSupport.setReference("attr1", "id2", CustomEntityType.SecurePassword);
        assertSame(1, entitiesSupport.references.size());
        assertEquals(entitiesSupport.getReference("attr1"), "id2");

        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
        assertSame(2, entitiesSupport.references.size());
        assertEquals(entitiesSupport.getReference("attr1"), "id2");
        assertEquals(entitiesSupport.getReference("attr2"), "id2");

        entitiesSupport.setKeyValueStoreReference(
                "attr3",
                "key1",
                "prefix1",
                new CustomEntitySerializer() {
                    public byte[] serialize(Object entity) { return new byte[0]; }
                    public Object deserialize(byte[] bytes) { return null; }
                }
        );
        assertSame(3, entitiesSupport.references.size());
        assertEquals(entitiesSupport.getReference("attr1"), "id2");
        assertEquals(entitiesSupport.getReference("attr2"), "id2");
        assertEquals(entitiesSupport.getReference("attr3"), "key1");

        entitiesSupport.setKeyValueStoreReference(
                "attr4",
                "key2",
                "prefix2",
                null
        );
        assertSame(4, entitiesSupport.references.size());
        assertEquals(entitiesSupport.getReference("attr1"), "id2");
        assertEquals(entitiesSupport.getReference("attr2"), "id2");
        assertEquals(entitiesSupport.getReference("attr3"), "key1");
        assertEquals(entitiesSupport.getReference("attr4"), "key2");

        assertTrue(entitiesSupport.removeReference("attr1"));
        assertSame(3, entitiesSupport.references.size());
        assertNull(entitiesSupport.getReference("attr1"));
        assertEquals(entitiesSupport.getReference("attr2"), "id2");
        assertEquals(entitiesSupport.getReference("attr3"), "key1");
        assertEquals(entitiesSupport.getReference("attr4"), "key2");

        assertFalse(entitiesSupport.removeReference("attr_not_existing"));
        assertSame(3, entitiesSupport.references.size());
        assertNull(entitiesSupport.getReference("attr1"));
        assertEquals(entitiesSupport.getReference("attr2"), "id2");
        assertEquals(entitiesSupport.getReference("attr3"), "key1");
        assertEquals(entitiesSupport.getReference("attr4"), "key2");

        assertTrue(entitiesSupport.removeReference("attr2"));
        assertSame(2, entitiesSupport.references.size());
        assertNull(entitiesSupport.getReference("attr1"));
        assertNull(entitiesSupport.getReference("attr2"));
        assertEquals(entitiesSupport.getReference("attr3"), "key1");
        assertEquals(entitiesSupport.getReference("attr4"), "key2");

        assertTrue(entitiesSupport.removeReference("attr3"));
        assertSame(1, entitiesSupport.references.size());
        assertNull(entitiesSupport.getReference("attr1"));
        assertNull(entitiesSupport.getReference("attr2"));
        assertNull(entitiesSupport.getReference("attr3"));
        assertEquals(entitiesSupport.getReference("attr4"), "key2");

        assertTrue(entitiesSupport.removeReference("attr4"));
        assertSame(0, entitiesSupport.references.size());
        assertNull(entitiesSupport.getReference("attr1"));
        assertNull(entitiesSupport.getReference("attr2"));
        assertNull(entitiesSupport.getReference("attr3"));
        assertNull(entitiesSupport.getReference("attr4"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferencesSupportSetReferenceAttributeIsMandatory() throws Exception {
        new CustomReferenceEntitiesSupport().setReference(null, "id1", CustomEntityType.SecurePassword);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferencesSupportSetReferenceIdIsMandatory() throws Exception {
        new CustomReferenceEntitiesSupport().setReference("attr1", null, CustomEntityType.SecurePassword);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferencesSupportSetReferenceTypeIsMandatory() throws Exception {
        new CustomReferenceEntitiesSupport().setReference("attr1", "id1", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferencesSupportSetKeyValueStoreReferenceAttributeIsMandatory() throws Exception {
        class TestSerializer implements CustomEntitySerializer{
            public byte[] serialize(Object entity) { return new byte[0]; }
            public Object deserialize(byte[] bytes) { return null; }
        }
        new CustomReferenceEntitiesSupport().setKeyValueStoreReference(null,"key1","prefix1",new TestSerializer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferencesSupportSetKeyValueStoreReferenceKeyIsMandatory() throws Exception {
        class TestSerializer implements CustomEntitySerializer{
            public byte[] serialize(Object entity) { return new byte[0]; }
            public Object deserialize(byte[] bytes) { return null; }
        }
        new CustomReferenceEntitiesSupport().setKeyValueStoreReference("attr1",null,"prefix1",new TestSerializer());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferencesSupportSetKeyValueStoreReferenceKeyPrefixIsMandatory() throws Exception {
        class TestSerializer implements CustomEntitySerializer{
            public byte[] serialize(Object entity) { return new byte[0]; }
            public Object deserialize(byte[] bytes) { return null; }
        }
        new CustomReferenceEntitiesSupport().setKeyValueStoreReference("attr1","key1",null,new TestSerializer());
    }

    @Test
    public void testReferencesSupportSetKeyValueStoreReferenceSerializerOptional() throws Exception {
        new CustomReferenceEntitiesSupport().setKeyValueStoreReference("attr1","key1","prefix1",null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReferenceElementSetIdWithNullValue() throws Exception {
        final CustomReferenceEntitiesSupport.ReferenceElement referenceElement = createReferenceElement("id1", null, CustomEntityType.SecurePassword, null);
        referenceElement.setId(null);
    }

    @Test
    public void testReferenceElementSetIdWithNonNullValue() throws Exception {
        final CustomReferenceEntitiesSupport.ReferenceElement referenceElement = createReferenceElement("id1", null, CustomEntityType.SecurePassword, null);
        referenceElement.setId("id2");
    }

    @Test
    public void testEqualsAndHashReferenceElement() throws Exception {
        assertEquals(
                createReferenceElement("id1", null, CustomEntityType.SecurePassword, null),
                createReferenceElement("id1", null, CustomEntityType.SecurePassword, null)
        );
        assertEquals(
                createReferenceElement("id1", null, CustomEntityType.SecurePassword, null).hashCode(),
                createReferenceElement("id1", null, CustomEntityType.SecurePassword, null).hashCode()
        );

        class TestSerializer implements CustomEntitySerializer{
            public byte[] serialize(Object entity) { return new byte[0]; }
            public Object deserialize(byte[] bytes) { return null; }
        }

        assertEquals(
                createReferenceElement("id2","prefix2",CustomEntityType.KeyValueStore,new TestSerializer()),
                createReferenceElement("id2","prefix2",CustomEntityType.KeyValueStore,new TestSerializer())
        );
        assertEquals(
                createReferenceElement("id2","prefix2",CustomEntityType.KeyValueStore,new TestSerializer()).hashCode(),
                createReferenceElement("id2","prefix2",CustomEntityType.KeyValueStore,new TestSerializer()).hashCode()
        );
    }

    private CustomReferenceEntitiesSupport.ReferenceElement createReferenceElement(
            final String id,
            final String keyPrefix,
            final CustomEntityType type,
            final CustomEntitySerializer entitySerializer
    ) throws Exception {
        final Constructor<CustomReferenceEntitiesSupport.ReferenceElement> constructor =
                CustomReferenceEntitiesSupport.ReferenceElement.class.getDeclaredConstructor(
                        String.class,
                        String.class,
                        CustomEntityType.class,
                        CustomEntitySerializer.class
                );
        constructor.setAccessible(true);
        return constructor.newInstance(id, keyPrefix, type, entitySerializer);
    }

    @Test
    public void testEqualsAndHash() throws Exception {
        class TestSerializer implements CustomEntitySerializer{
            public byte[] serialize(Object entity) { return new byte[0]; }
            public Object deserialize(byte[] bytes) { return null; }
        }

        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                })
        );
        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode(),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode()
        );


        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                    }
                })
        );
        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode(),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                    }
                }).hashCode()
        );


        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                })
        );
        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode(),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode()
        );


        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                    }
                })
        );
        assertEquals(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode(),
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                    }
                }).hashCode()
        );


        assertThat(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }),
                not(equalTo(
                        createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                            public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                                entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                                entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                                entitiesSupport.setReference("attr3", "id3", CustomEntityType.SecurePassword);
                            }
                        })
                ))
        );
        assertThat(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode(),
                not(equalTo(
                        createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                            public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                                entitiesSupport.setReference("attr1", "id1", CustomEntityType.SecurePassword);
                                entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                                entitiesSupport.setReference("attr3", "id3", CustomEntityType.SecurePassword);
                            }
                        }).hashCode()
                ))
        );


        assertThat(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }),
                not(equalTo(
                        createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                            public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                                entitiesSupport.setKeyValueStoreReference("attr2", "key1", "prefix1", new TestSerializer());
                                entitiesSupport.setReference("attr1", "id2", CustomEntityType.SecurePassword);
                                entitiesSupport.setKeyValueStoreReference("attr3", "key3", "prefix3", new TestSerializer());
                            }
                        })
                ))
        );
        assertThat(
                createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                    public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                        entitiesSupport.setKeyValueStoreReference("attr1", "key1", "prefix1", new TestSerializer());
                        entitiesSupport.setReference("attr2", "id2", CustomEntityType.SecurePassword);
                    }
                }).hashCode(),
                not(equalTo(
                        createCustomReferenceEntitiesSupport(new ReferencesSetter() {
                            public void setReferences(final CustomReferenceEntitiesSupport entitiesSupport) {
                                entitiesSupport.setKeyValueStoreReference("attr2", "key1", "prefix1", new TestSerializer());
                                entitiesSupport.setReference("attr1", "id2", CustomEntityType.SecurePassword);
                                entitiesSupport.setKeyValueStoreReference("attr3", "key3", "prefix3", new TestSerializer());
                            }
                        }).hashCode()
                ))
        );
    }

    static interface ReferencesSetter {
        void setReferences(CustomReferenceEntitiesSupport entitiesSupport);
    }

    private static CustomReferenceEntitiesSupport createCustomReferenceEntitiesSupport(final ReferencesSetter referencesSetter) throws Exception {
        final CustomReferenceEntitiesSupport entitiesSupport = new CustomReferenceEntitiesSupport();
        referencesSetter.setReferences(entitiesSupport);
        return entitiesSupport;
    }

    @org.junit.Ignore("Disabled for now. Perhaps enable it after it is fixed in one of java next releases")
    @org.junit.Test
    public void testVeryStrangeHashBehaviour() {
        final java.util.Map<String, Integer> m1 = new java.util.HashMap<String, Integer>();
        m1.put("1", 3);
        m1.put("2", 3);

        final java.util.Map<String, Integer> m2 = new java.util.HashMap<String, Integer>();
        m2.put("1", 2);
        m2.put("2", 2);

        // make sure maps are not equal
        org.junit.Assert.assertFalse("maps should be different", m1.equals(m2));
        org.junit.Assert.assertFalse("maps should be different", m2.equals(m1));

        final int h1 = m1.hashCode();
        final int h2 = m2.hashCode();
        org.junit.Assert.assertFalse("hashes should be different", h1 == h2);
    }

}
