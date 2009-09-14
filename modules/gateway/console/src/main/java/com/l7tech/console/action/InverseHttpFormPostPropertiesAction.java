package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.InverseHttpFormPostDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.InverseHttpFormPost;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>HttpFormPostPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.HttpFormPost} assertion
 * properties.
 */
public class InverseHttpFormPostPropertiesAction extends NodeActionWithMetaSupport {
    static final Logger log = Logger.getLogger(InverseHttpFormPostPropertiesAction.class.getName());

    public InverseHttpFormPostPropertiesAction(AssertionTreeNode node) {
        super(node, InverseHttpFormPost.class, node.asAssertion());
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
        InverseHttpFormPost hfp = (InverseHttpFormPost) node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final InverseHttpFormPostDialog hfpd = new InverseHttpFormPostDialog(f, hfp, !node.canEdit());
        hfpd.setModal(true);
        Utilities.setEscKeyStrokeDisposes(hfpd);
        hfpd.pack();
        Utilities.centerOnScreen(hfpd);
        DialogDisplayer.display(hfpd, new Runnable() {
            @Override
            public void run() {
                if (hfpd.isAssertionModified()) assertionChanged();
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
