package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HttpFormPost;


/**
 * The class represents a node element in the TreeModel.
 * It represents the HTTP Form POST node.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class HttpFormPostNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>HttpFormPostNode</CODE> instance.
     */
    public HttpFormPostNode() {
        super("Translate HTTP Form to MIME", "com/l7tech/console/resources/network.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new HttpFormPost();
    }
}
