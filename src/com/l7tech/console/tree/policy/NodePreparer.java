package com.l7tech.console.tree.policy;

/**
 * Performs operations on a newly created node to prepare it for insertion
 * into the policy tree.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public interface NodePreparer {
    /**
     * Performs operations on a newly created node to prepare it for use,
     * returning the prepared node, which may or may not be the argument itself.
     *
     * @param node the node to prepare
     * @return the prepared node
     */
    AssertionTreeNode prepare(AssertionTreeNode node);
}
