package com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager;

import com.l7tech.external.assertions.apiportalintegration.ApiPortalEncassIntegrationAssertion;
import com.l7tech.external.assertions.apiportalintegration.server.ModuleConstants;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedEncass;
import com.l7tech.gateway.common.audit.LogonEvent;
import com.l7tech.objectmodel.*;
import com.l7tech.objectmodel.encass.EncapsulatedAssertionConfig;
import com.l7tech.policy.*;
import com.l7tech.server.entity.GenericEntityManager;
import com.l7tech.server.event.EntityInvalidationEvent;
import com.l7tech.server.policy.EncapsulatedAssertionConfigManager;
import com.l7tech.server.policy.PolicyManager;
import com.l7tech.server.util.ApplicationEventProxy;
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

import static junit.framework.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PortalManagedEncassManagetImplTest {
    private static final Goid ENCASS_A = new Goid(0, 1L);
    private static final String ENCASS_A_STRING = String.valueOf(ENCASS_A);
    private static final Goid ENCASS_B = new Goid(0, 2L);
    private static final String ENCASS_B_STRING = String.valueOf(ENCASS_B);
    private PortalManagedEncassManagerImpl manager;
    private List<PolicyHeader> policyHeaders;
    private List<EncapsulatedAssertionConfig> encapsulatedAssertionConfigs;
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private PolicyManager policyManager;
    @Mock
    private EncapsulatedAssertionConfigManager encapsulatedAssertionConfigManager;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager entityManager;
    @Mock
    private ApplicationEventProxy applicationEventProxy;

    @Before
    public void setup() {
        when(applicationContext.getBean("policyManager", PolicyManager.class)).thenReturn(policyManager);
        when(applicationContext.getBean("encapsulatedAssertionConfigManager", EncapsulatedAssertionConfigManager.class)).thenReturn(encapsulatedAssertionConfigManager);
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(genericEntityManager.getEntityManager(PortalManagedEncass.class)).thenReturn(entityManager);
        manager = new PortalManagedEncassManagerImpl(applicationContext);
        policyHeaders = new ArrayList<PolicyHeader>();
        encapsulatedAssertionConfigs = new ArrayList<EncapsulatedAssertionConfig>();
    }

    @Test
    public void add() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(null, "a1", ENCASS_A);
        when(entityManager.save(portalManagedEncass)).thenReturn(new Goid(0, 1234L));

        final PortalManagedEncass result = manager.add(portalManagedEncass);

        assertEquals(1, manager.getCache().size());
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertNotSame(portalManagedEncass, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        assertSame(portalManagedEncass, result);
        assertEquals(new Goid(0, 1234L), result.getGoid());
        verify(entityManager).save(portalManagedEncass);
    }

    @Test(expected = SaveException.class)
    public void addNonDefaultOid() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(null, "a1", ENCASS_A);
        portalManagedEncass.setGoid(new Goid(0, 1L));
        try {
            manager.add(portalManagedEncass);
        } catch (final SaveException e) {
            //expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).save(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected SaveException");
    }

    @Test(expected = SaveException.class)
    public void addSaveException() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(null, "a1", ENCASS_A);
        when(entityManager.save(any(PortalManagedEncass.class))).thenThrow(new SaveException("Mocking exception"));

        try {
            manager.add(portalManagedEncass);
        } catch (final SaveException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).save(portalManagedEncass);
            throw e;
        }
    }

    @Test
    public void update() throws Exception {
        final PortalManagedEncass toUpdate = createPortalManagedEncass(null, "a1", ENCASS_A);
        toUpdate.setVersion(1);
        final PortalManagedEncass existing = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_B);
        existing.setVersion(5);
        final PortalManagedEncass expected = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        expected.setVersion(5);
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);

        final PortalManagedEncass result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        assertSame(toUpdate, result);
        assertEquals(new Goid(0, 1234L), result.getGoid());
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).update(expected);
    }

    @Test(expected = ObjectNotFoundException.class)
    public void updateNotFound() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenReturn(null);

        try {
            manager.update(createPortalManagedEncass(null, "a1", ENCASS_A));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = FindException.class)
    public void updateFindException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.update(createPortalManagedEncass(null, "a1", ENCASS_A));
        } catch (final FindException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void updateInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.update(createPortalManagedEncass(null, "a1", ENCASS_A));
        } catch (final InvalidGenericEntityException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @Test(expected = ObjectNotFoundException.class)
    public void updateInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        try {
            manager.update(createPortalManagedEncass(null, "a1", ENCASS_A));
        } catch (final ObjectNotFoundException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected ObjectNotFoundException");
    }

    @Test(expected = UpdateException.class)
    public void updateUpdateException() throws Exception {
        final PortalManagedEncass toUpdate = createPortalManagedEncass(null, "a1", ENCASS_A);
        toUpdate.setVersion(1);
        final PortalManagedEncass existing = createPortalManagedEncass(new Goid(0, 1234L), "a2", ENCASS_A);
        existing.setVersion(5);
        final PortalManagedEncass expected = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        expected.setVersion(5);
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);
        doThrow(new UpdateException("mocking exception")).when(entityManager).update(any(PortalManagedEncass.class));

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
        final PortalManagedEncass toUpdate = createPortalManagedEncass(null, null, ENCASS_A);

        try {
            manager.update(toUpdate);
        } catch (final UpdateException e) {
            // expected
            assertTrue(manager.getCache().isEmpty());
            assertTrue(manager.getNameCache().isEmpty());
            verify(entityManager, never()).findByUniqueName("a1");
            verify(entityManager, never()).update(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test
    public void updateCache() throws Exception {
        final PortalManagedEncass toUpdate = createPortalManagedEncass(null, "a1", ENCASS_A);
        toUpdate.setVersion(1);
        final PortalManagedEncass existing = createPortalManagedEncass(new Goid(0, 1234L), "a2", ENCASS_A);
        existing.setVersion(5);
        final PortalManagedEncass expected = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        expected.setVersion(5);
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);

        manager.getCache().put("a1", existing);
        manager.getNameCache().put(new Goid(0, 1234L), "a1");

        final PortalManagedEncass result = manager.update(toUpdate);

        assertEquals(1, manager.getCache().size());
        // cache should be updated
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        assertSame(toUpdate, result);
        assertEquals(new Goid(0, 1234L), result.getGoid());
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).update(expected);
    }

    /**
     * If not found - should add.
     */
    @Test
    public void addOrUpdateNotFound() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(null, "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenReturn(null);
        when(entityManager.save(any(PortalManagedEncass.class))).thenReturn(new Goid(0, 1234L));

        final PortalManagedEncass result = manager.addOrUpdate(portalManagedEncass);

        assertEquals(1, manager.getCache().size());
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertNotSame(portalManagedEncass, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        assertEquals(cached, result);
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).save(portalManagedEncass);
        verify(entityManager, never()).update(any(PortalManagedEncass.class));
    }

    /**
     * If found - should update.
     */
    @Test
    public void addOrUpdateFound() throws Exception {
        final PortalManagedEncass toUpdate = createPortalManagedEncass(null, "a1", ENCASS_A);
        final PortalManagedEncass existing = createPortalManagedEncass(new Goid(0, 1234L), "a2", ENCASS_A);
        final PortalManagedEncass expected = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenReturn(existing);

        final PortalManagedEncass result = manager.addOrUpdate(toUpdate);

        assertEquals(1, manager.getCache().size());
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertNotSame(toUpdate, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        assertEquals(cached, result);
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).update(expected);
        verify(entityManager, never()).save(any(PortalManagedEncass.class));
    }

    @Test(expected = FindException.class)
    public void addOrUpdateFindException() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(null, "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenThrow(new FindException("mocking exception"));

        try {
            manager.addOrUpdate(portalManagedEncass);
        } catch (final FindException e) {
            verify(entityManager).findByUniqueName("a1");
            throw e;
        }
        fail("expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void addOrUpdateInvalidGenericEntityException() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(null, "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        try {
            manager.addOrUpdate(portalManagedEncass);
        } catch (final InvalidGenericEntityException e) {
            verify(entityManager).findByUniqueName("a1");
            throw e;
        }
        fail("expected InvalidGenericEntityException");
    }

    @Test
    public void addOrUpdateInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(null, "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));
        when(entityManager.save(any(PortalManagedEncass.class))).thenReturn(new Goid(0, 1234L));

        final PortalManagedEncass result = manager.addOrUpdate(portalManagedEncass);

        assertEquals(1, manager.getCache().size());
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertNotSame(portalManagedEncass, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        assertEquals(cached, result);
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).save(portalManagedEncass);
        verify(entityManager, never()).update(any(PortalManagedEncass.class));
    }

    @Test
    public void delete() throws Exception {
        final PortalManagedEncass found = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
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
            verify(entityManager, never()).delete(any(PortalManagedEncass.class));
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
            verify(entityManager, never()).delete(any(PortalManagedEncass.class));
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
            verify(entityManager, never()).delete(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

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
            verify(entityManager, never()).delete(any(PortalManagedEncass.class));
            throw e;
        }
        fail("Expected InvalidGenericEntityException");
    }

    @Test(expected = DeleteException.class)
    public void deleteDeleteException() throws Exception {
        final PortalManagedEncass found = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenReturn(found);
        doThrow(new DeleteException("mocking exception")).when(entityManager).delete(any(PortalManagedEncass.class));

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
        final PortalManagedEncass found = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenReturn(found);
        manager.getCache().put("a1", found);
        manager.getNameCache().put(new Goid(0, 1234L), "a1");

        manager.delete("a1");

        assertTrue(manager.getCache().isEmpty());
        // named cache entry should not be removed
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        verify(entityManager).findByUniqueName("a1");
        verify(entityManager).delete(found);
    }

    @Test
    public void find() throws Exception {
        final PortalManagedEncass found = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        when(entityManager.findByUniqueName("a1")).thenReturn(found);

        final PortalManagedEncass portalManagedEncass = manager.find("a1");

        assertEquals(new Goid(0, 1234L), portalManagedEncass.getGoid());
        assertEquals("a1", portalManagedEncass.getName());
        assertEquals(ENCASS_A_STRING, portalManagedEncass.getEncassId());
        assertEquals(1, manager.getCache().size());
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertNotSame(found, cached);
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
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

    @Test
    public void findInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("a1")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        final PortalManagedEncass portalManagedEncass = manager.find("a1");

        assertNull(portalManagedEncass);
        assertTrue(manager.getCache().isEmpty());
        assertTrue(manager.getNameCache().isEmpty());
        verify(entityManager).findByUniqueName("a1");
    }

    @Test
    public void findFromCache() throws Exception {
        manager.getCache().put("a1", createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A));
        manager.getNameCache().put(new Goid(0, 1234L), "a1");

        final PortalManagedEncass portalManagedEncass = manager.find("a1");

        assertEquals(new Goid(0, 1234L), portalManagedEncass.getGoid());
        assertEquals("a1", portalManagedEncass.getName());
        assertEquals(ENCASS_A_STRING, portalManagedEncass.getEncassId());
        assertEquals(1, manager.getCache().size());
        final PortalManagedEncass cached = (PortalManagedEncass) manager.getCache().get("a1");
        assertEquals(new Goid(0, 1234L), cached.getGoid());
        assertEquals("a1", cached.getName());
        assertEquals(ENCASS_A_STRING, cached.getEncassId());
        assertEquals(1, manager.getNameCache().size());
        assertEquals("a1", manager.getNameCache().get(new Goid(0, 1234L)));
        verify(entityManager, never()).findByUniqueName("a1");
    }

    @Test(expected = IllegalStateException.class)
    public void findFromCacheReadOnly() throws Exception {
        manager.getCache().put("a1", createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A));
        manager.getNameCache().put(new Goid(0, 1234L), "a1");

        final PortalManagedEncass portalManagedEncass = manager.find("a1");

        assertEquals(new Goid(0, 1234L), portalManagedEncass.getGoid());
        verify(entityManager, never()).findByUniqueName("a1");
        try {
            portalManagedEncass.setEncassId("readonly?");
        } catch (final IllegalStateException e) {
            // expected
            throw e;
        }
        fail("Expected IllegalStateException");
    }

    @Test
    public void findAll() throws Exception {
        when(entityManager.findAll()).thenReturn(Arrays.asList(createPortalManagedEncass(new Goid(0, 1L), "a1", new Goid(0, 1L)),
                createPortalManagedEncass(new Goid(0, 2L), "a2", new Goid(0, 2L))));

        final List<PortalManagedEncass> portalManagedEncasss = manager.findAll();

        assertEquals(2, portalManagedEncasss.size());
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
    public void findAllFromEncassSingleEncass() throws Exception {
        final EncapsulatedAssertionConfig encass = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion());
        encapsulatedAssertionConfigs.add(encass);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        final List<PortalManagedEncass> portalManagedEncasss = manager.findAllFromEncass();

        assertEquals(1, portalManagedEncasss.size());
        assertEquals("a1", portalManagedEncasss.get(0).getName());
        assertEquals(ENCASS_A_STRING, portalManagedEncasss.get(0).getEncassId());
    }

    @Test
    public void findAllFromPolicySingleServiceDisabled() throws Exception {
        final EncapsulatedAssertionConfig encass = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createDisabledAssertion());
        encapsulatedAssertionConfigs.add(encass);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        assertTrue(manager.findAllFromEncass().isEmpty());
    }

    @Test
    public void findAllFromPolicySingleServiceMultipleAssertions() throws Exception {
        final EncapsulatedAssertionConfig encass = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion(), createAssertion());
        encapsulatedAssertionConfigs.add(encass);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        final List<PortalManagedEncass> portalManagedEncasss = manager.findAllFromEncass();

        assertEquals(1, portalManagedEncasss.size());
        assertEquals("a1", portalManagedEncasss.get(0).getName());
        assertEquals(ENCASS_A_STRING, portalManagedEncasss.get(0).getEncassId());
    }

    @Test
    public void findAllFromPolicySingleServiceMultipleAssertionsOneDisabled() throws Exception {
        final EncapsulatedAssertionConfig encass = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createDisabledAssertion(), createAssertion());
        encapsulatedAssertionConfigs.add(encass);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        final List<PortalManagedEncass> portalManagedEncasss = manager.findAllFromEncass();

        assertEquals(1, portalManagedEncasss.size());
        assertEquals("a1", portalManagedEncasss.get(0).getName());
        assertEquals(ENCASS_A_STRING, portalManagedEncasss.get(0).getEncassId());
    }

    @Test
    public void findAllFromPolicySingleServiceMultipleAssertionsAllDisabled() throws Exception {
        final EncapsulatedAssertionConfig encass = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createDisabledAssertion(), createDisabledAssertion());
        encapsulatedAssertionConfigs.add(encass);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        assertTrue(manager.findAllFromEncass().isEmpty());
    }

    @Test
    public void findAllFromPolicyMultipleServices() throws Exception {
        final EncapsulatedAssertionConfig encass1 = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion());
        final EncapsulatedAssertionConfig encass2 = createEncapsulatedAssertionConfig(ENCASS_B, "a2", createAssertion());
        encapsulatedAssertionConfigs.add(encass1);
        encapsulatedAssertionConfigs.add(encass2);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        final List<PortalManagedEncass> portalManagedEncasss = manager.findAllFromEncass();

        assertEquals(2, portalManagedEncasss.size());
        assertEquals("a1", portalManagedEncasss.get(0).getName());
        assertEquals(ENCASS_A_STRING, portalManagedEncasss.get(0).getEncassId());
        assertEquals("a2", portalManagedEncasss.get(1).getName());
        assertEquals(ENCASS_B_STRING, portalManagedEncasss.get(1).getEncassId());
    }

    @Test(expected = FindException.class)
    public void findAllFromPolicyHeaderFindException() throws Exception {
        when(encapsulatedAssertionConfigManager.findAll()).thenThrow(new FindException("mocking exception"));

        try {
            manager.findAllFromEncass();
        } catch (final FindException e) {
            // expected
            verify(encapsulatedAssertionConfigManager).findAll();
            verify(encapsulatedAssertionConfigManager, never()).findByPrimaryKey(any(Goid.class));
            throw e;
        }
        fail("Expected FindException");
    }

    /**
     * Should get as many as possible, skip the ones that fail.
     */
    @Test
    public void findAllFromPolicyServiceFindException() throws Exception {
        final EncapsulatedAssertionConfig encass1 = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion());
        encass1.getPolicy().setXml(ModuleConstants.PORTAL_MANAGED_ENCASS_INDICATOR + " invalid xml here!!!!");
        final EncapsulatedAssertionConfig encass2 = createEncapsulatedAssertionConfig(ENCASS_B, "a2", createAssertion());
        encapsulatedAssertionConfigs.add(encass1);
        encapsulatedAssertionConfigs.add(encass2);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        final List<PortalManagedEncass> portalManagedEncasss = manager.findAllFromEncass();

        assertEquals(1, portalManagedEncasss.size());
        assertEquals("a2", portalManagedEncasss.get(0).getName());
        assertEquals(ENCASS_B_STRING, portalManagedEncasss.get(0).getEncassId());
    }

    @Test
    public void findAllFromPolicyNoPortalManagedEncasses() throws Exception {
        final EncapsulatedAssertionConfig encass1 = createEncapsulatedAssertionConfig(ENCASS_A, "a1", null);
        encapsulatedAssertionConfigs.add(encass1);

        when(encapsulatedAssertionConfigManager.findAll()).thenReturn(encapsulatedAssertionConfigs);

        final List<PortalManagedEncass> portalManagedEncasss = manager.findAllFromEncass();

        assertTrue(portalManagedEncasss.isEmpty());
    }

    @Test
    public void fromService() throws Exception {
        final PortalManagedEncass portalManagedEncass = manager.fromEncass(createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion()));

        assertEquals(ENCASS_A_STRING, portalManagedEncass.getEncassId());
        assertEquals("a1", portalManagedEncass.getName());
        assertFalse(portalManagedEncass.getHasRouting());
    }

    @Test
    public void fromServiceWithDetails() throws Exception {
        final PortalManagedEncass portalManagedEncass = manager.fromEncass(createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion(), true));

        assertEquals(ENCASS_A_STRING, portalManagedEncass.getEncassId());
        assertEquals("a1", portalManagedEncass.getName());
        assertTrue(portalManagedEncass.getHasRouting());
    }

    @Test
    public void fromServiceDisabled() throws Exception {
        assertNull(manager.fromEncass(createEncapsulatedAssertionConfig(ENCASS_A, "a1", createDisabledAssertion())));
    }

    @Test
    public void fromServiceMultipleAssertions() throws Exception {
        final PortalManagedEncass portalManagedEncass = manager.fromEncass(createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion(), createAssertion()));

        assertEquals(ENCASS_A_STRING, portalManagedEncass.getEncassId());
        assertEquals("a1", portalManagedEncass.getName());
        assertFalse(portalManagedEncass.getHasRouting());
    }

    @Test
    public void fromServiceMultipleAssertionsOneDisabled() throws Exception {
        final PortalManagedEncass portalManagedEncass = manager.fromEncass(createEncapsulatedAssertionConfig(ENCASS_A, "a1", createDisabledAssertion(), createAssertion()));

        assertEquals(ENCASS_A_STRING, portalManagedEncass.getEncassId());
        assertEquals("a1", portalManagedEncass.getName());
    }

    @Test
    public void fromServiceMultipleAssertionsAllDisabled() throws Exception {
        assertNull(manager.fromEncass(createEncapsulatedAssertionConfig(ENCASS_A, "a1", createDisabledAssertion(), createDisabledAssertion())));
    }

    @Test
    public void fromServiceNotPortalManaged() throws Exception {
        assertNull(manager.fromEncass(createEncapsulatedAssertionConfig(ENCASS_A, "a1")));
    }

    @Test(expected = FindException.class)
    public void fromServiceInvalidPolicy() throws Exception {
        final EncapsulatedAssertionConfig encass = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion());
        String xml = encass.getPolicy().getXml();
        xml = xml + "INVALID LA LA LA";
        encass.getPolicy().setXml(xml);
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(ENCASS_A)).thenReturn(encass);

        manager.fromEncass(encass);
    }

    @Test
    public void fromServiceInvalidPortalManagedPolicy() throws Exception {
        final EncapsulatedAssertionConfig encass = createEncapsulatedAssertionConfig(ENCASS_A, "a1", createAssertion());
        String xml = encass.getPolicy().getXml();
        encass.getPolicy().setXml(StringUtils.replace(xml, "L7p:ApiPortalEncassIntegration", "L7p:ApiPortalEncassIntegration-invalid"));
        when(encapsulatedAssertionConfigManager.findByPrimaryKey(ENCASS_A)).thenReturn(encass);

        assertNull(manager.fromEncass(encass));
    }

    @Test
    public void onApplicationEventGenericEntity() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        manager.getCache().put("a1", portalManagedEncass);
        manager.getNameCache().put(new Goid(0, 1234L), "a1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(portalManagedEncass, GenericEntity.class, new Goid[]{new Goid(0, 1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertTrue(manager.getCache().isEmpty());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotGenericEntity() throws Exception {
        manager.getCache().put("a1", createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A));
        manager.getNameCache().put(new Goid(0, 1234L), "a1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(new EncapsulatedAssertionConfig(), EncapsulatedAssertionConfig.class, new Goid[]{new Goid(0, 1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotEntityInvalidationEvent() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        manager.getCache().put("a1", portalManagedEncass);
        manager.getNameCache().put(new Goid(0, 1234L), "a1");

        manager.onApplicationEvent(new LogonEvent("", LogonEvent.LOGOFF));

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    @Test
    public void onApplicationEventGenericEntityWrongId() throws Exception {
        final PortalManagedEncass portalManagedEncass = createPortalManagedEncass(new Goid(0, 1234L), "a1", ENCASS_A);
        manager.getCache().put("a1", portalManagedEncass);
        manager.getNameCache().put(new Goid(0, 1234L), "a1");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(portalManagedEncass, GenericEntity.class, new Goid[]{new Goid(0,5678L)}, new char[]{EntityInvalidationEvent.CREATE});

        manager.onApplicationEvent(event);

        assertEquals(1, manager.getCache().size());
        assertEquals(1, manager.getNameCache().size());
    }

    private ApiPortalEncassIntegrationAssertion createDisabledAssertion() {
        final ApiPortalEncassIntegrationAssertion assertion = createAssertion();
        assertion.setEnabled(false);
        return assertion;
    }

    private ApiPortalEncassIntegrationAssertion createAssertion() {
        final ApiPortalEncassIntegrationAssertion assertion = new ApiPortalEncassIntegrationAssertion();
        return assertion;
    }

    private EncapsulatedAssertionConfig createEncapsulatedAssertionConfig(final Goid goid, final String guid,
                                                                          final ApiPortalEncassIntegrationAssertion assertion,
                                                                          final boolean withRoutingAssertion) {
        final Policy policy = createPolicy(withRoutingAssertion, assertion);
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGoid(goid);
        encapsulatedAssertionConfig.setGuid(guid);
        encapsulatedAssertionConfig.setPolicy(policy);
        return encapsulatedAssertionConfig;
    }

    private EncapsulatedAssertionConfig createEncapsulatedAssertionConfig(final Goid goid, final String guid, final ApiPortalEncassIntegrationAssertion... assertions) {
        final Policy policy = createPolicy(assertions);
        final EncapsulatedAssertionConfig encapsulatedAssertionConfig = new EncapsulatedAssertionConfig();
        encapsulatedAssertionConfig.setGoid(goid);
        encapsulatedAssertionConfig.setGuid(guid);
        encapsulatedAssertionConfig.setPolicy(policy);
        return encapsulatedAssertionConfig;
    }

    private Policy createPolicy(final boolean withRoutingAssertion, final ApiPortalEncassIntegrationAssertion... assertions) {
        final String policyXml = createPolicyXml(withRoutingAssertion, assertions);
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "policy", policyXml, true);
        return policy;
    }

    private Policy createPolicy(final ApiPortalEncassIntegrationAssertion... assertions) {
        final String policyXml = createPolicyXml(false, assertions);
        final Policy policy = new Policy(PolicyType.INCLUDE_FRAGMENT, "policy", policyXml, true);
        return policy;
    }

    private PortalManagedEncass createPortalManagedEncass(final Goid goid, final String encassGuid, final Goid encassId) {
        final PortalManagedEncass portalManagedEncass = new PortalManagedEncass();
        if (goid != null) {
            portalManagedEncass.setGoid(goid);
        }
        portalManagedEncass.setEncassGuid(encassGuid);
        portalManagedEncass.setEncassId(String.valueOf(encassId));
        return portalManagedEncass;
    }

    private String createPolicyXml(final boolean withRoutingAssertion, final ApiPortalEncassIntegrationAssertion... assertions) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<wsp:Policy xmlns:L7p=\"http://www.layer7tech.com/ws/policy\" xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2002/12/policy\">\n" +
                "    <wsp:All wsp:Usage=\"Required\">");
        if (assertions != null) {
            for (final ApiPortalEncassIntegrationAssertion assertion : assertions) {
                stringBuilder.append("<L7p:ApiPortalEncassIntegration>");
                if (!assertion.isEnabled()) {
                    stringBuilder.append("<L7p:Enabled booleanValue=\"false\"/>");
                }
                stringBuilder.append("</L7p:ApiPortalEncassIntegration>");
            }
        }
        if (withRoutingAssertion) {
            stringBuilder.append("<L7p:HttpRoutingAssertion>");
            stringBuilder.append("<L7p:AssertionComment assertionComment=\"included\">");
            stringBuilder.append("<L7p:Properties mapValue=\"included\">");
            stringBuilder.append("<L7p:entry>");
            stringBuilder.append("<L7p:key stringValue=\"LEFT.COMMENT\"/>");
            stringBuilder.append("<L7p:value stringValue=\"Your API URL\"/>");
            stringBuilder.append("</L7p:entry>");
            stringBuilder.append("</L7p:Properties>");
            stringBuilder.append("</L7p:AssertionComment>");
            stringBuilder.append("<L7p:ProtectedServiceUrl stringValue=\"http://soapui-service.com/routingUrl\"/>");
            stringBuilder.append("<L7p:RequestHeaderRules httpPassthroughRuleSet=\"included\">");
            stringBuilder.append("<L7p:Rules httpPassthroughRules=\"included\">");
            stringBuilder.append("<L7p:item httpPassthroughRule=\"included\">");
            stringBuilder.append("<L7p:Name stringValue=\"Cookie\"/>");
            stringBuilder.append("</L7p:item>");
            stringBuilder.append("<L7p:item httpPassthroughRule=\"included\">");
            stringBuilder.append("<L7p:Name stringValue=\"SOAPAction\"/>");
            stringBuilder.append("</L7p:item>");
            stringBuilder.append("</L7p:Rules>");
            stringBuilder.append("</L7p:RequestHeaderRules>");
            stringBuilder.append("<L7p:RequestParamRules httpPassthroughRuleSet=\"included\">");
            stringBuilder.append("<L7p:ForwardAll booleanValue=\"true\"/>");
            stringBuilder.append("<L7p:Rules httpPassthroughRules=\"included\"/>");
            stringBuilder.append("</L7p:RequestParamRules>");
            stringBuilder.append("<L7p:ResponseHeaderRules httpPassthroughRuleSet=\"included\">");
            stringBuilder.append("<L7p:Rules httpPassthroughRules=\"included\">");
            stringBuilder.append("<L7p:item httpPassthroughRule=\"included\">");
            stringBuilder.append("<L7p:Name stringValue=\"Set-Cookie\"/>");
            stringBuilder.append("</L7p:item>");
            stringBuilder.append("</L7p:Rules>");
            stringBuilder.append("</L7p:ResponseHeaderRules>");
            stringBuilder.append("</L7p:HttpRoutingAssertion>");
        }
        stringBuilder.append("</wsp:All></wsp:Policy>");
        return stringBuilder.toString();
    }
}
