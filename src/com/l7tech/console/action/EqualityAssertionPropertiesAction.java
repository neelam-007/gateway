package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.EqualityAssertionPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.EqualityAssertionDialog;
import com.l7tech.policy.assertion.EqualityAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>EqualityAssertionPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.EqualityAssertion} properties.
 */
public class EqualityAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(EqualityAssertionPropertiesAction.class.getName());

    public EqualityAssertionPropertiesAction(EqualityAssertionPolicyNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Equality Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Equality Assertion Properties";
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
        EqualityAssertion eq = (EqualityAssertion)node.asAssertion();
        JFrame f = TopComponents.getInstance().getMainWindow();
        EqualityAssertionDialog eqd = new EqualityAssertionDialog(f, eq);
        Actions.setEscKeyStrokeDisposes(eqd);
        eqd.pack();
        Utilities.centerOnScreen(eqd);
        eqd.setVisible(true);
        if (eqd.isAssertionModified()) assertionChanged();
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
