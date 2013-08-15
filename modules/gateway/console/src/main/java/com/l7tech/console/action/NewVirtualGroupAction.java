package com.l7tech.console.action;

import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.gateway.common.security.rbac.AttemptedCreateSpecific;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.console.panels.NewVirtualGroupDialog;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.tree.IdentityProviderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.fed.VirtualGroup;
import com.l7tech.policy.assertion.identity.MemberOfGroup;

import javax.swing.*;
import java.awt.*;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class NewVirtualGroupAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewVirtualGroupAction.class.getName());

    public NewVirtualGroupAction(IdentityProviderNode node) {
        super(node,
            MemberOfGroup.class,
            new AttemptedCreateSpecific(
                    EntityType.GROUP,
                    new VirtualGroup(node.getEntityHeader().getGoid(), "<new virtual group>")));
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create Virtual Group";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Create a new Identity Provider virtual group";
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
                NewVirtualGroupDialog dialog = new NewVirtualGroupDialog(f, getIdentityProviderConfig((EntityHeaderNode) node));
                dialog.setResizable(false);
                DialogDisplayer.display(dialog);
            }
        });
    }
}