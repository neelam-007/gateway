package com.l7tech.util;

/**
 * Generic decorator interface.
 *
 * @author Steve Jones
 */
public interface Decorator<O> {

    /**
     * Decorate the given object.
     *
     * @param undecorated The object to decorate
     * @return The decorated version
     */
    O decorate(O undecorated);

    /**
     * Interface that can be implemented by decorated objects.
     *
     * <p>This allows unwrapping.<p>
     */
    interface Decorated<O> {

        /**
         * Get the undecorated version of the object.
         *
         * <p>Note that the underlying object may also be decorated.</p>
         *
         * @return The underlying object.
         */
        O undecorate();
    }
}
