/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.server.util;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utility superclass containing a working implementation of {@link ReferenceCounted}.
 */
public abstract class AbstractReferenceCounted<HT extends Handle> extends AbstractCloseable implements ReferenceCounted {
    private final AtomicInteger refcount = new AtomicInteger(0);

    protected abstract HT createHandle();

    public HT ref() {
        refcount.incrementAndGet();
        return createHandle();
    }

    public void unref() {
        int nval = refcount.decrementAndGet();
        if (nval <= 0) {
            close();
        }
    }
}
