package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.SamlPropertiesDialog;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.SamlTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.util.Registry;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The <code>SamlPropertiesAction</code> edits the SAML assertion
 * properties.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class SamlPropertiesAction extends BaseAction {
    static final Logger log = Logger.getLogger(SamlPropertiesAction.class.getName());
    SamlTreeNode assertion;

    public SamlPropertiesAction(SamlTreeNode node) {
        assertion = node;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "SAML properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/edit SAML properties";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        SamlPropertiesDialog pw = new SamlPropertiesDialog(f, assertion);
        pw.pack();
        Utilities.centerOnScreen(pw);
        pw.show();
        assertionChanged();
    }

    public void assertionChanged() {
        JTree tree =
          (JTree)TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.nodeChanged(assertion);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }
}
