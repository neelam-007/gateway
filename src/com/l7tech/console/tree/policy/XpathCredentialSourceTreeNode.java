/*
 * Copyright (C) 2003 Layer 7 Technologies Inc.
 *
 * $Id$
 */

package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditXpathCredentialSourceAction;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author alex
 * @version $Revision$
 */
public class XpathCredentialSourceTreeNode extends LeafAssertionTreeNode {
    private EditXpathCredentialSourceAction editAction = new EditXpathCredentialSourceAction(this);

    public XpathCredentialSourceTreeNode( XpathCredentialSource assertion ) {
        super( assertion );
        _assertion = assertion;
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }

    public Action getPreferredAction() {
        return editAction;
    }

    public Action[] getActions() {
        List actions = new ArrayList();
        actions.add(editAction);
        actions.addAll(Arrays.asList(super.getActions()));
        return (Action[])actions.toArray(new Action[0]);
    }

    public boolean canDelete() {
        return true;
    }

    private XpathCredentialSource _assertion;

    public String getName() {
        return "XPath credentials: login = '" + _assertion.getXpathExpression().getExpression() +
                       "', password = '" + _assertion.getPasswordExpression().getExpression();
    }
}
