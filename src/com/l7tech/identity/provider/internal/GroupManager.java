package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.Manager;

/**
 * @author alex
 */
public interface GroupManager extends Manager {
    public void delete( Group group );
    public Group create();
}
