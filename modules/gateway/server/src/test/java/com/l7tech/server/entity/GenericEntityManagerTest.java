package com.l7tech.server.entity;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.EntityManagerTest;
import com.l7tech.util.AnnotationClassFilter;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Hashtable;

import static org.junit.Assert.*;

/**
 *
 */
public class GenericEntityManagerTest extends EntityManagerTest {
    private GenericEntityManager genericEntityManager;
    private Goid testId;

    @Before
    public void setUp() throws Exception {
        genericEntityManager = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.unRegisterClass(TestDemoGenericEntity.class.getName());
        genericEntityManager.registerClass(TestDemoGenericEntity.class, null);

        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity entity = new TestDemoGenericEntity();
        entity.setName("test1");
        entity.setDescription("descr of test1");
        entity.setAge(27);
        entity.setPlaysTrombone(true);
        entity.setEnabled(true);
        entity.setTestGoid(new Goid(123, 456));

        testId = gem.save(entity);

        session.flush();
    }

    @Test
    public void testLookupByGoid() throws Exception {
        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity found = gem.findByPrimaryKey(new Goid(testId.toString()));
        assertNotNull(found);
        assertEquals("test1", found.getName());
        assertEquals("descr of test1", found.getDescription());
        assertEquals(27, found.getAge());
        assertEquals(true, found.isPlaysTrombone());
        assertEquals(new Goid(123, 456), found.getTestGoid());
    }

    @Test
    public void testPersistGenericEntity() throws Exception {
        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity entity = new TestDemoGenericEntity();
        entity.setName("test2");
        entity.setDescription("descr of test2");
        entity.setAge(41);
        entity.setPlaysTrombone(false);
        entity.setEnabled(true);
        entity.setTestGoid(new Goid(789, 999));

        Goid goid = gem.save(entity);
        session.flush();
        assertNotNull(goid);

        TestDemoGenericEntity found = gem.findByPrimaryKey(new Goid(goid.toString()));
        assertNotNull(found);
        assertEquals("test2", found.getName());
        assertEquals("descr of test2", found.getDescription());
        assertEquals(41, found.getAge());
        assertEquals(false, found.isPlaysTrombone());
        assertEquals(new Goid(789, 999), found.getTestGoid());
    }

    @Test
    public void testModifyExistingEntity() throws Exception {
        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity found = gem.findByPrimaryKey(new Goid(testId.toString()));
        assertNotNull(found);

        found.setAge(92);
        found.setName("EditedTest1");
        found.setDescription("Test1 that has been edited and resaved");
        found.setEnabled(true);
        found.setTestGoid(new Goid(789, 999));
        gem.update(found);
        session.flush();

        found = gem.findByPrimaryKey(new Goid(testId.toString()));
        assertNotNull(found);
        assertEquals(92, found.getAge());
        assertEquals("Test1 that has been edited and resaved", found.getDescription());
        assertEquals("EditedTest1", found.getName());
        assertEquals(new Goid(789, 999), found.getTestGoid());
    }

    @Test
    public void testPersistWithNonWhitelistedField() throws Exception {
        // Add something to the hashtable, forcing it to be persisted
        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity entity = new TestDemoGenericEntity();
        entity.setName("test3");
        entity.setDescription("descr of test3");
        entity.setAge(12);
        entity.setPlaysTrombone(true);
        entity.setEnabled(true);
        assertEquals("blah", entity.getHashtable().get("defaultEntry"));
        entity.getHashtable().clear();
        entity.getHashtable().put("otherkey", "otherval");
        entity.getHashtable().put("morekey", "moreval");

        Goid goid = gem.save(entity);
        session.flush();
        assertNotNull(goid);

        TestDemoGenericEntity found = gem.findByPrimaryKey(new Goid(goid.toString()));
        assertNotNull(found);
        final Hashtable<String, String> hashtable = found.getHashtable();
        assertNotNull("Hashtable field expected to revert to default value since it can't be deserialized (whitelist failure)", hashtable);
        assertEquals("Hashtable field expected to revert to default value due to whitelist failure", 1, hashtable.size());
        assertEquals("Hashtable field expected to revert to default value due to whitelist failure", "blah", hashtable.get("defaultEntry"));
    }

