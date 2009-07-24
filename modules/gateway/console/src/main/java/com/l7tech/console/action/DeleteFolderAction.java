package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * Action to delete a service/policy folder.
 */
public class DeleteFolderAction extends SecureAction {
    private long folderOid;
    private AbstractTreeNode folderToDelete;
    private FolderAdmin folderAdmin;
    private boolean confirmationEnabled; // Check if a deletion confirmation is needed or not.

    public DeleteFolderAction(long folderOid, AbstractTreeNode folderToDelete, FolderAdmin folderAdmin) {
        this(folderOid, folderToDelete, folderAdmin, true);
    }

    public DeleteFolderAction(long folderOid, AbstractTreeNode folderToDelete, FolderAdmin folderAdmin, boolean confirmationEnabled) {
        super(new AttemptedDeleteAll(EntityType.FOLDER));
        this.folderOid = folderOid;
        this.folderToDelete = folderToDelete;
        this.folderAdmin = folderAdmin;
        this.confirmationEnabled = confirmationEnabled;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return "Delete Folder";
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return "Delete Folder";
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     */
    @Override
    protected void performAction() {
        if(folderToDelete.getChildCount() > 0) {
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), "Cannot delete non-empty folders.", "Delete Error", JOptionPane.ERROR_MESSAGE);
        } else {
            if (! confirmationEnabled) {
                deleteFolderNode();
                return;
            }

            int result = JOptionPane.showConfirmDialog(TopComponents.getInstance().getTopParent(),
                                                       getUserConfirmationMessage(),
                                                       getUserConfirmationTitle(),
                                                       JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);
            if(result == JOptionPane.YES_OPTION) {
                deleteFolderNode();
            }
        }
    }

    private void deleteFolderNode() {
        try {
            folderAdmin.deleteFolder(folderOid);

            JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
            if (tree != null) {
                DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                model.removeNodeFromParent(folderToDelete);
            }
        } catch(ObjectModelException e) {
            JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), "Error deleting folder:\n" + ExceptionUtils.getMessage(e), "Delete Error", JOptionPane.ERROR_MESSAGE );
        }
    }

    public String getUserConfirmationMessage() {
        return "Are you sure you want to delete the " + folderToDelete.getName() + " folder?";
    }

    public String getUserConfirmationTitle() {
        return "Delete Folder";
    }
}
