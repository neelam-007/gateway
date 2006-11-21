package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.common.gui.util.DialogDisplayer;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.ComparisonAssertionPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.ComparisonAssertionDialog;
import com.l7tech.policy.assertion.ComparisonAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>ComparisonAssertionPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.ComparisonAssertion} properties.
 */
public class ComparisonAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(ComparisonAssertionPropertiesAction.class.getName());

    public ComparisonAssertionPropertiesAction(ComparisonAssertionPolicyNode node) {
        super(node, ComparisonAssertion.class);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Comparison Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Comparison Assertion Properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/About16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        ComparisonAssertion eq = (ComparisonAssertion)node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final ComparisonAssertionDialog eqd = new ComparisonAssertionDialog(f, eq);
        Utilities.setEscKeyStrokeDisposes(eqd);
        eqd.pack();
        Utilities.centerOnScreen(eqd);
        DialogDisplayer.display(eqd, new Runnable() {
            public void run() {
                if (eqd.isAssertionModified()) assertionChanged();
            }
        });
    }

    public void assertionChanged() {
        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
