package com.l7tech.console.tree.policy;

import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.composite.HandleErrorsAssertion;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedList;


public class HandleErrorsAssertionTreeNode extends CompositeAssertionTreeNode<HandleErrorsAssertion> {
    private final Action propertiesAction;

    public HandleErrorsAssertionTreeNode(HandleErrorsAssertion assertion) {
        super(assertion);

        //noinspection unchecked
        Functions.Unary<Action, AssertionTreeNode<HandleErrorsAssertion>> factory = assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        propertiesAction = factory == null ? null : factory.call(this);

    }

    @Override
    public Action[] getActions() {
        LinkedList<Action> actions = new LinkedList<Action>(Arrays.asList(super.getActions()));
        actions.addFirst(getPreferredAction());
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Action getPreferredAction() {
        return propertiesAction;
    }
}
