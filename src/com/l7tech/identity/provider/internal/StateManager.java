package com.l7tech.identity.provider.internal;

import com.l7tech.objectmodel.StandardManager;

/**
 * @author alex
 */
public interface StateManager extends StandardManager {
    public void delete( State state );
    public State create();
}
