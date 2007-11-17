package com.l7tech.console.tree.policy;


import com.l7tech.console.action.RegexPropertiesAction;
import com.l7tech.policy.assertion.Regex;

import javax.swing.*;

/**
 * Class RegexPolicyNode is a policy node that corresponds the
 * <code>Regex</code> assertion.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 */
public class RegexPolicyNode extends LeafAssertionTreeNode {

    public RegexPolicyNode(Regex assertion) {
        super(assertion);
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        Regex regex = (Regex) asAssertion();
        StringBuffer nameBuffer = new StringBuffer(256);
        nameBuffer.append("Reqular Expression");

        if (regex.getRegexName() != null) {
            nameBuffer.append(" - ");
            nameBuffer.append(regex.getRegexName());
        }

        return nameBuffer.toString();
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    public Action getPreferredAction() {
        return new RegexPropertiesAction(this);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/regex16.gif";
    }
}