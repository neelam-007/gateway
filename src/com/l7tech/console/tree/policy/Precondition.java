package com.l7tech.console.tree.policy;

/**
 * Class <code>Precondition</code> represents a precondition that is
 * executed (and potentially checked) before the node is inserted into
 * the policy.
 * <p>
 * Typical usage is when the user sleects the http client certificate
 * then the precondition adds the ssl assertion.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public abstract class Precondition implements Runnable {
    protected final AssertionTreeNode node;

    public Precondition(AssertionTreeNode node) {
        this.node = node;
    }

    /** empty node precondition */
    public static final Precondition EMPTY = new Precondition(null) {
        public final void run() {
            //empty
        }
    };
}
