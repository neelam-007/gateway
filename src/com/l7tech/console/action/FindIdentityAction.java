package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.EntityEditorPanel;
import com.l7tech.console.panels.FindIdentitiesDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.Registry;
import com.l7tech.identity.Group;
import com.l7tech.identity.IdentityProvider;
import com.l7tech.identity.User;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.awt.*;
import java.security.Principal;
import java.util.logging.Logger;


/**
 * The <code>FindIdentityAction</code> action invokes the searche identity
 * dialog.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class FindIdentityAction extends BaseAction {
    static final Logger log = Logger.getLogger(FindIdentityAction.class.getName());
    FindIdentitiesDialog.Options options = new FindIdentitiesDialog.Options();

    /**
     * create the action with the default find dialog options
     */
    public FindIdentityAction() {
        this(new FindIdentitiesDialog.Options());
    }

    /**
     * create the find idnetity action action with the dialog options
     * specified
     */
    public FindIdentityAction(FindIdentitiesDialog.Options opts) {
        if (opts == null) {
            throw new IllegalArgumentException();
        }
        options = opts;
    }


    /**
     * @return the action name
     */
    public String getName() {
        return "Find";
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Find Identities";
    }

    /**
     * subclasses override this method specifying the resource name
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Find16.gif";
    }

    /**
     * Actually perform the action.
     * This is the method which should be called programmatically.
     * note on threading usage: do not access GUI components
     * without explicitly asking for the AWT event thread!
     */
    public void performAction() {
        Frame f = Registry.getDefault().getComponentRegistry().getMainWindow();
        FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true, options);
        fd.pack();
        fd.getSearchResultTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.centerOnScreen(fd);
        Principal[] principals = fd.showDialog();
        if (principals != null && principals.length > 0) {
            showEditDialog(principals[0]);
        }
    }

    /**
     * instantiate the dialog for given AbstractTreeNode
     * 
     * @param principal the principal instance to edit
     */
    private void showEditDialog(Principal principal) {
        EntityEditorPanel panel = null;
        EntityHeader eh = null;
        long providerId = 0;
        if (principal instanceof User) {
            User u = (User)principal;
            eh = EntityHeader.fromUser(u);
            providerId = u.getProviderId();
        } else if (principal instanceof Group) {
            Group g = (Group)principal;
            eh = EntityHeader.fromGroup(g);
            providerId = g.getProviderId();
        }
        if (eh == null) return;

        AbstractTreeNode an = TreeNodeFactory.asTreeNode(eh);
        final BaseAction a = (BaseAction)an.getPreferredAction();
        if (a == null) return;
        IdentityProvider ip = Registry.getDefault().getIdentityProvider(providerId);
        if (ip == null) return;

        if (a instanceof UserPropertiesAction) {
            ((UserPropertiesAction)a).setIdProvider(ip);
        } else if (a instanceof GroupPropertiesAction) {
            ((GroupPropertiesAction)a).setIdProvider(ip);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                a.performAction();
            }
        });

        if (panel == null) return;
        panel.edit(principal);
        JFrame f = Registry.getDefault().getComponentRegistry().getMainWindow();

        EditorDialog dialog = new EditorDialog(f, panel);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.show();
    }
}
