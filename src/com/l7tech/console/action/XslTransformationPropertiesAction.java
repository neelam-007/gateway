package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.tree.policy.XslTransformationTreeNode;
import com.l7tech.console.util.ComponentRegistry;
import com.l7tech.console.util.Registry;
import com.l7tech.console.panels.XslTransformationPropertiesDialog;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Action for viewing or editing the properties of a Xsl Transformation Assertion node.
 *
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Feb 6, 2004<br/>
 * $Id$<br/>
 *
 */
public class XslTransformationPropertiesAction extends BaseAction {

    public XslTransformationPropertiesAction(XslTransformationTreeNode node) {
        this.node = node;
    }

    public String getName() {
        return "Xsl transformation properties";
    }

    public String getDescription() {
        return "View/Edit properties of the xsl transformation assertion.";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    public void performAction() {
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        XslTransformationPropertiesDialog dlg = new XslTransformationPropertiesDialog(f, false, node.getAssertion());
        dlg.addPolicyListener(listener);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        assertionChanged();
    }

    public void assertionChanged() {
        JTree tree = ComponentRegistry.getInstance().getPolicyTree();
        if (tree != null) {
            DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
            model.nodeChanged(node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = ComponentRegistry.getInstance().getPolicyTree();
            if (tree != null) {
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.nodeChanged(node);
                log.finest("model invalidated");
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    };

    private final Logger log = Logger.getLogger(getClass().getName());
    private XslTransformationTreeNode node;
}
