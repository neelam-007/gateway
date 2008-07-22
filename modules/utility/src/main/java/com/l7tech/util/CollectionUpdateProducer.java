/*
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.util;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class of producer of updates to be consumed by a {@link CollectionUpdateConsumer}.
 *
 * <p>To create an update producer, subclass this and implement {@link #getCollection}.
 *
 * <p>This class is thread-safe (if your implementation of {@link #getCollection} is thread-safe).
 *
 * <p>See {@link CollectionUpdate} for usage description.
 *
 * @see CollectionUpdate
 * @see CollectionUpdateConsumer
 * @since SecureSpan 5.0
 * @author rmak
 */
public abstract class CollectionUpdateProducer<T, E extends Exception> {

    /**
     * To be implemented by subclass to return a current copy of the original collection.
     *
     * @return the current original collection
     * @throws E if failure to get the original collection
     */
    protected abstract Collection<T> getCollection() throws E;

    /**
     * Constructs a collection update producer.
     *
     * @param maxAge            maximum age (in milliseconds) of versions to maintain;
     *                          zero or negative for no maximum
     * @param maxVersions       maximum number of versions to maintain;
     *                          zero or negative for no maximum
     * @param differentiator    if not null, supplies a special <code>equals</code> method
     *                          for determining changed items; instead of using the
     *                          <code>equals<code> method in T
     */
    public CollectionUpdateProducer(final long maxAge, final int maxVersions, final CollectionUpdate.Differentiator<T> differentiator) {
        _maxAge = maxAge;
        _maxVersions = maxVersions;
        _differentiator = differentiator;
    }

    /**
     * Create a <code>CollectionUpdate</code> based on difference between
     * the specified previous version and the current original collection.
     * Internally calls {@link #getCollection}.
     * 
     * @param oldVersionID  the previous version to diff
     * @return update to the collection
     * @throws E if {@link #getCollection} failed
     */
    public CollectionUpdate<T> createUpdate(final int oldVersionID) throws E {
        final CollectionUpdate<T> result = new CollectionUpdate<T>();

        final Collection<T> newData = getCollection();
        final long newVersionTime = System.currentTimeMillis();
        int newVersionID;
        do {
            newVersionID = _nextVersionID.getAndIncrement();
        } while (newVersionID == CollectionUpdate.NO_VERSION);  // Make sure integer wrap-around skips our special value.
        result.setNewVersionID(newVersionID);

        final Version<T> oldVersion = _versions.remove(oldVersionID);
        if (oldVersion == null) {
            result.setAdded(newData);
            // result._oldVersionID is left as NO_VERSION
        } else {
            result.setOldVersionID(oldVersionID);
            final Collection<T> oldData = oldVersion.data;

            final Collection<T> added = new HashSet<T>(newData);
            CollectionUpdate.removeAll(added, oldData, _differentiator);
            result.setAdded(added);

            final Collection<T> removed = new HashSet<T>(oldData);
            CollectionUpdate.removeAll(removed, newData, _differentiator);
            result.setRemoved(removed);
        }

        _versions.put(newVersionID, new Version<T>(newVersionTime, newData));

        return result;
    }

    /** Maximum age (in milliseconds) of old versions to maintain. Zero or negative for no maximum. */
    private long _maxAge;

    /** Maximum number of old versions to maintain. Zero or negative for no maximum. */
    private int _maxVersions;

    private final CollectionUpdate.Differentiator<T> _differentiator;

    private final AtomicInteger _nextVersionID = new AtomicInteger(0);

    /** Repository of old versions.
        Map key is version ID.
        Map value is copy of the original collection. */
    private final Map<Integer, Version<T>> _versions = Collections.synchronizedMap(
            new LinkedHashMap<Integer, Version<T>>() {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, Version<T>> eldest) {
                    return _maxVersions > 0 && size() > _maxVersions ||
                           _maxAge > 0 && (System.currentTimeMillis() - eldest.getValue().time) > _maxAge;
                 }
            });

    /**
     * A snapshot of the original collection at a specific time.
     */
    private static class Version<T> {
        public final long time;
        public final Collection<T> data;
        public Version(final long time, final Collection<T> data) {
            this.time = time;
            this.data = data;
        }
    }
}
