package com.l7tech.console.action;

import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.AttemptedOperation;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.console.panels.NewGroupDialog;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.GroupBean;
import com.l7tech.identity.IdentityProviderConfigManager;
import com.l7tech.objectmodel.Goid;
import com.l7tech.policy.assertion.identity.MemberOfGroup;

import javax.swing.*;
import java.util.logging.Logger;
import java.awt.*;

/**
 * The <code>NewGroupAction</code> action adds the new group.
 *
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class NewGroupAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewGroupAction.class.getName());
    private AttemptedCreateSpecific attemptedCreateInternal;

    public NewGroupAction(IdentityProviderNode node) {
        super(node, MemberOfGroup.class, null);
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Group";
    }

    @Override
    public boolean isAuthorized() {
        AttemptedOperation ao;
        if (node == null) {
            // This is the Create Internal Group action in MainWindow
            ao = getAttemptedCreateInternal();
        } else {
            // This probably belongs to a node in the provider tree
            Goid providerId = ((IdentityProviderNode)node).getEntityHeader().getGoid();
            if (providerId.equals(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID)) {
                ao = getAttemptedCreateInternal();
            } else {
                ao = new AttemptedCreateSpecific(EntityType.GROUP, new GroupBean(providerId, "<new group>"));
            }
        }
        return canAttemptOperation(ao);
    }

    private AttemptedOperation getAttemptedCreateInternal() {
        if (attemptedCreateInternal == null) {
            attemptedCreateInternal = new AttemptedCreateSpecific(EntityType.GROUP, new GroupBean(IdentityProviderConfigManager.INTERNALPROVIDER_SPECIAL_GOID, "<new group>"));
        }
        return attemptedCreateInternal;
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new Internal Identity Provider group";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/group16.png";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * <p/>
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    protected void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Frame f = TopComponents.getInstance().getTopParent();
                NewGroupDialog dialog = new NewGroupDialog(f, getIdentityProviderConfig((EntityHeaderNode) node));
                dialog.setResizable(false);
                DialogDisplayer.display(dialog);                
            }
        });
    }
}
