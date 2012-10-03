package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedService;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ApiResourceTransformerTest {
    private ApiResourceTransformer transformer;

    @Before
    public void setup() {
        transformer = new ApiResourceTransformer();
    }

    @Test
    public void resourceToEntity() {
        final PortalManagedService entity = transformer.resourceToEntity(new ApiResource("a1", "g1", "1111"));

        assertEquals("a1", entity.getName());
        assertEquals("g1", entity.getApiGroup());
        assertEquals("1111", entity.getDescription());
    }

    @Test
    public void entityToResource() {
        final PortalManagedService entity = new PortalManagedService();
        entity.setName("a1");
        entity.setApiGroup("g1");
        entity.setDescription("1111");

        final ApiResource resource = transformer.entityToResource(entity);

        assertEquals("a1", resource.getApiId());
        assertEquals("g1", resource.getApiGroup());
        assertEquals("1111", resource.getServiceOid());
    }
}
