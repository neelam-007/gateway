package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.ResponseWssSecurityTokenDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.ResponseWssSecurityTokenPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Edits the {@link com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken} properties.
 */
public class ResponseWssSecurityTokenPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(ResponseWssSecurityTokenPropertiesAction.class.getName());

    public ResponseWssSecurityTokenPropertiesAction(ResponseWssSecurityTokenPolicyNode node) {
        super(node);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Response Security Token Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "View/Edit Response Security Token Assertion Properties";
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
        ResponseWssSecurityToken ass = (ResponseWssSecurityToken) node.asAssertion();
        JFrame f = TopComponents.getInstance().getMainWindow();
        ResponseWssSecurityTokenDialog dlg = new ResponseWssSecurityTokenDialog(f, true, ass);
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        ResponseWssSecurityToken newAss = (ResponseWssSecurityToken)dlg.getValue();
        if (newAss == null) return;

        JTree tree = TopComponents.getInstance().getPolicyTree();
        if (tree != null) {
            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
            model.assertionTreeNodeChanged((AssertionTreeNode)node);
        } else {
            log.log(Level.WARNING, "Unable to reach the palette tree.");
        }
    }

}
