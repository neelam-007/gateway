package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNodeFactory;
import com.l7tech.console.tree.policy.PolicyTree;
import com.l7tech.console.util.WindowManager;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;
import java.util.ArrayList;


/**
 * The <code>AddAssertionAction</code> action assigns
 * the current assertion  to the target policy.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class AddAssertionAction extends BaseAction {
    protected AbstractTreeNode paletteNode;
    protected AssertionTreeNode assertionNode;
    private static final Logger log = Logger.getLogger(AddAssertionAction.class.getName());

    /**
     * @return the action name
     */
    public String getName() {
        return "Add assertion";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Add assertion to the policy assertion tree";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/assign.gif";
    }

    /** Actually perform the action.
     * This is the method which should be called programmatically.

     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (paletteNode == null || assertionNode == null) {
                    throw new IllegalStateException();
                }
                JTree tree =
                  (JTree)WindowManager.
                  getInstance().getComponent(PolicyTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    Assertion nass = paletteNode.asAssertion();

                    if (nass != null) {
                        Assertion receivingAssertion = assertionNode.asAssertion();
                        if (!(receivingAssertion instanceof CompositeAssertion)) {
                            log.log(Level.WARNING, "The receiving assertion is not composite " + assertionNode);
                            return;
                        }
                        CompositeAssertion ca =
                          (CompositeAssertion)receivingAssertion;
                        List kids = new ArrayList();
                        kids.addAll(ca.getChildren());
                        kids.add(nass);
                        ca.setChildren(kids);
                        model.
                          insertNodeInto(AssertionTreeNodeFactory.asTreeNode(nass),
                            assertionNode, assertionNode.getChildCount());
                    } else {
                        log.log(Level.WARNING, "The node has no associated assertion " + paletteNode);
                    }
                } else {
                    log.log(Level.WARNING, "Unable to reach the palette tree.");
                }
            }
        });
    }
}
