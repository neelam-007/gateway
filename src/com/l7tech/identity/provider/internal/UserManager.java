package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.StandardManager;

import java.util.Collection;

/**
 * @author alex
 */
public interface UserManager extends StandardManager {
    public void delete( User user );
    public User create();
}
