package com.l7tech.console.event;

import java.util.EventListener;

/**
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public interface PolicyWillChangeListener extends EventListener {
     /**
     * Invoked whenever a node in the tree is about to be expanded.
     */
    public void policyWillReceive(PolicyEvent event) throws PolicyChangeVetoException;

    /**
     * Invoked whenever a node in the tree is about to be collapsed.
     */
    public void policyWillRemove(PolicyEvent event) throws PolicyChangeVetoException;
}
