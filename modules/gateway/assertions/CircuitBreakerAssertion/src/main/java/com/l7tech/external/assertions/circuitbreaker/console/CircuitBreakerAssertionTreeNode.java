package com.l7tech.external.assertions.circuitbreaker.console;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CompositeAssertionTreeNode;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.policy.assertion.AssertionMetadata;
import com.l7tech.util.Functions;

import javax.swing.*;
import java.util.Arrays;
import java.util.LinkedList;

/**
 *
 * Tree node for Circuit breaker.
 *
 */
@SuppressWarnings("unused")
public class CircuitBreakerAssertionTreeNode extends CompositeAssertionTreeNode<CircuitBreakerAssertion> {
    private final Action propertiesAction;

    public CircuitBreakerAssertionTreeNode(CircuitBreakerAssertion assertion) {
        super(assertion);

        Functions.Unary<Action, AssertionTreeNode<CircuitBreakerAssertion>> factory =
                assertion.meta().get(AssertionMetadata.PROPERTIES_ACTION_FACTORY);
        propertiesAction = factory == null ? null : factory.call(this);
    }

    @Override
    public Action[] getActions() {
        LinkedList<Action> actions = new LinkedList<>(Arrays.asList(super.getActions()));
        actions.addFirst(getPreferredAction());
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Action getPreferredAction() {
        return propertiesAction;
    }
}
