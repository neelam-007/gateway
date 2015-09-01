package com.l7tech.external.assertions.apiportalintegration.server.resource;

import com.l7tech.external.assertions.apiportalintegration.server.PortalManagedEncass;
import org.jetbrains.annotations.NotNull;

/**
 * Handles transformation between {@link ApiFragmentResource} and {@link PortalManagedEncass}.
 */
public class ApiFragmentResourceTransformer implements ResourceTransformer<ApiFragmentResource, PortalManagedEncass> {
    public static ApiFragmentResourceTransformer getInstance() {
        if (instance == null) {
            instance = new ApiFragmentResourceTransformer();
        }
        return instance;
    }

    @Override
    public PortalManagedEncass resourceToEntity(final @NotNull ApiFragmentResource resource) {
        final PortalManagedEncass entity = new PortalManagedEncass();
        entity.setEncassGuid(resource.getEncassGuid());
        entity.setEncassId(resource.getEncassId());
        final FragmentDetails details = resource.getFragmentDetails();
        entity.setHasRouting(Boolean.parseBoolean(details.getHasRouting()));
        entity.setParsedPolicyDetails(details.getParsedPolicyDetails());
        return entity;
    }

    @Override
    public ApiFragmentResource entityToResource(final @NotNull PortalManagedEncass entity) {
        return new ApiFragmentResource(entity.getEncassGuid(), entity.getEncassId(),
                Boolean.toString(entity.getHasRouting()), entity.getParsedPolicyDetails());
    }

    private static ApiFragmentResourceTransformer instance;
}
