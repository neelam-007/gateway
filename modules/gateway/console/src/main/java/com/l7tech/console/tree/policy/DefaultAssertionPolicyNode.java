/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.SelectMessageTargetAction;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.console.action.SelectIdentityTargetAction;
import com.l7tech.policy.assertion.*;
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
    public String getName(final boolean decorate) {
        return getNameFromMeta(asAssertion(), decorate);
    }

    public static <AT extends Assertion> String getNameFromMeta(final AT assertion, final boolean decorate){
        //noinspection unchecked
        AssertionMetadata meta = assertion.meta();
        Object factory = meta.get(AssertionMetadata.POLICY_NODE_NAME_FACTORY);
        String name = null;
        if (factory instanceof Functions.Unary) {
            //noinspection unchecked
            Functions.Unary<String, Assertion> unary = (Functions.Unary<String, Assertion>)factory;
            name = unary.call(assertion);
        } else if(factory instanceof AssertionNodeNameFactory){
            AssertionNodeNameFactory nameFactory = (AssertionNodeNameFactory) factory;
            name = nameFactory.getAssertionName(assertion, decorate);
        } else if (factory != null && factory instanceof String) {
            name = addFeatureNames(assertion, factory.toString());
        } else {
            Object obj = meta.get(AssertionMetadata.POLICY_NODE_NAME);
            if (obj != null)
                name = addFeatureNames(assertion, obj.toString());
        }
        return name != null ? name : assertion.getClass().getName();
    }
    /**
     * Add any prefixes and suffixes to a static name based on supported features (like SecurityHeaderAddressable).
     * <p/>
     * This method is never invoked if the name is already a dynamic name, having come from a POLICY_NODE_NAME_FACTORY.
     *
     * @param name
     * @return the name, possibly with one or more prefixes or suffixes added.
     */
    protected static <AT extends Assertion> String addFeatureNames(final AT assertion, final String name) {
        return AssertionUtils.decorateName(assertion, name);
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
