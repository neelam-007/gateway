package com.l7tech.console.action;

import com.l7tech.console.tree.AbstractTreeNode;
import com.l7tech.console.tree.ServicesAndPoliciesTree;
import com.l7tech.console.tree.servicesAndPolicies.RootNode;
import com.l7tech.console.tree.servicesAndPolicies.FolderNode;
import com.l7tech.console.util.TopComponents;
import com.l7tech.console.panels.PolicyFolderPropertiesDialog;
import com.l7tech.objectmodel.UpdateException;
import com.l7tech.objectmodel.SaveException;
import com.l7tech.objectmodel.folder.Folder;
import com.l7tech.objectmodel.folder.FolderHeader;
import com.l7tech.gateway.common.admin.FolderAdmin;
import com.l7tech.objectmodel.EntityType;
import com.l7tech.objectmodel.ConstraintViolationException;
import com.l7tech.gateway.common.security.rbac.AttemptedCreate;
import com.l7tech.gui.util.DialogDisplayer;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.awt.*;

/**
 * Action for creating a new service/policy folder.
 */
public class CreateFolderAction extends SecureAction {
    static Logger log = Logger.getLogger(CreateFolderAction.class.getName());

    private long parentFolderOid;
    private AbstractTreeNode parentNode;
    private FolderAdmin folderAdmin;

    public CreateFolderAction(long parentFolderOid,
                                    AbstractTreeNode parentNode,
                                    FolderAdmin folderAdmin)
    {
        super(new AttemptedCreate(EntityType.FOLDER), UI_PUBLISH_SERVICE_WIZARD);
        this.parentFolderOid = parentFolderOid;
        this.parentNode = parentNode;
        this.folderAdmin = folderAdmin;
    }

    /**
     * @return the action name
     */
    public String getName() {
        return "Create New Folder";
    }

    /**
     * @return the action description
     */
    public String getDescription() {
        return "Create New Folder";
    }

    /**
     * specify the resource name for this action
     */
    protected String iconResource() {
        return "com/l7tech/console/resources/folder.gif";
    }

    /**
     */
    protected void performAction() {
        Frame f = TopComponents.getInstance().getTopParent();
        PolicyFolderPropertiesDialog dialog = new PolicyFolderPropertiesDialog(f, "");
        dialog.setModal(true);
        dialog.setVisible(true);

        if(dialog.isConfirmed()) {
            Folder folder = new Folder(dialog.getName(), parentFolderOid);
            try {
                folder.setOid(folderAdmin.saveFolder(folder));

                final JTree tree = (JTree)TopComponents.getInstance().getComponent(ServicesAndPoliciesTree.NAME);
                if (tree != null) {
                    DefaultTreeModel model = (DefaultTreeModel)tree.getModel();
                    FolderHeader header = new FolderHeader(folder);
                    final AbstractTreeNode sn = new FolderNode(header);
                    model.insertNodeInto(sn, parentNode, parentNode.getInsertPosition(sn, RootNode.getComparator()));

                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            //reset filter
                            ((ServicesAndPoliciesTree) tree).filterTreeToDefault();
                        }
                    });
                }
            } catch(ConstraintViolationException e) {
                DialogDisplayer.showMessageDialog(dialog,
                                                 "Folder '"+dialog.getName()+"' already exists.",
                                                 "Folder Already Exists",
                                                 JOptionPane.WARNING_MESSAGE, null);
            } catch(UpdateException e) {
                log.log(Level.WARNING, "Failed to create policy folder", e);
            } catch(SaveException e) {
                JOptionPane.showMessageDialog(f, "Cannot create folder: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
                log.log(Level.WARNING, "Failed to create policy folder", e);
            }
        }
    }
}
