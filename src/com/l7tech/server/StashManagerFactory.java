/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.common.mime.StashManager;

/**
 * Makes stash managers for server code that needs one.
 */
public final class StashManagerFactory {
    private StashManagerFactory() {}

    private static int stashFileUnique = 0;

    private static synchronized int getStashFileUnique() {
        return stashFileUnique++;
    }

    /**
     * Create a new StashManager to use for some request.  A HybridStashManager will be created
     *
     * @return
     */
    public static StashManager createStashManager() {
        StashManager stashManager = new HybridStashManager(ServerConfig.getInstance().getAttachmentDiskThreshold(),
                                              ServerConfig.getInstance().getAttachmentDirectory(),
                                              "att" + getStashFileUnique());
        return stashManager;
    }
}
