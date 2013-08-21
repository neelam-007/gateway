package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

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
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyManagerTest {
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager<ApiKey, GenericEntityHeader> entityManager;
    @Mock
    private ApplicationEventProxy applicationEventProxy;

    Map<String, String> serviceIds = new HashMap<String, String>();
    ApiKeyManager apiKeyManager;
    ApplicationListener<ApplicationEvent> apiKeyManagerEventListener;

    @Before
    public void setup() {
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(genericEntityManager.getEntityManager(ApiKey.class)).thenReturn(entityManager);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertNull("listener should only be added once", apiKeyManagerEventListener);
                apiKeyManagerEventListener = (ApplicationListener) invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(applicationEventProxy).addApplicationListener(any(ApplicationListener.class));
        apiKeyManager = new ApiKeyManager(applicationContext);
        verify(applicationEventProxy).addApplicationListener(any(ApplicationListener.class));
        serviceIds.put("23244", "plan1");
        serviceIds.put("857827", "plan2");
    }

    @Test
    public void testAddApiKey() throws Exception {
        when(entityManager.save(isA(ApiKey.class))).thenReturn(new Goid(0,3L));

        ApiKey key = new ApiKey();
        key.setName("fookey");
        key.setSecret("sekrit");
        key.setServiceIds(serviceIds);
        key.setStatus("somestatus");
        key.setVersion(7);
        key.setLabel("somelabel");
        key.setOauthCallbackUrl("someOauthCallbackUrl");
        key.setOauthScope("someOauthScope");
        key.setOauthType("someOauthType");
        key.setPlatform("somePlatform");
        final ApiKey result = apiKeyManager.add(key);

        verify(entityManager).save(isA(ApiKey.class));
        key.setGoid(new Goid(0, 3L));
        assertCacheContains(key);
        assertMatches(key, result);
    }

    @Test(expected = SaveException.class)
    public void testAddApiKey_alreadySavedBefore() throws Exception {
        ApiKey key = new ApiKey();
        key.setGoid(new Goid(0, 88));
        key.setName("fookey");
        apiKeyManager.add(key);
    }

    @Test(expected = DuplicateObjectException.class)
    public void testAddApiKey_nameAlreadyInUse() throws Exception {
        when(entityManager.save(isA(ApiKey.class))).thenThrow(new DuplicateObjectException());

        ApiKey key = new ApiKey();
        key.setGoid(new Goid(0,-1)); // Try to save a new one with colliding name
        key.setName("fookey");
        apiKeyManager.add(key);
    }

    @Test
    public void testUpdateApiKey() throws Exception {
        ApiKey existing = new ApiKey();
        existing.setName("fookey");
        existing.setSecret("oldsecret");
        existing.setServiceIds(serviceIds);
        existing.setStatus("prevstatus");
        existing.setVersion(2);
        existing.setGoid(new Goid(0, 1234L));
        when(entityManager.findByUniqueName(anyString())).thenReturn(existing);

        ApiKey key = new ApiKey();
        key.setName("fookey");
        key.setSecret("sekrit");
        key.setServiceIds(serviceIds);
        key.setStatus("somestatus");
        key.setGoid(new Goid(0, -1L));

        final ApiKey result = apiKeyManager.update(key);

        // merged key should have same oid as existing but updated fields
        ApiKey mergedkey = new ApiKey();
        mergedkey.setName("fookey");
        mergedkey.setSecret("sekrit");
        mergedkey.setServiceIds(serviceIds);
        mergedkey.setStatus("somestatus");
        mergedkey.setVersion(2);
        mergedkey.setGoid(new Goid(0, 1234L));

        verify(entityManager).findByUniqueName("fookey");
        verify(entityManager).update(argThat(new MatchesApiKey(mergedkey)));
        assertMatches(mergedkey, result);

    }

    @Test(expected = ObjectNotFoundException.class)
    public void testUpdateApiKey_notFound() throws Exception {
        ApiKey key = new ApiKey();
        key.setName("fookey");
        apiKeyManager.update(key);
    }

    @Test(expected = FindException.class)
    public void testUpdateApiKeyFindException() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new FindException("mocking exception"));
        ApiKey key = new ApiKey();
        key.setName("fookey");

        apiKeyManager.update(key);
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void testUpdateApiKeyInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("mocking exception"));
        ApiKey key = new ApiKey();
        key.setName("fookey");

        apiKeyManager.update(key);
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void testUpdateApiKeyInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));
        ApiKey key = new ApiKey();
        key.setName("fookey");

        apiKeyManager.update(key);
    }

    @Test
    public void testRemoveApiKey() throws Exception {
        ApiKey existing = makeExisting();
        when(entityManager.findByUniqueName("fookey")).thenReturn(existing);
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(new Goid(0,7L), "fookey");

        apiKeyManager.delete("fookey");

        verify(entityManager).delete(existing);
        assertTrue("entries are immediately removed from the cache when they are removed on this node", apiKeyManager.getCache().isEmpty());
        assertTrue("oids are permitted to remain in name cache for the full cleanup period, to avoid races", apiKeyManager.getNameCache().containsKey(new Goid(0,7L)));
    }

    @Test(expected = ObjectNotFoundException.class)
    public void testRemoveApiKey_notFound() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenReturn(null);
        apiKeyManager.delete("fookey");
    }

    @Test(expected = FindException.class)
    public void testRemoveApiKeyFindException() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new FindException("mocking exception"));

        apiKeyManager.delete("fookey");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void testRemoveApiKeyInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("mocking exception"));

        apiKeyManager.delete("fookey");
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void testRemoveApiKeyInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));

        apiKeyManager.delete("fookey");
    }

    @Test
    public void testGetApiKey() throws Exception {
        ApiKey existing = makeExisting();
        when(entityManager.findByUniqueName("fookey")).thenReturn(existing);
        assertCacheDoesNotContain(existing, true);

        ApiKey found = apiKeyManager.find("fookey");

        verify(entityManager).findByUniqueName("fookey");
        assertMatches(existing, found);
        assertCacheContains(found);

    }

    @Test(expected = FindException.class)
    public void testGetApiKeyFindException() throws Exception {
        ApiKey existing = makeExisting();
        when(entityManager.findByUniqueName("fookey")).thenThrow(new FindException("mocking exception"));
        assertCacheDoesNotContain(existing, true);

        try {
            apiKeyManager.find("fookey");
            fail("expected FindException");
        } catch (final FindException e) {
            verify(entityManager).findByUniqueName("fookey");
            throw e;
        }
        fail("expected FindException");
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void testGetApiKeyInvalidGenericEntityException() throws Exception {
        ApiKey existing = makeExisting();
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("mocking exception"));
        assertCacheDoesNotContain(existing, true);

        try {
            apiKeyManager.find("fookey");
            fail("expected FindException");
        } catch (final InvalidGenericEntityException e) {
            verify(entityManager).findByUniqueName("fookey");
            throw e;
        }
        fail("expected InvalidGenericEntityException");
    }

    @BugNumber(12334)
    @Test
    public void testGetApiKeyInvalidGenericEntityExceptionNotExpectedClass() throws Exception {
        ApiKey existing = makeExisting();
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));
        assertCacheDoesNotContain(existing, true);

        assertNull(apiKeyManager.find("fookey"));
        verify(entityManager).findByUniqueName("fookey");
    }

    @Test
    public void testGetApiKey_fromCache() throws Exception {
        ApiKey existing = makeExisting();
        apiKeyManager.getCache().put(existing.getName(), existing);
        apiKeyManager.getNameCache().put(existing.getGoid(), existing.getName());

        ApiKey found = apiKeyManager.find("fookey");

        verifyZeroInteractions(entityManager);
        assertMatches(existing, found);
        assertCacheContains(found);
    }

    @Test
    public void testGetApiKey_notFound() throws Exception {
        when(entityManager.findByUniqueName(anyString())).thenReturn(null);

        ApiKey found = apiKeyManager.find("keythatisnotfound");
        assertNull(found);
    }

    @Test
    public void testCacheEvictedWhenEntityUpdated() throws Exception {
        ApiKey existing = makeExisting();
        apiKeyManager.getCache().put(existing.getName(), existing);
        apiKeyManager.getNameCache().put(existing.getGoid(), existing.getName());

        ApiKey decoy = makeExisting();
        decoy.setName("otherkeyunrelated");
        decoy.setGoid(new Goid(0, 9833));
        apiKeyManager.getCache().put(decoy.getName(), decoy);
        apiKeyManager.getNameCache().put(decoy.getGoid(), decoy.getName());

        // Event should be ignored (oid doesn't match anything in cache)
        apiKeyManagerEventListener.onApplicationEvent(new EntityInvalidationEvent(new Object(), GenericEntity.class, new Goid[]{new Goid(0,222L)}, new char[]{'U'}));
        assertCacheContains(existing);
        assertCacheContains(decoy);

        // Event should cause cache entry to be removed (matches)
        apiKeyManagerEventListener.onApplicationEvent(new EntityInvalidationEvent(new Object(), GenericEntity.class, new Goid[]{existing.getGoid()}, new char[]{'U'}));
        assertCacheDoesNotContain(existing, false);
        assertCacheContains(decoy);
    }

    @Test
    public void onApplicationEventGenericEntity() throws Exception {
        ApiKey existing = makeExisting();
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(new Goid(0,1234L), "fookey");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(existing, GenericEntity.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});

        apiKeyManager.onApplicationEvent(event);

        assertTrue(apiKeyManager.getCache().isEmpty());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotGenericEntity() throws Exception {
        apiKeyManager.getCache().put("fookey", makeExisting());
        apiKeyManager.getNameCache().put(new Goid(0,1234L), "fookey");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(new PublishedService(), PublishedService.class, new Goid[]{new Goid(0,1234L)}, new char[]{EntityInvalidationEvent.CREATE});;

        apiKeyManager.onApplicationEvent(event);

        assertEquals(1, apiKeyManager.getCache().size());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotEntityInvalidationEvent() throws Exception {
        ApiKey existing = makeExisting();
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(new Goid(0,1234L), "fookey");

        apiKeyManager.onApplicationEvent(new LogonEvent("", LogonEvent.LOGOFF));

        assertEquals(1, apiKeyManager.getCache().size());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

    @Test
    public void onApplicationEventGenericEntityWrongId() throws Exception {
        ApiKey existing = makeExisting();
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(new Goid(0,1234L), "fookey");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(existing, GenericEntity.class, new Goid[]{new Goid(0,5678L)}, new char[]{EntityInvalidationEvent.CREATE});

        apiKeyManager.onApplicationEvent(event);

        assertEquals(1, apiKeyManager.getCache().size());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

     private void assertCacheDoesNotContain(ApiKey key, boolean checkNameCache) {
        assertFalse(apiKeyManager.getCache().containsKey(key.getName()));
        if (checkNameCache)
            assertFalse(apiKeyManager.getNameCache().containsKey(key.getGoid()));
    }

    private void assertCacheContains(ApiKey key) {
        assertTrue(apiKeyManager.getCache().containsKey(key.getName()));
        assertTrue(apiKeyManager.getNameCache().containsKey(key.getGoid()));
        assertEquals(key.getName(), apiKeyManager.getNameCache().get(key.getGoid()));
        ApiKey cached = (ApiKey) apiKeyManager.getCache().get(key.getName());
        assertMatches(key, cached);
    }

    private void assertMatches(ApiKey expected, ApiKey actual) {
        assertEquals(expected.getGoid(), actual.getGoid());
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getSecret(), actual.getSecret());
        assertEquals(expected.getServiceIds(), actual.getServiceIds());
        assertEquals(expected.getStatus(), actual.getStatus());
    }

    private ApiKey makeExisting() {
        ApiKey existing = new ApiKey();
        existing.setGoid(new Goid(0,7L));
        existing.setName("fookey");
        existing.setSecret("oldsecret");
        existing.setServiceIds(serviceIds);
        existing.setStatus("prevstatus");
        existing.setVersion(6);
        existing.setLabel("prevlabel");
        existing.setOauthCallbackUrl("prevOauthCallbackUrl");
        existing.setOauthScope("prevOauthScope");
        existing.setOauthType("prevOauthType");
        existing.setPlatform("prevPlatform");
        return existing;
    }

    /**
     * Matcher for ApiKey that looks ensures fields are equal.
     */
    class MatchesApiKey extends ArgumentMatcher<ApiKey> {
        private final ApiKey toMatch;

        public MatchesApiKey(final ApiKey toMatch) {
            this.toMatch = toMatch;
        }

        @Override
        public boolean matches(final Object o) {
            final ApiKey key = (ApiKey) o;
            if (!toMatch.getGoid().equals(key.getGoid())) {
                return false;
            }
            if (!toMatch.getName().equals(key.getName())) {
                return false;
            }
            if (!toMatch.getSecret().equals(key.getSecret())) {
                return false;
            }
            if (toMatch.getVersion() != key.getVersion()) {
                return false;
            }
            if (toMatch.getServiceIds().size() != key.getServiceIds().size()) {
                return false;
            }
            if (!toMatch.getServiceIds().entrySet().containsAll(key.getServiceIds().entrySet())) {
                return false;
            }
            return true;
        }
    }
}
