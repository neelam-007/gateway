package com.l7tech.server.entity;

import com.l7tech.objectmodel.EntityManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.server.EntityManagerTest;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 *
 */
public class GenericEntityManagerTest extends EntityManagerTest {
    private GenericEntityManager genericEntityManager;

    @Before
    public void setUp() throws Exception {
        genericEntityManager = applicationContext.getBean("genericEntityManager", GenericEntityManager.class);
        genericEntityManager.registerClass(TestDemoGenericEntity.class);
        session.flush();
    }

    @Test
    public void testPersistGenericEntity() throws Exception {
        EntityManager<TestDemoGenericEntity, GenericEntityHeader> gem = genericEntityManager.getEntityManager(TestDemoGenericEntity.class);

        TestDemoGenericEntity entity = new TestDemoGenericEntity();
        entity.setName("test1");
        entity.setDescription("descr of test1");
        entity.setAge(27);
        entity.setPlaysTrombone(true);
        entity.setEnabled(true);

        Goid goid = gem.save(entity);
        session.flush();
        assertNotNull(goid);

        TestDemoGenericEntity found = gem.findByPrimaryKey(new Goid(goid.toString()));
        assertNotNull(found);
        assertEquals("test1", found.getName());
        assertEquals("descr of test1", found.getDescription());
        assertEquals(27, found.getAge());
        assertEquals(true, found.isPlaysTrombone());
    }
}
