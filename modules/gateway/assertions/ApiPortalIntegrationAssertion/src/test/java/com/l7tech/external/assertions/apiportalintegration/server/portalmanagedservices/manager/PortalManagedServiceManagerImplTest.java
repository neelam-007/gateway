package com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalIntegrationAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.gateway.common.service.PublishedService;
import com.l7tech.gateway.common.service.ServiceHeader;
import com.l7tech.objectmodel.*;
import com.l7tech.policy.GenericEntity;
import com.l7tech.policy.InvalidGenericEntityException;
import com.l7tech.policy.Policy;
import com.l7tech.policy.PolicyType;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.service.ServiceManager;
import com.l7tech.server.util.ApplicationEventProxy;
import com.l7tech.test.BugNumber;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalManagedServiceManagerImplTest {
    private static final Goid SERVICE_A = new Goid(0,1L);
    private static final String SERVICE_A_STRING = String.valueOf(SERVICE_A);
    private static final Goid SERVICE_B = new Goid(0,2L);
    private static final String SERVICE_B_STRING = String.valueOf(SERVICE_B);
    private PortalManagedServiceManagerImpl manager;
    private List<ServiceHeader> serviceHeaders;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private ServiceManager serviceManager;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ApplicationEventProxy applicationEventProxy;

    @Before
    public void setup() {
        when(applicationContext.getBean("serviceManager", ServiceManager.class)).thenReturn(serviceManager);
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(genericEntityManager.getEntityManager(PortalManagedService.class)).thenReturn(entityManager);
        manager = new PortalManagedServiceManagerImpl(applicationContext);
        serviceHeaders = new ArrayList<ServiceHeader>();
    }

    @Test
    public void add() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        when(entityManager.save(portalManagedService)).thenReturn(new Goid(0,1234L));

        final PortalManagedService result = manager.add(portalManagedService);

        assertEquals(1, manager.getCache().size());
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertNotSame(portalManagedService, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        assertSame(portalManagedService, result);
        assertEquals(new Goid(0,1234L), result.getGoid());
        verify(entityManager).save(portalManagedService);
    }

    @Test(expected = SaveException.class)
    public void addNonDefaultOid() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        portalManagedService.setGoid(new Goid(0,1L));
        try {
            manager.add(portalManagedService);
        } catch (final SaveException e) {
            //expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).save(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected SaveException");
    }

    @Test(expected = SaveException.class)
    public void addSaveException() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        when(entityManager.save(any(PortalManagedService.class))).thenThrow(new SaveException("Mocking exception"));

        try {
            manager.add(portalManagedService);
        } catch (final SaveException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).save(portalManagedService);
            throw e;
        }
    }

    @Test
    public void update() throws Exception {
        final PortalManagedService toUpdate = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        toUpdate.setVersion(1);
        final PortalManagedService existing = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_B, "group2");
        existing.setVersion(5);
        final PortalManagedService expected = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        expected.setVersion(5);
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);

        final PortalManagedService result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        assertSame(toUpdate, result);
        assertEquals(new Goid(0,1234L), result.getGoid());
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).update(expected);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void updateNotFound() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenReturn(null);

        try {
            manager.update(createPortalManagedService(null, "a1", SERVICE_A, "group1"));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = FindException.class)
    public void updateFindException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.update(createPortalManagedService(null, "a1", SERVICE_A, "group1"));
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void updateInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.update(createPortalManagedService(null, "a1", SERVICE_A, "group1"));
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void updateInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        try {
            manager.update(createPortalManagedService(null, "a1", SERVICE_A, "group1"));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = UpdateException.class)
    public void updateUpdateException() throws Exception {
        final PortalManagedService toUpdate = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        toUpdate.setVersion(1);
        final PortalManagedService existing = createPortalManagedService(new Goid(0,1234L), "a2", SERVICE_A, "group2");
        existing.setVersion(5);
        final PortalManagedService expected = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        expected.setVersion(5);
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);
        doThrow(new UpdateException("mocking exception")).when(entityManager).update(any(PortalManagedService.class));

        try {
            manager.update(toUpdate);
        } catch (final UpdateException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager).update(expected);
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test(expected = UpdateException.class)
    public void updateMissingName() throws Exception {
        final PortalManagedService toUpdate = createPortalManagedService(null, null, SERVICE_A, "group1");

        try {
            manager.update(toUpdate);
        } catch (final UpdateException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test
    public void updateCache() throws Exception {
        final PortalManagedService toUpdate = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        toUpdate.setVersion(1);
        final PortalManagedService existing = createPortalManagedService(new Goid(0,1234L), "a2", SERVICE_A, "group2");
        existing.setVersion(5);
        final PortalManagedService expected = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        expected.setVersion(5);
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);

        manager.getCache().put("a1", existing);
        manager.getNameCache().put(new Goid(0,1234L), "a1");

        final PortalManagedService result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        // cache should be updated
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        assertSame(toUpdate, result);
        assertEquals(new Goid(0,1234L), result.getGoid());
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).update(expected);
    }

    /**
     * If not found - should add.
     */
    @Test
    public void addOrUpdateNotFound() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenReturn(null);
        when(entityManager.save(any(PortalManagedService.class))).thenReturn(new Goid(0,1234L));

        final PortalManagedService result = manager.addOrUpdate(portalManagedService);

        assertEquals(1, manager.getCache().size());
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertNotSame(portalManagedService, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        assertEquals(cached, result);
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).save(portalManagedService);
        verify(entityManager, never()).update(any(PortalManagedService.class));
    }

    /**
     * If found - should update.
     */
    @Test
    public void addOrUpdateFound() throws Exception {
        final PortalManagedService toUpdate = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        final PortalManagedService existing = createPortalManagedService(new Goid(0,1234L), "a2", SERVICE_A, "group2");
        final PortalManagedService expected = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);

        final PortalManagedService result = manager.addOrUpdate(toUpdate);

        assertEquals(1, manager.getCache().size());
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        assertEquals(cached, result);
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).update(expected);
        verify(entityManager, never()).save(any(PortalManagedService.class));
    }

    @Test(expected = FindException.class)
    public void addOrUpdateFindException() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.addOrUpdate(portalManagedService);
        } catch (final FindException e) {
            verify(entityManager).findByUniqueName("a1");
            throw e;
        }
        fail("expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void addOrUpdateInvalidGenericEntityException() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.addOrUpdate(portalManagedService);
        } catch (final InvalidGenericEntityException e) {
            verify(entityManager).findByUniqueName("a1");
            throw e;
        }
        fail("expected InvalidGenericEntityException");
    }

    /**
     * Should treat as if it was null/not found.
     */
    @BugNumber(12334)
    @Test
    public void addOrUpdateInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(null, "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));
        when(entityManager.save(any(PortalManagedService.class))).thenReturn(new Goid(0,1234L));

        final PortalManagedService result = manager.addOrUpdate(portalManagedService);

        assertEquals(1, manager.getCache().size());
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertNotSame(portalManagedService, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        assertEquals(cached, result);
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).save(portalManagedService);
        verify(entityManager, never()).update(any(PortalManagedService.class));
    }

    @Test
    public void delete() throws Exception {
        final PortalManagedService found = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenReturn(found);

        manager.delete("a1");

        assertTrue(manager.getCache().isEmpty());
        assertTrue(manager.getNameCache().isEmpty());
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).delete(found);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void deleteNotFound() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenReturn(null);

        try {
            manager.delete("a1");
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).delete(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = FindException.class)
    public void deleteFindException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.delete("a1");
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).delete(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void deleteInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.delete("a1");
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).delete(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void deleteInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        try {
            manager.delete("a1");
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).delete(any(PortalManagedService.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @Test(expected = DeleteException.class)
    public void deleteDeleteException() throws Exception {
        final PortalManagedService found = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenReturn(found);
        doThrow(new DeleteException("mocking exception")).when(entityManager).delete(any(PortalManagedService.class));

        try {
            manager.delete("a1");
        } catch (final DeleteException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager).delete(found);
            throw e;
        }
        fail("Expected DeleteException");
    }

    @Test
    public void deleteRemovesFromCache() throws Exception {
        final PortalManagedService found = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenReturn(found);
        manager.getCache().put("a1", found);
        manager.getNameCache().put(new Goid(0,1234L), "a1");

        manager.delete("a1");

        assertTrue(manager.getCache().isEmpty());
        // named cache entry should not be removed
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).delete(found);
    }

    @Test
    public void find() throws Exception {
        final PortalManagedService found = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        when(entityManager.findByUniqueName("a1")).thenReturn(found);

        final PortalManagedService portalManagedService = manager.find("a1");

        assertEquals(new Goid(0,1234L), portalManagedService.getGoid());
        assertEquals("a1", portalManagedService.getName());
        assertEquals(SERVICE_A_STRING, portalManagedService.getDescription());
        assertEquals("group1", portalManagedService.getApiGroup());
        assertEquals(1, manager.getCache().size());
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertNotSame(found, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        verify(entityManager).findByUniqueName("a1");
    }

    @Test
    public void findNotFound() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenReturn(null);

        assertNull(manager.find("a1"));
        assertTrue(manager.getCache().isEmpty());
        assertTrue(manager.getNameCache().isEmpty());
        verify(entityManager).findByUniqueName("a1");
    }

    @Test(expected = FindException.class)
    public void findFindException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.find("a1");
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            throw e;
        }
        fail("expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void findInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.find("a1");
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            throw e;
        }
        fail("expected InvalidGenericEntityException");
    }

    @BugNumber(12334)
    @Test
    public void findInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        final PortalManagedService portalManagedService = manager.find("a1");

        assertNull(portalManagedService);
        assertTrue(manager.getCache().isEmpty());
        assertTrue(manager.getNameCache().isEmpty());
        verify(entityManager).findByUniqueName("a1");
    }

    @Test
    public void findFromCache() throws Exception {
        manager.getCache().put("a1", createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1"));
        manager.getNameCache().put(new Goid(0,1234L), "a1");

        final PortalManagedService portalManagedService = manager.find("a1");

        assertEquals(new Goid(0,1234L), portalManagedService.getGoid());
        assertEquals("a1", portalManagedService.getName());
        assertEquals(SERVICE_A_STRING, portalManagedService.getDescription());
        assertEquals("group1", portalManagedService.getApiGroup());
        assertEquals(1, manager.getCache().size());
        final PortalManagedService cached = (PortalManagedService) manager.getCache().get("a1");
        assertEquals(new Goid(0,1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(SERVICE_A_STRING, cached.getDescription());
        assertEquals("group1", cached.getApiGroup());
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0,1234L)));
        verify(entityManager, never()).findByUniqueName("a1");
    }

    @Test(expected = IllegalStateException.class)
    public void findFromCacheReadOnly() throws Exception {
        manager.getCache().put("a1", createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1"));
        manager.getNameCache().put(new Goid(0,1234L), "a1");

        final PortalManagedService portalManagedService = manager.find("a1");

        assertEquals(new Goid(0,1234L), portalManagedService.getGoid());
        verify(entityManager, never()).findByUniqueName("a1");
        try {
            portalManagedService.setApiGroup("readonly?");
        } catch (final IllegalStateException e) {
            // expected
            throw e;
        }
        fail("Expected IllegalStateException");
    }

    @Test
    public void findAll() throws Exception {
        when(entityManager.findAll()).thenReturn(Arrays.asList(createPortalManagedService(new Goid(0,1L), "a1", new Goid(0,1L), "group1"),
                createPortalManagedService(new Goid(0,2L), "a2", new Goid(0,2L), "group2")));

        final List<PortalManagedService> portalManagedServices = manager.findAll();

        assertEquals(2, portalManagedServices.size());
        verify(entityManager).findAll();
    }

    @Test
    public void findAllNone() throws Exception {
        when(entityManager.findAll()).thenReturn(Collections.emptyList());

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
    public void findAllFromPolicySingleService() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createAssertion("a1", "group1"));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(1, portalManagedServices.size());
        assertEquals("a1", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_A_STRING, portalManagedServices.get(0).getDescription());
        assertEquals("group1", portalManagedServices.get(0).getApiGroup());
    }

    @Test
    public void findAllFromPolicySingleServiceDisabled() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createDisabledAssertion("a1", "group1"));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        assertTrue(manager.findAllFromPolicy().isEmpty());
    }

    @Test
    public void findAllFromPolicySingleServiceMultipleAssertions() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createAssertion("a1", "group1"), createAssertion("a2", "group2"));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(1, portalManagedServices.size());
        assertEquals("a1", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_A_STRING, portalManagedServices.get(0).getDescription());
        assertEquals("group1", portalManagedServices.get(0).getApiGroup());
    }

    @Test
    public void findAllFromPolicySingleServiceMultipleAssertionsOneDisabled() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createDisabledAssertion("a1", "group1"), createAssertion("a2", "group2"));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(1, portalManagedServices.size());
        assertEquals("a2", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_A_STRING, portalManagedServices.get(0).getDescription());
        assertEquals("group2", portalManagedServices.get(0).getApiGroup());
    }

    @Test
    public void findAllFromPolicySingleServiceMultipleAssertionsAllDisabled() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createDisabledAssertion("a1", "group1"), createDisabledAssertion("a2", "group2"));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        assertTrue(manager.findAllFromPolicy().isEmpty());
    }

    @Test
    public void findAllFromPolicyMultipleServices() throws Exception {
        final PublishedService service1 = createPublishedService(SERVICE_A, createAssertion("a1", "group1"));
        final PublishedService service2 = createPublishedService(SERVICE_B, createAssertion("a2", "group2"));
        serviceHeaders.add(new ServiceHeader(service1));
        serviceHeaders.add(new ServiceHeader(service2));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(SERVICE_B)).thenReturn(service2);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(2, portalManagedServices.size());
        assertEquals("a1", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_A_STRING, portalManagedServices.get(0).getDescription());
        assertEquals("group1", portalManagedServices.get(0).getApiGroup());
        assertEquals("a2", portalManagedServices.get(1).getName());
        assertEquals(SERVICE_B_STRING, portalManagedServices.get(1).getDescription());
        assertEquals("group2", portalManagedServices.get(1).getApiGroup());
    }

    @Test
    public void findAllFromPolicyMultipleServicesAndMultipleAssertions() throws Exception {
        final PublishedService service1 = createPublishedService(SERVICE_A, createAssertion("a1", "group1"));
        final PublishedService service2 = createPublishedService(SERVICE_B, createAssertion("a2", "group2"));
        serviceHeaders.add(new ServiceHeader(service1));
        serviceHeaders.add(new ServiceHeader(service2));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(SERVICE_B)).thenReturn(service2);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(2, portalManagedServices.size());
        assertEquals("a1", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_A_STRING, portalManagedServices.get(0).getDescription());
        assertEquals("group1", portalManagedServices.get(0).getApiGroup());
        assertEquals("a2", portalManagedServices.get(1).getName());
        assertEquals(SERVICE_B_STRING, portalManagedServices.get(1).getDescription());
        assertEquals("group2", portalManagedServices.get(1).getApiGroup());
    }

    @Test(expected = FindException.class)
    public void findAllFromPolicyHeaderFindException() throws Exception {
        when(serviceManager.findAllHeaders()).thenThrow(new FindException("mocking exception"));

        try {
            manager.findAllFromPolicy();
        } catch (final FindException e) {
            // expected
            verify(serviceManager).findAllHeaders();
            verify(serviceManager, never()).findByPrimaryKey(anyLong());
            throw e;
        }
        fail("Expected FindException");
    }

    /**
     * Should get as many as possible, skip the ones that fail.
     */
    @Test
    public void findAllFromPolicyServiceFindException() throws Exception {
        final PublishedService service1 = createPublishedService(SERVICE_A, createAssertion("a1", "group1"));
        service1.getPolicy().setXml(ModuleConstants.PORTAL_MANAGED_SERVICE_INDICATOR + " invalid xml here!!!!");
        final PublishedService service2 = createPublishedService(SERVICE_B, createAssertion("a2", "group2"));
        serviceHeaders.add(new ServiceHeader(service1));
        serviceHeaders.add(new ServiceHeader(service2));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenThrow(new FindException("mocking exception"));
        when(serviceManager.findByPrimaryKey(SERVICE_B)).thenReturn(service2);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(1, portalManagedServices.size());
        assertEquals("a2", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_B_STRING, portalManagedServices.get(0).getDescription());
        assertEquals("group2", portalManagedServices.get(0).getApiGroup());
    }

    @Test
    public void findAllFromPolicyInvalidPolicyXml() throws Exception {
        // service1 has invalid policy
        final PublishedService service1 = createPublishedService(SERVICE_A, createAssertion("a1", "group1"));
        service1.getPolicy().setXml(ModuleConstants.PORTAL_MANAGED_SERVICE_INDICATOR + " invalid xml here!!!!");
        final PublishedService service2 = createPublishedService(SERVICE_B, createAssertion("a2", "group2"));
        serviceHeaders.add(new ServiceHeader(service1));
        serviceHeaders.add(new ServiceHeader(service2));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service1);
        when(serviceManager.findByPrimaryKey(SERVICE_B)).thenReturn(service2);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(1, portalManagedServices.size());
        assertEquals("a2", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_B_STRING, portalManagedServices.get(0).getDescription());
        assertEquals("group2", portalManagedServices.get(0).getApiGroup());
    }

    @Test
    public void findAllFromPolicyNoPortalManagedServices() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, null);
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertTrue(portalManagedServices.isEmpty());
    }

    @Test
    public void findAllFromPolicyNoApiId() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createAssertion(null, "group1"));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertTrue(portalManagedServices.isEmpty());
    }

    @Test
    public void findAllFromPolicyNoApiGroup() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createAssertion("a1", null));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertEquals(1, portalManagedServices.size());
        assertEquals("a1", portalManagedServices.get(0).getName());
        assertEquals(SERVICE_A_STRING, portalManagedServices.get(0).getDescription());
        assertNull(portalManagedServices.get(0).getApiGroup());
    }

    @Test
    public void findAllFromPolicyNoApiIdOrApiGroup() throws Exception {
        final PublishedService service = createPublishedService(SERVICE_A, createAssertion(null, null));
        serviceHeaders.add(new ServiceHeader(service));

        when(serviceManager.findAllHeaders()).thenReturn(serviceHeaders);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(service);

        final List<PortalManagedService> portalManagedServices = manager.findAllFromPolicy();

        assertTrue(portalManagedServices.isEmpty());
    }

    @Test
    public void fromService() throws Exception {
        final PortalManagedService portalManagedService = manager.fromService(createPublishedService(SERVICE_A, createAssertion("a1", "group1")));

        assertEquals(SERVICE_A_STRING, portalManagedService.getDescription());
        assertEquals("a1", portalManagedService.getName());
        assertEquals("group1", portalManagedService.getApiGroup());
    }

    @Test
    public void fromServiceDisabled() throws Exception {
        assertNull(manager.fromService(createPublishedService(SERVICE_A, createDisabledAssertion("a1", "group1"))));
    }

    @Test
    public void fromServiceMultipleAssertions() throws Exception {
        final PortalManagedService portalManagedService = manager.fromService(createPublishedService(SERVICE_A, createAssertion("a1", "group1"), createAssertion("a2", "group2")));

        assertEquals(SERVICE_A_STRING, portalManagedService.getDescription());
        assertEquals("a1", portalManagedService.getName());
        assertEquals("group1", portalManagedService.getApiGroup());
    }

    @Test
    public void fromServiceMultipleAssertionsOneDisabled() throws Exception {
        final PortalManagedService portalManagedService = manager.fromService(createPublishedService(SERVICE_A, createDisabledAssertion("a1", "group1"), createAssertion("a2", "group2")));

        assertEquals(SERVICE_A_STRING, portalManagedService.getDescription());
        assertEquals("a2", portalManagedService.getName());
        assertEquals("group2", portalManagedService.getApiGroup());
    }

    @Test
    public void fromServiceMultipleAssertionsAllDisabled() throws Exception {
        assertNull(manager.fromService(createPublishedService(SERVICE_A, createDisabledAssertion("a1", "group1"), createDisabledAssertion("a2", "group2"))));
    }

    @Test
    public void fromServiceNotPortalManaged() throws Exception {
        assertNull(manager.fromService(createPublishedService(SERVICE_A)));
    }

    @Test(expected = FindException.class)
    public void fromServiceInvalidPolicy() throws Exception {
        final PublishedService publishedService = createPublishedService(SERVICE_A, createAssertion("a1", "group1"));
        String xml = publishedService.getPolicy().getXml();
        xml = xml + "INVALID LA LA LA";
        publishedService.getPolicy().setXml(xml);
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(publishedService);

        manager.fromService(publishedService);
    }

    @Test
    public void fromServiceInvalidPortalManagedPolicy() throws Exception {
        final PublishedService publishedService = createPublishedService(SERVICE_A, createAssertion("a1", "group1"));
        String xml = publishedService.getPolicy().getXml();
        publishedService.getPolicy().setXml(StringUtils.replace(xml, "L7p:ApiPortalIntegration", "L7p:ApiPortalIntegration-invalid"));
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(publishedService);

        assertNull(manager.fromService(publishedService));
    }

    @Test
    public void fromServiceNullName() throws Exception {
        final PublishedService publishedService = createPublishedService(SERVICE_A, createAssertion(null, "group1"));
        when(serviceManager.findByPrimaryKey(SERVICE_A)).thenReturn(publishedService);

        assertNull(manager.fromService(publishedService));
    }

    @Test
    public void onApplicationEventGenericEntity() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        manager.getCache().put("a1", portalManagedService);
        manager.getNameCache().put(new Goid(0,1234L), "a1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(portalManagedService, GenericEntity.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertTrue(manager.getCache().isEmpty());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotGenericEntity() throws Exception {
        manager.getCache().put("a1", createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1"));
        manager.getNameCache().put(new Goid(0,1234L), "a1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(new PublishedService(), PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotEntityInvalidationEvent() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        manager.getCache().put("a1", portalManagedService);
        manager.getNameCache().put(new Goid(0,1234L), "a1");

        manager.onApplicationEvent(new LogonEvent("", LogonEvent.LOGOFF));

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventGenericEntityWrongId() throws Exception {
        final PortalManagedService portalManagedService = createPortalManagedService(new Goid(0,1234L), "a1", SERVICE_A, "group1");
        manager.getCache().put("a1", portalManagedService);
        manager.getNameCache().put(new Goid(0,1234L), "a1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(portalManagedService, GenericEntity.class, new Goid[]{new Goid(0,5678L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    private ApiPortalIntegrationAssertion createAssertion(final String apiId, final String apiGroup) {
        final ApiPortalIntegrationAssertion assertion = new ApiPortalIntegrationAssertion();
        assertion.setApiId(apiId);
        assertion.setApiGroup(apiGroup);
        return assertion;
    }

    private ApiPortalIntegrationAssertion createDisabledAssertion(final String apiId, final String apiGroup) {
        final ApiPortalIntegrationAssertion assertion = createAssertion(apiId, apiGroup);
        assertion.setEnabled(false);
        return assertion;
    }

    private String createPolicyXml(final ApiPortalIntegrationAssertion... assertions) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">");
        if (assertions != null) {
            for (final ApiPortalIntegrationAssertion assertion : assertions) {
                stringBuilder.append("<L7p:ApiPortalIntegration>");
                if (assertion.getApiGroup() != null) {
                    stringBuilder.append("<L7p:ApiGroup stringValue=\"" + assertion.getApiGroup() + "\"/>");
                }
                if (assertion.getApiId() != null) {
                    stringBuilder.append("<L7p:ApiId stringValue=\"" + assertion.getApiId() + "\"/>");
                }
                stringBuilder.append("<L7p:PortalManagedApiFlag stringValue=\"L7p:ApiPortalManagedServiceAssertion\"/>");
                if(!assertion.isEnabled()){
                    stringBuilder.append("<L7p:Enabled booleanValue=\"false\"/>");
                }
                stringBuilder.append("</L7p:ApiPortalIntegration>");
            }
            stringBuilder.append("</wsp:All></wsp:Policy>");
        }
        return stringBuilder.toString();
    }

    private PublishedService createPublishedService(final Goid goid, final ApiPortalIntegrationAssertion... assertions) {
        final String policyXml = createPolicyXml(assertions);
        final Policy policy = new Policy(PolicyType.PRIVATE_SERVICE, "policy", policyXml, true);
        final PublishedService service = new PublishedService();
        service.setGoid(goid);
        service.setPolicy(policy);
        return service;
    }

    private PortalManagedService createPortalManagedService(final Goid goid, final String apiId, final Goid serviceGoid, final String apiGroup) {
        final PortalManagedService portalManagedService = new PortalManagedService();
        if (goid != null) {
            portalManagedService.setGoid(goid);
        }
        portalManagedService.setName(apiId);
        portalManagedService.setDescription(String.valueOf(serviceGoid));
        portalManagedService.setApiGroup(apiGroup);
        return portalManagedService;
    }
}
