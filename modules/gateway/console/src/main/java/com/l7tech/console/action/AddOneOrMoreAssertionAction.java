package com.l7tech.console.action;

import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.FindException;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

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
public class AddOneOrMoreAssertionAction extends PolicyUpdatingAssertionAction {
    private static final Logger log = Logger.getLogger(AddOneOrMoreAssertionAction.class.getName());
    private int insertPosition = 0;

    /**
     * @param n the assertion tree node must be composite
     */
    public AddOneOrMoreAssertionAction(AssertionTreeNode n) throws FindException {
        this(n, 0);
    }

    public AddOneOrMoreAssertionAction(AssertionTreeNode treeNode, int insertPosition) {
        super(treeNode, OneOrMoreAssertion.class, new OneOrMoreAssertion());
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
                    OneOrMoreAssertion oom = new OneOrMoreAssertion();
                    model.insertNodeInto(AssertionTreeNodeFactory.asTreeNode(oom), assertionTreeNode, insertPosition);
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }
}
