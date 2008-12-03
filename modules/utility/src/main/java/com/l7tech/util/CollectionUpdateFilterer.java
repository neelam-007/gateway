/**
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This only exists because the relevant methods of {@Link CollectionUpdate} are package-private.
 *
 * @author alex
 */
public class CollectionUpdateFilterer {
    public static <T> CollectionUpdate<T> filter(CollectionUpdate<T> ts, Collection<T> removals) {
        final List<T> newAdded = ts.getAdded() == null ? Collections.<T>emptyList() : new ArrayList<T>(ts.getAdded());
        final List<T> newRemoved = ts.getRemoved() == null ? Collections.<T>emptyList() : new ArrayList<T>(ts.getRemoved());
        final CollectionUpdate<T> newts = new CollectionUpdate<T>(ts.getOldVersionID(), ts.getNewVersionID(), newAdded, newRemoved);
        for (T removal : removals) {
            newts.getAdded().remove(removal);
            newts.getRemoved().remove(removal);
        }
        return newts;
    }
}
