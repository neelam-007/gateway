package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyResourceHandlerTest {
    private static final Date LAST_UPDATE = new Date();
    private ApiKeyResourceHandler handler;
    private ApiKeyResource resource;
    private List<ApiKeyResource> resources;
    @Mock
    private PortalGenericEntityManager<ApiKey> manager;
    @Mock
    private ResourceTransformer<ApiKeyResource, ApiKey> transformer;

    @Before
    public void setup() {
        handler = new ApiKeyResourceHandler(manager, transformer);
        resource = createDefaultResource();
        resources = new ArrayList<ApiKeyResource>();
    }

    @Test
    public void get() throws Exception {
        final ApiKey entity = createDefaultEntity();
        when(manager.find("k1")).thenReturn(entity);
        when(transformer.entityToResource(entity)).thenReturn(resource);

        final ApiKeyResource result = handler.get("k1");

        assertEquals(resource, result);
        verify(manager).find("k1");
        verify(transformer).entityToResource(entity);
    }

    @Test
    public void getNotFound() throws Exception {
        when(manager.find("k1")).thenReturn(null);

        final ApiKeyResource result = handler.get("k1");

        assertNull(result);
        verify(manager).find("k1");
        verify(transformer, never()).entityToResource(Matchers.<ApiKey>any());
    }

    @Test(expected = FindException.class)
    public void getFindException() throws Exception {
        when(manager.find("k1")).thenThrow(new FindException("mocking exception"));

        try {
            handler.get("k1");
        } catch (final FindException e) {
            verify(manager).find("k1");
            verify(transformer, never()).entityToResource(Matchers.<ApiKey>any());
            throw e;
        }
        fail("Expected FindException");
    }

    @Test
    public void putAdd() throws Exception {
        final ApiKey entity = createDefaultEntity();
        when(transformer.resourceToEntity(resource)).thenReturn(entity);
        when(manager.add(entity)).thenReturn(entity);
        final ApiKeyResource transformed = createDefaultResource();
        when(transformer.entityToResource(entity)).thenReturn(transformed);

        final ApiKeyResource result = handler.put(resource);

        assertEquals(transformed, result);
        verify(transformer).resourceToEntity(resource);
        verify(manager).find("k1");
        verify(manager).add(entity);
    }

    @Test
    public void putUpdate() throws Exception {
        final ApiKey entity = createDefaultEntity();
        when(transformer.resourceToEntity(resource)).thenReturn(entity);
        when(manager.find("k1")).thenReturn(new ApiKey());
        when(manager.update(entity)).thenReturn(entity);
        final ApiKeyResource transformed = createDefaultResource();
        when(transformer.entityToResource(entity)).thenReturn(transformed);

        final ApiKeyResource result = handler.put(resource);

        assertEquals(transformed, result);
        verify(transformer).resourceToEntity(resource);
        verify(manager).find("k1");
        verify(manager).update(entity);
    }



    @Test(expected = FindException.class)
    public void putAddOrUpdateFindException() throws Exception {
        final ApiKey entity = createDefaultEntity();
        when(transformer.resourceToEntity(resource)).thenReturn(entity);
        when(manager.find("k1")).thenThrow(new FindException("mocking exception"));

        try {
            handler.put(resource);
        } catch (final FindException e) {
            verify(transformer).resourceToEntity(resource);
            verify(manager).find("k1");
            verify(manager, never()).add(Matchers.<ApiKey>any());
            verify(manager, never()).update(Matchers.<ApiKey>any());
            verify(transformer, never()).entityToResource(Matchers.<ApiKey>any());
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = SaveException.class)
    public void putAddSaveException() throws Exception {
        final ApiKey entity = createDefaultEntity();
        when(transformer.resourceToEntity(resource)).thenReturn(entity);
        when(manager.add(entity)).thenThrow(new SaveException("mocking exception"));

        try {
            handler.put(resource);
        } catch (final SaveException e) {
            verify(transformer).resourceToEntity(resource);
            verify(manager).find("k1");
            verify(manager).add(entity);
            verify(transformer, never()).entityToResource(Matchers.<ApiKey>any());
            throw e;
        }
        fail("Expected SaveException");
    }

    @Test(expected = UpdateException.class)
    public void putUpdateUpdateException() throws Exception {
        final ApiKey entity = createDefaultEntity();
        when(transformer.resourceToEntity(resource)).thenReturn(entity);
        when(manager.find("k1")).thenReturn(entity);
        when(manager.update(entity)).thenThrow(new UpdateException("mocking exception"));

        try {
            handler.put(resource);
        } catch (final UpdateException e) {
            verify(transformer).resourceToEntity(resource);
            verify(manager).find("k1");
            verify(manager).update(entity);
            verify(transformer, never()).entityToResource(Matchers.<ApiKey>any());
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test
    public void putAllAdd() throws Exception {
        resources.add(createResource("k1","label1","platform1","secret1","active", null));
        resources.add(createResource("k2","label2","platform2","secret2","active", null));
        final ApiKey key1 = createEntity("k1", "label1", "platform1", "secret1", "active", null);
        final ApiKey key2 = createEntity("k2", "label2", "platform2", "secret2", "active", null);
        when(transformer.resourceToEntity(any(ApiKeyResource.class))).thenReturn(key1, key2);
        when(manager.add(any(ApiKey.class))).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE),
                createEntity("k2","label2","platform2","secret2","active", LAST_UPDATE));
        final ApiKeyResource resource1 = createResource("k1","label1","platform1","secret1","active", LAST_UPDATE);
        final ApiKeyResource resource2 = createResource("k2","label2","platform2","secret2","active", LAST_UPDATE);
        when(transformer.entityToResource(any(ApiKey.class))).thenReturn(resource1, resource2);

        final List<ApiKeyResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(any(ApiKeyResource.class));
        verify(manager).find("k1");
        verify(manager).add(key1);
        verify(manager).find("k2");
        verify(manager).add(key2);
        verify(transformer, times(2)).entityToResource(any(ApiKey.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAllUpdate() throws Exception {
        resources.add(createResource("k1","label1","platform1","secret1","active", null));
        resources.add(createResource("k2","label2","platform2","secret2","active", null));
        final ApiKey key1 = createEntity("k1", "label1", "platform1", "secret1", "active", null);
        final ApiKey key2 = createEntity("k2", "label2", "platform2", "secret2", "active", null);
        when(transformer.resourceToEntity(any(ApiKeyResource.class))).thenReturn(key1, key2);
        when(manager.find("k1")).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "suspend", LAST_UPDATE));
        when(manager.find("k2")).thenReturn(createEntity("k2", "label2", "platform2", "secret2", "suspend", LAST_UPDATE));
        when(manager.update(any(ApiKey.class))).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE),
                createEntity("k2", "label2", "platform2", "secret2", "active", LAST_UPDATE));
        final ApiKeyResource resource1 = createResource("k1","label1","platform1","secret1","active", LAST_UPDATE);
        final ApiKeyResource resource2 = createResource("k2","label2","platform2","secret2","active", LAST_UPDATE);
        when(transformer.entityToResource(any(ApiKey.class))).thenReturn(resource1, resource2);

        final List<ApiKeyResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(any(ApiKeyResource.class));
        verify(manager).find("k1");
        verify(manager).update(key1);
        verify(manager).find("k2");
        verify(manager).update(key2);
        verify(transformer, times(2)).entityToResource(any(ApiKey.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAddOrUpdate() throws Exception {
        resources.add(createResource("k1","label1","platform1","secret1","active", null));
        resources.add(createResource("k2","label2","platform2","secret2","suspend", null));
        final ApiKey key1 = createEntity("k1", "label1", "platform1", "secret1", "active", null);
        final ApiKey key2 = createEntity("k2", "label2", "platform2", "secret2", "suspend", null);
        when(transformer.resourceToEntity(any(ApiKeyResource.class))).thenReturn(key1, key2);
        when(manager.find("k1")).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE));
        when(manager.update(any(ApiKey.class))).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE));
        when(manager.add(any(ApiKey.class))).thenReturn(createEntity("k2", "label2", "platform2", "secret2", "active", LAST_UPDATE));
        final ApiKeyResource resource1 = createResource("k1","label1","platform1","secret1","active", LAST_UPDATE);
        final ApiKeyResource resource2 = createResource("k2","label2","platform2","secret2","active", LAST_UPDATE);
        when(transformer.entityToResource(any(ApiKey.class))).thenReturn(resource1, resource2);

        final List<ApiKeyResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(any(ApiKeyResource.class));
        verify(manager).find("k1");
        verify(manager).update(key1);
        verify(manager).find("k2");
        verify(manager).add(key2);
        verify(transformer, times(2)).entityToResource(any(ApiKey.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAddOrUpdateSetsLastUpdate() throws Exception {
        resources.add(createResource("k1","label1","platform1","secret1","active", null));
        resources.add(createResource("k2","label2","platform2","secret2","suspend", null));
        final ApiKey key1 = createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE);
        final ApiKey key2 = createEntity("k2", "label2", "platform2", "secret2", "suspend", LAST_UPDATE);
        when(transformer.resourceToEntity(any(ApiKeyResource.class))).thenReturn(key1, key2);
        when(manager.find("p1")).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE));
        when(manager.update(any(ApiKey.class))).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE));
        when(manager.add(any(ApiKey.class))).thenReturn(createEntity("k2", "label2", "platform2", "secret2", "active", LAST_UPDATE));
        final ApiKeyResource resource1 = createResource("k1","label1","platform1","secret1","active", LAST_UPDATE);
        final ApiKeyResource resource2 = createResource("k2","label2","platform2","secret2","active", LAST_UPDATE);
        when(transformer.entityToResource(any(ApiKey.class))).thenReturn(resource1, resource2);

        final List<ApiKeyResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(argThat(new NonNullLastUpdate()));
        verify(manager).find("k1");
        verify(manager).add(key1);
        verify(manager).find("k2");
        verify(manager).add(key2);
        verify(transformer, times(2)).entityToResource(any(ApiKey.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAddOrUpdateOverwritesInputLastUpdate() throws Exception {
        final Calendar calendar = new GregorianCalendar(1800, Calendar.JANUARY, 1);
        final Date shouldBeOverwritten = calendar.getTime();
        resources.add(createResource("k1","label1","platform1","secret1","active", shouldBeOverwritten));
        resources.add(createResource("k2","label2","platform2","secret2","suspend", shouldBeOverwritten));
        final ApiKey key1 = createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE);
        final ApiKey key2 = createEntity("k2", "label2", "platform2", "secret2", "suspend", LAST_UPDATE);
        when(transformer.resourceToEntity(any(ApiKeyResource.class))).thenReturn(key1, key2);
        when(manager.find("p1")).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE));
        when(manager.update(any(ApiKey.class))).thenReturn(createEntity("k1", "label1", "platform1", "secret1", "active", LAST_UPDATE));
        when(manager.add(any(ApiKey.class))).thenReturn(createEntity("k2", "label2", "platform2", "secret2", "active", LAST_UPDATE));
        final ApiKeyResource resource1 = createResource("k1","label1","platform1","secret1","active", LAST_UPDATE);
        final ApiKeyResource resource2 = createResource("k2","label2","platform2","secret2","active", LAST_UPDATE);
        when(transformer.entityToResource(any(ApiKey.class))).thenReturn(resource1, resource2);

        final List<ApiKeyResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(argThat(new LastUpdateNotEqualTo(shouldBeOverwritten)));
        verify(manager).find("k1");
        verify(manager).add(key1);
        verify(manager).find("k2");
        verify(manager).add(key2);
        verify(transformer, times(2)).entityToResource(any(ApiKey.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putNone() throws Exception {
        resources.clear();

        final List<ApiKeyResource> result = handler.put(resources, false);

        verify(transformer, never()).resourceToEntity(any(ApiKeyResource.class));
        verify(manager, never()).find(anyString());
        verify(manager, never()).update(any(ApiKey.class));
        verify(manager, never()).add(any(ApiKey.class));
        verify(transformer, never()).entityToResource(any(ApiKey.class));
        assertTrue(result.isEmpty());
    }

    @Test
    public void delete() throws Exception {
        handler.delete("k1");

        verify(manager).delete("k1");
    }

    @Test(expected = FindException.class)
    public void deleteFindException() throws Exception {
        doThrow(new FindException("mocking exception")).when(manager).delete(anyString());

        try {
            handler.delete("k1");
        } catch (final FindException e) {
            verify(manager).delete("k1");
            throw e;
        }
        fail("Expected FindException");
    }

    @Test(expected = DeleteException.class)
    public void deleteDeleteException() throws Exception {
        doThrow(new DeleteException("mocking exception")).when(manager).delete(anyString());

        try {
            handler.delete("k1");
        } catch (final DeleteException e) {
            verify(manager).delete("k1");
            throw e;
        }
        fail("Expected DeleteException");
    }

    private ApiKeyResource createResource(String key, String label, String platform, String secret,
                                          String status, Date lastUpdate) {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey(key);
        resource.setLabel(label);
        resource.setPlatform(platform);
        resource.setSecret(secret);
        resource.setStatus(status);
        resource.setLastUpdate(lastUpdate);
        resource.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        resource.setApis(services);
        return resource;
    }
    
    private ApiKeyResource createDefaultResource() {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey("k1");
        resource.setLabel("label");
        resource.setPlatform("platform");
        resource.setSecret("secret");
        resource.setStatus("active");
        resource.setLastUpdate(LAST_UPDATE);
        resource.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        resource.setApis(services);
        return resource;
    }

    private List<ApiKeyResource> createDefaultResources() {
        final List<ApiKeyResource> resources = new ArrayList<ApiKeyResource>();
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey("k1");
        resource.setLabel("label");
        resource.setPlatform("platform");
        resource.setSecret("secret");
        resource.setStatus("active");
        resource.setLastUpdate(LAST_UPDATE);
        resource.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        resource.setApis(services);
        resources.add(resource);
        final ApiKeyResource resource2 = new ApiKeyResource();
        resource2.setKey("k2");
        resource2.setLabel("label");
        resource2.setPlatform("platform");
        resource2.setSecret("secret");
        resource2.setStatus("suspend");
        resource2.setLastUpdate(LAST_UPDATE);
        resource2.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services2 = new HashMap<String, String>();
        services2.put("s1", "p1");
        services2.put("s2", "p2");
        resource2.setApis(services2);
        resources.add(resource2);
        return resources;
    }

    private ApiKey createEntity(String key, String label, String platform, String secret,
                                String status, Date lastUpdate) {
        final ApiKey entity = new ApiKey();
        entity.setName(key);
        entity.setLabel(label);
        entity.setOauthCallbackUrl(platform);
        entity.setOauthScope("scope");
        entity.setOauthType("oauthType");
        entity.setPlatform("platform");
        entity.setSecret(secret);
        entity.setStatus(status);
        entity.setLastUpdate(lastUpdate);
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        entity.setServiceIds(services);
        return entity;
    }

    private ApiKey createDefaultEntity() {
        final ApiKey entity = new ApiKey();
        entity.setName("k1");
        entity.setLabel("label");
        entity.setOauthCallbackUrl("callback");
        entity.setOauthScope("scope");
        entity.setOauthType("oauthType");
        entity.setPlatform("platform");
        entity.setSecret("secret");
        entity.setStatus("active");
        entity.setLastUpdate(LAST_UPDATE);
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        entity.setServiceIds(services);
        return entity;
    }

    private class NonNullLastUpdate extends ArgumentMatcher<ApiKeyResource> {
        @Override
        public boolean matches(Object o) {
            final ApiKeyResource plan = (ApiKeyResource) o;
            if (plan.getLastUpdate() != null) {
                return true;
            }
            return false;
        }
    }

    private class LastUpdateNotEqualTo extends ArgumentMatcher<ApiKeyResource> {
        private final Date date;

        LastUpdateNotEqualTo(final Date date) {
            this.date = date;
        }

        @Override
        public boolean matches(Object o) {
            final ApiKeyResource plan = (ApiKeyResource) o;
            if (!plan.getLastUpdate().equals(date)) {
                return true;
            }
            return false;
        }
    }
}
