package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.Manager;

import java.util.Collection;

/**
 * @author alex
 */
public interface UserManager extends Manager {
    public void delete( User user );
    public User create();
}
