package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.apiplan.ApiPlan;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class ApiPlanResourceTransformerTest {
    private static final Date LAST_UPDATE = new Date();
    private ApiPlanResourceTransformer transformer;

    @Before
    public void setup() {
        transformer = new ApiPlanResourceTransformer();
    }

    @Test
    public void resourceToEntity() {
        final ApiPlan entity = transformer.resourceToEntity(new ApiPlanResource("id", "name", LAST_UPDATE, "policy", true));

        assertEquals("id", entity.getName());
        assertEquals("name", entity.getDescription());
        assertEquals(LAST_UPDATE, entity.getLastUpdate());
        assertTrue(entity.isDefaultPlan());
    }

    @Test
    public void entityToResource() {
        final ApiPlan entity = new ApiPlan();
        entity.setName("id");
        entity.setDescription("name");
        entity.setLastUpdate(LAST_UPDATE);
        entity.setDefaultPlan(true);
        final ApiPlanResource resource = transformer.entityToResource(entity);

        assertEquals("id", resource.getPlanId());
        assertEquals("name", resource.getPlanName());
        assertEquals(LAST_UPDATE, resource.getLastUpdate());
        assertTrue(resource.isDefaultPlan());
    }
}
