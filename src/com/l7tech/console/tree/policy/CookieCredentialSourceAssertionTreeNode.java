/*
 * Copyright (C) 2005 Layer 7 Technologies Inc.
 *
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.CookieCredentialSourceAssertionPropertiesAction;
import com.l7tech.policy.assertion.credential.http.CookieCredentialSourceAssertion;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Policy tree node for cookie credential source.
 */
public class CookieCredentialSourceAssertionTreeNode extends LeafAssertionTreeNode {

    public CookieCredentialSourceAssertionTreeNode(CookieCredentialSourceAssertion assertion) {
        super(assertion);
    }

    public Action[] getActions() {
        List<Action> actions = new ArrayList<Action>();
        actions.add(new CookieCredentialSourceAssertionPropertiesAction(this));
        actions.addAll(Arrays.asList(super.getActions()));
        return actions.toArray(new Action[0]);
    }

    public Action getPreferredAction() {
        return new CookieCredentialSourceAssertionPropertiesAction(this);
    }

    public CookieCredentialSourceAssertion getAssertion() {
        return (CookieCredentialSourceAssertion)asAssertion();
    }

    /**
     * @return the node name that is displayed
     */
    public String getName() {
        return "Require HTTP Cookie (name=" + getAssertion().getCookieName() + ")";
    }

    /**
     * Test if the node can be deleted. Default is <code>true</code>
     *
     * @return true if the node can be deleted, false otherwise
     */
    public boolean canDelete() {
        return true;
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/authentication.gif";
    }
}
