package com.l7tech.console.action;

import com.l7tech.common.gui.util.Utilities;
import com.l7tech.console.panels.EditorDialog;
import com.l7tech.console.panels.EntityEditorPanel;
import com.l7tech.console.panels.FindIdentitiesDialog;
import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.TreeNodeFactory;
import com.l7tech.console.util.Registry;
import com.l7tech.console.util.TopComponents;
import com.l7tech.identity.IdentityProviderConfig;
import com.l7tech.objectmodel.EntityHeader;

import javax.swing.*;
import java.awt.*;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * The <code>FindIdentityAction</code> action invokes the searche identity
 * dialog.
 * 
 * @author <a href="mailto:emarceta@layer7-tech.com">Emil Marceta</a>
 * @version 1.0
 */
public class FindIdentityAction extends SecureAction {
    static final Logger log = Logger.getLogger(FindIdentityAction.class.getName());
    FindIdentitiesDialog.Options options = new FindIdentitiesDialog.Options();
    
    private static
       ResourceBundle resapplication =
         java.util.ResourceBundle.getBundle("com.l7tech.console.resources.console");

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
        String name = resapplication.getString("Find_MenuItem_text_name");
        return name;
    }

    /**
     * @return the aciton description
     */
    public String getDescription() {
        return "Search for users and/or groups in this Identity Provider";
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
    protected void performAction() {
        Frame f = TopComponents.getInstance().getMainWindow();
        FindIdentitiesDialog fd = new FindIdentitiesDialog(f, true, options);
        fd.pack();
        fd.getSearchResultTable().getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        Utilities.centerOnScreen(fd);
        FindIdentitiesDialog.FindResult result = fd.showDialog();
        if (result.entityHeaders != null && result.entityHeaders.length > 0) {
            showEditDialog(result.providerConfigOid, result.entityHeaders[0]);
        }
    }

    /**
     * instantiate the dialog for given AbstractTreeNode
     * 
     * @param header the principal instance to edit
     */
    private void showEditDialog(long providerId, EntityHeader header) {
        EntityEditorPanel panel = null;

        AbstractTreeNode an = TreeNodeFactory.asTreeNode(header);
        final BaseAction a = (BaseAction)an.getPreferredAction();
        if (a == null) return;
        IdentityProviderConfig config = null;
        try {
            config = Registry.getDefault().getIdentityAdmin().findIdentityProviderConfigByPrimaryKey(providerId);
        } catch ( Exception e ) {
            log.log( Level.WARNING, "Couldn't find Identity Provider " + providerId, e );
            return;
        }
        if (config == null) return;

        if (a instanceof UserPropertiesAction) {
            ((UserPropertiesAction)a).setIdProviderConfig(config);
        } else if (a instanceof GroupPropertiesAction) {
            ((GroupPropertiesAction)a).setIdProviderConfig(config);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                a.performAction();
            }
        });

        if (panel == null) return;
        panel.edit(header);
        JFrame f = TopComponents.getInstance().getMainWindow();

        EditorDialog dialog = new EditorDialog(f, panel);
        dialog.pack();
        Utilities.centerOnScreen(dialog);
        dialog.show();
    }
}
