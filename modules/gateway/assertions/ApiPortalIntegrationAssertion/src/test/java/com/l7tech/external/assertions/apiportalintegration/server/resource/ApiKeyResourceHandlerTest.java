package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalGenericEntityManager;
import com.l7tech.external.assertions.apiportalintegration.server.apikey.manager.ApiKey;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiKeyResourceHandlerTest {
    private ApiKeyResourceHandler handler;
    private ApiKeyResource resource;
    @Mock
    private PortalGenericEntityManager<ApiKey> manager;
    @Mock
    private ResourceTransformer<ApiKeyResource, ApiKey> transformer;

    @Before
    public void setup() {
        handler = new ApiKeyResourceHandler(manager, transformer);
        resource = createDefaultResource();
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

    private ApiKeyResource createDefaultResource() {
        final ApiKeyResource resource = new ApiKeyResource();
        resource.setKey("k1");
        resource.setLabel("label");
        resource.setPlatform("platform");
        resource.setSecret("secret");
        resource.setStatus("active");
        resource.setSecurity(new SecurityDetails(new OAuthDetails("callback", "scope", "oauthType")));
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        resource.setApis(services);
        return resource;
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
        final Map<String, String> services = new HashMap<String, String>();
        services.put("s1", "p1");
        services.put("s2", "p2");
        entity.setServiceIds(services);
        return entity;
    }
}
