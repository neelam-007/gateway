package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.JmsRoutingAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the JMS routing.
 *
 * @author <a href="mailto:mlyons@layer7-tech.com">Mike Lyons</a>
 * @version 1.0
 */
public class JmsRoutingNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>JmsRoutingNode</CODE> instance.
     */
    public JmsRoutingNode() {
        super("JMS Routing", "com/l7tech/console/resources/server16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new JmsRoutingAssertion();
    }
}
