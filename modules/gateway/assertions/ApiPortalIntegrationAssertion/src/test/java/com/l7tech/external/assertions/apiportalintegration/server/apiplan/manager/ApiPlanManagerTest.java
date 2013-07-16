package com.l7tech.external.assertions.apiportalintegration.server.apiplan.manager;

import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.GenericEntityHeader;
import com.l7tech.policy.InvalidGenericEntityException;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.test.BugNumber;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiPlanManagerTest {
    private static final String PLAN_NAME = "The Plan";
    private static final String POLICY_XML = "the xml";
    private static final Date DATE = new Date();
    private ApiPlanManager manager;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager<ApiPlan, GenericEntityHeader> entityManager;
    @Mock
    private ApplicationEventProxy applicationEventProxy;

    @Before
    public void setup() {
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(genericEntityManager.getEntityManager(ApiPlan.class)).thenReturn(entityManager);
        manager = new ApiPlanManager(applicationContext);
    }

    @Test
    public void add() throws Exception {
        final ApiPlan plan = createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML);
        when(entityManager.save(plan)).thenReturn(1234L);

        final ApiPlan result = manager.add(plan);

        assertEquals(1, manager.getCache().size());
        final ApiPlan cached = (ApiPlan) manager.getCache().get("p1");
        assertEquals(1234L, cached.getOid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertNotSame(plan, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(1234L));
        assertEquals(plan, result);
        assertEquals(1234L, result.getOid());
        verify(entityManager).save(plan);
    }

    @Test(expected = SaveException.class)
    public void addNonDefaultOid() throws Exception {
        final ApiPlan plan = createApiPlan(1L, "p1", PLAN_NAME, DATE, POLICY_XML);
        try {
            manager.add(plan);
        } catch (final SaveException e) {
            //expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).save(any(ApiPlan.class));
            throw e;
        }
        fail("Expected SaveException");
    }

    @Test(expected = SaveException.class)
    public void addSaveException() throws Exception {
        final ApiPlan plan = createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML);
        when(entityManager.save(any(ApiPlan.class))).thenThrow(new SaveException("Mocking exception"));

        try {
            manager.add(plan);
        } catch (final SaveException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).save(plan);
            throw e;
        }
    }

    @Test
    public void update() throws Exception {
        final ApiPlan toUpdate = createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML + "updated");
        toUpdate.setVersion(1);
        final ApiPlan existing = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        existing.setVersion(5);
        final ApiPlan expected = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML + "updated");
        expected.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);

        final ApiPlan result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        final ApiPlan cached = (ApiPlan) manager.getCache().get("p1");
        assertEquals(1234L, cached.getOid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML + "updated", cached.getPolicyXml());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(1234L));
        assertEquals(expected, result);
        assertEquals(1234L, result.getOid());
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager).update(expected);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void updateNotFound() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenReturn(null);

        try {
            manager.update(createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(ApiPlan.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = FindException.class)
    public void updateFindException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.update(createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML));
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(ApiPlan.class));
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void updateInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.update(createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML));
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(ApiPlan.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void updateInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        try {
            manager.update(createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(ApiPlan.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = UpdateException.class)
    public void updateUpdateException() throws Exception {
        final ApiPlan toUpdate = createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML + "updated");
        toUpdate.setVersion(1);
        final ApiPlan existing = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        existing.setVersion(5);
        final ApiPlan expected = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML + "updated");
        expected.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);
        doThrow(new UpdateException("mocking exception")).when(entityManager).update(any(ApiPlan.class));

        try {
            manager.update(toUpdate);
        } catch (final UpdateException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager).update(expected);
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test(expected = UpdateException.class)
    public void updateMissingPlanId() throws Exception {
        final ApiPlan toUpdate = createApiPlan(null, null, PLAN_NAME, DATE, POLICY_XML);

        try {
            manager.update(toUpdate);
        } catch (final UpdateException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).findByUniqueName(anyString());
            verify(entityManager, never()).update(any(ApiPlan.class));
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test
    public void updateNotNeeded() throws Exception {
        // name, description, policy have not changed
        final ApiPlan toUpdate = createApiPlan(null, "p1", PLAN_NAME, new Date(), POLICY_XML);
        toUpdate.setVersion(1);
        final ApiPlan existing = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        existing.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);

        final ApiPlan result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        final ApiPlan cached = (ApiPlan) manager.getCache().get("p1");
        assertEquals(1234L, cached.getOid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        // date should not have changed
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(1234L));
        assertEquals(existing, result);
        assertEquals(1234L, result.getOid());
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager, never()).update(Matchers.<ApiPlan>any());
    }

    @Test
    public void updateCache() throws Exception {
        final ApiPlan toUpdate = createApiPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML + "updated");
        toUpdate.setVersion(1);
        final ApiPlan existing = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        existing.setVersion(5);
        final ApiPlan expected = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML + "updated");
        expected.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);

        manager.getCache().put("p1", existing);
        manager.getNameCache().put(1234L, "p1");

        final ApiPlan result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        // cache should be updated
        final ApiPlan cached = (ApiPlan) manager.getCache().get("p1");
        assertEquals(1234L, cached.getOid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML + "updated", cached.getPolicyXml());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(1234L));
        assertEquals(expected, result);
        assertEquals(1234L, result.getOid());
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager).update(expected);
    }

    @Test
    public void delete() throws Exception {
        final ApiPlan found = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        when(entityManager.findByUniqueName("p1")).thenReturn(found);

        manager.delete("p1");

        assertTrue(manager.getCache().isEmpty());
        assertTrue(manager.getNameCache().isEmpty());
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager).delete(found);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void deleteNotFound() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenReturn(null);

        try {
            manager.delete("p1");
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).delete(any(ApiPlan.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = FindException.class)
    public void deleteFindException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.delete("p1");
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).delete(any(ApiPlan.class));
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void deleteInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.delete("p1");
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).delete(any(ApiPlan.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void deleteInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        try {
            manager.delete("p1");
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).delete(any(ApiPlan.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = DeleteException.class)
    public void deleteDeleteException() throws Exception {
        final ApiPlan found = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        when(entityManager.findByUniqueName("p1")).thenReturn(found);
        doThrow(new DeleteException("mocking exception")).when(entityManager).delete(any(ApiPlan.class));

        try {
            manager.delete("p1");
        } catch (final DeleteException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager).delete(found);
            throw e;
        }
        fail("Expected DeleteException");
    }

    @Test
    public void deleteRemovesFromCache() throws Exception {
        final ApiPlan found = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        when(entityManager.findByUniqueName("p1")).thenReturn(found);
        manager.getCache().put("p1", found);
        manager.getNameCache().put(1234L, "p1");

        manager.delete("p1");

        assertTrue(manager.getCache().isEmpty());
        // named cache entry should not be removed
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(1234L));
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager).delete(found);
    }

    @Test
    public void find() throws Exception {
        final ApiPlan found = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        when(entityManager.findByUniqueName("p1")).thenReturn(found);

        final ApiPlan apiPlan = manager.find("p1", false);

        assertEquals(1234L, apiPlan.getOid());
        assertEquals("p1", apiPlan.getName());
        assertEquals(PLAN_NAME, apiPlan.getDescription());
        assertEquals(DATE, apiPlan.getLastUpdate());
        assertEquals(POLICY_XML, apiPlan.getPolicyXml());
        assertEquals(1, manager.getCache().size());
        final ApiPlan cached = (ApiPlan) manager.getCache().get("p1");
        assertEquals(1234L, cached.getOid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertNotSame(found, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(1234L));
        verify(entityManager).findByUniqueName("p1");
    }

    @Test
    public void findNotFound() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenReturn(null);

        assertNull(manager.find("p1"));
        assertTrue(manager.getCache().isEmpty());
        assertTrue(manager.getNameCache().isEmpty());
        verify(entityManager).findByUniqueName("p1");
    }

    @Test(expected = FindException.class)
    public void findFindException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.find("p1");
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            throw e;
        }
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void findInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.find("p1");
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            throw e;
        }
    }

    @BugNumber(12334)
    @Test
    public void findInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        assertNull(manager.find("p1"));
        assertTrue(manager.getCache().isEmpty());
        assertTrue(manager.getNameCache().isEmpty());
        verify(entityManager).findByUniqueName("p1");
    }

    @Test
    public void findFromCache() throws Exception {
        manager.getCache().put("p1", createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML));
        manager.getNameCache().put(1234L, "p1");

        final ApiPlan plan = manager.find("p1", false);

        assertEquals(1234L, plan.getOid());
        assertEquals("p1", plan.getName());
        assertEquals(PLAN_NAME, plan.getDescription());
        assertEquals(DATE, plan.getLastUpdate());
        assertEquals(POLICY_XML, plan.getPolicyXml());
        assertEquals(1, manager.getCache().size());
        final ApiPlan cached = (ApiPlan) manager.getCache().get("p1");
        assertEquals(1234L, cached.getOid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(1234L));
        verify(entityManager, never()).findByUniqueName("p1");
    }

    @Test(expected = IllegalStateException.class)
    public void findFromCacheReadOnly() throws Exception {
        manager.getCache().put("p1", createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML));
        manager.getNameCache().put(1234L, "p1");

        final ApiPlan plan = manager.find("p1", false);

        assertEquals(1234L, plan.getOid());
        verify(entityManager, never()).findByUniqueName("p1");
        try {
            plan.setPolicyXml("readonly?");
        } catch (final IllegalStateException e) {
            // expected
            throw e;
        }
        fail("Expected IllegalStateException");
    }

    @Test
    public void findAll() throws Exception {
        when(entityManager.findAll()).thenReturn(Arrays.asList(createApiPlan(1L, "p1", PLAN_NAME + "1", DATE, POLICY_XML),
                createApiPlan(2L, "p2", PLAN_NAME + "2", DATE, POLICY_XML)));

        final List<ApiPlan> plans = manager.findAll();

        assertEquals(2, plans.size());
        verify(entityManager).findAll();
    }

    @Test
    public void findAllNone() throws Exception {
        when(entityManager.findAll()).thenReturn(Collections.<ApiPlan>emptyList());

        assertTrue(manager.findAll().isEmpty());
    }

    @Test
    public void findAllNull() throws Exception {
        when(entityManager.findAll()).thenReturn(null);

        assertTrue(manager.findAll().isEmpty());
    }

    @Test(expected = FindException.class)
    public void findAllFindException() throws Exception {
        when(entityManager.findAll()).thenThrow(new FindException("mocking exception"));

        manager.findAll();
    }

    @Test
    public void onApplicationEventGenericEntity() throws Exception {
        final ApiPlan plan = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        manager.getCache().put("p1", plan);
        manager.getNameCache().put(1234L, "p1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(plan, GenericEntity.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertTrue(manager.getCache().isEmpty());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotGenericEntity() throws Exception {
        manager.getCache().put("p1", createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML));
        manager.getNameCache().put(1234L, "p1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(new PublishedService(), PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotEntityInvalidationEvent() throws Exception {
        final ApiPlan plan = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        manager.getCache().put("p1", plan);
        manager.getNameCache().put(1234L, "p1");

        manager.onApplicationEvent(new LogonEvent("", LogonEvent.LOGOFF));

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventGenericEntityWrongId() throws Exception {
        final ApiPlan plan = createApiPlan(1234L, "p1", PLAN_NAME, DATE, POLICY_XML);
        manager.getCache().put("p1", plan);
        manager.getNameCache().put(1234L, "p1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(plan, GenericEntity.class, new long[]{5678L}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    private ApiPlan createApiPlan(final Long oid, final String planId, final String planName, final Date lastUpdate, final String policyXml) {
        final ApiPlan plan = new ApiPlan();
        if (oid != null) {
            plan.setOid(oid);
        }
        plan.setName(planId);
        plan.setDescription(planName);
        plan.setLastUpdate(lastUpdate);
        plan.setPolicyXml(policyXml);
        return plan;
    }
}
