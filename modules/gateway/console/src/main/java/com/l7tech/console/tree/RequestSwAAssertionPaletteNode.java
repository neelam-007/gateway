package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionPaletteNode extends AbstractLeafPaletteNode {

    /**
     * construct the <CODE>RequestSwAAssertionPaletteNode</CODE> instance.
     */
    public RequestSwAAssertionPaletteNode() {
        super("SOAP Request with Attachment", "com/l7tech/console/resources/xmlencryption.gif");
    }

    /**
      * Return assertion representation of the node
      * or <b>null</b> if the node cannot be an assertion
      *
      * @return the popup menu
      */
     public Assertion asAssertion() {
         return new RequestSwAAssertion();
     }
}
