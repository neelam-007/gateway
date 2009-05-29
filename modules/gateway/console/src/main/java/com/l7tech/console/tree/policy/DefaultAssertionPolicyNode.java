/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.console.action.SelectIdentityTargetAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.IdentityTargetable;
import com.l7tech.policy.assertion.MessageTargetable;
import com.l7tech.policy.assertion.AssertionUtils;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedList;

/**
 * Default PolicyNode for assertions that don't provide a custom one of their own.
 */
public class DefaultAssertionPolicyNode<AT extends Assertion> extends LeafAssertionTreeNode<AT> {
    private final Action propertiesAction;

    public DefaultAssertionPolicyNode(AT assertion) {
        super(assertion);
        //noinspection unchecked
        Functions.Unary< Action, AssertionTreeNode<AT> > factory =
                (Functions.Unary<Action, AssertionTreeNode<AT>>)
                        assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        propertiesAction = factory == null ? null : factory.call(this);
    }

    @Override
    public String getName() {
        //noinspection unchecked
        AssertionMetadata meta = asAssertion().meta();
        Object factory = meta.get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        String name = null;
        if (factory instanceof Functions.Unary) {
            //noinspection unchecked
            Functions.Unary<String, Assertion> unary = (Functions.Unary<String, Assertion>)factory;
            name = unary.call(asAssertion());
        } else if (factory != null) {
            // Very common error to set this to a string instead of a factory, so we'll support it here
            name = addFeatureNames(factory.toString());
        } else {
            Object obj = meta.get(AssertionMetadata.POLICY_NODE_NAME);
            if (obj != null)
                name = addFeatureNames(obj.toString());
        }
        return name != null ? name : asAssertion().getClass().getName();
    }

    /**
     * Add any prefixes and suffixes to a static name based on supported features (like SecurityHeaderAddressable).
     * <p/>
     * This method is never invoked if the name is already a dynamic name, having come from a POLICY_NODE_NAME_FACTORY.
     *
     * @param name
     * @return the name, possibly with one or more prefixes or suffixes added.
     */
    protected String addFeatureNames(String name) {
        AT ass = asAssertion();
        return AssertionUtils.decorateName(ass, name);
    }

    @Override
    protected String iconResource(boolean open) {
        final String s = (String)asAssertion().meta().get(AssertionMetadata.POLICY_NODE_ICON);
        return s != null ? s : "com/l7tech/console/resources/policy16.gif";
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public Action[] getActions() {
        LinkedList<Action> list = new LinkedList<Action>(Arrays.asList(super.getActions()));
        int addIndex = getPreferredAction() == null ? 0 : 1;
        if (asAssertion() instanceof SecurityHeaderAddressable)
            list.add(addIndex, new EditXmlSecurityRecipientContextAction(this));
        if (asAssertion() instanceof PrivateKeyable)
            list.add(addIndex, new EditKeyAliasForAssertion(this));
        if (asAssertion() instanceof MessageTargetable)
            list.add(addIndex, new SelectMessageTargetAction(this));
        if (asAssertion() instanceof IdentityTargetable)
            list.add(addIndex, new SelectIdentityTargetAction((AssertionTreeNode<? extends IdentityTargetable>)this));
        
        return list.toArray(new Action[list.size()]);
    }

    @Override
    public  Action getPreferredAction() {
        return propertiesAction;
    }
}
