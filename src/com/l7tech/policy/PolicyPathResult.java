package com.l7tech.policy;

import java.util.Set;

/**
 * Classes implementing the <code>PolicyPathResult</code> interface
 * represent a collection of paths over an assertion tree.
 *
 * @see PolicyPathBuilder
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.0
 */
public interface PolicyPathResult {
    /**
     * returns the number of paths in this result
     * @return the number of assertiuon paths
     */
    int getPathCount();

    /**
     * return the <code>Set</code> of paths that this path result contains
     *
     * @see com.l7tech.policy.AssertionPath
     * @return the set of assertion paths
     */
    Set paths();
}
