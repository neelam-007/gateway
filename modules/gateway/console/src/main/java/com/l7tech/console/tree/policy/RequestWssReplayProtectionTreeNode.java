package com.l7tech.console.tree.policy;

import com.l7tech.console.action.EditXmlSecurityRecipientContextAction;
import com.l7tech.console.action.NodeAction;
import com.l7tech.console.panels.ReplayProtectionPropertiesDialog;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gui.util.Utilities;
import com.l7tech.policy.assertion.xmlsec.RequestWssReplayProtection;
import com.l7tech.policy.assertion.xmlsec.SecurityHeaderAddressableSupport;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is the tree node corresponding to the RequestWssX509Cert assertion type.
 * <p/>
 * <br/><br/>
 * LAYER 7 TECHNOLOGIES, INC<br/>
 * User: flascell<br/>
 * Date: Jul 14, 2004<br/>
 * $Id$<br/>
 */
public class RequestWssReplayProtectionTreeNode extends LeafAssertionTreeNode<RequestWssReplayProtection> {
    private static final Logger logger = Logger.getLogger(RequestWssReplayProtectionTreeNode.class.getName());

    private final NodeAction propertiesAction = new NodeAction(this) {
        @Override
        public String getName() {
            return "WSS Replay Protection Properties";
        }

        @Override
        protected String iconResource() {
            return "com/l7tech/console/resources/Properties16.gif";
        }

        protected void performAction() {
            final ReplayProtectionPropertiesDialog dlg = new ReplayProtectionPropertiesDialog(TopComponents.getInstance().getTopParent(), true, assertion, !canEdit());
            dlg.pack();
            Utilities.centerOnScreen(dlg);
            DialogDisplayer.display(dlg, new Runnable() {
                public void run() {
                    if (dlg.wasOKed()) {
                        JTree tree = TopComponents.getInstance().getPolicyTree();
                        if (tree != null) {
                            PolicyTreeModel model = (PolicyTreeModel)tree.getModel();
                            model.assertionTreeNodeChanged((AssertionTreeNode)node);
                        } else {
                            logger.log(Level.WARNING, "Unable to reach the palette tree.");
                        }
                    }
                }
            });
        }
    };

    public RequestWssReplayProtectionTreeNode(RequestWssReplayProtection assertion) {
        super(assertion);
    }

    public String getName() {
        return "WSS Replay Protection in " + assertion.getTargetName() + SecurityHeaderAddressableSupport.getActorSuffix(assertion);
    }

    protected String iconResource(boolean open) {
        return "com/l7tech/console/resources/xmlencryption.gif";
    }

    public Action[] getActions() {
        List<Action> actions = new ArrayList<Action>(Arrays.asList(super.getActions()));
        actions.add(new EditXmlSecurityRecipientContextAction(this));
        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Action getPreferredAction() {
        return propertiesAction;
    }
}
