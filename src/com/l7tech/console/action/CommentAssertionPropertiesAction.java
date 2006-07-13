package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.CommentAssertionPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.CommentAssertionDialog;
import com.l7tech.policy.assertion.CommentAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>CommentAssertionPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.CommentAssertion} properties.
 */
public class CommentAssertionPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(CommentAssertionPropertiesAction.class.getName());

    public CommentAssertionPropertiesAction(CommentAssertionPolicyNode node) {
        super(node, CommentAssertion.class);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Comment Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Comment Assertion Properties";
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
        CommentAssertion ca = (CommentAssertion)node.asAssertion();
        JFrame f = TopComponents.getInstance().getMainWindow();
        CommentAssertionDialog cad = new CommentAssertionDialog(f, ca);
        Utilities.setEscKeyStrokeDisposes(cad);
        cad.pack();
        Utilities.centerOnScreen(cad);
        cad.setVisible(true);
        if (cad.isAssertionModified()) assertionChanged();
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
