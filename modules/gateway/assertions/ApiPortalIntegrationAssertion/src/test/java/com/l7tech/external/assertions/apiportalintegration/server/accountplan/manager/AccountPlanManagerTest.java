package com.l7tech.external.assertions.apiportalintegration.server.accountplan.manager;

import com.l7tech.external.assertions.apiportalintegration.server.accountplan.AccountPlan;
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

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AccountPlanManagerTest {
    private static final String PLAN_NAME = "The Plan";
    private static final String POLICY_XML = "the xml";
    private static final List<String> ORG_IDS = new ArrayList<String>(2);
    static {
        ORG_IDS.add("1");
        ORG_IDS.add("2");
    }
    private static final Date DATE = new Date();
    public static final boolean DEFAULT_PLAN_ENABLED = true;
    public static final boolean THROUGHPUT_QUOTA_ENABLED = true;
    public static final int QUOTA_10 = 10;
    public static final int TIME_UNIT_1 = 1;
    public static final int COUNTER_STRATEGY_1 = 1;
    private AccountPlanManager manager;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager<AccountPlan, GenericEntityHeader> entityManager;
    @Mock
    private ApplicationEventProxy applicationEventProxy;

    @Before
    public void setup() {
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(genericEntityManager.getEntityManager(AccountPlan.class)).thenReturn(entityManager);
        manager = new AccountPlanManager(applicationContext);
    }

    @Test
    public void add() throws Exception {
        final AccountPlan plan = createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        when(entityManager.save(plan)).thenReturn(new Goid(0,1234L));

        final AccountPlan result = manager.add(plan);

        assertEquals(1, manager.getCache().size());
        final AccountPlan cached = (AccountPlan) manager.getCache().get("p1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertEquals(DEFAULT_PLAN_ENABLED, cached.isDefaultPlan());
        assertEquals(THROUGHPUT_QUOTA_ENABLED, cached.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10, cached.getQuota());
        assertEquals(TIME_UNIT_1, cached.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, cached.getCounterStrategy());
        assertEquals(ORG_IDS, cached.getIds());
        assertNotSame(plan, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(new Goid(0,1234L)));
        assertEquals(plan, result);
        assertEquals(new Goid(0,1234L), result.getGoid());
        verify(entityManager).save(plan);
    }

    @Test(expected = SaveException.class)
    public void addNonDefaultOid() throws Exception {
        final AccountPlan plan = createAccountPlan(new Goid(0,1L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        try {
            manager.add(plan);
        } catch (final SaveException e) {
            //expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).save(any(AccountPlan.class));
            throw e;
        }
        fail("Expected SaveException");
    }

    @Test(expected = SaveException.class)
    public void addSaveException() throws Exception {
        final AccountPlan plan = createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        when(entityManager.save(any(AccountPlan.class))).thenThrow(new SaveException("Mocking exception"));

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
        final AccountPlan toUpdate = createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML + "updated",
                !DEFAULT_PLAN_ENABLED, !THROUGHPUT_QUOTA_ENABLED, QUOTA_10 - 1, TIME_UNIT_1 + 1, COUNTER_STRATEGY_1 + 1,
                ORG_IDS.subList(0, 1));
        toUpdate.setVersion(1);
        final AccountPlan existing = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        existing.setVersion(5);
        final AccountPlan expected = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML + "updated",
                !DEFAULT_PLAN_ENABLED, !THROUGHPUT_QUOTA_ENABLED, QUOTA_10 - 1, TIME_UNIT_1 + 1, COUNTER_STRATEGY_1 + 1,
                ORG_IDS.subList(0, 1));
        expected.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);

        final AccountPlan result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        final AccountPlan cached = (AccountPlan) manager.getCache().get("p1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML + "updated", cached.getPolicyXml());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(!DEFAULT_PLAN_ENABLED, cached.isDefaultPlan());
        assertEquals(!THROUGHPUT_QUOTA_ENABLED, cached.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10 - 1, cached.getQuota());
        assertEquals(TIME_UNIT_1 + 1, cached.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1 + 1, cached.getCounterStrategy());
        assertEquals(ORG_IDS.subList(0, 1), cached.getIds());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(new Goid(0,1234L)));
        assertEquals(expected, result);
        assertEquals(new Goid(0,1234L), result.getGoid());
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager).update(expected);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void updateNotFound() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenReturn(null);

        try {
            manager.update(createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML,
                    DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(AccountPlan.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = FindException.class)
    public void updateFindException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.update(createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML,
                    DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(AccountPlan.class));
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void updateInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.update(createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML,
                    DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(AccountPlan.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void updateInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("p1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        try {
            manager.update(createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML,
                    DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("p1");
            verify(entityManager, never()).update(any(AccountPlan.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = UpdateException.class)
    public void updateUpdateException() throws Exception {
        final AccountPlan toUpdate = createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML + "updated",
                !DEFAULT_PLAN_ENABLED, !THROUGHPUT_QUOTA_ENABLED, QUOTA_10 - 1, TIME_UNIT_1 + 1, COUNTER_STRATEGY_1 + 1,
                ORG_IDS.subList(0, 1));
        toUpdate.setVersion(1);
        final AccountPlan existing = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        existing.setVersion(5);
        final AccountPlan expected = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML + "updated",
                !DEFAULT_PLAN_ENABLED, !THROUGHPUT_QUOTA_ENABLED, QUOTA_10 - 1, TIME_UNIT_1 + 1, COUNTER_STRATEGY_1 + 1,
                ORG_IDS.subList(0, 1));
        expected.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);
        doThrow(new UpdateException("mocking exception")).when(entityManager).update(any(AccountPlan.class));

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
        final AccountPlan toUpdate = createAccountPlan(null, null, PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);

        try {
            manager.update(toUpdate);
        } catch (final UpdateException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).findByUniqueName(anyString());
            verify(entityManager, never()).update(any(AccountPlan.class));
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test
    public void updateNotNeeded() throws Exception {
        // name, description, policy, default plan, throughputquotaenabled, quota, timeunit, counter strategy
        // and organizations have not changed
        final AccountPlan toUpdate = createAccountPlan(null, "p1", PLAN_NAME, new Date(), POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        toUpdate.setVersion(1);
        final AccountPlan existing = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        existing.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);

        final AccountPlan result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        final AccountPlan cached = (AccountPlan) manager.getCache().get("p1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        // date should not have changed
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertEquals(DEFAULT_PLAN_ENABLED, cached.isDefaultPlan());
        assertEquals(THROUGHPUT_QUOTA_ENABLED, cached.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10, cached.getQuota());
        assertEquals(TIME_UNIT_1, cached.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, cached.getCounterStrategy());
        assertEquals(ORG_IDS, cached.getIds());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(new Goid(0,1234L)));
        assertEquals(existing, result);
        assertEquals(new Goid(0,1234L), result.getGoid());
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager, never()).update(Matchers.<AccountPlan>any());
    }

    @Test
    public void updateCache() throws Exception {
        final AccountPlan toUpdate = createAccountPlan(null, "p1", PLAN_NAME, DATE, POLICY_XML + "updated",
                !DEFAULT_PLAN_ENABLED, !THROUGHPUT_QUOTA_ENABLED, QUOTA_10 - 1, TIME_UNIT_1 + 1, COUNTER_STRATEGY_1 + 1,
                ORG_IDS.subList(0, 1));
        toUpdate.setVersion(1);
        final AccountPlan existing = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        existing.setVersion(5);
        final AccountPlan expected = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML + "updated",
                !DEFAULT_PLAN_ENABLED, !THROUGHPUT_QUOTA_ENABLED, QUOTA_10 - 1, TIME_UNIT_1 + 1, COUNTER_STRATEGY_1 + 1,
                ORG_IDS.subList(0, 1));
        expected.setVersion(5);
        when(entityManager.findByUniqueName("p1")).thenReturn(existing);

        manager.getCache().put("p1", existing);
        manager.getNameCache().put(new Goid(0,1234L), "p1");

        final AccountPlan result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        // cache should be updated
        final AccountPlan cached = (AccountPlan) manager.getCache().get("p1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML + "updated", cached.getPolicyXml());
        assertEquals(!DEFAULT_PLAN_ENABLED, cached.isDefaultPlan());
        assertEquals(!THROUGHPUT_QUOTA_ENABLED, cached.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10 - 1, cached.getQuota());
        assertEquals(TIME_UNIT_1 + 1, cached.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1 + 1, cached.getCounterStrategy());
        assertEquals(ORG_IDS.subList(0, 1), cached.getIds());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(new Goid(0,1234L)));
        assertEquals(expected, result);
        assertEquals(new Goid(0,1234L), result.getGoid());
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager).update(expected);
    }

    @Test
    public void delete() throws Exception {
        final AccountPlan found = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
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
            verify(entityManager, never()).delete(any(AccountPlan.class));
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
            verify(entityManager, never()).delete(any(AccountPlan.class));
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
            verify(entityManager, never()).delete(any(AccountPlan.class));
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
            verify(entityManager, never()).delete(any(AccountPlan.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = DeleteException.class)
    public void deleteDeleteException() throws Exception {
        final AccountPlan found = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        when(entityManager.findByUniqueName("p1")).thenReturn(found);
        doThrow(new DeleteException("mocking exception")).when(entityManager).delete(any(AccountPlan.class));

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
        final AccountPlan found = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        when(entityManager.findByUniqueName("p1")).thenReturn(found);
        manager.getCache().put("p1", found);
        manager.getNameCache().put(new Goid(0,1234L), "p1");

        manager.delete("p1");

        assertTrue(manager.getCache().isEmpty());
        // named cache entry should not be removed
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(new Goid(0,1234L)));
        verify(entityManager).findByUniqueName("p1");
        verify(entityManager).delete(found);
    }

    @Test
    public void find() throws Exception {
        final AccountPlan found = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        when(entityManager.findByUniqueName("p1")).thenReturn(found);

        final AccountPlan AccountPlan = manager.find("p1", false);

        assertEquals(new Goid(0,1234L), AccountPlan.getGoid());
        assertEquals("p1", AccountPlan.getName());
        assertEquals(PLAN_NAME, AccountPlan.getDescription());
        assertEquals(DATE, AccountPlan.getLastUpdate());
        assertEquals(POLICY_XML, AccountPlan.getPolicyXml());
        assertEquals(1, manager.getCache().size());
        final AccountPlan cached = (AccountPlan) manager.getCache().get("p1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertEquals(DEFAULT_PLAN_ENABLED, cached.isDefaultPlan());
        assertEquals(THROUGHPUT_QUOTA_ENABLED, cached.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10, cached.getQuota());
        assertEquals(TIME_UNIT_1, cached.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, cached.getCounterStrategy());
        assertEquals(ORG_IDS, cached.getIds());
        assertNotSame(found, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(new Goid(0,1234L)));
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
        manager.getCache().put("p1", createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        manager.getNameCache().put(new Goid(0,1234L), "p1");

        final AccountPlan plan = manager.find("p1", false);

        assertEquals(new Goid(0,1234L), plan.getGoid());
        assertEquals("p1", plan.getName());
        assertEquals(PLAN_NAME, plan.getDescription());
        assertEquals(DATE, plan.getLastUpdate());
        assertEquals(POLICY_XML, plan.getPolicyXml());
        assertEquals(1, manager.getCache().size());
        final AccountPlan cached = (AccountPlan) manager.getCache().get("p1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("p1", cached.getName());
        assertEquals(PLAN_NAME, cached.getDescription());
        assertEquals(DATE, cached.getLastUpdate());
        assertEquals(POLICY_XML, cached.getPolicyXml());
        assertEquals(DEFAULT_PLAN_ENABLED, cached.isDefaultPlan());
        assertEquals(THROUGHPUT_QUOTA_ENABLED, cached.isThroughputQuotaEnabled());
        assertEquals(QUOTA_10, cached.getQuota());
        assertEquals(TIME_UNIT_1, cached.getTimeUnit());
        assertEquals(COUNTER_STRATEGY_1, cached.getCounterStrategy());
        assertEquals(ORG_IDS, cached.getIds());
        assertEquals(1, manager.getNameCache().size());
        assertEquals("p1", manager.getNameCache().get(new Goid(0,1234L)));
        verify(entityManager, never()).findByUniqueName("p1");
    }

    @Test(expected = IllegalStateException.class)
    public void findFromCacheReadOnly() throws Exception {
        manager.getCache().put("p1", createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        manager.getNameCache().put(new Goid(0,1234L), "p1");

        final AccountPlan plan = manager.find("p1", false);

        assertEquals(new Goid(0,1234L), plan.getGoid());
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
        when(entityManager.findAll()).thenReturn(Arrays.asList(createAccountPlan(new Goid(0,1L), "p1", PLAN_NAME + "1", DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS),
                createAccountPlan(new Goid(0,2L), "p2", PLAN_NAME + "2", DATE, POLICY_XML,
                        DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS)));

        final List<AccountPlan> plans = manager.findAll();

        assertEquals(2, plans.size());
        verify(entityManager).findAll();
    }

    @Test
    public void findAllNone() throws Exception {
        when(entityManager.findAll()).thenReturn(Collections.<AccountPlan>emptyList());

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
        final AccountPlan plan = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        manager.getCache().put("p1", plan);
        manager.getNameCache().put(new Goid(0,1234L), "p1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(plan, GenericEntity.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertTrue(manager.getCache().isEmpty());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotGenericEntity() throws Exception {
        manager.getCache().put("p1", createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS));
        manager.getNameCache().put(new Goid(0,1234L), "p1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(new PublishedService(), PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotEntityInvalidationEvent() throws Exception {
        final AccountPlan plan = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        manager.getCache().put("p1", plan);
        manager.getNameCache().put(new Goid(0,1234L), "p1");

        manager.onApplicationEvent(new LogonEvent("", LogonEvent.LOGOFF));

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventGenericEntityWrongId() throws Exception {
        final AccountPlan plan = createAccountPlan(new Goid(0,1234L), "p1", PLAN_NAME, DATE, POLICY_XML,
                DEFAULT_PLAN_ENABLED, THROUGHPUT_QUOTA_ENABLED, QUOTA_10, TIME_UNIT_1, COUNTER_STRATEGY_1, ORG_IDS);
        manager.getCache().put("p1", plan);
        manager.getNameCache().put(new Goid(0,1234L), "p1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(plan, GenericEntity.class, new Goid[]{new Goid(0,5678L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    private AccountPlan createAccountPlan(final Goid goid, final String planId, final String planName,
                                          final Date lastUpdate, final String policyXml, final boolean defaultPlan,
                                          final boolean throughputQuotaEnabled, final int quota, final int timeUnit,
                                          final int counterStrategy, final List<String> organizationIds) {
        final AccountPlan plan = new AccountPlan();
        if (goid != null) {
            plan.setGoid(goid);
        }
        plan.setName(planId);
        plan.setDescription(planName);
        plan.setLastUpdate(lastUpdate);
        plan.setPolicyXml(policyXml);
        plan.setDefaultPlan(defaultPlan);
        plan.setThroughputQuotaEnabled(throughputQuotaEnabled);
        plan.setQuota(quota);
        plan.setTimeUnit(timeUnit);
        plan.setCounterStrategy(counterStrategy);
        plan.setIds(organizationIds);
        return plan;
    }
}
