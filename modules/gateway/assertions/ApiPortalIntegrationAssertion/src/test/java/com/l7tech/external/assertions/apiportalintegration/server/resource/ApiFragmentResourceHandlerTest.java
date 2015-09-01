package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedEncass;
import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import com.l7tech.external.assertions.apiportalintegration.server.portalmanagedservices.manager.PortalManagedEncassManager;
import com.l7tech.objectmodel.FindException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;


import static com.l7tech.external.assertions.apiportalintegration.server.resource.ApiFragmentResourceHandler.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiFragmentResourceHandlerTest {
    private ApiFragmentResourceHandler handler;
    private Map<String, String> filters;
    private List<PortalManagedEncass> portalManagedEncasses;
    @Mock
    private PortalManagedEncassManager manager;
    @Mock
    private ApiFragmentResourceTransformer transformer;

    @Before
    public void setup() {
        handler = new ApiFragmentResourceHandler(manager, transformer);
        filters = new HashMap<String, String>();
        portalManagedEncasses = new ArrayList<PortalManagedEncass>();
    }

    @Test
    public void getAll() throws Exception {
        portalManagedEncasses.add(createPortalManagedEncass("a1", "id1", true, "details1"));
        portalManagedEncasses.add(createPortalManagedEncass("a2", "id2", false, "details2"));
        when(manager.findAll()).thenReturn(portalManagedEncasses);
        final ApiFragmentResource api1 = new ApiFragmentResource("a1", "id1", "true", "details1");
        final ApiFragmentResource api2 = new ApiFragmentResource("a2", "id2", "false", "details2");
        when(transformer.entityToResource(any(PortalManagedEncass.class))).thenReturn(api1, api2);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, times(2)).entityToResource(any(PortalManagedEncass.class));
        assertEquals(2, resources.size());
        assertTrue(resources.contains(api1));
        assertTrue(resources.contains(api2));
    }

    @Test
    public void getAllNullFilters() throws Exception {
        portalManagedEncasses.add(createPortalManagedEncass("a1", "id1", true, "details1"));
        portalManagedEncasses.add(createPortalManagedEncass("a2", "id2", false, "details2"));
        when(manager.findAll()).thenReturn(portalManagedEncasses);
        final ApiFragmentResource api1 = new ApiFragmentResource("a1", "id1", "true", "details1");
        final ApiFragmentResource api2 = new ApiFragmentResource("a2", "id2", "true", "details2");
        when(transformer.entityToResource(any(PortalManagedEncass.class))).thenReturn(api1, api2);

        final List<ApiFragmentResource> resources = handler.get((Map)null);

        verify(manager).findAll();
        verify(transformer, times(2)).entityToResource(any(PortalManagedEncass.class));
        assertEquals(2, resources.size());
        assertTrue(resources.contains(api1));
        assertTrue(resources.contains(api2));
    }

    @Test
    public void getAllFilterByGuid() throws Exception {
        filters.put(GUID, "match");
        portalManagedEncasses.add(createPortalManagedEncass("match", "id1", true, "details1"));
        portalManagedEncasses.add(createPortalManagedEncass("nomatch", "id2", false, "details2"));
        when(manager.findAll()).thenReturn(portalManagedEncasses);
        final ApiFragmentResource matchGuid = new ApiFragmentResource("a1", "match", "true", "details1");
        when(transformer.entityToResource(any(PortalManagedEncass.class))).thenReturn(matchGuid);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, times(1)).entityToResource(any(PortalManagedEncass.class));
        assertEquals(1, resources.size());
        assertTrue(resources.contains(matchGuid));
    }

    @Test
    public void getAllNone() throws Exception {
        portalManagedEncasses.clear();
        when(manager.findAll()).thenReturn(portalManagedEncasses);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, never()).entityToResource(any(PortalManagedEncass.class));
        assertTrue(resources.isEmpty());
    }

    @Test
    public void getAllFilterByGuidNone() throws Exception {
        filters.put(GUID, "match");
        portalManagedEncasses.add(createPortalManagedEncass("nomatch1", "id1", true, "details1"));
        portalManagedEncasses.add(createPortalManagedEncass("nomatch2", "id2", false, "details2"));
        when(manager.findAll()).thenReturn(portalManagedEncasses);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, never()).entityToResource(any(PortalManagedEncass.class));
        assertTrue(resources.isEmpty());
    }

    @Test
    public void getAllFilterByGroupNull() throws Exception {
        filters.put(GUID, "nomatch");
        portalManagedEncasses.add(createPortalManagedEncass(null, "id1", true, "details1"));
        when(manager.findAll()).thenReturn(portalManagedEncasses);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).findAll();
        assertTrue(resources.isEmpty());
    }

    @Test
    public void getAllFilterByGuidNullFilter() throws Exception {
        filters.put(GUID, null);
        portalManagedEncasses.add(createPortalManagedEncass("a1", "id1", true, "details1"));
        when(manager.findAll()).thenReturn(portalManagedEncasses);
        final ApiFragmentResource api1 = new ApiFragmentResource("a1", "id1", "true", "details1");
        when(transformer.entityToResource(any(PortalManagedEncass.class))).thenReturn(api1);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, times(1)).entityToResource(any(PortalManagedEncass.class));
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
        when(manager.find("a1")).thenReturn(createPortalManagedEncass("a1", "id1", true, "details1"));
        final ApiFragmentResource api = new ApiFragmentResource("a1", "id1", "true", "details1");
        when(transformer.entityToResource(any(PortalManagedEncass.class))).thenReturn(api);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).find("a1");
        verify(transformer, times(1)).entityToResource(any(PortalManagedEncass.class));
        assertEquals(1, resources.size());
        assertTrue(resources.contains(api));
    }

    @Test
    public void getByIdIgnoresGuidFilter() throws Exception {
        filters.put(ID, "a1");
        filters.put(GUID, "ignoreMe");
        when(manager.find("a1")).thenReturn(createPortalManagedEncass("a1", "id1", true, "details1"));
        final ApiFragmentResource api = new ApiFragmentResource("a1", "id1", "true", "details1");
        when(transformer.entityToResource(any(PortalManagedEncass.class))).thenReturn(api);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).find("a1");
        verify(transformer, times(1)).entityToResource(any(PortalManagedEncass.class));
        assertEquals(1, resources.size());
        assertTrue(resources.contains(api));
    }

    @Test
    public void getByIdNotFound() throws Exception {
        filters.put(ID, "a1");
        when(manager.find("a1")).thenReturn(null);

        final List<ApiFragmentResource> resources = handler.get(filters);

        verify(manager).find("a1");
        verify(transformer, never()).entityToResource(any(PortalManagedEncass.class));
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
        handler.put(Collections.<ApiFragmentResource>emptyList(), false);
    }

    private PortalManagedEncass createPortalManagedEncass(final String encassGuid, final String encassId, final boolean hasRouting, final String parsedPolicyDetails) {
        final PortalManagedEncass encass = new PortalManagedEncass();
        encass.setEncassGuid(encassGuid);
        encass.setEncassId(encassId);
        encass.setHasRouting(hasRouting);
        encass.setParsedPolicyDetails(parsedPolicyDetails);
        return encass;
    }
}
