package com.l7tech.external.assertions.circuitbreaker.console;

import com.l7tech.console.action.NodeActionWithMetaSupport;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.external.assertions.circuitbreaker.CircuitBreakerAssertion;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * Action to open properties dialog for circuit breaker.
 *
 */
@SuppressWarnings("unused")
public class CircuitBreakerAssertionPropertiesAction extends NodeActionWithMetaSupport {
    private static final Logger log = Logger.getLogger(CircuitBreakerAssertionPropertiesAction.class.getName());

    public CircuitBreakerAssertionPropertiesAction(AssertionTreeNode node) {
        super(node, CircuitBreakerAssertion.class, node.asAssertion());
    }

    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    @Override
    protected void performAction() {
        final CircuitBreakerAssertion assertion = (CircuitBreakerAssertion) node.asAssertion();
        final CircuitBreakerAssertionPropertiesDialog dialog =
                new CircuitBreakerAssertionPropertiesDialog(TopComponents.getInstance().getTopParent(), assertion);
        dialog.pack();
        Utilities.centerOnParentWindow(dialog);
        Frame f = TopComponents.getInstance().getTopParent();
        DialogDisplayer.display(dialog, f, () -> {
            if (dialog.isConfirmed()) {
                CircuitBreakerAssertion updatedAssertion = dialog.getData(assertion);
                node.setUserObject(updatedAssertion);
                PolicyTree policyTree = (PolicyTree) TopComponents.getInstance().getPolicyTree();
                if (policyTree != null) {
                    PolicyTreeModel model = (PolicyTreeModel) policyTree.getModel();
                    model.assertionTreeNodeChanged((AssertionTreeNode) node);

                    if (updatedAssertion != assertion) {
                        throw new IllegalStateException("Should not be a new assertion!");
                    }
                } else {
                    log.log(Level.WARNING, "Unable to reach the policy tree.");
                }
            }
        });
    }
}