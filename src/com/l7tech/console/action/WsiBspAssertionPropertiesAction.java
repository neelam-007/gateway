package com.l7tech.console.action;

import java.awt.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.*;

import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.tree.policy.WsiBspAssertionPolicyNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.event.PolicyListener;
import com.l7tech.console.event.PolicyListenerAdapter;
import com.l7tech.console.event.PolicyEvent;
import com.l7tech.console.panels.WsiBspPropertiesDialog;
import com.l7tech.common.gui.util.Utilities;

/**
 * Edit properties action for WSI-BSP assertion.
 *
 * @author $Author$
 * @version $Revision$
 */
public class WsiBspAssertionPropertiesAction extends SecureAction {

    //- PUBLIC

    public WsiBspAssertionPropertiesAction(WsiBspAssertionPolicyNode node) {
        this.node = node;
    }

    public String getName() {
        return "WSI-BSP Properties";
    }

    public String getDescription() {
        return "View/Edit properties of the WS-I BSP assertion.";
    }

    //- PROTECTED

    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    protected void performAction() {
        Frame f = TopComponents.getInstance().getMainWindow();
        WsiBspPropertiesDialog dlg = new WsiBspPropertiesDialog(node.getAssertion(), f, true);
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        dlg.setVisible(true);
        if(dlg.isAssertionChanged()) {
            JTree tree = TopComponents.getInstance().getPolicyTree();
            if (tree != null) {
                PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                model.assertionTreeNodeChanged((AssertionTreeNode)node);
            } else {
                log.log(Level.WARNING, "Unable to reach the palette tree.");
            }            
        }
    }

    //- PRIVATE

    private final Logger log = Logger.getLogger(getClass().getName());
    
    private WsiBspAssertionPolicyNode node;
}
