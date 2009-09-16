package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.ExactlyOneAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>AddAllAssertionAction</code> action adds the
 * the composite <code>AllAssertion</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddExactlyOneAssertionAction extends PolicyUpdatingAssertionAction {
    private static final Logger log = Logger.getLogger(AddExactlyOneAssertionAction.class.getName());

    /**
     * @param n the assertion tree node must be composite
     */
    public AddExactlyOneAssertionAction(AssertionTreeNode n) {
        super(n, ExactlyOneAssertion.class, new ExactlyOneAssertion());
        if (!(n.getUserObject() instanceof CompositeAssertion)) {
            throw new IllegalArgumentException();
        }
    }

     /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    @Override
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                JTree tree =
                  (JTree)TopComponents.
                  getInstance().getComponent(PolicyTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    ExactlyOneAssertion aas = new ExactlyOneAssertion();
                    model.insertNodeInto(AssertionTreeNodeFactory.asTreeNode(aas),
                                         assertionTreeNode, assertionTreeNode.getChildCount());
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }
}
