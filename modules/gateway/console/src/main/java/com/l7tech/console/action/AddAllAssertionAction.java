package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

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
public class AddAllAssertionAction extends PolicyUpdatingAssertionAction {
    private static final Logger log = Logger.getLogger(AddAllAssertionAction.class.getName());
    private int insertPosition = 0;

    /**
     * @param n the assertion tree node must be composite
     */
    public AddAllAssertionAction(AssertionTreeNode n) {
        this(n, 0);
    }

    /**
     * Create the <code>AddAllAssertionAction</code> that adds
     *
     * @param treeNode
     * @param insertPosition the insert position
     */
    public AddAllAssertionAction(AssertionTreeNode treeNode, int insertPosition) {
        super(treeNode, AllAssertion.class, new AllAssertion());
        if (!(treeNode.getUserObject() instanceof CompositeAssertion)) {
            throw new IllegalArgumentException();
        }
        this.insertPosition = insertPosition;
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
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

                    AssertionTreeNode an =
                      AssertionTreeNodeFactory.asTreeNode(new AllAssertion());
                    model.insertNodeInto(an, assertionTreeNode, insertPosition);
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }
}
