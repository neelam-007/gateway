package com.l7tech.console.tree.policy;


import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;
import com.l7tech.policy.assertion.identity.IdentityAssertion;

import java.util.Iterator;

/**
 * Class OneOrMoreAssertionTreeNode.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
class OneOrMoreAssertionTreeNode extends CompositeAssertionTreeNode {
    /**
     * The <code>MemberOfGroupAssertionTreeNode</code> is the composite
     * assertion node that represents the group membership.
     *
     * @param assertion the composite assertion
     */
    public OneOrMoreAssertionTreeNode(OneOrMoreAssertion assertion) {
        super(assertion);
        if (assertion == null) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * specify this node image resource
     */
    protected String iconResource(boolean open) {
        if (open)
            return "com/l7tech/console/resources/folderOpen.gif";

        return "com/l7tech/console/resources/folder.gif";
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "At least on assertion must evaluate to true";
    }
}