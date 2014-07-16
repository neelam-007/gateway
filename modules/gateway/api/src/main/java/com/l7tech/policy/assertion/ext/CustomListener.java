package com.l7tech.policy.assertion.ext;

/**
 * An interface that all custom listeners must extend.
 */
public interface CustomListener<Callback> {

    /**
     * Adds a callback that will be invoked.
     *
     * @param callback the callback to add
     */
    void add(Callback callback);

    /**
     * Removes a callback that has been added.
     *
     * @param callback the callback to remove
     */
    void remove(Callback callback);
}