package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.XpathCredentialSourcePropertiesDialog;
import com.l7tech.policy.assertion.credential.XpathCredentialSource;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * @author alex
 */
public class EditXpathCredentialSourceAction extends NodeAction {
    private final XpathCredentialSource xpathCredsAssertion;

    /**
     * constructor accepting the node that this action will
     * act on.
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     */
    public EditXpathCredentialSourceAction(AbstractTreeNode node) {
        super(node);
        if (!(node.asAssertion() instanceof XpathCredentialSource)) {
            throw new IllegalArgumentException();
        }
        xpathCredsAssertion = (XpathCredentialSource)node.asAssertion();
    }

    public String getName() {
        return "View/Edit XPath Credential Source Properties";
    }

    public String getDescription() {
        return getName();
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        Frame parent = TopComponents.getInstance().getMainWindow();
        XpathCredentialSourcePropertiesDialog dlg = new XpathCredentialSourcePropertiesDialog(xpathCredsAssertion, parent, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        if (dlg.isAssertionChanged()) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged((AssertionTreeNode)node);
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    }

}
