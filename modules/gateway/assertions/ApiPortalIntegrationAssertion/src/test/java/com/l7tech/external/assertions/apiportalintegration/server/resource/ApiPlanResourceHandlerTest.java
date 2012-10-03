package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import com.l7tech.external.assertions.apiportalintegration.server.apiplan.manager.ApiPlanManager;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.UpdateException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static com.l7tech.external.assertions.apiportalintegration.server.resource.ApiPlanResourceHandler.*;

@RunWith(MockitoJUnitRunner.class)
public class ApiPlanResourceHandlerTest {
    private static final Date LAST_UPDATE = new Date();
    private ApiPlanResourceHandler handler;
    private Map<String, String> filters;
    private List<ApiPlan> apiPlans;
    private List<ApiPlanResource> resources;
    @Mock
    private ApiPlanManager manager;
    @Mock
    private ApiPlanResourceTransformer transformer;

    @Before
    public void setup() {
        handler = new ApiPlanResourceHandler(manager, transformer);
        filters = new HashMap<String, String>();
        apiPlans = new ArrayList<ApiPlan>();
        resources = new ArrayList<ApiPlanResource>();
    }

    @Test
    public void getById() throws Exception {
        filters.put(ID, "p1");
        final ApiPlan apiPlan = createApiPlan("p1", "pName", LAST_UPDATE, "policy xml", true);
        when(manager.find("p1")).thenReturn(apiPlan);
        final ApiPlanResource resource = new ApiPlanResource("p1", "pName", LAST_UPDATE, "policy xml", true);
        when(transformer.entityToResource(apiPlan)).thenReturn(resource);

        final List<ApiPlanResource> resources = handler.get(filters);

        verify(manager).find("p1");
        verify(transformer).entityToResource(apiPlan);
        assertEquals(1, resources.size());
        assertTrue(resources.contains(resource));
    }

    @Test
    public void getByIdNotFound() throws Exception {
        filters.put(ID, "p1");
        when(manager.find("p1")).thenReturn(null);

        final List<ApiPlanResource> resources = handler.get(filters);

        verify(manager).find("p1");
        verify(transformer, never()).entityToResource(any(ApiPlan.class));
        assertTrue(resources.isEmpty());
    }

    @Test(expected = FindException.class)
    public void getByIdFindException() throws Exception {
        filters.put(ID, "p1");
        when(manager.find("p1")).thenThrow(new FindException("mocking exception"));

        try {
            handler.get(filters);
        } catch (final FindException e) {
            verify(manager).find("p1");
            verify(transformer, never()).entityToResource(any(ApiPlan.class));
            throw e;
        }
        fail("Expected FindException");

    }

