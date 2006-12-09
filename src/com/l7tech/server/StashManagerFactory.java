package com.l7tech.server;

import com.l7tech.common.mime.StashManager;

/**
 * Factory for creating StashManagers.
 *
 * @author Steve Jones
 */
public interface StashManagerFactory {

    /**
     * Create a new StashManager to use for some request.  A HybridStashManager will be created
     *
     * @return a new StashManager instance.  Never null.
     */
    public StashManager createStashManager();
}
