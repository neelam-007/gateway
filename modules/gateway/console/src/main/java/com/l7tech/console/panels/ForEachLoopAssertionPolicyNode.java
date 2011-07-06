package com.l7tech.console.panels;

import com.l7tech.console.action.AddIdentityAssertionAction;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.policy.assertion.composite.ForEachLoopAssertion;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;

public class ForEachLoopAssertionPolicyNode extends CompositeAssertionTreeNode<ForEachLoopAssertion> {
    private final Action propertiesAction;

    public ForEachLoopAssertionPolicyNode(ForEachLoopAssertion assertion) {
        super(assertion);

        //noinspection unchecked
        Functions.Unary<Action, AssertionTreeNode<ForEachLoopAssertion>> factory =
                (Functions.Unary<Action, AssertionTreeNode<ForEachLoopAssertion>>)
                        assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        propertiesAction = factory == null ? null : factory.call(this);
    }

    @Override
    public Action[] getActions() {
        LinkedList<Action> actions = new LinkedList<Action>(Arrays.asList(super.getActions()));
        actions.addFirst(getPreferredAction());

        Iterator<Action> iter = actions.iterator();
        while (iter.hasNext()) {
            Action action = iter.next();
            if (action instanceof AddIdentityAssertionAction)
                iter.remove();
        }

        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Action getPreferredAction() {
        return propertiesAction;
    }
}
