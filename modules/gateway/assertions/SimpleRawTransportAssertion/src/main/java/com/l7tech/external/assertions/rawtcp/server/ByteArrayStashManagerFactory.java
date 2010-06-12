package com.l7tech.external.assertions.rawtcp.server;

import com.l7tech.common.mime.ByteArrayStashManager;
import com.l7tech.common.mime.StashManager;
import com.l7tech.server.StashManagerFactory;

/**
 * A {@link com.l7tech.server.StashManagerFactory} that always just creates a new {@link ByteArrayStashManager}.
 * <p/>
 * This should normally not be used except for testing purposes because attempting to handle large
 * messages with this stash manager factory can run the Gateway out of memory.
 */
public class ByteArrayStashManagerFactory implements StashManagerFactory {
    @Override
    public StashManager createStashManager() {
        return new ByteArrayStashManager();
    }
}
