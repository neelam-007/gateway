package com.l7tech.console.action;

import com.l7tech.gui.util.Utilities;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.WsFederationPassiveTokenRequestPropertiesDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.policy.AssertionTreeNode;
import com.l7tech.console.tree.policy.PolicyTreeModel;
import com.l7tech.console.util.TopComponents;
import com.l7tech.policy.assertion.Assertion;
import com.l7tech.policy.assertion.composite.CompositeAssertion;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenRequest;
import com.l7tech.policy.assertion.credential.WsFederationPassiveTokenExchange;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Level;

/**
 * Edit action for WS-Federation PRP assertions.
 *
 * @author $Author$
 * @version $Revision$
 */
public class EditWsFederationPassiveTokenRequestAction extends NodeActionWithMetaSupport {

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
        super(node, WsFederationPassiveTokenRequest.class, node.asAssertion());
        Assertion assertion = node.asAssertion();
        if (assertion instanceof WsFederationPassiveTokenRequest) {
            isTokenRequest = true;
            wsFedAssertion = (WsFederationPassiveTokenRequest) assertion;
        }
        else if (assertion instanceof WsFederationPassiveTokenExchange) {
            isTokenRequest = false;
            wsFedAssertion = new WsFederationPassiveTokenRequest();
            wsFedAssertion.copyFrom((WsFederationPassiveTokenExchange)assertion);
        }
        else {
            throw new IllegalArgumentException();
        }
        parent = assertion.getParent();
    }

    //- PROTECTED


    @Override
    protected void performAction() {
        Frame frame = TopComponents.getInstance().getTopParent();

        final WsFederationPassiveTokenRequestPropertiesDialog dlg =
                new WsFederationPassiveTokenRequestPropertiesDialog(wsFedAssertion, isTokenRequest, frame, true, !node.canEdit());
        dlg.pack();
        Utilities.centerOnScreen(dlg);
        DialogDisplayer.display(dlg, new Runnable() {
            public void run() {
                if (dlg.isAssertionChanged()) {

                    // handle switching of assertion type
                    if (!isTokenRequest || !dlg.isTokenRequest()) {
                        final Assertion updatedAssertion;

                        if (!dlg.isTokenRequest()) {
                            WsFederationPassiveTokenExchange wsFederationPassiveTokenExchange = new WsFederationPassiveTokenExchange();
                            wsFederationPassiveTokenExchange.copyFrom(wsFedAssertion);
                            updatedAssertion = wsFederationPassiveTokenExchange;
                        }
                        else {
                            updatedAssertion = wsFedAssertion;
                        }

                        if (updatedAssertion != null) {
                            Assertion oldAssertion = node.asAssertion();
                            node.setUserObject(updatedAssertion);
                            if (parent != null) {
                                parent.replaceChild(oldAssertion, updatedAssertion);
                            }
                        }
                    }

                    isTokenRequest = dlg.isTokenRequest();
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

    //- PRIVATE

    private final CompositeAssertion parent;
    private boolean isTokenRequest;
    private final WsFederationPassiveTokenRequest wsFedAssertion;

}
