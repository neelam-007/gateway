package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.WsFederationPassiveTokenRequestPropertiesDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Edit action for WS-Federation PRP assertions.
 *
 * @author $Author$
 * @version $Revision$
 */
public class EditWsFederationPassiveTokenRequestAction extends NodeAction {

    //- PUBLIC

    /**
     * Constructor accepting the node that this action will
     * act on.
     *
     * The tree will be set to <b>null<b/>
     *
     * @param node the node this action will acto on
     */
    public EditWsFederationPassiveTokenRequestAction(AbstractTreeNode node) {
        super(node, WsFederationPassiveTokenRequest.class);
        Assertion assertion = node.asAssertion();
        if (!(assertion instanceof WsFederationPassiveTokenRequest)) {
            throw new IllegalArgumentException();
        }
        wsFedAssertion = (WsFederationPassiveTokenRequest) assertion;
    }

    /**
     * Get the name of this action.
     *
     * @return the name.
     */
    public String getName() {
        return "View/Edit WS-Federation PRP Properties";
    }

    //- PROTECTED

    protected String iconResource() {
        return "com/l7tech/console/resources/Edit16.gif";
    }

    protected void performAction() {
        Frame parent = TopComponents.getInstance().getMainWindow();

        WsFederationPassiveTokenRequestPropertiesDialog dlg = new WsFederationPassiveTokenRequestPropertiesDialog(wsFedAssertion, parent, true);
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

    //- PRIVATE

    private final WsFederationPassiveTokenRequest wsFedAssertion;

}
