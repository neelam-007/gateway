package com.l7tech.gateway.api.impl;

import com.l7tech.util.ResourceUtils;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class ResourceTracker implements Closeable {

    //- PUBLIC

    public void registerCloseable( final Closeable closeable ) {
        closeableResources.add( closeable );
    }

    public void unregisterCloseable( final Closeable closeable ) {
        closeableResources.remove( closeable );
    }

    @Override
    public void close() throws IOException {
        for ( final Closeable closeable : closeableResources ) {
            ResourceUtils.closeQuietly( closeable );
        }

        closeableResources.clear();
    }

    //- PRIVATE

    private final Set<Closeable> closeableResources = new HashSet<Closeable>();
}
