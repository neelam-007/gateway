package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.console.policy.ConsoleAssertionRegistry;
import com.l7tech.common.util.Functions;

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
        final Functions.Unary< String, Assertion > s =
                (Functions.Unary<String, Assertion>)
                        asAssertion().meta().get(AssertionMetadata.POLICY_NODE_NAME);
        return s != null ? s.call(asAssertion()) : asAssertion().getClass().getName();
    }

    protected String iconResource(boolean open) {
        final String s = (String)asAssertion().meta().get(AssertionMetadata.POLICY_NODE_ICON);
        return s != null ? s : "com/l7tech/console/resources/policy16.gif";
    }

    public boolean canDelete() {
        return true;
    }

    public Action[] getActions() {
        java.util.List<Action> list = new ArrayList<Action>();
        final Action pref = getPreferredAction();
        if (pref != null) list.add(pref);
        list.addAll(Arrays.asList(super.getActions()));
        return list.toArray(new Action[0]);
    }

    public  Action getPreferredAction() {
        return propertiesAction;
    }
}
