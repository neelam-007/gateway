package com.l7tech.console.tree;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.RequestSwAAssertion;

import javax.swing.*;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class RequestSwAAssertionPaletteNode extends AbstractTreeNode {

    /**
     * construct the <CODE>RequestSwAAssertionPaletteNode</CODE> instance.
     */
    public RequestSwAAssertionPaletteNode() {
        super(null);
    }

    public boolean getAllowsChildren() {
        return false;
    }

    /**
     * subclasses override this method
     */
    protected void loadChildren() {}

    public String getName() {
        return "SOAP Request with Attachment";
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
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

     /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        return new Action[]{};
    }

}
