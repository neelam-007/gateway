package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.Manager;

/**
 * @author alex
 */
public interface StateManager extends Manager {
    public void delete( State state );
    public State create();
}
