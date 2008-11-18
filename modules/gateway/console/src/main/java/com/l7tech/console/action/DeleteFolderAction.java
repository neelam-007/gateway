package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.util.TopComponents;
import com.l7tech.objectmodel.DeleteException;
import com.l7tech.objectmodel.FindException;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gateway.common.security.rbac.AttemptedDeleteAll;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

/**
 * Action to delete a service/policy folder.
 */
public class DeleteFolderAction extends SecureAction {
    private long folderOid;
    private AbstractTreeNode folderToDelete;
    private FolderAdmin folderAdmin;

    public DeleteFolderAction(long folderOid, AbstractTreeNode folderToDelete, FolderAdmin folderAdmin) {
        super(new AttemptedDeleteAll(EntityType.FOLDER), UI_PUBLISH_SERVICE_WIZARD);
        this.folderOid = folderOid;
        this.folderToDelete = folderToDelete;
        this.folderAdmin = folderAdmin;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Delete Folder";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Delete Folder";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/delete.gif";
    }

    /**
     */
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        if(folderToDelete.getChildCount() > 0) {
            JOptionPane.showMessageDialog(f, "Cannot delete non-empty folders.", "Delete Error", JOptionPane.ERROR_MESSAGE);
        } else {
            int result = JOptionPane.showConfirmDialog(f,
                                                       getUserConfirmationMessage(),
                                                       getUserConfirmationTitle(),
                                                       JOptionPane.YES_NO_OPTION,
                                                       JOptionPane.QUESTION_MESSAGE);

            if(result == JOptionPane.YES_OPTION) {
                try {
                    folderAdmin.deleteFolder(folderOid);

                    JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                    if (tree != null) {
                        DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                        model.removeNodeFromParent(folderToDelete);
                    }
                } catch(DeleteException e) {
                } catch(FindException e) {
                }
            }
        }
    }

    public String getUserConfirmationMessage() {
        return "Are you sure you want to delete the " + folderToDelete.getName() + " folder?";
    }

    public String getUserConfirmationTitle() {
        return "Delete Folder";
    }
}
