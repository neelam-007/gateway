package com.l7tech.external.assertions.apiportalintegration.server;

import javax.persistence.Transient;

/**
 * Name = unique API id.
 * <p/>
 * Description = published service oid.
 */
public class PortalManagedService extends AbstractPortalGenericEntity {
    private String apiGroup;

    public String getApiGroup() {
        return apiGroup;
    }

    public void setApiGroup(final String apiGroup) {
        checkLocked();
        this.apiGroup = apiGroup;
    }

    @Transient
    public PortalManagedService getReadOnlyCopy() {
        final PortalManagedService readOnly = new PortalManagedService();
        copyBaseFields(this, readOnly);
        readOnly.setApiGroup(this.getApiGroup());
        readOnly.lock();
        return readOnly;
    }
}
