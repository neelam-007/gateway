/*
 * Copyright (C) 2004-2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.XpathBasedAssertionPropertiesAction;
import com.l7tech.console.action.SelectIdentityTargetAction;
import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.console.action.SelectIdentityTargetAction;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.XpathBasedAssertion;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;

import javax.swing.*;
import java.util.*;

/**
 * Abstract superclass for XpathBasedAssertion policy nodes
 */
public abstract class XpathBasedAssertionTreeNode<AT extends XpathBasedAssertion> extends LeafAssertionTreeNode<AT> {

    public XpathBasedAssertionTreeNode(AT assertion) {
        super(assertion);
    }

    /** Get the basic name of this node, ie "XML Request Security". */
    public abstract String getBaseName(final boolean decorate);

    @Override
    public String getName(final boolean decorate) {
        return getBaseName(decorate);
    }

    /**
     * Get the set of actions associated with this node.
     * This may be used e.g. in constructing a context menu.
     *
     * @return actions appropriate to the node
     */
    @SuppressWarnings({"unchecked"})
    @Override
    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        List<Action> superList = Arrays.asList(super.getActions());
        if ( !superList.isEmpty() ) {
            list.add(superList.get(0));
        }
        if (assertion instanceof PrivateKeyable) {
            list.add(new EditKeyAliasForAssertion(this));
        }
        if (assertion instanceof MessageTargetable) {
            list.add(new SelectMessageTargetAction(this));
        }
        if (assertion instanceof IdentityTargetable) {
            list.add(new SelectIdentityTargetAction((AssertionTreeNode<? extends IdentityTargetable>)this));
        }
        if (assertion instanceof SecurityHeaderAddressable) {
            list.add(new EditXmlSecurityRecipientContextAction(this));
        }
        if ( superList.size() > 1 ) {
            list.addAll( superList.subList(1, superList.size()) );
        }
        return list.toArray(new Action[list.size()]);
    }

    /**
     * Gets the default action for this node.
     *
     * @return <code>null</code> indicating there should be none default action
     */
    @Override
    public Action getPreferredAction() {
        return XpathBasedAssertionPropertiesAction.actionForNode(this);
    }

    /**
     * subclasses override this method specifying the resource name
     *
     * @param open for nodes that can be opened, can have children
     */
    @Override
    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlsignature.gif";
    }
}
