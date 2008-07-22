package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.HttpFormPostDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.HttpFormPostPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.HttpFormPost;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>HttpFormPostPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.HttpFormPost} assertion
 * properties.
 */
public class HttpFormPostPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(HttpFormPostPropertiesAction.class.getName());

    public HttpFormPostPropertiesAction(HttpFormPostPolicyNode node) {
        super(node, HttpFormPost.class);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "HTTP Form to MIME Translation Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View/Edit HTTP Form POST to MIME Translation Properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/network.gif";
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
        HttpFormPost hfp = (HttpFormPost) node.asAssertion();
        Frame f = TopComponents.getInstance().getTopParent();
        final HttpFormPostDialog hfpd = new HttpFormPostDialog(f, hfp, !node.canEdit());
        hfpd.setModal(true);
        Utilities.setEscKeyStrokeDisposes(hfpd);
        hfpd.pack();
        Utilities.centerOnScreen(hfpd);
        DialogDisplayer.display(hfpd, new Runnable() {
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
