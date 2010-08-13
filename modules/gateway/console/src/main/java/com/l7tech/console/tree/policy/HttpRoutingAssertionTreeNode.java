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
import java.util.List;

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
        List<Action> list = new ArrayList<Action>(Arrays.asList(super.getActions()));

        // Check if the super action list has already contained an EditXmlSecurityRecipientContextAction or an EditKeyAliasForAssertion.
        // If the list contains an EditKeyAliasForAssertion, then update its status such as enabled/disabled and value.
        boolean foundEditXmlSecurityRecipientContextAction = false;
        boolean foundEditKeyAliasForAssertion = false;
        for (Action action: list) {
            if (action instanceof EditXmlSecurityRecipientContextAction) {
                foundEditXmlSecurityRecipientContextAction = true;
            } else if (action instanceof EditKeyAliasForAssertion) {
                foundEditKeyAliasForAssertion = true;
                if (!isUsingPrivateKey()) {
                    action.setEnabled(false);
                    action.putValue(Action.SHORT_DESCRIPTION, "Disabled because the URL is not HTTPS");
                }
            }
        }
        // If not found EditXmlSecurityRecipientContextAction, create a new one and add into the list at the first position.
        if (! foundEditXmlSecurityRecipientContextAction) {
            if (getUserObject() instanceof SecurityHeaderAddressable) {
                list.add(0, new EditXmlSecurityRecipientContextAction(this));
            }
        }
        // If not found EditKeyAliasForAssertion, create a new one and add into the list at the second position.
        if (! foundEditKeyAliasForAssertion) {
            list.add(1, new EditKeyAliasForAssertion(this));
        }

        return list.toArray(new Action[list.size()]);
    }

    protected boolean isUsingPrivateKey() {
        HttpRoutingAssertion assertion = ((HttpRoutingAssertion)getUserObject());
        if (assertion == null)
            return false;

        // If "Use Multiple URLs" option is selected, then we need to check the URLs in the multiple URLs list.
        String[] urlsList = assertion.getCustomURLs();
        if (urlsList != null) {
            if (urlsList.length > 0) {
                for (String url: urlsList) {
                    if (! url.toLowerCase().startsWith("http:")) { // If one of them starts with "https:" or it is specified by a context variable, then return true.
                        return true;
                    }
                }
                return false;
            } else { // The list cannot leave by empty after we fixed bug 8882: "Route to HTTP assertion can be saved without a routing URL"
                throw new IllegalStateException("The URLs list must not be empty if \"Use Multiple URLs\" option is chosen.");
            }
        }

        // Otherwise, check the main URL
        String url = assertion.getProtectedServiceUrl();
        return url != null && !url.toLowerCase().startsWith("http:");
    }
}
