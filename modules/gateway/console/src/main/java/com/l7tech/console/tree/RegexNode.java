package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.Regex;


/**
 * The class represents a node element in the TreeModel.
 * It represents the Regular Exression node.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class RegexNode extends AbstractLeafPaletteNode {
    /**
     * construct the <CODE>ResponseRegexNode</CODE> instance.
     */
    public RegexNode() {
        super("Reqular Expression", "com/l7tech/console/resources/regex16.gif");
    }

    /**
     * Return assertion representation of the node
     * or <b>null</b> if the node cannot be an assertion
     *
     * @return the popup menu
     */
    public Assertion asAssertion() {
        return new Regex();
    }
}
