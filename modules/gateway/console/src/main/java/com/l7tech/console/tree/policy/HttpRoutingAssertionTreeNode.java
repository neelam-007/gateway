/*
 * Copyright (C) 2003-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.policy.assertion.HttpRoutingAssertion;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

public class HttpRoutingAssertionTreeNode extends DefaultAssertionPolicyNode<HttpRoutingAssertion> {

    public HttpRoutingAssertionTreeNode(HttpRoutingAssertion assertion) {
        super(assertion);
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @Override
    public Action[] getActions() {
        EditKeyAliasForAssertion privateKeyAction = new EditKeyAliasForAssertion(this);
        if (!isUsingPrivateKey()) {
            privateKeyAction.setEnabled(false);
            privateKeyAction.putValue(Action.SHORT_DESCRIPTION, "Disabled because the URL is not HTTPS");
        }

        java.util.List<Action> list = new ArrayList<Action>();
        list.add(privateKeyAction);
        if (getUserObject() instanceof SecurityHeaderAddressable) {
            list.add(new EditXmlSecurityRecipientContextAction(this));
        }
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[list.size()]);
    }

    protected boolean isUsingPrivateKey() {
        HttpRoutingAssertion assertion = ((HttpRoutingAssertion)getUserObject());
        if (assertion == null)
            return false;
        String url = assertion.getProtectedServiceUrl();
        return url != null && !url.toLowerCase().startsWith("http:");
    }

}
