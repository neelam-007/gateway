package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.CommentAssertionDialog;
import com.l7tech.policy.assertion.CommentAssertion;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>CommentAssertionPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.CommentAssertion} properties.
 */
public class CommentAssertionPropertiesAction extends NodeActionWithMetaSupport {
    static final Logger log = Logger.getLogger(CommentAssertionPropertiesAction.class.getName());

    public CommentAssertionPropertiesAction(AssertionTreeNode node) {
        super(node, CommentAssertion.class, node.asAssertion());
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
        CommentAssertion ca = (CommentAssertion)node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final CommentAssertionDialog cad = new CommentAssertionDialog(f, ca, !node.canEdit());
        Utilities.setEscKeyStrokeDisposes(cad);
        cad.pack();
        Utilities.centerOnScreen(cad);
        DialogDisplayer.display(cad, new Runnable() {
            @Override
            public void run() {
                if (cad.isAssertionModified()) assertionChanged();
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
