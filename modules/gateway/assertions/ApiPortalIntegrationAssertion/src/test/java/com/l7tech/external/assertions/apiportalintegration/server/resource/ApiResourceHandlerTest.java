package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedServiceManager;
import com.l7tech.objectmodel.FindException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static com.l7tech.external.assertions.apiportalintegration.server.resource.ApiResourceHandler.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiResourceHandlerTest {
    private ApiResourceHandler handler;
    private Map<String, String> filters;
    private List<PortalManagedService> portalManagedServices;
    @Mock
    private PortalManagedServiceManager manager;
    @Mock
    private ApiResourceTransformer transformer;

    @Before
    public void setup() {
        handler = new ApiResourceHandler(manager, transformer);
        filters = new HashMap<String, String>();
        portalManagedServices = new ArrayList<PortalManagedService>();
    }

    @Test
    public void getAll() throws Exception {
        portalManagedServices.add(createPortalManagedService("a1", "g1", "1111"));
        portalManagedServices.add(createPortalManagedService("a2", "g2", "2222"));
        when(manager.findAll()).thenReturn(portalManagedServices);
        final ApiResource api1 = new ApiResource("a1", "g1", "1111");
        final ApiResource api2 = new ApiResource("a2", "g2", "2222");
        when(transformer.entityToResource(any(PortalManagedService.class))).thenReturn(api1, api2);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, times(2)).entityToResource(any(PortalManagedService.class));
        assertEquals(2, resources.size());
        assertTrue(resources.contains(api1));
        assertTrue(resources.contains(api2));
    }

    @Test
    public void getAllNullFilters() throws Exception {
        portalManagedServices.add(createPortalManagedService("a1", "g1", "1111"));
        portalManagedServices.add(createPortalManagedService("a2", "g2", "2222"));
        when(manager.findAll()).thenReturn(portalManagedServices);
        final ApiResource api1 = new ApiResource("a1", "g1", "1111");
        final ApiResource api2 = new ApiResource("a2", "g2", "2222");
        when(transformer.entityToResource(any(PortalManagedService.class))).thenReturn(api1, api2);

        final List<ApiResource> resources = handler.get((Map)null);

        verify(manager).findAll();
        verify(transformer, times(2)).entityToResource(any(PortalManagedService.class));
        assertEquals(2, resources.size());
        assertTrue(resources.contains(api1));
        assertTrue(resources.contains(api2));
    }

    @Test
    public void getAllFilterByGroup() throws Exception {
        filters.put(API_GROUP, "testGroup");
        portalManagedServices.add(createPortalManagedService("a1", "testGroup", "1111"));
        portalManagedServices.add(createPortalManagedService("a2", "nomatch", "2222"));
        when(manager.findAll()).thenReturn(portalManagedServices);
        final ApiResource matchGroup = new ApiResource("a1", "testGroup", "1111");
        when(transformer.entityToResource(any(PortalManagedService.class))).thenReturn(matchGroup);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, times(1)).entityToResource(any(PortalManagedService.class));
        assertEquals(1, resources.size());
        assertTrue(resources.contains(matchGroup));
    }

    @Test
    public void getAllNone() throws Exception {
        portalManagedServices.clear();
        when(manager.findAll()).thenReturn(portalManagedServices);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, never()).entityToResource(any(PortalManagedService.class));
        assertTrue(resources.isEmpty());
    }

    @Test
    public void getAllFilterByGroupNone() throws Exception {
        filters.put(API_GROUP, "testGroup");
        portalManagedServices.add(createPortalManagedService("a1", "nomatch1", "1111"));
        portalManagedServices.add(createPortalManagedService("a2", "nomatch2", "2222"));
        when(manager.findAll()).thenReturn(portalManagedServices);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, never()).entityToResource(any(PortalManagedService.class));
        assertTrue(resources.isEmpty());
    }

    @Test
    public void getAllFilterByGroupNull() throws Exception {
        filters.put(API_GROUP, "testGroup");
        portalManagedServices.add(createPortalManagedService("a1", null, "1111"));
        when(manager.findAll()).thenReturn(portalManagedServices);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).findAll();
        assertTrue(resources.isEmpty());
    }

    @Test
    public void getAllFilterByGroupNullFilter() throws Exception {
        filters.put(API_GROUP, null);
        portalManagedServices.add(createPortalManagedService("a1", "testGroup", "1111"));
        when(manager.findAll()).thenReturn(portalManagedServices);
        final ApiResource api1 = new ApiResource("a1", "testGroup", "1111");
        when(transformer.entityToResource(any(PortalManagedService.class))).thenReturn(api1);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, times(1)).entityToResource(any(PortalManagedService.class));
        assertEquals(1, resources.size());
        assertTrue(resources.contains(api1));
    }

    @Test(expected = FindException.class)
    public void getAllFindException() throws Exception {
        when(manager.findAll()).thenThrow(new FindException("mocking exception"));

        try {
            handler.get(filters);
        } catch (final FindException e) {
            verify(manager).findAll();
            throw e;
        }
        fail("Expected FindException");

    }

    @Test
    public void getById() throws Exception {
        filters.put(ID, "a1");
        when(manager.find("a1")).thenReturn(createPortalManagedService("a1", "g1", "1111"));
        final ApiResource api = new ApiResource("a1", "g1", "1111");
        when(transformer.entityToResource(any(PortalManagedService.class))).thenReturn(api);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).find("a1");
        verify(transformer, times(1)).entityToResource(any(PortalManagedService.class));
        assertEquals(1, resources.size());
        assertTrue(resources.contains(api));
    }

    @Test
    public void getByIdIgnoresGroupFilter() throws Exception {
        filters.put(ID, "a1");
        filters.put(API_GROUP, "ignoreMe");
        when(manager.find("a1")).thenReturn(createPortalManagedService("a1", "g1", "1111"));
        final ApiResource api = new ApiResource("a1", "g1", "1111");
        when(transformer.entityToResource(any(PortalManagedService.class))).thenReturn(api);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).find("a1");
        verify(transformer, times(1)).entityToResource(any(PortalManagedService.class));
        assertEquals(1, resources.size());
        assertTrue(resources.contains(api));
    }

    @Test
    public void getByIdNotFound() throws Exception {
        filters.put(ID, "a1");
        when(manager.find("a1")).thenReturn(null);

        final List<ApiResource> resources = handler.get(filters);

        verify(manager).find("a1");
        verify(transformer, never()).entityToResource(any(PortalManagedService.class));
        assertTrue(resources.isEmpty());
    }

    @Test(expected = FindException.class)
    public void getByIdFindException() throws Exception {
        filters.put(ID, "a1");
        when(manager.find("a1")).thenThrow(new FindException("mocking exception"));

        try {
            handler.get(filters);
        } catch (final FindException e) {
            verify(manager).find("a1");
            throw e;
        }
        fail("Expected FindException");

    }

    @Test(expected=UnsupportedOperationException.class)
    public void delete() throws Exception{
        handler.delete("a1");
    }

    @Test(expected=UnsupportedOperationException.class)
    public void put() throws Exception{
        handler.put(Collections.<ApiResource>emptyList(), false);
    }

    private PortalManagedService createPortalManagedService(final String apiId, final String apiGroup, final String serviceOid) {
        final PortalManagedService service = new PortalManagedService();
        service.setName(apiId);
        service.setDescription(serviceOid);
        service.setApiGroup(apiGroup);
        return service;
    }
}
