package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.StandardManager;

/**
 * @author alex
 */
public interface GroupManager extends StandardManager {
    public void delete( Group group );
    public Group create();
}
