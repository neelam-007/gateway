package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import java.util.Iterator;

/**
 * Subclasses provide strategies of adding the children nodes to the policy
 * composite nodes.
 *
 * @author emil
 * @version 18-Apr-2004
 */
public abstract class LoadChildrenStrategy {
    /**
     * Factory method, instantiate the <code>LoadChildrenStrategy</code>
     * implementation.
     *
     * @param node the assertion tree node for which the strategy is requested
     * @return the <code>LoadChildrenStrategy</code> for the node
     */
    public static LoadChildrenStrategy newStrategy(AssertionTreeNode node) {
        if (PolicyTree.isIdentityView((AssertionTreeNode)node.getRoot())) {
            return new IdentityLoadChildrenStrategy();
        }
        return new DefaultLoadChildrenStrategy();
    }

    /**
     * Default strategy, populates the nodes from the composite assertion into the receiver
     *
     * @param receiver the receiver assertion
     * @param assertion the composite assertion's childrend are added to the receiver
     */
    public abstract void loadChildren(AssertionTreeNode receiver, CompositeAssertion assertion);

    static class DefaultLoadChildrenStrategy extends LoadChildrenStrategy {
        public void loadChildren(AssertionTreeNode receiver, CompositeAssertion assertion) {
            int index = 0;
            for (Iterator iterator = assertion.children(); iterator.hasNext();) {
                Assertion a = (Assertion)iterator.next();
                receiver.insert(AssertionTreeNodeFactory.asTreeNode(a), index++);
            }
        }
    }
}
