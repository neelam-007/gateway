package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.StandardManager;

/**
 * @author alex
 */
public interface OrganizationManager extends StandardManager {
    public void delete( Organization organization );
    public Organization create();
}
