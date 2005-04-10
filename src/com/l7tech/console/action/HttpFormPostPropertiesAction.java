package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.HttpFormPostDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.HttpFormPostPolicyNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.HttpFormPost;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>HttpFormPostPropertiesAction</code> edits the
 * {@link com.l7tech.policy.assertion.HttpFormPost} assertion
 * properties.
 */
public class HttpFormPostPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(HttpFormPostPropertiesAction.class.getName());

    public HttpFormPostPropertiesAction(HttpFormPostPolicyNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "HTTP Form POST Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit HTTP Form POST";
    }

    /**
     * specify the resource name for this action
     */
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
    protected void performAction() {
        HttpFormPost hfp = (HttpFormPost) node.asAssertion();
        JFrame f = TopComponents.getInstance().getMainWindow();
        HttpFormPostDialog hfpd = new HttpFormPostDialog(f, hfp);
        hfpd.setModal(true);
        Actions.setEscKeyStrokeDisposes(hfpd);
        hfpd.pack();
        hfpd.setSize(800, 600);
        Utilities.centerOnScreen(hfpd);
        hfpd.setVisible(true);
        if (hfpd.isAssertionModified()) assertionChanged();
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
