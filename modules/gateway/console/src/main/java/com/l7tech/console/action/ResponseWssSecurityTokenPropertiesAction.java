package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.ResponseWssSecurityTokenDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.ResponseWssSecurityTokenPolicyNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Edits the {@link com.l7tech.policy.assertion.xmlsec.ResponseWssSecurityToken} properties.
 */
public class ResponseWssSecurityTokenPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(ResponseWssSecurityTokenPropertiesAction.class.getName());

    public ResponseWssSecurityTokenPropertiesAction(ResponseWssSecurityTokenPolicyNode node) {
        super(node, ResponseWssSecurityToken.class);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Response Security Token Assertion Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View/Edit Response Security Token Assertion Properties";
    }

    /**
     * specify the resource name for this action
     */
    @Override
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
    @Override
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        final ResponseWssSecurityTokenDialog dlg = new ResponseWssSecurityTokenDialog(f, true, (ResponseWssSecurityToken) node.asAssertion().clone(), !node.canEdit());
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.wasOKed()) {
                    ResponseWssSecurityToken newAss = dlg.getValue();
                    if (newAss == null) return;
                    ResponseWssSecurityToken oldAss = (ResponseWssSecurityToken)node.asAssertion();
                    oldAss.copyFrom(newAss);
                    JTree tree = TopComponents.getInstance().getPolicyTree();
                    if (tree != null) {
                        PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                        model.assertionTreeNodeChanged((AssertionTreeNode)node);
                    } else {
                        log.log(Level.WARNING, "Unable to reach the palette tree.");
                    }
                }
            }
        });
    }

}
