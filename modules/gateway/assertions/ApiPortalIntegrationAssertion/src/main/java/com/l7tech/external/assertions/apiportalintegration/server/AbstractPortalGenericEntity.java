package com.l7tech.external.assertions.apiportalintegration.server;

import com.l7tech.policy.GenericEntity;

/**
 * Abstract parent class for all generic entities that are portal-related.
 */
public abstract class AbstractPortalGenericEntity extends GenericEntity {
    /**
     * @return a readonly copy of the AbstractPortalGenericEntity that cannot be modified.
     */
    public abstract AbstractPortalGenericEntity getReadOnlyCopy();
}
