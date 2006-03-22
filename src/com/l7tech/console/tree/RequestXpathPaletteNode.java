package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestXpathAssertion;


/**
 * The class represents a node element in the TreeModel.
 * It represents the XPath node.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RequestXpathPaletteNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>RequestXpathPaletteNode</CODE> instance.
     */
    public RequestXpathPaletteNode() {
        super("Evaluate Request XPath", "com/l7tech/console/resources/xmlsignature.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new RequestXpathAssertion();
    }
}