    @Test
    public void getAll() throws Exception {
        apiPlans.add(createApiPlan("p1", "pName1", LAST_UPDATE, "policyXml1", true));
        apiPlans.add(createApiPlan("p2", "pName2", LAST_UPDATE, "policyXml2", true));
        when(manager.findAll()).thenReturn(apiPlans);
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policyXml1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policyXml2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resource1));
        assertTrue(resources.contains(resource2));
    }

    @Test
    public void getAllNullFilters() throws Exception {
        apiPlans.add(createApiPlan("p1", "pName1", LAST_UPDATE, "policyXml1", true));
        apiPlans.add(createApiPlan("p2", "pName2", LAST_UPDATE, "policyXml2", true));
        when(manager.findAll()).thenReturn(apiPlans);
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policyXml1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policyXml2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> resources = handler.get((Map)null);

        verify(manager).findAll();
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, resources.size());
        assertTrue(resources.contains(resource1));
        assertTrue(resources.contains(resource2));
    }

    @Test
    public void getAllNone() throws Exception {
        apiPlans.clear();
        when(manager.findAll()).thenReturn(apiPlans);

        final List<ApiPlanResource> resources = handler.get(filters);

        verify(manager).findAll();
        verify(transformer, never()).entityToResource(any(ApiPlan.class));
        assertTrue(resources.isEmpty());
    }

    @Test(expected = FindException.class)
    public void getAllFindException() throws Exception {
        when(manager.findAll()).thenThrow(new FindException("mocking exception"));
        try {
            handler.get(filters);
        } catch (final FindException e) {
            verify(manager).findAll();
            verify(transformer, never()).entityToResource(any(ApiPlan.class));
            throw e;
        }
        fail("Expected FindException");
    }

    @Test
    public void putAllAdd() throws Exception {
        resources.add(new ApiPlanResource("p1", "pName1", null, "policy xml 1", true));
        resources.add(new ApiPlanResource("p2", "pName2", null, "policy xml 2", true));
        final ApiPlan plan1 = createApiPlan("p1", "pName1", null, "policy xml 1", true);
        final ApiPlan plan2 = createApiPlan("p2", "pName2", null, "policy xml 2", true);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan1, plan2);
        when(manager.add(any(ApiPlan.class))).thenReturn(createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true),
                createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", true));
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policy xml 2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(any(ApiPlanResource.class));
        verify(manager).find("p1");
        verify(manager).add(plan1);
        verify(manager).find("p2");
        verify(manager).add(plan2);
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAllUpdate() throws Exception {
        resources.add(new ApiPlanResource("p1", "pName1", null, "policy xml 1", true));
        resources.add(new ApiPlanResource("p2", "pName2", null, "policy xml 2", true));
        final ApiPlan plan1 = createApiPlan("p1", "pName1", null, "policy xml 1", true);
        final ApiPlan plan2 = createApiPlan("p2", "pName2", null, "policy xml 2", true);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan1, plan2);
        when(manager.find("p1")).thenReturn(createApiPlan("p1", "pNameA", LAST_UPDATE, "policy xml 1", false));
        when(manager.find("p2")).thenReturn(createApiPlan("p2", "pNameB", LAST_UPDATE, "policy xml 2", false));
        when(manager.update(any(ApiPlan.class))).thenReturn(createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true),
                createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", true));
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policy xml 2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(any(ApiPlanResource.class));
        verify(manager).find("p1");
        verify(manager).update(plan1);
        verify(manager).find("p2");
        verify(manager).update(plan2);
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAddOrUpdate() throws Exception {
        resources.add(new ApiPlanResource("p1", "pName1", null, "policy xml 1", true));
        resources.add(new ApiPlanResource("p2", "pName2", null, "policy xml 2", false));
        final ApiPlan plan1 = createApiPlan("p1", "pName1", null, "policy xml 1", true);
        final ApiPlan plan2 = createApiPlan("p2", "pName2", null, "policy xml 2", false);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan1, plan2);
        when(manager.find("p1")).thenReturn(createApiPlan("p1", "pNameA", LAST_UPDATE, "policy xml 1", true));
        when(manager.update(any(ApiPlan.class))).thenReturn(createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true));
        when(manager.add(any(ApiPlan.class))).thenReturn(createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", true));
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policy xml 2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(any(ApiPlanResource.class));
        verify(manager).find("p1");
        verify(manager).update(plan1);
        verify(manager).find("p2");
        verify(manager).add(plan2);
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAddOrUpdateSetsLastUpdate() throws Exception {
        resources.add(new ApiPlanResource("p1", "pName1", null, "policy xml 1", true));
        resources.add(new ApiPlanResource("p2", "pName2", null, "policy xml 2", false));
        final ApiPlan plan1 = createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlan plan2 = createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", false);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan1, plan2);
        when(manager.find("p1")).thenReturn(createApiPlan("p1", "pNameA", LAST_UPDATE, "policy xml 1", true));
        when(manager.update(any(ApiPlan.class))).thenReturn(createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true));
        when(manager.add(any(ApiPlan.class))).thenReturn(createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", true));
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policy xml 2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(argThat(new NonNullLastUpdate()));
        verify(manager).find("p1");
        verify(manager).update(plan1);
        verify(manager).find("p2");
        verify(manager).add(plan2);
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putAddOrUpdateOverwritesInputLastUpdate() throws Exception {
        final Calendar calendar = new GregorianCalendar(1800, Calendar.JANUARY, 1);
        final Date shouldBeOverwritten = calendar.getTime();
        resources.add(new ApiPlanResource("p1", "pName1", shouldBeOverwritten, "policy xml 1", true));
        resources.add(new ApiPlanResource("p2", "pName2", shouldBeOverwritten, "policy xml 2", false));
        final ApiPlan plan1 = createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlan plan2 = createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", false);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan1, plan2);
        when(manager.find("p1")).thenReturn(createApiPlan("p1", "pNameA", LAST_UPDATE, "policy xml 1", true));
        when(manager.update(any(ApiPlan.class))).thenReturn(createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true));
        when(manager.add(any(ApiPlan.class))).thenReturn(createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", true));
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policy xml 2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> result = handler.put(resources, false);

        verify(transformer, times(2)).resourceToEntity(argThat(new LastUpdateNotEqualTo(shouldBeOverwritten)));
        verify(manager).find("p1");
        verify(manager).update(plan1);
        verify(manager).find("p2");
        verify(manager).add(plan2);
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void putNone() throws Exception {
        resources.clear();

        final List<ApiPlanResource> result = handler.put(resources, false);

        verify(transformer, never()).resourceToEntity(any(ApiPlanResource.class));
        verify(manager, never()).find(anyString());
        verify(manager, never()).update(any(ApiPlan.class));
        verify(manager, never()).add(any(ApiPlan.class));
        verify(transformer, never()).entityToResource(any(ApiPlan.class));
        assertTrue(result.isEmpty());
    }

    @Test(expected = SaveException.class)
    public void putSaveException() throws Exception {
        resources.add(new ApiPlanResource("p1", "pName", null, "policy xml", true));
        final ApiPlan plan = createApiPlan("p1", "pName", null, "policy xml", true);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan);
        doThrow(new SaveException("mocking exception")).when(manager).add(any(ApiPlan.class));
        try {
            handler.put(resources, false);
        } catch (final SaveException e) {
            verify(transformer, times(1)).resourceToEntity(any(ApiPlanResource.class));
            verify(manager).add(plan);
            throw e;
        }
        fail("Expected SaveException");
    }

    @Test(expected = UpdateException.class)
    public void putUpdateException() throws Exception {
        resources.add(new ApiPlanResource("p1", "pName1", null, "policy xml 1", true));
        final ApiPlan plan = createApiPlan("p1", "pName1", null, "policy xml 1", true);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan);
        when(manager.find("p1")).thenReturn(createApiPlan("p1", "pNameA", null, "policy xml 1", true));
        doThrow(new UpdateException("mocking exception")).when(manager).update(any(ApiPlan.class));

        try {
            handler.put(resources, false);
        } catch (final UpdateException e) {
            verify(transformer, times(1)).resourceToEntity(any(ApiPlanResource.class));
            verify(manager).find("p1");
            verify(manager).update(plan);
            verify(transformer, never()).entityToResource(any(ApiPlan.class));
            throw e;
        }
        fail("Expected UpdateException");
    }

    @Test
    public void putRemoveOmitted() throws Exception {
        resources.add(new ApiPlanResource("p1", "pName1", null, "policy xml 1", true));
        resources.add(new ApiPlanResource("p2", "pName2", null, "policy xml 2", false));
        // p2 already exists
        apiPlans.add(createApiPlan("p2", "pNameB", LAST_UPDATE, "policy xml 2", true));
        // extra plan p3 also exists but is not specified in the input
        apiPlans.add(createApiPlan("p3", "pName3", LAST_UPDATE, "policy xml 3", false));
        final ApiPlan plan1 = createApiPlan("p1", "pName1", null, "policy xml 1", true);
        final ApiPlan plan2 = createApiPlan("p2", "pName2", null, "policy xml 2", false);
        when(transformer.resourceToEntity(any(ApiPlanResource.class))).thenReturn(plan1, plan2);
        when(manager.findAll()).thenReturn(apiPlans);
        when(manager.add(any(ApiPlan.class))).thenReturn(createApiPlan("p1", "pName1", LAST_UPDATE, "policy xml 1", true));
        when(manager.update(any(ApiPlan.class))).thenReturn(createApiPlan("p2", "pName2", LAST_UPDATE, "policy xml 2", true));
        final ApiPlanResource resource1 = new ApiPlanResource("p1", "pName1", LAST_UPDATE, "policy xml 1", true);
        final ApiPlanResource resource2 = new ApiPlanResource("p2", "pName2", LAST_UPDATE, "policy xml 2", true);
        when(transformer.entityToResource(any(ApiPlan.class))).thenReturn(resource1, resource2);

        final List<ApiPlanResource> result = handler.put(resources, true);

        verify(transformer, times(2)).resourceToEntity(any(ApiPlanResource.class));
        verify(manager).findAll();
        verify(manager).add(plan1);
        verify(manager).update(plan2);
        verify(manager).delete("p3");
        verify(transformer, times(2)).entityToResource(any(ApiPlan.class));
        assertEquals(2, result.size());
        assertTrue(result.contains(resource1));
        assertTrue(result.contains(resource2));
    }

    @Test
    public void delete() throws Exception {
        handler.delete("p1");

        verify(manager).delete("p1");
    }

    @Test(expected = DeleteException.class)
    public void deleteDeleteException() throws Exception {
        doThrow(new DeleteException("mocking exception")).when(manager).delete(anyString());

        try {
            handler.delete("p1");
        } catch (final DeleteException e) {
            verify(manager).delete("p1");
            throw e;
        }
        fail("Expected DeleteException");
    }

    @Test(expected = FindException.class)
    public void deleteFindException() throws Exception {
        doThrow(new FindException("mocking exception")).when(manager).delete(anyString());

        try {
            handler.delete("p1");
        } catch (final FindException e) {
            verify(manager).delete("p1");
            throw e;
        }
        fail("Expected FindException");
    }

    private ApiPlan createApiPlan(final String planId, final String planName, final Date lastUpdate, final String policyXml, final boolean defaultPlan) {
        final ApiPlan plan = new ApiPlan();
        plan.setName(planId);
        plan.setDescription(planName);
        plan.setLastUpdate(lastUpdate);
        plan.setPolicyXml(policyXml);
        plan.setDefaultPlan(defaultPlan);
        return plan;
    }

    private class NonNullLastUpdate extends ArgumentMatcher<ApiPlanResource> {
        @Override
        public boolean matches(Object o) {
            final ApiPlanResource plan = (ApiPlanResource) o;
            if (plan.getLastUpdate() != null) {
                return true;
            }
            return false;
        }
    }

    private class LastUpdateNotEqualTo extends ArgumentMatcher<ApiPlanResource> {
        private final Date date;

        LastUpdateNotEqualTo(final Date date) {
            this.date = date;
        }

        @Override
        public boolean matches(Object o) {
            final ApiPlanResource plan = (ApiPlanResource) o;
            if (!plan.getLastUpdate().equals(date)) {
                return true;
            }
            return false;
        }
    }
}
