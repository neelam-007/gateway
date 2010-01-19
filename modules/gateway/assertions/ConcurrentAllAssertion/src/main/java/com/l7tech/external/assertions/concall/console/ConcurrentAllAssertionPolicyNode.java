package com.l7tech.external.assertions.concall.console;

import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.external.assertions.concall.ConcurrentAllAssertion;

/**
 *
 */
public class ConcurrentAllAssertionPolicyNode extends CompositeAssertionTreeNode<ConcurrentAllAssertion>  {
    public ConcurrentAllAssertionPolicyNode(ConcurrentAllAssertion assertion) {
        super(assertion);
    }
}
