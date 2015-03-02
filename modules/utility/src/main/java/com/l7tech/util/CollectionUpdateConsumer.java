/*
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.util;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Base class of consumer of updates produced by a {@link CollectionUpdateProducer}.
 *
 * <p>To create an update consumer, subclass this and implement {@link #getUpdate}.
 *
 * <p>This class is thread-safe (if the replication collection, i.e., the argument
 * passed into {@link #update}, is thread-safe).
 *
 * <p>See {@link CollectionUpdate} for full description.
 *
 * @see CollectionUpdate
 * @see CollectionUpdateProducer
 * @since SecureSpan 5.0
 * @author rmak
 */
public abstract class CollectionUpdateConsumer<T, E extends Exception> {

    /**
     * To be implemented by subclass to return the changes from a previous version;
     * typically by calling {@link CollectionUpdateProducer#createUpdate(int)}
     * through some remote communication.
     *
     * @param oldVersionID  ID of previous version to calculate changes
     * @return update to the collection
     * @throws E if failed to get update
     */
    protected abstract CollectionUpdate<T> getUpdate(final int oldVersionID) throws E;

    /**
     * Constructs a collection update consumer.
     *
     * @param differentiator    if not null, supplies a special <code>equals</code> method
     *                          for determining changed items; instead of using the
     *                          <code>equals<code> method in T
     */
    public CollectionUpdateConsumer(final CollectionUpdate.Differentiator<T> differentiator) {
        _differentiator = differentiator;
    }

    /**
     * Updates a replicating collection.
     * Internally calls {@link #getUpdate}.
     *
     * @param data  the replicating collection copy to update
     * @return a <code>Pair</code> whose left is items added, and whose right is items removed
     * @throws E if {@link #getUpdate} failed
     */
    public synchronized Pair<Collection<T>, Collection<T>> update(final Collection<T> data) throws E {
        Collection<T> added = null;
        Collection<T> removed = null;

        final CollectionUpdate<T> update = getUpdate(_oldVersionID);
        if (update.getOldVersionID() == _oldVersionID) {
            added = update.getAdded();
            removed = update.getRemoved();
        } else if (update.getOldVersionID() == CollectionUpdate.NO_VERSION) {
            // Either:
            // (1) this is the first query, or
            // (2) the CollectionUpdateProducer has purged the previous version
            //     and returned the full collection in CollectionUpdate#getAdded()
            // We have to sort out which item is added and removed here.
            final Collection<T> oldData = data;
            final Collection<T> newData = update.getAdded();

            added = new ArrayList<T>(newData);
            added.removeAll(oldData);

            removed = new ArrayList<T>(oldData);
            removed.removeAll(newData);
        } else {
            throw new RuntimeException("Internal Error: Don't know where this old version ID come from: " + update.getOldVersionID());
        }
        //JDK8 does not allow null reference to be removed although it worked fine in JDK7 when the collection is null and the array list is empty
        if(removed != null) data.removeAll(removed);
        data.addAll(added);
        _oldVersionID = update.getNewVersionID();
        return new Pair<Collection<T>, Collection<T>>(added, removed);
    }

    /**
     * Updates a copy of the underlying collection as well as a
     * <code>MutableComboBoxModel</code>.
     * Internally calls {@link #getUpdate}.
     *
     * <p>Note that caller is responsible for initial population of the
     * <code>MutableComboBoxModel</code>.
     *
     * @param data          collection copy to update
     * @param comboModel    combo box model to be applied the same update
     * @return a <code>Pair</code> whose left is items added, and whose right is items removed
     * @throws E if {@link #getUpdate} failed
     */
    public synchronized Pair<Collection<T>, Collection<T>> update(final Collection<T> data, final MutableComboBoxModel comboModel) throws E {
        final Pair<Collection<T>, Collection<T>> addedRemoved = update(data);
        final Collection<T> added = addedRemoved.left;
        final Collection<T> removed = addedRemoved.right;

        // Removes removed items from model.
        if (removed != null) {
            for (T o : removed) {
                comboModel.removeElement(o);
            }
        }

        // Adds added items to model.
        if (added != null) {
            for (T o : added) {
                comboModel.addElement(o);
            }
        }

        return addedRemoved;
    }

    /** Version ID of the most recent update. */
    private int _oldVersionID = CollectionUpdate.NO_VERSION;

    private final CollectionUpdate.Differentiator<T> _differentiator;
}
