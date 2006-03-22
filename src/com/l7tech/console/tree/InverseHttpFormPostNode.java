package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.InverseHttpFormPost;


/**
 * The class represents a node element in the TreeModel.
 * It represents the Inverse HTTP Form POST node.
 */
public class InverseHttpFormPostNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>HttpFormPostNode</CODE> instance.
     */
    public InverseHttpFormPostNode() {
        super("Translate MIME to HTTP Form", "com/l7tech/console/resources/network.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new InverseHttpFormPost();
    }
}
