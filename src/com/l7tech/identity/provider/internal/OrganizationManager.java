package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.Manager;

/**
 * @author alex
 */
public interface OrganizationManager extends Manager {
    public void delete( Organization organization );
    public Organization create();
}
