/*
 * Copyright (C) 2008 Layer 7 Technologies Inc.
 */
package com.l7tech.common.util;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;

/**
 * Encapsulates changes in a {@link Collection} of class <code>T</code> objects
 * since a previous version.
 *
 * <p>Changes are divided into
 * <ul>
 *  <li>items added
 *  <li>items removed
 * </ul>
 * There is no consideration for items modified because such concept requires
 * <ul>
 *  <li>distinction between identifying and non-identifying fields in order to
 *      tell the difference between new item or modified item
 *  <li>mutable fields in order to update a modified item
 * </ul>
 * These conditions are not common enough to warrant the effort at present.
 *
 * <p><code>CollectionUpdate</code> is used in conjunction with
 * {@link CollectionUpdateProducer} and {@link CollectionUpdateConsumer} to
 * reduce polling traffic in a client-server situation.
 *
 * <h3>Scenerio</h3>
 * <p>Suppose there is an original {@link Collection} on the server side and it
 * needs to be replicated and updated frequently on the client side by polling.
 * And suppose the original collection is potentially large and does not
 * change very often.
 *
 * <p>If periodic polling fetches the entire collection every time, the response
 * bytes are large even when there is no change in the original collection.
 * But if a <code>CollectionUpdateProducer</code> and a <code>CollectionUpdateConsumer</code>
 * are used, only changes in collection are transmitted in each polling round.
 *
 * <blockquote>
 * replicated collection &larr; <code>CollectionUpdateConsumer</code> (client-side) &harr; RMI &harr; (server-side) <code>CollectionUpdateProducer</code> &larr; original collection
 * </blockquote>
 *
 * <p>Note that one producer can serve multiple consumers.
 *
 * <h3>Example Code</h3>
 * On the server-side,
 * <blockquote><pre>
 * private CollectionUpdateProducer&lt;Foo, FooException> updateProducer =
 *         new CollectionUpdateProducer&lt;Foo, FooException>(60000, 50, null) {
 *             protected Collection&lt;Foo> getCollection() throws FindException {
 *                 return ...the original collection...
 *             }
 *         };
 *
 * // Remote interface implementation.
 * public CollectionUpdate&lt;Foo> getFooUpdate(final int oldVersionID) throws FooException {
 *     return updateProducer.createUpdate(oldVersionID);
 * }
 * </pre></blockquote>
 *
 * On the client-side,
 * <blockquote><pre>
 * private CollectionUpdateConsumer&lt;Foo, Exception> updateConsumer =
 *         new CollectionUpdateConsumer&lt;Foo, FooException>(null) {
 *             protected CollectionUpdate&lt;Foo> getUpdate(final int oldVersionID) throws FooException {
 *                 return myRemoteServer.getFooUpdate(oldVersionID);
 *             }
 *         };
 *
 * List&lt;Foo> foosCopy = new ArrayList&lt;Foo>();
 * ...polling loop...
 *    updateConsumer.update(foosCopy);
 * </pre></blockquote>
 *
 * <p>Normally, the <code>equals</code> method (and perhaps <code>hashCode</code>)
 * of the collection element class is used to determine what is old or new. But
 * user can supply an alternative <code>equals</code> method in a {@link Differentiator}.
 *
 * <p>This class is to be used opaquely; all methods are package private.
 *
 * @see CollectionUpdateProducer
 * @see CollectionUpdateConsumer
 * @since SecureSpan 5.0
 * @author rmak
 */
public class CollectionUpdate<T> implements Serializable {

    /**
     * A differentiator supplies an alternative <code>equals</code> method.
     * Handy when a class's <code>equals</code> method does not provide the
     * desired discrimination between instances.
     */
    public interface Differentiator<T> {
        boolean equals(final T a, final T b);
    }

    //
    // Package Private
    //

    /**
     * Same as {@link Collection#removeAll} except allowing a different
     * <code>equals</code> method.
     *
     * @param target            the collection to modify
     * @param excludes          collection containing elements to be removed from <code>target</code>
     * @param differentiator    if not null, supplies a special <code>equals</code> method; instead of using the <code>equals<code> method in T
     * @return <code>true</code> if this collection changed as a result of the call
     */
    static <T> boolean removeAll(final Collection<T> target,
                                 final Collection<T> excludes,
                                 final Differentiator<T> differentiator) {
        boolean modified = false;
        if (differentiator == null) {
            modified = target.removeAll(excludes);
        } else {
            final Iterator<T> itor = target.iterator();
            while (itor.hasNext()) {
                final T candidate = itor.next();
                for (T exclude : excludes) {
                    if (differentiator.equals(candidate, exclude)) {
                        itor.remove();
                        modified = true;
                        break;
                    }
                }
            }
        }
        return modified;
    }

    static final int NO_VERSION = -1;

    private int _oldVersionID = NO_VERSION;
    private int _newVersionID = NO_VERSION;
    private Collection<T> _removed;
    private Collection<T> _added;

    CollectionUpdate() {
    }

    CollectionUpdate(final int oldVersionID,
                     final int newVersionID,
                     final Collection<T> added,
                     final Collection<T> removed) {
        _oldVersionID = oldVersionID;
        _newVersionID = newVersionID;
        _added = added;
        _removed = removed;
    }

    /**
     * <p>Returns the ID of the previous version used in producing the update.
     * {@link #NO_VERSION} is used to mean not compared against a previous version;
     * the full collection will be returned by {@link #getAdded}.
     *
     * @return ID of the previous version
     */
    int getOldVersionID() {
        return _oldVersionID;
    }

    /**
     * @id use {@link #NO_VERSION} if not comparing against a previous version
     */
    void setOldVersionID(int id) {
        _oldVersionID = id;
    }

    /**
     * @return version ID of new data
     */
    int getNewVersionID() {
        return _newVersionID;
    }

    void setNewVersionID(int id) {
        _newVersionID = id;
    }

    /**
     * @return <code>null</code> or empty if nothing added
     */
    Collection<T> getAdded() {
        return _added;
    }

    void setAdded(Collection<T> added) {
        _added = added;
    }

    /**
     * @return <code>null</code> or empty if nothing removed
     */
    Collection<T> getRemoved() {
        return _removed;
    }

    void setRemoved(Collection<T> removed) {
        _removed = removed;
    }
}
