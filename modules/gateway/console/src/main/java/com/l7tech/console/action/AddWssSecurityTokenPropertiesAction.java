package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.AddWssSecurityTokenDialog;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken;

import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.awt.*;

/**
 * Edits the {@link com.l7tech.policy.assertion.xmlsec.AddWssSecurityToken} properties.
 */
public class AddWssSecurityTokenPropertiesAction extends NodeAction {
    static final Logger log = Logger.getLogger(AddWssSecurityTokenPropertiesAction.class.getName());

    public AddWssSecurityTokenPropertiesAction(AbstractTreeNode node) {
        super(node, AddWssSecurityToken.class);
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Add Security Token Properties";
    }

    /**
     * @return the aciton description
     */
    @Override
    public String getDescription() {
        return "View/Edit Add Security Token Properties";
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
        final AddWssSecurityTokenDialog dlg = new AddWssSecurityTokenDialog(f, true, (AddWssSecurityToken) node.asAssertion().clone(), !node.canEdit());
        Utilities.setEscKeyStrokeDisposes(dlg);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            @Override
            public void run() {
                if (dlg.wasOKed()) {
                    AddWssSecurityToken newAss = dlg.getValue();
                    if (newAss == null) return;
                    AddWssSecurityToken oldAss = (AddWssSecurityToken)node.asAssertion();
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
