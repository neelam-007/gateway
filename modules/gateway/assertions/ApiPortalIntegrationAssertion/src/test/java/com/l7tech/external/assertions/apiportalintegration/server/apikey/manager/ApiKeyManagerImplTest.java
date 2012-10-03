package com.l7tech.external.assertions.apiportalintegration.server.apikey.manager;

import com.l7tech.external.assertions.apiportalintegration.server.ApiKeyData;
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
import static org.mockito.Mockito.*;

/**
 * Unit test for ApiKeyManagerImpl.
 */
@RunWith(MockitoJUnitRunner.class)
public class ApiKeyManagerImplTest {
    @Mock
    private ApplicationContext applicationContext;
    @Mock
    private GenericEntityManager genericEntityManager;
    @Mock
    private EntityManager<ApiKeyData, GenericEntityHeader> entityManager;
    @Mock
    private ApplicationEventProxy applicationEventProxy;

    Map<String, String> serviceIds = new HashMap<String, String>();
    ApiKeyManagerImpl apiKeyManager;
    ApplicationListener<ApplicationEvent> apiKeyManagerEventListener;

    @Before
    public void setup() {
        when(applicationContext.getBean("genericEntityManager", GenericEntityManager.class)).thenReturn(genericEntityManager);
        when(applicationContext.getBean("applicationEventProxy", ApplicationEventProxy.class)).thenReturn(applicationEventProxy);
        when(genericEntityManager.getEntityManager(ApiKeyData.class)).thenReturn(entityManager);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                assertNull("listener should only be added once", apiKeyManagerEventListener);
                apiKeyManagerEventListener = (ApplicationListener) invocationOnMock.getArguments()[0];
                return null;
            }
        }).when(applicationEventProxy).addApplicationListener(any(ApplicationListener.class));
        apiKeyManager = new ApiKeyManagerImpl(applicationContext);
        verify(applicationEventProxy).addApplicationListener(any(ApplicationListener.class));
        serviceIds.put("23244", "plan1");
        serviceIds.put("857827", "plan2");
    }

    private void assertCacheDoesNotContain(ApiKeyData data, boolean checkNameCache) {
        assertFalse(apiKeyManager.getCache().containsKey(data.getKey()));
        if (checkNameCache)
            assertFalse(apiKeyManager.getNameCache().containsKey(data.getOidAsLong()));
    }

    private void assertCacheContains(ApiKeyData data) {
        assertTrue(apiKeyManager.getCache().containsKey(data.getKey()));
        assertTrue(apiKeyManager.getNameCache().containsKey(data.getOidAsLong()));
        assertEquals(data.getKey(), apiKeyManager.getNameCache().get(data.getOidAsLong()));
        ApiKeyData cached = (ApiKeyData) apiKeyManager.getCache().get(data.getKey());
        assertMatches(data, cached);
    }

    private void assertMatches(ApiKeyData expected, ApiKeyData actual) {
        assertEquals(expected.getOid(), actual.getOid());
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getSecret(), actual.getSecret());
        assertEquals(expected.getServiceIds(), actual.getServiceIds());
        assertEquals(expected.getStatus(), actual.getStatus());
    }

    private ApiKeyData makeExisting() {
        ApiKeyData existing = new ApiKeyData();
        existing.setOid(7L);
        existing.setKey("fookey");
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

    @Test
    public void testAddApiKey() throws Exception {
        when(entityManager.save(isA(ApiKeyData.class))).thenReturn(3L);

        ApiKeyData data = new ApiKeyData();
        data.setKey("fookey");
        data.setSecret("sekrit");
        data.setServiceIds(serviceIds);
        data.setStatus("somestatus");
        data.setVersion(7);
        data.setLabel("somelabel");
        data.setOauthCallbackUrl("someOauthCallbackUrl");
        data.setOauthScope("someOauthScope");
        data.setOauthType("someOauthType");
        data.setPlatform("somePlatform");
        final ApiKeyData result = apiKeyManager.add(data);

        verify(entityManager).save(isA(ApiKeyData.class));
        data.setOid(3L);
        assertCacheContains(data);
        assertMatches(data, result);
    }

    @Test(expected = SaveException.class)
    public void testAddApiKey_alreadySavedBefore() throws Exception {
        ApiKeyData data = new ApiKeyData();
        data.setOid(88);
        data.setKey("fookey");
        apiKeyManager.add(data);
    }

    @Test(expected = DuplicateObjectException.class)
    public void testAddApiKey_nameAlreadyInUse() throws Exception {
        when(entityManager.save(isA(ApiKeyData.class))).thenThrow(new DuplicateObjectException());

        ApiKeyData data = new ApiKeyData();
        data.setOid(-1); // Try to save a new one with colliding name
        data.setKey("fookey");
        apiKeyManager.add(data);
    }

    @Test
    public void testUpdateApiKey() throws Exception {
        ApiKeyData existing = new ApiKeyData();
        existing.setKey("fookey");
        existing.setSecret("oldsecret");
        existing.setServiceIds(serviceIds);
        existing.setStatus("prevstatus");
        existing.setVersion(2);
        existing.setOid(1234L);
        when(entityManager.findByUniqueName(anyString())).thenReturn(existing);

        ApiKeyData data = new ApiKeyData();
        data.setKey("fookey");
        data.setSecret("sekrit");
        data.setServiceIds(serviceIds);
        data.setStatus("somestatus");
        data.setOid(-1L);

        final ApiKeyData result = apiKeyManager.update(data);

        // merged data should have same oid as existing but updated fields
        ApiKeyData mergedData = new ApiKeyData();
        mergedData.setKey("fookey");
        mergedData.setSecret("sekrit");
        mergedData.setServiceIds(serviceIds);
        mergedData.setStatus("somestatus");
        mergedData.setVersion(2);
        mergedData.setOid(1234L);

        verify(entityManager).findByUniqueName("fookey");
        verify(entityManager).update(argThat(new MatchesApiKeyData(mergedData)));
        assertMatches(mergedData, result);

    }

    @Test(expected = ObjectNotFoundException.class)
    public void testUpdateApiKey_notFound() throws Exception {
        ApiKeyData data = new ApiKeyData();
        data.setKey("fookey");
        apiKeyManager.update(data);
    }

    @Test(expected = FindException.class)
    public void testUpdateApiKeyFindException() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new FindException("mocking exception"));
        ApiKeyData key = new ApiKeyData();
        key.setName("fookey");

        apiKeyManager.update(key);
    }

    @Test(expected = InvalidGenericEntityException.class)
    public void testUpdateApiKeyInvalidGenericEntityException() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("mocking exception"));
        ApiKeyData key = new ApiKeyData();
        key.setName("fookey");

        apiKeyManager.update(key);
    }

    @BugNumber(12334)
    @Test(expected = ObjectNotFoundException.class)
    public void testUpdateApiKeyInvalidGenericEntityExceptionNotOfExpectedClass() throws Exception {
        when(entityManager.findByUniqueName("fookey")).thenThrow(new InvalidGenericEntityException("generic entity is not of expected class"));
        ApiKeyData key = new ApiKeyData();
        key.setName("fookey");

        apiKeyManager.update(key);
    }

    @Test
    public void testRemoveApiKey() throws Exception {
        ApiKeyData existing = makeExisting();
        when(entityManager.findByUniqueName("fookey")).thenReturn(existing);
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(7L, "fookey");

        apiKeyManager.delete("fookey");

        verify(entityManager).delete(existing);
        assertTrue("entries are immediately removed from the cache when they are removed on this node", apiKeyManager.getCache().isEmpty());
        assertTrue("oids are permitted to remain in name cache for the full cleanup period, to avoid races", apiKeyManager.getNameCache().containsKey(7L));
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
        ApiKeyData existing = makeExisting();
        when(entityManager.findByUniqueName("fookey")).thenReturn(existing);
        assertCacheDoesNotContain(existing, true);

        ApiKeyData found = apiKeyManager.find("fookey");

        verify(entityManager).findByUniqueName("fookey");
        assertMatches(existing, found);
        assertCacheContains(found);

    }

    @Test
    public void testGetApiKey_fromCache() throws Exception {
        ApiKeyData existing = makeExisting();
        apiKeyManager.getCache().put(existing.getKey(), existing);
        apiKeyManager.getNameCache().put(existing.getOidAsLong(), existing.getKey());

        ApiKeyData found = apiKeyManager.find("fookey");

        verifyZeroInteractions(entityManager);
        assertMatches(existing, found);
        assertCacheContains(found);
    }

    @Test
    public void testGetApiKey_notFound() throws Exception {
        when(entityManager.findByUniqueName(anyString())).thenReturn(null);

        ApiKeyData found = apiKeyManager.find("keythatisnotfound");
        assertNull(found);
    }

    @Test
    public void testCacheEvictedWhenEntityUpdated() throws Exception {
        ApiKeyData existing = makeExisting();
        apiKeyManager.getCache().put(existing.getKey(), existing);
        apiKeyManager.getNameCache().put(existing.getOidAsLong(), existing.getKey());

        ApiKeyData decoy = makeExisting();
        decoy.setKey("otherkeyunrelated");
        decoy.setOid(9833);
        apiKeyManager.getCache().put(decoy.getKey(), decoy);
        apiKeyManager.getNameCache().put(decoy.getOidAsLong(), decoy.getKey());

        // Event should be ignored (oid doesn't match anything in cache)
        apiKeyManagerEventListener.onApplicationEvent(new EntityInvalidationEvent(new Object(), GenericEntity.class, new long[]{222L}, new char[]{'U'}));
        assertCacheContains(existing);
        assertCacheContains(decoy);

        // Event should cause cache entry to be removed (matches)
        apiKeyManagerEventListener.onApplicationEvent(new EntityInvalidationEvent(new Object(), GenericEntity.class, new long[]{existing.getOid()}, new char[]{'U'}));
        assertCacheDoesNotContain(existing, false);
        assertCacheContains(decoy);
    }

    @Test
    public void onApplicationEventGenericEntity() throws Exception {
        ApiKeyData existing = makeExisting();
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(1234L, "fookey");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(existing, GenericEntity.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});

        apiKeyManager.onApplicationEvent(event);

        assertTrue(apiKeyManager.getCache().isEmpty());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotGenericEntity() throws Exception {
        apiKeyManager.getCache().put("fookey", makeExisting());
        apiKeyManager.getNameCache().put(1234L, "fookey");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(new PublishedService(), PublishedService.class, new long[]{1234L}, new char[]{EntityInvalidationEvent.CREATE});

        apiKeyManager.onApplicationEvent(event);

        assertEquals(1, apiKeyManager.getCache().size());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

    @Test
    public void onApplicationEventNotEntityInvalidationEvent() throws Exception {
        ApiKeyData existing = makeExisting();
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(1234L, "fookey");

        apiKeyManager.onApplicationEvent(new LogonEvent("", LogonEvent.LOGOFF));

        assertEquals(1, apiKeyManager.getCache().size());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

    @Test
    public void onApplicationEventGenericEntityWrongId() throws Exception {
        ApiKeyData existing = makeExisting();
        apiKeyManager.getCache().put("fookey", existing);
        apiKeyManager.getNameCache().put(1234L, "fookey");
        final EntityInvalidationEvent event = new EntityInvalidationEvent(existing, GenericEntity.class, new long[]{5678L}, new char[]{EntityInvalidationEvent.CREATE});

        apiKeyManager.onApplicationEvent(event);

        assertEquals(1, apiKeyManager.getCache().size());
        assertEquals(1, apiKeyManager.getNameCache().size());
    }

    /**
     * Matcher for ApiKeyData that looks ensures fields are equal.
     */
    class MatchesApiKeyData extends ArgumentMatcher<ApiKeyData> {
        private final ApiKeyData toMatch;

        public MatchesApiKeyData(final ApiKeyData toMatch) {
            this.toMatch = toMatch;
        }

        @Override
        public boolean matches(final Object o) {
            final ApiKeyData data = (ApiKeyData) o;
            if (toMatch.getOid() != data.getOid()) {
                return false;
            }
            if (!toMatch.getKey().equals(data.getKey())) {
                return false;
            }
            if (!toMatch.getSecret().equals(data.getSecret())) {
                return false;
            }
            if (toMatch.getVersion() != data.getVersion()) {
                return false;
            }
            if (toMatch.getServiceIds().size() != data.getServiceIds().size()) {
                return false;
            }
            if (!toMatch.getServiceIds().entrySet().containsAll(data.getServiceIds().entrySet())) {
                return false;
            }
            return true;
        }
    }
}
