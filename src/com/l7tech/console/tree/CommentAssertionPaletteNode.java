package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.CommentAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the {@link com.l7tech.policy.assertion.CommentAssertion}.
 */
public class CommentAssertionPaletteNode extends AbstractLeafPaletteNode {
    public CommentAssertionPaletteNode() {
        super("Comment","com/l7tech/console/resources/About16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new CommentAssertion();
    }
}
