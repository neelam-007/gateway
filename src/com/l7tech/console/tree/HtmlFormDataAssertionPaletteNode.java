/**
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.HtmlFormDataAssertion;

/**
 * A leaf node in the assertions palette tree that represents the HTML Form Data Assertion.
 *
 * @author rmak
 * @since SecureSpan 3.7
 */
public class HtmlFormDataAssertionPaletteNode extends AbstractLeafPaletteNode {
    public HtmlFormDataAssertionPaletteNode() {
        super("HTML Form Data", "com/l7tech/console/resources/check16.gif");
    }

    /**
     * Return a new instance of the underlying assertion.
     */
    public Assertion asAssertion() {
        return new HtmlFormDataAssertion();
    }
}
