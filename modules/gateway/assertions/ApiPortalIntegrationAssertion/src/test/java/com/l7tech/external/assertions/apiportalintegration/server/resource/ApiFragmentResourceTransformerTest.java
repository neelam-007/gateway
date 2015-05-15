package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedEncass;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ApiFragmentResourceTransformerTest {
    private ApiFragmentResourceTransformer transformer;

    @Before
    public void setup() {
        transformer = new ApiFragmentResourceTransformer();
    }

    @Test
    public void resourceToEntity() {
        final PortalManagedEncass entity = transformer.resourceToEntity(new ApiFragmentResource("a1", "id1", "true", "details"));

        assertEquals("a1", entity.getEncassGuid());
        assertEquals("id1", entity.getEncassId());
        assertTrue(entity.getHasRouting());
        assertEquals("details", entity.getParsedPolicyDetails());
    }

    @Test
    public void entityToResource() {
        final PortalManagedEncass entity = new PortalManagedEncass();
        entity.setEncassGuid("a1");
        entity.setEncassId("id1");
        entity.setHasRouting(false);
        entity.setParsedPolicyDetails("details");

        final ApiFragmentResource resource = transformer.entityToResource(entity);

        assertEquals("a1", resource.getEncassGuid());
        assertEquals("id1", resource.getEncassId());
        assertEquals("false", resource.getFragmentDetails().getHasRouting());
        assertEquals("details", resource.getFragmentDetails().getParsedPolicyDetails());
    }
}
