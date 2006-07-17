/*
 * Copyright (C) 2004 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.server;

import com.l7tech.common.mime.HybridStashManager;
import com.l7tech.common.mime.StashManager;

import java.io.File;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Makes stash managers for server code that needs one.
 */
public final class StashManagerFactory {
    private StashManagerFactory() {}

    private static AtomicLong stashFileUnique = new AtomicLong(0);

    private static long getStashFileUnique() {
        return stashFileUnique.getAndIncrement();
    }

    private static class ConfigHolder {
        private static final int DISK_THRESHOLD = ServerConfig.getInstance().getAttachmentDiskThreshold();
        private static final File ATTACHMENT_DIR = ServerConfig.getInstance().getAttachmentDirectory();
    }

    /**
     * Create a new StashManager to use for some request.  A HybridStashManager will be created
     *
     * @return a new StashManager instance.  Never null.
     */
    public static StashManager createStashManager() {
        StashManager stashManager = new HybridStashManager(ConfigHolder.DISK_THRESHOLD,
                                                           ConfigHolder.ATTACHMENT_DIR,
                                                           "att" + getStashFileUnique());
        return stashManager;
    }
}
