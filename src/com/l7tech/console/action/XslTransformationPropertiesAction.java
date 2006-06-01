package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.XslTransformationPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.XslTransformationTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a Xsl Transformation Assertion node.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 */
public class XslTransformationPropertiesAction extends SecureAction {

    public XslTransformationPropertiesAction(XslTransformationTreeNode node) {
        this.node = node;
    }

    public String getName() {
        return "XSL Transformation Properties";
    }

    public String getDescription() {
        return "View/Edit properties of the xsl transformation assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getMainWindow();
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(f, false, node.getAssertion());
        dlg.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dlg.setSize(600, 580);
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        Assertion ass = dlg.getAssertion();
        if (ass == null) return;

        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged(node);
            log.finest("model invalidated");
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

    private final Logger log = Logger.getLogger(getClass().getName());
    private XslTransformationTreeNode node;
}
