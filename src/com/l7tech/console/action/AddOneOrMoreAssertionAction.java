package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.ComponentManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.composite.AllAssertion;
import com.l7tech.policy.assertion.composite.OneOrMoreAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;


/**
 * The <code>AddAllAssertionAction</code> action adds the
 * the composite <code>AllAssertion</code>.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddOneOrMoreAssertionAction extends BaseAction {
    private static final Logger log = Logger.getLogger(AddOneOrMoreAssertionAction.class.getName());
    AssertionTreeNode treeNode;

    /**
     * @param n the assertion tree node must be composite
     */
    public AddOneOrMoreAssertionAction(AssertionTreeNode n) {
        treeNode = n;
        if (!(treeNode.getUserObject() instanceof CompositeAssertion)) {
            throw new IllegalArgumentException();
        }
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Add 'One or more' assertion";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Add 'one or more' assertion folder to the policy assertion tree";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JTree tree =
                  (JTree)ComponentManager.
                  getInstance().getComponent(PolicyTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    CompositeAssertion ca = (CompositeAssertion)treeNode.asAssertion();
                    List kids = new ArrayList();
                    kids.addAll(ca.getChildren());
                    OneOrMoreAssertion oom = new OneOrMoreAssertion();
                    kids.add(oom);
                    ca.setChildren(kids);
                        model.
                          insertNodeInto(AssertionTreeNodeFactory.asTreeNode(oom),
                            treeNode, treeNode.getChildCount());
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }
}
