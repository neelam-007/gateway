package com.l7tech.console.action;

import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.SamlSecurityPropertiesPanel;
import com.l7tech.console.tree.policy.SamlTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.policy.assertion.xmlsec.SamlSecurity;
import com.l7tech.common.gui.util.Utilities;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Action to edit the properties of a saml security authentication assertion.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * <p/>
 * User: flascell<br/>
 * Date: Nov 26, 2004<br/>
 */
public class SamlSecurityPropertiesAction extends SecureAction {
    private SamlTreeNode node;
    public SamlSecurityPropertiesAction(SamlTreeNode node) {
        this.node = node;
    }

    public String getName() {
        return "SAML Security Properties";
    }

    public String getDescription() {
        return "View/Edit the properties of the SAML Security assertion";
    }

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getMainWindow();
        SamlSecurityPropertiesPanel dlg = new SamlSecurityPropertiesPanel(f, (SamlSecurity)node.asAssertion());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.show();
        // todo, update the tree
    }

    private final PolicyListener listener = new PolicyListenerAdapter() {
        public void assertionsChanged(PolicyEvent e) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged(node);
                log.finest("model invalidated");
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }
        }
    };
}
