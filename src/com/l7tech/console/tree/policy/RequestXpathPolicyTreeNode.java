/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditXpathAssertionAction;
import com.l7tech.policy.assertion.RequestXpathAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author alex
 * @version $Revision$
 */
public class RequestXpathPolicyTreeNode extends LeafAssertionTreeNode {
    public RequestXpathPolicyTreeNode( RequestXpathAssertion assertion ) {
        super( assertion );
        _assertion = assertion;
    }

    public String getName() {
        return "Request must match XPath '" + _assertion.pattern() + "'";
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    public Action[] getActions() {
        java.util.List list = new ArrayList();
        Action a = new EditXpathAssertionAction(this);
        list.add(a);
        list.addAll(Arrays.asList(super.getActions()));
        return (Action[]) list.toArray(new Action[]{});
    }
    /**
       * Gets the default action for this node.
       *
       * @return <code>null</code> indicating there should be none default action
       */
      public Action getPreferredAction() {
          return new EditXpathAssertionAction(this);
      }

    public boolean canDelete() {
        return true;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    private RequestXpathAssertion _assertion;
}
