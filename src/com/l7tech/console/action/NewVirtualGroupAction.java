package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.EntityHeaderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.NewGroupDialog;
import com.l7tech.console.panels.NewVirtualGroupDialog;

import javax.swing.*;
import java.util.logging.Logger;

/**
 * <p> Copyright (C) 2004 Layer 7 Technologies Inc.</p>
 * <p> @author fpang </p>
 * $Id$
 */
public class NewVirtualGroupAction extends NodeAction {
    static final Logger log = Logger.getLogger(NewVirtualGroupAction.class.getName());

    public NewVirtualGroupAction(AbstractTreeNode node) {
        super(node);
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
    public void performAction() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame f = TopComponents.getInstance().getMainWindow();
                NewVirtualGroupDialog dialog = new NewVirtualGroupDialog(f, getIdentityProviderConfig((EntityHeaderNode) node));
                dialog.setResizable(false);
                dialog.show();
            }
        });
    }
}