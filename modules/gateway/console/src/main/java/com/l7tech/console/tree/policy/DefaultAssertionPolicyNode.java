/*
 * Copyright (C) 2007 Layer 7 Technologies Inc.
 */
package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditKeyAliasForAssertion;
import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.PrivateKeyable;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressable;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;

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
            name = addSuffixes(factory.toString());
        } else {
            Object obj = meta.get(AssertionMetadata.POLICY_NODE_NAME);
            if (obj != null)
                name = addSuffixes(obj.toString());
        }
        return name != null ? name : asAssertion().getClass().getName();
    }

    /**
     * Add any suffixes to a static name based on supported suffix features (like SecurityHeaderAddressable).
     * <p/>
     * This method is never invoked if the name is already a dynamic name, having come from a POLICY_NODE_NAME_FACTORY.
     *
     * @param name
     * @return the name, possibly with one or more suffixes added.
     */
    protected String addSuffixes(String name) {
        AT ass = asAssertion();
        if (ass instanceof SecurityHeaderAddressable)
            return name + SecurityHeaderAddressableSupport.getActorSuffix(ass);
        return name;
    }

    protected String iconResource(boolean open) {
        final String s = (String)asAssertion().meta().get(AssertionMetadata.POLICY_NODE_ICON);
        return s != null ? s : "com/l7tech/console/resources/policy16.gif";
    }

    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        list.addAll(Arrays.asList(super.getActions()));
        if (asAssertion() instanceof SecurityHeaderAddressable)
            list.add(new EditXmlSecurityRecipientContextAction(this));
        if (asAssertion() instanceof PrivateKeyable)
            list.add(new EditKeyAliasForAssertion(this));
        return list.toArray(new Action[list.size()]);
    }

    public  Action getPreferredAction() {
        return propertiesAction;
    }
}
