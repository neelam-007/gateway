package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;


/**
 * The class <code>AssertionTreeNodeFactory</code> is a factory
 * class that creates <code>TreeNode</code> instances based on
 * <code>Assertion</code> instances.
 *
 *
 * @author <a href="mailto:emarceta@layer7-tech.com>Emil Marceta</a>
 * @version 1.1
 */
class AssertionTreeNodeFactory {
    /**
     * private constructor, this class cannot be instantiated
     */
    private AssertionTreeNodeFactory() {
    }

    /**
     * Returns the corresponding TreeNode instance for
     * an directory <code>Entry</code>
     *
     * @return the TreeNode for a given Entry
     */
    static AssertionTreeNode asTreeNode(Assertion assertion) {
        if (assertion == null) {
            throw new IllegalArgumentException();
        }

        if (assertion instanceof CompositeAssertion) {
            return makeCompositeAssertionNode((CompositeAssertion)assertion);
        }
        return makeLeafAssertionNode(assertion);
    }

    private static AssertionTreeNode makeCompositeAssertionNode(CompositeAssertion assertion) {
        return new CompositeAssertionTreeNode(assertion);
    }

    private static AssertionTreeNode makeLeafAssertionNode(Assertion assertion) {
        return new LeafAssertionTreeNode(assertion);
    }
}
