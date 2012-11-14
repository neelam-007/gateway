package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteSpecific;
import com.l7tech.gui.util.DialogDisplayer;
import com.l7tech.objectmodel.EntityType;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

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
        super(new AttemptedDeleteSpecific(EntityType.FOLDER, folderToDelete.getFolder()));
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
        final List<AbstractTreeNode> nodesToDelete = new ArrayList<AbstractTreeNode>(1);
        nodesToDelete.add(folderToDelete);
        servicesAndPoliciesTree.deleteMultipleEntities(nodesToDelete, false);
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
