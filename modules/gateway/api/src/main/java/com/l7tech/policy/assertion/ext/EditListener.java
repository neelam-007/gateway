package com.l7tech.policy.assertion.ext;

/**
 * Listener implementations are registered with the <code>AssertionEditor</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public interface EditListener {
    /**
     * Fired when the edit is accepted.
     *
     * @param source the event source
     * @param bean   the object being edited
     */
    void onEditAccepted(Object source, Object bean);

    /**
     * Fired when the edit is cancelled.
     *
     * @param source the event source
     * @param bean   the object being edited
     */
    void onEditCancelled(Object source, Object bean);

}
