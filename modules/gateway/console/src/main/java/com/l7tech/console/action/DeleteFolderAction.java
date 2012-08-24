package com.l7tech.console.action;

import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ObjectModelException;
import com.l7tech.util.ExceptionUtils;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;

/**
 * Action to delete a service/policy folder.
 */
public class DeleteFolderAction extends SecureAction {
    private static final String DELETE_FOLDER = "Delete Folder";
    private FolderNode folderToDelete;
    private FolderAdmin folderAdmin;
    private boolean confirmationEnabled; // Check if a deletion confirmation is needed or not.

    public DeleteFolderAction(FolderNode folderToDelete, FolderAdmin folderAdmin) {
        this(folderToDelete, folderAdmin, true);
    }

    public DeleteFolderAction(FolderNode folderToDelete, FolderAdmin folderAdmin, boolean confirmationEnabled) {
        super(new AttemptedDeleteAll(EntityType.FOLDER));
        this.folderToDelete = folderToDelete;
        this.folderAdmin = folderAdmin;
        this.confirmationEnabled = confirmationEnabled;
    }

    /**
     * @return the action name
     */
    @Override
    public String getName() {
        return DELETE_FOLDER;
    }

    /**
     * @return the action description
     */
    @Override
    public String getDescription() {
        return DELETE_FOLDER;
    }

    /**
     * specify the resource name for this action
     */
    @Override
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    public static void confirmFolderDeletion(final String folderName, final DialogDisplayer.OptionListener optionListener) {
        DialogDisplayer.showSafeConfirmDialog(
                TopComponents.getInstance().getTopParent(),
                getUserConfirmationMessage(folderName),
                DELETE_FOLDER,
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE,
                optionListener
        );
    }

    public static void showNonEmptyFolderDialog(final String folderName) {
        JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), "Could not delete folder '" +
                folderName + "' because some of its contents are still in use.", "Delete Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     */
    @Override
    protected void performAction() {
        if (!confirmationEnabled) {
            doDelete();
            return;
        }

        confirmFolderDeletion(folderToDelete.getName(), new DialogDisplayer.OptionListener() {
            @Override
            public void reportResult(int option) {
                if (option == JOptionPane.YES_OPTION) {
                    doDelete();
                }
            }
        });
    }

    private void doDelete() {
        final ServicesAndPoliciesTree servicesAndPoliciesTree = (ServicesAndPoliciesTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
        // try to delete contents first
        servicesAndPoliciesTree.deleteMultipleEntities(folderToDelete.getChildNodes(), false);
        deleteFolderNode(folderToDelete);
    }

    private void deleteFolderNode(final FolderNode folderNode) {
        if(folderNode.getChildCount() == 0){
            try {
                folderAdmin.deleteFolder(folderNode.getOid());

                JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    model.removeNodeFromParent(folderNode);
                }
            } catch(ObjectModelException e) {
                JOptionPane.showMessageDialog(TopComponents.getInstance().getTopParent(), "Error deleting folder:\n" + ExceptionUtils.getMessage(e), "Delete Error", JOptionPane.ERROR_MESSAGE );
            }
        }else{
            showNonEmptyFolderDialog(folderNode.getName());
        }
    }

    private static String getUserConfirmationMessage(final String folderName) {
        StringBuilder sb = new StringBuilder("Are you sure you want to delete the '").append(folderName).append("' folder and all its contents?");
        if (sb.length() <= 80) return sb.toString();

        sb = new StringBuilder("Are you sure you want to delete the folder,\n");
        String nodeName = "'" + folderName + "'";
        while (nodeName.length() > 80) {
            sb.append(nodeName.substring(0, 80)).append("\n");
            nodeName = nodeName.substring(80);
        }
        sb.append(nodeName).append(" and all its contents?");

        return sb.toString();
    }
}
