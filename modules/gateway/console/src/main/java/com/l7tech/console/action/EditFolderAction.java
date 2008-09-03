package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PolicyFolderPropertiesDialog;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.EntityType;
import com.l7tech.gateway.common.security.rbac.AttemptedUpdate;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;


/**
 * Action to rename a folder.
 */
public class EditFolderAction extends SecureAction {
    static Logger log = Logger.getLogger(CreateFolderAction.class.getName());

    private Folder folder;
    private FolderAdmin folderAdmin;
    private AbstractTreeNode folderToRename;
    private FolderHeader folderHeader;

    public EditFolderAction(Folder folder, FolderHeader folderHeader, AbstractTreeNode folderToRename, FolderAdmin folderAdmin) {
        super(new AttemptedUpdate(EntityType.FOLDER, folder));
        this.folder = folder;
        this.folderAdmin = folderAdmin;
        this.folderToRename = folderToRename;
        this.folderHeader = folderHeader;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Rename Folder";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Rename Folder";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/Properties16.gif";
    }

    /**
     */
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        PolicyFolderPropertiesDialog dialog = new PolicyFolderPropertiesDialog(f, folder.getName());
        dialog.setModal(true);
        dialog.setVisible(true);

        if(dialog.isConfirmed()) {
            try {
                folder.setName(dialog.getName());
                folderAdmin.saveFolder(folder);
                folderHeader.setName(dialog.getName());
                JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.nodeChanged(folderToRename);
                }
            } catch(UpdateException e) {
                log.log(Level.WARNING, "Failed to update policy folder", e);
            } catch(SaveException e) {
                log.log(Level.WARNING, "Failed to save policy folder", e);
            }
        }
    }
}