    @Test
    public void testEntityMetadataAdditionalWhitelist() throws Exception {
        // Re-register with metadata added for Hashtable class
        assertTrue(genericEntityManager.unRegisterClass(TestDemoGenericEntity.class.getName()));
        final GenericEntityMetadata meta = new GenericEntityMetadata().
            addSafeXmlClasses("java.util.Hashtable").
            addSafeXmlConstructors("java.util.Hashtable()").
            addSafeXmlMethods(
                "java.util.Hashtable.put(java.lang.Object,java.lang.Object)",
                "java.util.Hashtable.remove(java.lang.Object)"
            );
        genericEntityManager.registerClass(TestDemoGenericEntity.class, meta);

        // Add something to the hashtable, forcing it to be persisted
        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity entity = new TestDemoGenericEntity();
        entity.setName("test3");
        entity.setDescription("descr of test3");
        entity.setAge(12);
        entity.setPlaysTrombone(true);
        entity.setEnabled(true);
        assertEquals("blah", entity.getHashtable().get("defaultEntry"));
        entity.getHashtable().clear();
        entity.getHashtable().put("otherkey", "otherval");
        entity.getHashtable().put("morekey", "moreval");

        Goid goid = gem.save(entity);
        session.flush();
        assertNotNull(goid);

        TestDemoGenericEntity found = gem.findByPrimaryKey(new Goid(goid.toString()));
        assertNotNull(found);
        assertEquals("test3", found.getName());
        assertEquals("descr of test3", found.getDescription());
        assertEquals(12, found.getAge());
        assertEquals(true, found.isPlaysTrombone());
        assertEquals("otherval", entity.getHashtable().get("otherkey"));
        assertEquals("moreval", entity.getHashtable().get("morekey"));
        assertFalse(entity.getHashtable().containsKey("defaultEntry"));
    }

    @Test
    public void testEntityMetadataCustomClassFilter() throws Exception {
        // Re-register with custom class filter for Hashtable class
        assertTrue(genericEntityManager.unRegisterClass(TestDemoGenericEntity.class.getName()));
        final GenericEntityMetadata meta = new GenericEntityMetadata().
            addSafeXmlClassFilter(new AnnotationClassFilter(null, Arrays.asList("java.util.")) {
                @Override
                protected boolean permitClass(@NotNull Class<?> clazz) {
                    return Hashtable.class.equals(clazz);
                }

                @Override
                public boolean permitConstructor(@NotNull Constructor<?> constructor) {
                    return Hashtable.class.equals(constructor.getDeclaringClass()) && isDefaultConstructor(constructor);
                }

                @Override
                public boolean permitMethod(@NotNull Method method) {
                    return Hashtable.class.equals(method.getDeclaringClass()) && isPublicNonStatic(method);
                }
            });
        genericEntityManager.registerClass(TestDemoGenericEntity.class, meta);

        // Add something to the hashtable, forcing it to be persisted
        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity entity = new TestDemoGenericEntity();
        entity.setName("test3");
        entity.setDescription("descr of test3");
        entity.setAge(12);
        entity.setPlaysTrombone(true);
        entity.setEnabled(true);
        assertEquals("blah", entity.getHashtable().get("defaultEntry"));
        entity.getHashtable().clear();
        entity.getHashtable().put("otherkey", "otherval");
        entity.getHashtable().put("morekey", "moreval");

        Goid goid = gem.save(entity);
        session.flush();
        assertNotNull(goid);

        TestDemoGenericEntity found = gem.findByPrimaryKey(new Goid(goid.toString()));
        assertNotNull(found);
        assertEquals("test3", found.getName());
        assertEquals("descr of test3", found.getDescription());
        assertEquals(12, found.getAge());
        assertEquals(true, found.isPlaysTrombone());
        assertEquals("otherval", entity.getHashtable().get("otherkey"));
        assertEquals("moreval", entity.getHashtable().get("morekey"));
        assertFalse(entity.getHashtable().containsKey("defaultEntry"));
    }

}
