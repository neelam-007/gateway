package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.AllAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the AllAssertion node.
 */
public class AllNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>ResponseRegexNode</CODE> instance.
     */
    public AllNode() {
        super("All assertions must evaluate to true", "com/l7tech/console/resources/folder.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new AllAssertion();
    }
}
